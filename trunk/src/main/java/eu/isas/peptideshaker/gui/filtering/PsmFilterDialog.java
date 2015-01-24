package eu.isas.peptideshaker.gui.filtering;

import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.filtering.AssumptionFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
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
public class PsmFilterDialog extends javax.swing.JDialog {

    /**
     * Boolean indicating whether the user canceled the filtering.
     */
    private boolean canceled = false;
    /**
     * The spectrum files table column header tooltips.
     */
    private ArrayList<String> spectrumFilesTableToolTips;
    /**
     * The original filter.
     */
    private PsmFilter psmFilter;

    /**
     * Creates a new ProteinFilterDialog.
     *
     * @param parent the parent frame
     * @param identificationParameters the identification parameters
     * @param spectrumFiles list of the loaded spectrum files
     */
    public PsmFilterDialog(java.awt.Frame parent, IdentificationParameters identificationParameters, ArrayList<String> spectrumFiles) {
        this(parent, null, identificationParameters, spectrumFiles);
    }

    /**
     * Creates a new ProteinFilterDialog.
     *
     * @param parent the parent frame
     * @param filter the protein filter to edit
     * @param identificationParameters the identification parameters
     * @param spectrumFiles list of the loaded spectrum files
     */
    public PsmFilterDialog(java.awt.Frame parent, PsmFilter filter, IdentificationParameters identificationParameters, ArrayList<String> spectrumFiles) {
        super(parent, true);
        initComponents();
        if (filter == null) {
            filter = new PsmFilter("New Filter");
        }
        this.psmFilter = filter;
        setUpGUI(filter, identificationParameters, spectrumFiles);

        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Sets up the GUI components.
     *
     * @param psmFilter the filter to use to populate the GUI
     * @param identificationParameters the identification parameters
     * @param spectrumFiles list of the loaded spectrum files
     */
    public void setUpGUI(PsmFilter psmFilter, IdentificationParameters identificationParameters, ArrayList<String> spectrumFiles) {

        nameTxt.setText(psmFilter.getName());
        descriptionTxt.setText(psmFilter.getDescription());

        precursorRTCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        precursorMzCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        precursorErrorCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        psmConfidenceCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        filesScrollPane.getViewport().setOpaque(false);

        if (spectrumFiles != null) {
            for (String fileName : spectrumFiles) {
                ((DefaultTableModel) spectrumFilesTable.getModel()).addRow(new Object[]{
                    true,
                    fileName
                });
            }
        } else {
            paramertersSplitPane.setDividerLocation(1.0);
        }

        spectrumFilesTableToolTips = new ArrayList<String>();
        spectrumFilesTableToolTips.add(null);
        spectrumFilesTableToolTips.add("File Name");

        spectrumFilesTable.getColumn(" ").setMaxWidth(30);
        spectrumFilesTable.getColumn(" ").setMinWidth(30);

        AssumptionFilter assumptionFilter = psmFilter.getAssumptionFilter();
        if (assumptionFilter.getPrecursorRT() != null) {
            precursorRTTxt.setText(assumptionFilter.getPrecursorRT() + "");
            precursorRTCmb.setSelectedIndex(getComparisonIndex(assumptionFilter.getPrecursorRTComparison()));
        }
        if (assumptionFilter.getPrecursorMz() != null) {
            precursorMzTxt.setText(assumptionFilter.getPrecursorMz() + "");
            precursorMzCmb.setSelectedIndex(getComparisonIndex(assumptionFilter.getPrecursorMzComparison()));
        }
        if (assumptionFilter.getPrecursorMzError() != null) {
            precursorErrorTxt.setText(assumptionFilter.getPrecursorMzError() + "");
            precursorErrorCmb.setSelectedIndex(getComparisonIndex(assumptionFilter.getPrecursorMzErrorComparison()));
        }
        if (psmFilter.getPsmConfidence() != null) {
            psmConfidenceTxt.setText(psmFilter.getPsmConfidence() + "");
            psmConfidenceCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPsmConfidenceComparison()));
        }

