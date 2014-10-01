package eu.isas.peptideshaker.gui;

import com.compomics.util.gui.filehandling.FileDisplayDialog;
import com.compomics.util.gui.gene_mapping.SpeciesDialog;
import com.compomics.util.gui.filehandling.FileSelectionDialog;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.io.identifications.IdentificationParametersReader;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.protein.SequenceDbDetailsDialog;
import com.compomics.util.gui.searchsettings.SearchSettingsDialog;
import com.compomics.util.gui.searchsettings.SearchSettingsDialogParent;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.io.compression.ZipUtils;
import com.compomics.util.messages.FeedBack;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.ModificationProfile;
import eu.isas.peptideshaker.PeptideShaker;
import com.compomics.util.preferences.gui.ImportSettingsDialog;
import com.compomics.util.preferences.gui.ProcessingPreferencesDialog;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import com.compomics.util.protein.Header.DatabaseType;
import eu.isas.peptideshaker.fileimport.FileImporter;
import eu.isas.peptideshaker.utils.Tips;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

/**
 * A dialog for selecting the files to load.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class NewDialog extends javax.swing.JDialog implements SearchSettingsDialogParent {

    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance(30000);
    /**
     * The experiment conducted.
     */
    private MsExperiment experiment = null;
    /**
     * The sample analyzed.
     */
    private Sample sample;
    /**
     * The replicate number.
     */
    private int replicateNumber;
    /**
     * A reference to the main frame.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The list of identification files.
     */
    private ArrayList<File> idFiles = new ArrayList<File>();
    /**
     * The XML modification files found.
     */
    private ArrayList<File> modificationFiles = new ArrayList<File>();
    /**
     * A file where the input will be stored.
     */
    public final static String SEARCHGUI_INPUT = "searchGUI_input.txt";
    /**
     * The list of spectrum files.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<File>();
    /**
     * The peptide shaker class which will take care of the pre-processing.
     */
    private PeptideShaker peptideShaker;
    /**
     * The processing preferences.
     */
    private ProcessingPreferences processingPreferences = new ProcessingPreferences();
    /**
     * The PTM scoring preferences.
     */
    private PTMScoringPreferences ptmScoringPreferences = new PTMScoringPreferences();
    /**
     * The filter to use for matches filtering.
     */
    private IdFilter idFilter = new IdFilter();
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The search parameters corresponding to the files selected.
     */
    private SearchParameters searchParameters = new SearchParameters();
    /**
     * The gene preferences.
     */
    private GenePreferences genePreferences;
    /*
     * The welcome dialog parent, can be null.
     */
    private WelcomeDialog welcomeDialog;
    /**
     * The FASTA file of the currently loaded project if any.
     */
    private File currentFastaFile = null;

    /**
     * Creates a new open dialog.
     *
     * @param peptideShaker a reference to the main frame
     * @param modal boolean indicating whether the dialog is modal
     */
    public NewDialog(PeptideShakerGUI peptideShaker, boolean modal) {
        super(peptideShaker, modal);
        this.peptideShakerGUI = peptideShaker;
        this.welcomeDialog = null;
        this.genePreferences = peptideShaker.getGenePreferences();
        currentFastaFile = sequenceFactory.getCurrentFastaFile();
        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        this.setLocationRelativeTo(peptideShaker);
        setVisible(true);
    }

    /**
     * Creates a new open dialog.
     *
     * @param welcomeDialog the welcome dialog parent frame
     * @param peptideShaker a reference to the main frame
     * @param modal boolean indicating whether the dialog is modal
     */
    public NewDialog(WelcomeDialog welcomeDialog, PeptideShakerGUI peptideShaker, boolean modal) {
        super(welcomeDialog, modal);
        this.peptideShakerGUI = peptideShaker;
        this.welcomeDialog = welcomeDialog;
        this.genePreferences = peptideShaker.getGenePreferences();
        currentFastaFile = sequenceFactory.getCurrentFastaFile();
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
        idFilesTxt.setText(idFiles.size() + " file(s) selected");
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
        fastaFileTxt.setText("");
        if (genePreferences.getCurrentSpecies() != null) {
            speciesTextField.setText(genePreferences.getCurrentSpecies());
        } else {
            speciesTextField.setText("(not selected)");
        }
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
        openButton = new javax.swing.JButton();
        projectDetailsPanel = new javax.swing.JPanel();
        replicateNumberIdtxt = new javax.swing.JTextField();
        projectNameIdTxt = new javax.swing.JTextField();
        replicateLabel = new javax.swing.JLabel();
        sampleNameLabel = new javax.swing.JLabel();
        projectReferenceLabel = new javax.swing.JLabel();
        sampleNameIdtxt = new javax.swing.JTextField();
        speciesLabel = new javax.swing.JLabel();
        speciesTextField = new javax.swing.JTextField();
        editSpeciesButton = new javax.swing.JButton();
        processingParametersPanel = new javax.swing.JPanel();
        importFilterTxt = new javax.swing.JTextField();
        importFiltersLabel = new javax.swing.JLabel();
        searchParamsLabel = new javax.swing.JLabel();
        searchTxt = new javax.swing.JTextField();
        editSearchButton = new javax.swing.JButton();
        editImportFilterButton = new javax.swing.JButton();
        importFiltersLabel1 = new javax.swing.JLabel();
        preferencesTxt = new javax.swing.JTextField();
        editPreferencesButton = new javax.swing.JButton();
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
        peptideShakerHomePageLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PeptideShaker - New Project");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        sampleDetailsPanel.setBackground(new java.awt.Color(230, 230, 230));

        openButton.setBackground(new java.awt.Color(0, 153, 0));
        openButton.setFont(openButton.getFont().deriveFont(openButton.getFont().getStyle() | java.awt.Font.BOLD));
        openButton.setForeground(new java.awt.Color(255, 255, 255));
        openButton.setText("Load Data!");
        openButton.setEnabled(false);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
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

        speciesLabel.setText("Species");
        speciesLabel.setToolTipText("Set the species to get gene annotations");

        speciesTextField.setEditable(false);
        speciesTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        speciesTextField.setToolTipText("Set the species to get gene annotations");

        editSpeciesButton.setText("Edit");
        editSpeciesButton.setToolTipText("Set the species");
        editSpeciesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSpeciesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout projectDetailsPanelLayout = new javax.swing.GroupLayout(projectDetailsPanel);
        projectDetailsPanel.setLayout(projectDetailsPanelLayout);
        projectDetailsPanelLayout.setHorizontalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(projectReferenceLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sampleNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(speciesLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectNameIdTxt)
                    .addComponent(sampleNameIdtxt)
                    .addComponent(speciesTextField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                        .addComponent(replicateLabel)
                        .addGap(18, 18, 18)
                        .addComponent(replicateNumberIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(editSpeciesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(speciesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editSpeciesButton)
                    .addComponent(speciesLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        processingParametersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Processing Parameters"));
        processingParametersPanel.setOpaque(false);

        importFilterTxt.setEditable(false);
        importFilterTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        importFilterTxt.setText("Default");

        importFiltersLabel.setText("Import Filters");

        searchParamsLabel.setText("Search Settings");

        searchTxt.setEditable(false);
        searchTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        searchTxt.setText("Default");

        editSearchButton.setText("Edit");
        editSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSearchButtonActionPerformed(evt);
            }
        });

        editImportFilterButton.setText("Edit");
        editImportFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editImportFilterButtonActionPerformed(evt);
            }
        });

        importFiltersLabel1.setText("Preferences");

        preferencesTxt.setEditable(false);
        preferencesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        preferencesTxt.setText("Default");

        editPreferencesButton.setText("Edit");
        editPreferencesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editPreferencesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout processingParametersPanelLayout = new javax.swing.GroupLayout(processingParametersPanel);
        processingParametersPanel.setLayout(processingParametersPanelLayout);
        processingParametersPanelLayout.setHorizontalGroup(
            processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingParametersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(importFiltersLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                        .addComponent(searchParamsLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                    .addComponent(importFiltersLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importFilterTxt)
                    .addComponent(preferencesTxt)
                    .addComponent(searchTxt, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(editPreferencesButton, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                    .addComponent(editSearchButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                    .addComponent(editImportFilterButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE))
                .addContainerGap())
        );
        processingParametersPanelLayout.setVerticalGroup(
            processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingParametersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchParamsLabel)
                    .addComponent(searchTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editSearchButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(importFiltersLabel)
                    .addComponent(importFilterTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editImportFilterButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(importFiltersLabel1)
                    .addComponent(preferencesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editPreferencesButton))
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
        databaseLabel.setText("Database File (FASTA)*");

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

        peptideShakerHomePageLabel.setText("<html>Please cite PeptideShaker as <a href=\"http://peptide-shaker.googlecode.com\">PeptideShaker (http://peptide-shaker.googlecode.com)</a>.</html>\n\n");
        peptideShakerHomePageLabel.setToolTipText("Open the PeptideShaker web page");
        peptideShakerHomePageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                peptideShakerHomePageLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                peptideShakerHomePageLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideShakerHomePageLabelMouseExited(evt);
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
                        .addGap(71, 71, 71)
                        .addComponent(peptideShakerHomePageLabel)
                        .addGap(44, 44, 44)
                        .addComponent(openButton, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(peptideShakerHomePageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(openButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
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
            .addComponent(sampleDetailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Tries to process the identification files, closes the dialog and then
     * opens the results in the main frame.
     *
     * @param evt
     */
    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed

        // check if default search parameters are used
        if (searchTxt.getText().equalsIgnoreCase("Default")) {
            int value = JOptionPane.showConfirmDialog(this,
                    "It seems like you are using the default search parameters without any PTMs.\nContinue anyway?",
                    "Default Search Parameters?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (value != JOptionPane.YES_OPTION) {
                editSearchButtonActionPerformed(null);
                return;
            } else {
                // set the default enzyme
                searchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
            }
        }

        if (validateUserInput()) {

            if (welcomeDialog != null) {
                welcomeDialog.setVisible(false);
            }

            this.setVisible(false);
            peptideShakerGUI.setVisible(true);
            peptideShakerGUI.clearData(true, false);
            peptideShakerGUI.setDefaultPreferences();
            peptideShakerGUI.setGenePreferences(genePreferences);
            peptideShakerGUI.setSearchParameters(searchParameters);
            peptideShakerGUI.updateAnnotationPreferencesFromSearchSettings();
            peptideShakerGUI.setProjectDetails(getProjectDetails());
            peptideShakerGUI.setCurentNotes(new ArrayList<String>());
            peptideShakerGUI.updateNotesNotificationCounter();
            peptideShakerGUI.resetDisplayFeaturesGenerator();

            experiment = new MsExperiment(projectNameIdTxt.getText().trim());
            sample = new Sample(sampleNameIdtxt.getText().trim());
            replicateNumber = getReplicateNumber();
            SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(replicateNumber));
            experiment.addAnalysisSet(sample, analysisSet);

            peptideShaker = new PeptideShaker(experiment, sample, replicateNumber);

            ArrayList<String> tips;
            try {
                tips = Tips.getTips();
            } catch (Exception e) {
                tips = new ArrayList<String>();
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
            progressCounter += 2; // resolving protein inference
            progressCounter += 4; // Correcting protein probabilities, Validating identifications at 1% FDR, Scoring PTMs in peptides, Scoring PTMs in proteins.
            progressCounter += 3; // Scoring PTMs in PSMs. Estimating PTM FLR. Resolving peptide inference issues.

            // add one more just to not start at 0%
            progressCounter++;

            waitingDialog.setMaxPrimaryProgressCounter(progressCounter);
            waitingDialog.increasePrimaryProgressCounter(); // just to not start at 0%

            boolean needDialog = false;

            // load the identification files
            if (idFiles.size() > 0
                    || searchParameters != null
                    || searchParameters.getFastaFile() != null
                    || spectrumFiles.size() > 0) {
                needDialog = true;
                importIdentificationFiles(waitingDialog);
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

                // show the warnings
                Iterator<String> iterator = peptideShaker.getWarnings().keySet().iterator();
                int counter = 0;
                while (iterator.hasNext()) {
                    FeedBack warning = peptideShaker.getWarnings().get(iterator.next());
                    if (warning.getType() == FeedBack.FeedBackType.WARNING) {
                        peptideShakerGUI.addNote("<b>" + ++counter + " " + warning.getTitle() + "</b><br><br>" + warning.getMessage()); // @TODO: better interaction between notes and feedback objetcs...
                    }
                }

                peptideShakerGUI.setProcessingPreferences(processingPreferences);
                peptideShakerGUI.setPtmScoringPreferences(ptmScoringPreferences);
                peptideShakerGUI.setIdFilter(idFilter);
                peptideShakerGUI.updateAnnotationPreferencesFromSearchSettings();
                peptideShakerGUI.setProject(experiment, sample, replicateNumber);
                peptideShakerGUI.setMetrics(peptideShaker.getMetrics());
                peptideShakerGUI.setIdentificationFeaturesGenerator(peptideShaker.getIdentificationFeaturesGenerator());
                peptideShakerGUI.setCache(peptideShaker.getCache());
                peptideShakerGUI.setUpInitialFilters();
                peptideShakerGUI.displayResults();
                peptideShakerGUI.initiateDisplay(); // display the overview tab
                peptideShakerGUI.getProjectDetails().setReport(waitingDialog.getReport(null));
                this.dispose();
            } else if (waitingDialog.isRunCanceled()) {

                // close the database
                try {
                    ProteomicAnalysis proteomicAnalysis = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber);
                    Identification identification = proteomicAnalysis.getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
                    identification.close();
                } catch (SQLException e) {
                    System.out.println("Failed to close the database!");
                    e.printStackTrace();
                }
            }
        }
}//GEN-LAST:event_openButtonActionPerformed

    /**
     * Clear the database field.
     *
     * @param evt
     */
    private void clearDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearDbButtonActionPerformed
        fastaFileTxt.setText("");
        try {
            sequenceFactory.clearFactory();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to clear the sequence factory.", "File Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to clear the sequence factory.", "File Error", JOptionPane.WARNING_MESSAGE);
        }
        validateInput();
}//GEN-LAST:event_clearDbButtonActionPerformed

    /**
     * Opens a file chooser where the user can select the database FASTA file to
     * use.
     *
     * @param evt
     */
    private void browseDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDbButtonActionPerformed

        SequenceDbDetailsDialog sequenceDbDetailsDialog = new SequenceDbDetailsDialog(peptideShakerGUI, peptideShakerGUI.getLastSelectedFolder(), true,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        boolean success = sequenceDbDetailsDialog.selectDB(true);
        if (success) {
            sequenceDbDetailsDialog.setVisible(true);
        }

        peptideShakerGUI.setLastSelectedFolder(sequenceDbDetailsDialog.getLastSelectedFolder());

        if (sequenceFactory.getCurrentFastaFile() != null) {
            fastaFileTxt.setText(sequenceFactory.getFileName());
            checkFastaFile();
            if (searchParameters == null) {
                searchParameters = new SearchParameters();
                searchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
            }
            searchParameters.setFastaFile(sequenceFactory.getCurrentFastaFile());
        }
        validateInput();
}//GEN-LAST:event_browseDbButtonActionPerformed

    /**
     * Clear the spectra selection.
     *
     * @param evt
     */
    private void clearSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectraActionPerformed
        spectrumFiles = new ArrayList<File>();
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
        JFileChooser fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());
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
        idFiles = new ArrayList<File>();
        idFilesTxt.setText(idFiles.size() + " file(s) selected");
        searchParameters = new SearchParameters();
        validateInput();
}//GEN-LAST:event_clearIdActionPerformed

    /**
     * Open a file chooser for selecting identification files.
     *
     * @param evt
     */
    private void browseIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIdActionPerformed

        final JFileChooser fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());
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

                return myFile.getName().toLowerCase().endsWith("omx")
                        || myFile.getName().toLowerCase().endsWith("t.xml")
                        || myFile.getName().toLowerCase().endsWith("dat")
                        || myFile.getName().toLowerCase().endsWith("mzid")
                        || myFile.getName().toLowerCase().endsWith("csv")
                        || myFile.getName().toLowerCase().endsWith("tags")
                        || myFile.getName().toLowerCase().endsWith("zip")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "mzIdentML (.mzid), OMSSA (.omx), X!Tandem (.xml), MS Amanda (.csv) and Mascot (.dat)"; // @TODO: add directag
            }
        };

        // filter for zip folders only
        FileFilter zipFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith("zip")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "compressed zip folder (.zip)";
            }
        };

        // filter for omssa only
        FileFilter omssaFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith("omx")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "OMSSA (.omx)";
            }
        };

        // filter for x!tandem only
        FileFilter tandemFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith("t.xml")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "X!Tandem (.xml)";
            }
        };

        // filter for mzIdentML only
        FileFilter mzidFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith("mzid")
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

                return myFile.getName().toLowerCase().endsWith("csv")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "MS Amanda (.csv)";
            }
        };

        // filter for DirecTag only
        FileFilter direcTagFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith("tags")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "DirecTag (.tags)";
            }
        };

        // filter for mascot only
        FileFilter mascotFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith("dat")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Mascot (.dat)";
            }
        };

        fileChooser.setFileFilter(allFilter);
        fileChooser.addChoosableFileFilter(zipFilter);
        fileChooser.addChoosableFileFilter(mzidFilter);
        fileChooser.addChoosableFileFilter(omssaFilter);
        fileChooser.addChoosableFileFilter(tandemFilter);
        fileChooser.addChoosableFileFilter(msAmandaFilter);
        fileChooser.addChoosableFileFilter(mascotFilter);
        fileChooser.addChoosableFileFilter(direcTagFilter);

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

        validateInput();
}//GEN-LAST:event_browseIdActionPerformed

    /**
     * Open the SearchSettingsDialog dialog.
     *
     * @param evt
     */
    private void editSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSearchButtonActionPerformed

        // set the default enzyme if not set
        if (searchParameters.getEnzyme() == null) {
            searchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
        }

        new SearchSettingsDialog(peptideShakerGUI, this, searchParameters,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true, true);
    }//GEN-LAST:event_editSearchButtonActionPerformed

    /**
     * Open the ImportSettingsDialog.
     *
     * @param evt
     */
    private void editImportFilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editImportFilterButtonActionPerformed
        ImportSettingsDialog importSettingsDialog = new ImportSettingsDialog(this, peptideShakerGUI.getIdFilter(), true);
        IdFilter newFilter = importSettingsDialog.getFilter();
        if (newFilter != null) {
            importFilterTxt.setText("User Defined");
            idFilter = newFilter;
        }
    }//GEN-LAST:event_editImportFilterButtonActionPerformed

    /**
     * Closes the dialog.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (currentFastaFile == null) {
            try {
                sequenceFactory.clearFactory();
                genePreferences.setCurrentSpeciesType(null);
                genePreferences.setCurrentSpecies(null);
                TempFilesManager.deleteTempFolders();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to clear the database.", "File Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to clear the database.", "File Error", JOptionPane.WARNING_MESSAGE);
            }
        } else if (!currentFastaFile.equals(sequenceFactory.getCurrentFastaFile()) && currentFastaFile.exists()) {
            loadFastaFile(currentFastaFile, null);
        }
        this.setVisible(false);
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
    private void editPreferencesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editPreferencesButtonActionPerformed
        new ProcessingPreferencesDialog(peptideShakerGUI, true, processingPreferences, ptmScoringPreferences);
        if (processingPreferences.getProteinFDR() != 1
                || processingPreferences.getPeptideFDR() != 1
                || processingPreferences.getPsmFDR() != 1
                || ptmScoringPreferences.getFlrThreshold() != 1
                || ptmScoringPreferences.isProbabilisticScoreNeutralLosses()
                || !ptmScoringPreferences.isEstimateFlr()) {
            preferencesTxt.setText("User Defined");
        } else if (!ptmScoringPreferences.isProbabilitsticScoreCalculation()) {
            preferencesTxt.setText("Reduced PTM Scoring");
        } else {
            preferencesTxt.setText("Default");
        }
    }//GEN-LAST:event_editPreferencesButtonActionPerformed

    /**
     * Open the species selection dialog.
     *
     * @param evt
     */
    private void editSpeciesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSpeciesButtonActionPerformed
        new SpeciesDialog(this, peptideShakerGUI, genePreferences, true, peptideShakerGUI.getWaitingIcon(), peptideShakerGUI.getNormalIcon());
        if (genePreferences.getCurrentSpecies() != null) {
            speciesTextField.setText(genePreferences.getCurrentSpecies());
        } else {
            speciesTextField.setText("(not selected)");
        }
    }//GEN-LAST:event_editSpeciesButtonActionPerformed

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
        BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    /**
     * Open the PeptideShaker web page.
     *
     * @param evt
     */
    private void peptideShakerHomePageLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerHomePageLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://peptide-shaker.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerHomePageLabelMouseClicked

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void peptideShakerHomePageLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerHomePageLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptideShakerHomePageLabelMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptideShakerHomePageLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerHomePageLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerHomePageLabelMouseExited

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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JButton browseDbButton;
    private javax.swing.JButton browseId;
    private javax.swing.JButton browseSpectra;
    private javax.swing.JButton clearDbButton;
    private javax.swing.JButton clearId;
    private javax.swing.JButton clearSpectra;
    private javax.swing.JLabel databaseLabel;
    private javax.swing.JButton editImportFilterButton;
    private javax.swing.JButton editPreferencesButton;
    private javax.swing.JButton editSearchButton;
    private javax.swing.JButton editSpeciesButton;
    private javax.swing.JTextField fastaFileTxt;
    private javax.swing.JLabel idFilesLabel;
    private javax.swing.JTextField idFilesTxt;
    private javax.swing.JTextField importFilterTxt;
    private javax.swing.JLabel importFiltersLabel;
    private javax.swing.JLabel importFiltersLabel1;
    private javax.swing.JPanel inputFilesPanel;
    private javax.swing.JButton openButton;
    private javax.swing.JLabel peptideShakerHomePageLabel;
    private javax.swing.JTextField preferencesTxt;
    private javax.swing.JPanel processingParametersPanel;
    private javax.swing.JPanel projectDetailsPanel;
    private javax.swing.JTextField projectNameIdTxt;
    private javax.swing.JLabel projectReferenceLabel;
    private javax.swing.JLabel replicateLabel;
    private javax.swing.JTextField replicateNumberIdtxt;
    private javax.swing.JPanel sampleDetailsPanel;
    private javax.swing.JTextField sampleNameIdtxt;
    private javax.swing.JLabel sampleNameLabel;
    private javax.swing.JLabel searchParamsLabel;
    private javax.swing.JTextField searchTxt;
    private javax.swing.JLabel speciesLabel;
    private javax.swing.JTextField speciesTextField;
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

        if (fastaFileTxt.getText().length() > 0) {
            databaseLabel.setForeground(Color.BLACK);
            databaseLabel.setToolTipText(null);
            fastaFileTxt.setToolTipText(null);
        } else {
            databaseLabel.setForeground(Color.RED);
            databaseLabel.setToolTipText("Please select the database file used");
            fastaFileTxt.setToolTipText("Please select the database file used");
            allValid = false;
        }

        // enable/disable the Create! button
        openButton.setEnabled(allValid);
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
     * Imports identifications from identification files.
     *
     * @param waitingDialog a dialog to display feedback to the user
     */
    private void importIdentificationFiles(WaitingDialog waitingDialog) {
        peptideShaker.importFiles(waitingDialog, idFilter, idFiles,
                spectrumFiles, searchParameters,
                peptideShakerGUI.getAnnotationPreferences(), peptideShakerGUI.getProjectDetails(),
                processingPreferences, ptmScoringPreferences, peptideShakerGUI.getSpectrumCountingPreferences(),
                SequenceMatchingPreferences.getDefaultSequenceMatching(searchParameters), true);
    }

    /**
     * Imports the search parameters from a SearchGUI file.
     *
     * @param file the selected searchGUI file
     * @param dataFolders folders where to look for the FASTA file
     * @param progressDialog the progress dialog
     */
    public void importSearchParameters(File file, ArrayList<File> dataFolders, ProgressDialogX progressDialog) {

        progressDialog.setTitle("Importing Search Parameters. Please Wait...");

        try {
            searchParameters = SearchParameters.getIdentificationParameters(file);
            PeptideShaker.loadModifications(searchParameters);
        } catch (Exception e) {
            try {
                // Old school format, overwrite old file
                Properties props = loadProperties(file);
                // We need the user mods file, try to see if it is along the search parameters or use the PeptideShaker version
                File userMods = new File(file.getParent(), "usermods.xml");
                if (!userMods.exists()) {
                    userMods = new File(peptideShakerGUI.getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE);
                }
                searchParameters = IdentificationParametersReader.getSearchParameters(props, userMods);

                String fileName = file.getName();
                if (fileName.endsWith(".properties")) {
                    String newName = fileName.substring(0, fileName.lastIndexOf(".")) + ".parameters";
                    try {
                        file.delete();
                    } catch (Exception deleteException) {
                        deleteException.printStackTrace();
                    }
                    file = new File(file.getParentFile(), newName);
                }
                SearchParameters.saveIdentificationParameters(searchParameters, file);
            } catch (Exception saveException) {
                e.printStackTrace();
                saveException.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error occured while reading " + file + ". Please verify the search paramters.", "File error", JOptionPane.ERROR_MESSAGE);
            }
        }

        ModificationProfile modificationProfile = searchParameters.getModificationProfile();

        ArrayList<String> missing = new ArrayList<String>();

        for (String name : modificationProfile.getAllNotFixedModifications()) {
            if (!ptmFactory.containsPTM(name)) {
                missing.add(name);
            } else {
                if (modificationProfile.getColor(name) == null) {
                    searchParameters.getModificationProfile().setColor(name, Color.lightGray);
                }
            }
        }
        if (!missing.isEmpty()) {
            // Might happen with old parameters files or when no parameter file is found
            for (File modFile : modificationFiles) {
                try {
                    ptmFactory.importModifications(modFile, true);
                } catch (Exception e) {
                    // ignore error
                }
            }
            ArrayList<String> missing2 = new ArrayList<String>();
            for (String ptmName : missing) {
                if (!ptmFactory.containsPTM(ptmName)) {
                    missing2.add(ptmName);
                }
            }
            if (!missing2.isEmpty()) {
                if (missing2.size() == 1) {
                    JOptionPane.showMessageDialog(this, "The following modification is currently not recognized by PeptideShaker: "
                            + missing2.get(0) + ".\nPlease import it by editing the search parameters.", "Modification Not Found", JOptionPane.WARNING_MESSAGE);
                } else {
                    String output = "The following modifications are currently not recognized by PeptideShaker:\n";
                    boolean first = true;
                    for (String ptm : missing2) {
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
        }

        File fastaFile = searchParameters.getFastaFile();
        if (fastaFile != null) {
            boolean found = false;
            if (fastaFile.exists()) {
                found = true;
            } else {
                // look in the database folder
                try {
                    UtilitiesUserPreferences utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
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
            if (found) {
                loadFastaFile(fastaFile, progressDialog);
                fastaFileTxt.setText(fastaFile.getName());
            }
        }

        searchTxt.setText(file.getName().substring(0, file.getName().lastIndexOf(".")));
        importFilterTxt.setText(file.getName().substring(0, file.getName().lastIndexOf(".")));
    }

    /**
     * This method loads the necessary parameters for populating (part of) the
     * GUI from a properties file.
     *
     * @deprecated use SearchParameters instead
     * @param aFile File with the relevant properties file.
     * @return Properties with the loaded properties.
     */
    private Properties loadProperties(File aFile) {
        Properties screenProps = new Properties();
        try {
            FileInputStream fis = new FileInputStream(aFile);
            if (fis != null) {
                screenProps.load(fis);
                fis.close();
            } else {
                throw new IllegalArgumentException("Could not read the file you specified ('" + aFile.getAbsolutePath() + "').");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to read file: " + aFile.getName() + ".\n" + ioe.getMessage(), "Error Reading File", JOptionPane.WARNING_MESSAGE);
        }
        return screenProps;
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
        ArrayList<String> result = new ArrayList<String>();
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
        ArrayList<String> names = new ArrayList<String>();
        String missing = "";
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
                        missing += newFile.getName() + "\n";
                    }
                }
            }
        }

        if (!missing.equals("")) {
            JOptionPane.showMessageDialog(this, "Input file(s) not found:\n" + missing
                    + "\nPlease locate them manually.", "File Not Found", JOptionPane.WARNING_MESSAGE);
        }

        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
    }

    /**
     * Checks whether the FASTA file loaded is UniProt concatenated target
     * decoy.
     */
    public void checkFastaFile() {
        if (sequenceFactory.getCurrentFastaIndex().getDatabaseType() != DatabaseType.UniProt) {
            showDataBaseHelpDialog();
        }
        if (!sequenceFactory.concatenatedTargetDecoy()) {
            JOptionPane.showMessageDialog(this, "PeptideShaker validation requires the use of a taget-decoy database.\n"
                    + "Some features will be limited if using other types of databases.\n\n"
                    + "Note that using Automatic Decoy Search in Mascot is not supported.\n\n"
                    + "See the PeptideShaker home page for details.",
                    "No Decoys Found",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Loads the FASTA file in the factory.
     *
     * @param file the FASTA file
     * @param progressDialog the progress dialog
     */
    private void loadFastaFile(File file, ProgressDialogX progressDialog) {

        try {
            if (progressDialog != null) {
                progressDialog.setTitle("Importing Database. Please Wait...");
                progressDialog.setPrimaryProgressCounterIndeterminate(false);
            }
            sequenceFactory.loadFastaFile(file, progressDialog);
            checkFastaFile();
            if (progressDialog != null) {
                progressDialog.setRunFinished();
            }
        } catch (IOException e) {
            if (progressDialog != null) {
                progressDialog.setRunFinished();
            }
            JOptionPane.showMessageDialog(peptideShakerGUI,
                    new String[]{"FASTA Import Error.", "File " + file.getAbsolutePath() + " not found."},
                    "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            if (progressDialog != null) {
                progressDialog.setRunFinished();
            }
            JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                    "File index of " + file.getName() + " could not be imported.<br>"
                    + "Please <a href=\"http://code.google.com/p/peptide-shaker/issues/list\">contact the developers</a>."),
                    "FASTA Import Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (StringIndexOutOfBoundsException e) {
            if (progressDialog != null) {
                progressDialog.setRunFinished();
            }
            JOptionPane.showMessageDialog(peptideShakerGUI,
                    e.getMessage(),
                    "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            if (progressDialog != null) {
                progressDialog.setRunFinished();
            }
            JOptionPane.showMessageDialog(peptideShakerGUI,
                    e.getMessage(),
                    "FASTA Import Error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Creates the project details for this new project.
     *
     * @return the project details
     */
    private ProjectDetails getProjectDetails() {
        ProjectDetails projectDetails = new ProjectDetails();
        projectDetails.setCreationDate(new Date());
        projectDetails.setPeptideShakerVersion(new eu.isas.peptideshaker.utils.Properties().getVersion());
        return projectDetails;
    }

    /**
     * Show a simple dialog saying that UniProt databases is recommended and
     * display a link to the Database Help web page.
     */
    private void showDataBaseHelpDialog() {
        JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                "We strongly recommend the use of UniProt databases. Some<br>"
                + "features will be limited if using other databases.<br><br>"
                + "See <a href=\"http://code.google.com/p/searchgui/wiki/DatabaseHelp\">Database Help</a> for details."),
                "Database Information", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public String getLastSelectedFolder() {
        return peptideShakerGUI.getLastSelectedFolder();
    }

    @Override
    public void setLastSelectedFolder(String lastSelectedFolder) {
        peptideShakerGUI.setLastSelectedFolder(lastSelectedFolder);
    }

    @Override
    public File getUserModificationsFile() {
        return new File(peptideShakerGUI.getJarFilePath(), PeptideShaker.USER_MODIFICATIONS_FILE);
    }

    @Override
    public SearchParameters getSearchParameters() {
        return searchParameters;
    }

    @Override
    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;
        if (searchParameters.getParametersFile() != null) {
            searchTxt.setText(searchParameters.getParametersFile().getName().substring(0, searchParameters.getParametersFile().getName().lastIndexOf(".")));
        } else {
            searchTxt.setText("User Defined");
        }
        fastaFileTxt.setText(searchParameters.getFastaFile().getName());
        validateInput();
    }

    @Override
    public ArrayList<String> getModificationUse() {
        return peptideShakerGUI.getModificationUse();
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

        ArrayList<File> parameterFiles = new ArrayList<File>();
        ArrayList<File> dataFolders = new ArrayList<File>();
        ArrayList<File> inputFiles = new ArrayList<File>();

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
                    if (lowerCaseName.endsWith("zip")) {
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
                if (lowerCaseName.endsWith("zip")) {
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
                    } else if (name.toLowerCase().endsWith(".parameters")
                            || name.toLowerCase().endsWith(".properties")) {
                        if (!parameterFiles.contains(file)) {
                            parameterFiles.add(file);
                        }
                    }
                    if (file.getName().endsWith("usermods.xml")) {
                        modificationFiles.add(file);
                    }
                }
            }

            peptideShakerGUI.setLastSelectedFolder(newFile.getAbsolutePath());

            if (loadCanceled) {
                break;
            }
        }

        if (!loadCanceled) {

            File parameterFile = null;
            if (parameterFiles.size() == 1) {
                parameterFile = parameterFiles.get(0);
            } else if (parameterFiles.size() > 1) {

                boolean equalParameters = true;

                try {
                    for (int i = 0; i < parameterFiles.size() && equalParameters; i++) {
                        for (int j = 0; j < parameterFiles.size() && equalParameters; j++) {
                            equalParameters = SearchParameters.getIdentificationParameters(parameterFiles.get(i)).equals(SearchParameters.getIdentificationParameters(parameterFiles.get(j)));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    equalParameters = false;
                } catch (IOException e) {
                    equalParameters = false;
                }

                if (equalParameters) {
                    // all parameters are equal, just select one of them
                    parameterFile = parameterFiles.get(0); // @TODO: can we be more clever in selecting the "right" one?
                } else {
                    FileSelectionDialog fileSelection = new FileSelectionDialog(peptideShakerGUI, parameterFiles, "Select the wanted SearchGUI parameters file.");
                    if (!fileSelection.isCanceled()) {
                        parameterFile = fileSelection.getSelectedFile();
                    }
                }
            }

            loadMgfs(inputFiles, dataFolders);

            idFilesTxt.setText(idFiles.size() + " file(s) selected");

            if (parameterFile != null) {
                importSearchParameters(parameterFile, dataFolders, progressDialog);
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
     * @param parameterFiles list of the parameters file found
     * @param dataFolders list of the folders where the mgf and FASTA files
     * could possibly be
     * @param inputFiles list of the input files found
     * @return true of the zipping completed withoth any issues
     */
    private boolean loadZipFile(File file, ArrayList<File> parameterFiles, ArrayList<File> dataFolders, ArrayList<File> inputFiles) {

        String newName = FileImporter.getTempFolderName(file.getName());
        File destinationFolder = new File(file.getParentFile(), newName);
        destinationFolder.mkdir();
        TempFilesManager.registerTempFolder(destinationFolder);

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
     * @param parameterFiles list of parameters files found
     * @param inputFiles list of the input files found
     */
    private void loadIdFile(File file, ArrayList<File> parameterFiles, ArrayList<File> inputFiles) {

        // add searchGUI_input.txt
        if (file.getName().equals(SEARCHGUI_INPUT)) {
            inputFiles.add(file);
        }

        String lowerCaseName = file.getName().toLowerCase();

        if (lowerCaseName.endsWith("dat")
                || lowerCaseName.endsWith("omx")
                || lowerCaseName.endsWith("xml")
                || lowerCaseName.endsWith("mzid")
                || lowerCaseName.endsWith("csv")
                || lowerCaseName.endsWith("tags")) {
            if (!lowerCaseName.endsWith("mods.xml")
                    && !lowerCaseName.endsWith("usermods.xml")
                    && !lowerCaseName.endsWith("settings.xml")) {
                idFiles.add(file);
            } else if (lowerCaseName.endsWith("usermods.xml")) {
                modificationFiles.add(file);
            }
        } else if (lowerCaseName.endsWith(".parameters")
                || lowerCaseName.endsWith(".properties")) {
            boolean found = false;
            for (File tempFile : parameterFiles) {
                if (tempFile.getName().equals(file.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                parameterFiles.add(file);
            }
        }
    }
}
