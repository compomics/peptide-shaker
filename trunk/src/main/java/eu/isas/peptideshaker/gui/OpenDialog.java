package eu.isas.peptideshaker.gui;

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
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.fileimport.IdFilter;
import eu.isas.peptideshaker.gui.preferencesdialogs.SearchPreferencesDialog;
import eu.isas.peptideshaker.myparameters.PSSettings;
import eu.isas.peptideshaker.preferences.SearchParameters;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
     * Boolean indicating whether we are opening a peptideshaker file.
     */
    private boolean isPsFile = false;
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
        idFilesTxt.setText(idFiles.size() + " file(s) selected");
        spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
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
        jPanel1 = new javax.swing.JPanel();
        minPepLengthTxt = new javax.swing.JTextField();
        maxPepLengthTxt = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        massDeviationTxt = new javax.swing.JTextField();
        precMassUnitCmb = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        mascotMaxEvalueTxt = new javax.swing.JTextField();
        xtandemMaxEvalueTxt = new javax.swing.JTextField();
        omssaMaxEvalueTxt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        searchTxt = new javax.swing.JTextField();
        editSearchButton = new javax.swing.JButton();
        projectDetailsPanel = new javax.swing.JPanel();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PeptideShaker - Open Files");
        setResizable(false);

        sampleDetailsPanel.setBackground(new java.awt.Color(230, 230, 230));

        openButton.setText("Open");
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

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Details"));
        jPanel7.setOpaque(false);

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

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectNameIdTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE)
                    .addComponent(sampleNameIdtxt, javax.swing.GroupLayout.DEFAULT_SIZE, 482, Short.MAX_VALUE))
                .addGap(20, 20, 20)
                .addComponent(jLabel9)
                .addGap(18, 18, 18)
                .addComponent(replicateNumberIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectNameIdTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleNameIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(replicateNumberIdtxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel8))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel12.setFont(jLabel12.getFont().deriveFont((jLabel12.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel12.setText("Insert the required information and click Open to load and view the results.");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Processing Parameters"));
        jPanel1.setOpaque(false);

        minPepLengthTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        minPepLengthTxt.setText("8");
        minPepLengthTxt.setToolTipText("Minimum Peptide Length");

        maxPepLengthTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        maxPepLengthTxt.setText("20");
        maxPepLengthTxt.setToolTipText("Maximum Peptide Length");

        jLabel11.setText("Precursor Accuracy:");

        massDeviationTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        massDeviationTxt.setText("10");
        massDeviationTxt.setToolTipText("Precursor Ion Mass Accuracy");

        precMassUnitCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ppm", "Da" }));

        jLabel7.setText("Peptide Length:");

        jLabel4.setText("OMSSA Max E-Value:");

        jLabel5.setText("X!Tandem Max E-Value:");

        mascotMaxEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        mascotMaxEvalueTxt.setText("10");

        xtandemMaxEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        xtandemMaxEvalueTxt.setText("10");

        omssaMaxEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        omssaMaxEvalueTxt.setText("10");

        jLabel3.setText("Mascot Max E-Value:");

        jLabel13.setText("-");

        jLabel6.setText("Search Parameters:");

        searchTxt.setEditable(false);
        searchTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        searchTxt.setText("Default");

        editSearchButton.setText("Edit");
        editSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSearchButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(omssaMaxEvalueTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(xtandemMaxEvalueTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(mascotMaxEvalueTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE)))
                .addGap(50, 50, 50)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(massDeviationTxt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                    .addComponent(minPepLengthTxt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(precMassUnitCmb, javax.swing.GroupLayout.Alignment.TRAILING, 0, 106, Short.MAX_VALUE)
                    .addComponent(maxPepLengthTxt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)
                    .addComponent(editSearchButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(omssaMaxEvalueTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(maxPepLengthTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(minPepLengthTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(xtandemMaxEvalueTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(precMassUnitCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(massDeviationTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                    .addComponent(jLabel3)
                    .addComponent(mascotMaxEvalueTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(searchTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(editSearchButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        projectDetailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Input Files"));
        projectDetailsPanel.setOpaque(false);

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

        jLabel14.setText("FASTA File:");

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

        javax.swing.GroupLayout projectDetailsPanelLayout = new javax.swing.GroupLayout(projectDetailsPanel);
        projectDetailsPanel.setLayout(projectDetailsPanelLayout);
        projectDetailsPanelLayout.setHorizontalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, projectDetailsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(idFilesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseId)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearId))
                    .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spectrumFilesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                        .addGap(10, 10, 10)
                        .addComponent(browseSpectra)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearSpectra))
                    .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fastaFileTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                        .addGap(10, 10, 10)
                        .addComponent(browseDbButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearDbButton)))
                .addContainerGap())
        );

        projectDetailsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseId, clearId});

        projectDetailsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseSpectra, clearSpectra});

        projectDetailsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {browseDbButton, clearDbButton});

        projectDetailsPanelLayout.setVerticalGroup(
            projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(projectDetailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(idFilesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(clearId)
                    .addComponent(browseId))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(clearSpectra)
                    .addComponent(browseSpectra)
                    .addComponent(spectrumFilesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(projectDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
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

        javax.swing.GroupLayout sampleDetailsPanelLayout = new javax.swing.GroupLayout(sampleDetailsPanel);
        sampleDetailsPanel.setLayout(sampleDetailsPanelLayout);
        sampleDetailsPanelLayout.setHorizontalGroup(
            sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jPanel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(projectDetailsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(sampleDetailsPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 279, Short.MAX_VALUE)
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
                .addComponent(projectDetailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sampleDetailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel12)
                        .addComponent(exitButton)
                        .addComponent(openButton))
                    .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE))
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
            .addComponent(sampleDetailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

            // clear the previuos data
            peptideShakerGUI.clearData();

            this.setVisible(false);

            if (!idFiles.isEmpty() && !isPsFile) {
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

            // add one more just to not start at 0%
            progressCounter++;

            waitingDialog.setMaxProgressValue(progressCounter);
            waitingDialog.increaseProgressValue();

            boolean needDialog = false;

            // load the identification files
            if (idFiles.size() > 0 && !isPsFile
                    || fastaFile != null
                    || spectrumFiles.size() > 0) {
                needDialog = true;
                importIdentificationFiles(waitingDialog);
            }

            if (needDialog) {
                waitingDialog.setVisible(true);
                this.dispose();
            }

            if (!needDialog || !waitingDialog.isRunCanceled()) {
                peptideShakerGUI.setProject(experiment, sample, replicateNumber);
                peptideShakerGUI.displayResults(true);
                peptideShakerGUI.setFrameTitle(projectNameIdTxt.getText().trim());
                this.dispose();
            }
        }
}//GEN-LAST:event_openButtonActionPerformed

    /**
     * @TODO: implement me
     * 
     * @param evt 
     */
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
        int returnVal = fileChooser.showDialog(this.getParent(), "Add");
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

            if (idFiles.size() > 1) {
                for (File file : idFiles) {
                    if (file.getName().endsWith(".cps")) {
                        JOptionPane.showMessageDialog(this, "A PeptideShaker file must be imported alone.", "Wrong identification file.", JOptionPane.ERROR_MESSAGE);
                        idFiles = new ArrayList<File>();
                    }
                }
            }
            idFilesTxt.setText(idFiles.size() + " file(s) selected");

            if (!idFiles.isEmpty()) {
                openButton.setEnabled(true);
            }

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
                minPepLengthTxt.setEditable(false);
                massDeviationTxt.setEditable(false);
                precMassUnitCmb.setEnabled(false);
                fastaFileTxt.setText("");
                browseDbButton.setEnabled(false);
                clearDbButton.setEnabled(false);
                editSearchButton.setEnabled(false);
                searchTxt.setText("");
            } else {
                experiment = null;
                projectNameIdTxt.setEditable(true);
                sampleNameIdtxt.setEditable(true);
                replicateNumberIdtxt.setEditable(true);
                mascotMaxEvalueTxt.setEditable(true);
                omssaMaxEvalueTxt.setEditable(true);
                xtandemMaxEvalueTxt.setEditable(true);
                maxPepLengthTxt.setEditable(true);
                minPepLengthTxt.setEditable(true);
                massDeviationTxt.setEditable(true);
                precMassUnitCmb.setEnabled(true);
                browseDbButton.setEnabled(true);
                clearDbButton.setEnabled(true);
                editSearchButton.setEnabled(true);

                if (isPsFile) {
                    projectNameIdTxt.setText("New Project");
                    sampleNameIdtxt.setText("New Sample");
                    replicateNumberIdtxt.setText("0");
                    mascotMaxEvalueTxt.setText("10");
                    omssaMaxEvalueTxt.setText("10");
                    xtandemMaxEvalueTxt.setText("10");
                    maxPepLengthTxt.setText("20");
                    minPepLengthTxt.setText("8");
                    massDeviationTxt.setText("10");
                    precMassUnitCmb.setSelectedItem("ppm");
                    fastaFileTxt.setText(fastaFile.getName());
                }

                isPsFile = false;
            }
        }
}//GEN-LAST:event_browseIdActionPerformed

    /**
     * Open the SearchPreferences dialog.
     * 
     * @param evt 
     */
    private void editSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSearchButtonActionPerformed
        new SearchPreferencesDialog(peptideShakerGUI);
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
        new HelpWindow(peptideShakerGUI, getClass().getResource("/helpFiles/OpenDialog.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_openDialogHelpJButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseDbButton;
    private javax.swing.JButton browseId;
    private javax.swing.JButton browseSpectra;
    private javax.swing.JButton clearDbButton;
    private javax.swing.JButton clearId;
    private javax.swing.JButton clearSpectra;
    private javax.swing.JButton editSearchButton;
    private javax.swing.JButton exitButton;
    private javax.swing.JTextField fastaFileTxt;
    private javax.swing.JTextField idFilesTxt;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JTextField mascotMaxEvalueTxt;
    private javax.swing.JTextField massDeviationTxt;
    private javax.swing.JTextField maxPepLengthTxt;
    private javax.swing.JTextField minPepLengthTxt;
    private javax.swing.JTextField omssaMaxEvalueTxt;
    private javax.swing.JButton openButton;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JComboBox precMassUnitCmb;
    private javax.swing.JPanel projectDetailsPanel;
    private javax.swing.JTextField projectNameIdTxt;
    private javax.swing.JTextField replicateNumberIdtxt;
    private javax.swing.JPanel sampleDetailsPanel;
    private javax.swing.JTextField sampleNameIdtxt;
    private javax.swing.JTextField searchTxt;
    private javax.swing.JTextField spectrumFilesTxt;
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
        if (fastaFile == null && !isPsFile) {
            JOptionPane.showMessageDialog(null, "Please verify the input for FASTA file.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        peptideShakerGUI.setDataSaved(isPsFile);
        
        return true;
    }

    /**
     * Returns the minimum peptide length.
     *
     * @return the minimum peptide length
     */
    private int getMinPeptideLength() {
        String input = minPepLengthTxt.getText().trim();
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
     * Returns the maximal mass deviation allowed.
     * 
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
     * Imports identifications form identification files.
     * 
     * @param waitingDialog a dialog to display feedback to the user
     */
    private void importIdentificationFiles(WaitingDialog waitingDialog) {
        boolean precTolUnit = ((String) precMassUnitCmb.getSelectedItem()).equals("ppm");
        IdFilter idFilter = new IdFilter(getMinPeptideLength(), getMaxPeptideLength(), getMascotMaxEvalue(), getOmssaMaxEvalue(), getXtandemMaxEvalue(), getMaxMassDeviation(), precTolUnit);
        peptideShaker.importFiles(waitingDialog, idFilter, idFiles, spectrumFiles, fastaFile, peptideShakerGUI.getSearchParameters());
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
                if (!searchParameters.getModificationProfile().containsKey(name)) {
                    searchParameters.addExpectedModification(name, name);
                }
            }

            temp = props.getProperty(IdentificationParametersReader.ENZYME);

            if (temp != null && !temp.equals("")) {
                searchParameters.setEnzyme(enzymeFactory.getEnzyme(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.FRAGMENT_MASS_TOLERANCE);

            if (temp != null) {
                searchParameters.setFragmentIonMZTolerance(new Double(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.PRECURSOR_MASS_TOLERANCE);

            if (temp != null) {
                massDeviationTxt.setText(temp);
            }

            temp = props.getProperty(IdentificationParametersReader.PRECURSOR_MASS_TOLERANCE_UNIT);

            if (temp != null) {
                precMassUnitCmb.setSelectedItem(temp);
            }

            temp = props.getProperty(IdentificationParametersReader.MISSED_CLEAVAGES);

            if (temp != null) {
                searchParameters.setnMissedCleavages(new Integer(temp.trim()));
            }

            temp = props.getProperty(IdentificationParametersReader.MIN_PEPTIDE_SIZE);

            if (temp != null && temp.length() > 0) {
                minPepLengthTxt.setText(temp);
            }

            temp = props.getProperty(IdentificationParametersReader.MAX_PEPTIDE_SIZE);

            if (temp != null && temp.length() > 0) {
                maxPepLengthTxt.setText(temp);
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
            peptideShakerGUI.setSearchParameters(searchParameters);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, searchGUIFile.getName() + " not found.", "File Not Found", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "An error occured while reading " + searchGUIFile.getName()
                    + ". Please verify the version compatibility.", "File Import error", JOptionPane.WARNING_MESSAGE);
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

    /**
     * Loads the new project information
     */
    private void loadProject() {

        projectNameIdTxt.setText(experiment.getReference());

        PSSettings experimentSettings = new PSSettings();
        experimentSettings = (PSSettings) experiment.getUrParam(experimentSettings);
        peptideShakerGUI.setAnnotationPreferences(experimentSettings.getAnnotationPreferences());
        peptideShakerGUI.setSearchParameters(experimentSettings.getSearchParameters());

        ArrayList<String> names = new ArrayList<String>();
        for (File file : spectrumFiles) {
            names.add(file.getName());
        }
        for (String filePath : experimentSettings.getSearchParameters().getSpectrumFiles()) {
            try {
                File newFile = new File(filePath);
                if (newFile.exists()
                        && !names.contains(newFile.getName())) {
                    names.add(newFile.getName());
                    spectrumFiles.add(newFile);
                }
            } catch (Exception e) {
            }
        }
        if (spectrumFiles.size() > 0) {
            spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
        }

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
        minPepLengthTxt.setText("");
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
        progressDialog = new ProgressDialogX(this, this, true);
        progressDialog.doNothingOnClose();

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIndeterminate(true);
                progressDialog.setTitle("Importing. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("ImportThread") {

            @Override
            public void run() {

                try {
                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                    experiment = experimentIO.loadExperiment(psFile);
                    loadProject();

                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    // change the peptide shaker icon back to the default version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                    int option = JOptionPane.showConfirmDialog(tempRef,
                            "Experiment \'" + experiment.getReference()
                            + "\' imported.\n"
                            + "View the results?", "Identifications Imported", JOptionPane.YES_NO_OPTION);

                    if (option == JOptionPane.YES_OPTION) {
                        tempRef.setVisible(false);
                        tempRef.openButtonActionPerformed(null);
                    }

                } catch (Exception e) {

                    // change the peptide shaker icon back to the default version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    JOptionPane.showMessageDialog(tempRef,
                            "An error occured while reading" + psFile + ".\\"
                            + "Please verif that the compomics utilities version used to create\n"
                            + "the file is compatible with your version of Peptide-Shaker.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
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
