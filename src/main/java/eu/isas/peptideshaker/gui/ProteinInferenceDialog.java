package eu.isas.peptideshaker.gui;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * This dialog allows the user to resolve manually some protein inference issues.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinInferenceDialog extends javax.swing.JDialog {

    /**
     * The inspected protein match.
     */
    private ProteinMatch inspectedMatch;
    /**
     * The protein accessions.
     */
    private ArrayList<String> accessions;
    /**
     * The detected unique matches (if any).
     */
    private ArrayList<String> uniqueMatches = new ArrayList<String>();
    /**
     * Associated matches presenting the same proteins or a share.
     */
    private ArrayList<String> associatedMatches = new ArrayList<String>();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The PeptideShaker parent frame.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The candidate protein table column header tooltips.
     */
    private ArrayList<String> candidateProteinsTableToolTips;
    /**
     * The unique hits table column header tooltips.
     */
    private ArrayList<String> uniqueHitsTableToolTips;
    /**
     * The related hits table column header tooltips.
     */
    private ArrayList<String> relatedHitsTableToolTips;
    /**
     * The identification.
     */
    private Identification identification;
    /**
     * the selected main match.
     */
    private int mainMatch;

    /**
     * Creates new form ProteinInferenceDialog.
     *
     * @param peptideShakerGUI
     * @param inspectedMatch
     * @param identification
     */
    public ProteinInferenceDialog(PeptideShakerGUI peptideShakerGUI, String inspectedMatch, Identification identification) {
        super(peptideShakerGUI, true);
        this.identification = identification;
        this.peptideShakerGUI = peptideShakerGUI;
        
        try {
            this.inspectedMatch = identification.getProteinMatch(inspectedMatch);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            this.dispose();
        }
        
        accessions = new ArrayList(Arrays.asList(ProteinMatch.getAccessions(inspectedMatch)));

        for (String proteinAccession : accessions) {
            if (identification.getProteinIdentification().contains(proteinAccession)) {
                uniqueMatches.add(proteinAccession);
            }
        }

        for (String proteinKey : identification.getProteinIdentification()) {
            if (ProteinMatch.getNProteins(proteinKey) > 1 && !associatedMatches.contains(proteinKey) && !proteinKey.equals(inspectedMatch)) {
                for (String proteinAccession : accessions) {
                    if (proteinKey.contains(proteinAccession)) {
                        associatedMatches.add(proteinKey);
                        break;
                    }
                }
            }
        }

        initComponents();

        // make sure that the scroll panes are see-through
        proteinMatchJScrollPane.getViewport().setOpaque(false);
        uniqueHitsJScrollPane.getViewport().setOpaque(false);
        relatedHitsJScrollPane.getViewport().setOpaque(false);

        groupClassJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        PSParameter psParameter = new PSParameter();
        try {
            psParameter = (PSParameter) identification.getProteinMatchParameter(inspectedMatch, psParameter);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        matchInfoLbl.setText("[Score: " + Util.roundDouble(psParameter.getProteinScore(), 2)
                + ", Confidence: " + Util.roundDouble(psParameter.getProteinConfidence(), 2) + "]");

        // set up the table column properties
        setColumnProperies();

        // The index should be set in the design according to the PSParameter class static fields!
        groupClassJComboBox.setSelectedIndex(psParameter.getGroupClass());

        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Set the properties for the columns in the results tables.
     */
    private void setColumnProperies() {

        proteinMatchTable.getTableHeader().setReorderingAllowed(false);
        uniqueHitsTable.getTableHeader().setReorderingAllowed(false);
        relatedHitsTable.getTableHeader().setReorderingAllowed(false);

        proteinMatchTable.getColumn("  ").setMinWidth(30);
        proteinMatchTable.getColumn("  ").setMaxWidth(30);

        // set the preferred size of the accession column
        Integer width = peptideShakerGUI.getPreferredAccessionColumnWidth(proteinMatchTable, proteinMatchTable.getColumn("Accession").getModelIndex(), 2);
        if (width != null) {
            proteinMatchTable.getColumn("Accession").setMinWidth(width);
            proteinMatchTable.getColumn("Accession").setMaxWidth(width);
        } else {
            proteinMatchTable.getColumn("Accession").setMinWidth(15);
            proteinMatchTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
        }

        // the validated column
        uniqueHitsTable.getColumn(" ").setMaxWidth(30);
        relatedHitsTable.getColumn(" ").setMaxWidth(30);
        uniqueHitsTable.getColumn(" ").setMinWidth(30);
        relatedHitsTable.getColumn(" ").setMinWidth(30);

        proteinMatchTable.getColumn("").setMaxWidth(30);
        uniqueHitsTable.getColumn("").setMaxWidth(30);
        relatedHitsTable.getColumn("").setMaxWidth(30);
        proteinMatchTable.getColumn("").setMinWidth(30);
        uniqueHitsTable.getColumn("").setMinWidth(30);
        relatedHitsTable.getColumn("").setMinWidth(30);

        // the score and confidence columns
        uniqueHitsTable.getColumn("Confidence").setMaxWidth(90);
        uniqueHitsTable.getColumn("Confidence").setMinWidth(90);
        uniqueHitsTable.getColumn("Score").setMaxWidth(90);
        uniqueHitsTable.getColumn("Score").setMinWidth(90);

        relatedHitsTable.getColumn("Confidence").setMaxWidth(90);
        relatedHitsTable.getColumn("Confidence").setMinWidth(90);
        relatedHitsTable.getColumn("Score").setMaxWidth(90);
        relatedHitsTable.getColumn("Score").setMinWidth(90);

        // change the cell renderer to fix a problem in Nimbus and alternating row colors
        proteinMatchTable.getColumn("  ").setCellRenderer(new NimbusCheckBoxRenderer());

        proteinMatchTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(
                peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));

        uniqueHitsTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(
                peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        uniqueHitsTable.getColumn(" ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));
        uniqueHitsTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        uniqueHitsTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) uniqueHitsTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        ((JSparklinesBarChartTableCellRenderer) uniqueHitsTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);

        relatedHitsTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(
                peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        relatedHitsTable.getColumn(" ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));
        relatedHitsTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        relatedHitsTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedHitsTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        ((JSparklinesBarChartTableCellRenderer) relatedHitsTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);

        // set up the table header tooltips
        candidateProteinsTableToolTips = new ArrayList<String>();
        candidateProteinsTableToolTips.add(null);
        candidateProteinsTableToolTips.add("Currently Selected Protein Match");
        candidateProteinsTableToolTips.add("Protein Accession");
        candidateProteinsTableToolTips.add("Protein Description");

        uniqueHitsTableToolTips = new ArrayList<String>();
        uniqueHitsTableToolTips.add(null);
        uniqueHitsTableToolTips.add("Protein Accession(s)");
        uniqueHitsTableToolTips.add("Protein Score");
        uniqueHitsTableToolTips.add("Protein Confidence");
        uniqueHitsTableToolTips.add("Validated");

        relatedHitsTableToolTips = new ArrayList<String>();
        relatedHitsTableToolTips.add(null);
        relatedHitsTableToolTips.add("Protein Accession(s)");
        relatedHitsTableToolTips.add("Protein Score");
        relatedHitsTableToolTips.add("Protein Confidence");
        relatedHitsTableToolTips.add("Validated");
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
        okButton = new javax.swing.JButton();
        relatedHitsJPanel = new javax.swing.JPanel();
        relatedHitsJScrollPane = new javax.swing.JScrollPane();
        relatedHitsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) relatedHitsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        uniqueHitsJPanel = new javax.swing.JPanel();
        uniqueHitsJScrollPane = new javax.swing.JScrollPane();
        uniqueHitsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) uniqueHitsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        proteinMatchJPanel = new javax.swing.JPanel();
        proteinMatchJScrollPane = new javax.swing.JScrollPane();
        proteinMatchTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) candidateProteinsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        groupDetalsJPanel = new javax.swing.JPanel();
        matchInfoLbl = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        groupClassJComboBox = new javax.swing.JComboBox();
        helpJButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Protein Inference - Protein Level");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        relatedHitsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Related Hits"));
        relatedHitsJPanel.setOpaque(false);

        relatedHitsTable.setModel(new AssociatedMatches());
        relatedHitsTable.setOpaque(false);
        relatedHitsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                relatedHitsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                relatedHitsTableMouseReleased(evt);
            }
        });
        relatedHitsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                relatedHitsTableMouseMoved(evt);
            }
        });
        relatedHitsJScrollPane.setViewportView(relatedHitsTable);

        javax.swing.GroupLayout relatedHitsJPanelLayout = new javax.swing.GroupLayout(relatedHitsJPanel);
        relatedHitsJPanel.setLayout(relatedHitsJPanelLayout);
        relatedHitsJPanelLayout.setHorizontalGroup(
            relatedHitsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(relatedHitsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedHitsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 723, Short.MAX_VALUE)
                .addContainerGap())
        );
        relatedHitsJPanelLayout.setVerticalGroup(
            relatedHitsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(relatedHitsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(relatedHitsJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        uniqueHitsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Unique Hits"));
        uniqueHitsJPanel.setOpaque(false);

        uniqueHitsTable.setModel(new UniqueMatches());
        uniqueHitsTable.setOpaque(false);
        uniqueHitsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                uniqueHitsTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                uniqueHitsTableMouseReleased(evt);
            }
        });
        uniqueHitsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                uniqueHitsTableMouseMoved(evt);
            }
        });
        uniqueHitsJScrollPane.setViewportView(uniqueHitsTable);

        javax.swing.GroupLayout uniqueHitsJPanelLayout = new javax.swing.GroupLayout(uniqueHitsJPanel);
        uniqueHitsJPanel.setLayout(uniqueHitsJPanelLayout);
        uniqueHitsJPanelLayout.setHorizontalGroup(
            uniqueHitsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uniqueHitsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(uniqueHitsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 723, Short.MAX_VALUE)
                .addContainerGap())
        );
        uniqueHitsJPanelLayout.setVerticalGroup(
            uniqueHitsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(uniqueHitsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(uniqueHitsJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        proteinMatchJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Candidate Proteins"));
        proteinMatchJPanel.setOpaque(false);

        proteinMatchTable.setModel(new MatchTable());
        proteinMatchTable.setOpaque(false);
        proteinMatchTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinMatchTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinMatchTableMouseReleased(evt);
            }
        });
        proteinMatchTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                proteinMatchTableMouseMoved(evt);
            }
        });
        proteinMatchJScrollPane.setViewportView(proteinMatchTable);

        javax.swing.GroupLayout proteinMatchJPanelLayout = new javax.swing.GroupLayout(proteinMatchJPanel);
        proteinMatchJPanel.setLayout(proteinMatchJPanelLayout);
        proteinMatchJPanelLayout.setHorizontalGroup(
            proteinMatchJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinMatchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinMatchJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 723, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinMatchJPanelLayout.setVerticalGroup(
            proteinMatchJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinMatchJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinMatchJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                .addContainerGap())
        );

        groupDetalsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Protein Group Details"));
        groupDetalsJPanel.setOpaque(false);

        matchInfoLbl.setText("protein match information");

        jLabel2.setText("Type:");

        groupClassJComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Single Protein", "Isoforms", "Unrelated Isoforms", "Unrelated Proteins" }));

        javax.swing.GroupLayout groupDetalsJPanelLayout = new javax.swing.GroupLayout(groupDetalsJPanel);
        groupDetalsJPanel.setLayout(groupDetalsJPanelLayout);
        groupDetalsJPanelLayout.setHorizontalGroup(
            groupDetalsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(groupDetalsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(20, 20, 20)
                .addComponent(groupClassJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 295, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(matchInfoLbl)
                .addContainerGap(249, Short.MAX_VALUE))
        );
        groupDetalsJPanelLayout.setVerticalGroup(
            groupDetalsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(groupDetalsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(groupDetalsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(groupClassJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(matchInfoLbl))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(uniqueHitsJPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(groupDetalsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(proteinMatchJPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(relatedHitsJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(helpJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 586, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(groupDetalsJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proteinMatchJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uniqueHitsJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(relatedHitsJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
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
     * Update the protein table according to the protein inference selection.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        String selectedMatch = accessions.get(mainMatch);
        PSParameter psParameter = new PSParameter();
        try {
            psParameter = (PSParameter) identification.getProteinMatchParameter(inspectedMatch.getKey(), psParameter);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            this.dispose();
            return;
        }
        if (!inspectedMatch.getMainMatch().equals(selectedMatch)
                || groupClassJComboBox.getSelectedIndex() != psParameter.getGroupClass()) {
            try {
                if (!inspectedMatch.getMainMatch().equals(selectedMatch)) {
                    inspectedMatch.setMainMatch(selectedMatch);
                    peptideShakerGUI.getIdentification().updateProteinMatch(inspectedMatch);
                }
                psParameter.setGroupClass(groupClassJComboBox.getSelectedIndex());
                identification.updateProteinMatchParameter(inspectedMatch.getKey(), psParameter);
                peptideShakerGUI.updateMainMatch(inspectedMatch.getMainMatch(), groupClassJComboBox.getSelectedIndex());
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
            peptideShakerGUI.setDataSaved(false);
        }
        this.dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Sets the main match if the main match column is selected, or opens the
     * protein web link if the accession number column is selected.
     *
     * @param evt
     */
    private void proteinMatchTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinMatchTableMouseReleased

        int row = proteinMatchTable.rowAtPoint(evt.getPoint());
        int column = proteinMatchTable.columnAtPoint(evt.getPoint());

        if (row != -1) {

            if (column == 1) {
                try {
                    inspectedMatch.setMainMatch(accessions.get(row));
                    identification.updateProteinMatch(inspectedMatch);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
                proteinMatchTable.revalidate();
                proteinMatchTable.repaint();
            } else if (column == 2) {

                // open protein link in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) proteinMatchTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                    String link = (String) proteinMatchTable.getValueAt(row, column);
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        }
    }//GEN-LAST:event_proteinMatchTableMouseReleased

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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/ProteinInference.html"));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_helpJButtonActionPerformed

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void proteinMatchTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinMatchTableMouseMoved
        int row = proteinMatchTable.rowAtPoint(evt.getPoint());
        int column = proteinMatchTable.columnAtPoint(evt.getPoint());

        proteinMatchTable.setToolTipText(null);

        if (column == 2 && proteinMatchTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinMatchTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == proteinMatchTable.getColumn("Description").getModelIndex() && proteinMatchTable.getValueAt(row, column) != null) {
            if (GuiUtilities.getPreferredWidthOfCell(proteinMatchTable, row, column) > proteinMatchTable.getColumn("Description").getWidth()) {
                proteinMatchTable.setToolTipText("" + proteinMatchTable.getValueAt(row, column));
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinMatchTableMouseMoved

    /**
     * Changes the cursor back to the default cursor a hand.
     *
     * @param evt
     */
    private void proteinMatchTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinMatchTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinMatchTableMouseExited

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void uniqueHitsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uniqueHitsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_uniqueHitsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void uniqueHitsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uniqueHitsTableMouseMoved
        int row = uniqueHitsTable.rowAtPoint(evt.getPoint());
        int column = uniqueHitsTable.columnAtPoint(evt.getPoint());

        if (column == 1 && uniqueHitsTable.getValueAt(row, column) != null) {

            String tempValue = (String) uniqueHitsTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("a href=") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_uniqueHitsTableMouseMoved

    /**
     * Open the protein html links.
     *
     * @param evt
     */
    private void uniqueHitsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_uniqueHitsTableMouseReleased
        int row = uniqueHitsTable.getSelectedRow();
        int column = uniqueHitsTable.getSelectedColumn();

        if (row != -1) {

            if (column == 1) {

                // open protein links in web browser
                if (evt != null && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) uniqueHitsTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) uniqueHitsTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_uniqueHitsTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void relatedHitsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedHitsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_relatedHitsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void relatedHitsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedHitsTableMouseMoved
        int row = relatedHitsTable.rowAtPoint(evt.getPoint());
        int column = relatedHitsTable.columnAtPoint(evt.getPoint());

        if (column == 1 && relatedHitsTable.getValueAt(row, column) != null) {

            String tempValue = (String) relatedHitsTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("a href=") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_relatedHitsTableMouseMoved

    /**
     * Open the protein html links.
     *
     * @param evt
     */
    private void relatedHitsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedHitsTableMouseReleased
        int row = relatedHitsTable.getSelectedRow();
        int column = relatedHitsTable.getSelectedColumn();

        if (row != -1) {

            if (column == 1) {

                // open protein links in web browser
                if (evt != null && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) relatedHitsTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) relatedHitsTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_relatedHitsTableMouseReleased

    /**
     * Closes the dialog.
     * 
     * @param evt 
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox groupClassJComboBox;
    private javax.swing.JPanel groupDetalsJPanel;
    private javax.swing.JButton helpJButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel matchInfoLbl;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel proteinMatchJPanel;
    private javax.swing.JScrollPane proteinMatchJScrollPane;
    private javax.swing.JTable proteinMatchTable;
    private javax.swing.JPanel relatedHitsJPanel;
    private javax.swing.JScrollPane relatedHitsJScrollPane;
    private javax.swing.JTable relatedHitsTable;
    private javax.swing.JPanel uniqueHitsJPanel;
    private javax.swing.JScrollPane uniqueHitsJScrollPane;
    private javax.swing.JTable uniqueHitsTable;
    // End of variables declaration//GEN-END:variables

    /**
     * Table model for the protein match table.
     */
    private class MatchTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return accessions.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
                case 0:
                    return "";
                case 1:
                    return "  ";
                case 2:
                    return "Accession";
                case 3:
                    return "Description";
                default:
                    return " ";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            switch (column) {
                case 0:
                    return (row + 1);
                case 1:
                    return inspectedMatch.getMainMatch().equals(accessions.get(row));
                case 2:
                    return peptideShakerGUI.getIdentificationFeaturesGenerator().addDatabaseLink(accessions.get(row));
                case 3:
                    try {
                        return sequenceFactory.getHeader(accessions.get(row)).getDescription();
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        return "Database Error";
                    }
                default:
                    return " ";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }
    }

    /**
     * Table model for the unique matches table.
     */
    private class UniqueMatches extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return uniqueMatches.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
                case 0:
                    return "";
                case 1:
                    return "Protein(s)";
                case 2:
                    return "Score";
                case 3:
                    return "Confidence";
                case 4:
                    return " ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            PSParameter pSParameter = new PSParameter();
            try {
                pSParameter = (PSParameter) identification.getProteinMatchParameter(uniqueMatches.get(row), pSParameter);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }

            switch (column) {
                case 0:
                    return (row + 1);
                case 1:
                    return peptideShakerGUI.getIdentificationFeaturesGenerator().addDatabaseLinks(new ArrayList<String>(Arrays.asList(ProteinMatch.getAccessions(uniqueMatches.get(row)))));
                case 2:
                    return pSParameter.getProteinScore();
                case 3:
                    return pSParameter.getProteinConfidence();
                case 4:
                    return pSParameter.isValidated();
                default:
                    return "";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    /**
     * Table model for the associated matches table.
     */
    private class AssociatedMatches extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return associatedMatches.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
                case 0:
                    return "";
                case 1:
                    return "Protein(s)";
                case 2:
                    return "Score";
                case 3:
                    return "Confidence";
                case 4:
                    return " ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            PSParameter pSParameter = new PSParameter();
            try {
                pSParameter = (PSParameter) identification.getProteinMatchParameter(associatedMatches.get(row), pSParameter);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
            switch (column) {
                case 0:
                    return (row + 1);
                case 1:
                    return peptideShakerGUI.getIdentificationFeaturesGenerator().addDatabaseLinks(new ArrayList<String>(Arrays.asList(ProteinMatch.getAccessions(associatedMatches.get(row)))));
                case 2:
                    return pSParameter.getProteinScore();
                case 3:
                    return pSParameter.getProteinConfidence();
                case 4:
                    return pSParameter.isValidated();
                default:
                    return "";
            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
