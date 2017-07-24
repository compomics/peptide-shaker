package eu.isas.peptideshaker.gui;

import com.compomics.util.gui.parameters.identification_parameters.AnnotationSettingsDialog;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import eu.isas.peptideshaker.gui.filtering.FiltersDialog;
import com.compomics.util.gui.error_handlers.notification.NotificationDialogParent;
import eu.isas.peptideshaker.gui.exportdialogs.FeaturesPreferencesDialog;
import eu.isas.peptideshaker.gui.exportdialogs.FollowupPreferencesDialog;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.software.CompomicsWrapper;
import com.compomics.software.ToolFactory;
import com.compomics.software.autoupdater.MavenJarFile;
import com.compomics.software.dialogs.JavaHomeOrMemoryDialogParent;
import com.compomics.software.dialogs.JavaSettingsDialog;
import com.compomics.software.dialogs.SearchGuiSetupDialog;
import com.compomics.software.dialogs.ReporterSetupDialog;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.genes.GeneFactory;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.*;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.PrivacySettingsDialog;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.error_handlers.notification.NotesDialog;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import com.compomics.util.gui.parameters.identification_parameters.SearchSettingsDialog;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.preferencesdialogs.*;
import eu.isas.peptideshaker.gui.tabpanels.AnnotationPanel;
import eu.isas.peptideshaker.gui.tabpanels.GOEAPanel;
import eu.isas.peptideshaker.gui.tabpanels.OverviewPanel;
import eu.isas.peptideshaker.gui.tabpanels.ProteinStructurePanel;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.gui.tabpanels.QCPanel;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import eu.isas.peptideshaker.gui.tabpanels.ValidationPanel;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.preferences.UserPreferences;
import eu.isas.peptideshaker.PeptideShakerWrapper;
import eu.isas.peptideshaker.gui.gettingStarted.GettingStartedDialog;
import eu.isas.peptideshaker.gui.tabpanels.*;
import eu.isas.peptideshaker.gui.pride.ProjectExportDialog;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathPreferences;
import com.compomics.software.settings.gui.PathSettingsDialog;
import com.compomics.util.FileAndFileFilter;
import com.compomics.util.experiment.ProjectParameters;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.filtering.Filter;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.preferences.IdMatchValidationPreferences;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.gui.parameters.IdentificationParametersEditionDialog;
import com.compomics.util.gui.parameters.IdentificationParametersOverviewDialog;
import com.compomics.util.gui.parameters.ProcessingPreferencesDialog;
import com.compomics.util.gui.parameters.identification_parameters.GenePreferencesDialog;
import com.compomics.util.preferences.ValidationQCPreferences;
import com.compomics.util.gui.parameters.identification_parameters.ValidationQCPreferencesDialog;
import com.compomics.util.gui.parameters.identification_parameters.ValidationQCPreferencesDialogParent;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.export.ProjectExport;
import eu.isas.peptideshaker.filtering.AssumptionFilter;
import eu.isas.peptideshaker.filtering.MatchFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.gui.exportdialogs.MethodsSectionDialog;
import eu.isas.peptideshaker.gui.exportdialogs.MzIdentMLExportDialog;
import eu.isas.peptideshaker.gui.filtering.FilterDialog;
import eu.isas.peptideshaker.gui.pride.PrideReshakeGUI;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences.PeptideShakerPathKey;
import eu.isas.peptideshaker.ptm.PtmScorer;
import eu.isas.peptideshaker.scoring.maps.PsmPTMMap;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import eu.isas.peptideshaker.utils.PsZipUtils;
import eu.isas.peptideshaker.utils.StarHider;
import eu.isas.peptideshaker.validation.MatchesValidator;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import net.jimmc.jshortcut.JShellLink;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;