        charge1CheckBox.setSelected(false);
        charge2CheckBox.setSelected(false);
        charge3CheckBox.setSelected(false);
        charge4CheckBox.setSelected(false);
        chargeOver4CheckBox.setSelected(false);
        ArrayList<Integer> charges = psmFilter.getAssumptionFilter().getCharges();
        if (charges != null) {
            for (Integer charge : charges) {
                if (charge == 1) {
                    charge1CheckBox.setSelected(true);
                } else if (charge == 2) {
                    charge2CheckBox.setSelected(true);
                } else if (charge == 3) {
                    charge3CheckBox.setSelected(true);
                } else if (charge == 4) {
                    charge4CheckBox.setSelected(true);
                } else if (charge > 4) {
                    chargeOver4CheckBox.setSelected(true);
                }
            }
        }

        ArrayList<String> selectedFilteNames = assumptionFilter.getFileNames();
        if (selectedFilteNames != null) {
            for (int row = 0; row < spectrumFilesTable.getRowCount(); row++) {
                if (selectedFilteNames.contains(
                        (String) spectrumFilesTable.getValueAt(row, 1))) {
                    spectrumFilesTable.setValueAt(true, row, 0);
                } else {
                    spectrumFilesTable.setValueAt(false, row, 0);
                }
            }
        }

        IonMatch.MzErrorType[] possibleErrorTypes = IonMatch.MzErrorType.values();
        String[] units = new String[possibleErrorTypes.length];
        for (int i = 0; i < possibleErrorTypes.length; i++) {
            units[i] = possibleErrorTypes[i].unit;
        }
        errorUnitCmb.setModel(new DefaultComboBoxModel(units));

        String text = "";

        for (String key : psmFilter.getManualValidation()) {
            if (!text.equals("")) {
                text += "; ";
            }
            text += key;
        }

        psmManualValidationTxt.setText(text);
        text = "";

        for (String accession : psmFilter.getExceptions()) {
            if (!text.equals("")) {
                text += "; ";
            }
            text += accession;
        }

