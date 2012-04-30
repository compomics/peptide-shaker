package eu.isas.peptideshaker.gui.tabpanels;

import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.tablemodels.PeptideFractionTableModel;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Displays information about which fractions the peptides and proteins were
 * detected in.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideFractionsPanel extends javax.swing.JPanel {

    /**
     * A reference to the main PeptideShakerGUI.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * A list of the peptides in the peptide table.
     */
    private ArrayList<String> peptideKeys = new ArrayList<String>();

    /**
     * Creates a new ProteinFractionsPanel.
     *
     * @param peptideShakerGUI
     */
    public PeptideFractionsPanel(PeptideShakerGUI peptideShakerGUI) {
        initComponents();
        this.peptideShakerGUI = peptideShakerGUI;
        setupGui();
        setPeptideTableProperties();
    }

    /**
     * Set up the GUI.
     */
    private void setupGui() {

        // set main table properties
        peptideTable.getTableHeader().setReorderingAllowed(false);

        peptideTable.setAutoCreateRowSorter(true);

        // make sure that the scroll panes are see-through
        peptideTableScrollPane.getViewport().setOpaque(false);

        // set up the table header tooltips
        setUpTableHeaderToolTips();
    }

    /**
     * Set up the table header tooltips.
     */
    private void setUpTableHeaderToolTips() {
        // @TODO: implement me!
    }

    /**
     * Set up the properties of the peptide table.
     */
    private void setPeptideTableProperties() {

        // the index column
        peptideTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMinWidth(50);

        peptideTable.getColumn("Confidence").setMaxWidth(90);
        peptideTable.getColumn("Confidence").setMinWidth(90);

        // the validated column
        peptideTable.getColumn("").setMaxWidth(30);
        peptideTable.getColumn("").setMinWidth(30);

        peptideTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());

        peptideTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        for (int i = 2; i < peptideTable.getColumnCount() - 2; i++) {
            if (peptideShakerGUI.showSparklines()) {
                peptideTable.getColumn(peptideTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumberAndChart(
                        true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            } else {
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }
    }

    /**
     * Display the results.
     */
    public void displayResults() {

        // update the table model
        peptideKeys = peptideShakerGUI.getIdentification().getPeptideIdentification();
        PeptideFractionTableModel peptideTableModel = new PeptideFractionTableModel(peptideShakerGUI, peptideKeys);
        peptideTable.setModel(peptideTableModel);

        ((DefaultTableModel) peptideTable.getModel()).fireTableDataChanged();
        setPeptideTableProperties();
        showSparkLines(peptideShakerGUI.showSparklines());

        updateSelection();
        peptideTable.requestFocus();

        peptideShakerGUI.setUpdated(PeptideShakerGUI.PEPTIDE_FRACTIONS_TAB_INDEX, true);
        
        ((TitledBorder) peptidePanel.getBorder()).setTitle("Peptides (" + peptideTable.getRowCount() + ")");
        peptidePanel.repaint();
    }

    /**
     * Update the selection.
     */
    public void updatePeptideTable() {
        displayResults(); // @TODO: should this be handled differently?
    }

    /**
     * Update the selected protein and peptide.
     */
    public void updateSelection() {

        String peptideKey = peptideShakerGUI.getSelectedPeptideKey();

        if (!peptideKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            
            int peptideRow = getPeptideRow(peptideKey);

            if (peptideTable.getSelectedRow() != peptideRow && peptideRow != -1) {
                peptideTable.setRowSelectionInterval(peptideRow, peptideRow);
                peptideTable.scrollRectToVisible(peptideTable.getCellRect(peptideRow, 0, false));
                //peptideTableMouseReleased(null);
            }
        } else {
            peptideShakerGUI.resetSelectedItems();
        }
    }

    /**
     * Returns the row of a desired peptide.
     *
     * @param peptideKey the key of the peptide
     * @return the row of the desired peptide
     */
    private int getPeptideRow(String peptideKey) {
        int modelIndex = peptideKeys.indexOf(peptideKey);
        if (modelIndex >= 0) {
            return peptideTable.convertRowIndexToView(modelIndex);
        } else {
            return -1;
        }
    }

    /**
     * Clear all the data.
     */
    public void clearData() {

        peptideKeys.clear();

        DefaultTableModel peptideTableModel = (DefaultTableModel) peptideTable.getModel();
        peptideTableModel.getDataVector().removeAllElements();
        peptideTableModel.fireTableDataChanged();

        ((TitledBorder) peptidePanel.getBorder()).setTitle("Peptides");
        peptidePanel.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        peptidePanel = new javax.swing.JPanel();
        peptideTableScrollPane = new javax.swing.JScrollPane();
        peptideTable = new javax.swing.JTable();
        disclamierPanel = new javax.swing.JPanel();
        disclaimerLabel = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 255, 255));

        peptidePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        peptidePanel.setOpaque(false);

        peptideTable.setModel(new eu.isas.peptideshaker.gui.tablemodels.PeptideFractionTableModel());
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
            }
        });
        peptideTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideTableKeyReleased(evt);
            }
        });
        peptideTableScrollPane.setViewportView(peptideTable);

        javax.swing.GroupLayout peptidePanelLayout = new javax.swing.GroupLayout(peptidePanel);
        peptidePanel.setLayout(peptidePanelLayout);
        peptidePanelLayout.setHorizontalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideTableScrollPane)
                .addContainerGap())
        );
        peptidePanelLayout.setVerticalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 433, Short.MAX_VALUE)
                .addContainerGap())
        );

        disclamierPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Disclaimer"));
        disclamierPanel.setOpaque(false);

        disclaimerLabel.setText("<html>\nThe values above are estimations of the confidence of a peptide if found in a fraction alone in the context of the whole analysis.<br>\n<i>These are <u><b>not</b></u> equal the confidence in the peptide identifications when processing the fractions independently!</i><br><br>\nIndependant fractions (like different donors, measurements) or replicates should be processed separately.<br>\nTo ensure comparable fractions, verify that the PSM PEP against score plots are similar for all the fractions in the <i>Validation</i> tab.\n</html>");

        javax.swing.GroupLayout disclamierPanelLayout = new javax.swing.GroupLayout(disclamierPanel);
        disclamierPanel.setLayout(disclamierPanelLayout);
        disclamierPanelLayout.setHorizontalGroup(
            disclamierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(disclamierPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(disclaimerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(290, Short.MAX_VALUE))
        );
        disclamierPanelLayout.setVerticalGroup(
            disclamierPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(disclamierPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(disclaimerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peptidePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(disclamierPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disclamierPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Remember the selection.
     *
     * @param evt
     */
    private void peptideTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseReleased
        // remember the selection
        newItemSelection();
    }//GEN-LAST:event_peptideTableMouseReleased

    /**
     * Remember the selection.
     *
     * @param evt
     */
    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased
        // remember the selection
        newItemSelection();
    }//GEN-LAST:event_peptideTableKeyReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel disclaimerLabel;
    private javax.swing.JPanel disclamierPanel;
    private javax.swing.JPanel peptidePanel;
    private javax.swing.JTable peptideTable;
    private javax.swing.JScrollPane peptideTableScrollPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        for (int i = 2; i < peptideTable.getColumnCount() - 1; i++) {
            if (peptideShakerGUI.showSparklines()) {
                peptideTable.getColumn(peptideTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumberAndChart(
                        true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            } else {
                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn(peptideTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }

        peptideTable.revalidate();
        peptideTable.repaint();
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected protein,
     * peptide and psm.
     */
    public void newItemSelection() {

        String proteinKey = PeptideShakerGUI.NO_SELECTION;
        String peptideKey = PeptideShakerGUI.NO_SELECTION;
        String psmKey = PeptideShakerGUI.NO_SELECTION;

        if (peptideTable.getSelectedRow() != -1) {
            peptideKey = peptideKeys.get(peptideTable.convertRowIndexToModel(peptideTable.getSelectedRow()));
        }

        peptideShakerGUI.setSelectedItems(proteinKey, peptideKey, psmKey);
    }

    /**
     * Returns a list of keys of the displayed peptides.
     *
     * @return a list of keys of the displayed peptides
     */
    public ArrayList<String> getDisplayedPeptides() {
        return peptideKeys;
    }
}
