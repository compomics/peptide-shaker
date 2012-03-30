package eu.isas.peptideshaker.gui.preferencesdialogs;

import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.pride.CvTerm;
import com.compomics.util.pride.PtmToPrideMap;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.pride.MyComboBoxRenderer;
import java.awt.Window;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
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
     * The neutral losses
     */
    ArrayList<NeutralLoss> neutralLosses = new ArrayList<NeutralLoss>();
    /**
     * The reporter ions
     */
    ArrayList<ReporterIon> reporterIons = new ArrayList<ReporterIon>();
    /**
     * The ptm to pride map
     */
    private PtmToPrideMap ptmToPrideMap;
    /**
     * The modification cv term
     */
    private CvTerm cvTerm = null;

    /**
     * Creates a new PTM dialog.
     *
     * @param searchPreferencesDialog the search panel
     * @param currentPTM the ptm to edit (can be null)
     */
    public PtmDialog(SearchPreferencesDialog searchPreferencesDialog, PTM currentPTM) {
        super(searchPreferencesDialog, true);

        this.searchPreferencesDialog = searchPreferencesDialog;
        this.ptmToPrideMap = searchPreferencesDialog.getPtmToPrideMap();
        this.currentPtm = currentPTM;

        initComponents();
        setUpGui();
        setLocationRelativeTo(searchPreferencesDialog);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        // set table properties
        neutralLossesTable.getTableHeader().setReorderingAllowed(false);
        reporterIonsTable.getTableHeader().setReorderingAllowed(false);

        // make sure that the scroll panes are see-through
        neutralLossesJScrollPane.getViewport().setOpaque(false);
        reporterIonsJScrollPane.getViewport().setOpaque(false);

        // the index column
        neutralLossesTable.getColumn(" ").setMaxWidth(50);
        neutralLossesTable.getColumn(" ").setMinWidth(50);
        reporterIonsTable.getColumn(" ").setMaxWidth(50);
        reporterIonsTable.getColumn(" ").setMinWidth(50);

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

        if (currentPtm != null) {
            typeCmb.setSelectedIndex(currentPtm.getType());
            nameTxt.setText(currentPtm.getName());
            massTxt.setText(currentPtm.getMass() + "");
            String residues = "";
            boolean first = true;

            for (String aa : currentPtm.getResidues()) {
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
            this.neutralLosses.addAll(currentPtm.getNeutralLosses());
            this.reporterIons.addAll(currentPtm.getReporterIons());
            updateTables();

            cvTerm = ptmToPrideMap.getCVTerm(currentPtm.getName());

            if (cvTerm == null) {
                cvTerm = PtmToPrideMap.getDefaultCVTerm(currentPtm.getName());
            }
            if (cvTerm != null) {
                updateModMappingText();
            }

            setTitle("Edit Modification");
        }
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
        if (ptmFactory.getDefaultModifications().contains(name)
                && (currentPtm == null || !name.equals(currentPtm.getName()))) {
            JOptionPane.showMessageDialog(this, "A modification named " + name + " alredy exists in the "
                    + "default modification lists. Please select the default modification or use another name.",
                    "Modification already exists", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (ptmFactory.getUserModifications().contains(name)
                && (currentPtm == null || !name.equals(currentPtm.getName()))) {
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

        backgroundPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        detailsPanel = new javax.swing.JPanel();
        typeCmb = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        nameTxt = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        massTxt = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        residuesTxt = new javax.swing.JTextField();
        neutralLossesAndReporterIonsPanel = new javax.swing.JPanel();
        neutralLossesJScrollPane = new javax.swing.JScrollPane();
        neutralLossesTable = new javax.swing.JTable();
        addNeutralLoss = new javax.swing.JButton();
        removeNeutralLoss = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        helpJButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        psiModMappingPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        psiModMappingJTextField = new javax.swing.JTextField();
        olsJButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        reporterIonsJScrollPane = new javax.swing.JScrollPane();
        reporterIonsTable = new javax.swing.JTable();
        removerReporterIon = new javax.swing.JButton();
        addReporterIon = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("New Modification");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        detailsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Properties"));
        detailsPanel.setOpaque(false);

        typeCmb.setMaximumRowCount(15);
        typeCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Particular Amino Acid", "Protein N-term", "Protein N-term - Particular Amino Acid(s)", "Protein C-term", "Protein C-term - Particular Amino Acid(s)", "Peptide N-term", "Peptide N-term - Particular Amino Acid(s)", "Peptide C-term", "Peptide C-term - Particular Amino Acid(s)" }));
        typeCmb.setToolTipText("The modification type. See help for details.");
        typeCmb.setEnabled(false);
        typeCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeCmbActionPerformed(evt);
            }
        });

        jLabel1.setText("Type:");
        jLabel1.setToolTipText("The modification type. See help for details.");

        jLabel2.setText("Name:");
        jLabel2.setToolTipText("The modification name");

        nameTxt.setEditable(false);
        nameTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nameTxt.setToolTipText("The modification name");

        jLabel3.setText("Mass (Da):");
        jLabel3.setToolTipText("Monoisotopic mass in Dalton");

        massTxt.setEditable(false);
        massTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        massTxt.setToolTipText("Monoisotopic mass in Dalton");

        jLabel5.setText("Residue(s):");
        jLabel5.setToolTipText("Residues modified");

        residuesTxt.setEditable(false);
        residuesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        residuesTxt.setToolTipText("Residues modified");

        javax.swing.GroupLayout detailsPanelLayout = new javax.swing.GroupLayout(detailsPanel);
        detailsPanel.setLayout(detailsPanelLayout);
        detailsPanelLayout.setHorizontalGroup(
            detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(detailsPanelLayout.createSequentialGroup()
                        .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameTxt)
                            .addComponent(typeCmb, 0, 345, Short.MAX_VALUE)))
                    .addGroup(detailsPanelLayout.createSequentialGroup()
                        .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(residuesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
                            .addComponent(massTxt))))
                .addContainerGap(58, Short.MAX_VALUE))
        );

        detailsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3, jLabel5});

        detailsPanelLayout.setVerticalGroup(
            detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeCmb)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameTxt)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(massTxt)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(residuesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addContainerGap())
        );

        neutralLossesAndReporterIonsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Neutral Losses"));
        neutralLossesAndReporterIonsPanel.setOpaque(false);

        neutralLossesTable.setModel(new NeutralLossesTable());
        neutralLossesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        neutralLossesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                neutralLossesTableMouseReleased(evt);
            }
        });
        neutralLossesJScrollPane.setViewportView(neutralLossesTable);

        addNeutralLoss.setText("+");
        addNeutralLoss.setToolTipText("Add a neutral loss");
        addNeutralLoss.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNeutralLossActionPerformed(evt);
            }
        });

        removeNeutralLoss.setText("-");
        removeNeutralLoss.setToolTipText("Remove the selected neutral loss");
        removeNeutralLoss.setEnabled(false);
        removeNeutralLoss.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeNeutralLossActionPerformed(evt);
            }
        });

        jLabel4.setText("Losses:");

        javax.swing.GroupLayout neutralLossesAndReporterIonsPanelLayout = new javax.swing.GroupLayout(neutralLossesAndReporterIonsPanel);
        neutralLossesAndReporterIonsPanel.setLayout(neutralLossesAndReporterIonsPanelLayout);
        neutralLossesAndReporterIonsPanelLayout.setHorizontalGroup(
            neutralLossesAndReporterIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(neutralLossesAndReporterIonsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(neutralLossesJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(neutralLossesAndReporterIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(addNeutralLoss, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(removeNeutralLoss, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE))
                .addContainerGap())
        );
        neutralLossesAndReporterIonsPanelLayout.setVerticalGroup(
            neutralLossesAndReporterIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(neutralLossesAndReporterIonsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(neutralLossesAndReporterIonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(neutralLossesAndReporterIonsPanelLayout.createSequentialGroup()
                        .addComponent(neutralLossesJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(neutralLossesAndReporterIonsPanelLayout.createSequentialGroup()
                        .addComponent(addNeutralLoss)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeNeutralLoss)
                        .addContainerGap())))
            .addGroup(neutralLossesAndReporterIonsPanelLayout.createSequentialGroup()
                .addGap(44, 44, 44)
                .addComponent(jLabel4)
                .addGap(0, 0, Short.MAX_VALUE))
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

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        psiModMappingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PSI-MOD Mapping"));
        psiModMappingPanel.setOpaque(false);

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

        javax.swing.GroupLayout psiModMappingPanelLayout = new javax.swing.GroupLayout(psiModMappingPanel);
        psiModMappingPanel.setLayout(psiModMappingPanelLayout);
        psiModMappingPanelLayout.setHorizontalGroup(
            psiModMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psiModMappingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(psiModMappingJTextField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(olsJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        psiModMappingPanelLayout.setVerticalGroup(
            psiModMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psiModMappingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(psiModMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(psiModMappingJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(olsJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Reporter Ions"));
        jPanel1.setOpaque(false);

        reporterIonsTable.setModel(new ReporterIonsTable());
        reporterIonsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        reporterIonsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                reporterIonsTableMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                reporterIonsTableMouseReleased(evt);
            }
        });
        reporterIonsJScrollPane.setViewportView(reporterIonsTable);

        removerReporterIon.setText("-");
        removerReporterIon.setToolTipText("Remove the selected reporter ion");
        removerReporterIon.setEnabled(false);
        removerReporterIon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removerReporterIonActionPerformed(evt);
            }
        });

        addReporterIon.setText("+");
        addReporterIon.setToolTipText("Add a reporter ion");
        addReporterIon.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addReporterIonActionPerformed(evt);
            }
        });

        jLabel7.setText("Reporters:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(reporterIonsJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(addReporterIon, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(removerReporterIon, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(addReporterIon)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removerReporterIon))
                            .addComponent(reporterIonsJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(jLabel7)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(detailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(helpJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(neutralLossesAndReporterIonsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(psiModMappingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(detailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(neutralLossesAndReporterIonsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psiModMappingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(helpJButton)
                    .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(cancelButton)
                        .addComponent(okButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

    /**
     * Add the ptm to the SearchPanel.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (validateInput()) {
            PTM otherPTM, newPTM = new PTM(typeCmb.getSelectedIndex(), nameTxt.getText().trim().toLowerCase(), new Double(massTxt.getText().trim()), parseResidues());
            ArrayList<NeutralLoss> tempNeutralLosses = new ArrayList<NeutralLoss>();
            for (int row = 0; row < neutralLossesTable.getRowCount(); row++) {
                tempNeutralLosses.add(new NeutralLoss((String) neutralLossesTable.getValueAt(row, 1),
                        (Double) neutralLossesTable.getValueAt(row, 2)));
            }

            newPTM.setNeutralLosses(tempNeutralLosses);
            ArrayList<ReporterIon> tempReporterIons = new ArrayList<ReporterIon>();

            for (int row = 0; row < reporterIonsTable.getRowCount(); row++) {
                tempReporterIons.add(new ReporterIon((String) reporterIonsTable.getValueAt(row, 1),
                        (Double) reporterIonsTable.getValueAt(row, 2)));
            }

            newPTM.setReporterIons(tempReporterIons);

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
                ptmFactory.replacePTM(currentPtm.getName(), newPTM);
            } else {
                ptmFactory.addUserPTM(newPTM);
            }

            ptmToPrideMap.putCVTerm(newPTM.getName(), cvTerm);

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

    private void addNeutralLossActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addNeutralLossActionPerformed
        neutralLosses.add(new NeutralLoss("new neutral loss", 0.0));
        updateTables();
    }//GEN-LAST:event_addNeutralLossActionPerformed

    private void addReporterIonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addReporterIonActionPerformed
        reporterIons.add(new ReporterIon("New reporter ion", 0.0));
        updateTables();
    }//GEN-LAST:event_addReporterIonActionPerformed

    private void removeNeutralLossActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeNeutralLossActionPerformed
        int row = neutralLossesTable.getSelectedRow();
        if (row != -1) {
            int index = neutralLossesTable.convertRowIndexToModel(row);
            neutralLosses.remove(index);
            updateTables();
        }
        row = neutralLossesTable.getSelectedRow();
        removeNeutralLoss.setEnabled(row != -1);
    }//GEN-LAST:event_removeNeutralLossActionPerformed

    private void removerReporterIonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removerReporterIonActionPerformed
        int row = reporterIonsTable.getSelectedRow();
        if (row != -1) {
            int index = reporterIonsTable.convertRowIndexToModel(row);
            reporterIons.remove(index);
            updateTables();
        }
        row = reporterIonsTable.getSelectedRow();
        removerReporterIon.setEnabled(row != -1);
    }//GEN-LAST:event_removerReporterIonActionPerformed

    private void neutralLossesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_neutralLossesTableMouseReleased
        int row = neutralLossesTable.getSelectedRow();
        removeNeutralLoss.setEnabled(row != -1);
    }//GEN-LAST:event_neutralLossesTableMouseReleased

    private void reporterIonsTableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_reporterIonsTableMousePressed
        // TODO add your handling code here:
    }//GEN-LAST:event_reporterIonsTableMousePressed

    private void reporterIonsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_reporterIonsTableMouseReleased
        int row = reporterIonsTable.getSelectedRow();
        removerReporterIon.setEnabled(row != -1);
    }//GEN-LAST:event_reporterIonsTableMouseReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addNeutralLoss;
    private javax.swing.JButton addReporterIon;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel detailsPanel;
    private javax.swing.JButton helpJButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField massTxt;
    private javax.swing.JTextField nameTxt;
    private javax.swing.JPanel neutralLossesAndReporterIonsPanel;
    private javax.swing.JScrollPane neutralLossesJScrollPane;
    private javax.swing.JTable neutralLossesTable;
    private javax.swing.JButton okButton;
    private javax.swing.JButton olsJButton;
    private javax.swing.JTextField psiModMappingJTextField;
    private javax.swing.JPanel psiModMappingPanel;
    private javax.swing.JButton removeNeutralLoss;
    private javax.swing.JButton removerReporterIon;
    private javax.swing.JScrollPane reporterIonsJScrollPane;
    private javax.swing.JTable reporterIonsTable;
    private javax.swing.JTextField residuesTxt;
    private javax.swing.JComboBox typeCmb;
    // End of variables declaration//GEN-END:variables

    @Override
    public void insertOLSResult(String field, String selectedValue,
            String accession, String ontologyShort, String ontologyLong, int modifiedRow, String mappedTerm, Map<String, String> metadata) {
        cvTerm = new CvTerm(ontologyShort, accession, selectedValue, null);
        updateModMappingText();
    }

    @Override
    public Window getWindow() {
        return (Window) this;
    }

    /**
     * Displays the PSI-MOD mapping information.
     */
    private void updateModMappingText() {
        psiModMappingJTextField.setText(cvTerm.getName() + " [" + cvTerm.getAccession() + "]");
        psiModMappingJTextField.setCaretPosition(0);
    }

    /**
     * Update the neutral losses and reportet ions tables.
     */
    private void updateTables() {
        ((DefaultTableModel) neutralLossesTable.getModel()).fireTableDataChanged();
        ((DefaultTableModel) reporterIonsTable.getModel()).fireTableDataChanged();
    }

    /**
     * Table model for the neutral losses table
     */
    private class NeutralLossesTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return neutralLosses.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Name";
                case 2:
                    return "Mass";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    return neutralLosses.get(row).name;
                case 2:
                    return neutralLosses.get(row).mass;
                default:
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
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 0;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            int index = neutralLossesTable.convertColumnIndexToModel(row);
            NeutralLoss neutralLoss = neutralLosses.get(index);
            if (column == 1) {
                neutralLoss.name = (String) aValue;
            } else if (column == 2) {
                neutralLoss.mass = (Double) aValue;
            }
        }
    }

    /**
     * Table model for the reporter ions table
     */
    private class ReporterIonsTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return reporterIons.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Name";
                case 2:
                    return "Mass";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return row + 1;
                case 1:
                    return reporterIons.get(row).getName();
                case 2:
                    return reporterIons.get(row).getTheoreticMass();
                default:
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
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 0;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            int index = reporterIonsTable.convertColumnIndexToModel(row);
            ReporterIon reporterIon = reporterIons.get(index);
            if (column == 1) {
                reporterIon.setName((String) aValue);
            } else if (column == 2) {
                reporterIon.setMass((Double) aValue);
            }
        }
    }
}