        psmExceptionsTxt.setText(text);
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
        propertiesPanel = new javax.swing.JPanel();
        nameLbl = new javax.swing.JLabel();
        nameTxt = new javax.swing.JTextField();
        descriptionLbl = new javax.swing.JLabel();
        descriptionScrollPane = new javax.swing.JScrollPane();
        descriptionTxt = new javax.swing.JTextArea();
        filterSettingsPanel = new javax.swing.JPanel();
        manualSelectionSplitPane = new javax.swing.JSplitPane();
        psmManualValidationPanel = new javax.swing.JPanel();
        psmManualValidationScrollPane = new javax.swing.JScrollPane();
        psmManualValidationTxt = new javax.swing.JTextArea();
        psmExceptionsPanel = new javax.swing.JPanel();
        psmExceptinosScrollPane = new javax.swing.JScrollPane();
        psmExceptionsTxt = new javax.swing.JTextArea();
        psmFilterParamsPanel = new javax.swing.JPanel();
        paramertersSplitPane = new javax.swing.JSplitPane();
        detailedParametersPanel = new javax.swing.JPanel();
        chargeLabel = new javax.swing.JLabel();
        psmConfidenceCmb = new javax.swing.JComboBox();
        chargeOver4CheckBox = new javax.swing.JCheckBox();
        psmConfidenceTxt = new javax.swing.JTextField();
        precursorMzLabel = new javax.swing.JLabel();
        precursorMzTxt = new javax.swing.JTextField();
        charge4CheckBox = new javax.swing.JCheckBox();
        charge2CheckBox = new javax.swing.JCheckBox();
        charge1CheckBox = new javax.swing.JCheckBox();
        charge3CheckBox = new javax.swing.JCheckBox();
        precursorMzCmb = new javax.swing.JComboBox();
        confidencePsmsLabel = new javax.swing.JLabel();
        precursorErrorLabel = new javax.swing.JLabel();
        precursorRtLabel = new javax.swing.JLabel();
        precursorErrorTxt = new javax.swing.JTextField();
        precursorErrorCmb = new javax.swing.JComboBox();
        precursorRTCmb = new javax.swing.JComboBox();
        precursorRTTxt = new javax.swing.JTextField();
        rtUnitLbl = new javax.swing.JLabel();
        errorUnitCmb = new javax.swing.JComboBox();
        mzUnitLbl = new javax.swing.JLabel();
        confidenceUnitLbl = new javax.swing.JLabel();
        spectrumFilesPanel = new javax.swing.JPanel();
        filesScrollPane = new javax.swing.JScrollPane();
        spectrumFilesTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) spectrumFilesTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        spectrumFilesLbl = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PSM Filters");

        proteinFilterPanel.setBackground(new java.awt.Color(230, 230, 230));

        filterSplitPane.setBorder(null);
        filterSplitPane.setDividerLocation(200);
        filterSplitPane.setDividerSize(0);
        filterSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        filterSplitPane.setOpaque(false);

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

        filterSettingsPanel.setOpaque(false);

        manualSelectionSplitPane.setBorder(null);
        manualSelectionSplitPane.setDividerLocation(425);
        manualSelectionSplitPane.setDividerSize(0);
        manualSelectionSplitPane.setOpaque(false);

        psmManualValidationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));
        psmManualValidationPanel.setOpaque(false);

        psmManualValidationScrollPane.setOpaque(false);

        psmManualValidationTxt.setColumns(20);
        psmManualValidationTxt.setRows(1);
        psmManualValidationScrollPane.setViewportView(psmManualValidationTxt);

        javax.swing.GroupLayout psmManualValidationPanelLayout = new javax.swing.GroupLayout(psmManualValidationPanel);
        psmManualValidationPanel.setLayout(psmManualValidationPanelLayout);
        psmManualValidationPanelLayout.setHorizontalGroup(
            psmManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE)
                .addContainerGap())
        );
        psmManualValidationPanelLayout.setVerticalGroup(
            psmManualValidationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmManualValidationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmManualValidationScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                .addContainerGap())
        );

        manualSelectionSplitPane.setLeftComponent(psmManualValidationPanel);

        psmExceptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Exceptions"));
        psmExceptionsPanel.setOpaque(false);

        psmExceptinosScrollPane.setOpaque(false);

        psmExceptionsTxt.setColumns(20);
        psmExceptionsTxt.setRows(1);
        psmExceptinosScrollPane.setViewportView(psmExceptionsTxt);

        javax.swing.GroupLayout psmExceptionsPanelLayout = new javax.swing.GroupLayout(psmExceptionsPanel);
        psmExceptionsPanel.setLayout(psmExceptionsPanelLayout);
        psmExceptionsPanelLayout.setHorizontalGroup(
            psmExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmExceptinosScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 807, Short.MAX_VALUE)
                .addContainerGap())
        );
        psmExceptionsPanelLayout.setVerticalGroup(
            psmExceptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmExceptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmExceptinosScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                .addContainerGap())
        );

        manualSelectionSplitPane.setRightComponent(psmExceptionsPanel);

        psmFilterParamsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));
        psmFilterParamsPanel.setOpaque(false);

        paramertersSplitPane.setBorder(null);
        paramertersSplitPane.setDividerLocation(700);
        paramertersSplitPane.setDividerSize(0);
        paramertersSplitPane.setOpaque(false);

        detailedParametersPanel.setOpaque(false);

        chargeLabel.setText("Charge");

        psmConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        psmConfidenceCmb.setSelectedIndex(3);

        chargeOver4CheckBox.setSelected(true);
        chargeOver4CheckBox.setText(">4");
        chargeOver4CheckBox.setIconTextGap(6);
        chargeOver4CheckBox.setOpaque(false);

        psmConfidenceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        psmConfidenceTxt.setToolTipText("Confidence [%]");

        precursorMzLabel.setText("Precursor m/z");
        precursorMzLabel.setToolTipText("Precursor m/z");

        precursorMzTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        charge4CheckBox.setSelected(true);
        charge4CheckBox.setText("4");
        charge4CheckBox.setIconTextGap(6);
        charge4CheckBox.setOpaque(false);

        charge2CheckBox.setSelected(true);
        charge2CheckBox.setText("2");
        charge2CheckBox.setIconTextGap(6);
        charge2CheckBox.setOpaque(false);

        charge1CheckBox.setSelected(true);
        charge1CheckBox.setText("1");
        charge1CheckBox.setIconTextGap(6);
        charge1CheckBox.setOpaque(false);

        charge3CheckBox.setSelected(true);
        charge3CheckBox.setText("3");
        charge3CheckBox.setIconTextGap(6);
        charge3CheckBox.setOpaque(false);

        precursorMzCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorMzCmb.setSelectedIndex(3);

        confidencePsmsLabel.setText("Confidence");
        confidencePsmsLabel.setToolTipText("Confidence [%]");

        precursorErrorLabel.setText("Precursor Error");
        precursorErrorLabel.setToolTipText("Precursor m/z Error");

        precursorRtLabel.setText("Precursor RT");
        precursorRtLabel.setToolTipText("Precursor Retention Time [s]");

        precursorErrorTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        precursorErrorTxt.setToolTipText("Precursor m/z Error");

        precursorErrorCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorErrorCmb.setSelectedIndex(3);

        precursorRTCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorRTCmb.setSelectedIndex(3);

        precursorRTTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        precursorRTTxt.setToolTipText("Precursor Retention Time [s]");

        rtUnitLbl.setText("s");

        errorUnitCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "m/z", "ppm", "%p" }));

        mzUnitLbl.setText("m/z");

        confidenceUnitLbl.setText("%");

        javax.swing.GroupLayout detailedParametersPanelLayout = new javax.swing.GroupLayout(detailedParametersPanel);
        detailedParametersPanel.setLayout(detailedParametersPanelLayout);
        detailedParametersPanelLayout.setHorizontalGroup(
            detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailedParametersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailedParametersPanelLayout.createSequentialGroup()
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(precursorMzLabel)
                            .addComponent(precursorErrorLabel)
                            .addComponent(confidencePsmsLabel)
                            .addComponent(precursorRtLabel))
                        .addGap(18, 18, 18)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(precursorMzCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorRTCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorErrorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(psmConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(10, 10, 10)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(psmConfidenceTxt)
                            .addComponent(precursorMzTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                            .addComponent(precursorErrorTxt)
                            .addComponent(precursorRTTxt, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(errorUnitCmb, 0, 48, Short.MAX_VALUE)
                                .addComponent(rtUnitLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(mzUnitLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(confidenceUnitLbl, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE))
                        .addContainerGap())
                    .addGroup(detailedParametersPanelLayout.createSequentialGroup()
                        .addComponent(chargeLabel)
                        .addGap(21, 21, 21)
                        .addComponent(charge1CheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(charge2CheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(charge3CheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(charge4CheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(chargeOver4CheckBox)
                        .addGap(45, 45, 45))))
        );
        detailedParametersPanelLayout.setVerticalGroup(
            detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailedParametersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(detailedParametersPanelLayout.createSequentialGroup()
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(precursorRTCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorRtLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(precursorMzCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorMzLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(precursorErrorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorErrorLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(psmConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(confidencePsmsLabel)))
                    .addGroup(detailedParametersPanelLayout.createSequentialGroup()
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(precursorRTTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(rtUnitLbl))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(precursorMzTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(mzUnitLbl))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(precursorErrorTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(errorUnitCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(psmConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(confidenceUnitLbl))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(detailedParametersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chargeLabel)
                    .addComponent(charge2CheckBox)
                    .addComponent(charge3CheckBox)
                    .addComponent(charge4CheckBox)
                    .addComponent(chargeOver4CheckBox)
                    .addComponent(charge1CheckBox))
                .addContainerGap(94, Short.MAX_VALUE))
        );

        paramertersSplitPane.setLeftComponent(detailedParametersPanel);

        spectrumFilesPanel.setOpaque(false);

        filesScrollPane.setOpaque(false);

        spectrumFilesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "File"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        filesScrollPane.setViewportView(spectrumFilesTable);

        spectrumFilesLbl.setText("Spectrum Files:");

        javax.swing.GroupLayout spectrumFilesPanelLayout = new javax.swing.GroupLayout(spectrumFilesPanel);
        spectrumFilesPanel.setLayout(spectrumFilesPanelLayout);
        spectrumFilesPanelLayout.setHorizontalGroup(
            spectrumFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumFilesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(spectrumFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(filesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
                    .addGroup(spectrumFilesPanelLayout.createSequentialGroup()
                        .addComponent(spectrumFilesLbl)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        spectrumFilesPanelLayout.setVerticalGroup(
            spectrumFilesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumFilesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumFilesLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(filesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                .addContainerGap())
        );

        paramertersSplitPane.setRightComponent(spectrumFilesPanel);

        javax.swing.GroupLayout psmFilterParamsPanelLayout = new javax.swing.GroupLayout(psmFilterParamsPanel);
        psmFilterParamsPanel.setLayout(psmFilterParamsPanelLayout);
        psmFilterParamsPanelLayout.setHorizontalGroup(
            psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmFilterParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(paramertersSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
        );
        psmFilterParamsPanelLayout.setVerticalGroup(
            psmFilterParamsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paramertersSplitPane)
        );

        javax.swing.GroupLayout filterSettingsPanelLayout = new javax.swing.GroupLayout(filterSettingsPanel);
        filterSettingsPanel.setLayout(filterSettingsPanelLayout);
        filterSettingsPanelLayout.setHorizontalGroup(
            filterSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmFilterParamsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(manualSelectionSplitPane)
        );
        filterSettingsPanelLayout.setVerticalGroup(
            filterSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(filterSettingsPanelLayout.createSequentialGroup()
                .addComponent(psmFilterParamsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manualSelectionSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE))
        );

        filterSplitPane.setRightComponent(filterSettingsPanel);

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
                    .addGroup(proteinFilterPanelLayout.createSequentialGroup()
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
    private javax.swing.JCheckBox charge1CheckBox;
    private javax.swing.JCheckBox charge2CheckBox;
    private javax.swing.JCheckBox charge3CheckBox;
    private javax.swing.JCheckBox charge4CheckBox;
    private javax.swing.JLabel chargeLabel;
    private javax.swing.JCheckBox chargeOver4CheckBox;
    private javax.swing.JLabel confidencePsmsLabel;
    private javax.swing.JLabel confidenceUnitLbl;
    private javax.swing.JLabel descriptionLbl;
    private javax.swing.JScrollPane descriptionScrollPane;
    private javax.swing.JTextArea descriptionTxt;
    private javax.swing.JPanel detailedParametersPanel;
    private javax.swing.JComboBox errorUnitCmb;
    private javax.swing.JScrollPane filesScrollPane;
    private javax.swing.JPanel filterSettingsPanel;
    private javax.swing.JSplitPane filterSplitPane;
    private javax.swing.JSplitPane manualSelectionSplitPane;
    private javax.swing.JLabel mzUnitLbl;
    private javax.swing.JLabel nameLbl;
    private javax.swing.JTextField nameTxt;
    private javax.swing.JButton okButton;
    private javax.swing.JSplitPane paramertersSplitPane;
    private javax.swing.JComboBox precursorErrorCmb;
    private javax.swing.JLabel precursorErrorLabel;
    private javax.swing.JTextField precursorErrorTxt;
    private javax.swing.JComboBox precursorMzCmb;
    private javax.swing.JLabel precursorMzLabel;
    private javax.swing.JTextField precursorMzTxt;
    private javax.swing.JComboBox precursorRTCmb;
    private javax.swing.JTextField precursorRTTxt;
    private javax.swing.JLabel precursorRtLabel;
    private javax.swing.JPanel propertiesPanel;
    private javax.swing.JPanel proteinFilterPanel;
    private javax.swing.JComboBox psmConfidenceCmb;
    private javax.swing.JTextField psmConfidenceTxt;
    private javax.swing.JScrollPane psmExceptinosScrollPane;
    private javax.swing.JPanel psmExceptionsPanel;
    private javax.swing.JTextArea psmExceptionsTxt;
    private javax.swing.JPanel psmFilterParamsPanel;
    private javax.swing.JPanel psmManualValidationPanel;
    private javax.swing.JScrollPane psmManualValidationScrollPane;
    private javax.swing.JTextArea psmManualValidationTxt;
    private javax.swing.JLabel rtUnitLbl;
    private javax.swing.JLabel spectrumFilesLbl;
    private javax.swing.JPanel spectrumFilesPanel;
    private javax.swing.JTable spectrumFilesTable;
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
    public PsmFilter getFilter() {
        return psmFilter;
    }

    /**
     * Modifies the filter according to the user selection.
     */
    public void setFilter() {

        psmFilter.setName(nameTxt.getText());
        psmFilter.setDescription(descriptionTxt.getText());

        ArrayList<Integer> charges = new ArrayList<Integer>();

        if (charge2CheckBox.isSelected()) {
            charges.add(2);
        }
        if (charge3CheckBox.isSelected()) {
            charges.add(3);
        }
        if (charge4CheckBox.isSelected()) {
            charges.add(4);
        }
        if (chargeOver4CheckBox.isSelected()) {
            charges.add(5);
        }

        ArrayList<String> files = new ArrayList<String>();
        for (int row = 0; row < spectrumFilesTable.getRowCount(); row++) {
            if ((Boolean) spectrumFilesTable.getValueAt(row, 0)) {
                files.add((String) spectrumFilesTable.getValueAt(row, 1));
            }
        }

        AssumptionFilter assumptionFilter = psmFilter.getAssumptionFilter();
        if (!charges.isEmpty()) {
            assumptionFilter.setCharges(charges);
        }
        assumptionFilter.setFileNames(files);
        if (!precursorRTTxt.getText().trim().equals("")) {
            assumptionFilter.setPrecursorRT(new Double(precursorRTTxt.getText().trim()));
            assumptionFilter.setPrecursorRTComparison(getComparisonType(precursorRTCmb.getSelectedIndex()));
        }
        if (!precursorMzTxt.getText().trim().equals("")) {
            assumptionFilter.setPrecursorMz(new Double(precursorMzTxt.getText().trim()));
            assumptionFilter.setPrecursorMzComparison(getComparisonType(precursorMzCmb.getSelectedIndex()));
            assumptionFilter.setPrecursorMzErrorType(IonMatch.MzErrorType.getMzErrorType(errorUnitCmb.getSelectedIndex()));
        }
        if (!precursorErrorTxt.getText().trim().equals("")) {
            assumptionFilter.setPrecursorMzError(new Double(precursorErrorTxt.getText().trim()));
            assumptionFilter.setPrecursorMzErrorComparison(getComparisonType(precursorErrorCmb.getSelectedIndex()));
            assumptionFilter.setPrecursorMzErrorType(IonMatch.MzErrorType.getMzErrorType(errorUnitCmb.getSelectedIndex()));
        }
        if (!psmConfidenceTxt.getText().trim().equals("")) {
            psmFilter.setPsmConfidence(new Double(psmConfidenceTxt.getText().trim()));
            psmFilter.setPsmConfidenceComparison(getComparisonType(psmConfidenceCmb.getSelectedIndex()));
        }
        psmFilter.setManualValidation(parseAccessions(psmManualValidationTxt.getText()));
        psmFilter.setExceptions(parseAccessions(psmExceptionsTxt.getText()));

    }

    /**
     * Validates the input.
     *
     * @return a boolean indicating whether the input is valid
     */
    public boolean validateInput() {
        if (!precursorRTTxt.getText().trim().equals("")) {
            try {
                new Double(precursorRTTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor retention time.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!precursorMzTxt.getText().trim().equals("")) {
            try {
                new Double(precursorMzTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor m/z.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!precursorErrorTxt.getText().trim().equals("")) {
            try {
                new Double(precursorErrorTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor error.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!psmConfidenceTxt.getText().trim().equals("")) {
            try {
                new Double(psmConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for PSM confidence.",
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
        if (!precursorRTTxt.getText().trim().equals("")
                || !precursorMzTxt.getText().trim().equals("")
                || !precursorErrorTxt.getText().trim().equals("")
                || !psmConfidenceTxt.getText().trim().equals("")) {
            return true;
        }

        if (charge2CheckBox.isSelected()
                || charge3CheckBox.isSelected()
                || charge4CheckBox.isSelected()
                || chargeOver4CheckBox.isSelected()) {
            return true;
        }

        for (int row = 0; row < spectrumFilesTable.getRowCount(); row++) {
            if (!(Boolean) spectrumFilesTable.getValueAt(row, 0)) {
                return true;
            }
        }
        return false;
    }
}
