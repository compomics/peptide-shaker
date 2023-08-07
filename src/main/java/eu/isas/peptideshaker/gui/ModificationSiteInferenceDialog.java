package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.gui.renderers.AlignedTableCellRenderer;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.experiment.identification.peptide_shaker.PSModificationScores;
import eu.isas.peptideshaker.preferences.DisplayParameters;
import eu.isas.peptideshaker.ptm.ModificationLocalizationScorer;
import com.compomics.util.experiment.identification.peptide_shaker.ModificationScoring;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.awt.Color;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;

/**
 * This dialog allows the user to verify/update the modification site.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ModificationSiteInferenceDialog extends javax.swing.JDialog {

    /**
     * The main GUI.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The modification investigated.
     */
    private Modification ptm;
    /**
     * The modification factory.
     */
    private ModificationFactory modificationFactory = ModificationFactory.getInstance();
    /**
     * The peptide modification scoring.
     */
    private PSModificationScores peptidePtmScore = null;
    /**
     * The peptide match.
     */
    private PeptideMatch peptideMatch;
    /**
     * list of PSMs for this peptide.
     */
    private ArrayList<SpectrumMatch> psms = new ArrayList<>();
    /**
     * Main modification site selection.
     */
    private boolean[] mainSelection;
    /**
     * Secondary modification site selection.
     */
    private boolean[] secondarySelection;
    /**
     * Modification confidence tooltip map, key: modification confidence type,
     * element: modification confidence as a string.
     */
    private HashMap<Integer, String> ptmConfidenceTooltipMap;

    /**
     * Constructor.
     *
     * @param peptideShakerGUI the main GUI
     * @param peptideKey the peptide key of the investigated peptide
     * @param modification the modification investigated
     */
    public ModificationSiteInferenceDialog(PeptideShakerGUI peptideShakerGUI, long peptideKey, Modification modification) {
        super(peptideShakerGUI, true);

        this.peptideShakerGUI = peptideShakerGUI;
        this.ptm = modification;
        double ptmMass = modification.getMass();

        Identification identification = peptideShakerGUI.getIdentification();
        peptideMatch = identification.getPeptideMatch(peptideKey);
        Peptide peptide = peptideMatch.getPeptide();

        mainSelection = new boolean[peptide.getSequence().length()];
        secondarySelection = new boolean[peptide.getSequence().length()];

        for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

            Modification tempMod = modificationFactory.getModification(modificationMatch.getModification());

            if (tempMod.getMass() == ptmMass) {

                int site = modificationMatch.getSite();

                if (modificationMatch.getConfident()) {

                    mainSelection[site - 1] = true;

                } else {

                    secondarySelection[site - 1] = true;

                }
            }
        }

        psms.addAll(
                identification.retrieveObjects(
                        Arrays.stream(peptideMatch.getSpectrumMatchesKeys()).boxed().collect(Collectors.toList()),
                        null,
                        false
                )
                        .stream()
                        .map(
                                object -> (SpectrumMatch) object
                        )
                        .collect(
                                Collectors.toCollection(ArrayList::new)
                        )
        );

        initComponents();

        setTableProperties();

        // set sequence
        updateSequenceLabel();

        // set the modification tooltip
        String tooltip = peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(
                (PeptideMatch) peptideShakerGUI.getIdentification().retrieveObject(peptideKey));
        sequenceLabel.setToolTipText(tooltip);

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);

    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        ptmSiteTableScrollPane.getViewport().setOpaque(false);
        ptmsTableScrollPane.getViewport().setOpaque(false);
        ptmSiteTable.getTableHeader().setReorderingAllowed(false);
        ptmsTable.getTableHeader().setReorderingAllowed(false);

        // centrally align the column headers 
        TableCellRenderer renderer = ptmSiteTable.getTableHeader().getDefaultRenderer();
        JLabel label = (JLabel) renderer;
        label.setHorizontalAlignment(JLabel.CENTER);

        // remove the column header in the ptm table
        ptmsTableScrollPane.setColumnHeaderView(null);

        // cell renderers
        ptmSiteTable.getColumn("").setCellRenderer(new AlignedTableCellRenderer(SwingConstants.CENTER, Color.LIGHT_GRAY));

        for (int i = 1; i < ptmSiteTable.getColumnCount(); i++) {
            ptmSiteTable.getColumn(ptmSiteTable.getColumnName(i)).setCellRenderer(new NimbusCheckBoxRenderer());
        }

        ptmsTable.getColumn("").setCellRenderer(new AlignedTableCellRenderer(SwingConstants.CENTER, Color.LIGHT_GRAY));

        ptmSiteTable.getColumn("").setMinWidth(35);
        ptmSiteTable.getColumn("").setMaxWidth(35);
        ptmsTable.getColumn("").setMinWidth(35);
        ptmsTable.getColumn("").setMaxWidth(35);

        // set up the PTM confidence color map
        HashMap<Integer, Color> ptmConfidenceColorMap = new HashMap<>();
        ptmConfidenceColorMap.put(ModificationScoring.NOT_FOUND, Color.lightGray);
        ptmConfidenceColorMap.put(ModificationScoring.RANDOM, Color.RED);
        ptmConfidenceColorMap.put(ModificationScoring.DOUBTFUL, Color.ORANGE);
        ptmConfidenceColorMap.put(ModificationScoring.CONFIDENT, Color.YELLOW);
        ptmConfidenceColorMap.put(ModificationScoring.VERY_CONFIDENT, peptideShakerGUI.getSparklineColor());

        // set up the PTM confidence tooltip map
        ptmConfidenceTooltipMap = new HashMap<>();
        ptmConfidenceTooltipMap.put(ModificationScoring.NOT_FOUND, "Not Found");
        ptmConfidenceTooltipMap.put(ModificationScoring.RANDOM, "Random Assignment");
        ptmConfidenceTooltipMap.put(ModificationScoring.DOUBTFUL, "Doubtful Assignment");
        ptmConfidenceTooltipMap.put(ModificationScoring.CONFIDENT, "Confident Assignment");
        ptmConfidenceTooltipMap.put(ModificationScoring.VERY_CONFIDENT, "Very Confident Assignment");

        for (int i = 1; i < ptmsTable.getColumnCount(); i++) {
            ptmsTable.getColumn(ptmsTable.getColumnName(i)).setCellRenderer(
                    new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), ptmConfidenceColorMap, ptmConfidenceTooltipMap));
        }
    }

    /**
     * Updates the sequence label based on the selection in the table.
     */
    private void updateSequenceLabel() {

        DisplayParameters displayParameters = peptideShakerGUI.getDisplayParameters();
        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceProvider sequenceProvider = peptideShakerGUI.getSequenceProvider();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        Peptide peptide = peptideMatch.getPeptide();

        String[] allFixedModifications = peptide.getFixedModifications(
                modificationParameters,
                sequenceProvider,
                modificationSequenceMatchingParameters
        );
        String[] displayedFixedModifications = DisplayFeaturesGenerator.getDisplayedModifications(
                allFixedModifications,
                displayParameters.getDisplayedModifications()
        );

        String[] confidentLocations = DisplayFeaturesGenerator.getFilteredConfidentModificationsSites(
                peptide,
                displayParameters.getDisplayedModifications()
        );
        String[] ambiguousLocations = DisplayFeaturesGenerator.getFilteredAmbiguousModifications(peptide, displayParameters.getDisplayedModifications());

        String modName = ptm.getName();

        for (int i = 0; i < mainSelection.length; i++) {

            int aa = i + 1;

            if (mainSelection[i]) {

                confidentLocations[aa] = modName;

            } else if (confidentLocations[aa].equals(modName)) {

                confidentLocations[aa] = null;

            }

            if (secondarySelection[i]) {

                ambiguousLocations[aa] = modName;

            } else if (ambiguousLocations[aa].equals(modName)) {

                ambiguousLocations[aa] = null;

            }
        }

        String taggedModifiedSequence = PeptideUtils.getTaggedModifiedSequence(
                peptide,
                modificationParameters,
                allFixedModifications,
                peptide.getIndexedVariableModifications(),
                confidentLocations,
                ambiguousLocations,
                null,
                displayedFixedModifications,
                true,
                true,
                true
        );
        sequenceLabel.setText(taggedModifiedSequence);

    }

    /**
     * Table model for the PTM site selection table.
     */
    private class SiteSelectionTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return 2;
        }

        @Override
        public int getColumnCount() {

            return peptideMatch.getPeptide().getSequence().length() + 1;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "";
                default:
                    return "" + peptideMatch.getPeptide().getSequence().charAt(column - 1) + column;
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            if (column == 0) {
                if (row == 0) {
                    return "S1";
                } else {
                    return "S2";
                }
            } else {
                if (row == 0) {
                    return mainSelection[column - 1];
                } else if (row == 1) {
                    return secondarySelection[column - 1];
                } else {
                    return null;
                }
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return String.class;
            } else {
                return Boolean.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 0;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {

            if (row == 0) {
                mainSelection[column - 1] = !mainSelection[column - 1];
                if (mainSelection[column - 1] && secondarySelection[column - 1]) {
                    secondarySelection[column - 1] = false;
                }
                updateSequenceLabel();
            } else if (row == 1) {
                secondarySelection[column - 1] = !secondarySelection[column - 1];
                if (mainSelection[column - 1] && secondarySelection[column - 1]) {
                    mainSelection[column - 1] = false;
                }
                updateSequenceLabel();
            }

            fireTableDataChanged();
        }
    }

    /**
     * Table model for the PTM table.
     */
    private class PtmTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return psms.size();
        }

        @Override
        public int getColumnCount() {
            return peptideMatch.getPeptide().getSequence().length() + 1;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "";
                default:
                    return "" + peptideMatch.getPeptide().getSequence().charAt(column - 1) + column;
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                if (column == 0) {
                    return row + 1;
                } else {
                    int psmNumber = row;
                    PSModificationScores psmScores = (PSModificationScores) psms.get(psmNumber).getUrParam(new PSModificationScores());
                    if (psmScores != null) {
                        ModificationScoring psmScoring = psmScores.getModificationScoring(ptm.getName());
                        if (psmScoring != null) {
                            int site = column;
                            if (psmScoring.getConfidentPtmLocations().contains(site)) {
                                return psmScoring.getLocalizationConfidence(site);
                            } else {
                                return ModificationScoring.NOT_FOUND;
                            }
                        }
                    }
                    return ModificationScoring.RANDOM;
                }

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                return "";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return Integer.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
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

        backgroundPanel = new javax.swing.JPanel();
        peptidePanel = new javax.swing.JPanel();
        sequenceLabel = new javax.swing.JLabel();
        ptmSitePanel = new javax.swing.JPanel();
        ptmSiteTableScrollPane = new javax.swing.JScrollPane();
        ptmSiteTable = new javax.swing.JTable();
        ptmsTableScrollPane = new javax.swing.JScrollPane();
        ptmsTable = new javax.swing.JTable();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        openDialogHelpJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PTM Site Assignment");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        peptidePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide"));
        peptidePanel.setOpaque(false);

        sequenceLabel.setText("Peptide Sequence");

        javax.swing.GroupLayout peptidePanelLayout = new javax.swing.GroupLayout(peptidePanel);
        peptidePanel.setLayout(peptidePanelLayout);
        peptidePanelLayout.setHorizontalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidePanelLayout.createSequentialGroup()
                .addGap(183, 183, 183)
                .addComponent(sequenceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                .addGap(225, 225, 225))
        );
        peptidePanelLayout.setVerticalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sequenceLabel)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        ptmSitePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Potential Modification Sites"));
        ptmSitePanel.setOpaque(false);

        ptmSiteTableScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        ptmSiteTable.setModel(new SiteSelectionTable());
        ptmSiteTable.setFillsViewportHeight(true);
        ptmSiteTable.setOpaque(false);
        ptmSiteTableScrollPane.setViewportView(ptmSiteTable);

        ptmsTableScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        ptmsTable.setModel(new PtmTable());
        ptmsTable.setOpaque(false);
        ptmsTableScrollPane.setViewportView(ptmsTable);

        javax.swing.GroupLayout ptmSitePanelLayout = new javax.swing.GroupLayout(ptmSitePanel);
        ptmSitePanel.setLayout(ptmSitePanelLayout);
        ptmSitePanelLayout.setHorizontalGroup(
            ptmSitePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ptmSitePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ptmSitePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(ptmsTableScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE)
                    .addComponent(ptmSiteTableScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE))
                .addContainerGap())
        );
        ptmSitePanelLayout.setVerticalGroup(
            ptmSitePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ptmSitePanelLayout.createSequentialGroup()
                .addComponent(ptmSiteTableScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(ptmsTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
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
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(ptmSitePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(peptidePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 510, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(openDialogHelpJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(okButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
     * Closes the dialog without saving.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Updates the data and then closes the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        boolean changed = false;

        if (changed) {

            try {
                // save changes in the peptide match
                Identification identification = peptideShakerGUI.getIdentification();

                // update protein level PTM scoring
                ModificationLocalizationScorer ptmScorer = new ModificationLocalizationScorer();
                SequenceProvider sequenceProvider = peptideShakerGUI.getSequenceProvider();
                IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();

                identification.getProteinMatches(peptideMatch.getKey())
                        .stream()
                        .map(
                                key -> identification.getProteinMatch(key)
                        )
                        .forEach(
                                proteinMatch -> ptmScorer.scorePTMs(
                                        identification,
                                        proteinMatch,
                                        identificationParameters,
                                        false,
                                        modificationFactory,
                                        sequenceProvider,
                                        null
                                )
                        );

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PtmSiteInferenceDialog.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PTM Site Assignment - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_openDialogHelpJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton okButton;
    private javax.swing.JButton openDialogHelpJButton;
    private javax.swing.JPanel peptidePanel;
    private javax.swing.JPanel ptmSitePanel;
    private javax.swing.JTable ptmSiteTable;
    private javax.swing.JScrollPane ptmSiteTableScrollPane;
    private javax.swing.JTable ptmsTable;
    private javax.swing.JScrollPane ptmsTableScrollPane;
    private javax.swing.JLabel sequenceLabel;
    // End of variables declaration//GEN-END:variables
}
