package eu.isas.peptideshaker.gui.filtering;

import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import javax.swing.SwingConstants;

/**
 * Dialog to edit protein filters.
 *
 * @author Marc Vaudel
 */
public class ProteinFilterDialog extends javax.swing.JDialog {

    /**
     * Boolean indicating whether the user canceled the filtering.
     */
    private boolean canceled = false;
    /**
     * The original filter
     */
    private ProteinFilter proteinFilter;

    /**
     * Creates a new ProteinFilterDialog.
     *
     * @param parent the parent frame
     */
    public ProteinFilterDialog(java.awt.Frame parent) {
        this(parent, null);
    }

    /**
     * Creates a new ProteinFilterDialog.
     *
     * @param parent the parent frame
     * @param filter the protein filter to edit
     */
    public ProteinFilterDialog(java.awt.Frame parent, ProteinFilter filter) {
        super(parent, true);
        initComponents();
        if (filter == null) {
            filter = new ProteinFilter("New Filter");
        }
        this.proteinFilter = filter;
        setUpGUI(filter);

        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Sets up the GUI components.
     *
     * @param proteinFilter the filter to use to populate the GUI
     */
    public void setUpGUI(ProteinFilter proteinFilter) {

        nameTxt.setText(proteinFilter.getName());
        descriptionTxt.setText(proteinFilter.getDescription());

        proteinPICmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinPiComparisonCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        spectrumCountingCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinCoverageCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        nPeptidesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinNSpectraCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinScoreCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proteinConfidenceCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        if (proteinFilter.getIdentifierRegex() != null) {
            proteinAccessionTxt.setText(proteinFilter.getIdentifierRegex());
        }
        if (proteinFilter.getPi() < 5) {
            proteinPICmb.setSelectedIndex(proteinFilter.getPi() + 1);
            proteinPiComparisonCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getPiComparison()));
        }
        if (proteinFilter.getSpectrumCounting() != null) {
            spectrumCountingTxt.setText(proteinFilter.getSpectrumCounting() + "");
            spectrumCountingCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getSpectrumCountingComparison()));
        }
        if (proteinFilter.getProteinCoverage() != null) {
            proteinCoverageTxt.setText(proteinFilter.getProteinCoverage() + "");
            proteinCoverageCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getProteinCoverageComparison()));
        }
        if (proteinFilter.getnConfidentPeptides() != null) {
            nPeptidesTxt.setText(proteinFilter.getnPeptides() + "");
            nPeptidesCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getnPeptidesComparison()));
        }
        if (proteinFilter.getProteinNConfidentSpectra() != null) {
            proteinsNSpectraTxt.setText(proteinFilter.getProteinNSpectra()+ "");
            proteinNSpectraCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getnSpectraComparison()));
        }
        if (proteinFilter.getProteinScore() != null) {
            proteinScoreTxt.setText(proteinFilter.getProteinScore() + "");
            proteinScoreCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getProteinScoreComparison()));
        }
        if (proteinFilter.getProteinConfidence() != null) {
            proteinConfidenceTxt.setText(proteinFilter.getProteinConfidence() + "");
            proteinConfidenceCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getProteinConfidenceComparison()));
        }

        boolean first = true;
        String text = "";

        for (String key : proteinFilter.getManualValidation()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += key;
        }

        proteinManualValidationTxt.setText(text);
        first = true;
        text = "";

        for (String key : proteinFilter.getExceptions()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += key;
        }

        proteinExceptionsTxt.setText(text);

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
        proteinFilterParamsPanel = new javax.swing.JPanel();
        identifierLabel = new javax.swing.JLabel();
        piStatusLabel = new javax.swing.JLabel();
        ms2QuantLabel = new javax.swing.JLabel();
        peptidesLabel = new javax.swing.JLabel();
        spectraLabel = new javax.swing.JLabel();
        scoreLabel = new javax.swing.JLabel();
        coverageLabel = new javax.swing.JLabel();
        confidenceLabel = new javax.swing.JLabel();
        spectrumCountingTxt = new javax.swing.JTextField();
        proteinCoverageCmb = new javax.swing.JComboBox();
        regExpLabel = new javax.swing.JLabel();
        proteinAccessionTxt = new javax.swing.JTextField();
        proteinPICmb = new javax.swing.JComboBox();
        proteinConfidenceTxt = new javax.swing.JTextField();
        proteinConfidenceCmb = new javax.swing.JComboBox();
        proteinScoreTxt = new javax.swing.JTextField();
        proteinScoreCmb = new javax.swing.JComboBox();
        proteinsNSpectraTxt = new javax.swing.JTextField();
        proteinNSpectraCmb = new javax.swing.JComboBox();
        nPeptidesTxt = new javax.swing.JTextField();
        nPeptidesCmb = new javax.swing.JComboBox();
        proteinCoverageTxt = new javax.swing.JTextField();
        spectrumCountingCmb = new javax.swing.JComboBox();
        proteinPiComparisonCmb = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        manualSelectionSplitPane = new javax.swing.JSplitPane();
        proteinExceptionsPanel = new javax.swing.JPanel();
        proteinExceptionsJScrollPane = new javax.swing.JScrollPane();
        proteinExceptionsTxt = new javax.swing.JTextArea();
        proteinManualValidationPanel = new javax.swing.JPanel();
        proteinManualValidationScrollPane = new javax.swing.JScrollPane();
        proteinManualValidationTxt = new javax.swing.JTextArea();
        propertiesPanel = new javax.swing.JPanel();
        nameLbl = new javax.swing.JLabel();
        nameTxt = new javax.swing.JTextField();
        descriptionLbl = new javax.swing.JLabel();
        descriptionScrollPane = new javax.swing.JScrollPane();
        descriptionTxt = new javax.swing.JTextArea();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Protein Filters");

        proteinFilterPanel.setBackground(new java.awt.Color(230, 230, 230));

        filterSplitPane.setBorder(null);
        filterSplitPane.setDividerLocation(200);
        filterSplitPane.setDividerSize(0);
        filterSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        filterSplitPane.setOpaque(false);

        settingsPanel.setOpaque(false);

        proteinFilterParamsPanel.setBackground(new java.awt.Color(230, 230, 230));
        proteinFilterParamsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));
        proteinFilterParamsPanel.setOpaque(false);

        identifierLabel.setText("Identifier");

        piStatusLabel.setText("PI Status");
        piStatusLabel.setToolTipText("Protein Inference Status");

        ms2QuantLabel.setText("MS2 Quant.");
        ms2QuantLabel.setToolTipText("MS2 Quantification [fmol]");

        peptidesLabel.setText("#Peptides");
        peptidesLabel.setToolTipText("Number of confident peptides");

        spectraLabel.setText("#PSMs");
        spectraLabel.setToolTipText("Number of Confident PSMs");

        scoreLabel.setText("Score");
        scoreLabel.setToolTipText("Protein Score");

        coverageLabel.setText("Coverage");
        coverageLabel.setToolTipText("Sequence Coverage [%]");

        confidenceLabel.setText("Confidence");
        confidenceLabel.setToolTipText("Confidence [%]");

        spectrumCountingTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        spectrumCountingTxt.setToolTipText("MS2 Quantification [fmol]");

        proteinCoverageCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinCoverageCmb.setSelectedIndex(3);

        regExpLabel.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        regExpLabel.setText("RegExp");
        regExpLabel.setToolTipText("<html>\nRegular Expression<br>\nexample: N[^P][ST] returns all sequences with:<br>\nan N, then anything but a P, and then an S or a T\n</html>");

        proteinAccessionTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        proteinPICmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Requirement", "Single Protein", "Related Proteins", "Related and Unrelated Proteins", "Unrelated Proteins" }));
        proteinPICmb.setToolTipText("Protein Inference Status");

        proteinConfidenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinConfidenceTxt.setToolTipText("Confidence [%]");

        proteinConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinConfidenceCmb.setSelectedIndex(3);

        proteinScoreTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinScoreTxt.setToolTipText("Protein Score");

        proteinScoreCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinScoreCmb.setSelectedIndex(3);

        proteinsNSpectraTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinsNSpectraTxt.setToolTipText("Number of Confident PSMs");

        proteinNSpectraCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinNSpectraCmb.setSelectedIndex(3);

        nPeptidesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nPeptidesTxt.setToolTipText("Number of confident peptides");

        nPeptidesCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        nPeptidesCmb.setSelectedIndex(3);

        proteinCoverageTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        proteinCoverageTxt.setToolTipText("Sequence Coverage [%]");

        spectrumCountingCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        spectrumCountingCmb.setSelectedIndex(3);

        proteinPiComparisonCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=" }));

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout proteinFilterParamsPanelLayout = new javax.swing.GroupLayout(proteinFilterParamsPanel);
        proteinFilterParamsPanel.setLayout(proteinFilterParamsPanelLayout);
        proteinFilterParamsPanelLayout.setHorizontalGroup(
            proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ms2QuantLabel)
                    .addComponent(coverageLabel)
                    .addComponent(piStatusLabel)
                    .addComponent(identifierLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinAccessionTxt)
                    .addComponent(proteinPICmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(spectrumCountingTxt)
                    .addComponent(proteinCoverageTxt))
                .addGap(18, 18, 18)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(proteinPiComparisonCmb, 0, 48, Short.MAX_VALUE)
                    .addComponent(spectrumCountingCmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(proteinCoverageCmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(regExpLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spectraLabel)
                    .addComponent(peptidesLabel)
                    .addComponent(scoreLabel)
                    .addComponent(confidenceLabel))
                .addGap(23, 23, 23)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nPeptidesTxt)
                    .addComponent(proteinsNSpectraTxt)
                    .addComponent(proteinScoreTxt)
                    .addComponent(proteinConfidenceTxt, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nPeptidesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinScoreCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        proteinFilterParamsPanelLayout.setVerticalGroup(
            proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nPeptidesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(proteinFilterParamsPanelLayout.createSequentialGroup()
                                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(nPeptidesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(peptidesLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(proteinsNSpectraTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spectraLabel)
                                    .addComponent(proteinNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(proteinScoreTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(scoreLabel)
                                    .addComponent(proteinScoreCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(proteinConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(proteinConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(proteinFilterParamsPanelLayout.createSequentialGroup()
                            .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(regExpLabel)
                                .addComponent(proteinAccessionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(identifierLabel))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(proteinPICmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(piStatusLabel)
                                .addComponent(proteinPiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(ms2QuantLabel)
                                .addComponent(spectrumCountingTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(spectrumCountingCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(proteinFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(proteinCoverageTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(coverageLabel)
                                .addComponent(proteinCoverageCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(confidenceLabel)))))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        manualSelectionSplitPane.setBorder(null);
        manualSelectionSplitPane.setDividerLocation(476);
        manualSelectionSplitPane.setDividerSize(0);
        manualSelectionSplitPane.setResizeWeight(0.5);
        manualSelectionSplitPane.setToolTipText("");
        manualSelectionSplitPane.setOpaque(false);

        proteinExceptionsPanel.setBackground(new java.awt.Color(230, 230, 230));
        proteinExceptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Exceptions"));
        proteinExceptionsPanel.setOpaque(false);

        proteinExceptionsTxt.setColumns(20);
        proteinExceptionsTxt.setRows(1);
        proteinExceptionsJScrollPane.setViewportView(proteinExceptionsTxt);

        javax.swing.GroupLayout proteinExceptionsPanelLayout = new javax.swing.GroupLayout(proteinExceptionsPanel);
        proteinExceptionsPanel.setLayout(proteinExceptionsPanelLayout);
        proteinExceptionsPanelLayout.setHorizontalGroup(
            proteinExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinExceptionsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinExceptionsPanelLayout.setVerticalGroup(
            proteinExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinExceptionsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                .addContainerGap())
        );

        manualSelectionSplitPane.setRightComponent(proteinExceptionsPanel);

        proteinManualValidationPanel.setBackground(new java.awt.Color(230, 230, 230));
        proteinManualValidationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));
        proteinManualValidationPanel.setOpaque(false);

        proteinManualValidationTxt.setColumns(20);
        proteinManualValidationTxt.setRows(1);
        proteinManualValidationScrollPane.setViewportView(proteinManualValidationTxt);

        javax.swing.GroupLayout proteinManualValidationPanelLayout = new javax.swing.GroupLayout(proteinManualValidationPanel);
        proteinManualValidationPanel.setLayout(proteinManualValidationPanelLayout);
        proteinManualValidationPanelLayout.setHorizontalGroup(
            proteinManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinManualValidationPanelLayout.setVerticalGroup(
            proteinManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                .addContainerGap())
        );

        manualSelectionSplitPane.setLeftComponent(proteinManualValidationPanel);

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinFilterParamsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(manualSelectionSplitPane, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addComponent(proteinFilterParamsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manualSelectionSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE))
        );

        filterSplitPane.setRightComponent(settingsPanel);

        propertiesPanel.setBackground(new java.awt.Color(230, 230, 230));
        propertiesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Properties"));
        propertiesPanel.setOpaque(false);

        nameLbl.setText("Name");

        descriptionLbl.setText("Description");

        descriptionTxt.setColumns(20);
        descriptionTxt.setRows(5);
        descriptionScrollPane.setViewportView(descriptionTxt);

        javax.swing.GroupLayout propertiesPanelLayout = new javax.swing.GroupLayout(propertiesPanel);
        propertiesPanel.setLayout(propertiesPanelLayout);
        propertiesPanelLayout.setHorizontalGroup(
            propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(propertiesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(propertiesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(descriptionScrollPane)
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
                .addComponent(descriptionScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
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
                .addGap(11, 11, 11)
                .addGroup(proteinFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(proteinFilterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(1, 1, 1))
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
    private javax.swing.JLabel confidenceLabel;
    private javax.swing.JLabel coverageLabel;
    private javax.swing.JLabel descriptionLbl;
    private javax.swing.JScrollPane descriptionScrollPane;
    private javax.swing.JTextArea descriptionTxt;
    private javax.swing.JSplitPane filterSplitPane;
    private javax.swing.JLabel identifierLabel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSplitPane manualSelectionSplitPane;
    private javax.swing.JLabel ms2QuantLabel;
    private javax.swing.JComboBox nPeptidesCmb;
    private javax.swing.JTextField nPeptidesTxt;
    private javax.swing.JLabel nameLbl;
    private javax.swing.JTextField nameTxt;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel peptidesLabel;
    private javax.swing.JLabel piStatusLabel;
    private javax.swing.JPanel propertiesPanel;
    private javax.swing.JTextField proteinAccessionTxt;
    private javax.swing.JComboBox proteinConfidenceCmb;
    private javax.swing.JTextField proteinConfidenceTxt;
    private javax.swing.JComboBox proteinCoverageCmb;
    private javax.swing.JTextField proteinCoverageTxt;
    private javax.swing.JScrollPane proteinExceptionsJScrollPane;
    private javax.swing.JPanel proteinExceptionsPanel;
    private javax.swing.JTextArea proteinExceptionsTxt;
    private javax.swing.JPanel proteinFilterPanel;
    private javax.swing.JPanel proteinFilterParamsPanel;
    private javax.swing.JPanel proteinManualValidationPanel;
    private javax.swing.JScrollPane proteinManualValidationScrollPane;
    private javax.swing.JTextArea proteinManualValidationTxt;
    private javax.swing.JComboBox proteinNSpectraCmb;
    private javax.swing.JComboBox proteinPICmb;
    private javax.swing.JComboBox proteinPiComparisonCmb;
    private javax.swing.JComboBox proteinScoreCmb;
    private javax.swing.JTextField proteinScoreTxt;
    private javax.swing.JTextField proteinsNSpectraTxt;
    private javax.swing.JLabel regExpLabel;
    private javax.swing.JLabel scoreLabel;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JLabel spectraLabel;
    private javax.swing.JComboBox spectrumCountingCmb;
    private javax.swing.JTextField spectrumCountingTxt;
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
    public ProteinFilter getFilter() {
        return proteinFilter;
    }

    /**
     * Modifies the filter according to the user selection.
     */
    public void setFilter() {

        proteinFilter.setName(nameTxt.getText());
        proteinFilter.setDescription(descriptionTxt.getText());

        Integer pi = null;
        if (proteinPICmb.getSelectedIndex() != 0) {
            pi = proteinPICmb.getSelectedIndex() - 1;
        }
        if (!proteinAccessionTxt.getText().trim().equals("")) {
            proteinFilter.setIdentifierRegex(proteinAccessionTxt.getText().trim());
        }
        if (pi != null) {
            proteinFilter.setPi(pi);
            proteinFilter.setPiComparison(getComparisonType(proteinPiComparisonCmb.getSelectedIndex()));
        }
        if (!spectrumCountingTxt.getText().trim().equals("")) {
            proteinFilter.setSpectrumCounting(new Double(spectrumCountingTxt.getText().trim()));
            proteinFilter.setSpectrumCountingComparison(getComparisonType(spectrumCountingCmb.getSelectedIndex()));
        }
        if (!proteinCoverageTxt.getText().trim().equals("")) {
            proteinFilter.setProteinCoverage(new Double(proteinCoverageTxt.getText().trim()));
            proteinFilter.setProteinCoverageComparison(getComparisonType(proteinCoverageCmb.getSelectedIndex()));
        }
        if (!nPeptidesTxt.getText().trim().equals("")) {
            proteinFilter.setnConfidentPeptides(new Integer(nPeptidesTxt.getText().trim()));
            proteinFilter.setnPeptidesComparison(getComparisonType(nPeptidesCmb.getSelectedIndex()));
        }
        if (!proteinsNSpectraTxt.getText().trim().equals("")) {
            proteinFilter.setProteinNConfidentSpectra(new Integer(proteinsNSpectraTxt.getText().trim()));
            proteinFilter.setnSpectraComparison(getComparisonType(proteinNSpectraCmb.getSelectedIndex()));
        }
        if (!proteinScoreTxt.getText().trim().equals("")) {
            proteinFilter.setProteinScore(new Double(proteinScoreTxt.getText().trim()));
            proteinFilter.setProteinScoreComparison(getComparisonType(proteinScoreCmb.getSelectedIndex()));
        }
        if (!proteinConfidenceTxt.getText().trim().equals("")) {
            proteinFilter.setProteinConfidence(new Double(proteinConfidenceTxt.getText().trim()));
            proteinFilter.setProteinConfidenceComparison(getComparisonType(proteinConfidenceCmb.getSelectedIndex()));
        }

        proteinFilter.setManualValidation(parseAccessions(proteinManualValidationTxt.getText()));
        proteinFilter.setExceptions(parseAccessions(proteinExceptionsTxt.getText()));

    }

    /**
     * Validates the input.
     *
     * @return a boolean indicating whether the input is valid
     */
    public boolean validateInput() {
        if (!proteinCoverageTxt.getText().trim().equals("")) {
            try {
                new Double(proteinCoverageTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein coverage.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!spectrumCountingTxt.getText().trim().equals("")) {
            try {
                new Double(spectrumCountingTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for spectrum counting.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!nPeptidesTxt.getText().trim().equals("")) {
            try {
                new Integer(nPeptidesTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for number of peptides.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinsNSpectraTxt.getText().trim().equals("")) {
            try {
                new Integer(proteinsNSpectraTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein number of spectra.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinScoreTxt.getText().trim().equals("")) {
            try {
                new Double(proteinScoreTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein score.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinConfidenceTxt.getText().trim().equals("")) {
            try {
                new Double(proteinConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein confidence.",
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
     * 
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
     * 
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
    private boolean hasInput() {
        return !proteinAccessionTxt.getText().trim().equals("")
                || proteinPICmb.getSelectedIndex() > 0
                || !spectrumCountingTxt.getText().trim().equals("")
                || !proteinCoverageTxt.getText().trim().equals("")
                || !nPeptidesTxt.getText().trim().equals("")
                || !proteinsNSpectraTxt.getText().trim().equals("")
                || !proteinScoreTxt.getText().trim().equals("")
                || !proteinConfidenceTxt.getText().trim().equals("");
    }
}
