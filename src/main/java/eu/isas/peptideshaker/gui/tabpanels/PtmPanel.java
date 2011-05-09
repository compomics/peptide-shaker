package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.SpectrumAnnotator.SpectrumAnnotationMap;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.events.RescalingEvent;
import com.compomics.util.gui.interfaces.SpectrumPanelListener;
import com.compomics.util.gui.spectrum.DefaultSpectrumAnnotation;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The PTM tab.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PtmPanel extends javax.swing.JPanel {

    /**
     * A hashmap of both the linked spectra.
     */
    private HashMap<Integer, SpectrumPanel> linkedSpectrumPanels;
    /**
     * The spectrum annotator
     */
    private SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();
    /**
     * The current spectrum annotations.
     */
    private Vector<DefaultSpectrumAnnotation> currentAnnotations;
    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * Map of all peptide keys indexed by their modification status
     */
    private HashMap<String, ArrayList<String>> peptideMap = new HashMap<String, ArrayList<String>>();
    /**
     * The modification name for no modification
     */
    private final String NO_MODIFICATION = "No modification";
    /**
     * The displayed identification
     */
    private Identification identification;
    /**
     * The keys of the peptides currently displayed
     */
    private ArrayList<String> displayedPeptides = new ArrayList<String>();
    /**
     * The keys of the related peptides currently displayed
     */
    private ArrayList<String> relatedPeptides = new ArrayList<String>();
    /**
     * The keys of the modified peptide PSMs currently displayed
     */
    private ArrayList<String> displayedModifiedPsms = new ArrayList<String>();
    /**
     * The keys of the related peptide PSMs currently displayed
     */
    private ArrayList<String> displayedRelatedPsms = new ArrayList<String>();

    /**
     * Creates a new PTM tab.
     *
     * @param peptideShakerGUI the PeptideShaker parent frame
     */
    public PtmPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();

        peptidesTable.getColumn(" ").setMaxWidth(50);
        relatedPeptidesTable.getColumn(" ").setMaxWidth(50);
        psmsModifiedPeptidesTable.getColumn(" ").setMaxWidth(50);
        psmsRelatedPeptidesTable.getColumn(" ").setMaxWidth(50);

        relatedPeptidesTableJScrollPane.getViewport().setOpaque(false);
        psmsModifiedTableJScrollPane.getViewport().setOpaque(false);
        peptidesTableJScrollPane.getViewport().setOpaque(false);
        psmsRelatedPeptidesTableJScrollPane.getViewport().setOpaque(false);

        peptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        peptidesTable.getColumn("Confidence [%]").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Confidence [%]").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        relatedPeptidesTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        relatedPeptidesTable.getColumn("Confidence [%]").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Confidence [%]").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bottomSplitPane = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        psmSplitPane = new javax.swing.JSplitPane();
        jPanel7 = new javax.swing.JPanel();
        psmsModifiedTableJScrollPane = new javax.swing.JScrollPane();
        psmsModifiedPeptidesTable = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        psmsRelatedPeptidesTableJScrollPane = new javax.swing.JScrollPane();
        psmsRelatedPeptidesTable = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        modificationsList = new javax.swing.JList();
        peptideTablesJSplitPane = new javax.swing.JSplitPane();
        jPanel4 = new javax.swing.JPanel();
        peptidesTableJScrollPane = new javax.swing.JScrollPane();
        peptidesTable = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        relatedPeptidesTableJScrollPane = new javax.swing.JScrollPane();
        relatedPeptidesTable = new javax.swing.JTable();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        bottomSplitPane.setBorder(null);
        bottomSplitPane.setDividerLocation(250);
        bottomSplitPane.setDividerSize(0);
        bottomSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        bottomSplitPane.setResizeWeight(1.0);
        bottomSplitPane.setOpaque(false);

        jSplitPane2.setBorder(null);
        jSplitPane2.setDividerSize(0);
        jSplitPane2.setResizeWeight(0.5);
        jSplitPane2.setOpaque(false);

        psmSplitPane.setBorder(null);
        psmSplitPane.setDividerLocation(120);
        psmSplitPane.setDividerSize(0);
        psmSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        psmSplitPane.setResizeWeight(0.5);
        psmSplitPane.setOpaque(false);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("PSMs - Selected Peptides"));
        jPanel7.setOpaque(false);

        psmsModifiedTableJScrollPane.setOpaque(false);

        psmsModifiedPeptidesTable.setModel(new ModifiedPeptdiesPsmsTable());
        psmsModifiedPeptidesTable.setOpaque(false);
        psmsModifiedPeptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        psmsModifiedPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                psmsModifiedPeptidesTableMouseClicked(evt);
            }
        });
        psmsModifiedPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmsModifiedPeptidesTableKeyReleased(evt);
            }
        });
        psmsModifiedTableJScrollPane.setViewportView(psmsModifiedPeptidesTable);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 524, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmSplitPane.setLeftComponent(jPanel7);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("PSMs - Related Peptides"));
        jPanel3.setOpaque(false);

        psmsRelatedPeptidesTableJScrollPane.setOpaque(false);

        psmsRelatedPeptidesTable.setModel(new RelatedPeptidesPsmsTable());
        psmsRelatedPeptidesTable.setOpaque(false);
        psmsRelatedPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                psmsRelatedPeptidesTableMouseClicked(evt);
            }
        });
        psmsRelatedPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmsRelatedPeptidesTableKeyReleased(evt);
            }
        });
        psmsRelatedPeptidesTableJScrollPane.setViewportView(psmsRelatedPeptidesTable);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmsRelatedPeptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 524, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(psmsRelatedPeptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmSplitPane.setRightComponent(jPanel3);

        jSplitPane2.setLeftComponent(psmSplitPane);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum"));
        jPanel5.setOpaque(false);

        spectrumJPanel.setOpaque(false);
        spectrumJPanel.setLayout(new javax.swing.BoxLayout(spectrumJPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
        );

        jSplitPane2.setRightComponent(jPanel5);

        bottomSplitPane.setTopComponent(jSplitPane2);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("PTM Location"));
        jPanel6.setOpaque(false);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1050, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 7, Short.MAX_VALUE)
        );

        bottomSplitPane.setRightComponent(jPanel6);

        jPanel8.setOpaque(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Variable Modifications"));
        jPanel1.setOpaque(false);

        modificationsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                modificationsListMouseClicked(evt);
            }
        });
        modificationsList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                modificationsListKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(modificationsList);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setBorder(null);
        peptideTablesJSplitPane.setDividerLocation(170);
        peptideTablesJSplitPane.setDividerSize(0);
        peptideTablesJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptideTablesJSplitPane.setResizeWeight(0.5);
        peptideTablesJSplitPane.setOpaque(false);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Peptides"));
        jPanel4.setOpaque(false);

        peptidesTableJScrollPane.setOpaque(false);

        peptidesTable.setModel(new PeptideTable()
        );
        peptidesTable.setOpaque(false);
        peptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        peptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                peptidesTableMouseClicked(evt);
            }
        });
        peptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptidesTableKeyReleased(evt);
            }
        });
        peptidesTableJScrollPane.setViewportView(peptidesTable);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setLeftComponent(jPanel4);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Related Peptides"));
        jPanel2.setOpaque(false);

        relatedPeptidesTableJScrollPane.setOpaque(false);

        relatedPeptidesTable.setModel(new RelatedPeptidesTable());
        relatedPeptidesTable.setOpaque(false);
        relatedPeptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        relatedPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                relatedPeptidesTableMouseClicked(evt);
            }
        });
        relatedPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                relatedPeptidesTableKeyReleased(evt);
            }
        });
        relatedPeptidesTableJScrollPane.setViewportView(relatedPeptidesTable);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedPeptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedPeptidesTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideTablesJSplitPane.setRightComponent(jPanel2);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptideTablesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 826, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(peptideTablesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(bottomSplitPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1062, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bottomSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the peptide table.
     *
     * @param evt
     */
    private void modificationsListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationsListMouseClicked
        updatePeptideTable();
    }//GEN-LAST:event_modificationsListMouseClicked

    /**
     * Update the related peptides and modified peptide psms tables.
     *
     * @param evt
     */
    private void peptidesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseClicked
        updateRelatedPeptidesTable();
        updateModifiedPeptidesPsmTable();
    }//GEN-LAST:event_peptidesTableMouseClicked

    /**
     * Update the related peptides psm table.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseClicked
        updateRelatedPeptidesPsmTable();
    }//GEN-LAST:event_relatedPeptidesTableMouseClicked

    /**
     * Update the peptide table.
     *
     * @param evt
     */
    private void modificationsListKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_modificationsListKeyReleased
        updatePeptideTable();
    }//GEN-LAST:event_modificationsListKeyReleased

    /**
     * @see #peptidesTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void peptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidesTableKeyReleased
        peptidesTableMouseClicked(null);
    }//GEN-LAST:event_peptidesTableKeyReleased

    /**
     * @see #relatedPeptidesTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void relatedPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_relatedPeptidesTableKeyReleased
        relatedPeptidesTableMouseClicked(null);
    }//GEN-LAST:event_relatedPeptidesTableKeyReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void psmsModifiedPeptidesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmsModifiedPeptidesTableMouseClicked
        updateSpectra();
    }//GEN-LAST:event_psmsModifiedPeptidesTableMouseClicked

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void psmsModifiedPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmsModifiedPeptidesTableKeyReleased
        updateSpectra();
    }//GEN-LAST:event_psmsModifiedPeptidesTableKeyReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void psmsRelatedPeptidesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmsRelatedPeptidesTableMouseClicked
        updateSpectra();
    }//GEN-LAST:event_psmsRelatedPeptidesTableMouseClicked

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void psmsRelatedPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmsRelatedPeptidesTableKeyReleased
        updateSpectra();
    }//GEN-LAST:event_psmsRelatedPeptidesTableKeyReleased

    /**
     * Resize the panels after frame resizing.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // update the splitters
        peptideTablesJSplitPane.setDividerLocation(peptideTablesJSplitPane.getHeight() / 2);
        bottomSplitPane.setDividerLocation(bottomSplitPane.getHeight() / 5 * 4);

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                psmSplitPane.setDividerLocation(psmSplitPane.getHeight() / 2);
                updateUI();
            }
        });

    }//GEN-LAST:event_formComponentResized
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSplitPane bottomSplitPane;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JList modificationsList;
    private javax.swing.JSplitPane peptideTablesJSplitPane;
    private javax.swing.JTable peptidesTable;
    private javax.swing.JScrollPane peptidesTableJScrollPane;
    private javax.swing.JSplitPane psmSplitPane;
    private javax.swing.JTable psmsModifiedPeptidesTable;
    private javax.swing.JScrollPane psmsModifiedTableJScrollPane;
    private javax.swing.JTable psmsRelatedPeptidesTable;
    private javax.swing.JScrollPane psmsRelatedPeptidesTableJScrollPane;
    private javax.swing.JTable relatedPeptidesTable;
    private javax.swing.JScrollPane relatedPeptidesTableJScrollPane;
    private javax.swing.JPanel spectrumJPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays or hide sparklines in tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Confidence [%]").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Confidence [%]").getCellRenderer()).showNumbers(!showSparkLines);

        peptidesTable.revalidate();
        peptidesTable.repaint();

        relatedPeptidesTable.revalidate();
        relatedPeptidesTable.repaint();
    }

    /**
     * Creates the peptide map.
     */
    private void createPeptideMap() {

        String modificationName;
        boolean modified;
        ArrayList<String> accountedModifications;

        for (PeptideMatch peptideMatch : identification.getPeptideIdentification().values()) {

            modified = false;
            accountedModifications = new ArrayList<String>();

            for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {

                if (modificationMatch.isVariable()) {

                    modificationName = modificationMatch.getTheoreticPtm().getName();

                    if (!accountedModifications.contains(modificationName)) {

                        if (!peptideMap.containsKey(modificationName)) {
                            peptideMap.put(modificationName, new ArrayList<String>());
                        }

                        peptideMap.get(modificationName).add(peptideMatch.getKey());
                        modified = true;
                        accountedModifications.add(modificationName);
                    }
                }
            }

            if (!modified) {
                if (!peptideMap.containsKey(NO_MODIFICATION)) {
                    peptideMap.put(NO_MODIFICATION, new ArrayList<String>());
                }
                peptideMap.get(NO_MODIFICATION).add(peptideMatch.getKey());
            }
        }
    }

    /**
     * Displays the results.
     */
    public void displayResults() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        this.identification = peptideShakerGUI.getIdentification();
        createPeptideMap();
        String[] modifications = new String[peptideMap.size()];
        int cpt = 0;

        for (String modification : peptideMap.keySet()) {
            modifications[cpt] = modification;
            cpt++;
        }

        Arrays.sort(modifications);
        modificationsList.setListData(modifications);
        modificationsList.setSelectedIndex(0);
        updatePeptideTable();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Updates the peptide table.
     */
    private void updatePeptideTable() {

        HashMap<Double, ArrayList<String>> scoreToPeptideMap = new HashMap<Double, ArrayList<String>>();
        PeptideMatch peptideMatch;
        PSParameter probabilities = new PSParameter();
        double p;

        for (String peptideKey : peptideMap.get((String) modificationsList.getSelectedValue())) {

            peptideMatch = identification.getPeptideIdentification().get(peptideKey);

            if (!peptideMatch.isDecoy()) {

                probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);
                p = probabilities.getPeptideProbability();

                if (!scoreToPeptideMap.containsKey(p)) {
                    scoreToPeptideMap.put(p, new ArrayList<String>());
                }

                scoreToPeptideMap.get(p).add(peptideKey);
            }
        }

        ArrayList<Double> scores = new ArrayList<Double>(scoreToPeptideMap.keySet());
        Collections.sort(scores);
        displayedPeptides = new ArrayList<String>();

        for (double score : scores) {
            displayedPeptides.addAll(scoreToPeptideMap.get(score));
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                peptidesTable.revalidate();
                peptidesTable.repaint();
                peptidesTable.setRowSelectionInterval(0, 0);
                updateRelatedPeptidesTable();
                updateModifiedPeptidesPsmTable();
            }
        });
    }

    /**
     * Updates the related peptides table.
     */
    private void updateRelatedPeptidesTable() {

        HashMap<Double, ArrayList<String>> scoreToKeyMap = new HashMap<Double, ArrayList<String>>();
        PeptideMatch selectedPeptide = identification.getPeptideIdentification().get(displayedPeptides.get(peptidesTable.getSelectedRow()));
        String currentSequence, referenceSequence = selectedPeptide.getTheoreticPeptide().getSequence();
        PSParameter probabilities = new PSParameter();
        double p;

        for (PeptideMatch currentPeptide : identification.getPeptideIdentification().values()) {

            currentSequence = currentPeptide.getTheoreticPeptide().getSequence();

            if (currentSequence.contains(referenceSequence) || referenceSequence.contains(currentSequence)) {

                if (!currentPeptide.getKey().equals(selectedPeptide.getKey())) {

                    probabilities = (PSParameter) currentPeptide.getUrParam(probabilities);
                    p = probabilities.getPeptideProbability();

                    if (!scoreToKeyMap.containsKey(p)) {
                        scoreToKeyMap.put(p, new ArrayList<String>());
                    }

                    scoreToKeyMap.get(p).add(currentPeptide.getKey());
                }
            }
        }

        relatedPeptides = new ArrayList<String>();
        ArrayList<Double> scores = new ArrayList<Double>(scoreToKeyMap.keySet());
        Collections.sort(scores);

        for (Double score : scores) {
            relatedPeptides.addAll(scoreToKeyMap.get(score));
        }

        if (relatedPeptides.size() > 0) {
            relatedPeptidesTable.setRowSelectionInterval(0, 0);
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                relatedPeptidesTable.revalidate();
                relatedPeptidesTable.repaint();
                updateRelatedPeptidesPsmTable();
                updateSpectra();
            }
        });
    }

    /**
     * Update the modified peptides PSM table.
     */
    private void updateModifiedPeptidesPsmTable() {

        PeptideMatch peptideMatch = identification.getPeptideIdentification().get(displayedPeptides.get(peptidesTable.getSelectedRow()));

        HashMap<Double, ArrayList<String>> scoreMap = new HashMap<Double, ArrayList<String>>();
        PSParameter probabilities = new PSParameter();
        double p;

        for (SpectrumMatch psmMatch : peptideMatch.getSpectrumMatches().values()) {

            probabilities = (PSParameter) psmMatch.getUrParam(probabilities);
            p = probabilities.getPsmProbability();

            if (!scoreMap.containsKey(p)) {
                scoreMap.put(p, new ArrayList<String>());
            }

            scoreMap.get(p).add(psmMatch.getKey());
        }

        displayedModifiedPsms = new ArrayList<String>();

        for (Double score : scoreMap.keySet()) {
            displayedModifiedPsms.addAll(scoreMap.get(score));
        }

        if (psmsModifiedPeptidesTable.getRowCount() > 0) {
            psmsModifiedPeptidesTable.setRowSelectionInterval(0, 0);
            updateSpectra();
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                psmsModifiedPeptidesTable.revalidate();
                psmsModifiedPeptidesTable.repaint();
            }
        });
    }

    /**
     * Update the related peptides PSM table.
     */
    private void updateRelatedPeptidesPsmTable() {

        if (relatedPeptides.size() > 0) {

            PeptideMatch peptideMatch = identification.getPeptideIdentification().get(relatedPeptides.get(relatedPeptidesTable.getSelectedRow()));

            HashMap<Double, ArrayList<String>> scoreMap = new HashMap<Double, ArrayList<String>>();
            PSParameter probabilities = new PSParameter();
            double p;

            for (SpectrumMatch psmMatch : peptideMatch.getSpectrumMatches().values()) {

                probabilities = (PSParameter) psmMatch.getUrParam(probabilities);
                p = probabilities.getPsmProbability();

                if (!scoreMap.containsKey(p)) {
                    scoreMap.put(p, new ArrayList<String>());
                }

                scoreMap.get(p).add(psmMatch.getKey());
            }

            displayedRelatedPsms = new ArrayList<String>();

            for (Double score : scoreMap.keySet()) {
                displayedRelatedPsms.addAll(scoreMap.get(score));
            }
        } else {
            displayedRelatedPsms = new ArrayList<String>();
        }

        if (psmsRelatedPeptidesTable.getRowCount() > 0) {
            psmsRelatedPeptidesTable.setRowSelectionInterval(0, 0);
            updateSpectra();
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                psmsRelatedPeptidesTable.revalidate();
                psmsRelatedPeptidesTable.repaint();
            }
        });
    }

    /**
     * Update the spectra according to the currently selected psms.
     */
    private void updateSpectra() {

        linkedSpectrumPanels = new HashMap<Integer, SpectrumPanel>();
        spectrumJPanel.removeAll();
        spectrumJPanel.revalidate();
        spectrumJPanel.repaint();

        try {
            if (psmsModifiedPeptidesTable.getSelectedRow() != -1) {
                MSnSpectrum currentSpectrum = (MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(
                        2, (String) psmsModifiedPeptidesTable.getValueAt(psmsModifiedPeptidesTable.getSelectedRow(), 2));

                Precursor precursor = currentSpectrum.getPrecursor();
                SpectrumPanel spectrum = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                        precursor.getMz(), precursor.getCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrum.setBorder(null);

//                // get the spectrum annotations
//                String peptideKey = (String) peptidesTable.getValueAt(peptidesTable.getSelectedRow(), 2) + (String) peptidesTable.getValueAt(peptidesTable.getSelectedRow(), 3);
//                peptideKey = peptideTableMap.get(peptideKey);
//                Peptide currentPeptide = peptideShakerGUI.getIdentification().getPeptideIdentification().get(peptideKey).getTheoreticPeptide();
//                SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
//                        currentPeptide, currentSpectrum, peptideShakerGUI.getSearchParameters().getFragmentIonMZTolerance(),
//                        currentSpectrum.getIntensityLimit(peptideShakerGUI.getAnnotationPreferences().shallAnnotateMostIntensePeaks()));
//
//                // add the spectrum annotations
//                currentAnnotations = spectrumAnnotator.getSpectrumAnnotations(annotations);
//                //spectrum.setAnnotations(filterAnnotations(currentAnnotations));
//                spectrum.setAnnotations(currentAnnotations);

                linkedSpectrumPanels.put(new Integer(0), spectrum);

                spectrum.addSpectrumPanelListener(new SpectrumPanelListener() {

                    public void rescaled(RescalingEvent rescalingEvent) {
                        SpectrumPanel source = (SpectrumPanel) rescalingEvent.getSource();
                        double minMass = rescalingEvent.getMinMass();
                        double maxMass = rescalingEvent.getMaxMass();

                        Iterator<Integer> iterator = linkedSpectrumPanels.keySet().iterator();

                        while (iterator.hasNext()) {
                            SpectrumPanel currentSpectrumPanel = linkedSpectrumPanels.get(iterator.next());
                            if (currentSpectrumPanel != source) {
                                currentSpectrumPanel.rescale(minMass, maxMass, false);
                                currentSpectrumPanel.repaint();
                            }
                        }
                    }
                });

                spectrumJPanel.add(spectrum);
            }

            if (psmsRelatedPeptidesTable.getSelectedRow() != -1 && !displayedRelatedPsms.isEmpty()) {

                MSnSpectrum currentSpectrum = (MSnSpectrum) peptideShakerGUI.getSpectrumCollection().getSpectrum(
                        2, (String) psmsRelatedPeptidesTable.getValueAt(psmsRelatedPeptidesTable.getSelectedRow(), 2));

                Precursor precursor = currentSpectrum.getPrecursor();
                SpectrumPanel spectrum = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                        precursor.getMz(), precursor.getCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrum.setBorder(null);

//                // get the spectrum annotations
//                String peptideKey = (String) relatedPeptidesTable.getValueAt(relatedPeptidesTable.getSelectedRow(), 2) + (String) relatedPeptidesTable.getValueAt(relatedPeptidesTable.getSelectedRow(), 3);
//                peptideKey = peptideTableMap.get(peptideKey);
//                Peptide currentPeptide = peptideShakerGUI.getIdentification().getPeptideIdentification().get(peptideKey).getTheoreticPeptide();
//                SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
//                        currentPeptide, currentSpectrum, peptideShakerGUI.getSearchParameters().getFragmentIonMZTolerance(),
//                        currentSpectrum.getIntensityLimit(peptideShakerGUI.getAnnotationPreferences().shallAnnotateMostIntensePeaks()));
//
//                // add the spectrum annotations
//                currentAnnotations = spectrumAnnotator.getSpectrumAnnotations(annotations);
//                //spectrum.setAnnotations(filterAnnotations(currentAnnotations));
//                spectrum.setAnnotations(currentAnnotations);

                linkedSpectrumPanels.put(new Integer(1), spectrum);

                spectrum.addSpectrumPanelListener(new SpectrumPanelListener() {

                    public void rescaled(RescalingEvent rescalingEvent) {
                        SpectrumPanel source = (SpectrumPanel) rescalingEvent.getSource();
                        double minMass = rescalingEvent.getMinMass();
                        double maxMass = rescalingEvent.getMaxMass();

                        Iterator<Integer> iterator = linkedSpectrumPanels.keySet().iterator();

                        while (iterator.hasNext()) {
                            SpectrumPanel currentSpectrumPanel = linkedSpectrumPanels.get(iterator.next());
                            if (currentSpectrumPanel != source) {
                                currentSpectrumPanel.rescale(minMass, maxMass, false);
                                currentSpectrumPanel.repaint();
                            }
                        }
                    }
                });

                // make sure that the two spectra have the same x-axis range
                spectrum.rescale(0, spectrum.getMaxXAxisValue());

                spectrumJPanel.add(spectrum);
            }

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    spectrumJPanel.revalidate();
                    spectrumJPanel.repaint();
                }
            });

        } catch (MzMLUnmarshallerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Table model for the peptide table.
     */
    private class PeptideTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return displayedPeptides.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {

            if (column == 0) {
                return " ";
            } else if (column == 1) {
                return "Protein(s)";
            } else if (column == 2) {
                return "Peptide Sequence";
            } else if (column == 3) {
                return "Modification(s)";
            } else if (column == 4) {
                return "Score";
            } else if (column == 5) {
                return "Confidence [%]";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            if (column == 0) {
                return new Double(row + 1);
            } else if (column == 1) {
                String result = "";
                for (Protein protein : identification.getPeptideIdentification().get(displayedPeptides.get(row)).getTheoreticPeptide().getParentProteins()) {
                    result += protein.getAccession() + " ";
                }
                return result;
            } else if (column == 2) {
                return identification.getPeptideIdentification().get(displayedPeptides.get(row)).getTheoreticPeptide().getSequence();
            } else if (column == 3) {
                String result = "";
                for (ModificationMatch modificationMatch : identification.getPeptideIdentification().get(displayedPeptides.get(row)).getTheoreticPeptide().getModificationMatches()) {
                    if (modificationMatch.isVariable()) {
                        result += modificationMatch.getTheoreticPtm().getName() + " ";
                    }
                }

                if (result.length() == 0) {
                    result = null;
                }

                return result;
            } else if (column == 4) {
                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideIdentification().get(displayedPeptides.get(row)).getUrParam(probabilities);
                return probabilities.getPeptideScore();
            } else if (column == 5) {
                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideIdentification().get(displayedPeptides.get(row)).getUrParam(probabilities);
                return probabilities.getPeptideConfidence();
            } else {
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
            return false;
        }
    }

    /**
     * Table model for the related peptides table.
     */
    private class RelatedPeptidesTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return relatedPeptides.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return " ";
            } else if (column == 1) {
                return "Protein(s)";
            } else if (column == 2) {
                return "Peptide Sequence";
            } else if (column == 3) {
                return "Modification(s)";
            } else if (column == 4) {
                return "Score";
            } else if (column == 5) {
                return "Confidence [%]";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return new Double(row + 1);
            } else if (column == 1) {
                String result = "";
                for (Protein protein : identification.getPeptideIdentification().get(relatedPeptides.get(row)).getTheoreticPeptide().getParentProteins()) {
                    result += protein.getAccession() + " ";
                }
                return result;
            } else if (column == 2) {
                return identification.getPeptideIdentification().get(relatedPeptides.get(row)).getTheoreticPeptide().getSequence();
            } else if (column == 3) {
                String result = "";
                for (ModificationMatch modificationMatch : identification.getPeptideIdentification().get(relatedPeptides.get(row)).getTheoreticPeptide().getModificationMatches()) {
                    if (modificationMatch.isVariable()) {
                        result += modificationMatch.getTheoreticPtm().getName() + " ";
                    }
                }

                if (result.length() == 0) {
                    result = null;
                }

                return result;
            } else if (column == 4) {
                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideIdentification().get(relatedPeptides.get(row)).getUrParam(probabilities);
                return probabilities.getPeptideScore();
            } else if (column == 5) {
                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideIdentification().get(relatedPeptides.get(row)).getUrParam(probabilities);
                return probabilities.getPeptideConfidence();
            } else {
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
            return false;
        }
    }

    /**
     * Table model for the modified peptide PSMs table
     */
    private class ModifiedPeptdiesPsmsTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return displayedModifiedPsms.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return " ";
            } else if (column == 1) {
                return "Peptide Sequence";
            } else if (column == 2) {
                return "Spectrum";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return new Double(row + 1);
            } else if (column == 1) {
                return identification.getSpectrumIdentification().get(displayedModifiedPsms.get(row)).getBestAssumption().getPeptide().getSequence();
            } else {
                return displayedModifiedPsms.get(row);
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
            return false;
        }
    }

    /**
     * Table model for the related peptide PSMs table.
     */
    private class RelatedPeptidesPsmsTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return displayedRelatedPsms.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return " ";
            } else if (column == 1) {
                return "Peptide Sequence";
            } else if (column == 2) {
                return "Spectrum";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return new Double(row + 1);
            } else if (column == 1) {
                return identification.getSpectrumIdentification().get(displayedRelatedPsms.get(row)).getBestAssumption().getPeptide().getSequence();
            } else {
                return displayedRelatedPsms.get(row);
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
            return false;
        }
    }
}
