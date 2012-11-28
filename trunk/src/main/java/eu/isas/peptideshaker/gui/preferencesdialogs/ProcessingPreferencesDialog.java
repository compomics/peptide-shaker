package eu.isas.peptideshaker.gui.preferencesdialogs;

import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.preferences.PTMScoringPreferences;
import eu.isas.peptideshaker.preferences.ProcessingPreferences;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * A simple dialog where the user can view/edit the processing preferences.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProcessingPreferencesDialog extends javax.swing.JDialog {

    /**
     * The processing preferences.
     */
    private ProcessingPreferences processingPreferences;
    /**
     * The PTM preferences. This deserves to be modifiable after import of the
     * files as well.
     */
    private PTMScoringPreferences ptmScoringPreferences;

    /**
     * Creates a new parameters dialog.
     *
     * @param parent the parent frame
     * @param editable a boolean indicating whether the processing parameters
     * are editable
     * @param processingPreferences the processing preferences
     * @param ptmScoringPreferences the ptm scoring preferences
     */
    public ProcessingPreferencesDialog(java.awt.Frame parent, boolean editable,
            ProcessingPreferences processingPreferences, PTMScoringPreferences ptmScoringPreferences) {
        super(parent, true);
        initComponents();

        proteinFdrTxt.setText(processingPreferences.getProteinFDR() + "");
        peptideFdrTxt.setText(processingPreferences.getPeptideFDR() + "");
        psmFdrTxt.setText(processingPreferences.getPsmFDR() + "");

        if (ptmScoringPreferences.aScoreCalculation()) {
            ascoreCmb.setSelectedIndex(0);
        } else {
            ascoreCmb.setSelectedIndex(1);
        }
        if (ptmScoringPreferences.isaScoreNeutralLosses()) {
            neutralLossesCmb.setSelectedIndex(0);
        } else {
            neutralLossesCmb.setSelectedIndex(1);
        }
        flrTxt.setText(ptmScoringPreferences.getFlrThreshold() + "");
        
        proteinConfidenceMwTxt.setText(processingPreferences.getProteinConfidenceMwPlots() + "");

        proteinFdrTxt.setEditable(editable);
        peptideFdrTxt.setEditable(editable);
        psmFdrTxt.setEditable(editable);
        proteinFdrTxt.setEnabled(editable);
        peptideFdrTxt.setEnabled(editable);
        psmFdrTxt.setEnabled(editable);
        ascoreCmb.setEnabled(editable);
        neutralLossesCmb.setEnabled(editable);
        flrTxt.setEnabled(editable);
        proteinConfidenceMwTxt.setEnabled(editable);

        ascoreCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        neutralLossesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        this.processingPreferences = processingPreferences;
        this.ptmScoringPreferences = ptmScoringPreferences;

        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Indicates whether the input is correct.
     *
     * @return a boolean indicating whether the input is correct
     */
    private boolean validateInput() {
        try {
            Double temp = new Double(proteinFdrTxt.getText().trim());
            if (temp < 0 || temp > 100) {
                JOptionPane.showMessageDialog(this, "Please verify the input for the protein FDR.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                proteinFdrTxt.requestFocus();
                return false;
            } 
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the input for the protein FDR.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            proteinFdrTxt.requestFocus();
            return false;
        }
        try {
            Double temp = new Double(peptideFdrTxt.getText().trim());
            if (temp < 0 || temp > 100) {
                JOptionPane.showMessageDialog(this, "Please verify the input for the peptide FDR.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                peptideFdrTxt.requestFocus();
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the input for the peptide FDR.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            peptideFdrTxt.requestFocus();
            return false;
        }
        try {
            Double temp = new Double(psmFdrTxt.getText().trim());
            if (temp < 0 || temp > 100) {
                JOptionPane.showMessageDialog(this, "Please verify the input for the PSM FDR.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                psmFdrTxt.requestFocus();
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the input for the PSM FDR.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            psmFdrTxt.requestFocus();
            return false;
        }
        try {
            new Double(flrTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the input for the A-score threshold.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            flrTxt.requestFocus();
            return false;
        }
        try {
            Double temp = new Double(proteinConfidenceMwTxt.getText().trim());
            if (temp < 0 || temp > 100) {
                JOptionPane.showMessageDialog(this, "Please verify the input for the Protein Confidence MW.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
                proteinConfidenceMwTxt.requestFocus();
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the input for the Protein Confidence MW.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            proteinConfidenceMwTxt.requestFocus();
            return false;
        }
        
        if (ascoreCmb.getSelectedIndex() == 1) {
            JOptionPane.showMessageDialog(this, "Disabling the A-score dramatically reduces the identification performance of PeptideShaker.",
                    "Warining", JOptionPane.WARNING_MESSAGE);
        }
        if (neutralLossesCmb.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "In our experience the A-score performs very poorely when accounting for neutral losses.",
                    "Warining", JOptionPane.WARNING_MESSAGE);
        }
        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        processingParamsPanel = new javax.swing.JPanel();
        proteinFdrLabel = new javax.swing.JLabel();
        peptideFdrLabel = new javax.swing.JLabel();
        psmFdrLabel = new javax.swing.JLabel();
        psmFdrTxt = new javax.swing.JTextField();
        percentLabel = new javax.swing.JLabel();
        peptideFdrTxt = new javax.swing.JTextField();
        percentLabel2 = new javax.swing.JLabel();
        proteinFdrTxt = new javax.swing.JTextField();
        percentLabel3 = new javax.swing.JLabel();
        ptmScoringPanel = new javax.swing.JPanel();
        neutralLossesCmb = new javax.swing.JComboBox();
        flrTxt = new javax.swing.JTextField();
        aScoreLabel = new javax.swing.JLabel();
        ascoreCmb = new javax.swing.JComboBox();
        neutralLossesLabel = new javax.swing.JLabel();
        estimateAScoreLabel = new javax.swing.JLabel();
        percentLabel1 = new javax.swing.JLabel();
        fractionsPanel = new javax.swing.JPanel();
        proteinMwLabel = new javax.swing.JLabel();
        proteinConfidenceMwTxt = new javax.swing.JTextField();
        percentLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Processing");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        processingParamsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Processing Parameters"));
        processingParamsPanel.setOpaque(false);

        proteinFdrLabel.setText("Protein FDR");

        peptideFdrLabel.setText("Peptide FDR");

        psmFdrLabel.setText("PSM FDR");

        psmFdrTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        psmFdrTxt.setText("1");

        percentLabel.setText("%");

        peptideFdrTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideFdrTxt.setText("1");

        percentLabel2.setText("%");

        proteinFdrTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinFdrTxt.setText("1");

        percentLabel3.setText("%");

        javax.swing.GroupLayout processingParamsPanelLayout = new javax.swing.GroupLayout(processingParamsPanel);
        processingParamsPanel.setLayout(processingParamsPanelLayout);
        processingParamsPanelLayout.setHorizontalGroup(
            processingParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processingParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinFdrLabel)
                    .addComponent(peptideFdrLabel)
                    .addComponent(psmFdrLabel))
                .addGap(32, 87, Short.MAX_VALUE)
                .addGroup(processingParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, processingParamsPanelLayout.createSequentialGroup()
                        .addComponent(proteinFdrTxt)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(percentLabel3))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, processingParamsPanelLayout.createSequentialGroup()
                        .addComponent(peptideFdrTxt)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(percentLabel2))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, processingParamsPanelLayout.createSequentialGroup()
                        .addComponent(psmFdrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(percentLabel)))
                .addContainerGap())
        );
        processingParamsPanelLayout.setVerticalGroup(
            processingParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processingParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processingParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proteinFdrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(percentLabel3)
                    .addComponent(proteinFdrLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(processingParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(peptideFdrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(percentLabel2)
                    .addComponent(peptideFdrLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(processingParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(psmFdrLabel)
                    .addComponent(psmFdrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(percentLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        ptmScoringPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PTM Scoring"));
        ptmScoringPanel.setOpaque(false);

        neutralLossesCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No" }));

        flrTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        flrTxt.setText("1");
        flrTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                flrTxtActionPerformed(evt);
            }
        });

        aScoreLabel.setText("False Localization Rate");

        ascoreCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No" }));

        neutralLossesLabel.setText("Account Neutral Losses");

        estimateAScoreLabel.setText("Estimate A-score");

        percentLabel1.setText("%");

        javax.swing.GroupLayout ptmScoringPanelLayout = new javax.swing.GroupLayout(ptmScoringPanel);
        ptmScoringPanel.setLayout(ptmScoringPanelLayout);
        ptmScoringPanelLayout.setHorizontalGroup(
            ptmScoringPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ptmScoringPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ptmScoringPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ptmScoringPanelLayout.createSequentialGroup()
                        .addComponent(estimateAScoreLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(ptmScoringPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(ascoreCmb, javax.swing.GroupLayout.Alignment.TRAILING, 0, 185, Short.MAX_VALUE)
                            .addComponent(neutralLossesCmb, javax.swing.GroupLayout.Alignment.TRAILING, 0, 185, Short.MAX_VALUE)
                            .addComponent(flrTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)))
                    .addGroup(ptmScoringPanelLayout.createSequentialGroup()
                        .addGroup(ptmScoringPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(neutralLossesLabel)
                            .addComponent(aScoreLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(percentLabel1)
                .addContainerGap())
        );
        ptmScoringPanelLayout.setVerticalGroup(
            ptmScoringPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ptmScoringPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ptmScoringPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(estimateAScoreLabel)
                    .addComponent(ascoreCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(ptmScoringPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(neutralLossesLabel)
                    .addComponent(neutralLossesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(ptmScoringPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aScoreLabel)
                    .addComponent(flrTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(percentLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fractionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Fractions"));
        fractionsPanel.setOpaque(false);

        proteinMwLabel.setText("Protein Confidence MW");
        proteinMwLabel.setToolTipText("<html>\nThe minium protein confidence required to be included in the<br>\naverage molecular weight analysis in the Fractions tab.\n</html>");

        proteinConfidenceMwTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinConfidenceMwTxt.setText("95");
        proteinConfidenceMwTxt.setToolTipText("<html>\nThe minium protein confidence required to be included in the<br>\naverage molecular weight analysis in the Fractions tab.\n</html>");

        percentLabel4.setText("%");

        javax.swing.GroupLayout fractionsPanelLayout = new javax.swing.GroupLayout(fractionsPanel);
        fractionsPanel.setLayout(fractionsPanelLayout);
        fractionsPanelLayout.setHorizontalGroup(
            fractionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fractionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinMwLabel)
                .addGap(30, 30, 30)
                .addComponent(proteinConfidenceMwTxt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(percentLabel4)
                .addContainerGap())
        );
        fractionsPanelLayout.setVerticalGroup(
            fractionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fractionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fractionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proteinConfidenceMwTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(percentLabel4)
                    .addComponent(proteinMwLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton))
                    .addComponent(ptmScoringPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(fractionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(processingParamsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(processingParamsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ptmScoringPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fractionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
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
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the preferences and close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (validateInput()) {
            processingPreferences.setProteinFDR(new Double(proteinFdrTxt.getText().trim()));
            processingPreferences.setPeptideFDR(new Double(peptideFdrTxt.getText().trim()));
            processingPreferences.setPsmFDR(new Double(psmFdrTxt.getText().trim()));
            ptmScoringPreferences.setaScoreCalculation(ascoreCmb.getSelectedIndex() == 0);
            ptmScoringPreferences.setaScoreNeutralLosses(neutralLossesCmb.getSelectedIndex() == 0);
            ptmScoringPreferences.setFlrThreshold(new Double(flrTxt.getText().trim()));
            processingPreferences.setProteinConfidenceMwPlots(new Double(proteinConfidenceMwTxt.getText().trim()));
            dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    private void flrTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_flrTxtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_flrTxtActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel aScoreLabel;
    private javax.swing.JComboBox ascoreCmb;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel estimateAScoreLabel;
    private javax.swing.JTextField flrTxt;
    private javax.swing.JPanel fractionsPanel;
    private javax.swing.JComboBox neutralLossesCmb;
    private javax.swing.JLabel neutralLossesLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel peptideFdrLabel;
    private javax.swing.JTextField peptideFdrTxt;
    private javax.swing.JLabel percentLabel;
    private javax.swing.JLabel percentLabel1;
    private javax.swing.JLabel percentLabel2;
    private javax.swing.JLabel percentLabel3;
    private javax.swing.JLabel percentLabel4;
    private javax.swing.JPanel processingParamsPanel;
    private javax.swing.JTextField proteinConfidenceMwTxt;
    private javax.swing.JLabel proteinFdrLabel;
    private javax.swing.JTextField proteinFdrTxt;
    private javax.swing.JLabel proteinMwLabel;
    private javax.swing.JLabel psmFdrLabel;
    private javax.swing.JTextField psmFdrTxt;
    private javax.swing.JPanel ptmScoringPanel;
    // End of variables declaration//GEN-END:variables
}
