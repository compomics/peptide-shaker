package eu.isas.peptideshaker.gui.filtering;

import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ModificationProfile;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Dialog to edit peptide filters.
 *
 * @author Marc Vaudel
 */
public class PeptideFilterDialog extends javax.swing.JDialog {

    /**
     * The modifications table column header tooltips.
     */
    private ArrayList<String> ptmTableToolTips;
    /**
     * Boolean indicating whether the user canceled the filtering.
     */
    private boolean canceled = false;
    /**
     * The original filter.
     */
    private PeptideFilter peptideFilter;

    /**
     * Creates a new ProteinFilterDialog.
     *
     * @param parent the parent frame
     * @param identificationParameters the identification parameters
     */
    public PeptideFilterDialog(java.awt.Frame parent, IdentificationParameters identificationParameters) {
        this(parent, null, identificationParameters);
    }

    /**
     * Creates a new  ProteinFilterDialog.
     *
     * @param parent the parent frame
     * @param filter the protein filter to edit
     * @param identificationParameters the identification parameters
     */
    public PeptideFilterDialog(java.awt.Frame parent, PeptideFilter filter, IdentificationParameters identificationParameters) {
        super(parent, true);
        initComponents();
        if (filter == null) {
            filter = new PeptideFilter("New Filter");
        }
        this.peptideFilter = filter;
        setUpGUI(filter, identificationParameters);

        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Sets up the GUI components.
     *
     * @param peptideFilter the filter to use to populate the GUI
     * @param identificationParameters the identification parameters
     */
    public void setUpGUI(PeptideFilter peptideFilter, IdentificationParameters identificationParameters) {

        nameTxt.setText(peptideFilter.getName());
        descriptionTxt.setText(peptideFilter.getDescription());

        peptidePICmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        peptidePIComparisonCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        peptideNSpectraCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        peptideConfidenceCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        modificationsScrollPane.getViewport().setOpaque(false);

        if (peptideFilter.getProtein() != null) {
            peptideProteinTxt.setText(peptideFilter.getProtein());
        }
        if (peptideFilter.getSequence() != null) {
            peptideSequenceTxt.setText(peptideFilter.getSequence());
        }
        if (peptideFilter.getPi() < 5) {
            peptidePICmb.setSelectedIndex(peptideFilter.getPi() + 1);
            peptidePIComparisonCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getPiComparison()));
        }
        if (peptideFilter.getNValidatedSpectra() != null) {
            peptideNSpectraTxt.setText(peptideFilter.getNValidatedSpectra() + "");
            peptideNSpectraCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getnSpectraComparison()));
        }
        if (peptideFilter.getPeptideConfidence() != null) {
            peptideConfidenceTxt.setText(peptideFilter.getPeptideConfidence() + "");
            peptideConfidenceCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getPeptideConfidenceComparison()));
        }

        ArrayList<String> modifications = peptideFilter.getModificationStatus();
        ((DefaultTableModel) modificationTable.getModel()).addRow(new Object[]{
            modifications.contains(PtmPanel.NO_MODIFICATION),
            Color.lightGray,
            PtmPanel.NO_MODIFICATION});
        ModificationProfile modificationProfile = identificationParameters.getSearchParameters().getModificationProfile();
        for (String modification : modificationProfile.getAllNotFixedModifications()) {
            ((DefaultTableModel) modificationTable.getModel()).addRow(new Object[]{
                modifications.contains(modification),
                modificationProfile.getColor(modification),
                modification});
        }

        ptmTableToolTips = new ArrayList<String>();
        ptmTableToolTips.add(null);
        ptmTableToolTips.add("Modification Color");
        ptmTableToolTips.add("Modification Name");

        modificationTable.getColumn(" ").setMaxWidth(30);
        modificationTable.getColumn(" ").setMinWidth(30);
        modificationTable.getColumn("  ").setMaxWidth(40);
        modificationTable.getColumn("  ").setMinWidth(40);

        String text = "";

        for (String key : peptideFilter.getManualValidation()) {
            if (!text.equals("")) {
                text += "; ";
            }
            text += key;
        }

        peptideManualValidationTxt.setText(text);
        text = "";

        for (String accession : peptideFilter.getExceptions()) {
            if (!text.equals("")) {
                text += "; ";
            }
            text += accession;
        }

        peptideExceptionsTxt.setText(text);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        proteinFilterPanel = new javax.swing.JPanel();
        filterSplitPane = new javax.swing.JSplitPane();
        settingsPanel = new javax.swing.JPanel();
        peptideFilterParamsPanel = new javax.swing.JPanel();
        proteinLabel = new javax.swing.JLabel();
        peptideProteinTxt = new javax.swing.JTextField();
        regExp2Label = new javax.swing.JLabel();
        piStatusPeptideLabel = new javax.swing.JLabel();
        peptidePICmb = new javax.swing.JComboBox();
        spectraPeptidesLabel = new javax.swing.JLabel();
        peptideNSpectraTxt = new javax.swing.JTextField();
        peptidePIComparisonCmb = new javax.swing.JComboBox();
        confidencePeptidesLabel = new javax.swing.JLabel();
        peptideConfidenceTxt = new javax.swing.JTextField();
        peptideConfidenceCmb = new javax.swing.JComboBox();
        peptideNSpectraCmb = new javax.swing.JComboBox();
        sequenceLabel = new javax.swing.JLabel();
        peptideSequenceTxt = new javax.swing.JTextField();
        regExp3Label = new javax.swing.JLabel();
        modificationsPanel = new javax.swing.JPanel();
        modificationsScrollPane = new javax.swing.JScrollPane();
        modificationTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) ptmTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        manualInputSplitPane = new javax.swing.JSplitPane();
        peptidesExceptionsPanel = new javax.swing.JPanel();
        peptidesExceptionsScrollPane = new javax.swing.JScrollPane();
        peptideExceptionsTxt = new javax.swing.JTextArea();
        peptidesManualValidationPanel = new javax.swing.JPanel();
        peptidesManualValidationScrollPane = new javax.swing.JScrollPane();
        peptideManualValidationTxt = new javax.swing.JTextArea();
        propertiesPanel = new javax.swing.JPanel();
        nameLbl = new javax.swing.JLabel();
        nameTxt = new javax.swing.JTextField();
        descriptionLbl = new javax.swing.JLabel();
        descriptionlScrollPane = new javax.swing.JScrollPane();
        descriptionTxt = new javax.swing.JTextArea();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Peptide Filter");

        proteinFilterPanel.setBackground(new java.awt.Color(230, 230, 230));

        filterSplitPane.setBackground(new java.awt.Color(230, 230, 230));
        filterSplitPane.setBorder(null);
        filterSplitPane.setDividerLocation(200);
        filterSplitPane.setDividerSize(0);
        filterSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        filterSplitPane.setOpaque(false);

        settingsPanel.setOpaque(false);

        peptideFilterParamsPanel.setBackground(new java.awt.Color(230, 230, 230));
        peptideFilterParamsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));
        peptideFilterParamsPanel.setOpaque(false);

        proteinLabel.setText("Protein");

        peptideProteinTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        regExp2Label.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        regExp2Label.setText("RegExp");
        regExp2Label.setToolTipText("<html> Regular Expression<br> example: N[^P][ST] returns all sequences with:<br> an N, then anything but a P, and then an S or a T </html>");

        piStatusPeptideLabel.setText("PI Status");
        piStatusPeptideLabel.setToolTipText("Protein Inference Status");

        peptidePICmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Requirement", "Single Protein", "Isoforms", "Isoforms/Unrelated Proteins", "Unrelated Proteins" }));
        peptidePICmb.setToolTipText("Protein Inference Status");

        spectraPeptidesLabel.setText("#Spectra");
        spectraPeptidesLabel.setToolTipText("Number of Validated Spectra");

        peptideNSpectraTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideNSpectraTxt.setToolTipText("Number of Validated Spectra");

        peptidePIComparisonCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptidePIComparisonCmb.setSelectedIndex(3);

        confidencePeptidesLabel.setText("Confidence");
        confidencePeptidesLabel.setToolTipText("Confidence [%]");

        peptideConfidenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        peptideConfidenceTxt.setToolTipText("Confidence [%]");

        peptideConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptideConfidenceCmb.setSelectedIndex(3);

        peptideNSpectraCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptideNSpectraCmb.setSelectedIndex(3);

        sequenceLabel.setText("Sequence");

        peptideSequenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        regExp3Label.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        regExp3Label.setText("RegExp");
        regExp3Label.setToolTipText("<html> Regular Expression<br> example: N[^P][ST] returns all sequences with:<br> an N, then anything but a P, and then an S or a T </html>");

        modificationsPanel.setBackground(new java.awt.Color(230, 230, 230));
        modificationsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Modifications"));
        modificationsPanel.setOpaque(false);

        modificationTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "  ", "PTM"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        modificationsScrollPane.setViewportView(modificationTable);

        javax.swing.GroupLayout modificationsPanelLayout = new javax.swing.GroupLayout(modificationsPanel);
        modificationsPanel.setLayout(modificationsPanelLayout);
        modificationsPanelLayout.setHorizontalGroup(
            modificationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modificationsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modificationsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 406, Short.MAX_VALUE)
                .addContainerGap())
        );
        modificationsPanelLayout.setVerticalGroup(
            modificationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modificationsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modificationsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout peptideFilterParamsPanelLayout = new javax.swing.GroupLayout(peptideFilterParamsPanel);
        peptideFilterParamsPanel.setLayout(peptideFilterParamsPanelLayout);
        peptideFilterParamsPanelLayout.setHorizontalGroup(
            peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sequenceLabel)
                    .addComponent(piStatusPeptideLabel)
                    .addComponent(spectraPeptidesLabel)
                    .addComponent(confidencePeptidesLabel)
                    .addComponent(proteinLabel))
                .addGap(18, 18, 18)
                .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(regExp2Label)
                    .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(peptideNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(peptideConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(peptidePIComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(regExp3Label, javax.swing.GroupLayout.Alignment.LEADING)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(peptideConfidenceTxt)
                    .addComponent(peptidePICmb, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(peptideSequenceTxt, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(peptideProteinTxt, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(peptideNSpectraTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 448, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(69, 69, 69)
                .addComponent(modificationsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        peptideFilterParamsPanelLayout.setVerticalGroup(
            peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addComponent(modificationsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(peptideFilterParamsPanelLayout.createSequentialGroup()
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(proteinLabel)
                            .addComponent(peptideProteinTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(regExp2Label))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(sequenceLabel)
                            .addComponent(peptideSequenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(regExp3Label))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(piStatusPeptideLabel)
                            .addComponent(peptidePICmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptidePIComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(spectraPeptidesLabel)
                            .addComponent(peptideNSpectraTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(peptideFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(peptideConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(confidencePeptidesLabel)
                            .addComponent(peptideConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        manualInputSplitPane.setBorder(null);
        manualInputSplitPane.setDividerLocation(500);
        manualInputSplitPane.setDividerSize(0);
        manualInputSplitPane.setOpaque(false);

        peptidesExceptionsPanel.setBackground(new java.awt.Color(230, 230, 230));
        peptidesExceptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Exceptions"));
        peptidesExceptionsPanel.setOpaque(false);

        peptideExceptionsTxt.setColumns(20);
        peptideExceptionsTxt.setRows(1);
        peptidesExceptionsScrollPane.setViewportView(peptideExceptionsTxt);

        javax.swing.GroupLayout peptidesExceptionsPanelLayout = new javax.swing.GroupLayout(peptidesExceptionsPanel);
        peptidesExceptionsPanel.setLayout(peptidesExceptionsPanelLayout);
        peptidesExceptionsPanelLayout.setHorizontalGroup(
            peptidesExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptidesExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesExceptionsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 585, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesExceptionsPanelLayout.setVerticalGroup(
            peptidesExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesExceptionsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                .addContainerGap())
        );

        manualInputSplitPane.setRightComponent(peptidesExceptionsPanel);

        peptidesManualValidationPanel.setBackground(new java.awt.Color(230, 230, 230));
        peptidesManualValidationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));
        peptidesManualValidationPanel.setOpaque(false);

        peptideManualValidationTxt.setColumns(20);
        peptideManualValidationTxt.setRows(1);
        peptidesManualValidationScrollPane.setViewportView(peptideManualValidationTxt);

        javax.swing.GroupLayout peptidesManualValidationPanelLayout = new javax.swing.GroupLayout(peptidesManualValidationPanel);
        peptidesManualValidationPanel.setLayout(peptidesManualValidationPanelLayout);
        peptidesManualValidationPanelLayout.setHorizontalGroup(
            peptidesManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesManualValidationPanelLayout.setVerticalGroup(
            peptidesManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                .addContainerGap())
        );

        manualInputSplitPane.setLeftComponent(peptidesManualValidationPanel);

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(peptideFilterParamsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(manualInputSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addComponent(peptideFilterParamsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manualInputSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE))
        );

        filterSplitPane.setRightComponent(settingsPanel);

        propertiesPanel.setBackground(new java.awt.Color(230, 230, 230));
        propertiesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Properties"));
        propertiesPanel.setOpaque(false);

        nameLbl.setText("Name");

        descriptionLbl.setText("Description");

        descriptionTxt.setColumns(20);
        descriptionTxt.setRows(5);
        descriptionlScrollPane.setViewportView(descriptionTxt);

        javax.swing.GroupLayout propertiesPanelLayout = new javax.swing.GroupLayout(propertiesPanel);
        propertiesPanel.setLayout(propertiesPanelLayout);
        propertiesPanelLayout.setHorizontalGroup(
            propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(propertiesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(descriptionlScrollPane)
                    .addGroup(propertiesPanelLayout.createSequentialGroup()
                        .addGroup(propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(propertiesPanelLayout.createSequentialGroup()
                                .addComponent(nameLbl)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(nameTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 348, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(descriptionLbl))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        propertiesPanelLayout.setVerticalGroup(
            propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(propertiesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameLbl)
                    .addComponent(nameTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(descriptionLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(descriptionlScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE)
                .addContainerGap())
        );

        filterSplitPane.setLeftComponent(propertiesPanel);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout proteinFilterPanelLayout = new javax.swing.GroupLayout(proteinFilterPanel);
        proteinFilterPanel.setLayout(proteinFilterPanelLayout);
        proteinFilterPanelLayout.setHorizontalGroup(
            proteinFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinFilterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinFilterPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(filterSplitPane))
                .addContainerGap())
        );
        proteinFilterPanelLayout.setVerticalGroup(
            proteinFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinFilterPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filterSplitPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(proteinFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinFilterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinFilterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Set the filters and close the dialog.
     * 
     * @param evt 
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (validateInput()) {
            setFilter();
            dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Close the dialog without saving.
     * 
     * @param evt 
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        canceled = true;
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel confidencePeptidesLabel;
    private javax.swing.JLabel descriptionLbl;
    private javax.swing.JTextArea descriptionTxt;
    private javax.swing.JScrollPane descriptionlScrollPane;
    private javax.swing.JSplitPane filterSplitPane;
    private javax.swing.JSplitPane manualInputSplitPane;
    private javax.swing.JTable modificationTable;
    private javax.swing.JPanel modificationsPanel;
    private javax.swing.JScrollPane modificationsScrollPane;
    private javax.swing.JLabel nameLbl;
    private javax.swing.JTextField nameTxt;
    private javax.swing.JButton okButton;
    private javax.swing.JComboBox peptideConfidenceCmb;
    private javax.swing.JTextField peptideConfidenceTxt;
    private javax.swing.JTextArea peptideExceptionsTxt;
    private javax.swing.JPanel peptideFilterParamsPanel;
    private javax.swing.JTextArea peptideManualValidationTxt;
    private javax.swing.JComboBox peptideNSpectraCmb;
    private javax.swing.JTextField peptideNSpectraTxt;
    private javax.swing.JComboBox peptidePICmb;
    private javax.swing.JComboBox peptidePIComparisonCmb;
    private javax.swing.JTextField peptideProteinTxt;
    private javax.swing.JTextField peptideSequenceTxt;
    private javax.swing.JPanel peptidesExceptionsPanel;
    private javax.swing.JScrollPane peptidesExceptionsScrollPane;
    private javax.swing.JPanel peptidesManualValidationPanel;
    private javax.swing.JScrollPane peptidesManualValidationScrollPane;
    private javax.swing.JLabel piStatusPeptideLabel;
    private javax.swing.JPanel propertiesPanel;
    private javax.swing.JPanel proteinFilterPanel;
    private javax.swing.JLabel proteinLabel;
    private javax.swing.JLabel regExp2Label;
    private javax.swing.JLabel regExp3Label;
    private javax.swing.JLabel sequenceLabel;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JLabel spectraPeptidesLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Indicates whether the editing was canceled by the user.
     *
     * @return a boolean indicating whether the editing was canceled by the user
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Returns the filter.
     *
     * @return the filter
     */
    public PeptideFilter getFilter() {
        return peptideFilter;
    }

    /**
     * Modifies the filter according to the user selection
     */
    public void setFilter() {

        peptideFilter.setName(nameTxt.getText());
        peptideFilter.setDescription(descriptionTxt.getText());

        ArrayList<String> modifications = new ArrayList<String>();
        for (int row = 0; row < modificationTable.getRowCount(); row++) {
            if ((Boolean) modificationTable.getValueAt(row, 0)) {
                modifications.add((String) modificationTable.getValueAt(row, 2));
            }
        }
        peptideFilter.setModificationStatus(modifications);
        if (!peptideSequenceTxt.getText().trim().equals("")) {
            peptideFilter.setSequence(peptideSequenceTxt.getText().trim());
        }
        if (!peptideProteinTxt.getText().trim().equals("")) {
            peptideFilter.setProtein(peptideProteinTxt.getText().trim());
        }
        Integer pi = null;
        if (peptidePICmb.getSelectedIndex() != 0) {
            pi = peptidePICmb.getSelectedIndex() - 1;
        }
        if (pi != null) {
            peptideFilter.setPi(pi);
        }
        if (!peptideNSpectraTxt.getText().trim().equals("")) {
            peptideFilter.setNValidatedSpectra(new Integer(peptideNSpectraTxt.getText().trim()));
            peptideFilter.setnSpectraComparison(getComparisonType(peptideNSpectraCmb.getSelectedIndex()));
        }
        if (!peptideConfidenceTxt.getText().trim().equals("")) {
            peptideFilter.setPeptideConfidence(new Double(peptideConfidenceTxt.getText().trim()));
            peptideFilter.setPeptideConfidenceComparison(getComparisonType(peptideConfidenceCmb.getSelectedIndex()));
        }
        peptideFilter.setManualValidation(parseAccessions(peptideManualValidationTxt.getText()));
        peptideFilter.setExceptions(parseAccessions(peptideExceptionsTxt.getText()));
    }

    /**
     * Validates the input.
     *
     * @return a boolean indicating whether the input is valid
     */
    public boolean validateInput() {

        if (!peptideNSpectraTxt.getText().trim().equals("")) {
            try {
                new Integer(peptideNSpectraTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for peptide number of spectra.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!peptideConfidenceTxt.getText().trim().equals("")) {
            try {
                new Double(peptideConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for peptide confidence.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * Convenience method returning the combo box index based on a
     * ComparisonType.
     *
     * @param comparisonType the comparison type
     * @return the corresponding index to select
     */
    private int getComparisonIndex(RowFilter.ComparisonType comparisonType) {
        switch (comparisonType) {
            case EQUAL:
                return 0;
            case NOT_EQUAL:
                return 1;
            case BEFORE:
                return 2;
            case AFTER:
                return 3;
            default:
                return 0;
        }
    }

    /**
     * Convenience method returning the comparison type based on the selected
     * item in the smaller than, equals or greater than combo boxes.
     *
     * @param selectedItem the index of the item selected
     * @return the corresponding comparison type
     */
    private ComparisonType getComparisonType(int selectedItem) {
        switch (selectedItem) {
            case 0:
                return ComparisonType.EQUAL;
            case 1:
                return ComparisonType.NOT_EQUAL;
            case 2:
                return ComparisonType.BEFORE;
            case 3:
                return ComparisonType.AFTER;
            default:
                return ComparisonType.EQUAL;
        }
    }

    /**
     * Convenience method parsing keys in the manual validation/exception text
     * fields.
     *
     * @param text the text in the text field
     * @return a list of the parsed keys
     */
    private ArrayList<String> parseAccessions(String text) {
        ArrayList<String> result = new ArrayList<String>();
        String[] split = text.split(";"); //todo allow other separators
        for (String part : split) {
            if (!part.trim().equals("")) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /**
     * Indicates whether something was input for the filter.
     *
     * @return a boolean indicating whether something was input for the filter
     */
    public boolean hasInput() {
        if (!peptideProteinTxt.getText().trim().equals("")
                || !peptideSequenceTxt.getText().trim().equals("")
                || peptidePICmb.getSelectedIndex() > 0
                || !peptideNSpectraTxt.getText().trim().equals("")
                || !peptideConfidenceTxt.getText().trim().equals("")) {
            return true;
        }
        for (int row = 0; row < modificationTable.getRowCount(); row++) {
            if (!(Boolean) modificationTable.getValueAt(row, 0)) {
                return true;
            }
        }
        return false;
    }
}