/**
 * The main PeptideShaker frame.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class PeptideShakerGUI extends JFrame implements ClipboardOwner, JavaHomeOrMemoryDialogParent, NotificationDialogParent, ValidationQCPreferencesDialogParent {

    /**
     * The path to the example dataset.
     */
    private final String EXAMPLE_DATASET_PATH = "/resources/example_dataset/HeLa Example.cpsx";
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
    private LastSelectedFolder lastSelectedFolder;
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory;
    /**
     * The compomics enzyme factory.
     */
    private EnzymeFactory enzymeFactory;
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * The processing preferences
     */
    private ProcessingPreferences processingPreferences = new ProcessingPreferences();
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
    private ValidationPanel statsPanel;
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
    private SpectrumFactory spectrumFactory;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory;
    /**
     * The exception handler
     */
    private FrameExceptionHandler exceptionHandler = new FrameExceptionHandler(this, "https://github.com/compomics/peptide-shaker/issues");
    /**
     * The spectrum annotator.
     */
    private PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
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
    public static String TITLED_BORDER_HORIZONTAL_PADDING = ""; // @TODO: move to utilities?
    /**
     * The horizontal padding used before and after the text in the titled
     * borders. (Needed to make it look as good in Java 7 as it did in Java
     * 6...)
     */
    public static String TITLED_BORDER_HORIZONTAL_PADDING_HTML = ""; // @TODO: move to utilities?
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
     * The list of current tips to the user.
     */
    private ArrayList<String> currentTips = new ArrayList<String>();
    /**
     * The cps parent used to manage the data.
     */
    private CpsParent cpsParent = new CpsParent(PeptideShaker.getMatchesFolder());
    /**
     * True if an existing project is currently in the process of being opened.
     */
    private boolean openingExistingProject = false;
    /**
     * The annotation preferences for the currently selected spectrum and
     * peptide.
     */
    private SpecificAnnotationSettings specificAnnotationPreferences;
    /**
     * The list of spectrum files.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<File>();

    /**
     * The main method used to start PeptideShaker.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {

        // set the look and feel
        boolean numbusLookAndFeelSet = false;
        try {
            numbusLookAndFeelSet = UtilitiesGUIDefaults.setLookAndFeel();

            // fix for the scroll bar thumb disappearing...
            LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
            UIDefaults defaults = lookAndFeel.getDefaults();
            defaults.put("ScrollBar.minimumThumbSize", new Dimension(30, 30));
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
            TITLED_BORDER_HORIZONTAL_PADDING_HTML = "&nbsp;&nbsp;&nbsp;";
        }

        // turn off the derby log file
        DerbyUtil.disableDerbyLog();

        // create the gene mappping folder if not set
        GeneFactory geneFactory = GeneFactory.getInstance();
        try {
            geneFactory.initialize(PeptideShaker.getJarFilePath());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred while loading the gene mappings.", "Gene Mapping File Error", JOptionPane.ERROR_MESSAGE);
        }

        // load the species mapping
        try {
            SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
            speciesFactory.initiate(PeptideShaker.getJarFilePath());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred while loading the species mapping.", "File Error", JOptionPane.OK_OPTION);
        }

        // see if a cps or url is to be opened, or a px accesion for reshake
        File cpsFile = null;
        boolean cps = false;
        String zipUrl = null;
        boolean url = false;
        String zipUrlDownloadFolder = null;
        boolean downloadFolder = false;
        String pxAccession = null;
        boolean pxAccessionProvided = false;
        boolean pxAccessionPrivate = false;

        for (String arg : args) {
            if (cps) {
                cpsFile = new File(arg);
                cps = false;
            } else if (url) {
                zipUrl = arg;
                url = false;
            } else if (downloadFolder) {
                zipUrlDownloadFolder = arg;
                downloadFolder = false;
            } else if (pxAccessionProvided) {
                pxAccession = arg;
                pxAccessionProvided = false;
            }

            if (arg.equals(ToolFactory.peptideShakerFileOption)) {
                cps = true;
            } else if (arg.equals(ToolFactory.peptideShakerUrlOption)) {
                url = true;
            } else if (arg.equals(ToolFactory.peptideShakerUrlDownloadFolderOption)) {
                downloadFolder = true;
            } else if (arg.equals(ToolFactory.peptideShakerPxAccessionOption)) {
                pxAccessionProvided = true;
            } else if (arg.equals(ToolFactory.peptideShakerPxAccessionPrivateOption)) {
                pxAccessionPrivate = true;
            }
        }

        // check if the download folder was provided if a url was given
        if (zipUrl != null && zipUrlDownloadFolder == null) {
            zipUrl = null;
            JOptionPane.showMessageDialog(null,
                    "Setting the download folder is mandatory when opening a URL. Please use the -zipUrlFolder option.", "Downloading URL Error",
                    JOptionPane.WARNING_MESSAGE);
        }

        new PeptideShakerGUI(cpsFile, zipUrl, zipUrlDownloadFolder, pxAccession, pxAccessionPrivate, true);
    }

    /**
     * Sets the path configuration, displays the path settings dialog if a
     * folder cannot be accessed.
     */
    private void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(PeptideShaker.getJarFilePath(), UtilitiesPathPreferences.configurationFileName);
        if (pathConfigurationFile.exists()) {
            PeptideShakerPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
        }
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
     * @param zipURL the URL to a zipped cps file to download and load, can be
     * null
     * @param zipUrlDownloadFolder the folder to download the URL to, can be
     * null
     * @param pxAccession a PX accession to open in the PRIDE Reshake, can be
     * null
     * @param pxAccessionPrivate if true, the PX accession is private
     * @param showWelcomeDialog boolean indicating if the Welcome Dialog is to
     * be shown
     */
    public PeptideShakerGUI(File cpsFile, String zipURL, String zipUrlDownloadFolder, String pxAccession, boolean pxAccessionPrivate, boolean showWelcomeDialog) {

        // set up the ErrorLog
        setUpLogFile(true);

        // set path configuration
        try {
            setPathConfiguration();
        } catch (Exception e) {
            // Will be taken care of next 
        }
        try {
            if (!PeptideShakerPathPreferences.getErrorKeys().isEmpty()) {
                editPathSettings(null);
            }
        } catch (Exception e) {
            editPathSettings(null);
        }

        // load the user preferences
        loadUserPreferences();

        // check for new version
        boolean newVersion = false;
        if (!PeptideShaker.getJarFilePath().equalsIgnoreCase(".") && utilitiesUserPreferences.isAutoUpdate()) {
            newVersion = checkForNewVersion();
        }

        if (!newVersion) {

            // set this version as the default PeptideShaker version
            if (!PeptideShaker.getJarFilePath().equalsIgnoreCase(".")) {
                utilitiesUserPreferences.setPeptideShakerPath(new File(PeptideShaker.getJarFilePath(), "PeptideShaker-" + PeptideShaker.getVersion() + ".jar").getAbsolutePath());
                UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
            }

            // check for 64 bit java and for at least 4 gb memory 
            boolean java64bit = CompomicsWrapper.is64BitJava();
            boolean memoryOk = (utilitiesUserPreferences.getMemoryPreference() >= 4000);
            String javaVersion = System.getProperty("java.version");
            boolean javaVersionWarning = javaVersion.startsWith("1.5") || javaVersion.startsWith("1.6");

            // add desktop shortcut?
            if (!PeptideShaker.getJarFilePath().equalsIgnoreCase(".")
                    && System.getProperty("os.name").lastIndexOf("Windows") != -1
                    && new File(PeptideShaker.getJarFilePath() + "/resources/conf/firstRun").exists()) {

                // @TODO: add support for desktop icons in mac and linux??
                // delete the firstRun file such that the user is not asked the next time around
                boolean fileDeleted = new File(PeptideShaker.getJarFilePath() + "/resources/conf/firstRun").delete();

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

            // Instantiate factories
            PeptideShaker.instantiateFacories(utilitiesUserPreferences);
            ptmFactory = PTMFactory.getInstance();
            enzymeFactory = EnzymeFactory.getInstance();
            spectrumFactory = SpectrumFactory.getInstance();
            sequenceFactory = SequenceFactory.getInstance();

            // set the font color for the titlted borders, looks better than the default black
            UIManager.put("TitledBorder.titleColor", new Color(59, 59, 59));

            initComponents();

            psmSortRtRadioButtonMenuItem.setSelected(utilitiesUserPreferences.getSortPsmsOnRt());

            reshakeMenuItem.setVisible(false); // @TODO: re-enable later?
            quantifyMenuItem.setVisible(false); // @TODO: re-enable later?
            jSeparator2.setVisible(false); // @TODO: re-enable later?
            reporterPreferencesJMenuItem.setVisible(false); // @TODO: re-enable later?
            
            exportPrideMenuItem.setVisible(false); // disable the pride xml export

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
            statsPanel = new ValidationPanel(this);

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

            setLocationRelativeTo(null);

            if (cpsFile != null) {
                setVisible(true);
                if (cpsFile.getName().endsWith(".zip")) {
                    importPeptideShakerZipFile(cpsFile);
                } else {
                    importPeptideShakerFile(cpsFile);
                }
            } else if (zipURL != null) {
                setVisible(true);
                importPeptideShakerZipFromURL(zipURL, zipUrlDownloadFolder);
            } else if (pxAccession != null) {
                new PrideReshakeGUI(this, pxAccession, pxAccessionPrivate);
            } else if (showWelcomeDialog) {
                // open the welcome dialog
                new WelcomeDialog(this, !java64bit || !memoryOk, javaVersionWarning, true);
            }
        }
    }

    /**
     * Loads the user preferences.
     */
    public void loadUserPreferences() {
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
            lastSelectedFolder = utilitiesUserPreferences.getLastSelectedFolder();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (lastSelectedFolder == null) {
            lastSelectedFolder = new LastSelectedFolder();
        }
        cpsParent.loadUserPreferences();
    }

    /**
     * Sets the project.
     *
     * @param projectParameters the experiment
     */
    public void setProject(ProjectParameters projectParameters) {
        cpsParent.setProject(projectParameters);
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
        if (getProjectParameters() != null) {
            this.setTitle("PeptideShaker " + PeptideShaker.getVersion() + " - " + getProjectParameters().getProjectUniqueName());
        } else {
            this.setTitle("PeptideShaker " + PeptideShaker.getVersion());
        }
    }

    /**
     * Reset the frame title.
     */
    public void resetFrameTitle() {
        this.setTitle("PeptideShaker " + PeptideShaker.getVersion());
    }

    /**
     * Returns the last selected folder.
     *
     * @return the last selected folder
     */
    public LastSelectedFolder getLastSelectedFolder() {
        if (lastSelectedFolder == null) {
            lastSelectedFolder = new LastSelectedFolder();
            utilitiesUserPreferences.setLastSelectedFolder(lastSelectedFolder);
        }
        return lastSelectedFolder;
    }

    /**
     * Set the last selected folder.
     *
     * @param lastSelectedFolder the folder to set
     */
    public void setLastSelectedFolder(LastSelectedFolder lastSelectedFolder) {
        this.lastSelectedFolder = lastSelectedFolder;
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
        relatedIonsCheckMenu = new javax.swing.JCheckBoxMenuItem();
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
        highResAnnotationCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        barsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        bubbleScaleJMenuItem = new javax.swing.JMenuItem();
        intensityIonTableRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        mzIonTableRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        defaultAnnotationCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
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
        resetAnnotationMenu = new javax.swing.JMenu();
        ionTableButtonGroup = new javax.swing.ButtonGroup();
        deNovoChargeButtonGroup = new javax.swing.ButtonGroup();
        psmSortOrderButtonGroup = new javax.swing.ButtonGroup();
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
        jSeparator20 = new javax.swing.JPopupMenu.Separator();
        openExampleMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        reshakeMenuItem = new javax.swing.JMenuItem();
        quantifyMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        projectPropertiesMenuItem = new javax.swing.JMenuItem();
        projectSettingsMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        exitJMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        searchParametersMenu = new javax.swing.JMenuItem();
        annotationPreferencesMenu = new javax.swing.JMenuItem();
        validationQcMenuItem = new javax.swing.JMenuItem();
        speciesJMenuItem = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        preferencesMenuItem = new javax.swing.JMenuItem();
        fractionDetailsJMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        javaOptionsJMenuItem = new javax.swing.JMenuItem();
        processingMenuItem = new javax.swing.JMenuItem();
        editIdSettingsFilesMenuItem = new javax.swing.JMenuItem();
        configurationFilesSettings = new javax.swing.JMenuItem();
        privacyMenuItem = new javax.swing.JMenuItem();
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
        methodsSectionMenuItem = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        projectExportMenu = new javax.swing.JMenu();
        exportProjectMenuItem = new javax.swing.JMenuItem();
        exportMzIdentMLMenuItem = new javax.swing.JMenuItem();
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
        psmSortOrderMenu = new javax.swing.JMenu();
        psmSortScoreRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        psmSortRtRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
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

        relatedIonsCheckMenu.setSelected(true);
        relatedIonsCheckMenu.setText("Related");
        relatedIonsCheckMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                relatedIonsCheckMenuActionPerformed(evt);
            }
        });
        otherMenu.add(relatedIonsCheckMenu);

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

        forwardIonsDeNovoCheckBoxMenuItem.setText("b-ions");
        forwardIonsDeNovoCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardIonsDeNovoCheckBoxMenuItemActionPerformed(evt);
            }
        });
        deNovoMenu.add(forwardIonsDeNovoCheckBoxMenuItem);

        rewindIonsDeNovoCheckBoxMenuItem.setText("y-ions");
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

        highResAnnotationCheckBoxMenuItem.setSelected(true);
        highResAnnotationCheckBoxMenuItem.setText("High Resolution");
        highResAnnotationCheckBoxMenuItem.setToolTipText("Use high resolution annotation");
        highResAnnotationCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                highResAnnotationCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(highResAnnotationCheckBoxMenuItem);

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

        defaultAnnotationCheckBoxMenuItem.setSelected(true);
        defaultAnnotationCheckBoxMenuItem.setText("Automatic Annotation");
        defaultAnnotationCheckBoxMenuItem.setToolTipText("Use automatic annotation");
        defaultAnnotationCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                defaultAnnotationCheckBoxMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(defaultAnnotationCheckBoxMenuItem);
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

        exportMassErrorPlotGraphicsJMenuItem.setText("m/z Error Plot");
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

        resetAnnotationMenu.setText("<html><a href>Reset Annotation</a></html>");
        resetAnnotationMenu.setFocusable(false);
        resetAnnotationMenu.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                resetAnnotationMenuSelected(evt);
            }
        });
        annotationMenuBar.add(resetAnnotationMenu);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PeptideShaker");
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(1280, 750));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
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
        backgroundLayeredPane.setLayer(newsButton, javax.swing.JLayeredPane.MODAL_LAYER);
        backgroundLayeredPane.add(newsButton);
        newsButton.setBounds(1205, 825, 70, 20);

        notesButton.setBackground(new java.awt.Color(204, 204, 204));
        notesButton.setFont(notesButton.getFont().deriveFont(notesButton.getFont().getStyle() | java.awt.Font.BOLD));
        notesButton.setForeground(new java.awt.Color(255, 255, 255));
        notesButton.setText("Notes");
        notesButton.setBorder(null);
        notesButton.setBorderPainted(false);
        notesButton.setContentAreaFilled(false);
        notesButton.setOpaque(true);
        notesButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                notesButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                notesButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                notesButtonMouseReleased(evt);
            }
        });
        backgroundLayeredPane.setLayer(notesButton, javax.swing.JLayeredPane.MODAL_LAYER);
        backgroundLayeredPane.add(notesButton);
        notesButton.setBounds(1205, 775, 70, 20);

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
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tipsButtonMouseReleased(evt);
            }
        });
        backgroundLayeredPane.setLayer(tipsButton, javax.swing.JLayeredPane.MODAL_LAYER);
        backgroundLayeredPane.add(tipsButton);
        tipsButton.setBounds(1205, 800, 70, 20);

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
        fileJMenu.add(jSeparator20);

        openExampleMenuItem.setMnemonic('E');
        openExampleMenuItem.setText("Open Example");
        openExampleMenuItem.setToolTipText("Open a PeptideShaker example project");
        openExampleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openExampleMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(openExampleMenuItem);
        fileJMenu.add(jSeparator8);

        reshakeMenuItem.setMnemonic('H');
        reshakeMenuItem.setText("Reshake...");
        reshakeMenuItem.setToolTipText("<html>\nReanalyze PRIDE experiments.<br>\n</html>");
        reshakeMenuItem.setEnabled(false);
        reshakeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reshakeMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(reshakeMenuItem);

        quantifyMenuItem.setMnemonic('R');
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

        projectSettingsMenuItem.setMnemonic('I');
        projectSettingsMenuItem.setText("Project Settings");
        projectSettingsMenuItem.setEnabled(false);
        projectSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projectSettingsMenuItemActionPerformed(evt);
            }
        });
        fileJMenu.add(projectSettingsMenuItem);
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

        annotationPreferencesMenu.setMnemonic('A');
        annotationPreferencesMenu.setText("Spectrum Annotations");
        annotationPreferencesMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationPreferencesMenuActionPerformed(evt);
            }
        });
        editMenu.add(annotationPreferencesMenu);

        validationQcMenuItem.setMnemonic('V');
        validationQcMenuItem.setText("Validation Filters");
        validationQcMenuItem.setEnabled(false);
        validationQcMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validationQcMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(validationQcMenuItem);

        speciesJMenuItem.setMnemonic('P');
        speciesJMenuItem.setText("Species Settings");
        speciesJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speciesJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(speciesJMenuItem);
        editMenu.add(jSeparator13);

        preferencesMenuItem.setMnemonic('O');
        preferencesMenuItem.setText("Project Preferences");
        preferencesMenuItem.setEnabled(false);
        preferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                preferencesMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(preferencesMenuItem);

        fractionDetailsJMenuItem.setMnemonic('R');
        fractionDetailsJMenuItem.setText("Fraction Details");
        fractionDetailsJMenuItem.setEnabled(false);
        fractionDetailsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fractionDetailsJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(fractionDetailsJMenuItem);
        editMenu.add(jSeparator4);

        javaOptionsJMenuItem.setMnemonic('J');
        javaOptionsJMenuItem.setText("Java Settings");
        javaOptionsJMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                javaOptionsJMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(javaOptionsJMenuItem);

        processingMenuItem.setMnemonic('C');
        processingMenuItem.setText("Processing Settings");
        processingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processingMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(processingMenuItem);

        editIdSettingsFilesMenuItem.setMnemonic('D');
        editIdSettingsFilesMenuItem.setText("Identification Settings");
        editIdSettingsFilesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editIdSettingsFilesMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(editIdSettingsFilesMenuItem);

        configurationFilesSettings.setMnemonic('E');
        configurationFilesSettings.setText("Resource Settings");
        configurationFilesSettings.setToolTipText("Set paths to resource folders");
        configurationFilesSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configurationFilesSettingsActionPerformed(evt);
            }
        });
        editMenu.add(configurationFilesSettings);

        privacyMenuItem.setMnemonic('V');
        privacyMenuItem.setText("Privacy Settings");
        privacyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                privacyMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(privacyMenuItem);
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

        methodsSectionMenuItem.setMnemonic('M');
        methodsSectionMenuItem.setText("Methods Section");
        methodsSectionMenuItem.setToolTipText("<html>\nExport a draft of the method<br>\nsection for your manuscript\n</html>");
        methodsSectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                methodsSectionMenuItemActionPerformed(evt);
            }
        });
        exportJMenu.add(methodsSectionMenuItem);
        exportJMenu.add(jSeparator10);

        projectExportMenu.setText("PeptideShaker Project As");

        exportProjectMenuItem.setMnemonic('Z');
        exportProjectMenuItem.setText("Zip File");
        exportProjectMenuItem.setToolTipText("Export the complete project as a zip file");
        exportProjectMenuItem.setEnabled(false);
        exportProjectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProjectMenuItemActionPerformed(evt);
            }
        });
        projectExportMenu.add(exportProjectMenuItem);

        exportMzIdentMLMenuItem.setMnemonic('M');
        exportMzIdentMLMenuItem.setText("mzIdentML");
        exportMzIdentMLMenuItem.setToolTipText("Export the project as mzIdentML");
        exportMzIdentMLMenuItem.setEnabled(false);
        exportMzIdentMLMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMzIdentMLMenuItemActionPerformed(evt);
            }
        });
        projectExportMenu.add(exportMzIdentMLMenuItem);

        exportPrideMenuItem.setMnemonic('P');
        exportPrideMenuItem.setText("PRIDE XML");
        exportPrideMenuItem.setToolTipText("Export the project as PRIDE XML");
        exportPrideMenuItem.setEnabled(false);
        exportPrideMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPrideMenuItemActionPerformed(evt);
            }
        });
        projectExportMenu.add(exportPrideMenuItem);

        exportJMenu.add(projectExportMenu);

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

        psmSortOrderMenu.setText("PSM Sort Order");

        psmSortOrderButtonGroup.add(psmSortScoreRadioButtonMenuItem);
        psmSortScoreRadioButtonMenuItem.setSelected(true);
        psmSortScoreRadioButtonMenuItem.setText("Score");
        psmSortScoreRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmSortScoreRadioButtonMenuItemActionPerformed(evt);
            }
        });
        psmSortOrderMenu.add(psmSortScoreRadioButtonMenuItem);

        psmSortOrderButtonGroup.add(psmSortRtRadioButtonMenuItem);
        psmSortRtRadioButtonMenuItem.setText("Retention Time");
        psmSortRtRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmSortRtRadioButtonMenuItemActionPerformed(evt);
            }
        });
        psmSortOrderMenu.add(psmSortRtRadioButtonMenuItem);

        viewJMenu.add(psmSortOrderMenu);

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

        if (!dataSaved && getProjectParameters() != null) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getProjectParameters().getProjectUniqueName()+ "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
                //clearData(true, false); // @TODO: add this?
                new NewDialog(this, true);
            } else if (value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION) {
                // do nothing
            } else { // no option
                clearData(true, true);
                new NewDialog(this, true);
            }
        } else {
            clearData(true, true);
            new NewDialog(this, true);
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
                "About PeptideShaker " + PeptideShaker.getVersion());
    }//GEN-LAST:event_aboutJMenuItemActionPerformed

    /**
     * Opens the AnnotationSettingsDialog.
     *
     * @param evt
     */
    private void annotationPreferencesMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationPreferencesMenuActionPerformed
        PtmSettings ptmSettings = getIdentificationParameters().getSearchParameters().getPtmSettings();
        ArrayList<NeutralLoss> neutralLosses = IonFactory.getNeutralLosses(ptmSettings);
        ArrayList<Integer> reporterIons = new ArrayList<Integer>(IonFactory.getReporterIons(ptmSettings));
        AnnotationSettingsDialog annotationSettingsDialog = new AnnotationSettingsDialog(this, getIdentificationParameters().getAnnotationPreferences(),
                getIdentificationParameters().getSearchParameters().getFragmentIonAccuracy(), neutralLosses, reporterIons, true);
        if (!annotationSettingsDialog.isCanceled()) {
            AnnotationSettings newAnnotationSettings = annotationSettingsDialog.getAnnotationSettings();
            if (!newAnnotationSettings.isSameAs(getIdentificationParameters().getAnnotationPreferences())) {
                getIdentificationParameters().setAnnotationSettings(newAnnotationSettings);
                updateSpectrumAnnotations();
                setDataSaved(false);
            }
        }
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
                    backgroundLayeredPane.getHeight() - backgroundLayeredPane.getComponent(1).getHeight() - 59,
                    backgroundLayeredPane.getComponent(1).getWidth(),
                    backgroundLayeredPane.getComponent(1).getHeight());

            backgroundLayeredPane.getComponent(2).setBounds(
                    backgroundLayeredPane.getWidth() - backgroundLayeredPane.getComponent(2).getWidth() - 12,
                    backgroundLayeredPane.getHeight() - backgroundLayeredPane.getComponent(2).getHeight() - 37,
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

        SearchParameters searchParameters = getIdentificationParameters().getSearchParameters();
        if (searchParameters == null) {
            setDefaultPreferences();
            searchParameters = getIdentificationParameters().getSearchParameters();
        }

        new SearchSettingsDialog(this, searchParameters,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true, true, PeptideShaker.getConfigurationFile(), getLastSelectedFolder(), getIdentificationParameters().getName(), false);
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
        exportSpectrumAsFigure();
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
                            this, "Discard the changed validation settings?", "Discard Settings?",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                    if (value == JOptionPane.YES_OPTION) {
                        statsPanel.resetAllThresholds();
                        updateNeeded.put(VALIDATION_TAB_INDEX, true);
                    } else {
                        updateNeeded.put(VALIDATION_TAB_INDEX, false);
                        allTabsJTabbedPane.setSelectedIndex(VALIDATION_TAB_INDEX);
                    }
                }
            }

            new Thread(new Runnable() {
                public void run() {
                    try {
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
                            ProteinMatch proteinMatch = (ProteinMatch)getIdentification().retrieveObject(selectedProteinKey);
                            if (proteinMatch != null) {
                                annotationPanel.updateBasicProteinAnnotation(proteinMatch.getMainMatch());
                            }
                        }

                        // move the spectrum annotation menu bar and set the intensity slider value
                        AnnotationSettings annotationPreferences = getIdentificationParameters().getAnnotationPreferences();
                        switch (selectedIndex) {
                            case OVER_VIEW_TAB_INDEX:
                                overviewPanel.showSpectrumAnnotationMenu();
                                overviewPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
                                break;
                            case SPECTRUM_ID_TAB_INDEX:
                                spectrumIdentificationPanel.showSpectrumAnnotationMenu();
                                spectrumIdentificationPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
                                break;
                            case MODIFICATIONS_TAB_INDEX:
                                ptmPanel.showSpectrumAnnotationMenu();
                                ptmPanel.setIntensitySliderValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
                                break;
                            default:
                                break;
                        }

                        if (selectedIndex == OVER_VIEW_TAB_INDEX || selectedIndex == SPECTRUM_ID_TAB_INDEX || selectedIndex == MODIFICATIONS_TAB_INDEX) {
                            // invoke later to give time for components to update
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    try {
                                        updateSpectrumAnnotations();
                                    } catch (Exception e) {
                                        catchException(e);
                                    }
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

                    } catch (Exception e) {
                        catchException(e);
                    }
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
        exportBubblePlotAsFigure();
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
     * Turns the hiding of the scores columns on or off.
     *
     * @param evt
     */
    private void scoresJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scoresJCheckBoxMenuItemActionPerformed
        try {
            getDisplayPreferences().showScores(scoresJCheckBoxMenuItem.isSelected());
            overviewPanel.updateScores();
            spectrumIdentificationPanel.updateScores();
            proteinStructurePanel.updateScores();
            proteinFractionsPanel.updateScores();
            goPanel.updateScores();

            // make sure that the jsparklines are showing correctly
            sparklinesJCheckBoxMenuItemActionPerformed(null);
        } catch (Exception e) {
            catchException(e);
        }
    }//GEN-LAST:event_scoresJCheckBoxMenuItemActionPerformed

    /**
     * Open a file chooser to open an existing PeptideShaker project.
     *
     * @param evt
     */
    private void openJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openJMenuItemActionPerformed

        if (!dataSaved && getProjectParameters()!= null) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getProjectParameters().getProjectUniqueName()+ "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
            } else if (value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION) {
                return;
            } else { // no option
                // do nothing
            }
        }

        String lastSelectedFolderPath = lastSelectedFolder.getLastSelectedFolder();

        String cpsFileFilterDescription = "PeptideShaker (.cpsx)";
        String zipFileFilterDescription = "Zipped PeptideShaker (.zip)";
        FileAndFileFilter selectedFileAndFilter = Util.getUserSelectedFile(this, new String[]{".cpsx", ".zip"},
                new String[]{cpsFileFilterDescription, zipFileFilterDescription}, "Open PeptideShaker Project", lastSelectedFolderPath, null, true, false, false, 0);

        if (selectedFileAndFilter != null) {

            File selectedFile = selectedFileAndFilter.getFile();
            lastSelectedFolder.setLastSelectedFolder(selectedFile.getParent());

            if (selectedFile.getName().endsWith(".zip")) {
                importPeptideShakerZipFile(selectedFile);
            } else if (selectedFile.getName().endsWith(".cpsx")) {
                exceptionHandler.setIgnoreExceptions(true);
                clearData(true, true);
                exceptionHandler.setIgnoreExceptions(false);
                clearPreferences();
                getUserPreferences().addRecentProject(selectedFile);
                updateRecentProjectsList();
                importPeptideShakerFile(selectedFile);
                lastSelectedFolder.setLastSelectedFolder(selectedFile.getAbsolutePath());
            } else {
                JOptionPane.showMessageDialog(this, "Not a PeptideShaker file (.cpsx).", "Unsupported File.", JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_openJMenuItemActionPerformed

    /**
     * Open the GUI Settings dialog.
     *
     * @param evt
     */
    private void preferencesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preferencesMenuItemActionPerformed

        ProjectSettingsDialog preferencesDialog = new ProjectSettingsDialog(this, getSpectrumCountingPreferences(), getDisplayPreferences());

        if (!preferencesDialog.isCanceled()) {

            // See if the spectrum counting preferences need to be updated
            SpectrumCountingPreferences newSpectrumCountingPreferences = preferencesDialog.getSpectrumCountingPreferences();
            if (!newSpectrumCountingPreferences.isSameAs(getSpectrumCountingPreferences())) {
                setSpectrumCountingPreferences(newSpectrumCountingPreferences);
                getIdentificationFeaturesGenerator().clearSpectrumCounting();
                setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, false);
                setUpdated(PeptideShakerGUI.STRUCTURES_TAB_INDEX, false);
                setUpdated(PeptideShakerGUI.QC_PLOTS_TAB_INDEX, false);
                updateTabbedPanes();
                setDataSaved(false);
                // @TODO: update the metrics if necessary
            }

            // See if the display preferences need to be updated
            DisplayPreferences newDisplayPreferences = preferencesDialog.getDisplayPreferences();
            // @TODO: uncomment the code below when the display prefrences have been set
//            if (!newDisplayPreferences.isSameAs(getDisplayPreferences())) {
//                setDisplayPreferences(newDisplayPreferences);
//                //@TODO: update the display
//            }
            if (newDisplayPreferences.getnAASurroundingPeptides() != getDisplayPreferences().getnAASurroundingPeptides()) {
                getDisplayPreferences().setnAASurroundingPeptides(newDisplayPreferences.getnAASurroundingPeptides());
                updateSurroundingAminoAcids();
            }
        }
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
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_aIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void bIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_bIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void cIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_cIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void xIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_xIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void yIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_yIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void zIonCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_zIonCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void allCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allCheckBoxMenuItemActionPerformed
        updateSpectrumAnnotations();
    }//GEN-LAST:event_allCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void barsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_barsCheckBoxMenuItemActionPerformed
        getDisplayPreferences().setShowBars(barsCheckBoxMenuItem.isSelected());
        updateSpectrumAnnotations();
    }//GEN-LAST:event_barsCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void intensityIonTableRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intensityIonTableRadioButtonMenuItemActionPerformed
        getDisplayPreferences().setIntensityIonTable(intensityIonTableRadioButtonMenuItem.isSelected());
        updateSpectrumAnnotations();
    }//GEN-LAST:event_intensityIonTableRadioButtonMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void mzIonTableRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mzIonTableRadioButtonMenuItemActionPerformed
        getDisplayPreferences().setIntensityIonTable(intensityIonTableRadioButtonMenuItem.isSelected());
        updateSpectrumAnnotations();
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
                        "Ion Table - Help");
            } else if (spectrumTabIndex == 1) {
                new HelpDialog(this, getClass().getResource("/helpFiles/BubblePlot.html"),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        "Bubble Plot - Help");
            } else if (spectrumTabIndex == 2) {
                new HelpDialog(this, getClass().getResource("/helpFiles/SpectrumPanel.html"),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        "Spetrum Panel - Help");
            }
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            new HelpDialog(this, getClass().getResource("/helpFiles/SpectrumPanel.html"),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "Spectrum Panel - Help");
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            new HelpDialog(this, getClass().getResource("/helpFiles/PTMPanel.html"),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "Modification Analysis - Help");
        }

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpMenuItemActionPerformed

    /**
     * Save the current spectrum/spectra to an MGF file.
     */
    private void exportSpectrumValuesJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumValuesJMenuItemActionPerformed
        try {
            exportSelectedSpectraAsMgf();
        } catch (Exception e) {
            catchException(e);
        }
    }//GEN-LAST:event_exportSpectrumValuesJMenuItemActionPerformed

    /**
     * Set if the current annotation is to be used for all spectra.
     *
     * @param evt
     */
    private void defaultAnnotationCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultAnnotationCheckBoxMenuItemActionPerformed
        updateSpectrumAnnotations();
    }//GEN-LAST:event_defaultAnnotationCheckBoxMenuItemActionPerformed

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
        exportSequenceFragmentationAsFigure();
    }//GEN-LAST:event_exportSequenceFragmentationGraphicsJMenuItemActionPerformed

    /**
     * Export the intensity histogram as a figure.
     *
     * @param evt
     */
    private void exportIntensityHistogramGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportIntensityHistogramGraphicsJMenuItemActionPerformed
        exportIntensityHistogramAsFigure();
    }//GEN-LAST:event_exportIntensityHistogramGraphicsJMenuItemActionPerformed

    /**
     * Export the mass error plot as a figure.
     *
     * @param evt
     */
    private void exportMassErrorPlotGraphicsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMassErrorPlotGraphicsJMenuItemActionPerformed
        exportMassErrorPlotAsFigure();
    }//GEN-LAST:event_exportMassErrorPlotGraphicsJMenuItemActionPerformed

    /**
     * Opens the filter settings dialog.
     *
     * @param evt
     */
    private void projectSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_projectSettingsMenuItemActionPerformed
        IdentificationParameters identificationParameters = getIdentificationParameters();
        IdentificationParametersEditionDialog identificationParametersEditionDialog = new IdentificationParametersEditionDialog(
                this, identificationParameters, PeptideShaker.getConfigurationFile(), getNormalIcon(), getWaitingIcon(), lastSelectedFolder, this, false);
    }//GEN-LAST:event_projectSettingsMenuItemActionPerformed

    /**
     * Save the current project.
     *
     * @param evt
     */
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        if (cpsParent.getCpsFile() != null && cpsParent.getCpsFile().exists()) {
            saveProject(false, false);
        } else {
            saveProjectAs(false, false);
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    /**
     * Hide/display the spectrum accuracy and intensity level sliders.
     *
     * @param evt
     */
    private void spectrumSlidersCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumSlidersCheckBoxMenuItemActionPerformed
        cpsParent.getUserPreferences().setShowSliders(spectrumSlidersCheckBoxMenuItem.isSelected());
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
        new BugReport(this, lastSelectedFolder, "PeptideShaker", "peptide-shaker", PeptideShaker.getVersion(),
                "peptide-shaker", "PeptideShaker", new File(PeptideShaker.getJarFilePath() + "/resources/PeptideShaker.log"));
    }//GEN-LAST:event_logReportMenuActionPerformed

    /**
     * Open the Java Settings dialog.
     *
     * @param evt
     */
    private void javaOptionsJMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaOptionsJMenuItemActionPerformed
        new JavaSettingsDialog(this, this, null, "PeptideShaker", true);
    }//GEN-LAST:event_javaOptionsJMenuItemActionPerformed

    /**
     * Update annotations.
     *
     * @param evt
     */
    private void adaptCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adaptCheckBoxMenuItemActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
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
            JOptionPane.showMessageDialog(null, "An error occurred when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        new SpectrumColorsDialog(this);
    }//GEN-LAST:event_annotationColorsJMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void precursorCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorCheckMenuActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_precursorCheckMenuActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void immoniumIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_immoniumIonsCheckMenuActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_immoniumIonsCheckMenuActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void reporterIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reporterIonsCheckMenuActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
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
     * @see #updateAnnotationPreferences()
     */
    private void forwardIonsDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardIonsDeNovoCheckBoxMenuItemActionPerformed
        updateSpectrumAnnotations();
    }//GEN-LAST:event_forwardIonsDeNovoCheckBoxMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void rewindIonsDeNovoCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rewindIonsDeNovoCheckBoxMenuItemActionPerformed
        updateSpectrumAnnotations();
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
        updateSpectrumAnnotations();
    }//GEN-LAST:event_deNovoChargeOneJRadioButtonMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void deNovoChargeTwoJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deNovoChargeTwoJRadioButtonMenuItemActionPerformed
        updateSpectrumAnnotations();
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
            for (String ptm : getIdentificationParameters().getSearchParameters().getPtmSettings().getFixedModifications()) {
                getDisplayPreferences().setDisplayedPTM(ptm, true);
            }
        } else {
            for (String ptm : getIdentificationParameters().getSearchParameters().getPtmSettings().getFixedModifications()) {
                getDisplayPreferences().setDisplayedPTM(ptm, false);
            }
        }

        displayFeaturesGenerator.setDisplayedPTMs(getDisplayPreferences().getDisplayedPtms());
        updatePtmColorCoding();
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
            new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, (Component) overviewPanel.getSpectrumAndPlots(), lastSelectedFolder);
        }
    }//GEN-LAST:event_exportSpectrumAndPlotsGraphicsJMenuItemActionPerformed

    /**
     * Export project to PRIDE XML.
     *
     * @param evt
     */
    private void exportPrideMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPrideMenuItemActionPerformed
        // @TODO: check that all ptms are mapped to a cv term
        new ProjectExportDialog(this, true);
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
        IdentificationParameters identificationParameters = getIdentificationParameters();
        GenePreferences genePreferences = identificationParameters.getGenePreferences();
        GenePreferencesDialog genePreferencesDialog = new GenePreferencesDialog(this, genePreferences, identificationParameters.getSearchParameters(), false);
        if (!genePreferencesDialog.isCanceled()) {
            genePreferences = genePreferencesDialog.getGenePreferences();
            identificationParameters.setGenePreferences(genePreferences);
            if (allTabsJTabbedPane.getSelectedIndex() == GO_ANALYSIS_TAB_INDEX) {
                goPanel.updateMappings();
            } else {
                updateNeeded.put(GO_ANALYSIS_TAB_INDEX, true);
            }
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
     * Export to mzIdentML.
     *
     * @param evt
     */
    private void exportMzIdentMLMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMzIdentMLMenuItemActionPerformed
        // @TODO: check that all ptms are mapped to a cv term
        new MzIdentMLExportDialog(this, true);
    }//GEN-LAST:event_exportMzIdentMLMenuItemActionPerformed

    /**
     * Open the PeptideShaker example dataset.
     *
     * @param evt
     */
    private void openExampleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openExampleMenuItemActionPerformed
        openExampleFile();
    }//GEN-LAST:event_openExampleMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void highResAnnotationCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highResAnnotationCheckBoxMenuItemActionPerformed
        updateSpectrumAnnotations();
    }//GEN-LAST:event_highResAnnotationCheckBoxMenuItemActionPerformed

    /**
     * Opens the method section draft dialog.
     *
     * @param evt
     */
    private void methodsSectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_methodsSectionMenuItemActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new MethodsSectionDialog(this, true);
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_methodsSectionMenuItemActionPerformed

    /**
     * Open the PrivacySettingsDialog.
     *
     * @param evt
     */
    private void privacyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_privacyMenuItemActionPerformed
        new PrivacySettingsDialog(this, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
    }//GEN-LAST:event_privacyMenuItemActionPerformed

    /**
     * Show the tips.
     *
     * @param evt
     */
    private void tipsButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tipsButtonMouseReleased
        NotesDialog notesDialog = new NotesDialog(this, false, currentTips);
        notesDialog.setLocation(PeptideShakerGUI.this.getWidth() - notesDialog.getWidth() - 25 + PeptideShakerGUI.this.getX(),
                PeptideShakerGUI.this.getHeight() - notesDialog.getHeight() - 25 + PeptideShakerGUI.this.getY());
        notesDialog.setVisible(true);
        currentTips = new ArrayList<String>();
        updateTipsNotificationCounter();
    }//GEN-LAST:event_tipsButtonMouseReleased

    /**
     * Open the Edit Paths dialog.
     *
     * @param evt
     */
    private void configurationFilesSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configurationFilesSettingsActionPerformed
        editPathSettings(null);
    }//GEN-LAST:event_configurationFilesSettingsActionPerformed

    /**
     * Open the ValidationQCPreferencesDialog.
     *
     * @param evt
     */
    private void validationQcMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validationQcMenuItemActionPerformed

        final IdMatchValidationPreferences idValidationPreferences = getIdentificationParameters().getIdValidationPreferences();
        final ValidationQCPreferences validationQCPreferences = idValidationPreferences.getValidationQCPreferences();
        ValidationQCPreferencesDialog validationQCPreferencesDialog = new ValidationQCPreferencesDialog(this, this, validationQCPreferences, true);

        if (!validationQCPreferencesDialog.isCanceled()) {

            ValidationQCPreferences newPreferences = validationQCPreferencesDialog.getValidationQCPreferences();

            if (!newPreferences.isSameAs(validationQCPreferences)) {

                idValidationPreferences.setValidationQCPreferences(newPreferences);

                // Update the assumptions QC filters
                for (Filter filter : newPreferences.getPsmFilters()) {
                    PsmFilter psmFilter = (PsmFilter) filter;
                    AssumptionFilter assumptionFilter = psmFilter.getAssumptionFilter();
                    assumptionFilter.clear();
                    for (String itemName : psmFilter.getItemsNames()) {
                        assumptionFilter.setFilterItem(itemName, psmFilter.getComparatorForItem(itemName), psmFilter.getValue(itemName));
                    }
                }

                progressDialog = new ProgressDialogX(PeptideShakerGUI.this,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
                progressDialog.setTitle("Validating. Please Wait...");
                progressDialog.setPrimaryProgressCounterIndeterminate(false);

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            progressDialog.setVisible(true);
                        } catch (IndexOutOfBoundsException e) {
                            // ignore
                        }
                    }
                }, "ProgressDialog").start();

                new Thread("RecalculateThread") {
                    @Override
                    public void run() {

                        PeptideShakerGUI peptideShakerGUI = PeptideShakerGUI.this;

                        try {
                            PSMaps pSMaps = new PSMaps();
                            pSMaps = (PSMaps) peptideShakerGUI.getIdentification().getUrParam(pSMaps);

                            MatchesValidator matchesValidator = new MatchesValidator(pSMaps.getPsmSpecificMap(), pSMaps.getPeptideSpecificMap(), pSMaps.getProteinMap());
                            matchesValidator.validateIdentifications(peptideShakerGUI.getIdentification(), peptideShakerGUI.getMetrics(), peptideShakerGUI.getGeneMaps(), pSMaps.getInputMap(), progressDialog, exceptionHandler,
                                    peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getIdentificationParameters(),
                                    peptideShakerGUI.getSpectrumCountingPreferences(), peptideShakerGUI.getProcessingPreferences());

                            progressDialog.setPrimaryProgressCounterIndeterminate(true);

                            if (!progressDialog.isRunCanceled()) {
                                // update the other tabs
                                peptideShakerGUI.getMetrics().setnValidatedProteins(-1);
                                peptideShakerGUI.getMetrics().setnConfidentProteins(-1);
                                peptideShakerGUI.setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, false);
                                peptideShakerGUI.setUpdated(PeptideShakerGUI.PROTEIN_FRACTIONS_TAB_INDEX, false);
                                peptideShakerGUI.setUpdated(PeptideShakerGUI.STRUCTURES_TAB_INDEX, false);
                                peptideShakerGUI.setUpdated(PeptideShakerGUI.MODIFICATIONS_TAB_INDEX, false);
                                peptideShakerGUI.setUpdated(PeptideShakerGUI.QC_PLOTS_TAB_INDEX, false);
                                peptideShakerGUI.setUpdated(PeptideShakerGUI.SPECTRUM_ID_TAB_INDEX, false);
                                peptideShakerGUI.setDataSaved(false);
                            } else {
                                idValidationPreferences.setValidationQCPreferences(validationQCPreferences);
                            }
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                        }

                        progressDialog.setRunFinished();

                        PeptideShakerGUI.this.repaintPanels();
                    }
                }.start();
            }
        }
    }//GEN-LAST:event_validationQcMenuItemActionPerformed

    /**
     * Change the PSM sort order.
     *
     * @param evt
     */
    private void psmSortScoreRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmSortScoreRadioButtonMenuItemActionPerformed
        utilitiesUserPreferences.setSortPsmsOnRt(!psmSortScoreRadioButtonMenuItem.isSelected());
        overviewPanel.updatePsmOrder();
    }//GEN-LAST:event_psmSortScoreRadioButtonMenuItemActionPerformed

    /**
     * Change the PSM sort order.
     *
     * @param evt
     */
    private void psmSortRtRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmSortRtRadioButtonMenuItemActionPerformed
        psmSortScoreRadioButtonMenuItemActionPerformed(null);
    }//GEN-LAST:event_psmSortRtRadioButtonMenuItemActionPerformed

    /**
     * Reset the annotation to the default annotation.
     *
     * @param evt
     */
    private void resetAnnotationMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_resetAnnotationMenuSelected
        defaultAnnotationCheckBoxMenuItem.setSelected(true);
        defaultAnnotationCheckBoxMenuItemActionPerformed(null);
    }//GEN-LAST:event_resetAnnotationMenuSelected

    /**
     * Open the ProcessingPreferencesDialog.
     *
     * @param evt
     */
    private void processingMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processingMenuItemActionPerformed
        ProcessingPreferencesDialog processingPreferencesDialog = new ProcessingPreferencesDialog(this, processingPreferences, true);
        if (!processingPreferencesDialog.isCanceled()) {
            processingPreferences = processingPreferencesDialog.getProcessingPreferences();
        }
    }//GEN-LAST:event_processingMenuItemActionPerformed

    /**
     * Open the identification parameters overview dialog.
     *
     * @param evt
     */
    private void editIdSettingsFilesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editIdSettingsFilesMenuItemActionPerformed
        new IdentificationParametersOverviewDialog(this);
    }//GEN-LAST:event_editIdSettingsFilesMenuItemActionPerformed

    /**
     * @see #updateAnnotationPreferences()
     */
    private void relatedIonsCheckMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_relatedIonsCheckMenuActionPerformed
        deselectDefaultAnnotationMenuItem();
        updateSpectrumAnnotations();
    }//GEN-LAST:event_relatedIonsCheckMenuActionPerformed

    /**
     * Opens a dialog allowing the setting of paths.
     *
     * @param welcomeDialog reference to the Welcome dialog, can be null
     */
    public void editPathSettings(WelcomeDialog welcomeDialog) {
        try {
            HashMap<PathKey, String> pathSettings = new HashMap<PathKey, String>();
            for (PeptideShakerPathKey peptideShakerPathKey : PeptideShakerPathKey.values()) {
                pathSettings.put(peptideShakerPathKey, PeptideShakerPathPreferences.getPathPreference(peptideShakerPathKey));
            }
            for (UtilitiesPathPreferences.UtilitiesPathKey utilitiesPathKey : UtilitiesPathPreferences.UtilitiesPathKey.values()) {
                pathSettings.put(utilitiesPathKey, UtilitiesPathPreferences.getPathPreference(utilitiesPathKey));
            }
            PathSettingsDialog pathSettingsDialog = new PathSettingsDialog(this, "PeptideShaker", pathSettings);
            if (!pathSettingsDialog.isCanceled()) {
                HashMap<PathKey, String> newSettings = pathSettingsDialog.getKeyToPathMap();
                for (PathKey pathKey : pathSettings.keySet()) {
                    String oldPath = pathSettings.get(pathKey);
                    String newPath = newSettings.get(pathKey);
                    if (oldPath == null && newPath != null
                            || oldPath != null && newPath == null
                            || oldPath != null && newPath != null && !oldPath.equals(newPath)) {
                        PeptideShakerPathPreferences.setPathPreferences(pathKey, newPath);
                    }
                }
                // write path file preference
                File destinationFile = new File(PeptideShaker.getJarFilePath(), UtilitiesPathPreferences.configurationFileName);
                try {
                    PeptideShakerPathPreferences.writeConfigurationToFile(destinationFile);
                    if (welcomeDialog != null) {
                        welcomeDialog.setVisible(false);
                    }
                    restart();
                } catch (Exception e) {
                    catchException(e);
                }
            }
        } catch (Exception e) {
            catchException(e);
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
            getDisplayPreferences().setDefaultSelection(getIdentificationParameters().getSearchParameters().getPtmSettings());
            getDisplayFeaturesGenerator().setDisplayedPTMs(getDisplayPreferences().getDisplayedPtms());

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
            projectSettingsMenuItem.setEnabled(true);
            preferencesMenuItem.setEnabled(true);
            findJMenuItem.setEnabled(true);
            starHideJMenuItem.setEnabled(true);
            validationQcMenuItem.setEnabled(true);
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
            speciesJMenuItem.setEnabled(true);

            // show/hide the sliders
            spectrumSlidersCheckBoxMenuItem.setSelected(getUserPreferences().showSliders());

            projectExportMenu.setEnabled(true);
            exportPrideMenuItem.setEnabled(true);
            exportMzIdentMLMenuItem.setEnabled(true);
            exportProjectMenuItem.setEnabled(true);

            // disable the fractions tab if only one mgf file
            boolean fractions = getIdentification().getSpectrumFiles().size() > 1;
            allTabsJTabbedPane.setEnabledAt(PROTEIN_FRACTIONS_TAB_INDEX, fractions);
            fractionDetailsJMenuItem.setEnabled(fractions);

            // Disable the validation tab if no decoy was used
            boolean targetDecoy = sequenceFactory.concatenatedTargetDecoy();
            allTabsJTabbedPane.setEnabledAt(VALIDATION_TAB_INDEX, targetDecoy);
            validatedProteinsOnlyJCheckBoxMenuItem.setEnabled(targetDecoy);

        } catch (Exception e) {

            // return the peptide shaker icon to the standard version
            setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

            e.printStackTrace();
            catchException(e);
            JOptionPane.showMessageDialog(null, "A problem occurred when loading the data.\n"
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
    private javax.swing.JCheckBoxMenuItem bIonCheckBoxMenuItem;
    private javax.swing.JLayeredPane backgroundLayeredPane;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JCheckBoxMenuItem barsCheckBoxMenuItem;
    private javax.swing.JMenuItem bubblePlotJMenuItem;
    private javax.swing.JMenuItem bubbleScaleJMenuItem;
    private javax.swing.JCheckBoxMenuItem cIonCheckBoxMenuItem;
    private javax.swing.JMenu chargeMenu;
    private javax.swing.JMenuItem configurationFilesSettings;
    private javax.swing.ButtonGroup deNovoChargeButtonGroup;
    private javax.swing.JRadioButtonMenuItem deNovoChargeOneJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem deNovoChargeTwoJRadioButtonMenuItem;
    private javax.swing.JMenu deNovoMenu;
    private javax.swing.JCheckBoxMenuItem defaultAnnotationCheckBoxMenuItem;
    private javax.swing.JMenuItem editIdSettingsFilesMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitJMenuItem;
    private javax.swing.JMenu exportGraphicsMenu;
    private javax.swing.JMenuItem exportIntensityHistogramGraphicsJMenuItem;
    private javax.swing.JMenu exportJMenu;
    private javax.swing.JMenuItem exportMassErrorPlotGraphicsJMenuItem;
    private javax.swing.JMenuItem exportMzIdentMLMenuItem;
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
    private javax.swing.JCheckBoxMenuItem highResAnnotationCheckBoxMenuItem;
    private javax.swing.JMenuItem identificationFeaturesMenuItem;
    private javax.swing.JCheckBoxMenuItem immoniumIonsCheckMenu;
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
    private javax.swing.JPopupMenu.Separator jSeparator20;
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
    private javax.swing.JMenuItem methodsSectionMenuItem;
    private javax.swing.JRadioButtonMenuItem mzIonTableRadioButtonMenuItem;
    private javax.swing.JMenuItem newJMenuItem;
    private javax.swing.JButton newsButton;
    private javax.swing.JButton notesButton;
    private javax.swing.JMenuItem openExampleMenuItem;
    private javax.swing.JMenuItem openJMenuItem;
    private javax.swing.JMenu openRecentJMenu;
    private javax.swing.JMenu otherMenu;
    private javax.swing.JMenu overViewTabViewMenu;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JCheckBoxMenuItem peptidesAndPsmsJCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem precursorCheckMenu;
    private javax.swing.JMenuItem preferencesMenuItem;
    private javax.swing.JMenuItem privacyMenuItem;
    private javax.swing.JMenuItem processingMenuItem;
    private javax.swing.JMenu projectExportMenu;
    private javax.swing.JMenuItem projectPropertiesMenuItem;
    private javax.swing.JMenuItem projectSettingsMenuItem;
    private javax.swing.JPanel proteinFractionsJPanel;
    private javax.swing.JPanel proteinStructureJPanel;
    private javax.swing.JCheckBoxMenuItem proteinsJCheckBoxMenuItem;
    private javax.swing.ButtonGroup psmSortOrderButtonGroup;
    private javax.swing.JMenu psmSortOrderMenu;
    private javax.swing.JRadioButtonMenuItem psmSortRtRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem psmSortScoreRadioButtonMenuItem;
    private javax.swing.JPanel ptmJPanel;
    private javax.swing.JPanel qcJPanel;
    private javax.swing.JMenuItem quantifyMenuItem;
    private javax.swing.JCheckBoxMenuItem relatedIonsCheckMenu;
    private javax.swing.JCheckBoxMenuItem reporterIonsCheckMenu;
    private javax.swing.JMenuItem reporterPreferencesJMenuItem;
    private javax.swing.JMenu resetAnnotationMenu;
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
    private javax.swing.JMenuItem validationQcMenuItem;
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

        if (useLogFile && !PeptideShaker.getJarFilePath().equalsIgnoreCase(".")) {
            try {
                String path = PeptideShaker.getJarFilePath() + "/resources/PeptideShaker.log";

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
                                + "Please <a href=\"https://github.com/compomics/peptide-shaker/issues\">contact the developers</a>."),
                                "File Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                System.err.println(System.getProperty("line.separator") + System.getProperty("line.separator") + new Date()
                        + ": PeptideShaker version " + PeptideShaker.getVersion() + ".");
                System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
                System.err.println("Total amount of memory in the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
                System.err.println("Free memory: " + Runtime.getRuntime().freeMemory() + ".");
                System.err.println("Java version: " + System.getProperty("java.version") + ".");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null, "An error occurred when trying to create the PeptideShaker log file.",
                        "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the user preferences.
     *
     * @return the user preferences
     */
    public UserPreferences getUserPreferences() {
        return cpsParent.getUserPreferences();
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
     * @param utilitiesUserPreferences the utilities user preferences
     */
    public void setUtilitiesUserPreferences(UtilitiesUserPreferences utilitiesUserPreferences) {
        this.utilitiesUserPreferences = utilitiesUserPreferences;
    }

    /**
     * Set the default preferences.
     */
    public void setDefaultPreferences() {
        cpsParent.setDefaultPreferences();
        updateAnnotationMenu();
    }

    /**
     * Updates the ions used for fragment annotation.
     */
    public void updateAnnotationMenu() {

        IdentificationParameters identificationParameters = getIdentificationParameters();
        AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        if (searchParameters.getForwardIons().contains(PeptideFragmentIon.A_ION)) {
            forwardIonsDeNovoCheckBoxMenuItem.setText("a-ions");
        }
        if (searchParameters.getForwardIons().contains(PeptideFragmentIon.B_ION)) {
            forwardIonsDeNovoCheckBoxMenuItem.setText("b-ions");
        }
        if (searchParameters.getForwardIons().contains(PeptideFragmentIon.C_ION)) {
            forwardIonsDeNovoCheckBoxMenuItem.setText("c-ions");
        }

        forwardIonsDeNovoCheckBoxMenuItem.repaint();

        if (searchParameters.getRewindIons().contains(PeptideFragmentIon.X_ION)) {
            rewindIonsDeNovoCheckBoxMenuItem.setText("x-ions");
        }
        if (searchParameters.getRewindIons().contains(PeptideFragmentIon.Y_ION)) {
            rewindIonsDeNovoCheckBoxMenuItem.setText("y-ions");
        }
        if (searchParameters.getRewindIons().contains(PeptideFragmentIon.Z_ION)) {
            rewindIonsDeNovoCheckBoxMenuItem.setText("z-ions");
        }

        rewindIonsDeNovoCheckBoxMenuItem.repaint();

        if (annotationPreferences.getDeNovoCharge() == 1) {
            deNovoChargeOneJRadioButtonMenuItem.isSelected();
        } else {
            deNovoChargeTwoJRadioButtonMenuItem.isSelected();
        }
    }

    /**
     * Returns the spectrum annotator. Warning: should not be used in different
     * threads.
     *
     * @return the spectrum annotator
     */
    public PeptideSpectrumAnnotator getSpectrumAnnotator() {
        return spectrumAnnotator;
    }

    /**
     * Updates the annotations in the selected tab.
     */
    public void updateSpectrumAnnotations() {

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();
        IdentificationParameters identificationParameters = getIdentificationParameters();
        AnnotationSettings annotationPreferences = identificationParameters.getAnnotationPreferences();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

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
     * Returns the experiment.
     *
     * @return the experiment
     */
    public ProjectParameters getProjectParameters() {
        return cpsParent.getProjectParameters();
    }

    /**
     * Returns the identification displayed.
     *
     * @return the identification displayed
     */
    public Identification getIdentification() {
        return cpsParent.getIdentification();
    }
    
    public void setIdentification(Identification identification) {
        this.cpsParent.setIdentification(identification);
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
     * Return the filter preferences to use.
     *
     * @return the filter preferences to use
     */
    public FilterPreferences getFilterPreferences() {
        return cpsParent.getFilterPreferences();
    }

    /**
     * Return the display preferences to use.
     *
     * @return the display preferences to use
     */
    public DisplayPreferences getDisplayPreferences() {
        return cpsParent.getDisplayPreferences();
    }

    /**
     * Sets the GUI filter preferences to use.
     *
     * @param filterPreferences the GUI filter preferences to use
     */
    public void setFilterPreferences(FilterPreferences filterPreferences) {
        cpsParent.setFilterPreferences(filterPreferences);
    }

    /**
     * Sets the display preferences to use.
     *
     * @param displayPreferences the display preferences to use
     */
    public void setDisplayPreferences(DisplayPreferences displayPreferences) {
        cpsParent.setDisplayPreferences(displayPreferences);
    }

    /**
     * Returns the spectrum counting preferences.
     *
     * @return the spectrum counting preferences
     */
    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        return cpsParent.getSpectrumCountingPreferences();
    }

    /**
     * Sets new spectrum counting preferences.
     *
     * @param spectrumCountingPreferences new spectrum counting preferences
     */
    public void setSpectrumCountingPreferences(SpectrumCountingPreferences spectrumCountingPreferences) {
        cpsParent.setSpectrumCountingPreferences(spectrumCountingPreferences);
    }

    /**
     * Returns the identification parameters.
     *
     * @return the identification parameters
     */
    public IdentificationParameters getIdentificationParameters() {
        return cpsParent.getIdentificationParameters();
    }

    /**
     * Sets the identification parameters.
     *
     * @param identificationParameters the identification parameters
     */
    public void setIdentificationParameters(IdentificationParameters identificationParameters) {
        cpsParent.setIdentificationParameters(identificationParameters);
        setSelectedItems();
        backgroundPanel.revalidate();
        backgroundPanel.repaint();
        dataSaved = false;
    }

    /**
     * Returns information on the protocol used.
     *
     * @return information on the protocol used
     */
    public ShotgunProtocol getShotgunProtocol() {
        return cpsParent.getShotgunProtocol();
    }

    /**
     * Sets information on the protocol used.
     *
     * @param shotgunProtocol information on the protocol used
     */
    public void setShotgunProtocol(ShotgunProtocol shotgunProtocol) {
        cpsParent.setShotgunProtocol(shotgunProtocol);
    }

    /**
     * Returns the initial processing preferences.
     *
     * @return the initial processing preferences
     */
    public ProcessingPreferences getProcessingPreferences() {
        if (processingPreferences == null) {
            processingPreferences = new ProcessingPreferences();
        }
        return processingPreferences;
    }

    /**
     * Sets the initial processing preferences.
     *
     * @param processingPreferences the initial processing preferences
     */
    public void setProcessingPreferences(ProcessingPreferences processingPreferences) {
        this.processingPreferences = processingPreferences;
    }

    /**
     * Resets the PTM factory.
     */
    public void resetPtmFactory() {

        ptmFactory.reloadFactory();
        ptmFactory = PTMFactory.getInstance();
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
     * Get the not found sparklines color.
     *
     * @return the not found sparklineColor
     */
    public Color getSparklineColorNotFound() {
        return utilitiesUserPreferences.getSparklineColorNotFound();
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
    public void updateSelectionInCurrentTab() throws SQLException, IOException, ClassNotFoundException, InterruptedException {

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
     * @param updateGuiComponents true if the GUI components are to be updated
     */
    public void clearData(boolean clearDatabaseFolder, boolean updateGuiComponents) {

        // reset the preferences
        selectedProteinKey = NO_SELECTION;
        selectedPeptideKey = NO_SELECTION;
        selectedPsmKey = NO_SELECTION;

        cpsParent.setProjectDetails(null);
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

        try {
            spectrumFactory.clearFactory();
            spectrumFiles.clear();
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

        if (clearDatabaseFolder) {
            clearDatabaseFolder();
        }

        resetIdentificationFeaturesGenerator();

        if (updateGuiComponents) {
            // set up the tabs/panels
            scoresJCheckBoxMenuItem.setSelected(false);
            setUpPanels(true);

            // repaint the panels
            repaintPanels();

            // select the overview tab
            allTabsJTabbedPane.setSelectedIndex(OVER_VIEW_TAB_INDEX);
        }

        cpsParent.setCpsFile(null);
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
                cpsParent.setIdentification(null);
            } catch (Exception e) {
                databaseClosed = false;
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to close the database.", "Database Error", JOptionPane.WARNING_MESSAGE);
            }
        }

        // empty the matches folder
        if (databaseClosed) {

            File matchFolder = PeptideShaker.getMatchesFolder();

            if (matchFolder.exists()) {

                DerbyUtil.closeConnection();

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

        // reset enzymes, ptms and preferences
        resetPtmFactory();
        setDefaultPreferences();
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
                statsPanel = new ValidationPanel(this);
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
    public ValidationPanel getStatsPanel() {
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
     * an additional width of 2*margin pixels. <br> Note that this method
     * iterates all rows in the table to get the perfect width of the column!
     *
     * @param table the table
     * @param colIndex the colum index
     * @param margin the margin to add
     * @return the preferred width of the column
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
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while retrieving a match from the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while retrieving a match from the database
     * @throws java.lang.InterruptedException exception thrown whenever an error
     * occurred while retrieving a match from the database
     */
    public void updateMainMatch(String mainMatch, int proteinInferenceType) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        try {
            PSMaps psMaps = new PSMaps();
            psMaps = (PSMaps) getIdentification().getUrParam(psMaps);
            PsmPTMMap psmPTMMap = psMaps.getPsmPTMMap();
            PtmScorer ptmScorer = new PtmScorer(psmPTMMap);
            Identification identification = getIdentification();
            ProteinMatch proteinMatch = (ProteinMatch)identification.retrieveObject(selectedProteinKey);
            ptmScorer.scorePTMs(identification, proteinMatch, getIdentificationParameters(), false, null);
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
        if (!openingExistingProject) {
            this.dataSaved = dataSaved;
        }
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
     * @param links the links to open
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
    public FrameExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * Closes the frame by first checking if the project ought to be saved.
     */
    public void close() {

        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        if (!dataSaved && getProjectParameters() != null) {

            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getProjectParameters().getProjectUniqueName()+ "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                if (cpsParent.getCpsFile() != null && cpsParent.getCpsFile().exists()) {
                    saveProject(true, false);
                } else {
                    saveProjectAs(true, false);
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
                        cpsParent.saveUserPreferences();
                        TempFilesManager.deleteTempFolders();
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

                    // close the jvm
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

        if (!dataSaved && getProjectParameters() != null) {

            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getProjectParameters().getProjectUniqueName()+ "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
            } else if (value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION) {
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
                    cpsParent.saveUserPreferences();
                    PeptideShakerGUI.this.clearData(true, false);
                    TempFilesManager.deleteTempFolders();
                    UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
                } catch (Exception e) {
                    e.printStackTrace();
                    catchException(e);
                }
                progressDialog.setRunFinished();
                PeptideShakerGUI.this.dispose();

                // @TODO: pass the current project to the new instance of PeptideShaker.
                new PeptideShakerWrapper();
                System.exit(0); // have to close the current java process (as a new one is started on the line above)
            }
        }.start();
    }

    /**
     * Update the annotation menu bar with the current annotation preferences.
     *
     * @param precursorCharge the precursor charges
     * @param modificationMatches the modifications
     */
    public void updateAnnotationMenus(int precursorCharge, ArrayList<ModificationMatch> modificationMatches) {

        aIonCheckBoxMenuItem.setSelected(false);
        bIonCheckBoxMenuItem.setSelected(false);
        cIonCheckBoxMenuItem.setSelected(false);
        xIonCheckBoxMenuItem.setSelected(false);
        yIonCheckBoxMenuItem.setSelected(false);
        zIonCheckBoxMenuItem.setSelected(false);
        precursorCheckMenu.setSelected(false);
        immoniumIonsCheckMenu.setSelected(false);
        relatedIonsCheckMenu.setSelected(false);
        reporterIonsCheckMenu.setSelected(false);

        for (Ion.IonType ionType : specificAnnotationPreferences.getIonTypes().keySet()) {
            if (ionType == IonType.IMMONIUM_ION) {
                immoniumIonsCheckMenu.setSelected(true);
            } else if (ionType == IonType.RELATED_ION) {
                relatedIonsCheckMenu.setSelected(true);
            } else if (ionType == IonType.PRECURSOR_ION) {
                precursorCheckMenu.setSelected(true);
            } else if (ionType == IonType.REPORTER_ION) {
                reporterIonsCheckMenu.setSelected(true);
            } else if (ionType == IonType.PEPTIDE_FRAGMENT_ION) {
                for (int subtype : specificAnnotationPreferences.getIonTypes().get(ionType)) {
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

        for (JCheckBoxMenuItem lossMenuItem : lossMenus.values()) {
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
        for (ModificationMatch modMatch : modificationMatches) {
            PTM ptm = ptmFactory.getPTM(modMatch.getTheoreticPtm());
            for (NeutralLoss neutralLoss : ptm.getNeutralLosses()) {
                if (!neutralLosses.containsKey(neutralLoss.name)) {
                    neutralLosses.put(neutralLoss.name, neutralLoss);
                }
            }
        }

        ArrayList<String> names = new ArrayList<String>(neutralLosses.keySet());
        Collections.sort(names);

        if (neutralLosses.isEmpty()) {
            lossMenu.setVisible(false);
            lossSplitter.setVisible(false);
        } else {

            for (int i = 0; i < names.size(); i++) {

                String neutralLossName = names.get(i);
                NeutralLoss neutralLoss = neutralLosses.get(neutralLossName);

                boolean selected = false;
                for (String specificNeutralLossName : specificAnnotationPreferences.getNeutralLossesMap().getAccountedNeutralLosses()) {
                    NeutralLoss specificNeutralLoss = NeutralLoss.getNeutralLoss(specificNeutralLossName);
                    if (neutralLoss.isSameAs(specificNeutralLoss)) {
                        selected = true;
                        break;
                    }
                }

                JCheckBoxMenuItem lossMenuItem = new JCheckBoxMenuItem(neutralLossName);
                lossMenuItem.setSelected(selected);
                lossMenuItem.setEnabled(!specificAnnotationPreferences.isNeutralLossesAuto());
                lossMenuItem.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        deselectDefaultAnnotationMenuItem();
                        updateSpectrumAnnotations();
                    }
                });
                lossMenus.put(neutralLosses.get(neutralLossName), lossMenuItem);
                lossMenu.add(lossMenuItem, i);
            }
            adaptCheckBoxMenuItem.setSelected(specificAnnotationPreferences.isNeutralLossesAuto());
        }

        chargeMenus.clear();
        chargeMenu.removeAll();

        if (precursorCharge == 1) {
            precursorCharge = 2;
        }

        for (Integer charge = 1; charge < precursorCharge; charge++) {

            final JCheckBoxMenuItem chargeMenuItem = new JCheckBoxMenuItem(charge + "+");

            chargeMenuItem.setSelected(specificAnnotationPreferences.getSelectedCharges().contains(charge));
            chargeMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    deselectDefaultAnnotationMenuItem();
                    updateSpectrumAnnotations();
                }
            });

            chargeMenus.put(charge, chargeMenuItem);
            chargeMenu.add(chargeMenuItem);
        }

        // General annotation settings
        AnnotationSettings annotationPreferences = getIdentificationParameters().getAnnotationPreferences();
        highResAnnotationCheckBoxMenuItem.setSelected(annotationPreferences.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz); //@TODO: change for a drop down menu
        allCheckBoxMenuItem.setSelected(annotationPreferences.showAllPeaks());

        // Display preferenecs
        DisplayPreferences displayPreferences = getDisplayPreferences();
        barsCheckBoxMenuItem.setSelected(displayPreferences.showBars());
        intensityIonTableRadioButtonMenuItem.setSelected(displayPreferences.useIntensityIonTable());
    }

    /**
     * Save the current annotation preferences selected in the annotation menus
     * in the specific annotation preferences.
     */
    public void updateAnnotationPreferences() {

        try {
            AnnotationSettings annotationPreferences = getIdentificationParameters().getAnnotationPreferences();

            specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(specificAnnotationPreferences.getSpectrumKey(),
                    specificAnnotationPreferences.getSpectrumIdentificationAssumption(), getIdentificationParameters().getSequenceMatchingPreferences(),
                    getIdentificationParameters().getPtmScoringPreferences().getSequenceMatchingPreferences());

            if (!defaultAnnotationCheckBoxMenuItem.isSelected()) {

                specificAnnotationPreferences.clearIonTypes();
                if (aIonCheckBoxMenuItem.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.A_ION);
                    specificAnnotationPreferences.addIonType(IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.A_ION);
                }
                if (bIonCheckBoxMenuItem.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
                    specificAnnotationPreferences.addIonType(IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.B_ION);
                }
                if (cIonCheckBoxMenuItem.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.C_ION);
                    specificAnnotationPreferences.addIonType(IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.C_ION);
                }
                if (xIonCheckBoxMenuItem.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.X_ION);
                    specificAnnotationPreferences.addIonType(IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.X_ION);
                }
                if (yIonCheckBoxMenuItem.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
                    specificAnnotationPreferences.addIonType(IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
                }
                if (zIonCheckBoxMenuItem.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Z_ION);
                    specificAnnotationPreferences.addIonType(IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.Z_ION);
                }
                if (precursorCheckMenu.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.PRECURSOR_ION);
                }
                if (immoniumIonsCheckMenu.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.IMMONIUM_ION);
                }
                if (relatedIonsCheckMenu.isSelected()) {
                    specificAnnotationPreferences.addIonType(IonType.RELATED_ION);
                }
                if (reporterIonsCheckMenu.isSelected()) {
                    ArrayList<Integer> reporterIons = new ArrayList<Integer>(IonFactory.getReporterIons(getIdentificationParameters().getSearchParameters().getPtmSettings()));
                    for (int subtype : reporterIons) {
                        specificAnnotationPreferences.addIonType(IonType.REPORTER_ION, subtype);
                    }
                }

                if (!adaptCheckBoxMenuItem.isSelected()) {
                    specificAnnotationPreferences.setNeutralLossesAuto(false);
                    specificAnnotationPreferences.clearNeutralLosses();
                    for (NeutralLoss neutralLoss : lossMenus.keySet()) {
                        if (lossMenus.get(neutralLoss).isSelected()) {
                            specificAnnotationPreferences.addNeutralLoss(neutralLoss);
                        }
                    }
                }

                specificAnnotationPreferences.clearCharges();
                for (int charge : chargeMenus.keySet()) {
                    if (chargeMenus.get(charge).isSelected()) {
                        specificAnnotationPreferences.addSelectedCharge(charge);
                    }
                }

            } else {
                selectDefaultAnnotationMenuItem();
            }

            // The following preferences are kept for all spectra
            SpectrumAnnotator.TiesResolution tiesResolution = highResAnnotationCheckBoxMenuItem.isSelected() ? SpectrumAnnotator.TiesResolution.mostAccurateMz : SpectrumAnnotator.TiesResolution.mostIntense;
            annotationPreferences.setTiesResolution(tiesResolution); //@TODO: replace by a drop down menu
            annotationPreferences.setShowAllPeaks(allCheckBoxMenuItem.isSelected());
            annotationPreferences.setShowForwardIonDeNovoTags(forwardIonsDeNovoCheckBoxMenuItem.isSelected());
            annotationPreferences.setShowRewindIonDeNovoTags(rewindIonsDeNovoCheckBoxMenuItem.isSelected());

            // Display preferenecs
            DisplayPreferences displayPreferences = getDisplayPreferences();
            barsCheckBoxMenuItem.setSelected(displayPreferences.showBars());
            intensityIonTableRadioButtonMenuItem.setSelected(displayPreferences.useIntensityIonTable());

            if (deNovoChargeOneJRadioButtonMenuItem.isSelected()) {
                annotationPreferences.setDeNovoCharge(1);
            } else {
                annotationPreferences.setDeNovoCharge(2);
            }

        } catch (Exception e) {
            catchException(e);
        }
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
     * @param showPtmPlotOptions if true, the PTM plot option is shown
     * @param showSingleSpectrumExportOptions if true, the single spectrum
     * export options are shown
     */
    public void updateAnnotationMenuBarVisableOptions(boolean showSpectrumOptions, boolean showBubblePlotOptions,
            boolean showIonTableOptions, boolean showPtmPlotOptions, boolean showSingleSpectrumExportOptions) {

        // @TODO: replace boolean variables with an Enum
        allCheckBoxMenuItem.setVisible(showSpectrumOptions);
        exportSpectrumGraphicsJMenuItem.setVisible(showSpectrumOptions);
        exportSpectrumMenu.setVisible(showSpectrumOptions);
        highResAnnotationCheckBoxMenuItem.setVisible(showSpectrumOptions || showBubblePlotOptions);

        // @TODO: update the other tabs support the spectrum sub plots
        exportSpectrumAndPlotsGraphicsJMenuItem.setVisible(showSingleSpectrumExportOptions && allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportSpectrumGraphicsSeparator.setVisible(showSingleSpectrumExportOptions && allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportSequenceFragmentationGraphicsJMenuItem.setVisible(showSingleSpectrumExportOptions && allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportIntensityHistogramGraphicsJMenuItem.setVisible(showSingleSpectrumExportOptions && allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportMassErrorPlotGraphicsJMenuItem.setVisible(showSingleSpectrumExportOptions && allTabsJTabbedPane.getSelectedIndex() == OVER_VIEW_TAB_INDEX);
        exportSpectrumValuesJMenuItem.setVisible(showSingleSpectrumExportOptions);

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
        ArrayList<String> paths = cpsParent.getUserPreferences().getRecentProjects();
        int counter = 1;

        for (String line : paths) {
            JMenuItem menuItem = new JMenuItem(counter++ + ": " + line);

            final String filePath = line;
            final PeptideShakerGUI temp = this;

            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {

                    boolean open = true;

                    if (!dataSaved && getProjectParameters() != null) {
                        int value = JOptionPane.showConfirmDialog(temp,
                                "Do you want to save the changes to " + getProjectParameters().getProjectUniqueName()+ "?",
                                "Unsaved Changes",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE);

                        if (value == JOptionPane.YES_OPTION) {
                            saveMenuItemActionPerformed(null);
                            open = false;
                        } else if (value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION) {
                            open = false;
                        }
                    }

                    if (open) {
                        if (!new File(filePath).exists()) {
                            JOptionPane.showMessageDialog(null, "File not found!", "File Error", JOptionPane.ERROR_MESSAGE);
                            temp.getUserPreferences().removeRecentProject(filePath);
                        } else {
                            clearData(true, true);
                            clearPreferences();
                            importPeptideShakerFile(new File(filePath));
                            cpsParent.getUserPreferences().addRecentProject(filePath);
                            lastSelectedFolder.setLastSelectedFolder(new File(filePath).getAbsolutePath());
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
        ArrayList<String> paths = cpsParent.getUserPreferences().getRecentProjects();
        int counter = 1;

        for (String line : paths) {
            JMenuItem menuItem = new JMenuItem(counter++ + ": " + line);

            final String filePath = line;
            final PeptideShakerGUI temp = this;

            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {

                    File projectFile = new File(filePath);

                    if (!projectFile.exists()) {
                        JOptionPane.showMessageDialog(null, "File not found!", "File Error", JOptionPane.ERROR_MESSAGE);
                        temp.getUserPreferences().removeRecentProject(filePath);
                    } else {
                        tempWelcomeDialog.setVisible(false);
                        tempWelcomeDialog.dispose();
                        setVisible(true);

                        clearData(true, true);
                        clearPreferences();

                        if (filePath.endsWith(".zip")) {
                            importPeptideShakerZipFile(projectFile);
                        } else {
                            importPeptideShakerFile(new File(filePath));
                        }
                        cpsParent.getUserPreferences().addRecentProject(filePath);
                        lastSelectedFolder.setLastSelectedFolder(filePath);
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
        return cpsParent.getProjectDetails();
    }

    /**
     * Sets the project details.
     *
     * @param projectDetails the project details
     */
    public void setProjectDetails(ProjectDetails projectDetails) {
        cpsParent.setProjectDetails(projectDetails);
    }

    /**
     * Imports a PeptideShaker zip file from a URL.
     *
     * @param zipURL the PeptideShaker zip file to import
     * @param destinationFolder the folder to download and unzip the project in
     */
    public void importPeptideShakerZipFromURL(final String zipURL, final String destinationFolder) {

        // @TODO: add a default url download folder to the temp folder structure
//        String newName = "zipped_url";
//        String parentFolder = PsZipUtils.getUnzipParentFolder();
//        if (parentFolder == null) {
//            parentFolder = zipFile.getParent();
//        }
//        File parentFolderFile = new File(parentFolder, PsZipUtils.getUnzipSubFolder());
//        final File destinationFolder = new File(parentFolderFile, newName);
//        destinationFolder.mkdir();
//        TempFilesManager.registerTempFolder(parentFolderFile);
//        
        final PeptideShakerGUI peptideShakerGUI = this; // needed due to threading issues

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setWaitingText("Downloading PeptideShaker Project. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("DownloadingUrlThread") {
            @Override
            public void run() {

                try {
                    URL url = new URL(zipURL);
                    String path = url.getPath();
                    if (!path.endsWith(".zip")) {
                        path += ".zip";
                    }
                    File tmpFile = new File(destinationFolder, new File(path).getName());
                    Util.saveUrl(tmpFile, zipURL, Util.getFileSize(url), null, null, progressDialog);
                    progressDialog.setRunFinished();
                    importPeptideShakerZipFile(tmpFile);
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            e.getMessage(),
                            "Download Error", JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Imports informations from a PeptideShaker zip file.
     *
     * @param zipFile the PeptideShaker zip file to import
     */
    public void importPeptideShakerZipFile(final File zipFile) {

        String newName = PsZipUtils.getTempFolderName(zipFile.getName());
        String parentFolder = PsZipUtils.getUnzipParentFolder();
        if (parentFolder == null) {
            parentFolder = zipFile.getParent();
        }
        File parentFolderFile = new File(parentFolder, PsZipUtils.getUnzipSubFolder());
        final File destinationFolder = new File(parentFolderFile, newName);
        destinationFolder.mkdir();
        TempFilesManager.registerTempFolder(parentFolderFile);

        final PeptideShakerGUI peptideShakerGUI = this; // needed due to threading issues

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setWaitingText("Unzipping " + zipFile.getName() + ". Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("UnzippingThread") {
            @Override
            public void run() {

                try {
                    ZipUtils.unzip(zipFile, destinationFolder, progressDialog);
                    progressDialog.setSecondaryProgressCounterIndeterminate(true);
                    if (!progressDialog.isRunCanceled()) {
                        for (File file : destinationFolder.listFiles()) {
                            if (file.getName().toLowerCase().endsWith(".cpsx")) {
                                exceptionHandler.setIgnoreExceptions(true);
                                clearData(true, true);
                                exceptionHandler.setIgnoreExceptions(false);
                                clearPreferences();
                                getUserPreferences().addRecentProject(zipFile);
                                updateRecentProjectsList();
                                progressDialog.setRunFinished();
                                importPeptideShakerFile(file);
                                lastSelectedFolder.setLastSelectedFolder(file.getAbsolutePath());
                                return;
                            }
                        }
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                "No PeptideShaker project was found in the zip file.",
                                "No PeptideShaker Project Found", JOptionPane.WARNING_MESSAGE);
                    }
                    progressDialog.setRunFinished();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            e.getMessage(),
                            "Unzip Error", JOptionPane.WARNING_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Imports informations from a PeptideShaker file.
     *
     * @param aPsFile the PeptideShaker file to import
     */
    public void importPeptideShakerFile(File aPsFile) {

        cpsParent.setCpsFile(aPsFile);

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
                    resetPtmFactory();
                    setDefaultPreferences();
                    setCurentNotes(new ArrayList<String>());
                    updateNotesNotificationCounter();
                    openingExistingProject = true;

                    cpsParent.loadCpsFile(PeptideShaker.getMatchesFolder(), progressDialog);

                    // load project specific PTMs
                    String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
                    if (error != null) {
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                error,
                                "PTM Definition Changed", JOptionPane.WARNING_MESSAGE);
                    }

                    // resets the display features generator according to the new project
                    resetDisplayFeaturesGenerator();

                    if (progressDialog.isRunCanceled()) {
                        clearData(true, true);
                        clearPreferences();
                        progressDialog.setRunFinished();
                        openingExistingProject = false;
                        return;
                    }

                    progressDialog.setTitle("Loading FASTA File. Please Wait...");

                    boolean fileFound;
                    try {
                        fileFound = cpsParent.loadFastaFile(new File(getLastSelectedFolder().getLastSelectedFolder()), progressDialog);
                    } catch (Exception e) {
                        fileFound = false;
                    }

                    if (!fileFound && !locateFastaFileManually()) {
                        File fastaFile = getIdentificationParameters().getProteinInferencePreferences().getProteinSequenceDatabase();
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                "An error occurred while reading:\n" + fastaFile.getAbsolutePath() + "."
                                + "\n\nFile not found.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                        clearData(true, true);
                        clearPreferences();
                        progressDialog.setRunFinished();
                        openingExistingProject = false;
                        return;
                    }

                    if (progressDialog.isRunCanceled()) {
                        clearData(true, true);
                        clearPreferences();
                        progressDialog.setRunFinished();
                        openingExistingProject = false;
                        return;
                    }

                    progressDialog.setTitle("Loading Spectrum Files. Please Wait...");
                    progressDialog.resetPrimaryProgressCounter();
                    progressDialog.setMaxPrimaryProgressCounter(getIdentification().getSpectrumFiles().size() + 1);
                    progressDialog.increasePrimaryProgressCounter();

                    int cpt = 0, total = getIdentification().getSpectrumFiles().size();
                    for (String spectrumFileName : getIdentification().getSpectrumFiles()) {

                        progressDialog.setTitle("Loading Spectrum Files (" + ++cpt + " of " + total + "). Please Wait...");
                        progressDialog.increasePrimaryProgressCounter();

                        boolean found;
                        try {
                            found = cpsParent.loadSpectrumFile(spectrumFileName, spectrumFiles, progressDialog);
                        } catch (Exception e) {
                            found = false;
                        }
                        if (!found) {
                            JOptionPane.showMessageDialog(peptideShakerGUI,
                                    "Spectrum file not found: \'" + spectrumFileName + "\'."
                                    + "\nPlease select the spectrum file or the folder containing it manually.",
                                    "File Not Found", JOptionPane.WARNING_MESSAGE);

                            JFileChooser fileChooser = new JFileChooser(getLastSelectedFolder().getLastSelectedFolder());
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
                                lastSelectedFolder.setLastSelectedFolder(mgfFolder.getAbsolutePath());
                                found = false;
                                for (File file : mgfFolder.listFiles()) {
                                    for (String spectrumFileName2 : getIdentification().getSpectrumFiles()) {
                                        try {
                                            String fileName = file.getName();
                                            if (spectrumFileName2.equals(fileName)) {
                                                getProjectDetails().addSpectrumFile(file);
                                                spectrumFactory.addSpectra(file, progressDialog);
                                                spectrumFiles.add(file);
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
                                    openingExistingProject = false;
                                    return;
                                }
                            }
                        }

                        if (progressDialog.isRunCanceled()) {
                            clearData(true, true);
                            clearPreferences();
                            progressDialog.setRunFinished();
                            openingExistingProject = false;
                            return;
                        }
                    }

                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setRunFinished();
                    peptideShakerGUI.displayResults();
                    allTabsJTabbedPaneStateChanged(null); // display the overview tab data
                    peptideShakerGUI.updateFrameTitle();
                    dataSaved = true;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            openingExistingProject = false;
                        }
                    });
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            "An error occurred while reading:\n" + cpsParent.getCpsFile() + ".\n\n"
                            + "It looks like another instance of PeptideShaker is still connected to the file.\n"
                            + "Please close all instances of PeptideShaker and try again.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                } catch (OutOfMemoryError error) {

                    System.err.println("Ran out of memory!");
                    System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + ".");
                    System.err.println("Memory used by the Java virtual machine: " + Runtime.getRuntime().totalMemory() + ".");
                    System.err.println("Free memory in the Java virtual machine: " + Runtime.getRuntime().freeMemory() + ".");

                    Runtime.getRuntime().gc();
                    JOptionPane.showMessageDialog(PeptideShakerGUI.this, JOptionEditorPane.getJOptionEditorPane(
                            "PeptideShaker used up all the available memory and had to be stopped.<br>"
                            + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                            + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                            + "Java Options). See also <a href=\"http://compomics.github.io/projects/compomics-utilities/wiki/javatroubleshooting.html\">JavaTroubleShooting</a>."),
                            "Out Of Memory", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    error.printStackTrace();
                } catch (OptionalDataException e) {
                    progressDialog.setRunFinished();
                    if (e.eof) {
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                "An error occurred while reading:\n" + cpsParent.getCpsFile() + ".\n\n"
                                + "The end of the file was reached unexpectedly. The file seems to be corrupt and cannot\n"
                                + "be opened. If the file is a copy, make sure that it is identical to the original file.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(peptideShakerGUI,
                                "An error occurred while reading:\n" + cpsParent.getCpsFile() + ".\n\n"
                                + "Please verify that the version used to create the file\n"
                                + "is compatible with your version of PeptideShaker.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                    }
                    e.printStackTrace();
                } catch (EOFException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            "An error occurred while reading:\n" + cpsParent.getCpsFile() + ".\n\n"
                            + "The end of the file was reached unexpectedly. The file seems to be corrupt and cannot\n"
                            + "be opened. If the file is a copy, make sure that it is identical to the original file.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            "An error occurred while reading:\n" + cpsParent.getCpsFile() + ".\n\n"
                            + "Please verify that the version used to create the file\n"
                            + "is compatible with your version of PeptideShaker.",
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

        File fastaFile = getIdentificationParameters().getProteinInferencePreferences().getProteinSequenceDatabase();
        JOptionPane.showMessageDialog(this,
                "FASTA file " + fastaFile.getAbsolutePath() + " was not found."
                + "\n\nPlease locate it manually.",
                "File Input Error", JOptionPane.WARNING_MESSAGE);

        LastSelectedFolder tempLastSelectedFolder = getLastSelectedFolder();
        JFileChooser fileChooser = new JFileChooser(tempLastSelectedFolder.getLastSelectedFolder()); // @TODO: replace by new getUserSelectedFile with multiple file endings option
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
            File selectedFastaFile = fileChooser.getSelectedFile();
            tempLastSelectedFolder.setLastSelectedFolder(selectedFastaFile.getAbsolutePath());
            getIdentificationParameters().getProteinInferencePreferences().setProteinSequenceDatabase(selectedFastaFile);
            dataSaved = false;
            return cpsParent.loadFastaFile(selectedFastaFile.getParentFile(), progressDialog);
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
     *
     * @throws java.io.IOException Exception thrown whenever an error occurred
     * while writing the mgf file
     */
    public void exportSelectedSpectraAsMgf() throws IOException {

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        ArrayList<String> selectedSpectra = new ArrayList<String>(1);

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            selectedSpectra = overviewPanel.getSelectedSpectrumKeys();
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            selectedSpectra.add(spectrumIdentificationPanel.getSelectedSpectrumKey());
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            selectedSpectra = ptmPanel.getSelectedPsmsKeys();
        }

        if (!selectedSpectra.isEmpty()) {

            File selectedFile = getUserSelectedFile("selected_spectra.mgf", ".mgf", "Mascot Generic Format (*.mgf)", "Save As...", false);

            if (selectedFile != null) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile));
                try {
                    for (String spectrumKey : selectedSpectra) {
                        MSnSpectrum spectrum = getSpectrum(spectrumKey);
                        if (spectrum == null) {
                            throw new IllegalArgumentException("Spectrum " + spectrumKey + " not found.");
                        }
                        bw.write(spectrum.asMgf());
                    }

                    JOptionPane.showMessageDialog(this, "Spectrum saved to " + selectedFile.getPath() + ".",
                            "File Saved", JOptionPane.INFORMATION_MESSAGE);
                } finally {
                    bw.close();
                }
            }
        }
    }

    /**
     * Export the current spectrum annotation.
     *
     * @throws SQLException exception thrown whenever an error occurred while
     * loading the object from the database
     * @throws IOException exception thrown whenever an error occurred while
     * reading or writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while casting the database input in the desired match class
     * @throws InterruptedException thrown whenever a threading issue occurred
     * while interacting with the database
     */
    public void exportAnnotatedSpectrum() throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        HashMap<String, ArrayList<SpectrumIdentificationAssumption>> selectedAssumptions = null;

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            selectedAssumptions = overviewPanel.getSelectedIdentificationAssumptions();
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            selectedAssumptions = spectrumIdentificationPanel.getSelectedIdentificationAssumptions();
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            selectedAssumptions = ptmPanel.getSelectedIdentificationAssumptions();
        }

        if (selectedAssumptions != null && !selectedAssumptions.isEmpty()) {

            File selectedFile = getUserSelectedFile("annotated_spectra.txt", ".txt", "Text (*.txt)", "Save As...", false);

            if (selectedFile != null) {

                AnnotationSettings annotationPreferences = getIdentificationParameters().getAnnotationPreferences();
                PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
                TagSpectrumAnnotator tagSpectrumAnnotator = new TagSpectrumAnnotator();

                String separator = "\t";
                BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile));

                try {
                    for (String spectrumKey : selectedAssumptions.keySet()) {
                        MSnSpectrum spectrum = getSpectrum(spectrumKey);
                        if (spectrum == null) {
                            throw new IllegalArgumentException("Spectrum " + spectrumKey + " not found.");
                        }
                        ArrayList<IonMatch> annotations = null;

                        ArrayList<SpectrumIdentificationAssumption> assumptions = selectedAssumptions.get(spectrumKey);
                        if (assumptions != null && !assumptions.isEmpty()) {
                            for (SpectrumIdentificationAssumption assumption : assumptions) {
                                String identifier;
                                if (assumption instanceof PeptideAssumption) {
                                    PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                                    Peptide peptide = peptideAssumption.getPeptide();
                                    SpecificAnnotationSettings exportAnnotationPreferences = new SpecificAnnotationSettings(spectrumKey, peptideAssumption);
                                    exportAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(exportAnnotationPreferences.getSpectrumKey(), exportAnnotationPreferences.getSpectrumIdentificationAssumption(), getIdentificationParameters().getSequenceMatchingPreferences(), getIdentificationParameters().getPtmScoringPreferences().getSequenceMatchingPreferences());
                                    annotations = peptideSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, exportAnnotationPreferences, spectrum, peptide);
                                    identifier = peptide.getSequenceWithLowerCasePtms();
                                } else if (assumption instanceof TagAssumption) {
                                    TagAssumption tagAssumption = (TagAssumption) assumption;
                                    Tag tag = tagAssumption.getTag();
                                    SpecificAnnotationSettings exportAnnotationPreferences = new SpecificAnnotationSettings(spectrumKey, tagAssumption);
                                    exportAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(exportAnnotationPreferences.getSpectrumKey(), exportAnnotationPreferences.getSpectrumIdentificationAssumption(), getIdentificationParameters().getSequenceMatchingPreferences(), getIdentificationParameters().getPtmScoringPreferences().getSequenceMatchingPreferences());
                                    annotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationPreferences, exportAnnotationPreferences, spectrum, tag);
                                    identifier = tag.asSequence(); //@TODO: add PTMs?
                                } else {
                                    throw new UnsupportedOperationException("Spectrum annotation not implemented for identification assumption of type " + assumption.getClass() + ".");
                                }

                                HashMap<Double, ArrayList<IonMatch>> annotationMap = new HashMap<Double, ArrayList<IonMatch>>();
                                if (annotations != null) {
                                    for (IonMatch ionMatch : annotations) {
                                        Double mz = ionMatch.peak.mz;
                                        ArrayList<IonMatch> matchesAtMz = annotationMap.get(mz);
                                        if (matchesAtMz == null) {
                                            matchesAtMz = new ArrayList<IonMatch>(1);
                                            annotationMap.put(mz, matchesAtMz);
                                        }
                                        matchesAtMz.add(ionMatch);
                                    }
                                }

                                bw.write("File: " + Spectrum.getSpectrumFile(spectrumKey) + separator + "Spectrum: " + Spectrum.getSpectrumTitle(spectrumKey) + separator + "Spectrum Identification Assumption: " + identifier);
                                bw.newLine();
                                bw.write("m/z" + separator + "Intensity" + separator + "Ion" + separator + "Theoretic m/z" + separator + "Absolute Error");
                                bw.newLine();
                                HashMap<Double, Peak> peakMap = spectrum.getPeakMap();
                                for (Double mz : spectrum.getOrderedMzValues()) {
                                    Peak peak = peakMap.get(mz);
                                    ArrayList<IonMatch> matches = annotationMap.get(mz);
                                    if (matches != null) {
                                        for (IonMatch ionMatch : matches) {
                                            bw.write(mz + separator + peak.intensity + separator + ionMatch.getPeakAnnotation() + separator + ionMatch.ion.getTheoreticMz(ionMatch.charge) + separator + ionMatch.getAbsoluteError());
                                            bw.newLine();
                                        }
                                    } else {
                                        bw.write(mz + separator + peak.intensity + separator + separator + separator);
                                        bw.newLine();
                                    }
                                }
                                bw.newLine();
                            }
                        } else {
                            bw.write("File: " + Spectrum.getSpectrumFile(spectrumKey) + separator + "Spectrum: " + Spectrum.getSpectrumTitle(spectrumKey));
                            bw.newLine();
                            bw.write("m/z" + separator + "Intensity" + separator + "Ion" + separator + "Theoretic m/z" + separator + "Absolute Error");
                            bw.newLine();
                            HashMap<Double, Peak> peakMap = spectrum.getPeakMap();
                            for (Double mz : spectrum.getOrderedMzValues()) {
                                Peak peak = peakMap.get(mz);
                                bw.write(mz + separator + peak.intensity + separator + separator + separator);
                                bw.newLine();
                            }
                        }
                    }

                    JOptionPane.showMessageDialog(this, "Spectrum saved to " + selectedFile.getPath() + ".",
                            "File Saved", JOptionPane.INFORMATION_MESSAGE);
                } finally {
                    bw.close();
                }
            }
        }
    }

    /**
     * Export the current spectrum as a figure.
     */
    public void exportSpectrumAsFigure() {

        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, (Component) overviewPanel.getSpectrum(), lastSelectedFolder);
        } else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
            new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, (Component) spectrumIdentificationPanel.getSpectrum(), lastSelectedFolder);
        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
            new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, (Component) ptmPanel.getSpectrum(), lastSelectedFolder);
        }
    }

    /**
     * Export the current sequence fragmentation as a figure.
     */
    public void exportSequenceFragmentationAsFigure() {
        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, (Component) overviewPanel.getSequenceFragmentationPlot(), lastSelectedFolder);
        }
//        else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) spectrumIdentificationPanel.getSpectrum());
//        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) ptmPanel.getSpectrum());
//        }

        // @TODO: add export support for the other tabs
    }

    /**
     * Export the current intensity histogram as a figure.
     */
    public void exportIntensityHistogramAsFigure() {
        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {

            ChartPanel chartPanel = overviewPanel.getIntensityHistogramPlot().getChartPanel();
            ChartPanel tempChartPanel = new ChartPanel(chartPanel.getChart());
            tempChartPanel.setBounds(new Rectangle(chartPanel.getBounds().width * 5, chartPanel.getBounds().height * 5));

            new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, tempChartPanel, lastSelectedFolder);
        }
//        else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) spectrumIdentificationPanel.getSpectrum());
//        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) ptmPanel.getSpectrum());
//        }

        // @TODO: add export support for the other tabs
    }

    /**
     * Export the current mass error plot as a figure.
     */
    public void exportMassErrorPlotAsFigure() {
        int selectedTabIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedTabIndex == OVER_VIEW_TAB_INDEX) {
            if (overviewPanel.getMassErrorPlot() != null) {

                ChartPanel chartPanel = overviewPanel.getMassErrorPlot().getChartPanel();
                ChartPanel tempChartPanel = new ChartPanel(chartPanel.getChart());
                tempChartPanel.setBounds(new Rectangle(chartPanel.getBounds().width * 5, chartPanel.getBounds().height * 5));

                new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, tempChartPanel, lastSelectedFolder);
            } else {
                JOptionPane.showMessageDialog(this, "No m/z error plot to export!", "Export Error", JOptionPane.INFORMATION_MESSAGE);
            }
        }
//        else if (selectedTabIndex == SPECTRUM_ID_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) spectrumIdentificationPanel.getSpectrum());
//        } else if (selectedTabIndex == MODIFICATIONS_TAB_INDEX) {
//            new ExportGraphicsDialog(this, true, (Component) ptmPanel.getSpectrum());
//        }

        // @TODO: add export support for the other tabs
    }

    /**
     * Export the current bubble plot as a figure.
     */
    public void exportBubblePlotAsFigure() {

        int selectedIndex = allTabsJTabbedPane.getSelectedIndex();

        if (selectedIndex == OVER_VIEW_TAB_INDEX) {
            new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, (Component) overviewPanel.getBubblePlot(), lastSelectedFolder);
        } else if (selectedIndex == SPECTRUM_ID_TAB_INDEX) {
            new ExportGraphicsDialog(this, getNormalIcon(), getWaitingIcon(), true, (Component) spectrumIdentificationPanel.getBubblePlot(), lastSelectedFolder);
        }
    }

    /**
     * Update the display options for the overview tab.
     *
     * @param displayProteins if the proteins panel is to be displayed
     * @param displayPeptidesAndPsms if the peptides and PSMs panel is to be
     * displayed
     * @param displayCoverage if the protein coverage panel is to be displayed
     * @param displaySpectrum if the spectrum panel is to be displayed
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
     * Get the current delta masses for use when annotating the spectra.
     *
     * @return the current delta masses
     */
    public HashMap<Double, String> getCurrentMassDeltas() {

        HashMap<Double, String> knownMassDeltas = new HashMap<Double, String>();

        // add the monoisotopic amino acids masses
        knownMassDeltas.put(AminoAcid.A.getMonoisotopicMass(), "A");
        knownMassDeltas.put(AminoAcid.R.getMonoisotopicMass(), "R");
        knownMassDeltas.put(AminoAcid.N.getMonoisotopicMass(), "N");
        knownMassDeltas.put(AminoAcid.D.getMonoisotopicMass(), "D");
        knownMassDeltas.put(AminoAcid.C.getMonoisotopicMass(), "C");
        knownMassDeltas.put(AminoAcid.Q.getMonoisotopicMass(), "Q");
        knownMassDeltas.put(AminoAcid.E.getMonoisotopicMass(), "E");
        knownMassDeltas.put(AminoAcid.G.getMonoisotopicMass(), "G");
        knownMassDeltas.put(AminoAcid.H.getMonoisotopicMass(), "H");
        knownMassDeltas.put(AminoAcid.I.getMonoisotopicMass(), "I/L");
        knownMassDeltas.put(AminoAcid.K.getMonoisotopicMass(), "K");
        knownMassDeltas.put(AminoAcid.M.getMonoisotopicMass(), "M");
        knownMassDeltas.put(AminoAcid.F.getMonoisotopicMass(), "F");
        knownMassDeltas.put(AminoAcid.P.getMonoisotopicMass(), "P");
        knownMassDeltas.put(AminoAcid.S.getMonoisotopicMass(), "S");
        knownMassDeltas.put(AminoAcid.T.getMonoisotopicMass(), "T");
        knownMassDeltas.put(AminoAcid.W.getMonoisotopicMass(), "W");
        knownMassDeltas.put(AminoAcid.Y.getMonoisotopicMass(), "Y");
        knownMassDeltas.put(AminoAcid.V.getMonoisotopicMass(), "V");
        knownMassDeltas.put(AminoAcid.U.getMonoisotopicMass(), "U");
        knownMassDeltas.put(AminoAcid.O.getMonoisotopicMass(), "O");

        // add default neutral losses
//        knownMassDeltas.put(NeutralLoss.H2O.mass, "H2O");
//        knownMassDeltas.put(NeutralLoss.NH3.mass, "NH3");
//        knownMassDeltas.put(NeutralLoss.CH4OS.mass, "CH4OS");
//        knownMassDeltas.put(NeutralLoss.H3PO4.mass, "H3PO4");
//        knownMassDeltas.put(NeutralLoss.HPO3.mass, "HPO3");
//        knownMassDeltas.put(4d, "18O"); // @TODO: should this be added to neutral losses??
//        knownMassDeltas.put(44d, "PEG"); // @TODO: should this be added to neutral losses??
        // add the modifications
        SearchParameters searchParameters = getIdentificationParameters().getSearchParameters();
        PtmSettings modificationProfile = searchParameters.getPtmSettings();
        ArrayList<String> modificationList = modificationProfile.getAllModifications();
        Collections.sort(modificationList);

        // iterate the modifications list and add the non-terminal modifications
        for (String modification : modificationList) {
            PTM ptm = ptmFactory.getPTM(modification);

            if (ptm != null) {

                String shortName = ptm.getShortName();
                double mass = ptm.getMass();

                if (ptm.getType() == PTM.MODAA) {
                    AminoAcidPattern ptmPattern = ptm.getPattern();
                    for (Character aa : ptmPattern.getAminoAcidsAtTarget()) {
                        if (!knownMassDeltas.containsValue(aa + "<" + shortName + ">")) {
                            AminoAcid aminoAcid = AminoAcid.getAminoAcid(aa);
                            knownMassDeltas.put(mass + aminoAcid.getMonoisotopicMass(),
                                    aa + "<" + shortName + ">");
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
     * @param aExportToZipWhenDone if true, the project is also saved as a zip
     * file
     */
    public void saveProject(boolean aCloseWhenDone, boolean aExportToZipWhenDone) {

        // check if the project is the example project
        if (cpsParent.getCpsFile() != null && cpsParent.getCpsFile().equals(new File(PeptideShaker.getJarFilePath() + EXAMPLE_DATASET_PATH))) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Overwriting the example project is not possible.\n"
                    + "Please save to a different location.", "Example Project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (value == JOptionPane.OK_OPTION) {
                saveProjectAs(aCloseWhenDone, aExportToZipWhenDone);
            } else {
                // cancel the saving
            }

        } else if (cpsParent.getCpsFile() == null) {
            saveProjectAs(false, false);
        } else {

            final boolean closeWhenDone = aCloseWhenDone;
            final boolean exportToZipWhenDone = aExportToZipWhenDone;

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
                        cpsParent.saveProject(progressDialog, closeWhenDone);

                        try {
                            ptmFactory.saveFactory();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (!progressDialog.isRunCanceled()) {
                            progressDialog.setRunFinished();
                            cpsParent.getUserPreferences().addRecentProject(cpsParent.getCpsFile());
                            updateRecentProjectsList();

                            // save the peptide shaker report next to the cps file
                            String report = cpsParent.getExtendedProjectReport(null);

                            if (report != null) {
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
                                String fileName = "PeptideShaker Report " + cpsParent.getCpsFile().getName() + " " + df.format(new Date()) + ".html";
                                File psReportFile = new File(cpsParent.getCpsFile().getParentFile(), fileName);
                                FileWriter fw = new FileWriter(psReportFile);
                                fw.write(report);
                                fw.close();
                            }

                            if (closeWhenDone) {
                                closePeptideShaker();
                            } else {
                                JOptionPane.showMessageDialog(tempRef, "Project successfully saved.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                                dataSaved = true;
                                if (exportToZipWhenDone) {
                                    exportProjectAsZip();
                                }
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
        return cpsParent.getIdentificationFeaturesGenerator();
    }

    /**
     * Resets the feature generator.
     */
    public void resetIdentificationFeaturesGenerator() {
        cpsParent.resetIdentificationFeaturesGenerator();
    }

    /**
     * Sets the feature generator.
     *
     * @param identificationFeaturesGenerator the identification features
     * generator
     */
    public void setIdentificationFeaturesGenerator(IdentificationFeaturesGenerator identificationFeaturesGenerator) {
        cpsParent.setIdentificationFeaturesGenerator(identificationFeaturesGenerator);
    }

    /**
     * Resets the display features generator.
     */
    public void resetDisplayFeaturesGenerator() {
        SearchParameters searchParameters = getIdentificationParameters().getSearchParameters();
        displayFeaturesGenerator = new DisplayFeaturesGenerator(searchParameters.getPtmSettings(), exceptionHandler);
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
        return cpsParent.getMetrics();
    }

    /**
     * Sets the metrics saved while loading the files.
     *
     * @param metrics the metrics saved while loading the files
     */
    public void setMetrics(Metrics metrics) {
        cpsParent.setMetrics(metrics);
    }

    /**
     * Returns the gene maps.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {
        return cpsParent.getGeneMaps();
    }

    /**
     * Sets the gene maps.
     *
     * @param geneMaps the gene maps
     */
    public void setGeneMaps(GeneMaps geneMaps) {
        cpsParent.setGeneMaps(geneMaps);
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
     * Save the project to a new location.
     *
     * @param closeWhenDone if true, PeptideShaker closes when done saving
     * @param aExportToZipWhenDone if true, the project is also saved as a zip
     * file
     */
    public void saveProjectAs(boolean closeWhenDone, boolean aExportToZipWhenDone) {
        File selectedFile = getUserSelectedFile(cpsParent.getProjectParameters().getProjectUniqueName()+ ".psDB", ".psDB", "Peptide Shaker Database format (*.psDB)", "Save As...", false);
        cpsParent.setCpsFile(selectedFile);
        if (selectedFile != null) {
            saveProject(closeWhenDone, aExportToZipWhenDone);
        }
    }

    /**
     * Returns the file selected by the user, or null if no file was selected.
     *
     * @param aSuggestedFileName the suggested file name, can be null
     * @param aFileEnding the file type, e.g., .txt
     * @param aFileFormatDescription the file format description, e.g., (Mascot
     * Generic Format) *.mgf
     * @param aDialogTitle the title for the dialog
     * @param openDialog if true an open dialog is shown, false results in a
     * save dialog
     * @return the file selected by the user, or null if no file or folder was
     * selected
     */
    public File getUserSelectedFile(String aSuggestedFileName, String aFileEnding, String aFileFormatDescription, String aDialogTitle, boolean openDialog) {

        File selectedFile = Util.getUserSelectedFile(this, aFileEnding, aFileFormatDescription, aDialogTitle, lastSelectedFolder.getLastSelectedFolder(), aSuggestedFileName, openDialog);

        if (selectedFile != null) {
            if (selectedFile.isDirectory()) {
                lastSelectedFolder.setLastSelectedFolder(selectedFile.getAbsolutePath());
            } else {
                lastSelectedFolder.setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());
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

        String jarFilePath = PeptideShaker.getJarFilePath();

        if (!jarFilePath.equalsIgnoreCase(".")) {

            // remove the initial '/' at the start of the line
            if (jarFilePath.startsWith("\\") && !jarFilePath.startsWith("\\\\")) {
                jarFilePath = jarFilePath.substring(1);
            }

            String iconFileLocation = jarFilePath + "\\resources\\peptide-shaker.ico";
            String jarFileLocation = jarFilePath + "\\PeptideShaker-" + PeptideShaker.getVersion() + ".jar";

            try {
                JShellLink link = new JShellLink();
                link.setFolder(JShellLink.getDirectory("desktop"));
                link.setName("Peptide Shaker " + PeptideShaker.getVersion());
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
//                        notificationDialog.setLocation(PeptideShakerGUI.this.getWidth() - notificationDialog.getWidth() - 95 + PeptideShakerGUI.this.getX(),
//                                PeptideShakerGUI.this.getHeight() - 46 + PeptideShakerGUI.this.getY());
//                        SwingUtils.fadeInAndOut(notificationDialog);
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
                if (strLine.lastIndexOf("dir=\"ltr\" data-aria-label-part=\"0\"") != -1) {
                    String tweetId = strLine.substring(strLine.indexOf(">") + 1, strLine.indexOf("</p>") - 1);
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

    /**
     * Open the PeptideShaker example dataset.
     */
    public void openExampleFile() {

        boolean open = true;

        if (!dataSaved && getProjectParameters() != null) {
            int value = JOptionPane.showConfirmDialog(this,
                    "Do you want to save the changes to " + getProjectParameters().getProjectUniqueName()+ "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                saveMenuItemActionPerformed(null);
                open = false;
            } else if (value == JOptionPane.CANCEL_OPTION || value == JOptionPane.CLOSED_OPTION) {
                open = false;
            }
        }

        if (open) {

            String filePath = PeptideShaker.getJarFilePath() + EXAMPLE_DATASET_PATH;

            if (!new File(filePath).exists()) {
                JOptionPane.showMessageDialog(null, "File not found!", "File Error", JOptionPane.ERROR_MESSAGE);
            } else {
                clearData(true, true);
                clearPreferences();

                importPeptideShakerFile(new File(filePath));
                cpsParent.getUserPreferences().addRecentProject(filePath);
            }

            updateRecentProjectsList();
        }
    }

    /**
     * Update the filter settings field. (Interface method: not implemented in
     * this class as it is not needed.)
     *
     * @param text the text to set
     */
    public void updateFilterSettingsField(String text) {
        // interface method: not implemented in this class as it is not needed
    }

    /**
     * Export the project as a zip file.
     */
    public void exportProjectAsZip() {

        if (!dataSaved) {

            int option = JOptionPane.showConfirmDialog(this, "You first need to save the project.", "Unsaved Data", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                // save the data first
                if (cpsParent.getCpsFile() != null && cpsParent.getCpsFile().exists()) {
                    saveProject(false, true);
                } else {
                    saveProjectAs(false, true);
                }
            }

        } else if (cpsParent.getCpsFile() != null) {

            // select the output folder
            String suggestedFileName = cpsParent.getCpsFile().getName();
            suggestedFileName = suggestedFileName.substring(0, suggestedFileName.lastIndexOf(".")) + ".zip";
            File selectedFile = getUserSelectedFile(suggestedFileName, ".zip", "Compressed file format (*.zip)", "Export As Zip...", false);

            if (selectedFile != null) {

                final File zipFile = selectedFile;

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

                        File cpsFile = cpsParent.getCpsFile();
                        File fastaFile = PeptideShakerGUI.this.getIdentificationParameters().getProteinInferencePreferences().getProteinSequenceDatabase();
                        ArrayList<File> spectrumFiles = new ArrayList<File>();
                        for (String spectrumFileName : getIdentification().getSpectrumFiles()) {
                            File spectrumFile = getProjectDetails().getSpectrumFile(spectrumFileName);
                            spectrumFiles.add(spectrumFile);
                        }

                        try {
                            ProjectExport.exportProjectAsZip(zipFile, fastaFile, spectrumFiles, cpsFile, progressDialog);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            progressDialog.setRunFinished();
                            JOptionPane.showMessageDialog(PeptideShakerGUI.this, "Could not zip files.", "Zip Error", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        } catch (IOException e) {
                            e.printStackTrace();
                            progressDialog.setRunFinished();
                            JOptionPane.showMessageDialog(PeptideShakerGUI.this, "Could not zip files.", "Zip Error", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled) {
                            // get the size (in MB) of the zip file
                            final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
                            double sizeOfZippedFile = Util.roundDouble(((double) zipFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE), 2);
                            JOptionPane.showMessageDialog(PeptideShakerGUI.this, "Project zipped to \'" + zipFile.getAbsolutePath() + "\' (" + sizeOfZippedFile + " MB)",
                                    "Export Sucessful", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }.start();
            }
        }
    }

    /**
     * Returns the normal icon.
     *
     * @return the normal icon
     */
    public Image getNormalIcon() {
        return Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif"));
    }

    /**
     * Returns the waiting icon.
     *
     * @return the waiting icon
     */
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
            PeptideMatch peptideMatch = (PeptideMatch)getIdentification().retrieveObject(peptideKey);
            ArrayList<String> psmKeys;

            try {
                psmKeys = getIdentificationFeaturesGenerator().getSortedPsmKeys(peptideKey, utilitiesUserPreferences.getSortPsmsOnRt(), false);
            } catch (Exception e) {
                try {
                    // try without order
                    psmKeys = peptideMatch.getSpectrumMatchesKeys();
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
            ProteinMatch proteinMatch = (ProteinMatch)getIdentification().retrieveObject(proteinKey);
            ArrayList<String> peptideKeys;

            try {
                peptideKeys = getIdentificationFeaturesGenerator().getSortedPeptideKeys(proteinKey);
            } catch (Exception e) {
                try {
                    // try without order
                    peptideKeys = proteinMatch.getPeptideMatchesKeys();
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

    @Override
    public void notificationClicked(String notificationType) {
        if (notificationType.equalsIgnoreCase("note") || notificationType.equalsIgnoreCase("notes")) {
            notesButtonMouseReleased(null);
        } else if (notificationType.equalsIgnoreCase("tip") || notificationType.equalsIgnoreCase("tips")) {
            tipsButtonMouseReleased(null);
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
        // show a pop up
//        if (currentNotes.size() > 0) {
//
//            String type = "notes";
//
//            if (currentNotes.size() == 1) {
//                type = "note";
//            }
//
//            NotificationDialog notificationDialog = new NotificationDialog(PeptideShakerGUI.this, PeptideShakerGUI.this, false, currentNotes.size(), type);
//            notificationDialog.setLocation(PeptideShakerGUI.this.getWidth() - notificationDialog.getWidth() - 95 + PeptideShakerGUI.this.getX(),
//                    PeptideShakerGUI.this.getHeight() - 114 + PeptideShakerGUI.this.getY());
//            SwingUtils.fadeInAndOut(notificationDialog);
//        }
    }

    /**
     * Add a tip to the current list of tips.
     *
     * @param tip the tip to add, can contain HTML formatting, but not the HTML
     * start and end tags
     */
    public void addTip(String tip) {
        currentTips.add(tip);
        updateTipsNotificationCounter();
    }

    /**
     * Set the list of current tips.
     *
     * @param currentTips the tips to set
     */
    public void setCurentTips(ArrayList<String> currentTips) {
        this.currentTips = currentTips;
        updateTipsNotificationCounter();
    }

    /**
     * Update the notification counter for the tips.
     */
    public void updateTipsNotificationCounter() {
        if (currentTips.size() > 0) {
            tipsButton.setText("Tips (" + currentTips.size() + ")");
        } else {
            tipsButton.setText("Tips");
        }
    }

    /**
     * Show a tip notification pop up.
     */
    public void showTipsNotification() {
        // show a pop up
//        if (currentTips.size() > 0) {
//
//            String type = "tips";
//
//            if (currentTips.size() == 1) {
//                type = "tip";
//            }
//
//            NotificationDialog notificationDialog = new NotificationDialog(PeptideShakerGUI.this, PeptideShakerGUI.this, false, currentTips.size(), type);
//            notificationDialog.setLocation(PeptideShakerGUI.this.getWidth() - notificationDialog.getWidth() - 95 + PeptideShakerGUI.this.getX(),
//                    PeptideShakerGUI.this.getHeight() - 80 + PeptideShakerGUI.this.getY());
//            SwingUtils.fadeInAndOut(notificationDialog);
//        }
    }

    /**
     * Returns an extended HTML project report.
     *
     * @return an extended HTML project report
     */
    public String getExtendedProjectReport() {
        return cpsParent.getExtendedProjectReport(null);
    }

    /**
     * Check for new version.
     *
     * @return true if a new version is to be downloaded
     */
    public boolean checkForNewVersion() {
        try {
            File jarFile = new File(PeptideShakerGUI.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            MavenJarFile oldMavenJarFile = new MavenJarFile(jarFile.toURI());
            URL jarRepository = new URL("http", "genesis.ugent.be", new StringBuilder().append("/maven2/").toString());

            return CompomicsWrapper.checkForNewDeployedVersion(
                    "PeptideShaker", oldMavenJarFile, jarRepository, "peptide-shaker.ico",
                    false, true, true, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true);
        } catch (UnknownHostException ex) {
            // no internet connection
            System.out.println("Checking for new version failed. No internet connection.");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.out.println("Checking for new version failed. Unknown error.");
            return false;
        }
    }

    @Override
    public Filter createPsmFilter() {
        FilterDialog filterDialog = new FilterDialog(this, new PsmFilter(), getIdentificationParameters());
        if (!filterDialog.isCanceled()) {
            return filterDialog.getFilter();
        }
        return null;
    }

    @Override
    public Filter createPeptideFilter() {
        FilterDialog filterDialog = new FilterDialog(this, new PeptideFilter(), getIdentificationParameters());
        if (!filterDialog.isCanceled()) {
            return filterDialog.getFilter();
        }
        return null;
    }

    @Override
    public Filter createProteinFilter() {
        FilterDialog filterDialog = new FilterDialog(this, new ProteinFilter(), getIdentificationParameters());
        if (!filterDialog.isCanceled()) {
            return filterDialog.getFilter();
        }
        return null;
    }

    @Override
    public Filter editFilter(Filter filter) {
        FilterDialog filterDialog = new FilterDialog(this, (MatchFilter) filter, getIdentificationParameters());
        if (!filterDialog.isCanceled()) {
            return filterDialog.getFilter();
        }
        return null;
    }

    /**
     * Returns the specific annotation preferences for the currently selected
     * spectrum and peptide.
     *
     * @return the specific annotation preferences for the currently selected
     * spectrum and peptide
     */
    public SpecificAnnotationSettings getSpecificAnnotationPreferences() {
        return specificAnnotationPreferences;
    }

    /**
     * Sets the specific annotation preferences for the currently selected
     * spectrum and peptide.
     *
     * @param specificAnnotationPreferences the specific annotation preferences
     * for the currently selected spectrum and peptide
     */
    public void setSpecificAnnotationPreferences(SpecificAnnotationSettings specificAnnotationPreferences) {
        this.specificAnnotationPreferences = specificAnnotationPreferences;
    }

    /**
     * Selects the default annotation menu item.
     */
    private void selectDefaultAnnotationMenuItem() {
        defaultAnnotationCheckBoxMenuItem.setSelected(true);
        resetAnnotationMenu.setVisible(false);
    }

    /**
     * Deselects the default annotation menu item.
     */
    private void deselectDefaultAnnotationMenuItem() {
        defaultAnnotationCheckBoxMenuItem.setSelected(false);
        resetAnnotationMenu.setVisible(true);
    }
}
