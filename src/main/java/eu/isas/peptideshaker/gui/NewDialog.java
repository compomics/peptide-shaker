package eu.isas.peptideshaker.gui;

import com.compomics.util.gui.filehandling.FileDisplayDialog;
import com.compomics.util.gui.filehandling.FileSelectionDialog;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.WaitingDialogExceptionHandler;
import com.compomics.util.experiment.ProjectParameters;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.identification.identification_parameters.IdentificationParametersFactory;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.io.biology.protein.FastaSummary;
import com.compomics.util.experiment.io.biology.protein.ProteinDatabase;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.gui.parameters.identification.IdentificationParametersEditionDialog;
import com.compomics.util.gui.parameters.identification.search.SequenceDbDetailsDialog;
import com.compomics.util.gui.parameters.tools.ProcessingParametersDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.parameters.identification.advanced.ProteinInferenceParameters;
import com.compomics.util.parameters.tools.UtilitiesUserParameters;
import com.compomics.util.parameters.identification.advanced.ValidationQcParameters;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.gui.parameters.ProjectParametersDialog;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import eu.isas.peptideshaker.preferences.SpectrumCountingParameters;
import eu.isas.peptideshaker.utils.PsZipUtils;
import eu.isas.peptideshaker.utils.Tips;
import eu.isas.peptideshaker.validation.MatchesValidator;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.Function;

