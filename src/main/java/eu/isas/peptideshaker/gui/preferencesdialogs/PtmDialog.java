package eu.isas.peptideshaker.gui.preferencesdialogs;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.pride.MyComboBoxRenderer;
import java.awt.Window;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import no.uib.olsdialog.OLSDialog;
import no.uib.olsdialog.OLSInputable;

/**
 * This dialog allows the user to create/edit PTMs.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PtmDialog extends javax.swing.JDialog implements OLSInputable {

    /**
     * SearchGUIs search panel.
     */
    private SearchPreferencesDialog searchPreferencesDialog;
    /**
     * The post translational modifications factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The supported amino-acids.
     */
    private final List<String> aminoAcids = Arrays.asList(
            "A", "C", "D", "E", "F", "G", "H", "I", "K", "L", "M",
            "N", "P", "Q", "R", "S", "T", "V", "W", "Y", "U",
            "[", "]");
    /**
     * The edited ptm.
     */
    private PTM currentPtm = null;

    /**
     * Creates a new PTM dialog.
     *
     * @param searchPreferencesDialog the search panel
     * @param currentPTM the ptm to edit (can be null)
     */
    public PtmDialog(SearchPreferencesDialog searchPreferencesDialog, PTM currentPTM) {
        super(searchPreferencesDialog, true);

        this.searchPreferencesDialog = searchPreferencesDialog;
        this.currentPtm = currentPTM;

        initComponents();

        Vector comboboxTooltips = new Vector();
        comboboxTooltips.add("Modification at particular amino acids");
        comboboxTooltips.add("Modification at the N terminus of a protein");
        comboboxTooltips.add("Modification at the N terminus of a protein at particular amino acids");
        comboboxTooltips.add("Modification at the C terminus of a protein");
        comboboxTooltips.add("Modification at the C terminus of a protein at particular amino acids");
        comboboxTooltips.add("Modification at the N terminus of a peptide");
        comboboxTooltips.add("Modification at the N terminus of a peptide at particular amino acids");
        comboboxTooltips.add("Modification at the C terminus of a peptide");
        comboboxTooltips.add("Modification at the C terminus of a peptide at particular amino acids");
        typeCmb.setRenderer(new MyComboBoxRenderer(comboboxTooltips, SwingConstants.CENTER));

        if (currentPTM != null) {
            typeCmb.setSelectedIndex(currentPTM.getType());
            nameTxt.setText(currentPTM.getName());
            massTxt.setText(currentPTM.getMass() + "");
            String residues = "";
            boolean first = true;
            for (String aa : currentPTM.getResidues()) {
                if (!aa.equals("[") && !aa.equals("]")) {
                    if (first) {
                        first = false;
                    } else {
                        residues += ", ";
                    }
                    residues += aa;
                }
            }
            residuesTxt.setText(residues);
            setTitle("Edit Modification");
        }

        setLocationRelativeTo(searchPreferencesDialog);
        setVisible(true);
    }

    /**
     * Parses residues from the residues text field.
     *
     * @return a list of residues
     */
    private ArrayList<String> parseResidues() {
        ArrayList<String> result = new ArrayList<String>();
        int modType = typeCmb.getSelectedIndex();
        if (modType == PTM.MODAA
                || modType == PTM.MODNAA
                || modType == PTM.MODNPAA
                || modType == PTM.MODCAA
                || modType == PTM.MODCPAA) {
            String text = residuesTxt.getText();
            String[] split = text.split(","); // @TODO: allow other separators
            for (String part : split) {
                if (!part.trim().equals("")) {
                    result.add(part.trim().toUpperCase());
                }
            }
        }
        if (modType == PTM.MODC || modType == PTM.MODCP || modType == PTM.MODCAA || modType == PTM.MODCPAA) {
            result.add("]");
        }
        if (modType == PTM.MODN || modType == PTM.MODNP || modType == PTM.MODNAA || modType == PTM.MODNPAA) {
            result.add("[");
        }
        return result;
    }

    /**
     * Returns a boolean indicating whether the input can be translated into a
     * PTM.
     *
     * @return a boolean indicating whether the input can be translated into a
     * PTM
     */
    private boolean validateInput() {
        String name = nameTxt.getText().trim();
        if (name.contains("_")) {
            String newName = name.replace("_", " ");
            int outcome = JOptionPane.showConfirmDialog(this, "For processing with PeptideShaker '_' "
                    + "should be avoided in modification names. Shall " + name + " be replaced by "
                    + newName + "?", "'_' in name", JOptionPane.YES_NO_OPTION);
            if (outcome == JOptionPane.YES_OPTION) {
                nameTxt.setText(newName);
            } else {
                return false;
            }
        }
        name = nameTxt.getText().trim();
        if (ptmFactory.getDefaultModifications().contains(name)) {
            JOptionPane.showMessageDialog(this, "A modification named " + name + " alredy exists in the "
                    + "default modification lists. Please select the default modification or use another name.",
                    "Modification already exists", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (ptmFactory.getUserModifications().contains(name) && currentPtm == null) {
            int outcome = JOptionPane.showConfirmDialog(this, "There is already a modification named " + name
                    + ". Shall it be overwritten?", "Modification already exists", JOptionPane.YES_NO_OPTION);
            if (outcome == JOptionPane.NO_OPTION) {
                return false;
            }
        }
        try {
            new Double(massTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please verify the input for the modification mass.",
                    "Wrong mass", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        for (String aa : parseResidues()) {
            if (!aminoAcids.contains(aa.toUpperCase())) {
                JOptionPane.showMessageDialog(this, "The following entry could not be parsed into an amino-acid: "
                        + aa, "Wrong amino-acid", JOptionPane.WARNING_MESSAGE);
                return false;
            }
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

        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        detailsPanel = new javax.swing.JPanel();
        typeCmb = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        nameTxt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        massTxt = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        residuesTxt = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        psiModMappingJTextField = new javax.swing.JTextField();
        olsJButton = new javax.swing.JButton();
        helpJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("New Modification");
        setResizable(false);

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

        detailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Properties"));

        typeCmb.setMaximumRowCount(15);
        typeCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Particular Amino Acid", "Protein N-term", "Protein N-term - Particular Amino Acid(s)", "Protein C-term", "Protein C-term - Particular Amino Acid(s)", "Peptide N-term", "Peptide N-term - Particular Amino Acid(s)", "Peptide C-term", "Peptide C-term - Particular Amino Acid(s)" }));
        typeCmb.setEnabled(false);
        typeCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeCmbActionPerformed(evt);
            }
        });

        jLabel1.setText("Type:");

        jLabel2.setText("Name:");

        nameTxt.setEditable(false);
        nameTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel3.setText("Monoisotopic Mass:");

        massTxt.setEditable(false);
        massTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel4.setText("Da");

        jLabel5.setText("Modified Residue(s):");

        residuesTxt.setEditable(false);
        residuesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel6.setText("PSI-MOD:");

        psiModMappingJTextField.setEditable(false);
        psiModMappingJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        olsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ols_transparent.GIF"))); // NOI18N
        olsJButton.setToolTipText("Ontology Lookup Service");
        olsJButton.setPreferredSize(new java.awt.Dimension(61, 23));
        olsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                olsJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout detailsPanelLayout = new javax.swing.GroupLayout(detailsPanel);
        detailsPanel.setLayout(detailsPanelLayout);
        detailsPanelLayout.setHorizontalGroup(
            detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailsPanelLayout.createSequentialGroup()
                        .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
                            .addComponent(jLabel5)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(detailsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(78, 78, 78)))
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailsPanelLayout.createSequentialGroup()
                        .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(massTxt)
                            .addComponent(psiModMappingJTextField)
                            .addComponent(nameTxt)
                            .addComponent(typeCmb, 0, 282, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(olsJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(residuesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        detailsPanelLayout.setVerticalGroup(
            detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeCmb)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameTxt)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(massTxt)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(residuesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(olsJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(psiModMappingJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6)))
                .addContainerGap())
        );

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(helpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(detailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(detailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cancelButton)
                    .addComponent(okButton)
                    .addComponent(helpJButton, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Add the ptm to the SearchPanel.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (validateInput()) {
            PTM otherPTM, newPTM = new PTM(typeCmb.getSelectedIndex(), nameTxt.getText().trim().toLowerCase(), new Double(massTxt.getText().trim()), parseResidues());
            for (String ptm : ptmFactory.getPTMs()) {
                if (currentPtm == null || !ptm.equals(currentPtm.getName())) {
                    otherPTM = ptmFactory.getPTM(ptm);
                    if (newPTM.isSameAs(otherPTM)) {
                        int outcome = JOptionPane.showConfirmDialog(this, "The modification " + ptm
                                + " presents characteristics similar to your input. Are you sure you want to create this new modification?",
                                "Modification already exists", JOptionPane.YES_NO_OPTION);
                        if (outcome == JOptionPane.NO_OPTION) {
                            return;
                        }
                    }
                }
            }
            if (currentPtm != null) {
                ptmFactory.removeUserPtm(currentPtm.getName());
            }

            ptmFactory.addUserPTM(newPTM);

            // @TODO: we have store the PSI-MOD mappings!!!

            // @TODO: update the psi-mod mappings in the SearchPreferencesDialog!
            
            dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Update the type selection.
     *
     * @param evt
     */
    private void typeCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeCmbActionPerformed
        if (typeCmb.getSelectedIndex() == 0
                || typeCmb.getSelectedIndex() == 2
                || typeCmb.getSelectedIndex() == 4
                || typeCmb.getSelectedIndex() == 6
                || typeCmb.getSelectedIndex() == 8) {
            residuesTxt.setEnabled(true);
        } else {
            residuesTxt.setEnabled(false);
        }
    }//GEN-LAST:event_typeCmbActionPerformed

    /**
     * Opens the OLS Dialog.
     *
     * @param evt
     */
    private void olsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_olsJButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        String searchTerm = null;
        String ontology = "MOD";

        if (psiModMappingJTextField.getText().length() > 0) {

            searchTerm = psiModMappingJTextField.getText();

            ontology = searchTerm.substring(searchTerm.lastIndexOf("[") + 1, searchTerm.lastIndexOf("]") - 1);

            searchTerm = psiModMappingJTextField.getText().substring(
                    0, psiModMappingJTextField.getText().lastIndexOf("[") - 1);
            searchTerm = searchTerm.replaceAll("-", " ");
            searchTerm = searchTerm.replaceAll(":", " ");
            searchTerm = searchTerm.replaceAll("\\(", " ");
            searchTerm = searchTerm.replaceAll("\\)", " ");
            searchTerm = searchTerm.replaceAll("&", " ");
            searchTerm = searchTerm.replaceAll("\\+", " ");
            searchTerm = searchTerm.replaceAll("\\[", " ");
            searchTerm = searchTerm.replaceAll("\\]", " ");
        }

        new OLSDialog(this, this, true, "mod", ontology, searchTerm);
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_olsJButtonActionPerformed

    /**
     * Changes the cursor to a hand cursor.
     *
     * @param evt
     */
    private void helpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_helpJButtonMouseEntered

    /**
     * Change the cursor to the default cursor.
     *
     * @param evt
     */
    private void helpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpJButtonMouseExited

    /**
     * Opens the help dialog.
     *
     * @param evt
     */
    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(this, getClass().getResource("/helpFiles/PtmDialog.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel detailsPanel;
    private javax.swing.JButton helpJButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JTextField massTxt;
    private javax.swing.JTextField nameTxt;
    private javax.swing.JButton okButton;
    private javax.swing.JButton olsJButton;
    private javax.swing.JTextField psiModMappingJTextField;
    private javax.swing.JTextField residuesTxt;
    private javax.swing.JComboBox typeCmb;
    // End of variables declaration//GEN-END:variables

    @Override
    public void insertOLSResult(String field, String selectedValue,
            String accession, String ontologyShort, String ontologyLong, int modifiedRow, String mappedTerm, Map<String, String> metadata) {
        setModMapping(selectedValue, accession, ontologyShort);
    }

    @Override
    public Window getWindow() {
        return (Window) this;
    }

    /**
     * Set the PSI-MOD mapping.
     *
     * @param name
     * @param accession
     * @param ontology
     */
    public void setModMapping(String name, String accession, String ontology) {
        psiModMappingJTextField.setText(name + " [" + accession + "]");
        psiModMappingJTextField.setCaretPosition(0);
    }
}
