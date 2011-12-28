package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.renderers.AlignedTableCellRenderer;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;

/**
 * This dialog allows the user to verify/update the modification site
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
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
     * Main ptm site selection
     */
    private boolean[] mainSelection;
    /**
     * Secondary ptm site selection
     */
    private boolean[] secondarySelection;
    /**
     * PTM confidence tooltip map, key: ptm confidence type, element: ptm confidence as a string.
     */
    private HashMap<Integer, String> ptmConfidenceTooltipMap;

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
                mainSelection = new boolean[Peptide.getSequence(peptideKey).length()];
                secondarySelection = new boolean[Peptide.getSequence(peptideKey).length()];
                for (int aa = 0; aa < Peptide.getSequence(peptideKey).length(); aa++) {
                    if (peptideScoring.getPtmLocation().contains(aa + 1)) {
                        mainSelection[aa] = true;
                    }
                    if (peptideScoring.getSecondaryPtmLocations().contains(aa + 1)) {
                        secondarySelection[aa] = true;
                    }
                }
            }
            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                psms.add(peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey));
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            dispose();
        }

        initComponents();

        setTableProperties();

        if (peptideScoring != null) {
            peptidePtmConfidence.setSelectedIndex(peptideScoring.getPtmSiteConfidence() + 1);
        }

        // set sequence
        updateSequenceLabel();

        // set the modification tooltip
        String tooltip = peptideShakerGUI.getPeptideModificationTooltipAsHtml(peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide());
        sequenceLabel.setToolTipText(tooltip);

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        peptidePtmConfidence.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        ptmSiteTableScrollPane.getViewport().setOpaque(false);
        ptmSiteTable.getTableHeader().setReorderingAllowed(false);

        // centrally align the column headers 
        TableCellRenderer renderer = ptmSiteTable.getTableHeader().getDefaultRenderer();
        JLabel label = (JLabel) renderer;
        label.setHorizontalAlignment(JLabel.CENTER);

        // cell renderers
        ptmSiteTable.getColumn("AA").setCellRenderer(new AlignedTableCellRenderer(SwingConstants.CENTER, Color.LIGHT_GRAY));
        ptmSiteTable.getColumn("S1").setCellRenderer(new NimbusCheckBoxRenderer());
        ptmSiteTable.getColumn("S2").setCellRenderer(new NimbusCheckBoxRenderer());

        ptmSiteTable.getColumn("AA").setMinWidth(25);
        ptmSiteTable.getColumn("AA").setMaxWidth(25);
        ptmSiteTable.getColumn("S1").setMinWidth(25);
        ptmSiteTable.getColumn("S1").setMaxWidth(25);
        ptmSiteTable.getColumn("S2").setMinWidth(25);
        ptmSiteTable.getColumn("S2").setMaxWidth(25);

        // set up the PTM confidence color map
        HashMap<Integer, Color> ptmConfidenceColorMap = new HashMap<Integer, Color>();
        ptmConfidenceColorMap.put(PtmScoring.NOT_FOUND, Color.lightGray);
        ptmConfidenceColorMap.put(PtmScoring.RANDOM, Color.RED);
        ptmConfidenceColorMap.put(PtmScoring.DOUBTFUL, Color.ORANGE);
        ptmConfidenceColorMap.put(PtmScoring.CONFIDENT, Color.YELLOW);
        ptmConfidenceColorMap.put(PtmScoring.VERY_CONFIDENT, peptideShakerGUI.getSparklineColor());

        // set up the PTM confidence tooltip map
        ptmConfidenceTooltipMap = new HashMap<Integer, String>();
        ptmConfidenceTooltipMap.put(PtmScoring.NOT_FOUND, "Not Found");
        ptmConfidenceTooltipMap.put(PtmScoring.RANDOM, "Random Assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.DOUBTFUL, "Doubtful Assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.CONFIDENT, "Confident Assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.VERY_CONFIDENT, "Very Confident Assignment");

        for (int i = 3; i < ptmSiteTable.getColumnCount(); i++) {
            ptmSiteTable.getColumn(ptmSiteTable.getColumnName(i)).setCellRenderer(
                    new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), ptmConfidenceColorMap, ptmConfidenceTooltipMap));
        }
    }

    /**
     * Updates the sequence label based on the selection in the table
     */
    private void updateSequenceLabel() {
        PeptideMatch peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
        PSPtmScores ptmScores = new PSPtmScores();
        ptmScores = (PSPtmScores) peptideMatch.getUrParam(ptmScores);
        HashMap<Integer, ArrayList<String>> mainLocations = new HashMap<Integer, ArrayList<String>>();
        mainLocations.putAll(ptmScores.getMainModificationSites());
        HashMap<Integer, ArrayList<String>> secondaryLocations = new HashMap<Integer, ArrayList<String>>();
        secondaryLocations.putAll(ptmScores.getSecondaryModificationSites());
        String modName = ptm.getName();
        int aa;
        for (int i = 0; i < mainSelection.length; i++) {
            aa = i + 1;
            if (mainSelection[i]) {
                if (!mainLocations.containsKey(aa)) {
                    mainLocations.put(aa, new ArrayList<String>());
                }
                if (!mainLocations.get(aa).contains(modName)) {
                    mainLocations.get(aa).add(modName);
                }
            } else {
                if (mainLocations.containsKey(aa) && mainLocations.get(aa).contains(modName)) {
                    mainLocations.get(aa).remove(modName);
                }
            }
            if (secondarySelection[i]) {
                if (!secondaryLocations.containsKey(aa)) {
                    secondaryLocations.put(aa, new ArrayList<String>());
                }
                if (!secondaryLocations.get(aa).contains(modName)) {
                    secondaryLocations.get(aa).add(modName);
                }
            } else {
                if (secondaryLocations.containsKey(aa) && secondaryLocations.get(aa).contains(modName)) {
                    secondaryLocations.get(aa).remove(modName);
                }
            }
        }

        sequenceLabel.setText(Peptide.getModifiedSequenceAsHtml(peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(),
                true, peptideMatch.getTheoreticPeptide(), mainLocations, secondaryLocations));
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
                    return "S1";
                case 2:
                    return "S2";
                default:
                    int psmNumber = column - 2;
                    return "" + psmNumber;
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                switch (column) {
                    case 0:
                        return Peptide.getSequence(peptideKey).charAt(row);
                    case 1:
                        return mainSelection[row];
                    case 2:
                        return secondarySelection[row];
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

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == 1) {
                mainSelection[row] = !mainSelection[row];
                if (mainSelection[row] && secondarySelection[row]) {
                    secondarySelection[row] = false;
                }
                updateSequenceLabel();
            } else if (column == 2) {
                secondarySelection[row] = !secondarySelection[row];
                if (mainSelection[row] && secondarySelection[row]) {
                    mainSelection[row] = false;
                }
                updateSequenceLabel();
            }
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

        backgroundPanel = new javax.swing.JPanel();
        peptidePanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        peptidePtmConfidence = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        sequenceLabel = new javax.swing.JLabel();
        ptmSitePanel = new javax.swing.JPanel();
        ptmSiteTableScrollPane = new javax.swing.JScrollPane();
        ptmSiteTable = new javax.swing.JTable();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        openDialogHelpJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PTM Site Assignment");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        peptidePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide"));
        peptidePanel.setOpaque(false);

        jLabel1.setText("Site Assignment Confidence:");

        peptidePtmConfidence.setModel(new DefaultComboBoxModel(PtmScoring.getPossibleConfidenceLevels()));

        jLabel2.setText("Sequence:");

        sequenceLabel.setText("Peptide Sequence");

        javax.swing.GroupLayout peptidePanelLayout = new javax.swing.GroupLayout(peptidePanel);
        peptidePanel.setLayout(peptidePanelLayout);
        peptidePanelLayout.setHorizontalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(peptidePtmConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(sequenceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidePanelLayout.setVerticalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(peptidePtmConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sequenceLabel)
                    .addComponent(jLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        ptmSitePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Potential Modification Sites"));
        ptmSitePanel.setOpaque(false);

        ptmSiteTableScrollPane.setOpaque(false);

        ptmSiteTable.setModel(new SiteSelectionTable());
        ptmSiteTable.setOpaque(false);
        ptmSiteTableScrollPane.setViewportView(ptmSiteTable);

        javax.swing.GroupLayout ptmSitePanelLayout = new javax.swing.GroupLayout(ptmSitePanel);
        ptmSitePanel.setLayout(ptmSitePanelLayout);
        ptmSitePanelLayout.setHorizontalGroup(
            ptmSitePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ptmSitePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ptmSiteTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE)
                .addContainerGap())
        );
        ptmSitePanelLayout.setVerticalGroup(
            ptmSitePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ptmSitePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ptmSiteTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                .addContainerGap())
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

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(ptmSitePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 510, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(peptidePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ptmSitePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cancelButton)
                        .addComponent(okButton))
                    .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE))
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

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        boolean changed = false;
        int aa;
        for (int i = 0; i < mainSelection.length; i++) {
            aa = i + 1;
            if (mainSelection[i]) {
                if (!peptideScoring.getPtmLocation().contains(aa)) {
                    peptideScoring.addPtmLocation(aa);
                    changed = true;
                }
            } else {
                if (peptideScoring.getPtmLocation().contains(aa)) {
                    peptideScoring.removePtmLocation(aa);
                    changed = true;
                }
            }
            if (secondarySelection[i]) {
                if (!peptideScoring.getSecondaryPtmLocations().contains(aa)) {
                    peptideScoring.addPtmSecondaryLocation(aa);
                    changed = true;
                }
            } else {
                if (peptideScoring.getSecondaryPtmLocations().contains(aa)) {
                    peptideScoring.removePtmSecondaryLocation(aa);
                    changed = true;
                }
            }
        }
        if (changed) {
            // save changes in the peptide match
            PeptideMatch peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
            PSPtmScores scores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
            scores.addPtmScoring(ptm.getName(), peptideScoring);
            for (int mainLocation : peptideScoring.getPtmLocation()) {
                scores.addMainModificationSite(ptm.getName(), mainLocation);
            }
            for (int secondaryLocation : peptideScoring.getSecondaryPtmLocations()) {
                scores.addSecondaryModificationSite(ptm.getName(), secondaryLocation);
            }
            peptideShakerGUI.getIdentification().setMatchChanged(peptideMatch);
            // update protein level PTM scoring
            PeptideShaker miniShaker = new PeptideShaker(peptideShakerGUI.getExperiment(), peptideShakerGUI.getSample(), peptideShakerGUI.getReplicateNumber());
            ArrayList<String> proteins = peptideMatch.getTheoreticPeptide().getParentProteins();
            ProteinMatch proteinMatch;
            boolean candidate;
            for (String proteinKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {
                candidate = false;
                for (String protein : proteins) {
                    if (proteinKey.contains(protein)) {
                        candidate = true;
                    }
                }
                if (candidate) {
                    proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                    if (proteins.contains(proteinMatch.getMainMatch())) {
                        try {
                            miniShaker.scorePTMs(proteinMatch, peptideShakerGUI.getSearchParameters(), peptideShakerGUI.getAnnotationPreferences(), false);
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                        }
                    }
                }
            }
        }
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Change the cursor icon to a hand icon.
     * 
     * @param evt 
     */
    private void openDialogHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openDialogHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonMouseEntered

    /**
     * Change the cursor icon to the default icon.
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PtmSiteInferenceDialog.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton okButton;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JPanel peptidePanel;
    private javax.swing.JComboBox peptidePtmConfidence;
    private javax.swing.JPanel ptmSitePanel;
    private javax.swing.JTable ptmSiteTable;
    private javax.swing.JScrollPane ptmSiteTableScrollPane;
    private javax.swing.JLabel sequenceLabel;
    // End of variables declaration//GEN-END:variables
}