/**
 * A dialog for selecting the files to load.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class NewDialog extends javax.swing.JDialog {

    /**
     * The compomics PTM factory.
     */
    private ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * A reference to the main frame.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The list of identification files.
     */
    private ArrayList<File> idFiles = new ArrayList<>();
    /**
     * The XML modification files found.
     */
    private ArrayList<File> modificationFiles = new ArrayList<>();
    /**
     * A file where the input will be stored.
     */
    public final static String SEARCHGUI_INPUT = "searchGUI_input.txt";
    /**
     * The list of spectrum files.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<>();
    /**
     * The peptide shaker class which will take care of the pre-processing.
     */
    private PeptideShaker peptideShaker;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The parameters to use when loading the files.
     */
    private IdentificationParameters identificationParameters = null;
    /**
     * The processing preferences.
     */
    private ProcessingParameters processingParameters;
    /**
     * The display preferences.
     */
    private DisplayParameters displayPreferences = new DisplayParameters();
    /*
     * The welcome dialog parent, can be null.
     */
    private WelcomeDialog welcomeDialog;
    /**
     * The spectrum counting preferences.
     */
    private SpectrumCountingParameters spectrumCountingPreferences = new SpectrumCountingParameters();
    /**
     * The project details.
     */
    private ProjectDetails projectDetails = new ProjectDetails();
    /**
     * The identification parameters factory.
     */
    private IdentificationParametersFactory identificationParametersFactory = IdentificationParametersFactory.getInstance();

    /**
     * Creates a new open dialog.
     *
     * @param peptideShakerGui a reference to the main frame
     * @param modal boolean indicating whether the dialog is modal
     */
    public NewDialog(PeptideShakerGUI peptideShakerGui, boolean modal) {
        super(peptideShakerGui, modal);
        this.peptideShakerGUI = peptideShakerGui;
        this.welcomeDialog = null;

        processingParameters = new ProcessingParameters();

        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setLocationRelativeTo(peptideShakerGui);
        setVisible(true);
    }

    /**
     * Creates a new open dialog.
     *
     * @param welcomeDialog the welcome dialog parent frame
     * @param peptideShakerGui a reference to the main frame
     * @param modal boolean indicating whether the dialog is modal
     */
    public NewDialog(WelcomeDialog welcomeDialog, PeptideShakerGUI peptideShakerGui, boolean modal) {
        super(welcomeDialog, modal);
        this.peptideShakerGUI = peptideShakerGui;
        this.welcomeDialog = welcomeDialog;

        processingParameters = new ProcessingParameters();

        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setLocationRelativeTo(welcomeDialog);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {
        initComponents();
        settingsComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        idFilesTxt.setText(idFiles.size() + " file(s) selected");
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
        fastaFileTxt.setText("");
        processingTxt.setText(processingParameters.getnThreads() + " cores");

        // set the search parameters
        Vector parameterList = new Vector();
        parameterList.add("-- Select --");

        for (String tempParameters : identificationParametersFactory.getParametersList()) {
            parameterList.add(tempParameters);
        }

        settingsComboBox.setModel(new javax.swing.DefaultComboBoxModel(parameterList));

        validateInput();
        GuiUtilities.installEscapeCloseOperation(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sampleDetailsPanel = new javax.swing.JPanel();
        loadButton = new javax.swing.JButton();
        projectDetailsPanel = new javax.swing.JPanel();
        replicateNumberIdtxt = new javax.swing.JTextField();
        projectNameIdTxt = new javax.swing.JTextField();
        replicateLabel = new javax.swing.JLabel();
        sampleNameLabel = new javax.swing.JLabel();
        projectReferenceLabel = new javax.swing.JLabel();
        sampleNameIdtxt = new javax.swing.JTextField();
        processingParametersPanel = new javax.swing.JPanel();
        projectSettingsTxt = new javax.swing.JTextField();
        projectSettingsLabel = new javax.swing.JLabel();
        identificationParametersLabel = new javax.swing.JLabel();
        editSettingsButton = new javax.swing.JButton();
        projectSettingsButton = new javax.swing.JButton();
        processingLbl = new javax.swing.JLabel();
        processingTxt = new javax.swing.JTextField();
        editProcessingButton = new javax.swing.JButton();
        addSettingsButton = new javax.swing.JButton();
        settingsComboBox = new javax.swing.JComboBox();
        inputFilesPanel = new javax.swing.JPanel();
        idFilesLabel = new javax.swing.JLabel();
        idFilesTxt = new javax.swing.JTextField();
        browseId = new javax.swing.JButton();
        clearId = new javax.swing.JButton();
        spectrumFilesLabel = new javax.swing.JLabel();
        spectrumFilesTxt = new javax.swing.JTextField();
        browseSpectra = new javax.swing.JButton();
        clearSpectra = new javax.swing.JButton();
        databaseLabel = new javax.swing.JLabel();
        fastaFileTxt = new javax.swing.JTextField();
        browseDbButton = new javax.swing.JButton();
        clearDbButton = new javax.swing.JButton();
        aboutButton = new javax.swing.JButton();
        peptideShakerPublicationLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PeptideShaker - New Project");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        sampleDetailsPanel.setBackground(new java.awt.Color(230, 230, 230));

        loadButton.setBackground(new java.awt.Color(0, 153, 0));
        loadButton.setFont(loadButton.getFont().deriveFont(loadButton.getFont().getStyle() | java.awt.Font.BOLD));
        loadButton.setForeground(new java.awt.Color(255, 255, 255));
        loadButton.setText("Load Data!");
        loadButton.setEnabled(false);
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });

        projectDetailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Details"));
        projectDetailsPanel.setOpaque(false);

        replicateNumberIdtxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        replicateNumberIdtxt.setText("0");
        replicateNumberIdtxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                replicateNumberIdtxtKeyReleased(evt);
            }
        });

        projectNameIdTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        projectNameIdTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                projectNameIdTxtKeyReleased(evt);
            }
        });

        replicateLabel.setForeground(new java.awt.Color(255, 0, 0));
        replicateLabel.setText("Replicate*");
        replicateLabel.setToolTipText("The replicate number");

        sampleNameLabel.setForeground(new java.awt.Color(255, 0, 0));
        sampleNameLabel.setText("Sample Name*");
        sampleNameLabel.setToolTipText("The name of the sample or experiment");

        projectReferenceLabel.setForeground(new java.awt.Color(255, 0, 0));
        projectReferenceLabel.setText("Project Reference*");
        projectReferenceLabel.setToolTipText("A project name for future reference");

        sampleNameIdtxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        sampleNameIdtxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                sampleNameIdtxtKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout projectDetailsPanelLayout = new javax.swing.GroupLayout(projectDetailsPanel);
        projectDetailsPanel.setLayout(projectDetailsPanelLayout);
        projectDetailsPanelLayout.setHorizontalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(projectReferenceLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectNameIdTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                    .addComponent(sampleNameIdtxt))
                .addGap(21, 21, 21)
                .addComponent(replicateLabel)
                .addGap(18, 18, 18)
                .addComponent(replicateNumberIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        projectDetailsPanelLayout.setVerticalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectNameIdTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectReferenceLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleNameIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(replicateNumberIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(replicateLabel)
                    .addComponent(sampleNameLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        processingParametersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Settings"));
        processingParametersPanel.setOpaque(false);

        projectSettingsTxt.setEditable(false);
        projectSettingsTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        projectSettingsTxt.setText("Default");

        projectSettingsLabel.setText("Project");

        identificationParametersLabel.setText("Identification");

        editSettingsButton.setText("Edit");
        editSettingsButton.setEnabled(false);
        editSettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSettingsButtonActionPerformed(evt);
            }
        });

        projectSettingsButton.setText("Edit");
        projectSettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projectSettingsButtonActionPerformed(evt);
            }
        });

        processingLbl.setText("Processing");

        processingTxt.setEditable(false);
        processingTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        processingTxt.setText("Default");

        editProcessingButton.setText("Edit");
        editProcessingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editProcessingButtonActionPerformed(evt);
            }
        });

        addSettingsButton.setText("Add");
        addSettingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSettingsButtonActionPerformed(evt);
            }
        });

        settingsComboBox.setMaximumRowCount(16);
        settingsComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "-- Select --" }));
        settingsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout processingParametersPanelLayout = new javax.swing.GroupLayout(processingParametersPanel);
        processingParametersPanel.setLayout(processingParametersPanelLayout);
        processingParametersPanelLayout.setHorizontalGroup(
            processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingParametersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(processingParametersPanelLayout.createSequentialGroup()
                        .addComponent(processingLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(processingTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
                            .addComponent(projectSettingsTxt, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(processingParametersPanelLayout.createSequentialGroup()
                        .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(projectSettingsLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                            .addComponent(identificationParametersLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(settingsComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editProcessingButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(projectSettingsButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(processingParametersPanelLayout.createSequentialGroup()
                        .addComponent(addSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(editSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        processingParametersPanelLayout.setVerticalGroup(
            processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingParametersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(identificationParametersLabel)
                    .addComponent(editSettingsButton)
                    .addComponent(addSettingsButton)
                    .addComponent(settingsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectSettingsLabel)
                    .addComponent(projectSettingsTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectSettingsButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(processingLbl)
                    .addComponent(processingTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editProcessingButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        inputFilesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Input Files"));
        inputFilesPanel.setOpaque(false);

        idFilesLabel.setForeground(new java.awt.Color(255, 0, 0));
        idFilesLabel.setText("Identification File(s)*");

        idFilesTxt.setEditable(false);
        idFilesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        idFilesTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                idFilesTxtMouseClicked(evt);
            }
        });

        browseId.setText("Browse");
        browseId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseIdActionPerformed(evt);
            }
        });

        clearId.setText("Clear");
        clearId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearIdActionPerformed(evt);
            }
        });

        spectrumFilesLabel.setForeground(new java.awt.Color(255, 0, 0));
        spectrumFilesLabel.setText("Spectrum File(s)*");

        spectrumFilesTxt.setEditable(false);
        spectrumFilesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        spectrumFilesTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                spectrumFilesTxtMouseClicked(evt);
            }
        });

        browseSpectra.setText("Browse");
        browseSpectra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseSpectraActionPerformed(evt);
            }
        });

        clearSpectra.setText("Clear");
        clearSpectra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSpectraActionPerformed(evt);
            }
        });

        databaseLabel.setForeground(new java.awt.Color(255, 0, 0));
        databaseLabel.setText("Database File*");

        fastaFileTxt.setEditable(false);
        fastaFileTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        browseDbButton.setText("Browse");
        browseDbButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseDbButtonActionPerformed(evt);
            }
        });

        clearDbButton.setText("Clear");
        clearDbButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearDbButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout inputFilesPanelLayout = new javax.swing.GroupLayout(inputFilesPanel);
        inputFilesPanel.setLayout(inputFilesPanelLayout);
        inputFilesPanelLayout.setHorizontalGroup(
            inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                        .addComponent(idFilesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(idFilesTxt)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseId)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearId))
                    .addGroup(inputFilesPanelLayout.createSequentialGroup()
                        .addComponent(spectrumFilesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spectrumFilesTxt)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseSpectra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearSpectra))
                    .addGroup(inputFilesPanelLayout.createSequentialGroup()
                        .addComponent(databaseLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fastaFileTxt)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseDbButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearDbButton)))
                .addContainerGap())
        );

        inputFilesPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseId, clearId});

        inputFilesPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseSpectra, clearSpectra});

        inputFilesPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseDbButton, clearDbButton});

        inputFilesPanelLayout.setVerticalGroup(
            inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(idFilesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(idFilesLabel)
                    .addComponent(clearId)
                    .addComponent(browseId))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spectrumFilesLabel)
                    .addComponent(clearSpectra)
                    .addComponent(browseSpectra)
                    .addComponent(spectrumFilesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(clearDbButton)
                    .addComponent(browseDbButton)
                    .addComponent(fastaFileTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(databaseLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        aboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/peptide-shaker-medium-orange-shadow.png"))); // NOI18N
        aboutButton.setToolTipText("Open the PeptideShaker web page");
        aboutButton.setBorder(null);
        aboutButton.setBorderPainted(false);
        aboutButton.setContentAreaFilled(false);
        aboutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                aboutButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                aboutButtonMouseExited(evt);
            }
        });
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });

        peptideShakerPublicationLabel.setText("<html>Please cite PeptideShaker as <a href=\"http://www.nature.com/nbt/journal/v33/n1/full/nbt.3109.html\">Vaudel <i>et al.</i>: Nature Biotechnol. 2015 Jan;33(1):22–24</a>.</html>\n\n");
        peptideShakerPublicationLabel.setToolTipText("Open the PeptideShaker publication");
        peptideShakerPublicationLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                peptideShakerPublicationLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                peptideShakerPublicationLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideShakerPublicationLabelMouseExited(evt);
            }
        });

        javax.swing.GroupLayout sampleDetailsPanelLayout = new javax.swing.GroupLayout(sampleDetailsPanel);
        sampleDetailsPanel.setLayout(sampleDetailsPanelLayout);
        sampleDetailsPanelLayout.setHorizontalGroup(
            sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(aboutButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(peptideShakerPublicationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 527, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(22, 22, 22))
                    .addComponent(projectDetailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(inputFilesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(processingParametersPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        sampleDetailsPanelLayout.setVerticalGroup(
            sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inputFilesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(processingParametersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(aboutButton)
                    .addComponent(peptideShakerPublicationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(loadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sampleDetailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sampleDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Tries to process the identification files, closes the dialog and then
     * opens the results in the main frame.
     *
     * @param evt
     */
    private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed

        if (validateUserInput()) {

            if (welcomeDialog != null) {
                welcomeDialog.setVisible(false);
            }

            this.setVisible(false);
            peptideShakerGUI.setVisible(true);

            peptideShakerGUI.setIdentificationParameters(identificationParameters);
            peptideShakerGUI.setProcessingParameters(processingParameters);
            peptideShakerGUI.setDisplayParameters(displayPreferences);
            projectDetails = new ProjectDetails();
            projectDetails.setCreationDate(new Date());
            projectDetails.setPeptideShakerVersion(new eu.isas.peptideshaker.utils.Properties().getVersion());
            peptideShakerGUI.setProjectDetails(projectDetails);
            peptideShakerGUI.setCurentNotes(new ArrayList<>());
            peptideShakerGUI.updateNotesNotificationCounter();
            peptideShakerGUI.resetDisplayFeaturesGenerator();
            peptideShakerGUI.setSpectrumCountingParameters(spectrumCountingPreferences);

            ProjectParameters projectParameters = new ProjectParameters(projectNameIdTxt.getText().trim());

            // incrementing the counter for a new PeptideShaker start run via GUI
            if (peptideShakerGUI.getUtilitiesUserParameters().isAutoUpdate()) {
                Util.sendGAUpdate("UA-36198780-1", "startrun-gui", "peptide-shaker-" + PeptideShaker.getVersion());
            }

            peptideShaker = new PeptideShaker(projectParameters);

            ArrayList<String> tips;
            try {
                tips = Tips.getTips();
            } catch (Exception e) {
                tips = new ArrayList<>();
                // Do something here?
            }

            WaitingDialog waitingDialog = new WaitingDialog(peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true, tips, "Importing Data", "PeptideShaker", PeptideShaker.getVersion(), true);
            waitingDialog.setCloseDialogWhenImportCompletes(true, true);

            int progressCounter = idFiles.size() + spectrumFiles.size();

            progressCounter++; // establishing the database connection
            progressCounter++; // the FASTA file
            progressCounter++; // the peptide to protein map
            progressCounter += 6; // computing probabilities etc
            if (identificationParameters.getProteinInferenceParameters().getSimplifyGroups()) {
                progressCounter++; // simplify protein groups
            }
            progressCounter++; // resolving protein inference
            progressCounter += 4; // Correcting protein probabilities, Validating identifications at 1% FDR, Scoring PTMs in peptides, Scoring PTMs in proteins.
            progressCounter += 2; // Scoring PTMs in PSMs. Estimating PTM FLR.
            if (identificationParameters.getModificationLocalizationParameters().getAlignNonConfidentPTMs()) {
                progressCounter++; // Peptide inference
            }

            // add one more just to not start at 0%
            progressCounter++;

            waitingDialog.setMaxPrimaryProgressCounter(progressCounter);
            waitingDialog.increasePrimaryProgressCounter(); // just to not start at 0%

            boolean needDialog = false;

            // load the identification files
            if (idFiles.size() > 0) {

                try {

                    needDialog = true;
                    ExceptionHandler exceptionHandler = new WaitingDialogExceptionHandler((WaitingDialog) waitingDialog, "https://github.com/compomics/peptide-shaker/issues");
                    peptideShaker.importFiles(waitingDialog, idFiles, spectrumFiles,
                            identificationParameters, projectDetails, processingParameters, exceptionHandler);
                    peptideShaker.createProject(identificationParameters, processingParameters, spectrumCountingPreferences, projectDetails, waitingDialog, exceptionHandler);

                } catch (Exception e) {

                    // Put in separate thread
                }

            }

            if (needDialog) {
                try {
                    waitingDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
                this.dispose();
            }

            if (!needDialog || !waitingDialog.isRunCanceled()) {

                peptideShakerGUI.setProject(projectParameters);
                peptideShakerGUI.setIdentification(peptideShaker.getIdentification());
                peptideShakerGUI.setMetrics(peptideShaker.getMetrics());
                peptideShakerGUI.setGeneMaps(peptideShaker.getGeneMaps());
                peptideShakerGUI.setIdentificationFeaturesGenerator(peptideShaker.getIdentificationFeaturesGenerator());
                peptideShakerGUI.displayResults();
                peptideShakerGUI.initiateDisplay(); // display the overview tab
                peptideShakerGUI.getProjectDetails().setReport(waitingDialog.getReport(null));
                this.dispose();

            } else if (waitingDialog.isRunCanceled()) {

                // close the database
                try {
                    peptideShaker.getIdentification().close();
                } catch (Exception e) {
                    System.out.println("Failed to close the database!");
                    e.printStackTrace();
                }
            }
        }
}//GEN-LAST:event_loadButtonActionPerformed

    /**
     * Clear the database field.
     *
     * @param evt
     */
    private void clearDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearDbButtonActionPerformed
        fastaFileTxt.setText("");
        validateInput();
}//GEN-LAST:event_clearDbButtonActionPerformed

    /**
     * Opens a file chooser where the user can select the database FASTA file to
     * use.
     *
     * @param evt
     */
    private void browseDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDbButtonActionPerformed

        if (identificationParameters == null) {

            editSettingsButtonActionPerformed(null);

        } else {

            SearchParameters searchParameters = identificationParameters.getSearchParameters();

            SequenceDbDetailsDialog sequenceDbDetailsDialog = new SequenceDbDetailsDialog(peptideShakerGUI,
                    searchParameters.getFastaFile(), searchParameters.getFastaParameters(),
                    peptideShakerGUI.getLastSelectedFolder(), true,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

            boolean success = sequenceDbDetailsDialog.selectDB(true);

            if (success) {

                sequenceDbDetailsDialog.setVisible(true);

                if (!sequenceDbDetailsDialog.isCanceled()) {

                    File fastaFile = sequenceDbDetailsDialog.getSelectedFastaFile();
                    FastaParameters fastaParameters = sequenceDbDetailsDialog.getFastaParameters();
                    fastaFileTxt.setText(fastaFile.getName());
                    searchParameters.setFastaFile(fastaFile);
                    searchParameters.setFastaParameters(fastaParameters);
                    checkFastaFile();

                }
            }

            validateInput();
        }
}//GEN-LAST:event_browseDbButtonActionPerformed

    /**
     * Clear the spectra selection.
     *
     * @param evt
     */
    private void clearSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectraActionPerformed
        spectrumFiles = new ArrayList<>();
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
        validateInput();
}//GEN-LAST:event_clearSpectraActionPerformed

    /**
     * Open a file selection dialog where the user can select the spectrum files
     * to use.
     *
     * @param evt
     */
    private void browseSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseSpectraActionPerformed

        // @TODO: implement mzML support
        JFileChooser fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Spectrum File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mgf")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Mascot Generic Format (.mgf)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this, "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            // get the files
            for (File newFile : fileChooser.getSelectedFiles()) {
                if (newFile.isDirectory()) {
                    File[] tempFiles = newFile.listFiles();
                    for (File file : tempFiles) {
                        if (file.getName().toLowerCase().endsWith(".mgf")) {
                            spectrumFiles.add(file);
                        }
                    }
                } else {
                    spectrumFiles.add(newFile);
                }
            }

            spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
            validateInput();
        }
}//GEN-LAST:event_browseSpectraActionPerformed

    /**
     * Clear the identification files.
     *
     * @param evt
     */
    private void clearIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearIdActionPerformed
        idFiles = new ArrayList<>();
        idFilesTxt.setText(idFiles.size() + " file(s) selected");
        validateInput();
}//GEN-LAST:event_clearIdActionPerformed

    /**
     * Open a file chooser for selecting identification files.
     *
     * @param evt
     */
    private void browseIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIdActionPerformed

        final JFileChooser fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Identification File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

        // filter for all search engines
        FileFilter allFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                if (myFile.getName().equalsIgnoreCase("mods.xml")
                        || myFile.getName().equalsIgnoreCase("usermods.xml")) {
                    return false;
                }

                return myFile.getName().toLowerCase().endsWith(".omx")
                        || myFile.getName().toLowerCase().endsWith(".t.xml")
                        || myFile.getName().toLowerCase().endsWith(".pep.xml")
                        || myFile.getName().toLowerCase().endsWith(".dat")
                        || myFile.getName().toLowerCase().endsWith(".mzid")
                        || myFile.getName().toLowerCase().endsWith(".ms-amanda.csv")
                        || myFile.getName().toLowerCase().endsWith(".res")
                        || myFile.getName().toLowerCase().endsWith(".tide-search.target.txt")
                        || myFile.getName().toLowerCase().endsWith(".tags")
                        || myFile.getName().toLowerCase().endsWith(".pnovo.txt")
                        || myFile.getName().toLowerCase().endsWith(".novor.csv")
                        || myFile.getName().toLowerCase().endsWith(".psm")
                        || myFile.getName().toLowerCase().endsWith(".zip")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "All supported search result output formats";
            }
        };

        // filter for zip folders only
        FileFilter zipFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".zip")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Compressed zip folder (.zip)";
            }
        };

        // filter for omssa only
        FileFilter omssaFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".omx")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "OMSSA (.omx)";
            }
        };

        // filter for andromeda only
        FileFilter andromedaFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".res")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Andromeda (.res)";
            }
        };

        // filter for x!tandem only
        FileFilter tandemFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".t.xml")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "X!Tandem (.t.xml)";
            }
        };

        // filter for PepXML only
        FileFilter pepXMLFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".pep.xml")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "PepXML (.pep.xml)";
            }
        };

        // filter for mzIdentML only
        FileFilter mzidFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".mzid")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "mzIdentML (.mzid)";
            }
        };

        // filter for ms amanda only
        FileFilter msAmandaFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".csv")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "MS Amanda (.ms-amanda.csv)";
            }
        };

        // filter for tide only
        FileFilter tideFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".tide-search.target.txt")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Tide (.tide-search.target.txt)";
            }
        };

        // filter for DirecTag only
        FileFilter direcTagFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".tags")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "DirecTag (.tags)";
            }
        };

        // filter for novor only
        FileFilter pNovoFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".pnovo.txt")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "pNovo+ (.pnovo.txt)";
            }
        };

        // filter for novor only
        FileFilter novorFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".novor.csv")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Novor (.novor.csv)";
            }
        };

        // filter for Onyase only
        FileFilter onyaseFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".psm")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Onyase (.dat)";
            }
        };

        // filter for mascot only
        FileFilter mascotFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".dat")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Mascot (.dat)";
            }
        };

        fileChooser.setFileFilter(allFilter);
        fileChooser.addChoosableFileFilter(zipFilter);
        fileChooser.addChoosableFileFilter(pepXMLFilter);
        fileChooser.addChoosableFileFilter(mzidFilter);
        fileChooser.addChoosableFileFilter(omssaFilter);
        fileChooser.addChoosableFileFilter(andromedaFilter);
        fileChooser.addChoosableFileFilter(tandemFilter);
        fileChooser.addChoosableFileFilter(msAmandaFilter);
        fileChooser.addChoosableFileFilter(tideFilter);
        fileChooser.addChoosableFileFilter(mascotFilter);
        fileChooser.addChoosableFileFilter(onyaseFilter);
        fileChooser.addChoosableFileFilter(direcTagFilter);
        fileChooser.addChoosableFileFilter(novorFilter);
        fileChooser.addChoosableFileFilter(pNovoFilter);

        int returnVal = fileChooser.showDialog(this, "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            progressDialog = new ProgressDialogX(this, peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Loading Files. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("importThread") {
                public void run() {
                    loadIdInputFiles(fileChooser.getSelectedFiles());
                }
            }.start();
        }
}//GEN-LAST:event_browseIdActionPerformed

    /**
     * Open the IdentificationParametersEditionDialog.
     *
     * @param evt
     */
    private void editSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSettingsButtonActionPerformed
        IdentificationParametersEditionDialog identificationParametersEditionDialog = new IdentificationParametersEditionDialog(
                this, peptideShakerGUI, identificationParameters, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), peptideShakerGUI.getLastSelectedFolder(), peptideShakerGUI, true);

        if (!identificationParametersEditionDialog.isCanceled()) {
            setIdentificationParameters(identificationParametersEditionDialog.getIdentificationParameters());
        }
    }//GEN-LAST:event_editSettingsButtonActionPerformed

    /**
     * Open the ImportSettingsDialog.
     *
     * @param evt
     */
    private void projectSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_projectSettingsButtonActionPerformed
        ProjectParametersDialog preferencesDialog = new ProjectParametersDialog(peptideShakerGUI, spectrumCountingPreferences, displayPreferences);
        if (!preferencesDialog.isCanceled()) {
            spectrumCountingPreferences = preferencesDialog.getSpectrumCountingParameters();
            displayPreferences = preferencesDialog.getDisplayParameters();
        }
    }//GEN-LAST:event_projectSettingsButtonActionPerformed

    /**
     * Closes the dialog.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.dispose();
    }//GEN-LAST:event_formWindowClosing

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void projectNameIdTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_projectNameIdTxtKeyReleased
        validateInput();
    }//GEN-LAST:event_projectNameIdTxtKeyReleased

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void sampleNameIdtxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sampleNameIdtxtKeyReleased
        validateInput();
    }//GEN-LAST:event_sampleNameIdtxtKeyReleased

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void replicateNumberIdtxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_replicateNumberIdtxtKeyReleased
        validateInput();
    }//GEN-LAST:event_replicateNumberIdtxtKeyReleased

    /**
     * Open the ProcessingPreferences dialog.
     *
     * @param evt
     */
    private void editProcessingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editProcessingButtonActionPerformed
        ProcessingParametersDialog processingPreferencesDialog = new ProcessingParametersDialog(this, peptideShakerGUI, processingParameters, true);
        if (!processingPreferencesDialog.isCanceled()) {
            processingParameters = processingPreferencesDialog.getProcessingParameters();
            processingTxt.setText(processingParameters.getnThreads() + " cores");
        }
    }//GEN-LAST:event_editProcessingButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void aboutButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void aboutButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseExited

    /**
     * Open the PeptideShaker web page.
     *
     * @param evt
     */
    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://compomics.github.io/projects/peptide-shaker.html");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    /**
     * Open the PeptideShaker web page.
     *
     * @param evt
     */
    private void peptideShakerPublicationLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerPublicationLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://www.nature.com/nbt/journal/v33/n1/full/nbt.3109.html");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerPublicationLabelMouseClicked

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void peptideShakerPublicationLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerPublicationLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptideShakerPublicationLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptideShakerPublicationLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerPublicationLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerPublicationLabelMouseExited

    /**
     * Display the list of selected identification files.
     *
     * @param evt
     */
    private void idFilesTxtMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_idFilesTxtMouseClicked
        if (!idFiles.isEmpty()) {
            FileDisplayDialog fileDisplayDialog = new FileDisplayDialog(this, idFiles, true);
            if (!fileDisplayDialog.canceled()) {
                idFiles = fileDisplayDialog.getSelectedFiles();
                idFilesTxt.setText(idFiles.size() + " file(s) selected");
                validateInput();
            }
        }
    }//GEN-LAST:event_idFilesTxtMouseClicked

    /**
     * Display the list of selected spectrum files.
     *
     * @param evt
     */
    private void spectrumFilesTxtMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumFilesTxtMouseClicked
        if (!spectrumFiles.isEmpty()) {
            FileDisplayDialog fileDisplayDialog = new FileDisplayDialog(this, spectrumFiles, true);
            if (!fileDisplayDialog.canceled()) {
                spectrumFiles = fileDisplayDialog.getSelectedFiles();
                spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
                validateInput();
            }
        }
    }//GEN-LAST:event_spectrumFilesTxtMouseClicked

    /**
     * Load search settings from a file.
     *
     * @param evt the action event
     */
    private void addSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSettingsButtonActionPerformed
        IdentificationParametersEditionDialog identificationParametersEditionDialog = new IdentificationParametersEditionDialog(
                this, peptideShakerGUI, null, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), peptideShakerGUI.getLastSelectedFolder(), null, true);

        if (!identificationParametersEditionDialog.isCanceled()) {
            setIdentificationParameters(identificationParametersEditionDialog.getIdentificationParameters());
        }
    }//GEN-LAST:event_addSettingsButtonActionPerformed

    /**
     * Enable/disable the Edit button for the settings.
     *
     * @param evt
     */
    private void settingsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsComboBoxActionPerformed
        editSettingsButton.setEnabled(settingsComboBox.getSelectedIndex() != 0);

        if (settingsComboBox.getSelectedIndex() != 0) {
            File identificationParametersFile = IdentificationParametersFactory.getIdentificationParametersFile((String) settingsComboBox.getSelectedItem());
            try {
                identificationParameters = IdentificationParameters.getIdentificationParameters(identificationParametersFile);

                // load project specific PTMs
                String error = PeptideShaker.loadModifications(identificationParameters.getSearchParameters());
                if (error != null) {
                    JOptionPane.showMessageDialog(peptideShakerGUI,
                            error,
                            "PTM Definition Changed", JOptionPane.WARNING_MESSAGE);
                }

                setIdentificationParameters(identificationParameters);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to import search parameters from: " + identificationParametersFile.getAbsolutePath() + ".", "Search Parameters",
                        JOptionPane.WARNING_MESSAGE);
                e.printStackTrace();
            }
        }

        validateInput();
    }//GEN-LAST:event_settingsComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JButton addSettingsButton;
    private javax.swing.JButton browseDbButton;
    private javax.swing.JButton browseId;
    private javax.swing.JButton browseSpectra;
    private javax.swing.JButton clearDbButton;
    private javax.swing.JButton clearId;
    private javax.swing.JButton clearSpectra;
    private javax.swing.JLabel databaseLabel;
    private javax.swing.JButton editProcessingButton;
    private javax.swing.JButton editSettingsButton;
    private javax.swing.JTextField fastaFileTxt;
    private javax.swing.JLabel idFilesLabel;
    private javax.swing.JTextField idFilesTxt;
    private javax.swing.JLabel identificationParametersLabel;
    private javax.swing.JPanel inputFilesPanel;
    private javax.swing.JButton loadButton;
    private javax.swing.JLabel peptideShakerPublicationLabel;
    private javax.swing.JLabel processingLbl;
    private javax.swing.JPanel processingParametersPanel;
    private javax.swing.JTextField processingTxt;
    private javax.swing.JPanel projectDetailsPanel;
    private javax.swing.JTextField projectNameIdTxt;
    private javax.swing.JLabel projectReferenceLabel;
    private javax.swing.JButton projectSettingsButton;
    private javax.swing.JLabel projectSettingsLabel;
    private javax.swing.JTextField projectSettingsTxt;
    private javax.swing.JLabel replicateLabel;
    private javax.swing.JTextField replicateNumberIdtxt;
    private javax.swing.JPanel sampleDetailsPanel;
    private javax.swing.JTextField sampleNameIdtxt;
    private javax.swing.JLabel sampleNameLabel;
    private javax.swing.JComboBox settingsComboBox;
    private javax.swing.JLabel spectrumFilesLabel;
    private javax.swing.JTextField spectrumFilesTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * Validates the input parameters.
     *
     * @return true if the input is valid, false otherwise.
     */
    private void validateInput() {

        boolean allValid = true;

        // highlight the fields that have not been filled
        if (projectNameIdTxt.getText().length() > 0) {
            projectReferenceLabel.setForeground(Color.BLACK);
            projectReferenceLabel.setToolTipText(null);
            projectNameIdTxt.setToolTipText(null);
        } else {
            projectReferenceLabel.setForeground(Color.RED);
            projectReferenceLabel.setToolTipText("Please provide a project reference");
            projectNameIdTxt.setToolTipText("Please provide a project reference");
            allValid = false;
        }

        if (sampleNameIdtxt.getText().length() > 0) {
            sampleNameLabel.setForeground(Color.BLACK);
            sampleNameLabel.setToolTipText(null);
            sampleNameIdtxt.setToolTipText(null);
        } else {
            sampleNameLabel.setForeground(Color.RED);
            sampleNameLabel.setToolTipText("Please provide a project sample name");
            sampleNameIdtxt.setToolTipText("Please provide a project sample name");
            allValid = false;
        }

        if (replicateNumberIdtxt.getText().length() > 0) {
            replicateLabel.setForeground(Color.BLACK);
            replicateLabel.setToolTipText(null);
            replicateNumberIdtxt.setToolTipText(null);
        } else {
            replicateLabel.setForeground(Color.RED);
            replicateLabel.setToolTipText("Please provide a replicate number");
            replicateNumberIdtxt.setToolTipText("Please provide a replicate number");
            allValid = false;
        }

        if (idFiles.size() > 0) {
            idFilesLabel.setForeground(Color.BLACK);
            idFilesLabel.setToolTipText(null);
            idFilesTxt.setToolTipText("Click to see the selected files");
            idFilesTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            idFilesLabel.setForeground(Color.RED);
            idFilesLabel.setToolTipText("Please select at least one identification file");
            idFilesTxt.setToolTipText("Please select at least one identification file");
            idFilesTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (spectrumFiles.size() > 0) {
            spectrumFilesLabel.setForeground(Color.BLACK);
            spectrumFilesLabel.setToolTipText(null);
            spectrumFilesTxt.setToolTipText("Click to see the selected files");
            spectrumFilesTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            spectrumFilesLabel.setForeground(Color.RED);
            spectrumFilesLabel.setToolTipText("Please select the spectrum file(s) for the identfication files");
            spectrumFilesTxt.setToolTipText("Please select the spectrum file(s) for the identfication files");
            spectrumFilesTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (fastaFileTxt.getText() != null && fastaFileTxt.getText().length() > 0
                && identificationParameters != null
                && identificationParameters.getSearchParameters() != null
                && identificationParameters.getSearchParameters().getFastaFile() != null
                && identificationParameters.getSearchParameters().getFastaFile().exists()) {
            databaseLabel.setForeground(Color.BLACK);
            databaseLabel.setToolTipText(null);
            fastaFileTxt.setToolTipText(null);
        } else {
            databaseLabel.setForeground(Color.RED);
            if (fastaFileTxt.getText().length() > 0) {
                databaseLabel.setToolTipText("FASTA file not found!");
                fastaFileTxt.setToolTipText("FASTA file not found!");
            } else {
                databaseLabel.setToolTipText("Please select the database file used");
                fastaFileTxt.setToolTipText("Please select the database file used");
            }
            allValid = false;
        }

        if (identificationParameters != null && settingsComboBox.getSelectedIndex() != 0) {
            identificationParametersLabel.setForeground(Color.BLACK);
            identificationParametersLabel.setToolTipText(null);
            settingsComboBox.setToolTipText(null);
        } else {
            identificationParametersLabel.setForeground(Color.RED);
            identificationParametersLabel.setToolTipText("Please set the identification parameters");
            settingsComboBox.setToolTipText("Please set the identification parameters");
            allValid = false;
        }

        // enable/disable the Create! button
        loadButton.setEnabled(allValid);
    }

    /**
     * Validates the format of the replicate number and the FASTA file.
     *
     * @return true if the input is valid, false otherwise.
     */
    private boolean validateUserInput() {

        for (String forbiddenChar : Util.forbiddenCharacters) {
            if (projectNameIdTxt.getText().contains(forbiddenChar)) {
                JOptionPane.showMessageDialog(null, "The project name should not contain " + forbiddenChar + ".\n"
                        + "Forbidden character in project name",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                projectReferenceLabel.setForeground(Color.red);
                return false;
            }
        }
        for (String forbiddenChar : Util.forbiddenCharacters) {
            if (sampleNameIdtxt.getText().contains(forbiddenChar)) {
                JOptionPane.showMessageDialog(null, "The sample name should not contain " + forbiddenChar + ".\n"
                        + "Forbidden character in sample name",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                sampleNameLabel.setForeground(Color.red);
                return false;
            }
        }

        try {
            getReplicateNumber();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for replicate number.\n"
                    + "Has to be a number!",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            replicateLabel.setForeground(Color.RED);
            return false;
        }

        if (identificationParameters == null) {
            JOptionPane.showMessageDialog(null, "Please edit the search parameters.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        if (searchParameters == null) {
            JOptionPane.showMessageDialog(null, "Please edit the search parameters.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (searchParameters.getFastaFile() == null) {
            JOptionPane.showMessageDialog(null, "Please verify the input for FASTA file.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            databaseLabel.setForeground(Color.RED);
            return false;
        }

        return true;
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicate number
     */
    private int getReplicateNumber() {
        return new Integer(replicateNumberIdtxt.getText().trim());
    }

    /**
     * Imports the search parameters from a file.
     *
     * @param file the selected searchGUI file
     * @param dataFolders folders where to look for the FASTA file
     * @param progressDialog the progress dialog
     *
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while importing the search parameters
     * @throws java.io.FileNotFoundException exception thrown whenever an error
     * occurred while importing the search parameters
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while importing the search parameters
     */
    public void importSearchParameters(File file, ArrayList<File> dataFolders, ProgressDialogX progressDialog) throws IOException, FileNotFoundException, ClassNotFoundException {

        progressDialog.setTitle("Importing Search Parameters. Please Wait...");

        IdentificationParameters tempIdentificationParameters = IdentificationParameters.getIdentificationParameters(file);
        SearchParameters searchParameters = tempIdentificationParameters.getSearchParameters();
        String toCheck = PeptideShaker.loadModifications(searchParameters);
        if (toCheck != null) {
            JOptionPane.showMessageDialog(this, toCheck, "Modification Definition Changed", JOptionPane.WARNING_MESSAGE);
        }

        ModificationParameters modificationProfile = searchParameters.getModificationParameters();

        ArrayList<String> missing = new ArrayList<>();

        for (String name : modificationProfile.getAllModifications()) {
            if (!modificationFactory.containsModification(name)) {
                missing.add(name);
                Modification ptm = modificationFactory.getModification(name);
                ptm.getMass();
            } else if (modificationProfile.getColor(name) == null) {
                modificationProfile.setColor(name, Color.lightGray);
            }
        }
        if (!missing.isEmpty()) {
            if (missing.size() == 1) {
                JOptionPane.showMessageDialog(this, "The following modification is currently not recognized by PeptideShaker: "
                        + missing.get(0) + ".\nPlease import it by editing the search parameters.", "Modification Not Found", JOptionPane.WARNING_MESSAGE);
            } else {
                String output = "The following modifications are currently not recognized by PeptideShaker:\n";
                boolean first = true;
                for (String ptm : missing) {
                    if (first) {
                        first = false;
                    } else {
                        output += ", ";
                    }
                    output += ptm;
                }
                output += ".\nPlease import it by editing the search parameters.";
                JOptionPane.showMessageDialog(this, output, "Modification Not Found", JOptionPane.WARNING_MESSAGE);
            }
        }

        File fastaFile = searchParameters.getFastaFile();

        if (fastaFile != null) {
            boolean found = false;
            if (fastaFile.exists()) {
                found = true;
            } else {
                // look in the database folder
                try {
                    UtilitiesUserParameters utilitiesUserPreferences = UtilitiesUserParameters.loadUserParameters();
                    File dbFolder = utilitiesUserPreferences.getDbFolder();
                    File newFile = new File(dbFolder, fastaFile.getName());
                    if (newFile.exists()) {
                        fastaFile = newFile;
                        searchParameters.setFastaFile(fastaFile);
                        found = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!found) {
                    // look in the data folders
                    for (File dataFolder : dataFolders) {
                        File newFile = new File(dataFolder, fastaFile.getName());
                        if (newFile.exists()) {
                            fastaFile = newFile;
                            searchParameters.setFastaFile(fastaFile);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // try to find it in the same folder as the SearchGUI.properties file
                        File parentFolder = file.getParentFile();
                        File newFile = new File(parentFolder, fastaFile.getName());
                        if (newFile.exists()) {
                            fastaFile = newFile;
                            searchParameters.setFastaFile(fastaFile);
                            found = true;
                        } else {
                            JOptionPane.showMessageDialog(this, "FASTA file \'" + fastaFile.getName()
                                    + "\' not found.\nPlease locate it manually.", "File Not Found", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            }

            fastaFileTxt.setText(fastaFile.getName());

        }

        boolean matchesValidationAdded;
        ValidationQcParameters validationQCPreferences = tempIdentificationParameters.getIdValidationParameters().getValidationQCParameters();
        if (validationQCPreferences == null
                || validationQCPreferences.getPsmFilters() == null
                || validationQCPreferences.getPeptideFilters() == null
                || validationQCPreferences.getProteinFilters() == null
                || validationQCPreferences.getPsmFilters().isEmpty()
                && validationQCPreferences.getPeptideFilters().isEmpty()
                && validationQCPreferences.getProteinFilters().isEmpty()) {
            MatchesValidator.setDefaultMatchesQCFilters(validationQCPreferences);
            matchesValidationAdded = true;
        } else {
            matchesValidationAdded = false;
        }

        if (!identificationParametersFactory.getParametersList().contains(tempIdentificationParameters.getName())) {
            identificationParametersFactory.addIdentificationParameters(tempIdentificationParameters);
        } else {
            boolean matchesValidationChanged = identificationParametersFactory.getIdentificationParameters(tempIdentificationParameters.getName()).getIdValidationParameters().equals(tempIdentificationParameters.getIdValidationParameters());
            boolean otherSettingsChanged = !identificationParametersFactory.getIdentificationParameters(tempIdentificationParameters.getName()).equals(tempIdentificationParameters);

            if (otherSettingsChanged || matchesValidationChanged && !matchesValidationAdded) {

                int value = JOptionPane.showOptionDialog(null,
                        "A settings file with the name \'" + tempIdentificationParameters.getName() + "\' already exists.\n"
                        + "What do you want to do?",
                        "Identification Settings",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{"Replace File", "Use Existing File", "Keep Both Files"},
                        "default");

                switch (value) {
                    case JOptionPane.YES_OPTION:
                        identificationParametersFactory.addIdentificationParameters(tempIdentificationParameters);
                        break;
                    case JOptionPane.NO_OPTION:
                        tempIdentificationParameters = identificationParametersFactory.getIdentificationParameters(tempIdentificationParameters.getName());
                        break;
                    case JOptionPane.CANCEL_OPTION:
                        tempIdentificationParameters.setName(getIdentificationSettingsFileName(tempIdentificationParameters));
                        identificationParametersFactory.addIdentificationParameters(tempIdentificationParameters);
                        break;
                    default:
                        break;
                }
            } else if (matchesValidationAdded) {
                identificationParametersFactory.addIdentificationParameters(tempIdentificationParameters);
            }
        }

        setIdentificationParameters(tempIdentificationParameters);
    }

    /**
     * Returns the name to use for the identification settings file.
     *
     * @return the name to use for the identification settings file
     */
    private String getIdentificationSettingsFileName(IdentificationParameters tempIdentificationParameters) {

        String name = tempIdentificationParameters.getName();
        int counter = 2;
        String currentName = name;

        while (identificationParametersFactory.getParametersList().contains(currentName)
                && !identificationParametersFactory.getIdentificationParameters(currentName).equals(tempIdentificationParameters)) {
            currentName = name + "_" + counter++;
        }

        return currentName;
    }

    /**
     * Loads the path of the mgf files listed in the given searchGUI input files
     * and provides them in a list without duplicates.
     *
     * @param searchguiInputFiles the SearchGUI input files to inspect
     *
     * @return a list of mgf input files
     */
    private ArrayList<String> getMgfFiles(ArrayList<File> searchguiInputFiles) {
        ArrayList<String> result = new ArrayList<>();
        for (File searchguiInputFile : searchguiInputFiles) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(searchguiInputFile));
                String line;
                while ((line = br.readLine()) != null) {
                    // Skip empty lines.
                    line = line.trim();
                    if (!line.equals("")) {
                        // dirty fix to be able to open windows files on linux/mac and the other way around
                        if (System.getProperty("os.name").lastIndexOf("Windows") == -1) {
                            line = line.replaceAll("\\\\", "/");
                        } else {
                            line = line.replaceAll("/", "\\\\");
                        }
                        if (!result.contains(line)) {
                            result.add(line);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Loads the mgf files listed in SearchGUI input files in the data folders
     * provided.
     *
     * @param inputFiles the SearchGUI input files
     * @param dataFolders the data folders where to look in
     */
    private void loadMgfs(ArrayList<File> inputFiles, ArrayList<File> dataFolders) {

        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Spectrum Files. Please Wait...");

        ArrayList<String> neededMgfs = getMgfFiles(inputFiles);
        ArrayList<String> names = new ArrayList<>();
        String missing = "";
        int nMissing = 0;
        for (File file : spectrumFiles) {
            names.add(file.getName());
        }

        for (String path : neededMgfs) {
            File newFile = new File(path);
            String name = newFile.getName();
            if (!names.contains(newFile.getName())) {
                if (newFile.exists()) {
                    spectrumFiles.add(newFile);
                } else {
                    boolean found = false;
                    for (File folder : dataFolders) {
                        for (File file : folder.listFiles()) {
                            if (file.getName().equals(name)) {
                                spectrumFiles.add(file);
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (!found) {
                        nMissing++;
                        missing += newFile.getName() + "\n";
                    }
                }
            }
        }

        if (nMissing > 0) {
            if (nMissing < 11) {
                JOptionPane.showMessageDialog(this, "Spectrum file(s) not found:\n" + missing
                        + "\nPlease locate them manually.", "Spectrum File Not Found", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Spectrum files not found.\n"
                        + "Please locate them manually.", "Spectrum File Not Found", JOptionPane.WARNING_MESSAGE);
            }
        }

        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
    }

    /**
     * Checks whether the FASTA file loaded contains mainly UniProt concatenated
     * target decoy.
     */
    public void checkFastaFile() {

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        File fastaFile = searchParameters.getFastaFile();
        FastaParameters fastaParameters = searchParameters.getFastaParameters();

        try {

            FastaSummary fastaSummary = FastaSummary.getSummary(fastaFile, fastaParameters, progressDialog);

            Integer nUniprot = fastaSummary.databaseType.get(ProteinDatabase.UniProt);
            int total = fastaSummary.databaseType.values().stream()
                    .mapToInt(a -> a)
                    .sum();

            if (nUniprot == null || ((double) nUniprot) / total < 0.4) {
                showDataBaseHelpDialog();
            }
            if (!fastaParameters.isTargetDecoy()) {
                JOptionPane.showMessageDialog(this, "PeptideShaker validation requires the use of a taget-decoy database.\n"
                        + "Some features will be limited if using other types of databases.\n\n"
                        + "Note that using Automatic Decoy Search in Mascot is not supported.\n\n"
                        + "See the PeptideShaker home page for details.",
                        "No Decoys Found",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (IOException e) {

            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "An error occurred while parsing the fasta file.",
                    "Error while parsing the file",
                    JOptionPane.INFORMATION_MESSAGE);

        }
    }

    /**
     * Show a simple dialog saying that UniProt databases is recommended and
     * display a link to the Database Help web page.
     */
    private void showDataBaseHelpDialog() {
        JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                "We strongly recommend the use of UniProt databases. Some<br>"
                + "features will be limited if using other databases.<br><br>"
                + "See <a href=\"http://compomics.github.io/projects/searchgui/wiki/databasehelp.html\">Database Help</a> for details."),
                "Database Information", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Sets the search parameters in the identification parameters and updates
     * the GUI.
     *
     * @param newIdentificationParameters the new identification parameters
     */
    private void setIdentificationParameters(IdentificationParameters newIdentificationParameters) {

        try {
            ValidationQcParameters validationQCPreferences = newIdentificationParameters.getIdValidationParameters().getValidationQCParameters();
            if (validationQCPreferences == null
                    || validationQCPreferences.getPsmFilters() == null
                    || validationQCPreferences.getPeptideFilters() == null
                    || validationQCPreferences.getProteinFilters() == null
                    || validationQCPreferences.getPsmFilters().isEmpty()
                    && validationQCPreferences.getPeptideFilters().isEmpty()
                    && validationQCPreferences.getProteinFilters().isEmpty()) {
                MatchesValidator.setDefaultMatchesQCFilters(validationQCPreferences);
                identificationParametersFactory.addIdentificationParameters(newIdentificationParameters);
            }

            this.identificationParameters = newIdentificationParameters;

            Vector parameterList = new Vector();
            parameterList.add("-- Select --");

            for (String tempParameters : identificationParametersFactory.getParametersList()) {
                parameterList.add(tempParameters);
            }

            settingsComboBox.setModel(new javax.swing.DefaultComboBoxModel(parameterList));
            settingsComboBox.setSelectedItem(identificationParameters.getName());

            File fastaFile = identificationParameters.getSearchParameters().getFastaFile();
            if (fastaFile != null) {
                fastaFileTxt.setText(fastaFile.getName());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to import identification parameters from: " + newIdentificationParameters.getName() + ".", "Identification Parameters",
                    JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();

            // set the search settings to default
            this.identificationParameters = null;
        }

        validateInput();
    }

    @Override
    public void setIconImage(Image image) {
        super.setIconImage(image);
        if (welcomeDialog != null) {
            welcomeDialog.setIconImage(image);
        }
    }

    /**
     * Loads the identification files collected and related information.
     *
     * @param selectedFiles the files selected by the user
     */
    private void loadIdInputFiles(File[] selectedFiles) {

        HashMap<String, File> parameterFiles = new HashMap<>();
        ArrayList<File> dataFolders = new ArrayList<>();
        ArrayList<File> inputFiles = new ArrayList<>();

        boolean loadCanceled = false;

        for (File newFile : selectedFiles) {
            if (newFile.isDirectory()) {

                if (!dataFolders.contains(newFile)) {
                    dataFolders.add(newFile);
                }

                File dataFolder = new File(newFile, PeptideShaker.DATA_DIRECTORY);
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(newFile, "mgf");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(newFile, "fasta");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }

                File[] tempFiles = newFile.listFiles();
                for (File file : tempFiles) {
                    String lowerCaseName = file.getName().toLowerCase();
                    if (lowerCaseName.endsWith(".zip")) {
                        loadCanceled = !loadZipFile(file, parameterFiles, dataFolders, inputFiles);
                        if (loadCanceled) {
                            break;
                        }
                    } else {
                        loadIdFile(file, parameterFiles, inputFiles);
                    }
                }
            } else {
                File parentFolder = newFile.getParentFile();
                if (!dataFolders.contains(parentFolder)) {
                    dataFolders.add(parentFolder);
                }

                File dataFolder = new File(parentFolder, PeptideShaker.DATA_DIRECTORY);
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(parentFolder, "mgf");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(parentFolder, "fasta");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }

                String lowerCaseName = newFile.getName().toLowerCase();
                if (lowerCaseName.endsWith(".zip")) {
                    loadCanceled = !loadZipFile(newFile, parameterFiles, dataFolders, inputFiles);
                    if (loadCanceled) {
                        break;
                    }
                } else {
                    loadIdFile(newFile, parameterFiles, inputFiles);
                }

                for (File file : newFile.getParentFile().listFiles()) {
                    String name = file.getName();
                    if (name.equals(SEARCHGUI_INPUT)) {
                        inputFiles.add(file);
                    } else if (name.toLowerCase().endsWith(".par")) {
                        parameterFiles.put(name, file);
                    }
                    if (file.getName().endsWith("usermods.xml")) {
                        modificationFiles.add(file);
                    }
                }
            }

            peptideShakerGUI.getLastSelectedFolder().setLastSelectedFolder(newFile.getAbsolutePath());

            if (loadCanceled) {
                break;
            }
        }

        if (!loadCanceled) {

            File parameterFile = null;
            ArrayList<String> names = new ArrayList<>(parameterFiles.keySet());
            if (parameterFiles.size() == 1) {
                ArrayList<String> fileNames = new ArrayList<>(parameterFiles.keySet());
                parameterFile = parameterFiles.get(fileNames.get(0));
            } else if (parameterFiles.size() > 1) {

                boolean equalParameters = true;

                try {
                    IdentificationParameters identificationParameters0 = IdentificationParameters.getIdentificationParameters(parameterFiles.get(names.get(0)));
                    for (int i = 1; i < names.size(); i++) {
                        IdentificationParameters identificationParametersI = IdentificationParameters.getIdentificationParameters(parameterFiles.get(names.get(i)));
                        if (!identificationParameters0.equals(identificationParametersI)) {
                            equalParameters = false;
                            break;
                        }
                    }
                } catch (ClassNotFoundException e) {
                    equalParameters = false;
                } catch (IOException e) {
                    equalParameters = false;
                }

                if (equalParameters) {
                    // all parameters are equal, just select one of them
                    parameterFile = parameterFiles.get(names.get(0));
                } else {
                    setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    ArrayList<File> parameterFilesList = new ArrayList<>(parameterFiles.values());
                    FileSelectionDialog fileSelection = new FileSelectionDialog(this, parameterFilesList, "Select the wanted SearchGUI parameters file.");
                    if (!fileSelection.isCanceled()) {
                        parameterFile = fileSelection.getSelectedFile();
                    }
                    setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
                }
            }

            loadMgfs(inputFiles, dataFolders);

            idFilesTxt.setText(idFiles.size() + " file(s) selected");

            if (parameterFile != null) {
                try {
                    importSearchParameters(parameterFile, dataFolders, progressDialog);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error occurred while reading " + parameterFile + ". Please verify the search parameters.", "File error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }

            progressDialog.setRunFinished();
            validateInput();
        } else {
            progressDialog.setRunFinished();
            validateInput();
        }
    }

    /**
     * Unzips and loads the identification files from a compressed folder. Files
     * in sub folders will be ignored.
     *
     * @param file the zip file to load
     * @param parameterFiles a map of the parameters files found indexed by name
     * @param dataFolders list of the folders where the mgf and FASTA files
     * could possibly be
     * @param inputFiles list of the input files found
     * @return true of the zipping completed without any issues
     */
    private boolean loadZipFile(File file, HashMap<String, File> parameterFiles, ArrayList<File> dataFolders, ArrayList<File> inputFiles) {

        String newName = PsZipUtils.getTempFolderName(file.getName());
        String parentFolder = PsZipUtils.getUnzipParentFolder();
        if (parentFolder == null) {
            parentFolder = file.getParent();
        }
        File parentFolderFile = new File(parentFolder, PsZipUtils.getUnzipSubFolder());
        File destinationFolder = new File(parentFolderFile, newName);
        destinationFolder.mkdir();
        TempFilesManager.registerTempFolder(parentFolderFile);

        progressDialog.setWaitingText("Unzipping " + file.getName() + ". Please Wait...");

        try {
            ZipUtils.unzip(file, destinationFolder, progressDialog);
            progressDialog.setSecondaryProgressCounterIndeterminate(true);
            if (!progressDialog.isRunCanceled()) {
                File dataFolder = new File(destinationFolder, PeptideShaker.DATA_DIRECTORY);
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(destinationFolder, "mgf");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                dataFolder = new File(destinationFolder, "fasta");
                if (dataFolder.exists() && !dataFolders.contains(dataFolder)) {
                    dataFolders.add(dataFolder);
                }
                for (File zippedFile : destinationFolder.listFiles()) {
                    loadIdFile(zippedFile, parameterFiles, inputFiles);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(peptideShakerGUI,
                    e.getMessage(),
                    "Unzip Error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
            idFiles.clear();
            modificationFiles.clear();
            return false;
        }

        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Files. Please Wait...");

        return !progressDialog.isRunCanceled();
    }

    /**
     * Loads the given identification file in the file list.
     *
     * @param file the identification file to load
     * @param parameterFiles a map of the parameters files found indexed by name
     * @param inputFiles list of the input files found
     */
    private void loadIdFile(File file, HashMap<String, File> parameterFiles, ArrayList<File> inputFiles) {

        // add searchGUI_input.txt
        if (file.getName().equals(SEARCHGUI_INPUT)) {
            inputFiles.add(file);
        }

        String lowerCaseName = file.getName().toLowerCase();

        if (lowerCaseName.endsWith(".dat")
                || lowerCaseName.endsWith(".omx")
                || lowerCaseName.endsWith(".res")
                || lowerCaseName.endsWith(".xml")
                || lowerCaseName.endsWith(".mzid")
                || lowerCaseName.endsWith(".csv")
                || lowerCaseName.endsWith(".tags")
                || lowerCaseName.endsWith(".pnovo.txt")
                || lowerCaseName.endsWith(".tide-search.target.txt")
                || lowerCaseName.endsWith(".psm")) {
            if (!lowerCaseName.endsWith("mods.xml")
                    && !lowerCaseName.endsWith("usermods.xml")
                    && !lowerCaseName.endsWith("settings.xml")) {
                idFiles.add(file);
            } else if (lowerCaseName.endsWith("usermods.xml")) {
                modificationFiles.add(file);
            }
        } else if (lowerCaseName.endsWith(".par")) {
            parameterFiles.put(file.getName(), file);
        }
    }
}
