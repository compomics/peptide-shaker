package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.HtmlLinksRenderer;

/**
 * A simple dialog for showing the list of proteins a given peptide can map to.
 *  
 * @author Harald Barsnes
 */
public class ProteinInferencePeptideLevelDialog extends javax.swing.JDialog {

    /**
     * The PeptideShakerGUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

    /**
     * Create a new ProteinInferencePeptideLevelDialog.
     * 
     * @param aPeptideShakerGUI     the PeptideShakerGUI parent
     * @param modal                 modal or not modal
     * @param peptideMatchKey       the peptide match key
     * @param proteinMatchKey       the protein match key
     */
    public ProteinInferencePeptideLevelDialog(PeptideShakerGUI aPeptideShakerGUI, boolean modal, String peptideMatchKey, String proteinMatchKey) throws Exception {
        super(aPeptideShakerGUI, modal);
        initComponents();

        this.peptideShakerGUI = aPeptideShakerGUI;

        // make sure that the scroll panes are see-through
        proteinsJScrollPane.getViewport().setOpaque(false);

        // set up the table properties
        otherProteinJTable.getTableHeader().setReorderingAllowed(false);
        otherProteinJTable.getColumn(" ").setMinWidth(50);
        otherProteinJTable.getColumn(" ").setMaxWidth(50);
        otherProteinJTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));


        // set up the table properties
        retainedProteinJTable.getTableHeader().setReorderingAllowed(false);
        retainedProteinJTable.getColumn(" ").setMinWidth(50);
        retainedProteinJTable.getColumn(" ").setMaxWidth(50);
        retainedProteinJTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));

        // insert the values
        peptideSequenceJTextField.setText(Peptide.getSequence(peptideMatchKey));

        PeptideMatch peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideMatchKey);
        ArrayList<String> possibleProteins = peptideMatch.getTheoreticPeptide().getParentProteins();
        List<String> retainedProteins = Arrays.asList(ProteinMatch.getAccessions(proteinMatchKey));
        int possibleCpt = 0, retainedCpt = 0;
        for (String protein : possibleProteins) {

            String description;
            try {
                description = sequenceFactory.getHeader(protein).getDescription();
            } catch (Exception e) {
                description = "Fasta file Error";
            }

            if (retainedProteins.contains(protein)) {
                ((DefaultTableModel) retainedProteinJTable.getModel()).addRow(new Object[]{
                            (++retainedCpt),
                            peptideShakerGUI.addDatabaseLink(protein),
                            description
                        });
            } else {
                ((DefaultTableModel) otherProteinJTable.getModel()).addRow(new Object[]{
                            (++possibleCpt),
                            peptideShakerGUI.addDatabaseLink(protein),
                            description
                        });
            }
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                // set the preferred size of the accession column
                int width = peptideShakerGUI.getPreferredColumnWidth(otherProteinJTable, otherProteinJTable.getColumn("Accession").getModelIndex(), 6);
                otherProteinJTable.getColumn("Accession").setMinWidth(width);
                otherProteinJTable.getColumn("Accession").setMaxWidth(width);
                // set the preferred size of the accession column
                width = peptideShakerGUI.getPreferredColumnWidth(retainedProteinJTable, retainedProteinJTable.getColumn("Accession").getModelIndex(), 6);
                retainedProteinJTable.getColumn("Accession").setMinWidth(width);
                retainedProteinJTable.getColumn("Accession").setMaxWidth(width);
            }
        });

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        proteinsPanel = new javax.swing.JPanel();
        proteinsJScrollPane = new javax.swing.JScrollPane();
        otherProteinJTable = new javax.swing.JTable();
        peptidesPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        peptideSequenceJTextField = new javax.swing.JTextField();
        closeButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        retainedProteinJTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Protein Inference - Peptide Level");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        proteinsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Other proteins"));
        proteinsPanel.setOpaque(false);

        proteinsJScrollPane.setOpaque(false);

        otherProteinJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        otherProteinJTable.setOpaque(false);
        otherProteinJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                otherProteinJTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                otherProteinJTableMouseReleased(evt);
            }
        });
        otherProteinJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                otherProteinJTableMouseMoved(evt);
            }
        });
        proteinsJScrollPane.setViewportView(otherProteinJTable);

        javax.swing.GroupLayout proteinsPanelLayout = new javax.swing.GroupLayout(proteinsPanel);
        proteinsPanel.setLayout(proteinsPanelLayout);
        proteinsPanelLayout.setHorizontalGroup(
            proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 725, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinsPanelLayout.setVerticalGroup(
            proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide"));
        peptidesPanel.setOpaque(false);

        jLabel1.setText("Sequence:");

        peptideSequenceJTextField.setEditable(false);
        peptideSequenceJTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout peptidesPanelLayout = new javax.swing.GroupLayout(peptidesPanel);
        peptidesPanel.setLayout(peptidesPanelLayout);
        peptidesPanelLayout.setHorizontalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(peptideSequenceJTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 656, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesPanelLayout.setVerticalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(peptideSequenceJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Retained proteins"));
        jPanel1.setOpaque(false);

        retainedProteinJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Description"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        retainedProteinJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                retainedProteinJTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                retainedProteinJTableMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(retainedProteinJTable);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 725, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(peptidesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(proteinsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(closeButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proteinsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(closeButton)
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
     * Closes the dialog.
     * 
     * @param evt 
     */
    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    /**
     * Try to open the protein's web link (if available).
     * 
     * @param evt 
     */
    private void otherProteinJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_otherProteinJTableMouseReleased
        int row = otherProteinJTable.getSelectedRow();
        int column = otherProteinJTable.getSelectedColumn();

        if (row != -1) {
            if (column == otherProteinJTable.getColumn("Accession").getModelIndex()) {

                // open protein links in web browser
                if (evt != null && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) otherProteinJTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) otherProteinJTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_otherProteinJTableMouseReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * HTML link.
     *
     * @param evt
     */
    private void otherProteinJTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_otherProteinJTableMouseMoved
        int row = otherProteinJTable.rowAtPoint(evt.getPoint());
        int column = otherProteinJTable.columnAtPoint(evt.getPoint());

        if (otherProteinJTable.getValueAt(row, column) != null) {
            if (column == otherProteinJTable.getColumn("Accession").getModelIndex()) {
                String tempValue = (String) otherProteinJTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_otherProteinJTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void otherProteinJTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_otherProteinJTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_otherProteinJTableMouseExited

    private void retainedProteinJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_retainedProteinJTableMouseReleased
        int row = retainedProteinJTable.rowAtPoint(evt.getPoint());
        int column = retainedProteinJTable.columnAtPoint(evt.getPoint());

        if (retainedProteinJTable.getValueAt(row, column) != null) {
            if (column == retainedProteinJTable.getColumn("Accession").getModelIndex()) {
                String tempValue = (String) retainedProteinJTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_retainedProteinJTableMouseReleased

    private void retainedProteinJTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_retainedProteinJTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_retainedProteinJTableMouseExited

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable otherProteinJTable;
    private javax.swing.JTextField peptideSequenceJTextField;
    private javax.swing.JPanel peptidesPanel;
    private javax.swing.JScrollPane proteinsJScrollPane;
    private javax.swing.JPanel proteinsPanel;
    private javax.swing.JTable retainedProteinJTable;
    // End of variables declaration//GEN-END:variables
}
