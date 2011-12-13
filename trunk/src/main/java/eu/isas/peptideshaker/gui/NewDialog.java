package eu.isas.peptideshaker.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.io.identifications.IdentificationParametersReader;
import com.compomics.util.gui.dialogs.ProgressDialogParent;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.preferencesdialogs.ImportSettingsDialog;
import eu.isas.peptideshaker.gui.preferencesdialogs.SearchPreferencesDialog;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SearchParameters;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * A dialog for selecting the files to load.
 *
 * @author  Marc Vaudel
 * @author  Harald Barsnes
 */
public class NewDialog extends javax.swing.JDialog implements ProgressDialogParent {

    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
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
     * The parameters files found.
     */
    private ArrayList<File> searchParametersFiles = new ArrayList<File>();
    /**
     * A file where the input will be stored.
     */
    private final static String SEARCHGUI_INPUT = "searchGUI_input.txt";
    /**
     * The list of spectrum files.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<File>();
    /**
     * The fasta file.
     */
    private File fastaFile = null;
    /**
     * Compomics experiment saver and opener.
     */
    private ExperimentIO experimentIO = new ExperimentIO();
    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;
    /**
     * If set to true the progress stopped and the simple progress dialog.
     * disposed.
     */
    private boolean cancelProgress = false;
    /**
     * The peptide shaker class which will take care of the pre-processing..
     */
    private PeptideShaker peptideShaker;

    /**
     * Creates a new open dialog.
     *
     * @param peptideShaker a reference to the main frame
     * @param modal         boolean indicating whether the dialog is modal
     */
    public NewDialog(PeptideShakerGUI peptideShaker, boolean modal) {
        super(peptideShaker, modal);
        this.peptideShakerGUI = peptideShaker;
        setUpGui();
        this.setLocationRelativeTo(peptideShaker);
    }

    /**
     * Creates a new open dialog.
     *
     * @param peptideShaker     a reference to the main frame
     * @param modal             boolean indicating whether the dialog is modal
     * @param experiment        The experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNumber   The replicate number
     */
    public NewDialog(PeptideShakerGUI peptideShaker, boolean modal, MsExperiment experiment, Sample sample, int replicateNumber) {
        super(peptideShaker, modal);

        this.peptideShakerGUI = peptideShaker;
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        setUpGui();
        this.setLocationRelativeTo(peptideShaker);
    }

