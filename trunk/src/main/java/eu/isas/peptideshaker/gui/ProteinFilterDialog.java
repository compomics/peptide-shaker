package eu.isas.peptideshaker.gui;

import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

/**
 * A dialog displaying various filters that can be applied to the protein tables.
 *
 * @author Harald Barsnes
 */
public class ProteinFilterDialog extends javax.swing.JDialog {

    /**
     * The protein table.
     */
    private JTable proteinTable;
    /**
     * The PeptideShakerGUI parent frame.
     */
    private PeptideShakerGUI peptideShakerGUI;

    /**
     * Creates a new ProteinFilter dialog.
     *
     * @param peptideShakerGUI                              the PeptideShakerGUI parent frame
     * @param modal                                         if the dialog is modal or not
     * @param visible                                       if true the dialog is made visible
     */
    public ProteinFilterDialog(PeptideShakerGUI peptideShakerGUI, boolean modal, boolean visible) {
        super(peptideShakerGUI, modal);

        this.peptideShakerGUI = peptideShakerGUI;

        //proteinTable = peptideShakerGUI.getOverviewProteinTable();

        initComponents();
        
        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();

        // update the filter properties
        proteinAccessionJTextField.setText(filterPreferences.getProteinAccession());
        descriptionJTextField.setText(filterPreferences.getProteinDescription());
        coverageJTextField.setText(filterPreferences.getProteinCoverage());
        spectrumCountingJTextField.setText(filterPreferences.getSpectrumCounting());
        peptideJTextField.setText(filterPreferences.getnPeptides());
        spectraJTextField.setText(filterPreferences.getProteinNSpectra());
        scoreJTextField.setText(filterPreferences.getProteinScore());
        confidenceJTextField.setText(filterPreferences.getProteinConfidence());

        // update the radio buttons
        coverageGreaterThanJRadioButton.setSelected(filterPreferences.getCoverageButtonSelection() == 0);
        coverageEqualJRadioButton.setSelected(filterPreferences.getCoverageButtonSelection() == 1);
        coverageLessThanJRadioButton.setSelected(filterPreferences.getCoverageButtonSelection() == 2);

        spectrumCountingGreaterThanJRadioButton.setSelected(filterPreferences.getSpectrumCountingButtonSelection() == 0);
        spectrumCountingEqualJRadioButton.setSelected(filterPreferences.getSpectrumCountingButtonSelection() == 1);
        spectrumCountingLessThanJRadioButton.setSelected(filterPreferences.getSpectrumCountingButtonSelection() == 2);

        peptideGreaterThanJRadioButton.setSelected(filterPreferences.getnPeptidesButtonSelection() == 0);
        peptideEqualJRadioButton.setSelected(filterPreferences.getnPeptidesButtonSelection() == 1);
        peptideLessThanJRadioButton.setSelected(filterPreferences.getnPeptidesButtonSelection() == 2);

        spectraGreaterThanJRadioButton.setSelected(filterPreferences.getProteinNSpectraButtonSelection() == 0);
        spectraEqualJRadioButton.setSelected(filterPreferences.getProteinNSpectraButtonSelection() == 1);
        spectraLessThanJRadioButton.setSelected(filterPreferences.getProteinNSpectraButtonSelection() == 2);

        scoreGreaterThanJRadioButton.setSelected(filterPreferences.getProteinScoreButtonSelection() == 0);
        scoreEqualJRadioButton.setSelected(filterPreferences.getProteinScoreButtonSelection() == 1);
        scoreLessThanJRadioButton.setSelected(filterPreferences.getProteinScoreButtonSelection() == 2);

        confidenceGreaterThanJRadioButton.setSelected(filterPreferences.getProteinConfidenceButtonSelection() == 0);
        confidenceEqualJRadioButton.setSelected(filterPreferences.getProteinConfidenceButtonSelection() == 1);
        confidenceLessThanJRadioButton.setSelected(filterPreferences.getProteinConfidenceButtonSelection() == 2);

        if (filterPreferences.getCurrentProteinInferenceFilterSelection() == PSParameter.NOT_GROUP) {
            singleProteinJRadioButton.setSelected(true);
        } else if (filterPreferences.getCurrentProteinInferenceFilterSelection() == PSParameter.ISOFORMS) {
            isoformsJRadioButton.setSelected(true);
        } else if (filterPreferences.getCurrentProteinInferenceFilterSelection() == PSParameter.ISOFORMS_UNRELATED) {
            unrelatedIsoformsJRadioButton.setSelected(true);
        } else if (filterPreferences.getCurrentProteinInferenceFilterSelection() == PSParameter.UNRELATED) {
            unrelatedProteinsJRadioButton.setSelected(true);
        } else {
            allJRadioButton.setSelected(true);
        }

        showHiddenProteinsCheckBox.setSelected(peptideShakerGUI.getDisplayPreferences().showHiddenProteins());
        
        // set the focus traveral policy
        setUpFocusTraversal();

        peptideShakerGUI.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        setLocationRelativeTo(peptideShakerGUI);
        setVisible(visible);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        peptidesButtonGroup = new javax.swing.ButtonGroup();
        coverageButtonGroup = new javax.swing.ButtonGroup();
        expCountButtonGroup = new javax.swing.ButtonGroup();
        foldChangeButtonGroup = new javax.swing.ButtonGroup();
        pValueButtonGroup = new javax.swing.ButtonGroup();
        qValueButtonGroup = new javax.swing.ButtonGroup();
        piButtonGroup = new javax.swing.ButtonGroup();
        backgroundPanel = new javax.swing.JPanel();
        optionsPanel = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        proteinAccessionJTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        coverageJTextField = new javax.swing.JTextField();
        spectrumCountingJTextField = new javax.swing.JTextField();
        spectraJTextField = new javax.swing.JTextField();
        peptideJTextField = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        scoreJTextField = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        confidenceJTextField = new javax.swing.JTextField();
        coverageGreaterThanJRadioButton = new javax.swing.JRadioButton();
        coverageEqualJRadioButton = new javax.swing.JRadioButton();
        coverageLessThanJRadioButton = new javax.swing.JRadioButton();
        spectrumCountingGreaterThanJRadioButton = new javax.swing.JRadioButton();
        spectrumCountingEqualJRadioButton = new javax.swing.JRadioButton();
        spectrumCountingLessThanJRadioButton = new javax.swing.JRadioButton();
        peptideGreaterThanJRadioButton = new javax.swing.JRadioButton();
        peptideEqualJRadioButton = new javax.swing.JRadioButton();
        peptideLessThanJRadioButton = new javax.swing.JRadioButton();
        spectraGreaterThanJRadioButton = new javax.swing.JRadioButton();
        spectraEqualJRadioButton = new javax.swing.JRadioButton();
        spectraLessThanJRadioButton = new javax.swing.JRadioButton();
        scoreGreaterThanJRadioButton = new javax.swing.JRadioButton();
        scoreEqualJRadioButton = new javax.swing.JRadioButton();
        scoreLessThanJRadioButton = new javax.swing.JRadioButton();
        confidenceGreaterThanJRadioButton = new javax.swing.JRadioButton();
        confidenceEqualJRadioButton = new javax.swing.JRadioButton();
        confidenceLessThanJRadioButton = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        descriptionJTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        singleProteinJRadioButton = new javax.swing.JRadioButton();
        isoformsJRadioButton = new javax.swing.JRadioButton();
        unrelatedIsoformsJRadioButton = new javax.swing.JRadioButton();
        unrelatedProteinsJRadioButton = new javax.swing.JRadioButton();
        allJRadioButton = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        showHiddenProteinsCheckBox = new javax.swing.JCheckBox();
        clearJButton = new javax.swing.JButton();
        okJButton = new javax.swing.JButton();
        proteinFilteringHelpJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Protein Filter");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        optionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Fillter Options"));
        optionsPanel.setOpaque(false);

        jLabel9.setText("Accession:");

        proteinAccessionJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinAccessionJTextField.setToolTipText("<html>\nFind all proteins containing a given string.<br>\nRegular expressions are supported.\n</html>");
        proteinAccessionJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinAccessionJTextFieldKeyReleased(evt);
            }
        });

        jLabel10.setText("Coverage:");

        jLabel11.setText("emPAI/NSAF:");

        jLabel12.setText("#Peptides:");

        jLabel13.setText("#Spectra:");

        coverageJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        coverageJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                coverageJTextFieldKeyReleased(evt);
            }
        });

        spectrumCountingJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        spectrumCountingJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                spectrumCountingJTextFieldKeyReleased(evt);
            }
        });

        spectraJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        spectraJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                spectraJTextFieldKeyReleased(evt);
            }
        });

        peptideJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideJTextFieldKeyReleased(evt);
            }
        });

        jLabel14.setText("Score:");

        scoreJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        scoreJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                scoreJTextFieldKeyReleased(evt);
            }
        });

        jLabel15.setText("Confidence:");

        confidenceJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        confidenceJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                confidenceJTextFieldKeyReleased(evt);
            }
        });

        peptidesButtonGroup.add(coverageGreaterThanJRadioButton);
        coverageGreaterThanJRadioButton.setSelected(true);
        coverageGreaterThanJRadioButton.setText(">");
        coverageGreaterThanJRadioButton.setOpaque(false);
        coverageGreaterThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        peptidesButtonGroup.add(coverageEqualJRadioButton);
        coverageEqualJRadioButton.setText("=");
        coverageEqualJRadioButton.setOpaque(false);
        coverageEqualJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        peptidesButtonGroup.add(coverageLessThanJRadioButton);
        coverageLessThanJRadioButton.setText("<");
        coverageLessThanJRadioButton.setOpaque(false);
        coverageLessThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        coverageButtonGroup.add(spectrumCountingGreaterThanJRadioButton);
        spectrumCountingGreaterThanJRadioButton.setSelected(true);
        spectrumCountingGreaterThanJRadioButton.setText(">");
        spectrumCountingGreaterThanJRadioButton.setOpaque(false);
        spectrumCountingGreaterThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        coverageButtonGroup.add(spectrumCountingEqualJRadioButton);
        spectrumCountingEqualJRadioButton.setText("=");
        spectrumCountingEqualJRadioButton.setOpaque(false);
        spectrumCountingEqualJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        coverageButtonGroup.add(spectrumCountingLessThanJRadioButton);
        spectrumCountingLessThanJRadioButton.setText("<");
        spectrumCountingLessThanJRadioButton.setOpaque(false);
        spectrumCountingLessThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        expCountButtonGroup.add(peptideGreaterThanJRadioButton);
        peptideGreaterThanJRadioButton.setSelected(true);
        peptideGreaterThanJRadioButton.setText(">");
        peptideGreaterThanJRadioButton.setOpaque(false);
        peptideGreaterThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        expCountButtonGroup.add(peptideEqualJRadioButton);
        peptideEqualJRadioButton.setText("=");
        peptideEqualJRadioButton.setOpaque(false);
        peptideEqualJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        expCountButtonGroup.add(peptideLessThanJRadioButton);
        peptideLessThanJRadioButton.setText("<");
        peptideLessThanJRadioButton.setOpaque(false);
        peptideLessThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        foldChangeButtonGroup.add(spectraGreaterThanJRadioButton);
        spectraGreaterThanJRadioButton.setSelected(true);
        spectraGreaterThanJRadioButton.setText(">");
        spectraGreaterThanJRadioButton.setOpaque(false);
        spectraGreaterThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        foldChangeButtonGroup.add(spectraEqualJRadioButton);
        spectraEqualJRadioButton.setText("=");
        spectraEqualJRadioButton.setOpaque(false);
        spectraEqualJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        foldChangeButtonGroup.add(spectraLessThanJRadioButton);
        spectraLessThanJRadioButton.setText("<");
        spectraLessThanJRadioButton.setOpaque(false);
        spectraLessThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        pValueButtonGroup.add(scoreGreaterThanJRadioButton);
        scoreGreaterThanJRadioButton.setSelected(true);
        scoreGreaterThanJRadioButton.setText(">");
        scoreGreaterThanJRadioButton.setOpaque(false);
        scoreGreaterThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        pValueButtonGroup.add(scoreEqualJRadioButton);
        scoreEqualJRadioButton.setText("=");
        scoreEqualJRadioButton.setOpaque(false);
        scoreEqualJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        pValueButtonGroup.add(scoreLessThanJRadioButton);
        scoreLessThanJRadioButton.setText("<");
        scoreLessThanJRadioButton.setOpaque(false);
        scoreLessThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        qValueButtonGroup.add(confidenceGreaterThanJRadioButton);
        confidenceGreaterThanJRadioButton.setSelected(true);
        confidenceGreaterThanJRadioButton.setText(">");
        confidenceGreaterThanJRadioButton.setOpaque(false);
        confidenceGreaterThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        qValueButtonGroup.add(confidenceEqualJRadioButton);
        confidenceEqualJRadioButton.setText("=");
        confidenceEqualJRadioButton.setOpaque(false);
        confidenceEqualJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        qValueButtonGroup.add(confidenceLessThanJRadioButton);
        confidenceLessThanJRadioButton.setText("<");
        confidenceLessThanJRadioButton.setOpaque(false);
        confidenceLessThanJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonActionPerformed(evt);
            }
        });

        jLabel1.setFont(jLabel1.getFont().deriveFont((jLabel1.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel1.setText("(contains, RegExp)");
        jLabel1.setToolTipText("<html>\nFind all proteins containing a given string.<br>\nRegular expressions are supported.\n</html>");

        jLabel16.setText("Description:");

        descriptionJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        descriptionJTextField.setToolTipText("<html>\nFind all proteins containing a given accession number.<br>\nRegular expressions are supported.\n</html>");
        descriptionJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                descriptionJTextFieldKeyReleased(evt);
            }
        });

        jLabel2.setFont(jLabel2.getFont().deriveFont((jLabel2.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel2.setText("(contains, RegExp)");
        jLabel2.setToolTipText("<html>\nFind all proteins containing a given accession number.<br>\nRegular expressions are supported.\n</html>");

        jLabel3.setText("PI:");
        jLabel3.setToolTipText("Protein Inference");

        piButtonGroup.add(singleProteinJRadioButton);
        singleProteinJRadioButton.setText("Single Protein");
        singleProteinJRadioButton.setIconTextGap(10);
        singleProteinJRadioButton.setOpaque(false);
        singleProteinJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                singleProteinJRadioButtonActionPerformed(evt);
            }
        });

        piButtonGroup.add(isoformsJRadioButton);
        isoformsJRadioButton.setText("Isoforms");
        isoformsJRadioButton.setIconTextGap(10);
        isoformsJRadioButton.setOpaque(false);
        isoformsJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                isoformsJRadioButtonActionPerformed(evt);
            }
        });

        piButtonGroup.add(unrelatedIsoformsJRadioButton);
        unrelatedIsoformsJRadioButton.setText("Unrelated Isoforms");
        unrelatedIsoformsJRadioButton.setIconTextGap(10);
        unrelatedIsoformsJRadioButton.setOpaque(false);
        unrelatedIsoformsJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unrelatedIsoformsJRadioButtonActionPerformed(evt);
            }
        });

        piButtonGroup.add(unrelatedProteinsJRadioButton);
        unrelatedProteinsJRadioButton.setText("Unrelated Proteins");
        unrelatedProteinsJRadioButton.setIconTextGap(10);
        unrelatedProteinsJRadioButton.setOpaque(false);
        unrelatedProteinsJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unrelatedProteinsJRadioButtonActionPerformed(evt);
            }
        });

        piButtonGroup.add(allJRadioButton);
        allJRadioButton.setSelected(true);
        allJRadioButton.setText("All Types");
        allJRadioButton.setIconTextGap(10);
        allJRadioButton.setOpaque(false);
        allJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allJRadioButtonActionPerformed(evt);
            }
        });

        jLabel4.setText("All Proteins:");
        jLabel4.setToolTipText("Show the hidden proteins");

        showHiddenProteinsCheckBox.setFont(showHiddenProteinsCheckBox.getFont().deriveFont((showHiddenProteinsCheckBox.getFont().getStyle() | java.awt.Font.ITALIC)));
        showHiddenProteinsCheckBox.setSelected(true);
        showHiddenProteinsCheckBox.setText("(show the hidden proteins)");
        showHiddenProteinsCheckBox.setToolTipText("Show the hidden proteins");
        showHiddenProteinsCheckBox.setIconTextGap(15);
        showHiddenProteinsCheckBox.setOpaque(false);
        showHiddenProteinsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showHiddenProteinsCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout optionsPanelLayout = new javax.swing.GroupLayout(optionsPanel);
        optionsPanel.setLayout(optionsPanelLayout);
        optionsPanelLayout.setHorizontalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(jLabel15)
                    .addComponent(jLabel14)
                    .addComponent(jLabel12)
                    .addComponent(jLabel11)
                    .addComponent(jLabel10)
                    .addComponent(jLabel9)
                    .addComponent(jLabel16)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(showHiddenProteinsCheckBox)
                    .addGroup(optionsPanelLayout.createSequentialGroup()
                        .addComponent(allJRadioButton)
                        .addContainerGap())
                    .addGroup(optionsPanelLayout.createSequentialGroup()
                        .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addComponent(isoformsJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(unrelatedProteinsJRadioButton))
                            .addComponent(confidenceJTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(scoreJTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spectraJTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(peptideJTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spectrumCountingJTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(coverageJTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(descriptionJTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(proteinAccessionJTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, optionsPanelLayout.createSequentialGroup()
                                .addComponent(singleProteinJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(unrelatedIsoformsJRadioButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                        .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addComponent(coverageGreaterThanJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(coverageEqualJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(coverageLessThanJRadioButton))
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addComponent(spectrumCountingGreaterThanJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(spectrumCountingEqualJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(spectrumCountingLessThanJRadioButton))
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addComponent(peptideGreaterThanJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(peptideEqualJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(peptideLessThanJRadioButton))
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addComponent(spectraGreaterThanJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(spectraEqualJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(spectraLessThanJRadioButton))
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addComponent(scoreGreaterThanJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(scoreEqualJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(scoreLessThanJRadioButton))
                            .addGroup(optionsPanelLayout.createSequentialGroup()
                                .addComponent(confidenceGreaterThanJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(confidenceEqualJRadioButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(confidenceLessThanJRadioButton))
                            .addComponent(jLabel2))
                        .addGap(10, 10, 10))))
        );

        optionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {allJRadioButton, isoformsJRadioButton, singleProteinJRadioButton, unrelatedIsoformsJRadioButton, unrelatedProteinsJRadioButton});

        optionsPanelLayout.setVerticalGroup(
            optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(optionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proteinAccessionJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(descriptionJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coverageJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(coverageGreaterThanJRadioButton)
                    .addComponent(coverageEqualJRadioButton)
                    .addComponent(coverageLessThanJRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(spectrumCountingJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spectrumCountingGreaterThanJRadioButton)
                    .addComponent(spectrumCountingEqualJRadioButton)
                    .addComponent(spectrumCountingLessThanJRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(peptideJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(peptideGreaterThanJRadioButton)
                    .addComponent(peptideEqualJRadioButton)
                    .addComponent(peptideLessThanJRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spectraJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(spectraGreaterThanJRadioButton)
                    .addComponent(spectraEqualJRadioButton)
                    .addComponent(spectraLessThanJRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scoreJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14)
                    .addComponent(scoreGreaterThanJRadioButton)
                    .addComponent(scoreEqualJRadioButton)
                    .addComponent(scoreLessThanJRadioButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(confidenceJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(confidenceGreaterThanJRadioButton)
                    .addComponent(confidenceEqualJRadioButton)
                    .addComponent(confidenceLessThanJRadioButton))
                .addGap(18, 18, 18)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(singleProteinJRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(unrelatedIsoformsJRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(isoformsJRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(unrelatedProteinsJRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(allJRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(optionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(showHiddenProteinsCheckBox)
                    .addComponent(jLabel4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        optionsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {allJRadioButton, isoformsJRadioButton, singleProteinJRadioButton, unrelatedIsoformsJRadioButton, unrelatedProteinsJRadioButton});

        clearJButton.setText("Clear");
        clearJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearJButtonActionPerformed(evt);
            }
        });

        okJButton.setText("OK");
        okJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okJButtonActionPerformed(evt);
            }
        });

        proteinFilteringHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        proteinFilteringHelpJButton.setToolTipText("Help");
        proteinFilteringHelpJButton.setBorder(null);
        proteinFilteringHelpJButton.setBorderPainted(false);
        proteinFilteringHelpJButton.setContentAreaFilled(false);
        proteinFilteringHelpJButton.setFocusable(false);
        proteinFilteringHelpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        proteinFilteringHelpJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        proteinFilteringHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                proteinFilteringHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinFilteringHelpJButtonMouseExited(evt);
            }
        });
        proteinFilteringHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinFilteringHelpJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(optionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(proteinFilteringHelpJButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 341, Short.MAX_VALUE)
                .addComponent(okJButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(clearJButton)
                .addGap(18, 18, 18))
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {clearJButton, okJButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(optionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinFilteringHelpJButton)
                    .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                        .addComponent(clearJButton)
                        .addComponent(okJButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Clears all filters.
     *
     * @param evt
     */
    private void clearJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearJButtonActionPerformed
        proteinAccessionJTextField.setText("");
        descriptionJTextField.setText("");
        coverageJTextField.setText("");
        spectrumCountingJTextField.setText("");
        peptideJTextField.setText("");
        spectraJTextField.setText("");
        scoreJTextField.setText("");
        confidenceJTextField.setText("");
        allJRadioButton.setSelected(true);
        filter();
    }//GEN-LAST:event_clearJButtonActionPerformed

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void proteinAccessionJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinAccessionJTextFieldKeyReleased
        filter();
    }//GEN-LAST:event_proteinAccessionJTextFieldKeyReleased

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void coverageJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_coverageJTextFieldKeyReleased
        filter();
    }//GEN-LAST:event_coverageJTextFieldKeyReleased

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void spectrumCountingJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectrumCountingJTextFieldKeyReleased
        filter();
    }//GEN-LAST:event_spectrumCountingJTextFieldKeyReleased

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void peptideJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideJTextFieldKeyReleased
        filter();
    }//GEN-LAST:event_peptideJTextFieldKeyReleased

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void spectraJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectraJTextFieldKeyReleased
        filter();
    }//GEN-LAST:event_spectraJTextFieldKeyReleased

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void scoreJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_scoreJTextFieldKeyReleased
        filter();
    }//GEN-LAST:event_scoreJTextFieldKeyReleased

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void confidenceJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_confidenceJTextFieldKeyReleased
        filter();
    }//GEN-LAST:event_confidenceJTextFieldKeyReleased

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void radioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonActionPerformed
        filter();
    }//GEN-LAST:event_radioButtonActionPerformed

    /**
     * Saves the filter settings and closes the dialog.
     *
     * @param evt
     */
    private void okJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okJButtonActionPerformed

        FilterPreferences filterPreferences = peptideShakerGUI.getFilterPreferences();
        
        filterPreferences.setProteinAccession(proteinAccessionJTextField.getText());
        filterPreferences.setProteinDescription(descriptionJTextField.getText());
        filterPreferences.setProteinCoverage(coverageJTextField.getText());
        filterPreferences.setSpectrumCounting(spectrumCountingJTextField.getText());
        filterPreferences.setnPeptides(peptideJTextField.getText());
        filterPreferences.setProteinNSpectra(spectraJTextField.getText());
        filterPreferences.setProteinScore(scoreJTextField.getText());
        filterPreferences.setProteinConfidence(confidenceJTextField.getText());
        

        if (coverageEqualJRadioButton.isSelected()) {
            filterPreferences.setCoverageButtonSelection(1);
        } else if (coverageLessThanJRadioButton.isSelected()) {
            filterPreferences.setCoverageButtonSelection(2);
        } else {
            filterPreferences.setCoverageButtonSelection(0);
        }

        if (spectrumCountingEqualJRadioButton.isSelected()) {
            filterPreferences.setSpectrumCountingButtonSelection(1);
        } else if (spectrumCountingLessThanJRadioButton.isSelected()) {
            filterPreferences.setSpectrumCountingButtonSelection(2);
        } else {
            filterPreferences.setSpectrumCountingButtonSelection(0);
        }
        if (peptideEqualJRadioButton.isSelected()) {
            filterPreferences.setnPeptidesButtonSelection(1);
        } else if (peptideLessThanJRadioButton.isSelected()) {
            filterPreferences.setnPeptidesButtonSelection(2);
        } else {
            filterPreferences.setnPeptidesButtonSelection(1);
        }

        if (spectraEqualJRadioButton.isSelected()) {
            filterPreferences.setProteinNSpectraButtonSelection(1);
        } else if (spectraLessThanJRadioButton.isSelected()) {
            filterPreferences.setProteinNSpectraButtonSelection(2);
        } else {
            filterPreferences.setProteinNSpectraButtonSelection(0);
        }

        if (scoreEqualJRadioButton.isSelected()) {
            filterPreferences.setProteinScoreButtonSelection(1);
        } else if (scoreLessThanJRadioButton.isSelected()) {
            filterPreferences.setProteinScoreButtonSelection(1);
        } else {
            filterPreferences.setProteinScoreButtonSelection(1);
        }

        if (confidenceEqualJRadioButton.isSelected()) {
            filterPreferences.setProteinConfidenceButtonSelection(1);
        } else if (confidenceLessThanJRadioButton.isSelected()) {
            filterPreferences.setProteinConfidenceButtonSelection(1);
        } else {
            filterPreferences.setProteinConfidenceButtonSelection(1);
        }

        if (singleProteinJRadioButton.isSelected()) {
            filterPreferences.setCurrentProteinInferenceFilterSelection(PSParameter.NOT_GROUP);
        } else if (isoformsJRadioButton.isSelected()) {
            filterPreferences.setCurrentProteinInferenceFilterSelection(PSParameter.ISOFORMS);
        } else if (unrelatedIsoformsJRadioButton.isSelected()) {
            filterPreferences.setCurrentProteinInferenceFilterSelection(PSParameter.ISOFORMS_UNRELATED);
        } else if (unrelatedProteinsJRadioButton.isSelected()) {
            filterPreferences.setCurrentProteinInferenceFilterSelection(PSParameter.UNRELATED);
        } else {
            filterPreferences.setCurrentProteinInferenceFilterSelection(5);
        }

        peptideShakerGUI.setSelectedItems();
        
        // close the dialog
        peptideShakerGUI.setModalExclusionType(ModalExclusionType.NO_EXCLUDE);
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_okJButtonActionPerformed

    /**
     * Closes the dialog.
     * 
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        okJButtonActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void descriptionJTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_descriptionJTextFieldKeyReleased
        filter();
    }//GEN-LAST:event_descriptionJTextFieldKeyReleased

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void proteinFilteringHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinFilteringHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_proteinFilteringHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void proteinFilteringHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinFilteringHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_proteinFilteringHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void proteinFilteringHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinFilteringHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/ProteinFiltering.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_proteinFilteringHelpJButtonActionPerformed

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
private void allJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allJRadioButtonActionPerformed
    filter();
}//GEN-LAST:event_allJRadioButtonActionPerformed

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
private void singleProteinJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_singleProteinJRadioButtonActionPerformed
    filter();
}//GEN-LAST:event_singleProteinJRadioButtonActionPerformed

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
private void unrelatedIsoformsJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unrelatedIsoformsJRadioButtonActionPerformed
    filter();
}//GEN-LAST:event_unrelatedIsoformsJRadioButtonActionPerformed

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
private void isoformsJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_isoformsJRadioButtonActionPerformed
    filter();
}//GEN-LAST:event_isoformsJRadioButtonActionPerformed

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
private void unrelatedProteinsJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unrelatedProteinsJRadioButtonActionPerformed
    filter();
}//GEN-LAST:event_unrelatedProteinsJRadioButtonActionPerformed

    /**
     * Filters the protein table according to the current filter settings.
     *
     * @param evt
     */
    private void showHiddenProteinsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showHiddenProteinsCheckBoxActionPerformed
        filter();
    }//GEN-LAST:event_showHiddenProteinsCheckBoxActionPerformed

    /**
     * Filters the protein table according to the current filter settings.
     */
    public void filter() {

        int previouslySelectedProteinIndex = -1;

        if (proteinTable.getSelectedRow() != -1) {
            previouslySelectedProteinIndex = (Integer) proteinTable.getValueAt(proteinTable.getSelectedRow(), 0);
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

        // protein accession filter
        String text = proteinAccessionJTextField.getText();

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Accession").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for protein accession!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // protein description filter
        text = descriptionJTextField.getText();

        if (text == null || text.length() == 0) {
            filters.add(RowFilter.regexFilter(".*"));
        } else {
            try {
                filters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Description").getModelIndex()));
            } catch (PatternSyntaxException pse) {
                //JOptionPane.showMessageDialog(this, "Bad regex pattern for protein description!", "Filter Error", JOptionPane.ERROR_MESSAGE);
                //pse.printStackTrace();
            }
        }

        // coverage filter
        if (coverageJTextField.getText().length() > 0) {

            try {
                Double value = new Double(coverageJTextField.getText());

                if (coverageGreaterThanJRadioButton.isSelected()) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, proteinTable.getColumn("Coverage").getModelIndex()));
                } else if (coverageEqualJRadioButton.isSelected()) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, proteinTable.getColumn("Coverage").getModelIndex()));
                } else {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, proteinTable.getColumn("Coverage").getModelIndex()));
                }
            } catch (NumberFormatException e) {
                //JOptionPane.showMessageDialog(this, "Coverage has to be a number!", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // emPAI/NSAF filter
        if (spectrumCountingJTextField.getText().length() > 0) {

            try {
                Double value = new Double(spectrumCountingJTextField.getText());

                if (spectrumCountingGreaterThanJRadioButton.isSelected()) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, proteinTable.getColumn("Spectrum Counting").getModelIndex()));
                } else if (spectrumCountingEqualJRadioButton.isSelected()) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, proteinTable.getColumn("Spectrum Counting").getModelIndex()));
                } else {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, proteinTable.getColumn("Spectrum Counting").getModelIndex()));
                }
            } catch (NumberFormatException e) {
                //JOptionPane.showMessageDialog(this, "Spectrum count has to be a number!", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // number of peptides filter
        if (peptideJTextField.getText().length() > 0) {

            try {
                Integer value = new Integer(peptideJTextField.getText());

                if (peptideGreaterThanJRadioButton.isSelected()) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, proteinTable.getColumn("#Peptides").getModelIndex()));
                } else if (peptideEqualJRadioButton.isSelected()) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, proteinTable.getColumn("#Peptides").getModelIndex()));
                } else {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, proteinTable.getColumn("#Peptides").getModelIndex()));
                }
            } catch (NumberFormatException e) {
                //JOptionPane.showMessageDialog(this, "#Peptides has to be an integer!", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // number of spectra filter
        if (spectraJTextField.getText().length() > 0) {

            try {
                Integer value = new Integer(spectraJTextField.getText());

                if (spectraGreaterThanJRadioButton.isSelected()) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, proteinTable.getColumn("#Spectra").getModelIndex()));
                } else if (spectraEqualJRadioButton.isSelected()) {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, proteinTable.getColumn("#Spectra").getModelIndex()));
                } else {
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, proteinTable.getColumn("#Spectra").getModelIndex()));
                }
            } catch (NumberFormatException e) {
                //JOptionPane.showMessageDialog(this, "#Spectra has to be an integer!", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // score filter
        if (scoreJTextField.getText().length() > 0) {

            try {
                Double value = new Double(scoreJTextField.getText());

                if (value != 0) {

                    if (scoreGreaterThanJRadioButton.isSelected()) {
                        filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, proteinTable.getColumn("Score").getModelIndex()));
                    } else if (scoreEqualJRadioButton.isSelected()) {
                        filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, proteinTable.getColumn("Score").getModelIndex()));
                    } else {
                        filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, proteinTable.getColumn("Score").getModelIndex()));
                    }
                }
            } catch (NumberFormatException e) {
                //JOptionPane.showMessageDialog(this, "Score has to be a number!", "Filter Error", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                // if the score column is hidden
            }
        }

        // confidence filter
        if (confidenceJTextField.getText().length() > 0) {

            try {
                Double value = new Double(confidenceJTextField.getText());

                if (value != 0) {

                    if (confidenceGreaterThanJRadioButton.isSelected()) {
                        filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, value, proteinTable.getColumn("Confidence").getModelIndex()));
                    } else if (confidenceEqualJRadioButton.isSelected()) {
                        filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, value, proteinTable.getColumn("Confidence").getModelIndex()));
                    } else {
                        filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, value, proteinTable.getColumn("Confidence").getModelIndex()));
                    }
                }
            } catch (NumberFormatException e) {
                //JOptionPane.showMessageDialog(this, "Confidence has to be a number!", "Filter Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // protein inference filter
        if (singleProteinJRadioButton.isSelected()) {
            filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, PSParameter.NOT_GROUP, proteinTable.getColumn("PI").getModelIndex()));
        }
        if (isoformsJRadioButton.isSelected()) {
            filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, PSParameter.ISOFORMS, proteinTable.getColumn("PI").getModelIndex()));
        }
        if (unrelatedIsoformsJRadioButton.isSelected()) {
            filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, PSParameter.ISOFORMS_UNRELATED, proteinTable.getColumn("PI").getModelIndex()));
        }
        if (unrelatedProteinsJRadioButton.isSelected()) {
            filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, PSParameter.UNRELATED, proteinTable.getColumn("PI").getModelIndex()));
        }

        // show hidden proteins filter
        if (!showHiddenProteinsCheckBox.isSelected()) {

            RowFilter<Object, Object> hiddenFilter = new RowFilter<Object, Object>() {

                public boolean include(Entry<? extends Object, ? extends Object> entry) { 
                    return (Boolean) entry.getValue(2);
                }
            };

            filters.add(hiddenFilter);
        }

        // note: if none of the above, 'All' is selected and no filtering is needed


        // add the filters to the table
        RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);
        ((TableRowSorter) proteinTable.getRowSorter()).setRowFilter(allFilters);

        boolean updateProteinSelection = false;

        if (proteinTable.getRowCount() > 0) {
            proteinTable.setRowSelectionInterval(0, 0);

            if (previouslySelectedProteinIndex != (Integer) proteinTable.getValueAt(0, 0)) {
                updateProteinSelection = true;
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton allJRadioButton;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton clearJButton;
    private javax.swing.JRadioButton confidenceEqualJRadioButton;
    private javax.swing.JRadioButton confidenceGreaterThanJRadioButton;
    private javax.swing.JTextField confidenceJTextField;
    private javax.swing.JRadioButton confidenceLessThanJRadioButton;
    private javax.swing.ButtonGroup coverageButtonGroup;
    private javax.swing.JRadioButton coverageEqualJRadioButton;
    private javax.swing.JRadioButton coverageGreaterThanJRadioButton;
    private javax.swing.JTextField coverageJTextField;
    private javax.swing.JRadioButton coverageLessThanJRadioButton;
    private javax.swing.JTextField descriptionJTextField;
    private javax.swing.ButtonGroup expCountButtonGroup;
    private javax.swing.ButtonGroup foldChangeButtonGroup;
    private javax.swing.JRadioButton isoformsJRadioButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JButton okJButton;
    private javax.swing.JPanel optionsPanel;
    private javax.swing.ButtonGroup pValueButtonGroup;
    private javax.swing.JRadioButton peptideEqualJRadioButton;
    private javax.swing.JRadioButton peptideGreaterThanJRadioButton;
    private javax.swing.JTextField peptideJTextField;
    private javax.swing.JRadioButton peptideLessThanJRadioButton;
    private javax.swing.ButtonGroup peptidesButtonGroup;
    private javax.swing.ButtonGroup piButtonGroup;
    private javax.swing.JTextField proteinAccessionJTextField;
    private javax.swing.JButton proteinFilteringHelpJButton;
    private javax.swing.ButtonGroup qValueButtonGroup;
    private javax.swing.JRadioButton scoreEqualJRadioButton;
    private javax.swing.JRadioButton scoreGreaterThanJRadioButton;
    private javax.swing.JTextField scoreJTextField;
    private javax.swing.JRadioButton scoreLessThanJRadioButton;
    private javax.swing.JCheckBox showHiddenProteinsCheckBox;
    private javax.swing.JRadioButton singleProteinJRadioButton;
    private javax.swing.JRadioButton spectraEqualJRadioButton;
    private javax.swing.JRadioButton spectraGreaterThanJRadioButton;
    private javax.swing.JTextField spectraJTextField;
    private javax.swing.JRadioButton spectraLessThanJRadioButton;
    private javax.swing.JRadioButton spectrumCountingEqualJRadioButton;
    private javax.swing.JRadioButton spectrumCountingGreaterThanJRadioButton;
    private javax.swing.JTextField spectrumCountingJTextField;
    private javax.swing.JRadioButton spectrumCountingLessThanJRadioButton;
    private javax.swing.JRadioButton unrelatedIsoformsJRadioButton;
    private javax.swing.JRadioButton unrelatedProteinsJRadioButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Set up the focus traversal.
     */
    private void setUpFocusTraversal () {
        
        HashMap<Component, Component> focusMap = new HashMap<Component, Component>();
        focusMap.put(proteinAccessionJTextField, descriptionJTextField);
        focusMap.put(descriptionJTextField, coverageJTextField);
        focusMap.put(coverageJTextField, spectrumCountingJTextField);
        focusMap.put(spectrumCountingJTextField, peptideJTextField);
        focusMap.put(peptideJTextField, spectraJTextField);
        focusMap.put(spectraJTextField, scoreJTextField);
        focusMap.put(scoreJTextField, confidenceJTextField);
        focusMap.put(confidenceJTextField, singleProteinJRadioButton);
        focusMap.put(singleProteinJRadioButton, unrelatedIsoformsJRadioButton);
        focusMap.put(unrelatedIsoformsJRadioButton, isoformsJRadioButton);
        focusMap.put(isoformsJRadioButton, unrelatedProteinsJRadioButton);
        focusMap.put(unrelatedProteinsJRadioButton, allJRadioButton);
        focusMap.put(allJRadioButton, showHiddenProteinsCheckBox);
        focusMap.put(showHiddenProteinsCheckBox, okJButton);
        focusMap.put(okJButton, clearJButton);
        focusMap.put(clearJButton, proteinAccessionJTextField);
        
        HashMap<Component, Component> focusReverseMap = new HashMap<Component, Component>();
        focusReverseMap.put(proteinAccessionJTextField, clearJButton);
        focusReverseMap.put(descriptionJTextField, proteinAccessionJTextField);
        focusReverseMap.put(coverageJTextField, descriptionJTextField);
        focusReverseMap.put(spectrumCountingJTextField, coverageJTextField);
        focusReverseMap.put(peptideJTextField, spectrumCountingJTextField);
        focusReverseMap.put(spectraJTextField, peptideJTextField);
        focusReverseMap.put(scoreJTextField, spectraJTextField);
        focusReverseMap.put(confidenceJTextField, scoreJTextField);
        focusReverseMap.put(singleProteinJRadioButton, confidenceJTextField);
        focusReverseMap.put(unrelatedIsoformsJRadioButton, singleProteinJRadioButton);
        focusReverseMap.put(isoformsJRadioButton, unrelatedIsoformsJRadioButton);
        focusReverseMap.put(unrelatedProteinsJRadioButton, isoformsJRadioButton);
        focusReverseMap.put(allJRadioButton, unrelatedProteinsJRadioButton);
        focusReverseMap.put(showHiddenProteinsCheckBox, allJRadioButton);
        focusReverseMap.put(okJButton, showHiddenProteinsCheckBox);
        focusReverseMap.put(clearJButton, okJButton);
        
        MyFocusPolicy focusPolicy = new MyFocusPolicy(focusMap, focusReverseMap, proteinAccessionJTextField, clearJButton);
        this.setFocusTraversalPolicy(focusPolicy);
    }
    
    /**
     * The focus traversal policy map.
     */
    class MyFocusPolicy extends FocusTraversalPolicy {

        private HashMap<Component, Component> focusMap;
        private HashMap<Component, Component> focusReverseMap;
        private Component first;
        private Component last;
        
        public MyFocusPolicy (HashMap<Component, Component> focusMap, HashMap<Component, Component> focusReverseMap, Component first, Component last) {
            this.focusMap = focusMap;
            this.focusReverseMap = focusReverseMap;
            this.first = first;
            this.last = last;
        }
        
        @Override
        public Component getComponentAfter(Container aContainer, Component aComponent) {
            return focusMap.get(aComponent);  
        } 

        @Override
        public Component getComponentBefore(Container aContainer, Component aComponent) {
            return focusReverseMap.get(aComponent);  
        }

        @Override
        public Component getFirstComponent(Container aContainer) {
            return first;
        }

        @Override
        public Component getLastComponent(Container aContainer) {
            return last;
        }

        @Override
        public Component getDefaultComponent(Container aContainer) {
            return first;
        }
    }
}
