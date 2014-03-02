package eu.isas.peptideshaker.gui.protein_inference;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.preferences.GenePreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.tablemodels.ProteinTableModel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.data.Chromosome;
import no.uib.jsparklines.extra.ChromosomeTableCellRenderer;
import no.uib.jsparklines.extra.HtmlLinksRenderer;

/**
 * A simple dialog for showing the list of proteins a given peptide can map to.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
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
     * The gene factory.
     */
    private GeneFactory geneFactory = GeneFactory.getInstance();
    /**
     * The key of the peptide match of interest.
     */
    private String peptideMatchKey;
    /**
     * The retained proteins table column header tooltips.
     */
    private ArrayList<String> retainedProteinsTableToolTips;
    /**
     * The other proteins table column header tooltips.
     */
    private ArrayList<String> otherProteinsTableToolTips;

    /**
     * Create a new ProteinInferencePeptideLevelDialog.
     *
     * @param aPeptideShakerGUI the PeptideShakerGUI parent
     * @param modal modal or not modal
     * @param peptideMatchKey the peptide match key
     * @param proteinMatchKey the protein match key
     * @throws Exception
     */
    public ProteinInferencePeptideLevelDialog(PeptideShakerGUI aPeptideShakerGUI, boolean modal, String peptideMatchKey, String proteinMatchKey) throws Exception {

        super(aPeptideShakerGUI, modal);

        this.peptideMatchKey = peptideMatchKey;
        this.peptideShakerGUI = aPeptideShakerGUI;
        
        PeptideMatch peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideMatchKey);

        initComponents();

        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) peptideShakerGUI.getIdentification().getPeptideMatchParameter(peptideMatchKey, psParameter);
        protInferenceTypeCmb.setSelectedIndex(psParameter.getProteinInferenceClass());

        protInferenceTypeCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        // make sure that the scroll panes are see-through
        proteinsJScrollPane.getViewport().setOpaque(false);
        otherProteinsJScrollPane.getViewport().setOpaque(false);

        // set up the table properties
        otherProteinJTable.getTableHeader().setReorderingAllowed(false);
        otherProteinJTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));

        retainedProteinJTable.getTableHeader().setReorderingAllowed(false);
        retainedProteinJTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));

        // insert the values
        sequenceLabel.setText(peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideMatch, true, true, true));

        // set the modification tooltip
        String tooltip = peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(
                peptideShakerGUI.getIdentification().getPeptideMatch(peptideMatchKey).getTheoreticPeptide());
        sequenceLabel.setToolTipText(tooltip);

        ArrayList<String> possibleProteins = peptideMatch.getTheoreticPeptide().getParentProteins(PeptideShaker.MATCHING_TYPE, peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy());
        List<String> retainedProteins;

        if (proteinMatchKey != null) {
            retainedProteins = Arrays.asList(ProteinMatch.getAccessions(proteinMatchKey));
        } else {
            retainedProteins = new ArrayList<String>();
            for (String proteinKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {
                for (String protein : possibleProteins) {
                    if (!retainedProteins.contains(protein) && proteinKey.contains(protein)) {
                        retainedProteins.add(protein);
                        if (retainedProteins.size() == possibleProteins.size()) {
                            break;
                        }
                    }
                }
            }
        }

        int possibleCpt = 0, retainedCpt = 0;

        for (String protein : possibleProteins) {

            String description, geneName, proteinEvidenceLevel;
            Chromosome chromosome;

            try {
                description = sequenceFactory.getHeader(protein).getSimpleProteinDescription();
                geneName = sequenceFactory.getHeader(protein).getGeneName();
                proteinEvidenceLevel = sequenceFactory.getHeader(protein).getProteinEvidence();

                if (proteinEvidenceLevel != null) {
                    try {
                        Integer level = new Integer(proteinEvidenceLevel);
                        proteinEvidenceLevel = GenePreferences.getProteinEvidencAsString(level);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }

                String chromosomeNumber = geneFactory.getChromosomeForGeneName(geneName);
                chromosome = new Chromosome(chromosomeNumber);

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                description = "Error";
                geneName = "Error";
                proteinEvidenceLevel = "Error";
                chromosome = null;
            }

            if (retainedProteins.contains(protein)) {
                ((DefaultTableModel) retainedProteinJTable.getModel()).addRow(new Object[]{
                    (++retainedCpt),
                    peptideShakerGUI.getDisplayFeaturesGenerator().addDatabaseLink(protein),
                    description,
                    geneName,
                    chromosome,
                    proteinEvidenceLevel
                });
            } else {
                ((DefaultTableModel) otherProteinJTable.getModel()).addRow(new Object[]{
                    (++possibleCpt),
                    peptideShakerGUI.getDisplayFeaturesGenerator().addDatabaseLink(protein),
                    description,
                    geneName,
                    chromosome,
                    proteinEvidenceLevel
                });
            }
        }

        retainedProteinJTable.getColumn(" ").setMinWidth(50);
        retainedProteinJTable.getColumn(" ").setMaxWidth(50);
        retainedProteinJTable.getColumn("Gene").setMinWidth(90);
        retainedProteinJTable.getColumn("Gene").setMaxWidth(90);
        retainedProteinJTable.getColumn("Chr").setMinWidth(50);
        retainedProteinJTable.getColumn("Chr").setMaxWidth(50);
        retainedProteinJTable.getColumn("Evidence").setMinWidth(90);
        retainedProteinJTable.getColumn("Evidence").setMaxWidth(90);

        otherProteinJTable.getColumn(" ").setMinWidth(50);
        otherProteinJTable.getColumn(" ").setMaxWidth(50);
        otherProteinJTable.getColumn("Gene").setMinWidth(90);
        otherProteinJTable.getColumn("Gene").setMaxWidth(90);
        otherProteinJTable.getColumn("Chr").setMinWidth(50);
        otherProteinJTable.getColumn("Chr").setMaxWidth(50);
        otherProteinJTable.getColumn("Evidence").setMinWidth(90);
        otherProteinJTable.getColumn("Evidence").setMaxWidth(90);

        // set the preferred size of the accession column
        Integer width = ProteinTableModel.getPreferredAccessionColumnWidth(otherProteinJTable, otherProteinJTable.getColumn("Accession").getModelIndex(), 2, peptideShakerGUI.getMetrics().getMaxProteinKeyLength());
        if (width != null) {
            otherProteinJTable.getColumn("Accession").setMinWidth(width);
            otherProteinJTable.getColumn("Accession").setMaxWidth(width);
        } else {
            otherProteinJTable.getColumn("Accession").setMinWidth(15);
            otherProteinJTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
        }

        otherProteinJTable.getColumn("Chr").setCellRenderer(new ChromosomeTableCellRenderer());

        // set the preferred size of the accession column
        width = ProteinTableModel.getPreferredAccessionColumnWidth(retainedProteinJTable, retainedProteinJTable.getColumn("Accession").getModelIndex(), 2, peptideShakerGUI.getMetrics().getMaxProteinKeyLength());
        if (width != null) {
            retainedProteinJTable.getColumn("Accession").setMinWidth(width);
            retainedProteinJTable.getColumn("Accession").setMaxWidth(width);
        } else {
            retainedProteinJTable.getColumn("Accession").setMinWidth(15);
            retainedProteinJTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
        }

        retainedProteinJTable.getColumn("Chr").setCellRenderer(new ChromosomeTableCellRenderer());

        // set up the table header tooltips
        retainedProteinsTableToolTips = new ArrayList<String>();
        retainedProteinsTableToolTips.add(null);
        retainedProteinsTableToolTips.add("Protein Accession");
        retainedProteinsTableToolTips.add("Protein Description");
        retainedProteinsTableToolTips.add("Gene Name");
        retainedProteinsTableToolTips.add("Chromosome Number");
        retainedProteinsTableToolTips.add("Protein Evidence Level");

        otherProteinsTableToolTips = new ArrayList<String>();
        otherProteinsTableToolTips.add(null);
        otherProteinsTableToolTips.add("Protein Accession");
        otherProteinsTableToolTips.add("Protein Description");
        otherProteinsTableToolTips.add("Gene Name");
        retainedProteinsTableToolTips.add("Chromosome Number");
        otherProteinsTableToolTips.add("Protein Evidence Level");

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
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
        proteinsPanel = new javax.swing.JPanel();
        proteinsJScrollPane = new javax.swing.JScrollPane();
        otherProteinJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return (String) otherProteinsTableToolTips.get(realIndex);
                    }
                };
            }
        };
        peptidesPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        protInferenceTypeCmb = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        sequenceLabel = new javax.swing.JLabel();
        cancelButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        otherProteinsJScrollPane = new javax.swing.JScrollPane();
        retainedProteinJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return (String) retainedProteinsTableToolTips.get(realIndex);
                    }
                };
            }
        };
        okButton = new javax.swing.JButton();
        helpJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Protein Inference - Peptide Level");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        proteinsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Other Proteins"));
        proteinsPanel.setOpaque(false);

        proteinsJScrollPane.setOpaque(false);

        otherProteinJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Description", "Gene", "Chr", "Evidence"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
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
                .addComponent(proteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide"));
        peptidesPanel.setOpaque(false);

        jLabel1.setText("Type:");

        protInferenceTypeCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Unique Protein", "Related Proteins", "Related and Unrelated Proteins", "Unrelated Proteins" }));
        protInferenceTypeCmb.setMinimumSize(new java.awt.Dimension(112, 18));

        jLabel2.setText("Sequence:");

        sequenceLabel.setText("peptide sequence");

        javax.swing.GroupLayout peptidesPanelLayout = new javax.swing.GroupLayout(peptidesPanel);
        peptidesPanel.setLayout(peptidesPanelLayout);
        peptidesPanelLayout.setHorizontalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(protInferenceTypeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(sequenceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesPanelLayout.setVerticalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(protInferenceTypeCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(sequenceLabel)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Retained Proteins"));
        jPanel1.setOpaque(false);

        retainedProteinJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Description", "Gene", "Chr", "Evidence"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
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
        retainedProteinJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                retainedProteinJTableMouseMoved(evt);
            }
        });
        otherProteinsJScrollPane.setViewportView(retainedProteinJTable);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(otherProteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 725, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(otherProteinsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE)
                .addContainerGap())
        );

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
        helpJButton.setFocusable(false);
        helpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        helpJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
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
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(peptidesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(helpJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 594, Short.MAX_VALUE)
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
                .addComponent(peptidesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proteinsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
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
     * Closes the dialog.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Try to open the protein's web link (if available).
     *
     * @param evt
     */
    private void otherProteinJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_otherProteinJTableMouseReleased
        int row = otherProteinJTable.rowAtPoint(evt.getPoint());
        int column = otherProteinJTable.columnAtPoint(evt.getPoint());

        if (row != -1) {

            if (column == 1) {

                // open protein link in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) otherProteinJTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                    String link = (String) otherProteinJTable.getValueAt(row, column);
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        }
    }//GEN-LAST:event_otherProteinJTableMouseReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void otherProteinJTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_otherProteinJTableMouseMoved
        int row = otherProteinJTable.rowAtPoint(evt.getPoint());
        int column = otherProteinJTable.columnAtPoint(evt.getPoint());

        otherProteinJTable.setToolTipText(null);

        if (otherProteinJTable.getValueAt(row, column) != null) {
            if (column == otherProteinJTable.getColumn("Accession").getModelIndex()) {
                String tempValue = (String) otherProteinJTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        } else if (column == otherProteinJTable.getColumn("Description").getModelIndex() && otherProteinJTable.getValueAt(row, column) != null) {
            if (GuiUtilities.getPreferredWidthOfCell(otherProteinJTable, row, column) > otherProteinJTable.getColumn("Description").getWidth()) {
                otherProteinJTable.setToolTipText("" + otherProteinJTable.getValueAt(row, column));
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
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

    /**
     * Opens the link in a web browser.
     *
     * @param evt
     */
    private void retainedProteinJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_retainedProteinJTableMouseReleased
        int row = retainedProteinJTable.rowAtPoint(evt.getPoint());
        int column = retainedProteinJTable.columnAtPoint(evt.getPoint());

        if (row != -1) {

            if (column == 1) {

                // open protein link in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) retainedProteinJTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                    String link = (String) retainedProteinJTable.getValueAt(row, column);
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        }
    }//GEN-LAST:event_retainedProteinJTableMouseReleased

    private void retainedProteinJTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_retainedProteinJTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_retainedProteinJTableMouseExited

    /**
     * Update the peptide level protein inference type and close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        PSParameter psParameter = new PSParameter();
        try {
            psParameter = (PSParameter) peptideShakerGUI.getIdentification().getPeptideMatchParameter(peptideMatchKey, psParameter);
            if (psParameter.getProteinInferenceClass() != protInferenceTypeCmb.getSelectedIndex()) {
                psParameter.setProteinInferenceClass(protInferenceTypeCmb.getSelectedIndex());
                peptideShakerGUI.getIdentification().updatePeptideMatchParameter(peptideMatchKey, psParameter);
                peptideShakerGUI.setDataSaved(false);
                peptideShakerGUI.setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, false);
                peptideShakerGUI.setUpdated(PeptideShakerGUI.MODIFICATIONS_TAB_INDEX, false);
                peptideShakerGUI.updateTabbedPanes();
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            this.dispose();
            return;
        }
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void helpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void helpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_helpJButtonMouseEntered

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/ProteinInferencePeptideLevel.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpJButtonActionPerformed

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void retainedProteinJTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_retainedProteinJTableMouseMoved
        int row = retainedProteinJTable.rowAtPoint(evt.getPoint());
        int column = retainedProteinJTable.columnAtPoint(evt.getPoint());

        retainedProteinJTable.setToolTipText(null);

        if (column == 1 && retainedProteinJTable.getValueAt(row, column) != null) {

            String tempValue = (String) retainedProteinJTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == retainedProteinJTable.getColumn("Description").getModelIndex() && retainedProteinJTable.getValueAt(row, column) != null) {
            if (GuiUtilities.getPreferredWidthOfCell(retainedProteinJTable, row, column) > retainedProteinJTable.getColumn("Description").getWidth()) {
                retainedProteinJTable.setToolTipText("" + retainedProteinJTable.getValueAt(row, column));
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_retainedProteinJTableMouseMoved
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton helpJButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton okButton;
    private javax.swing.JTable otherProteinJTable;
    private javax.swing.JScrollPane otherProteinsJScrollPane;
    private javax.swing.JPanel peptidesPanel;
    private javax.swing.JComboBox protInferenceTypeCmb;
    private javax.swing.JScrollPane proteinsJScrollPane;
    private javax.swing.JPanel proteinsPanel;
    private javax.swing.JTable retainedProteinJTable;
    private javax.swing.JLabel sequenceLabel;
    // End of variables declaration//GEN-END:variables
}
