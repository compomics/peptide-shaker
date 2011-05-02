package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.io.identifications.IdentificationParametersReader;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.fileimport.IdFilter;
import eu.isas.peptideshaker.preferences.SearchParameters;
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
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

/**
 * A dialog for selecting the identification files to compare and visualize.
 *
 * @author  Marc Vaudel
 * @author  Harald Barsnes
 */
public class OpenDialog extends javax.swing.JDialog implements ProgressDialogParent {

    /**
     * The compomics PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The enzyme factory
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The experiment conducted
     */
    private MsExperiment experiment = null;
    /**
     * The sample analyzed
     */
    private Sample sample;
    /**
     * The replicate number
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
     * The parameters files found
     */
    private ArrayList<File> searchParametersFiles = new ArrayList<File>();
    /**
     * A file where the input will be stored
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
     * Compomics experiment saver and opener
     */
    private ExperimentIO experimentIO = new ExperimentIO();
    /**
     * Boolean indicating whether we are opening a peptideshaker file
     */
    private boolean isPsFile = false;
    /**
     * A simple progress dialog.
     */
    private static ProgressDialog progressDialog;
    /**
     * If set to true the progress stopped and the simple progress dialog
     * disposed.
     */
    private boolean cancelProgress = false;
    /**
     * The peptide shaker class which will take care of the pre-processing.
     */
    private PeptideShaker peptideShaker;

