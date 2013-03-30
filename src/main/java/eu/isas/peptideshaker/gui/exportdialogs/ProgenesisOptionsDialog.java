/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.gui.exportdialogs;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.awt.Frame;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import uk.ac.ebi.das.jdas.schema.features.PARENT;

/**
 * This dialog allows the user to choose beteen the different progenesis export
 * options
 *
 * @author Marc
 */
public class ProgenesisOptionsDialog extends javax.swing.JDialog {

    /**
     * Boolean indicating whether the user canceled the process
     */
    private boolean canceled = false;
    /**
     * The identification of the project
     */
    private Identification identification;
    /**
     * The identification features generator
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The filter prefereneces
     */
    private FilterPreferences filterPreferences;
    /**
     * The parent frame
     */
    private Frame parent;
    /**
     * The search parameters
     */
    private SearchParameters searchParameters;

    /**
     * Creates new form ProgenesisOptionsDialog
     */
    public ProgenesisOptionsDialog(java.awt.Frame parent, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, FilterPreferences filterPreferences, SearchParameters searchParameters) {
        super(parent, true);
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.filterPreferences = filterPreferences;
        this.parent = parent;
        this.searchParameters = searchParameters;
        initComponents();
        this.setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Returns a list of the selected psms
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     */
    public ArrayList<String> getSelectedPsms() throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        if (!canceled) {
            ArrayList<String> result = new ArrayList<String>();
            PSParameter psParameter = new PSParameter();
            if (choiceComboBox.getSelectedIndex() == 0) {
                identification.loadProteinMatches(null);
                identification.loadProteinMatchParameters(psParameter, null);
                for (String proteinKey : identificationFeaturesGenerator.getProteinKeys(null, filterPreferences)) {
                    psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                    if (psParameter.isValidated() && !psParameter.isHidden()) {
                        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
                        identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);
                        identification.loadPeptideMatchParameters(proteinMatch.getPeptideMatches(), psParameter, null);
                        for (String peptideKey : proteinMatch.getPeptideMatches()) {
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                            if (psParameter.isValidated() && !psParameter.isHidden()) {
                                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                                identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), psParameter, null);
                                for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                                    if (psParameter.isValidated() && !psParameter.isHidden()) {
                                        result.add(spectrumKey);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (choiceComboBox.getSelectedIndex() == 1) {
                identification.loadPeptideMatches(null);
                identification.loadPeptideMatchParameters(psParameter, null);
                for (String peptideKey : identification.getPeptideIdentification()) {
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
                    if (psParameter.isValidated() && !psParameter.isHidden()) {
                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                        identification.loadSpectrumMatchParameters(peptideMatch.getSpectrumMatches(), psParameter, null);
                        for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                            if (psParameter.isValidated() && !psParameter.isHidden()) {
                                result.add(spectrumKey);
                            }
                        }
                    }
                }
            } else if (choiceComboBox.getSelectedIndex() == 2) {
                for (String spectrumFile : identification.getSpectrumFiles()) {
                    identification.loadSpectrumMatchParameters(spectrumFile, psParameter, null);
                    for (String spectrumKey : identification.getSpectrumIdentification(spectrumFile)) {
                        psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                        if (psParameter.isValidated() && !psParameter.isHidden()) {
                            result.add(spectrumKey);
                        }
                    }
                }
            } else if (choiceComboBox.getSelectedIndex() == 3) {
                PtmSelectionDialog ptmSelectionDialog = new PtmSelectionDialog(parent, searchParameters.getModificationProfile().getAllNotFixedModifications());
                ArrayList<String> ptms = ptmSelectionDialog.selectedModifications();
                if (ptms != null && !ptms.isEmpty()) {
                    boolean confidentOnly = ptmSelectionDialog.confidentOnly();
                    for (String spectrumFile : identification.getSpectrumFiles()) {
                        identification.loadSpectrumMatches(spectrumFile, null);
                        identification.loadSpectrumMatchParameters(spectrumFile, psParameter, null);
                        for (String spectrumKey : identification.getSpectrumIdentification(spectrumFile)) {
                            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);
                            if (psParameter.isValidated() && !psParameter.isHidden()) {
                                SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                                boolean confident = true;
                                boolean found = false;
                                for (ModificationMatch modMatch : spectrumMatch.getBestAssumption().getPeptide().getModificationMatches()) {
                                    if (ptms.contains(modMatch.getTheoreticPtm())) {
                                        found = true;
                                        if (!confidentOnly) {
                                            break;
                                        } else if (!modMatch.isConfident()) {
                                            confident = false;
                                            break;
                                        }
                                    }
                                }
                                if (found && confident) {
                                    result.add(spectrumKey);
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        choiceComboBox = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jButton1.setText("Cancel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("OK");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("PSMs to export"));

        choiceComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Protein", "Peptide", "PSM", "Confidently localized PTMs" }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(choiceComboBox, 0, 348, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(choiceComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        canceled = true;
        dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton2ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox choiceComboBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
