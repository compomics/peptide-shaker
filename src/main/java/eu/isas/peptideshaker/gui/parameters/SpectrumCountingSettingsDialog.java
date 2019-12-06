package eu.isas.peptideshaker.gui.parameters;

import com.compomics.util.experiment.units.MetricsPrefix;
import com.compomics.util.experiment.units.StandardUnit;
import com.compomics.util.experiment.units.UnitOfMeasurement;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import com.compomics.util.experiment.quantification.spectrumcounting.SpectrumCountingMethod;
import com.compomics.util.gui.error_handlers.HelpDialog;
import java.awt.Toolkit;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * SpectrumCountingSettingsDialog.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SpectrumCountingSettingsDialog extends javax.swing.JDialog {

    /**
     * Boolean indicating whether the user canceled the editing.
     */
    private boolean canceled = false;
    /**
     * The parent frame.
     */
    private java.awt.Frame parentFrame;

    /**
     * Constructor.
     *
     * @param parentFrame a parent frame
     * @param spectrumCountingPreferences the spectrum counting preferences to
     * display
     */
    public SpectrumCountingSettingsDialog(java.awt.Frame parentFrame, SpectrumCountingParameters spectrumCountingPreferences) {
        super(parentFrame, true);
        this.parentFrame = parentFrame;
        initComponents();
        setUpGui();
        populateGUI(spectrumCountingPreferences);
        setLocationRelativeTo(parentFrame);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {
        methodCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        validationLevelCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        normalizationCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        unitCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
    }

    /**
     * Fills the GUI with the given settings.
     *
     * @param spectrumCountingPreferences the spectrum counting preferences to
     * display
     */
    private void populateGUI(SpectrumCountingParameters spectrumCountingPreferences) {

        // The spectrum couting method
        if (spectrumCountingPreferences.getSelectedMethod() == SpectrumCountingMethod.NSAF) {
            methodCmb.setSelectedIndex(0);
            validationLevelLbl.setText("Spectra Considered:");
        } else {
            methodCmb.setSelectedIndex(1);
            validationLevelLbl.setText("Peptides Considered:");
        }
        validationLevelCmb.setSelectedIndex(spectrumCountingPreferences.getMatchValidationLevel());
        validationLevelCmb.setEnabled(false); //@TODO: enable when supported

        // The Normalization
        if (!spectrumCountingPreferences.getNormalize() || spectrumCountingPreferences.getUnit() == null) {
            normalizationCmb.setSelectedIndex(0);
            unitCmb.setEnabled(false);
            referenceTxt.setText("");
            referenceTxt.setEnabled(false);
        } else {
            UnitOfMeasurement unit = spectrumCountingPreferences.getUnit();
            String unitFullName = unit.getFullName();
            StandardUnit standardUnit = StandardUnit.getStandardUnit(unitFullName);
            if (standardUnit == null) {
                throw new UnsupportedOperationException("Unit " + unitFullName + " not supported.");
            }
            switch (standardUnit) {
                case mol:
                    normalizationCmb.setSelectedIndex(1);
                    unitCmb.setEnabled(true);
                    UnitOfMeasurement[] units = getUnits(standardUnit);
                    unitCmb.setModel(new DefaultComboBoxModel(units));
                    int selectedIndex = 0;
                    for (int i = 0; i < units.length; i++) {
                        if (units[i].isSameAs(unit)) {
                            selectedIndex = i;
                            break;
                        }
                    }
                    unitCmb.setSelectedIndex(selectedIndex);
                    referenceTxt.setEnabled(true);
                    referenceTxt.setText(spectrumCountingPreferences.getReferenceMass() + "");
                    break;
                case percentage:
                case ppm:
                    normalizationCmb.setSelectedIndex(2);
                    unitCmb.setEnabled(true);
                    units = getRelativeUnits();
                    unitCmb.setModel(new DefaultComboBoxModel(units));
                    selectedIndex = 0;
                    for (int i = 0; i < units.length; i++) {
                        if (units[i].isSameAs(unit)) {
                            selectedIndex = i;
                            break;
                        }
                    }
                    unitCmb.setSelectedIndex(selectedIndex);
                    referenceTxt.setText("");
                    referenceTxt.setEnabled(false);
                    break;
                default:
                    throw new UnsupportedOperationException("Unit " + unitFullName + " not supported.");
            }
        }

    }

    /**
     * Returns the supported relative units as an array.
     *
     * @param standardUnit the standard unit
     *
     * @return returns the possible units for a standard unit
     */
    private UnitOfMeasurement[] getRelativeUnits() {
        UnitOfMeasurement[] units = {new UnitOfMeasurement(StandardUnit.percentage), new UnitOfMeasurement(StandardUnit.ppm)};
        return units;
    }

    /**
     * Returns the supported units with prefix for the given standard unit as an
     * array.
     *
     * @param standardUnit the standard unit
     *
     * @return returns the possible units for a standard unit
     */
    private UnitOfMeasurement[] getUnits(StandardUnit standardUnit) {
        MetricsPrefix[] metricsPrefixes = MetricsPrefix.values();
        UnitOfMeasurement[] units = new UnitOfMeasurement[metricsPrefixes.length];
        for (int i = 0; i < metricsPrefixes.length; i++) {
            MetricsPrefix metricsPrefix = metricsPrefixes[i];
            UnitOfMeasurement unit = new UnitOfMeasurement(standardUnit, metricsPrefix);
            units[i] = unit;
        }
        return units;
    }

    /**
     * Indicates whether the user canceled the editing.
     *
     * @return a boolean indicating whether the user canceled the editing
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Validates the user input.
     *
     * @return a boolean indicating whether the user input is valid
     */
    public boolean validateInput() {

        if (normalizationCmb.getSelectedIndex() == 0 && normalizationCmb.getSelectedIndex() == 1) {
            try {
                new Double(referenceTxt.getText());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for the protein amount.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the spectrum couting preferences as set by the user.
     *
     * @return the spectrum couting preferences as set by the user
     */
    public SpectrumCountingParameters getSpectrumCountingPreferences() {

        SpectrumCountingParameters spectrumCountingPreferences = new SpectrumCountingParameters();
        if (methodCmb.getSelectedIndex() == 0) {
            spectrumCountingPreferences.setSelectedMethod(SpectrumCountingMethod.NSAF);
        } else if (methodCmb.getSelectedIndex() == 1) {
            spectrumCountingPreferences.setSelectedMethod(SpectrumCountingMethod.EMPAI);
        } else {
            throw new UnsupportedOperationException("Option " + methodCmb.getSelectedIndex() + "not supported.");
        }

        spectrumCountingPreferences.setMatchValidationLevel(validationLevelCmb.getSelectedIndex());

        if (normalizationCmb.getSelectedIndex() == 0) {
            spectrumCountingPreferences.setNormalize(false);
        } else {
            spectrumCountingPreferences.setNormalize(true);
            spectrumCountingPreferences.setUnit((UnitOfMeasurement) unitCmb.getSelectedItem());
            if (normalizationCmb.getSelectedIndex() == 1) {
                Double value = new Double(referenceTxt.getText());
                spectrumCountingPreferences.setReferenceMass(value);
            }
        }

        return spectrumCountingPreferences;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSeparator1 = new javax.swing.JSeparator();
        backgroundPanel = new javax.swing.JPanel();
        quantificationOptionsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        methodCmb = new javax.swing.JComboBox();
        validationLevelLbl = new javax.swing.JLabel();
        validationLevelCmb = new javax.swing.JComboBox();
        normalizationPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        normalizationCmb = new javax.swing.JComboBox();
        referenceLbl = new javax.swing.JLabel();
        referenceTxt = new javax.swing.JTextField();
        unitLbl = new javax.swing.JLabel();
        unitCmb = new javax.swing.JComboBox();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        helpJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Spectrum Counting Settings");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        quantificationOptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("MS2 Quantification Options"));
        quantificationOptionsPanel.setOpaque(false);

        jLabel1.setText("Quantification Method");

        methodCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "NSAF+", "emPAI" }));
        methodCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                methodCmbActionPerformed(evt);
            }
        });

        validationLevelLbl.setText("Spectra Considered");

        validationLevelCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All", "Validated", "Confident" }));

        javax.swing.GroupLayout quantificationOptionsPanelLayout = new javax.swing.GroupLayout(quantificationOptionsPanel);
        quantificationOptionsPanel.setLayout(quantificationOptionsPanelLayout);
        quantificationOptionsPanelLayout.setHorizontalGroup(
            quantificationOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(quantificationOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(quantificationOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(quantificationOptionsPanelLayout.createSequentialGroup()
                        .addComponent(validationLevelLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(validationLevelCmb, 0, 143, Short.MAX_VALUE))
                    .addGroup(quantificationOptionsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(methodCmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        quantificationOptionsPanelLayout.setVerticalGroup(
            quantificationOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(quantificationOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(quantificationOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(methodCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(quantificationOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(validationLevelLbl)
                    .addComponent(validationLevelCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        normalizationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Normalization"));
        normalizationPanel.setOpaque(false);

        jLabel2.setText("Normalization Method");

        normalizationCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Abundance", "Relative" }));
        normalizationCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                normalizationCmbActionPerformed(evt);
            }
        });

        referenceLbl.setText("Protein Amount [µg]");

        referenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        unitLbl.setText("Unit");

        javax.swing.GroupLayout normalizationPanelLayout = new javax.swing.GroupLayout(normalizationPanel);
        normalizationPanel.setLayout(normalizationPanelLayout);
        normalizationPanelLayout.setHorizontalGroup(
            normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(normalizationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(normalizationPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(normalizationCmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(normalizationPanelLayout.createSequentialGroup()
                        .addComponent(referenceLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(referenceTxt))
                    .addGroup(normalizationPanelLayout.createSequentialGroup()
                        .addComponent(unitLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(unitCmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        normalizationPanelLayout.setVerticalGroup(
            normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(normalizationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(normalizationCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(unitLbl)
                    .addComponent(unitCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(normalizationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(referenceLbl)
                    .addComponent(referenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

        helpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help.GIF"))); // NOI18N
        helpJButton.setToolTipText("Help");
        helpJButton.setBorder(null);
        helpJButton.setBorderPainted(false);
        helpJButton.setContentAreaFilled(false);
        helpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                helpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                helpJButtonMouseExited(evt);
            }
        });
        helpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(normalizationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(quantificationOptionsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(helpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(quantificationOptionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(normalizationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(helpJButton)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap())
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
     * Enable/disable the unit options.
     * 
     * @param evt 
     */
    private void normalizationCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_normalizationCmbActionPerformed
        if (normalizationCmb.getSelectedIndex() == 0) {
            unitCmb.setEnabled(false);
            referenceTxt.setEnabled(false);
        } else if (normalizationCmb.getSelectedIndex() == 1) {
            unitCmb.setModel(new DefaultComboBoxModel(getUnits(StandardUnit.mol)));
            unitCmb.setEnabled(true);
            referenceTxt.setEnabled(true);
        } else if (normalizationCmb.getSelectedIndex() == 2) {
            unitCmb.setModel(new DefaultComboBoxModel(getRelativeUnits()));
            unitCmb.setEnabled(true);
            referenceTxt.setEnabled(false);
        }
    }//GEN-LAST:event_normalizationCmbActionPerformed

    /**
     * Cancel the dialog.
     * 
     * @param evt 
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        canceled = true;
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Change the method.
     * 
     * @param evt 
     */
    private void methodCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_methodCmbActionPerformed
        if (methodCmb.getSelectedIndex() == 1) {
            validationLevelLbl.setText("Peptides Considered");
        } else {
            validationLevelLbl.setText("Spectra Considered");
        }
    }//GEN-LAST:event_methodCmbActionPerformed

    /**
     * Close the dialog.
     * 
     * @param evt 
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (validateInput()) {
            dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Cancel the dialog.
     * 
     * @param evt 
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        canceled = true;
    }//GEN-LAST:event_formWindowClosing

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void helpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_helpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void helpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(parentFrame, getClass().getResource("/helpFiles/SpectrumCounting.html"),
            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
            "Preferences - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpJButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton helpJButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox methodCmb;
    private javax.swing.JComboBox normalizationCmb;
    private javax.swing.JPanel normalizationPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel quantificationOptionsPanel;
    private javax.swing.JLabel referenceLbl;
    private javax.swing.JTextField referenceTxt;
    private javax.swing.JComboBox unitCmb;
    private javax.swing.JLabel unitLbl;
    private javax.swing.JComboBox validationLevelCmb;
    private javax.swing.JLabel validationLevelLbl;
    // End of variables declaration//GEN-END:variables

}
