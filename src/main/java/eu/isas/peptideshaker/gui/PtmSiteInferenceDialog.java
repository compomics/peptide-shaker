/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PtmInferenceDialog.java
 *
 * Created on Dec 19, 2011, 3:45:30 PM
 */
package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author vaudel
 */
public class PtmSiteInferenceDialog extends javax.swing.JDialog {

    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The PTM investigated
     */
    private PTM ptm;
    /**
     * The peptide scoring
     */
    private PtmScoring peptideScoring = null;
    /**
     * The key of the investigated peptide
     */
    private String peptideKey;
    /**
     * list of psms for this peptide
     */
    private ArrayList<SpectrumMatch> psms = new ArrayList<SpectrumMatch>();

    /**
     * Constructor
     * @param peptideShakerGUI  The main GUI
     * @param peptideKey        The peptide key of the investigated peptide
     * @param ptm               The PTM investigated
     */
    public PtmSiteInferenceDialog(PeptideShakerGUI peptideShakerGUI, String peptideKey, PTM ptm) {
        super(peptideShakerGUI, true);

        this.peptideShakerGUI = peptideShakerGUI;
        this.ptm = ptm;
        this.peptideKey = peptideKey;
        try {
            PeptideMatch peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
            PSPtmScores peptideScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
            if (peptideScores != null) {
                this.peptideScoring = peptideScores.getPtmScoring(ptm.getName());
            }
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psms.add(peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey));
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            dispose();
        }

        initComponents();

        if (peptideScoring != null) {
            peptidePtmConfidence.setSelectedIndex(peptideScoring.getPtmSiteConfidence()+1);
        }

        // set sequence
        sequenceLabel.setText(peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide().getModifiedSequenceAsHtml(
                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true));

        // set the modification tooltip
        String tooltip = peptideShakerGUI.getPeptideModificationTooltipAsHtml(peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide());
        sequenceLabel.setToolTipText(tooltip);

        setVisible(true);
    }

    /**
     * Table model for the ptm site selection table
     */
    private class SiteSelectionTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return Peptide.getSequence(peptideKey).length();
        }

        @Override
        public int getColumnCount() {
            return psms.size() + 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "AA";
                case 1:
                    return "Main site";
                case 2:
                    return "Secondary site";
                default:
                    int psmNumber = column - 3;
                    return "PSM " + psmNumber;
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                switch (column) {
                    case 0:
                        return Peptide.getSequence(peptideKey).charAt(row);
                    case 1:
                        if (peptideScoring != null && peptideScoring.getPtmLocation().contains(row + 1)) {
                            return true;
                        }
                        return false;
                    case 2:
                        if (peptideScoring != null && peptideScoring.getSecondaryPtmLocations().contains(row + 1)) {
                            return true;
                        }
                        return false;
                    default:
                        int psmNumber = column - 3;
                        PSPtmScores psmScores = (PSPtmScores) psms.get(psmNumber).getUrParam(new PSPtmScores());
                        if (psmScores != null) {
                            PtmScoring psmScoring = psmScores.getPtmScoring(ptm.getName());
                            if (psmScoring != null) {
                                if (psmScoring.getPtmLocation().contains(row + 1)) {
                                    return psmScoring.getPtmSiteConfidence();
                                } else {
                                    return PtmScoring.NOT_FOUND;
                                }
                            }
                        }
                        return PtmScoring.RANDOM;
                }

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
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
            return (new Double(0.0)).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {

            if (columnIndex == 1 || columnIndex == 2) {
                return true;
            }

            return false;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        peptidePtmConfidence = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        sequenceLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        ptmSiteTable = new javax.swing.JTable();
        cancelButton = new javax.swing.JButton();
        okbutton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide"));

        jLabel1.setText("Site assignment confidence:");

        peptidePtmConfidence.setModel(new DefaultComboBoxModel(PtmScoring.getPossibleConfidenceLevels()));

        jLabel2.setText("Sequence:");

        sequenceLabel.setText("Peptide Sequence");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(peptidePtmConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(63, 63, 63)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(sequenceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(peptidePtmConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(sequenceLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Potential Modification Sites"));

        ptmSiteTable.setModel(new SiteSelectionTable());
        jScrollPane1.setViewportView(ptmSiteTable);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                .addContainerGap())
        );

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okbutton.setText("OK");
        okbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okbuttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(553, 553, 553)
                        .addComponent(okbutton, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okbutton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okbuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okbuttonActionPerformed
        // TODO update back-end data and reload panels
    }//GEN-LAST:event_okbuttonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okbutton;
    private javax.swing.JComboBox peptidePtmConfidence;
    private javax.swing.JTable ptmSiteTable;
    private javax.swing.JLabel sequenceLabel;
    // End of variables declaration//GEN-END:variables
}