    /**
     * Creates a new open dialog.
     *
     * @param peptideShaker a reference to the main frame
     * @param modal         boolean indicating whether the dialog is modal
     */
    public OpenDialog(PeptideShakerGUI peptideShaker, boolean modal) {
        super(peptideShaker, modal);
        this.peptideShakerGUI = peptideShaker;
        setUpGui();
        this.setLocationRelativeTo(peptideShaker);
        this.setVisible(true);
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
    public OpenDialog(PeptideShakerGUI peptideShaker, boolean modal, MsExperiment experiment, Sample sample, int replicateNumber) {
        super(peptideShaker, modal);

        this.peptideShakerGUI = peptideShaker;
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNumber;
        setUpGui();
        this.setLocationRelativeTo(peptideShaker);
        this.setVisible(true);
    }

    /**
     * Set up the gui.
     */
    private void setUpGui() {
        initComponents();
        generateHeaderExample();
        idFilesTxt.setText(idFiles.size() + " file(s) selected.");
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected.");
        fastaFileTxt.setText("");
        precMassUnitCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
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
        jTabbedPane = new javax.swing.JTabbedPane();
        fileImportPanel = new javax.swing.JPanel();
        projectDetailsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        idFilesTxt = new javax.swing.JTextField();
        browseId = new javax.swing.JButton();
        editId = new javax.swing.JButton();
        clearId = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        mascotMaxEvalueTxt = new javax.swing.JTextField();
        omssaMaxEvalueTxt = new javax.swing.JTextField();
        xtandemMaxEvalueTxt = new javax.swing.JTextField();
        minPeplengthTxt = new javax.swing.JTextField();
        maxPepLengthTxt = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        massDeviationTxt = new javax.swing.JTextField();
        precMassUnitCmb = new javax.swing.JComboBox();
        configPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        spectrumFilesTxt = new javax.swing.JTextField();
        browseSpectra = new javax.swing.JButton();
        editSpectra = new javax.swing.JButton();
        clearSpectra = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        fastaFileTxt = new javax.swing.JTextField();
        browseDbButton = new javax.swing.JButton();
        clearDbButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        dbNameTxt = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        dbVersionTxt = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        stringBeforeTxt = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        stringAfterTxt = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        headerExampleTxt = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        openButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        replicateNumberIdtxt = new javax.swing.JTextField();
        projectNameIdTxt = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        sampleNameIdtxt = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PeptideShaker - Open Files");
        setResizable(false);

        projectDetailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification File(s)"));

        jLabel1.setText("Identification File(s):");

        idFilesTxt.setEditable(false);
        idFilesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        browseId.setText("Browse");
        browseId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseIdActionPerformed(evt);
            }
        });

        editId.setText("Edit");
        editId.setEnabled(false);

        clearId.setText("Clear");
        clearId.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearIdActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout projectDetailsPanelLayout = new javax.swing.GroupLayout(projectDetailsPanel);
        projectDetailsPanel.setLayout(projectDetailsPanelLayout);
        projectDetailsPanelLayout.setHorizontalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(idFilesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseId)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editId)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(clearId)
                .addContainerGap())
        );

        projectDetailsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseId, clearId, editId});

        projectDetailsPanelLayout.setVerticalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(idFilesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(clearId)
                    .addComponent(editId)
                    .addComponent(browseId))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification Processing Options"));

        jLabel3.setText("Mascot Max E-value:");

        jLabel4.setText("OMSSA Max E-value:");

        jLabel5.setText("X!Tandem Max E-value:");

        jLabel6.setText("Min Peptide Length:");

        jLabel7.setText("Max Peptide Length:");

        mascotMaxEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        mascotMaxEvalueTxt.setText("10");
        mascotMaxEvalueTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mascotMaxEvalueTxtActionPerformed(evt);
            }
        });

        omssaMaxEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        omssaMaxEvalueTxt.setText("10");

        xtandemMaxEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        xtandemMaxEvalueTxt.setText("10");
        xtandemMaxEvalueTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xtandemMaxEvalueTxtActionPerformed(evt);
            }
        });

        minPeplengthTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        minPeplengthTxt.setText("8");

        maxPepLengthTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        maxPepLengthTxt.setText("20");

        jLabel11.setText("Max Mass Deviation:");

        massDeviationTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        massDeviationTxt.setText("10");

        precMassUnitCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ppm", "Da" }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(mascotMaxEvalueTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                    .addComponent(omssaMaxEvalueTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                    .addComponent(xtandemMaxEvalueTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jLabel6)
                    .addComponent(jLabel11))
                .addGap(28, 28, 28)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(minPeplengthTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)
                    .addComponent(maxPepLengthTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)
                    .addComponent(massDeviationTxt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(precMassUnitCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel11, jLabel6, jLabel7});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {mascotMaxEvalueTxt, omssaMaxEvalueTxt, xtandemMaxEvalueTxt});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(minPeplengthTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(maxPepLengthTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(massDeviationTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precMassUnitCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(mascotMaxEvalueTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(omssaMaxEvalueTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(xtandemMaxEvalueTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout fileImportPanelLayout = new javax.swing.GroupLayout(fileImportPanel);
        fileImportPanel.setLayout(fileImportPanelLayout);
        fileImportPanelLayout.setHorizontalGroup(
            fileImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fileImportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fileImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 802, Short.MAX_VALUE)
                    .addComponent(projectDetailsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        fileImportPanelLayout.setVerticalGroup(
            fileImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fileImportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(projectDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(32, Short.MAX_VALUE))
        );

        jTabbedPane.addTab("Identification Files", fileImportPanel);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Files"));

        jLabel10.setText("Spectrum File(s):");

        spectrumFilesTxt.setEditable(false);
        spectrumFilesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        browseSpectra.setText("Browse");
        browseSpectra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseSpectraActionPerformed(evt);
            }
        });

        editSpectra.setText("Edit");
        editSpectra.setEnabled(false);
        editSpectra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSpectraActionPerformed(evt);
            }
        });

        clearSpectra.setText("Clear");
        clearSpectra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSpectraActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumFilesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 419, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseSpectra)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editSpectra)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(clearSpectra)
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseSpectra, clearSpectra, editSpectra});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(clearSpectra)
                    .addComponent(editSpectra)
                    .addComponent(browseSpectra)
                    .addComponent(spectrumFilesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout configPanelLayout = new javax.swing.GroupLayout(configPanel);
        configPanel.setLayout(configPanelLayout);
        configPanelLayout.setHorizontalGroup(
            configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, configPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        configPanelLayout.setVerticalGroup(
            configPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(configPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(159, Short.MAX_VALUE))
        );

        jTabbedPane.addTab("Spectrum Files", configPanel);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Database File"));

        jLabel14.setText("FASTA File:");

        fastaFileTxt.setEditable(false);
        fastaFileTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fastaFileTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fastaFileTxtActionPerformed(evt);
            }
        });

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

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fastaFileTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 492, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseDbButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(clearDbButton)
                .addContainerGap())
        );

        jPanel5Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseDbButton, clearDbButton});

        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(fastaFileTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearDbButton)
                    .addComponent(browseDbButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Sequences Processing Options"));

        jLabel16.setText("Database Name:");

        dbNameTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        dbNameTxt.setText("UniProtKB/Swiss-prot");

        jLabel17.setText("Database Version:");

        dbVersionTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        dbVersionTxt.setText("2010.11.04");

        jLabel18.setText("Separators:");

        stringBeforeTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        stringBeforeTxt.setText("|");
        stringBeforeTxt.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                stringBeforeTxtCaretUpdate(evt);
            }
        });
        stringBeforeTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stringBeforeTxtActionPerformed(evt);
            }
        });

        jLabel19.setFont(jLabel19.getFont().deriveFont((jLabel19.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel19.setText("(following the accession)");

        stringAfterTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        stringAfterTxt.setText("|");
        stringAfterTxt.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                stringAfterTxtCaretUpdate(evt);
            }
        });
        stringAfterTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stringAfterTxtActionPerformed(evt);
            }
        });

        jLabel20.setText("Example:");

        headerExampleTxt.setEditable(false);
        headerExampleTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel21.setFont(jLabel21.getFont().deriveFont((jLabel21.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel21.setText("(preceding the accession)");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(stringBeforeTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel21)
                        .addGap(18, 18, 18)
                        .addComponent(stringAfterTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel19))
                    .addComponent(dbVersionTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
                    .addComponent(dbNameTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
                    .addComponent(headerExampleTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(jLabel16)
                    .addComponent(dbNameTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(jLabel17)
                    .addComponent(dbVersionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(jLabel21)
                    .addComponent(stringAfterTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19)
                    .addComponent(stringBeforeTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(jLabel20)
                    .addComponent(headerExampleTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane.addTab("Sequence File", jPanel2);

        openButton.setText("Open");
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

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Details"));

        replicateNumberIdtxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        replicateNumberIdtxt.setText("0");

        projectNameIdTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        projectNameIdTxt.setText("new project");
        projectNameIdTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projectNameIdTxtActionPerformed(evt);
            }
        });

        jLabel9.setText("Replicate Number:");

        jLabel8.setText("Sample Name:");

        jLabel2.setText("Project Reference:");

        sampleNameIdtxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        sampleNameIdtxt.setText("new sample");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(projectNameIdTxt)
                    .addComponent(sampleNameIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, 396, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                .addComponent(jLabel9)
                .addGap(18, 18, 18)
                .addComponent(replicateNumberIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(projectNameIdTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sampleNameIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(replicateNumberIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel9)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel12.setFont(jLabel12.getFont().deriveFont((jLabel12.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel12.setText("Select the identification, spectrum and sequence files, and click Open to load and view the files.");

        javax.swing.GroupLayout sampleDetailsPanelLayout = new javax.swing.GroupLayout(sampleDetailsPanel);
        sampleDetailsPanel.setLayout(sampleDetailsPanelLayout);
        sampleDetailsPanelLayout.setHorizontalGroup(
            sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sampleDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTabbedPane, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 235, Short.MAX_VALUE)
                        .addComponent(openButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(exitButton)))
                .addContainerGap())
        );

        sampleDetailsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {exitButton, openButton});

        sampleDetailsPanelLayout.setVerticalGroup(
            sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exitButton)
                    .addComponent(openButton)
                    .addComponent(jLabel12))
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

            if (experiment == null) {
                experiment = new MsExperiment(projectNameIdTxt.getText().trim());
                sample = new Sample(sampleNameIdtxt.getText().trim());
                SampleAnalysisSet analysisSet = new SampleAnalysisSet(sample, new ProteomicAnalysis(getReplicateNumber()));
                replicateNumber = getReplicateNumber();
                experiment.addAnalysisSet(sample, analysisSet);
            }

            peptideShaker = new PeptideShaker(experiment, sample, replicateNumber);

            WaitingDialog waitingDialog = new WaitingDialog(peptideShakerGUI, true, experiment.getReference());

            int progressCounter = idFiles.size() + spectrumFiles.size();

            if (fastaFile != null) {
                progressCounter++;
            }

            waitingDialog.setMaxProgressValue(progressCounter);

            boolean needDialog = false;

            // load the identification files
            if (idFiles.size() > 0 && !isPsFile) {
                needDialog = true;
                importIdentificationFiles(waitingDialog);
            }

            // load the fasta files
            if (fastaFile != null) {
                needDialog = true;
                importFastaFile(waitingDialog);
            }
            
            if (needDialog) {
                waitingDialog.setVisible(true);
                this.dispose();
            }
            
            if (!needDialog || !waitingDialog.isRunCanceled()) {
                peptideShakerGUI.setProject(experiment, sample, replicateNumber);
                peptideShakerGUI.displayResults();
                this.dispose();
            }
        }
}//GEN-LAST:event_openButtonActionPerformed

    private void editIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editIdActionPerformed
        // @TODO: implement
    }//GEN-LAST:event_editIdActionPerformed

    private void projectNameIdTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_projectNameIdTxtActionPerformed
    }//GEN-LAST:event_projectNameIdTxtActionPerformed

    private void clearDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearDbButtonActionPerformed
        fastaFile = null;
        fastaFileTxt.setText("");
}//GEN-LAST:event_clearDbButtonActionPerformed

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
        int returnVal = fileChooser.showDialog(this.getParent(), "Add");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            fastaFile = fileChooser.getSelectedFile();
            peptideShakerGUI.setLastSelectedFolder(fastaFile.getPath());
            fastaFileTxt.setText(fastaFile.getAbsolutePath());
        }
}//GEN-LAST:event_browseDbButtonActionPerformed

    private void fastaFileTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fastaFileTxtActionPerformed
}//GEN-LAST:event_fastaFileTxtActionPerformed

    private void clearSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectraActionPerformed
        spectrumFiles = new ArrayList<File>();
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected.");
}//GEN-LAST:event_clearSpectraActionPerformed

    private void editSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSpectraActionPerformed
        // @TODO: implement
}//GEN-LAST:event_editSpectraActionPerformed

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
                peptideShakerGUI.setLastSelectedFolder(newFile.getPath());
            }
            spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected.");
        }
}//GEN-LAST:event_browseSpectraActionPerformed

    private void xtandemMaxEvalueTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xtandemMaxEvalueTxtActionPerformed
}//GEN-LAST:event_xtandemMaxEvalueTxtActionPerformed

    private void clearIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearIdActionPerformed
        idFiles = new ArrayList<File>();
        idFilesTxt.setText(idFiles.size() + " file(s) selected.");
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
                return myFile.getName().toLowerCase().endsWith("dat")
                        || myFile.getName().toLowerCase().endsWith("omx")
                        || myFile.getName().toLowerCase().endsWith("xml")
                        || myFile.getName().toLowerCase().endsWith("cps")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Supported formats: Mascot (.dat), OMSSA (.omx), X!Tandem (.xml), Peptide Shaker (.cps)";
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
                                || file.getName().toLowerCase().endsWith("xml")
                                || file.getName().toLowerCase().endsWith("cps")) {
                            idFiles.add(file);
                        } else if (file.getName().toLowerCase().endsWith(".properties")) {
                            searchParametersFiles.add(file);
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
                peptideShakerGUI.setLastSelectedFolder(newFile.getPath());
            }

            if (idFiles.size() > 1) {
                for (File file : idFiles) {
                    if (file.getName().endsWith(".cps")) {
                        JOptionPane.showMessageDialog(this, "A PeptideShaker file must be imported alone.", "Wrong identification file.", JOptionPane.ERROR_MESSAGE);
                        idFiles = new ArrayList<File>();
                    }
                }
            }
            idFilesTxt.setText(idFiles.size() + " file(s) selected.");

            if (idFiles.size() == 1 && idFiles.get(0).getName().endsWith(".cps")) {
                importPeptideShakerFile(idFiles.get(0));
                isPsFile = true;
                projectNameIdTxt.setEditable(false);
                sampleNameIdtxt.setEditable(false);
                replicateNumberIdtxt.setEditable(false);
                mascotMaxEvalueTxt.setEditable(false);
                omssaMaxEvalueTxt.setEditable(false);
                xtandemMaxEvalueTxt.setEditable(false);
                maxPepLengthTxt.setEditable(false);
                minPeplengthTxt.setEditable(false);
                massDeviationTxt.setEditable(false);
            } else {
                experiment = null;
                projectNameIdTxt.setEditable(true);
                sampleNameIdtxt.setEditable(true);
                replicateNumberIdtxt.setEditable(true);
                mascotMaxEvalueTxt.setEditable(true);
                omssaMaxEvalueTxt.setEditable(true);
                xtandemMaxEvalueTxt.setEditable(true);
                maxPepLengthTxt.setEditable(true);
                minPeplengthTxt.setEditable(true);
                massDeviationTxt.setEditable(true);
                isPsFile = false;
            }

            if (searchParametersFiles.size() == 1) {
                importSearchParameters(searchParametersFiles.get(0));
            } else if (searchParametersFiles.size() > 1) {
                new FileSelection(this, searchParametersFiles);
            }

            for (File folder : folders) {
                File inputFile = new File(folder, SEARCHGUI_INPUT);
                if (inputFile.exists()) {
                    importMgfFiles(inputFile);
                }
            }
        }
}//GEN-LAST:event_browseIdActionPerformed

    private void mascotMaxEvalueTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mascotMaxEvalueTxtActionPerformed
    }//GEN-LAST:event_mascotMaxEvalueTxtActionPerformed

    private void stringBeforeTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stringBeforeTxtActionPerformed
        generateHeaderExample();
    }//GEN-LAST:event_stringBeforeTxtActionPerformed

    private void stringAfterTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stringAfterTxtActionPerformed
        generateHeaderExample();
    }//GEN-LAST:event_stringAfterTxtActionPerformed

    private void stringBeforeTxtCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_stringBeforeTxtCaretUpdate
        generateHeaderExample();
    }//GEN-LAST:event_stringBeforeTxtCaretUpdate

    private void stringAfterTxtCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_stringAfterTxtCaretUpdate
        generateHeaderExample();
    }//GEN-LAST:event_stringAfterTxtCaretUpdate
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseDbButton;
    private javax.swing.JButton browseId;
    private javax.swing.JButton browseSpectra;
    private javax.swing.JButton clearDbButton;
    private javax.swing.JButton clearId;
    private javax.swing.JButton clearSpectra;
    private javax.swing.JPanel configPanel;
    private javax.swing.JTextField dbNameTxt;
    private javax.swing.JTextField dbVersionTxt;
    private javax.swing.JButton editId;
    private javax.swing.JButton editSpectra;
    private javax.swing.JButton exitButton;
    private javax.swing.JTextField fastaFileTxt;
    private javax.swing.JPanel fileImportPanel;
    private javax.swing.JTextField headerExampleTxt;
    private javax.swing.JTextField idFilesTxt;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JTextField mascotMaxEvalueTxt;
    private javax.swing.JTextField massDeviationTxt;
    private javax.swing.JTextField maxPepLengthTxt;
    private javax.swing.JTextField minPeplengthTxt;
    private javax.swing.JTextField omssaMaxEvalueTxt;
    private javax.swing.JButton openButton;
    private javax.swing.JComboBox precMassUnitCmb;
    private javax.swing.JPanel projectDetailsPanel;
    private javax.swing.JTextField projectNameIdTxt;
    private javax.swing.JTextField replicateNumberIdtxt;
    private javax.swing.JPanel sampleDetailsPanel;
    private javax.swing.JTextField sampleNameIdtxt;
    private javax.swing.JTextField spectrumFilesTxt;
    private javax.swing.JTextField stringAfterTxt;
    private javax.swing.JTextField stringBeforeTxt;
    private javax.swing.JTextField xtandemMaxEvalueTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * Validates the input parameters.
     *
     * @return true if the input is valid, false otherwise.
     */
    private boolean validateInput() {

        try {
            getMaxMassDeviation();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for max mass deviation.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            getMinPeptideLength();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for min peptide length.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            getMaxPeptideLength();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for max peptide length.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            getMascotMaxEvalue();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for Mascot max e-value.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            getOmssaMaxEvalue();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for OMSSA max e-value.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            getXtandemMaxEvalue();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for X!Tandem max e-value.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            getReplicateNumber();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Please verify the input for replicate number.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Returns the minimum peptide length.
     *
     * @return the minimum peptide length
     */
    private int getMinPeptideLength() {
        String input = minPeplengthTxt.getText().trim();
        if (input == null || input.equals("")) {
            input = "0";
        }
        return new Integer(input);
    }

    /**
     * Returns the maximum peptide length.
     *
     * @return  the maximum peptide length
     */
    private int getMaxPeptideLength() {
        String input = maxPepLengthTxt.getText().trim();
        if (input == null || input.equals("")) {
            input = "0";
        }
        return new Integer(input);
    }

    /**
     * Returns the maximal mass deviation allowed
     * @return the maximal mass deviation allowed
     */
    private double getMaxMassDeviation() {
        String input = massDeviationTxt.getText().trim();
        if (input == null || input.equals("")) {
            input = "0";
        }
        return new Double(input);
    }

    /**
     * Returns the Mascot max e-value.
     *
     * @return the Mascot max e-value
     */
    private double getMascotMaxEvalue() {
        String input = mascotMaxEvalueTxt.getText().trim();
        if (input == null || input.equals("")) {
            input = "0";
        }
        return new Double(input);
    }

    /**
     * Returns the OMSSA max e-value.
     *
     * @return the OMSSA max e-value
     */
    private double getOmssaMaxEvalue() {
        String input = omssaMaxEvalueTxt.getText().trim();
        if (input == null || input.equals("")) {
            input = "0";
        }
        return new Double(input);
    }

    /**
     * Returns the XTandem max e-value.
     *
     * @return the XTandem max e-value
     */
    private double getXtandemMaxEvalue() {
        String input = xtandemMaxEvalueTxt.getText().trim();
        if (input == null || input.equals("")) {
            input = "0";
        }
        return new Double(input);
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
     * Imports identifications form identification files
     * @param waitingDialog a dialog to display feedback to the user
     */
    private void importIdentificationFiles(WaitingDialog waitingDialog) {
        boolean precTolUnit;
        if (((String) precMassUnitCmb.getSelectedItem()).equals("ppm")) {
            precTolUnit = true;
        } else {
            precTolUnit = false;
        }
        IdFilter idFilter = new IdFilter(getMinPeptideLength(), getMaxPeptideLength(), getMascotMaxEvalue(), getOmssaMaxEvalue(), getXtandemMaxEvalue(), getMaxMassDeviation(), precTolUnit);
        peptideShaker.importIdentifications(waitingDialog, idFilter, idFiles, spectrumFiles);
    }

    /**
     * Imports sequences form a fasta file
     * @param waitingDialog a dialog to display feedback to the user
     */
    private void importFastaFile(WaitingDialog waitingDialog) {
        peptideShaker.importFasta(waitingDialog, fastaFile, dbNameTxt.getText().trim(), dbVersionTxt.getText().trim(), stringBeforeTxt.getText(), stringAfterTxt.getText());
    }

    /**
     * Generates a fasta header example and displays it
     */
    private void generateHeaderExample() {
        String example = ">";
        if (!stringBeforeTxt.getText().equals("")) {
            example += "xx" + stringBeforeTxt.getText();
        }
        example += "ACCESSION" + stringAfterTxt.getText() + "DESCRIPTION";
        headerExampleTxt.setText(example);
    }

    /**
     * Imports the search parameters from a searchGUI file
     * @param searchGUIFile    the selected searchGUI file
     */
    public void importSearchParameters(File searchGUIFile) {
        SearchParameters searchParameters = new SearchParameters();
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
                searchParameters.addExpectedModifications(name, 2);
            }
            temp = props.getProperty(IdentificationParametersReader.ENZYME);
            if (temp != null && !temp.equals("")) {
                searchParameters.setEnzyme(enzymeFactory.getEnzyme(temp.trim()));
            }
            temp = props.getProperty(IdentificationParametersReader.FRAGMENT_MASS_TOLERANCE);
            if (temp != null) {
                searchParameters.setFragmentIonMZTolerance(new Double(temp.trim()));
            }
            temp = props.getProperty(IdentificationParametersReader.MISSED_CLEAVAGES);
            if (temp != null) {
                searchParameters.setnMissedCleavages(new Integer(temp.trim()));
            }
            searchParameters.setParametersFile(searchGUIFile);
            temp = props.getProperty(IdentificationParametersReader.DATABASE_FILE);
            try {
                File file = new File(temp);
                searchParameters.setFastaFile(file);
                fastaFileTxt.setText(file.getAbsolutePath());
                fastaFile = file;
            } catch (Exception e) {
                // file not found: use manual input
            }

            peptideShakerGUI.setSearchParameters(searchParameters);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, searchGUIFile.getName() + " not found.", "File Not Found", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "An error occured while reading " + searchGUIFile.getName()
                    + ". Please verify the version compatibility.", "File Import error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Imports the mgf files from a searchGUI file
     * @param searchGUIFile a searchGUI file
     */
    private void importMgfFiles(File searchGUIFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(searchGUIFile));
            String line = null;
            ArrayList<String> names = new ArrayList<String>();
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
                            names.add(newFile.getName());
                            spectrumFiles.add(newFile);
                        }
                    } catch (Exception e) {
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            // ignore exception
        }
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected.");
    }

    /**
     * Loads the new project information
     */
    private void loadProject() {

        projectNameIdTxt.setText(experiment.getReference());

        ArrayList<Sample> samples = new ArrayList(experiment.getSamples().values());
        if (samples.size() == 1) {
            sample = samples.get(0);
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
                    sample = sampleTemp;
                    break;
                }
            }
        }

        sampleNameIdtxt.setText(sample.getReference());

        ArrayList<Integer> replicates = new ArrayList(experiment.getAnalysisSet(sample).getReplicateNumberList());
        if (replicates.size() == 1) {
            replicateNumber = replicates.get(0);
        } else {
            String[] replicateNames = new String[replicates.size()];
            for (int cpt = 0; cpt < replicateNames.length; cpt++) {
                replicateNames[cpt] = samples.get(cpt).getReference();
            }
            SampleSelection sampleSelection = new SampleSelection(null, true, replicateNames, "replicate");
            sampleSelection.setVisible(true);
            Integer choice = new Integer(sampleSelection.getChoice());
            replicateNumber = choice;
        }
        replicateNumberIdtxt.setText(replicateNumber + "");
        mascotMaxEvalueTxt.setText("");
        omssaMaxEvalueTxt.setText("");
        xtandemMaxEvalueTxt.setText("");
        maxPepLengthTxt.setText("");
        minPeplengthTxt.setText("");
        massDeviationTxt.setText("");

    }

    /**
     * Imports informations from a peptide shaker file.
     *
     * @param aPsFile    the peptide shaker file
     */
    private void importPeptideShakerFile(File aPsFile) {

        final File psFile = aPsFile;

        final OpenDialog tempRef = this; // needed due to threading issues
        cancelProgress = false;
        progressDialog = new ProgressDialog(this, this, true);
        progressDialog.doNothingOnClose();

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIntermidiate(true);
                progressDialog.setTitle("Importing. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("ImportThread") {

            @Override
            public void run() {

                try {
                    Date date = experimentIO.getDate(psFile);
                    experiment = experimentIO.loadExperiment(psFile);
                    loadProject();

                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    JOptionPane.showMessageDialog(tempRef,
                            "Experiment " + experiment.getReference() + " created on " + date.toString()
                            + " imported.\n\n"
                            + "Click Open, to open the results.", "Identifications Imported.", JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception e) {

                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    JOptionPane.showMessageDialog(tempRef,
                            "An error occured while reading" + psFile + ".\\"
                            + "Please verif that the compomics utilities version used to create\n"
                            + "the file was the same as the one used by your version of Reporter.",
                            "File Input Error.", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void cancelProgress() {
        cancelProgress = true;
    }
}