    /**
     * Set up the gui.
     */
    private void setUpGui() {
        initComponents();
        idFilesTxt.setText(idFiles.size() + " file(s) selected");
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
        fastaFileTxt.setText("");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sampleDetailsPanel = new javax.swing.JPanel();
        openButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();
        projectDetailsPanel = new javax.swing.JPanel();
        replicateNumberIdtxt = new javax.swing.JTextField();
        projectNameIdTxt = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        sampleNameIdtxt = new javax.swing.JTextField();
        helpLabel = new javax.swing.JLabel();
        processingParametersPanel = new javax.swing.JPanel();
        importFilterTxt = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        searchTxt = new javax.swing.JTextField();
        editSearchButton = new javax.swing.JButton();
        editImportFilterButton = new javax.swing.JButton();
        inputFilesPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        idFilesTxt = new javax.swing.JTextField();
        browseId = new javax.swing.JButton();
        clearId = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        spectrumFilesTxt = new javax.swing.JTextField();
        browseSpectra = new javax.swing.JButton();
        clearSpectra = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        fastaFileTxt = new javax.swing.JTextField();
        browseDbButton = new javax.swing.JButton();
        clearDbButton = new javax.swing.JButton();
        openDialogHelpJButton = new javax.swing.JButton();
        exampleFilesLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PeptideShaker - New Project");
        setResizable(false);

        sampleDetailsPanel.setBackground(new java.awt.Color(230, 230, 230));

        openButton.setText("Create");
        openButton.setEnabled(false);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });

        exitButton.setText("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        projectDetailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Details"));
        projectDetailsPanel.setOpaque(false);

        replicateNumberIdtxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        replicateNumberIdtxt.setText("0");
        replicateNumberIdtxt.setToolTipText("Replicate Number");

        projectNameIdTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        projectNameIdTxt.setText("new project");

        jLabel9.setText("Replicate:");
        jLabel9.setToolTipText("Replicate Number");

        jLabel8.setText("Sample Name:");

        jLabel2.setText("Project Reference:");

        sampleNameIdtxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        sampleNameIdtxt.setText("new sample");

        javax.swing.GroupLayout projectDetailsPanelLayout = new javax.swing.GroupLayout(projectDetailsPanel);
        projectDetailsPanel.setLayout(projectDetailsPanelLayout);
        projectDetailsPanelLayout.setHorizontalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectNameIdTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
                    .addComponent(sampleNameIdtxt, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE))
                .addGap(20, 20, 20)
                .addComponent(jLabel9)
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
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleNameIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(replicateNumberIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel8))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        helpLabel.setFont(helpLabel.getFont().deriveFont((helpLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        helpLabel.setText("Insert the required information and click Create to load and view the results.");

        processingParametersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Processing Parameters"));
        processingParametersPanel.setOpaque(false);

        importFilterTxt.setEditable(false);
        importFilterTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        importFilterTxt.setText("Default");
        importFilterTxt.setToolTipText("Minimum Peptide Length");

        jLabel7.setText("Import Filters:");

        jLabel4.setText("Search Parameters:");

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

        javax.swing.GroupLayout processingParametersPanelLayout = new javax.swing.GroupLayout(processingParametersPanel);
        processingParametersPanel.setLayout(processingParametersPanelLayout);
        processingParametersPanelLayout.setHorizontalGroup(
            processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingParametersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE)
                    .addComponent(importFilterTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(editSearchButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(editImportFilterButton, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE))
                .addContainerGap())
        );
        processingParametersPanelLayout.setVerticalGroup(
            processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingParametersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(searchTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editSearchButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(processingParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(importFilterTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editImportFilterButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        inputFilesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Input Files"));
        inputFilesPanel.setOpaque(false);

        jLabel1.setText("Identification File(s):");

        idFilesTxt.setEditable(false);
        idFilesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

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

        jLabel10.setText("Spectrum File(s):");

        spectrumFilesTxt.setEditable(false);
        spectrumFilesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

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

        jLabel14.setText("Database File (FASTA):");

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
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(idFilesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseId)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearId))
                    .addGroup(inputFilesPanelLayout.createSequentialGroup()
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spectrumFilesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseSpectra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearSpectra))
                    .addGroup(inputFilesPanelLayout.createSequentialGroup()
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fastaFileTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
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
                    .addComponent(jLabel1)
                    .addComponent(clearId)
                    .addComponent(browseId))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(clearSpectra)
                    .addComponent(browseSpectra)
                    .addComponent(spectrumFilesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(inputFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(clearDbButton)
                    .addComponent(browseDbButton)
                    .addComponent(fastaFileTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        openDialogHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        openDialogHelpJButton.setToolTipText("Help");
        openDialogHelpJButton.setBorder(null);
        openDialogHelpJButton.setBorderPainted(false);
        openDialogHelpJButton.setContentAreaFilled(false);
        openDialogHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                openDialogHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                openDialogHelpJButtonMouseExited(evt);
            }
        });
        openDialogHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDialogHelpJButtonActionPerformed(evt);
            }
        });

        exampleFilesLabel.setForeground(new java.awt.Color(0, 0, 255));
        exampleFilesLabel.setText("<html><u><i>Need example files?</i></u></html>");
        exampleFilesLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                exampleFilesLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exampleFilesLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exampleFilesLabelMouseExited(evt);
            }
        });

        javax.swing.GroupLayout sampleDetailsPanelLayout = new javax.swing.GroupLayout(sampleDetailsPanel);
        sampleDetailsPanel.setLayout(sampleDetailsPanelLayout);
        sampleDetailsPanelLayout.setHorizontalGroup(
            sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sampleDetailsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(helpLabel)
                        .addGap(18, 18, 18)
                        .addComponent(exampleFilesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 141, Short.MAX_VALUE)
                        .addComponent(openButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(exitButton))
                    .addComponent(projectDetailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(inputFilesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(processingParametersPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        sampleDetailsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {exitButton, openButton});

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
                    .addComponent(openButton)
                    .addComponent(exitButton)
                    .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(exampleFilesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                        .addComponent(helpLabel)))
                .addContainerGap())
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
     * Closes the dialog.
     *
     * @param evt
     */
    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
        peptideShakerGUI.clearData(); // @TODO: find a better way of clearing this as the current option resets the whole tool...
        this.setVisible(false);
        this.dispose();
}//GEN-LAST:event_exitButtonActionPerformed

    /**
     * Tries to process the identification files, closes the dialog and then
     * opens the results in the main frame.
     *
     * @param evt
     */
    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        if (validateInput()) {

            this.setVisible(false);
            
            experiment = new MsExperiment(projectNameIdTxt.getText().trim());
            sample = new Sample(sampleNameIdtxt.getText().trim());
            SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(getReplicateNumber()));
            replicateNumber = getReplicateNumber();
            experiment.addAnalysisSet(sample, analysisSet);

            peptideShakerGUI.setProjectDetails(getProjectDetails());

            peptideShaker = new PeptideShaker(experiment, sample, replicateNumber);

            WaitingDialog waitingDialog = new WaitingDialog(peptideShakerGUI, true, experiment.getReference());

            int progressCounter = idFiles.size() + spectrumFiles.size() + 1;


            // add one more just to not start at 0%
            progressCounter++;

            waitingDialog.setMaxProgressValue(progressCounter);

            boolean needDialog = false;

            // load the identification files
            if (idFiles.size() > 0
                    || fastaFile != null
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
                peptideShakerGUI.setProject(experiment, sample, replicateNumber);
                peptideShakerGUI.setUpInitialFilters();
                peptideShakerGUI.displayResults();
                peptideShakerGUI.setFrameTitle(projectNameIdTxt.getText().trim());
                this.dispose();
            }
        }
}//GEN-LAST:event_openButtonActionPerformed

    /**
     * Clear the database field.
     * 
     * @param evt 
     */
    private void clearDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearDbButtonActionPerformed
        fastaFile = null;
        fastaFileTxt.setText("");
}//GEN-LAST:event_clearDbButtonActionPerformed

    /**
     * Opens a file chooser where the user can select the database FATA file to use.
     * 
     * @param evt 
     */
    private void browseDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseDbButtonActionPerformed
        JFileChooser fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select FASTA File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

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
                return "Supported formats: FASTA (.fasta)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this.getParent(), "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fastaFile = fileChooser.getSelectedFile();
            peptideShakerGUI.setLastSelectedFolder(fastaFile.getAbsolutePath());
            fastaFileTxt.setText(fastaFile.getName());
        }
}//GEN-LAST:event_browseDbButtonActionPerformed

    /**
     * Clear the spectra selection.
     * 
     * @param evt 
     */
    private void clearSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectraActionPerformed
        spectrumFiles = new ArrayList<File>();
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
}//GEN-LAST:event_clearSpectraActionPerformed

    private void browseSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseSpectraActionPerformed
        // @TODO: implement mzML
        JFileChooser fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Spectrum File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

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
        int returnVal = fileChooser.showDialog(this.getParent(), "Add");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            for (File newFile : fileChooser.getSelectedFiles()) {
                if (newFile.isDirectory()) {
                    File[] tempFiles = newFile.listFiles();
                    for (File file : tempFiles) {
                        if (file.getName().endsWith("mgf")) {
                            spectrumFiles.add(file);
                        }
                    }
                } else {
                    spectrumFiles.add(newFile);
                }
                peptideShakerGUI.setLastSelectedFolder(newFile.getAbsolutePath());
            }
            spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
        }
}//GEN-LAST:event_browseSpectraActionPerformed

    private void clearIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearIdActionPerformed
        idFiles = new ArrayList<File>();
        idFilesTxt.setText(idFiles.size() + " file(s) selected");
        searchParametersFiles = new ArrayList<File>();

        openButton.setEnabled(false);
}//GEN-LAST:event_clearIdActionPerformed

    private void browseIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseIdActionPerformed

        JFileChooser fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Identification File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        ArrayList<File> folders = new ArrayList<File>();

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File myFile) {
                
                if (myFile.getName().equalsIgnoreCase("mods.xml") ||
                    myFile.getName().equalsIgnoreCase("usermods.xml")) {
                    return false;
                }
                
                return myFile.getName().toLowerCase().endsWith("dat")
                        || myFile.getName().toLowerCase().endsWith("omx")
                        || myFile.getName().toLowerCase().endsWith("xml")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Supported formats: Mascot (.dat), OMSSA (.omx), X!Tandem (.xml)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this.getParent(), "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            for (File newFile : fileChooser.getSelectedFiles()) {
                if (newFile.isDirectory()) {
                    folders.add(newFile);
                    File[] tempFiles = newFile.listFiles();
                    for (File file : tempFiles) {
                        if (file.getName().toLowerCase().endsWith("dat")
                                || file.getName().toLowerCase().endsWith("omx")
                                || file.getName().toLowerCase().endsWith("xml")) {
                            if (!file.getName().equals("mods.xml")
                                    && !file.getName().equals("usermods.xml")) {
                            idFiles.add(file);
                            }
                        } else if (file.getName().toLowerCase().endsWith(".properties")) {
                            if (!searchParametersFiles.contains(file)) {
                                searchParametersFiles.add(file);
                            }
                        }
                    }
                } else {
                    folders.add(newFile.getParentFile());
                    idFiles.add(newFile);
                    for (File file : newFile.getParentFile().listFiles()) {
                        if (file.getName().toLowerCase().endsWith(".properties")) {
                            if (!searchParametersFiles.contains(file)) {
                                searchParametersFiles.add(file);
                            }
                        }
                    }
                }
                peptideShakerGUI.setLastSelectedFolder(newFile.getAbsolutePath());
            }

            if (searchParametersFiles.size() == 1) {
                importSearchParameters(searchParametersFiles.get(0));
            } else if (searchParametersFiles.size() > 1) {
                new FileSelection(this, searchParametersFiles);
            }

            boolean importSuccessfull = true;

            for (int i = 0; i < folders.size() && importSuccessfull; i++) {
                File folder = folders.get(i);
                File inputFile = new File(folder, SEARCHGUI_INPUT);
                if (inputFile.exists()) {
                    importSuccessfull = importMgfFiles(inputFile);
                }
            }
            idFilesTxt.setText(idFiles.size() + " file(s) selected");

            if (!idFiles.isEmpty()) {
                openButton.setEnabled(true);
            }
        }
}//GEN-LAST:event_browseIdActionPerformed

    /**
     * Open the SearchPreferences dialog.
     * 
     * @param evt 
     */
    private void editSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSearchButtonActionPerformed
        new SearchPreferencesDialog(peptideShakerGUI, true);
    }//GEN-LAST:event_editSearchButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void openDialogHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_openDialogHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void openDialogHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_openDialogHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void openDialogHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OpenDialog.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_openDialogHelpJButtonActionPerformed

    private void editImportFilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editImportFilterButtonActionPerformed
        new ImportSettingsDialog(peptideShakerGUI, this, true);
    }//GEN-LAST:event_editImportFilterButtonActionPerformed

    /**
     * Change the icon to a hand icon.
     * 
     * @param evt 
     */
    private void exampleFilesLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exampleFilesLabelMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exampleFilesLabelMouseEntered

    /**
     * Change the icon to the default icon.
     * 
     * @param evt 
     */
    private void exampleFilesLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exampleFilesLabelMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exampleFilesLabelMouseExited

    /**
     * Open the example file web page.
     * 
     * @param evt 
     */
    private void exampleFilesLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exampleFilesLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://code.google.com/p/peptide-shaker/downloads/detail?name=peptide-shaker_test_files.zip");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exampleFilesLabelMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseDbButton;
    private javax.swing.JButton browseId;
    private javax.swing.JButton browseSpectra;
    private javax.swing.JButton clearDbButton;
    private javax.swing.JButton clearId;
    private javax.swing.JButton clearSpectra;
    private javax.swing.JButton editImportFilterButton;
    private javax.swing.JButton editSearchButton;
    private javax.swing.JLabel exampleFilesLabel;
    private javax.swing.JButton exitButton;
    private javax.swing.JTextField fastaFileTxt;
    private javax.swing.JLabel helpLabel;
    private javax.swing.JTextField idFilesTxt;
    private javax.swing.JTextField importFilterTxt;
    private javax.swing.JPanel inputFilesPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JButton openButton;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JPanel processingParametersPanel;
    private javax.swing.JPanel projectDetailsPanel;
    private javax.swing.JTextField projectNameIdTxt;
    private javax.swing.JTextField replicateNumberIdtxt;
    private javax.swing.JPanel sampleDetailsPanel;
    private javax.swing.JTextField sampleNameIdtxt;
    private javax.swing.JTextField searchTxt;
    private javax.swing.JTextField spectrumFilesTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * Sets the fileter settings field to the given text.
     * 
     * @param text 
     */
    public void updateFilterSettingsField(String text) {
        importFilterTxt.setText(text);
    }
    
    /**
     * Validates the input parameters.
     *
     * @return true if the input is valid, false otherwise.
     */
    private boolean validateInput() {
        try {
            getReplicateNumber();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for replicate number.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (fastaFile == null) {
            JOptionPane.showMessageDialog(null, "Please verify the input for FASTA file.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (spectrumFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please verify the input for spectrum file(s).",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
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
     * Imports identifications form identification files.
     * 
     * @param waitingDialog a dialog to display feedback to the user
     */
    private void importIdentificationFiles(WaitingDialog waitingDialog) {
        peptideShakerGUI.getSearchParameters().setFastaFile(fastaFile);
        peptideShaker.importFiles(waitingDialog, peptideShakerGUI.getIdFilter(), idFiles, spectrumFiles, fastaFile, peptideShakerGUI.getSearchParameters(), peptideShakerGUI.getAnnotationPreferences(), peptideShakerGUI.getProjectDetails());
    }

    /**
     * Imports the search parameters from a searchGUI file.
     * 
     * @param searchGUIFile    the selected searchGUI file
     */
    public void importSearchParameters(File searchGUIFile) {

        SearchParameters searchParameters = peptideShakerGUI.getSearchParameters();

        try {
            Properties props = IdentificationParametersReader.loadProperties(searchGUIFile);
            ArrayList<String> variableMods = new ArrayList<String>();
            String temp = props.getProperty(IdentificationParametersReader.VARIABLE_MODIFICATIONS);

            if (temp != null && !temp.trim().equals("")) {
                try {
                    variableMods = IdentificationParametersReader.parseModificationLine(temp, ptmFactory);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Modification Not Found", JOptionPane.WARNING_MESSAGE);
                }
            }

            for (String name : variableMods) {
                if (!searchParameters.getModificationProfile().getUtilitiesNames().contains(name)) {
                    searchParameters.getModificationProfile().setPeptideShakerName(name, name);
                    if (!searchParameters.getModificationProfile().getPeptideShakerNames().contains(name)) {
                        int index = name.length() - 1;
                        if (name.lastIndexOf(" ") > 0) {
                            index = name.indexOf(" ");
                        }
                        if (name.lastIndexOf("-") > 0) {
                            index = Math.min(index, name.indexOf("-"));
                        }
                        searchParameters.getModificationProfile().setShortName(name, name.substring(0, index));
                        searchParameters.getModificationProfile().setColor(name, Color.lightGray);
                    }
                }
            }

            temp = props.getProperty(IdentificationParametersReader.ENZYME);

            if (temp != null && !temp.equals("")) {
                searchParameters.setEnzyme(enzymeFactory.getEnzyme(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.FRAGMENT_ION_MASS_ACCURACY);

            if (temp != null) {
                searchParameters.setFragmentIonAccuracy(new Double(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.PRECURSOR_MASS_TOLERANCE);

            if (temp != null) {
                try {
                    searchParameters.setPrecursorAccuracy(new Double(temp.trim()));
                    peptideShakerGUI.getIdFilter().setMaxMassDeviation(new Double(temp.trim()));
                } catch (Exception e) {
                }
            }

            temp = props.getProperty(IdentificationParametersReader.PRECURSOR_MASS_ACCURACY_UNIT);

            if (temp != null) {
                if (temp.equalsIgnoreCase("ppm")) {
                    searchParameters.setPrecursorAccuracyType(SearchParameters.PrecursorAccuracyType.PPM);
                    peptideShakerGUI.getIdFilter().setIsPpm(true);
                } else {
                    searchParameters.setPrecursorAccuracyType(SearchParameters.PrecursorAccuracyType.DA);
                    peptideShakerGUI.getIdFilter().setIsPpm(false);
                }
            }

            temp = props.getProperty(IdentificationParametersReader.MISSED_CLEAVAGES);

            if (temp != null) {
                searchParameters.setnMissedCleavages(new Integer(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.MIN_PEPTIDE_SIZE);

            if (temp != null && temp.length() > 0) {
                try {
                    searchParameters.setPrecursorAccuracy(new Double(temp.trim()));
                    peptideShakerGUI.getIdFilter().setMinPepLength(new Integer(temp.trim()));
                } catch (Exception e) {
                }
            }

            temp = props.getProperty(IdentificationParametersReader.MAX_PEPTIDE_SIZE);

            if (temp != null && temp.length() > 0) {
                try {
                    searchParameters.setPrecursorAccuracy(new Double(temp.trim()));
                    peptideShakerGUI.getIdFilter().setMaxPepLength(new Integer(temp.trim()));
                } catch (Exception e) {
                }
            }

            temp = props.getProperty(IdentificationParametersReader.FRAGMENT_ION_TYPE_1);

            if (temp != null && temp.length() > 0) {
                searchParameters.setIonSearched1(temp);
            }

            temp = props.getProperty(IdentificationParametersReader.FRAGMENT_ION_TYPE_2);

            if (temp != null && temp.length() > 0) {
                searchParameters.setIonSearched2(temp);
            }


            searchParameters.setParametersFile(searchGUIFile);
            temp = props.getProperty(IdentificationParametersReader.DATABASE_FILE);

            try {
                File file = new File(temp);
                if (file.exists()) {
                    searchParameters.setFastaFile(file);
                    fastaFileTxt.setText(file.getName());
                    fastaFile = file;
                } else {
                    JOptionPane.showMessageDialog(this, "FASTA file \'" + temp + "\' not found.\nPlease locate it manually.", "File Not Found", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception e) {
                // file not found: use manual input
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "FASTA file \'" + temp + "\' not found.\nPlease locate it manually.", "File Not Found", JOptionPane.WARNING_MESSAGE);
            }
            searchTxt.setText(searchGUIFile.getName().substring(0, searchGUIFile.getName().lastIndexOf(".")));
            importFilterTxt.setText(searchGUIFile.getName().substring(0, searchGUIFile.getName().lastIndexOf(".")));
            peptideShakerGUI.setSearchParameters(searchParameters);
            peptideShakerGUI.updateAnnotationPreferencesFromSearchSettings();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, searchGUIFile.getName() + " not found.", "File Not Found", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured while reading " + searchGUIFile.getName() + ".\n"
                    + "Please verify the version compatibility.", "File Import Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Imports the mgf files from a searchGUI file.
     * 
     * @param searchGUIFile a searchGUI file
     * @returns true of the mgf files were imported successfully
     */
    private boolean importMgfFiles(File searchGUIFile) {

        boolean success = true;

        try {
            BufferedReader br = new BufferedReader(new FileReader(searchGUIFile));
            String line = null;
            ArrayList<String> names = new ArrayList<String>();
            String missing = "";
            for (File file : spectrumFiles) {
                names.add(file.getName());
            }
            while ((line = br.readLine()) != null) {
                // Skip empty lines.
                line = line.trim();
                if (line.equals("")) {
                } else {
                    try {
                        File newFile = new File(line);
                        if (!names.contains(newFile.getName())) {
                            if (newFile.exists()) {
                                names.add(newFile.getName());
                                spectrumFiles.add(newFile);
                            } else {
                                if (!missing.equals("")) {
                                    missing += ", ";
                                }
                                missing += newFile.getName();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!missing.equals("")) {
                JOptionPane.showMessageDialog(this, "Input file(s) \'" + missing
                        + "\' not found.\nPlease locate it manually if needed.", "File Not Found", JOptionPane.WARNING_MESSAGE);
                success = false;
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");

        return success;
    }

    @Override
    public void cancelProgress() {
        cancelProgress = true;
    }

    /**
     * Get the search paramater files.
     * 
     * @return the search paramater files
     */
    public ArrayList<File> getSearchParametersFiles() {
        return searchParametersFiles;
    }

    /**
     * Set the search parameters files.
     * 
     * 
     * @param searchParametersFiles the search parameters files
     */
    public void setSearchParamatersFiles(ArrayList<File> searchParametersFiles) {
        this.searchParametersFiles = searchParametersFiles;
    }

    private ProjectDetails getProjectDetails() {
        ProjectDetails projectDetails = new ProjectDetails();
        projectDetails.setCreationDate(new Date());
        projectDetails.setDbFile(fastaFile);
        projectDetails.setIdentificationFiles(idFiles);

        return projectDetails;
    }
}
