package eu.isas.peptideshaker.gui.preferencesdialogs;

import com.compomics.util.experiment.biology.Ion.IonType;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.ImageIcon;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;

/**
 * A simple dialog for setting the spectrum annotation preferences.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class AnnotationPreferencesDialog extends javax.swing.JDialog {

    /**
     * The annotation preferences.
     */
    private AnnotationPreferences annotationPreferences;
    /**
     * The PeptideShakerGUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * Map of the charges selection.
     */
    private HashMap<Integer, Boolean> chargesMap = new HashMap<Integer, Boolean>();
    /**
     * Map of the neutral losses selection.
     */
    private HashMap<NeutralLoss, Boolean> neutralLossesMap = new HashMap<NeutralLoss, Boolean>();

    /**
     * Creates a new AnnotationPreferencesDialog.
     *
     * @param peptideShakerGUI the PeptideShaker GUI parent
     */
    public AnnotationPreferencesDialog(PeptideShakerGUI peptideShakerGUI) {
        super(peptideShakerGUI, true);
        this.peptideShakerGUI = peptideShakerGUI;
        this.annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        setUpData();
        initComponents();
        setUpGui();
        updateGUI();
        this.setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        // set main table properties
        chargesTable.getTableHeader().setReorderingAllowed(false);
        neutralLossesTable.getTableHeader().setReorderingAllowed(false);

        // make sure that the scroll panes are see-through
        chargeScrollPane.getViewport().setOpaque(false);
        neutralLossScrollPane.getViewport().setOpaque(false);

        chargesTable.getColumn(" ").setMaxWidth(50);
        chargesTable.getColumn(" ").setMinWidth(50);
        chargesTable.getColumn("  ").setMaxWidth(30);
        chargesTable.getColumn("  ").setMinWidth(30);

        neutralLossesTable.getColumn(" ").setMaxWidth(50);
        neutralLossesTable.getColumn(" ").setMinWidth(50);
        neutralLossesTable.getColumn("  ").setMaxWidth(30);
        neutralLossesTable.getColumn("  ").setMinWidth(30);

        chargesTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());
        neutralLossesTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());

        chargesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
                null,
                "Selected", null));
        neutralLossesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
                null,
                "Selected", null));
    }

    /**
     * Set up the required data.
     */
    private void setUpData() {
        setUpCharges();
        setUpNeutralLosses();
    }

    /**
     * Set up the charges.
     */
    public void setUpCharges() {
        ArrayList<Integer> charges = peptideShakerGUI.getCharges();

        int maxCharge = 1;

        if (!charges.isEmpty()) {
            maxCharge = Collections.max(charges);
        }

        ArrayList<Integer> selectedCharges = annotationPreferences.getValidatedCharges();

        for (int charge = 1; charge <= maxCharge; charge++) {
            chargesMap.put(charge, selectedCharges.contains(charge));
        }
    }

    /**
     * Set up the neutral losses
     */
    public void setUpNeutralLosses() {
        ArrayList<NeutralLoss> possibleNeutralLosses = peptideShakerGUI.getNeutralLosses();
        ArrayList<NeutralLoss> selectedNeutralLosses = annotationPreferences.getNeutralLosses().getAccountedNeutralLosses();

        for (NeutralLoss possibleNeutralLoss : possibleNeutralLosses) {

            boolean found = false;

            for (NeutralLoss selectedNeutralLoss : selectedNeutralLosses) {
                if (possibleNeutralLoss.isSameAs(selectedNeutralLoss)) {
                    found = true;
                    break;
                }
            }

            neutralLossesMap.put(possibleNeutralLoss, found);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        backgroundPanel = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        annotationPreferencesHelpJButton = new javax.swing.JButton();
        ionsPanel = new javax.swing.JPanel();
        aBox = new javax.swing.JCheckBox();
        bBox = new javax.swing.JCheckBox();
        cBox = new javax.swing.JCheckBox();
        xBox = new javax.swing.JCheckBox();
        yBox = new javax.swing.JCheckBox();
        zBox = new javax.swing.JCheckBox();
        precursorBox = new javax.swing.JCheckBox();
        immoniumBox = new javax.swing.JCheckBox();
        reporterBox = new javax.swing.JCheckBox();
        chargePanel = new javax.swing.JPanel();
        chargeScrollPane = new javax.swing.JScrollPane();
        chargesTable = new javax.swing.JTable();
        neutralLossPanel = new javax.swing.JPanel();
        neutralLossScrollPane = new javax.swing.JScrollPane();
        neutralLossesTable = new javax.swing.JTable();
        peakMatchingPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        intensitySpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        accuracySpinner = new javax.swing.JSpinner();
        adaptNeutralLossesBox = new javax.swing.JCheckBox();
        automaticAnnotationCheck = new javax.swing.JCheckBox();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Spectrum Annotation");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        annotationPreferencesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        annotationPreferencesHelpJButton.setToolTipText("Help");
        annotationPreferencesHelpJButton.setBorder(null);
        annotationPreferencesHelpJButton.setBorderPainted(false);
        annotationPreferencesHelpJButton.setContentAreaFilled(false);
        annotationPreferencesHelpJButton.setFocusable(false);
        annotationPreferencesHelpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        annotationPreferencesHelpJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        annotationPreferencesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                annotationPreferencesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                annotationPreferencesHelpJButtonMouseExited(evt);
            }
        });
        annotationPreferencesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                annotationPreferencesHelpJButtonActionPerformed(evt);
            }
        });

        ionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Ion Type"));
        ionsPanel.setOpaque(false);

        aBox.setText("a-ion");
        aBox.setIconTextGap(10);
        aBox.setOpaque(false);

        bBox.setText("b-ion");
        bBox.setIconTextGap(10);
        bBox.setOpaque(false);

        cBox.setText("c-ion");
        cBox.setIconTextGap(10);
        cBox.setOpaque(false);

        xBox.setText("x-ion");
        xBox.setIconTextGap(10);
        xBox.setOpaque(false);

        yBox.setText("y-ion");
        yBox.setIconTextGap(10);
        yBox.setOpaque(false);

        zBox.setText("z-ion");
        zBox.setIconTextGap(10);
        zBox.setOpaque(false);

        precursorBox.setText("Precursor");
        precursorBox.setToolTipText("Precursor ions");
        precursorBox.setIconTextGap(10);
        precursorBox.setOpaque(false);

        immoniumBox.setText("Immonium");
        immoniumBox.setToolTipText("Immonium ions");
        immoniumBox.setIconTextGap(10);
        immoniumBox.setOpaque(false);

        reporterBox.setText("Reporter");
        reporterBox.setToolTipText("Report ions");
        reporterBox.setIconTextGap(10);
        reporterBox.setOpaque(false);

        javax.swing.GroupLayout ionsPanelLayout = new javax.swing.GroupLayout(ionsPanel);
        ionsPanel.setLayout(ionsPanelLayout);
        ionsPanelLayout.setHorizontalGroup(
            ionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ionsPanelLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(ionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(aBox)
                    .addComponent(bBox)
                    .addComponent(cBox))
                .addGap(50, 50, 50)
                .addGroup(ionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(yBox)
                    .addComponent(xBox)
                    .addComponent(zBox))
                .addGap(50, 50, 50)
                .addGroup(ionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(reporterBox)
                    .addComponent(immoniumBox)
                    .addComponent(precursorBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        ionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {aBox, bBox, cBox, immoniumBox, precursorBox, xBox, yBox, zBox});

        ionsPanelLayout.setVerticalGroup(
            ionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aBox)
                    .addComponent(xBox)
                    .addComponent(precursorBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bBox)
                    .addComponent(yBox, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(immoniumBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cBox)
                    .addComponent(zBox)
                    .addComponent(reporterBox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        chargePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Charge"));
        chargePanel.setOpaque(false);

        chargesTable.setModel(new ChargesTableModel());
        chargesTable.setOpaque(false);
        chargeScrollPane.setViewportView(chargesTable);

        javax.swing.GroupLayout chargePanelLayout = new javax.swing.GroupLayout(chargePanel);
        chargePanel.setLayout(chargePanelLayout);
        chargePanelLayout.setHorizontalGroup(
            chargePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chargePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chargeScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        chargePanelLayout.setVerticalGroup(
            chargePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chargePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chargeScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                .addContainerGap())
        );

        neutralLossPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Neutral Loss"));
        neutralLossPanel.setOpaque(false);

        neutralLossesTable.setModel(new NeutralLossesTableModel());
        neutralLossesTable.setOpaque(false);
        neutralLossScrollPane.setViewportView(neutralLossesTable);

        javax.swing.GroupLayout neutralLossPanelLayout = new javax.swing.GroupLayout(neutralLossPanel);
        neutralLossPanel.setLayout(neutralLossPanelLayout);
        neutralLossPanelLayout.setHorizontalGroup(
            neutralLossPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(neutralLossPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(neutralLossScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        neutralLossPanelLayout.setVerticalGroup(
            neutralLossPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(neutralLossPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(neutralLossScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE)
                .addContainerGap())
        );

        peakMatchingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peak Matching"));
        peakMatchingPanel.setOpaque(false);

        jLabel2.setText("Fragment Ion Accuracy:");
        jLabel2.setToolTipText("Fragment ion annotation accuracy .");

        jLabel3.setText("Da");

        intensitySpinner.setModel(new javax.swing.SpinnerNumberModel(25, 0, 100, 1));
        intensitySpinner.setToolTipText("<html>\nDisplay a certain percent of the<br>\npossible annotations relative<br>\nto the most intense peak.\n</html>");

        jLabel1.setText("%");

        jLabel4.setText("Annotation Level:");
        jLabel4.setToolTipText("<html>\nDisplay a certain percent of the<br>\npossible annotations relative<br>\nto the most intense peak.\n</html>");

        accuracySpinner.setModel(new javax.swing.SpinnerNumberModel(0.05d, 0.0d, 0.05d, 0.0010d));
        accuracySpinner.setToolTipText("Fragment ion annotation accuracy.");

        adaptNeutralLossesBox.setSelected(true);
        adaptNeutralLossesBox.setText("Adapt Neutral Losses");
        adaptNeutralLossesBox.setIconTextGap(10);
        adaptNeutralLossesBox.setOpaque(false);
        adaptNeutralLossesBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adaptNeutralLossesBoxActionPerformed(evt);
            }
        });

        automaticAnnotationCheck.setSelected(true);
        automaticAnnotationCheck.setText("Automatic Annotation");
        automaticAnnotationCheck.setIconTextGap(10);
        automaticAnnotationCheck.setOpaque(false);
        automaticAnnotationCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                automaticAnnotationCheckActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout peakMatchingPanelLayout = new javax.swing.GroupLayout(peakMatchingPanel);
        peakMatchingPanel.setLayout(peakMatchingPanelLayout);
        peakMatchingPanelLayout.setHorizontalGroup(
            peakMatchingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peakMatchingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peakMatchingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addGroup(peakMatchingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(intensitySpinner)
                    .addComponent(accuracySpinner, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(peakMatchingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 31, Short.MAX_VALUE)
                .addGroup(peakMatchingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(adaptNeutralLossesBox)
                    .addComponent(automaticAnnotationCheck))
                .addGap(31, 31, 31))
        );

        peakMatchingPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel3});

        peakMatchingPanelLayout.setVerticalGroup(
            peakMatchingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peakMatchingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peakMatchingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel4)
                    .addComponent(intensitySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(adaptNeutralLossesBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(peakMatchingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel2)
                    .addComponent(accuracySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(automaticAnnotationCheck))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(annotationPreferencesHelpJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(peakMatchingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(chargePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(neutralLossPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chargePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(neutralLossPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peakMatchingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(okButton)
                    .addComponent(cancelButton)
                    .addComponent(annotationPreferencesHelpJButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {chargePanel, neutralLossPanel});

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
     * Close the dialog and update the spectrum annotations.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        annotationPreferences.clearIonTypes();
        if (aBox.isSelected()) {
            annotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.A_ION);
        }
        if (bBox.isSelected()) {
            annotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
        }
        if (cBox.isSelected()) {
            annotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.C_ION);
        }
        if (xBox.isSelected()) {
            annotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.X_ION);
        }
        if (yBox.isSelected()) {
            annotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
        }
        if (zBox.isSelected()) {
            annotationPreferences.addIonType(IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Z_ION);
        }
        if (precursorBox.isSelected()) {
            annotationPreferences.addIonType(IonType.PRECURSOR_ION);
        }
        if (immoniumBox.isSelected()) {
            annotationPreferences.addIonType(IonType.IMMONIUM_ION);
        }
        if (reporterBox.isSelected()) {
            for (int subtype : peptideShakerGUI.getReporterIons()) {
                annotationPreferences.addIonType(IonType.REPORTER_ION, subtype);
            }
        }

        annotationPreferences.setAnnotationLevel(((Integer) intensitySpinner.getValue()) / 100.0);
        annotationPreferences.setFragmentIonAccuracy((Double) accuracySpinner.getValue());

        annotationPreferences.clearNeutralLosses();

        for (NeutralLoss neutralLoss : neutralLossesMap.keySet()) {
            if (neutralLossesMap.get(neutralLoss)) {
                annotationPreferences.addNeutralLoss(neutralLoss);
            }
        }

        annotationPreferences.useAutomaticAnnotation(automaticAnnotationCheck.isSelected());
        annotationPreferences.setNeutralLossesSequenceDependant(adaptNeutralLossesBox.isSelected());

        annotationPreferences.clearCharges();

        for (int charge : chargesMap.keySet()) {
            if (chargesMap.get(charge)) {
                annotationPreferences.addSelectedCharge(charge);
            }
        }

        peptideShakerGUI.setAnnotationPreferences(annotationPreferences);
        peptideShakerGUI.updateSpectrumAnnotations();
        peptideShakerGUI.setDataSaved(false);
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Closes the dialog without saving.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void annotationPreferencesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_annotationPreferencesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
}//GEN-LAST:event_annotationPreferencesHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void annotationPreferencesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_annotationPreferencesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_annotationPreferencesHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void annotationPreferencesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_annotationPreferencesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/AnnotationPreferences.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_annotationPreferencesHelpJButtonActionPerformed

    /**
     * Reset the automatic annotation in the tables.
     *
     * @param evt
     */
    private void automaticAnnotationCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_automaticAnnotationCheckActionPerformed
        if (automaticAnnotationCheck.isSelected()) {
            adaptNeutralLossesBox.setSelected(true);
            setUpData();
            ((DefaultTableModel) chargesTable.getModel()).fireTableDataChanged();
            ((DefaultTableModel) neutralLossesTable.getModel()).fireTableDataChanged();
        }
    }//GEN-LAST:event_automaticAnnotationCheckActionPerformed

    /**
     * Reset the neutral losses to adapt to the sequence if selected.
     *
     * @param evt
     */
    private void adaptNeutralLossesBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adaptNeutralLossesBoxActionPerformed
        if (adaptNeutralLossesBox.isSelected()) {
            setUpNeutralLosses();
            ((DefaultTableModel) neutralLossesTable.getModel()).fireTableDataChanged();
        }
    }//GEN-LAST:event_adaptNeutralLossesBoxActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox aBox;
    private javax.swing.JSpinner accuracySpinner;
    private javax.swing.JCheckBox adaptNeutralLossesBox;
    private javax.swing.JButton annotationPreferencesHelpJButton;
    private javax.swing.JCheckBox automaticAnnotationCheck;
    private javax.swing.JCheckBox bBox;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox cBox;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel chargePanel;
    private javax.swing.JScrollPane chargeScrollPane;
    private javax.swing.JTable chargesTable;
    private javax.swing.JCheckBox immoniumBox;
    private javax.swing.JSpinner intensitySpinner;
    private javax.swing.JPanel ionsPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel neutralLossPanel;
    private javax.swing.JScrollPane neutralLossScrollPane;
    private javax.swing.JTable neutralLossesTable;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel peakMatchingPanel;
    private javax.swing.JCheckBox precursorBox;
    private javax.swing.JCheckBox reporterBox;
    private javax.swing.JCheckBox xBox;
    private javax.swing.JCheckBox yBox;
    private javax.swing.JCheckBox zBox;
    // End of variables declaration//GEN-END:variables

    /**
     * Refresh the selection.
     */
    private void updateGUI() {
        intensitySpinner.setValue((int) (annotationPreferences.getAnnotationIntensityLimit() * 100));
        ((SpinnerNumberModel) accuracySpinner.getModel()).setMaximum(peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy());
        accuracySpinner.setValue(new Double(annotationPreferences.getFragmentIonAccuracy()));
        aBox.setSelected(false);
        bBox.setSelected(false);
        cBox.setSelected(false);
        xBox.setSelected(false);
        yBox.setSelected(false);
        zBox.setSelected(false);
        precursorBox.setSelected(false);
        immoniumBox.setSelected(false);
        reporterBox.setSelected(false);
        for (IonType ionType : annotationPreferences.getIonTypes().keySet()) {
            if (ionType == IonType.IMMONIUM_ION) {
                immoniumBox.setSelected(true);
            } else if (ionType == IonType.PEPTIDE_FRAGMENT_ION) {
                for (int subType : annotationPreferences.getIonTypes().get(ionType)) {
                    if (subType == PeptideFragmentIon.A_ION) {
                        aBox.setSelected(true);
                    } else if (subType == PeptideFragmentIon.B_ION) {
                        bBox.setSelected(true);
                    } else if (subType == PeptideFragmentIon.C_ION) {
                        cBox.setSelected(true);
                    } else if (subType == PeptideFragmentIon.X_ION) {
                        xBox.setSelected(true);
                    } else if (subType == PeptideFragmentIon.Y_ION) {
                        yBox.setSelected(true);
                    } else if (subType == PeptideFragmentIon.Z_ION) {
                        zBox.setSelected(true);
                    }
                }
            } else if (ionType == IonType.PRECURSOR_ION) {
                precursorBox.setSelected(true);
            } else if (ionType == IonType.REPORTER_ION) {
                reporterBox.setSelected(true);
            }
        }

        automaticAnnotationCheck.setSelected(annotationPreferences.useAutomaticAnnotation());
        adaptNeutralLossesBox.setSelected(annotationPreferences.areNeutralLossesSequenceDependant());
    }

    /**
     * Table model for the charges table.
     */
    private class ChargesTableModel extends DefaultTableModel {

        private ArrayList<Integer> charges;

        public ChargesTableModel() {
            charges = new ArrayList<Integer>(chargesMap.keySet());
            Collections.sort(charges);
        }

        @Override
        public int getRowCount() {
            return chargesMap.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Charge";
                case 2:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    return charges.get(row) + "+";
                case 2:
                    return chargesMap.get(charges.get(row));
                default:
                    return "";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            for (int i = 0; i < getRowCount(); i++) {
                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            chargesMap.put(charges.get(row), !chargesMap.get(charges.get(row)));
            automaticAnnotationCheck.setSelected(false);
        }
    }

    /**
     * Table model for the neutral losses table.
     */
    private class NeutralLossesTableModel extends DefaultTableModel {

        private HashMap<String, NeutralLoss> namesMap = new HashMap<String, NeutralLoss>();
        private ArrayList<String> namesList = new ArrayList<String>();

        public NeutralLossesTableModel() {
            for (NeutralLoss neutralLoss : neutralLossesMap.keySet()) {
                namesMap.put(neutralLoss.name, neutralLoss);
            }
            namesList = new ArrayList<String>(namesMap.keySet());
            Collections.sort(namesList);
        }

        @Override
        public int getRowCount() {
            if (namesList == null) {
                return 0;
            }
            return namesList.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Neutral Loss";
                case 2:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    return namesList.get(row);
                case 2:
                    return neutralLossesMap.get(namesMap.get(namesList.get(row)));
                default:
                    return "";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            for (int i = 0; i < getRowCount(); i++) {
                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            NeutralLoss neutralLoss = namesMap.get(namesList.get(row));
            neutralLossesMap.put(neutralLoss, !neutralLossesMap.get(neutralLoss));
            adaptNeutralLossesBox.setSelected(false);
            automaticAnnotationCheck.setSelected(false);
        }
    }
}
