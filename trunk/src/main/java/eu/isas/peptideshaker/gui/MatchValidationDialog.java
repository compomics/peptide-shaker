package eu.isas.peptideshaker.gui;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import eu.isas.peptideshaker.validation.MatchesValidator;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class displays information about the validation of a match.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class MatchValidationDialog extends javax.swing.JDialog {

    /**
     * The key of the match.
     */
    private String matchKey;
    /**
     * The ps parameter of the match.
     */
    private PSParameter psParameter;
    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The identification feature generator.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The exception handler.
     */
    private FrameExceptionHandler exceptionHandler;
    /**
     * Indicates whether the validation status of the match changed.
     */
    private boolean validationChanged = false;
    /**
     * The type of match selected.
     */
    private Type type;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The color to use when writing in green.
     */
    private static final Color green = new Color(0, 125, 0);
    /**
     * The color to use when writing in orange.
     */
    private static final Color orange = new Color(220, 110, 0);
    /**
     * Information on the protocol,
     */
    private ShotgunProtocol shotgunProtocol;
    /**
     * the identification parameters used,
     */
    private IdentificationParameters identificationParameters;
    /**
     * The filter table column header tooltips.
     */
    private ArrayList<String> validationTableToolTips;

    /**
     * Type of match selected.
     */
    private enum Type {

        PROTEIN,
        PEPTIDE,
        PSM
    }

    /**
     * Constructor for a protein match validation dialog.
     *
     * @param parent the parent frame
     * @param exceptionHandler the handler catches exception happening when
     * filling the table
     * @param identification the identification where to get the match from
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param proteinMap the protein map
     * @param proteinMatchKey the protein match key
     * @param shotgunProtocol identification on the protocol used
     * @param identificationParameters the identification parameters used
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public MatchValidationDialog(java.awt.Frame parent, FrameExceptionHandler exceptionHandler, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ProteinMap proteinMap, String proteinMatchKey, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        super(parent, true);
        initComponents();
        setUpGui();

        this.matchKey = proteinMatchKey;
        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, new PSParameter());
        this.exceptionHandler = exceptionHandler;
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.shotgunProtocol = shotgunProtocol;
        this.identificationParameters = identificationParameters;

        type = Type.PROTEIN;

        TargetDecoyMap targetDecoyMap = proteinMap.getTargetDecoyMap();
        populateGUI(targetDecoyMap, "Proteins");

        setTitle("Protein Group Validation Quality");

        setLocationRelativeTo(parent);
        setVisible(true);

        if (!psParameter.hasQcFilters()) {
            JOptionPane.showMessageDialog(null,
                    "No Quality Control filters was implemented in the PeptideShaker version used to create this project.", "No QC Filter",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Creates a peptide match validation dialog.
     *
     * @param parent the parent frame
     * @param exceptionHandler the handler catches exception happening when
     * filling the table
     * @param identification the identification where to get the match from
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param peptideSpecificMap the peptide specific target decoy map
     * @param peptideMatchKey the peptide match key
     * @param shotgunProtocol identification on the protocol used
     * @param identificationParameters the identification parameters used
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public MatchValidationDialog(java.awt.Frame parent, FrameExceptionHandler exceptionHandler, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            PeptideSpecificMap peptideSpecificMap, String peptideMatchKey, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        super(parent, true);
        initComponents();
        setUpGui();

        this.matchKey = peptideMatchKey;
        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideMatchKey, new PSParameter());
        this.exceptionHandler = exceptionHandler;
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.shotgunProtocol = shotgunProtocol;
        this.identificationParameters = identificationParameters;
        type = Type.PEPTIDE;

        String peptideGroupKey = psParameter.getSpecificMapKey();
        TargetDecoyMap targetDecoyMap = peptideSpecificMap.getTargetDecoyMap(peptideSpecificMap.getCorrectedKey(peptideGroupKey));
        String groupName = "";
        if (peptideSpecificMap.getKeys().size() > 1) {
            groupName += PeptideSpecificMap.getKeyName(identificationParameters.getSearchParameters().getModificationProfile(), peptideGroupKey) + " ";
        }
        groupName += "Peptides";
        populateGUI(targetDecoyMap, groupName);

        setTitle("Peptide Validation Quality");

        setLocationRelativeTo(parent);
        setVisible(true);

        if (!psParameter.hasQcFilters()) {
            JOptionPane.showMessageDialog(null,
                    "No Quality Control filters was implemented in the PeptideShaker version used to create this project.", "No QC Filter",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Creates a PSM match validation dialog.
     *
     * @param parent the parent frame
     * @param exceptionHandler the handler catches exception happening when
     * filling the table
     * @param identification the identification where to get the match from
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param psmSpecificMap the PSM specific target decoy map
     * @param psmMatchKey the spectrum match key
     * @param shotgunProtocol identification on the protocol used
     * @param identificationParameters the identification parameters used
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    public MatchValidationDialog(java.awt.Frame parent, FrameExceptionHandler exceptionHandler, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            PsmSpecificMap psmSpecificMap, String psmMatchKey, ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        super(parent, true);
        initComponents();
        setUpGui();

        this.matchKey = psmMatchKey;
        psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmMatchKey, new PSParameter());
        this.exceptionHandler = exceptionHandler;
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.shotgunProtocol = shotgunProtocol;
        this.identificationParameters = identificationParameters;
        type = Type.PSM;

        String fileName = Spectrum.getSpectrumFile(psmMatchKey);
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmMatchKey);
        Integer charge;
        if (spectrumMatch.getBestPeptideAssumption() != null) {
            charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
        } else if (spectrumMatch.getBestTagAssumption() != null) {
            charge = spectrumMatch.getBestTagAssumption().getIdentificationCharge().value;
        } else {
            throw new IllegalArgumentException("No best match found for spectrum " + psmMatchKey + ".");
        }

        TargetDecoyMap targetDecoyMap = psmSpecificMap.getTargetDecoyMap(charge, fileName);
        String groupName = "Charge ";
        if (!psmSpecificMap.isFileGrouped(charge, fileName)) {
            groupName += charge + " in file " + fileName;
        } else {
            int correctedCharge = psmSpecificMap.getCorrectedCharge(charge);
            groupName += correctedCharge;
        }
        groupName += " PSMs";
        populateGUI(targetDecoyMap, groupName);

        setTitle("PSM Validation Quality");

        setLocationRelativeTo(parent);
        setVisible(true);

        if (!psParameter.hasQcFilters()) {
            JOptionPane.showMessageDialog(null,
                    "No Quality Control filters was implemented in the PeptideShaker version used to create this project.", "No QC Filter",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {
        // make sure that the scroll panes are see-through
        qualityFiltersTableScrollPane.getViewport().setOpaque(false);

        // disable column reordering
        qualityFiltersTable.getTableHeader().setReorderingAllowed(false);

        validationLevelJComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        // set up the table header tooltips
        validationTableToolTips = new ArrayList<String>(3);
        validationTableToolTips.add(null);
        validationTableToolTips.add("Quality Test");
        validationTableToolTips.add("Passed");
    }

    /**
     * Populates the GUI with information on a protein match.
     *
     * @param identificationFeaturesGenerator
     * @param proteinMap
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     * @throws MzMLUnmarshallerException thrown if an MzMLUnmarshallerException
     * occurs
     */
    private void populateGUI(TargetDecoyMap targetDecoyMap, String targetDecoyCategory)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        // Validation level
        validationLevelJComboBox.setSelectedItem(psParameter.getMatchValidationLevel().getName());

        // Database info
        boolean targetDecoy = sequenceFactory.concatenatedTargetDecoy();
        if (!targetDecoy) {
            targetDecoyLbl.setText("Target only");
            targetDecoyLbl.setForeground(Color.red);
        } else {
            targetDecoyLbl.setForeground(green);
        }
        int nTarget = sequenceFactory.getNTargetSequences();
        nTargetLbl.setText(nTarget + " target sequences");
        if (nTarget < 10000) { // @TODO: make the threshold editable by the user!
            nTargetLbl.setForeground(Color.red);
        } else if (nTarget > 100000) { // @TODO: make the threshold editable by the user!
            nTargetLbl.setForeground(orange);
        } else {
            nTargetLbl.setForeground(green);
        }

        // Target/Decoy group
        ((TitledBorder) targetDecoyGroupPanel.getBorder()).setTitle(targetDecoyCategory + " Target/Decoy Distributions");
        targetDecoyGroupPanel.repaint();
        if (targetDecoy) {
            int nTargetOnly = targetDecoyMap.getnTargetOnly();
            matchesBeforeFirstDecoyLbl.setText(nTargetOnly + " matches before the first decoy hit");

            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double fdrLimit = targetDecoyResults.getFdrLimit();
            double nTargetLimit = 100.0 / fdrLimit;
            if (nTargetOnly < nTargetLimit) {
                matchesBeforeFirstDecoyLbl.setForeground(Color.red);
            } else {
                matchesBeforeFirstDecoyLbl.setForeground(green);
            }
            recommendedNumberOfTargetHitsLbl.setText("Recommended: " + Util.roundDouble(nTargetLimit, 0) + " matches before the first decoy hit");

            double resolution = targetDecoyMap.getResolution();
            confidenceResolutionLbl.setText("PEP/Confidence resolution of " + Util.roundDouble(resolution, 2) + "%");

            double resolutionLimit = fdrLimit;
            if (resolution > 5 * resolutionLimit) {
                confidenceResolutionLbl.setForeground(Color.red);
            } else if (resolution > resolutionLimit) {
                confidenceResolutionLbl.setForeground(orange);
            } else {
                confidenceResolutionLbl.setForeground(green);
            }
            recommendedResolutionLbl.setText("Recommended: resolution < " + Util.roundDouble(resolutionLimit, 2) + "%");
        } else {
            matchesBeforeFirstDecoyLbl.setText("No decoy");
            matchesBeforeFirstDecoyLbl.setForeground(Color.gray);
            confidenceResolutionLbl.setText("Impossible to estimate confidence resolution");
            confidenceResolutionLbl.setForeground(Color.gray);
        }

        // Target/decoy results
        if (targetDecoy) {
            double confidence = 0;

            if (targetDecoyCategory.equalsIgnoreCase("Proteins")) {
                confidence = psParameter.getProteinConfidence();
            } else if (targetDecoyCategory.endsWith("Peptides")) {
                confidence = psParameter.getPeptideConfidence();
            } else if (targetDecoyCategory.endsWith("PSMs")) {
                confidence = psParameter.getPsmConfidence();
            }

            MatchValidationLevel matchValidationLevel = psParameter.getMatchValidationLevel();
            validationStatusLbl.setText("Validation Status: " + matchValidationLevel.getName());
            switch (matchValidationLevel) {
                case confident:
                    validationStatusLbl.setForeground(green);
                    break;
                case doubtful:
                    validationStatusLbl.setForeground(orange);
                    break;
                case not_validated:
                    validationStatusLbl.setForeground(Color.red);
                    break;
                case none:
                    validationStatusLbl.setForeground(Color.gray);
            }

            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            String validationThresholdTxt = "";
            double threshold = targetDecoyResults.getUserInput();
            int thresholdType = targetDecoyResults.getInputType();
            if (thresholdType == 0) {
                validationThresholdTxt += "Validation Threshold: " + Util.roundDouble(threshold, 2) + "%";
            } else if (targetDecoyResults.getInputType() == 1) {
                validationThresholdTxt += "FDR Threshold: " + Util.roundDouble(threshold, 2) + "%";
            } else if (targetDecoyResults.getInputType() == 2) {
                validationThresholdTxt += "FNR Threshold: " + Util.roundDouble(threshold, 2) + "%";
            }
            validationThresholdLbl.setText(validationThresholdTxt);

            confidenceLbl.setText("Confidence: " + Util.roundDouble(confidence, 2) + "%");
            double validationThreshold = targetDecoyResults.getConfidenceLimit();
            confidenceThresholdLbl.setText("Expected Confidence: " + Util.roundDouble(validationThreshold, 2) + "%");
            double resolution = targetDecoyMap.getResolution();
            double confidenceThreshold = validationThreshold + resolution;
            if (confidenceThreshold > 100) {
                confidenceThreshold = 100;
            }
            confidenceThresholdLbl.setText("Confident confidence: " + Util.roundDouble(confidenceThreshold, 2) + "%");
            if (confidence < validationThreshold) {
                confidenceLbl.setForeground(Color.red);
            } else if (confidence < confidenceThreshold) {
                confidenceLbl.setForeground(orange);
            } else {
                confidenceLbl.setForeground(green);
            }
        } else {
            validationStatusLbl.setText("Validation Status: " + psParameter.getMatchValidationLevel().getName());
            validationThresholdLbl.setText("Impossible to estimate validation threshold");
            confidenceLbl.setText("Impossible to estimate confidence");
            confidenceThresholdLbl.setText("Impossible to estimate confidence threshold");
            validationStatusLbl.setForeground(Color.gray);
            confidenceLbl.setForeground(Color.gray);
        }

        // Quality filters
        final DefaultTableModel tableModel = new FiltersTableModel();
        qualityFiltersTable.setModel(tableModel);
        qualityFiltersTable.getColumn("").setMaxWidth(50);
        qualityFiltersTable.getColumn(" ").setMaxWidth(50);
        qualityFiltersTable.getColumn(" ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Yes", "No"));

        int valid = 0;
        for (String qcCheck : psParameter.getQcCriteria()) {
            if (psParameter.isQcPassed(qcCheck)) {
                valid++;
            }
        }
        ((TitledBorder) qualityFiltersPanel.getBorder()).setTitle("Quality Filters (" + valid + "/" + psParameter.getQcCriteria().size() + ")");
    }

    /**
     * Table model for the filters.
     */
    private class FiltersTableModel extends DefaultTableModel {

        /**
         * The ordered QC criteria.
         */
        private ArrayList<String> qcCriteria;

        /**
         * Constructor.
         *
         * @param psParameter the PSParameter of the match
         */
        public FiltersTableModel() {
            qcCriteria = new ArrayList<String>(psParameter.getQcCriteria());
            Collections.sort(qcCriteria);
        }

        @Override
        public int getRowCount() {
            if (qcCriteria == null) {
                return 0;
            }
            return qcCriteria.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
                case 0:
                    return "";
                case 1:
                    return "Name";
                case 2:
                    return " ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            switch (column) {
                case 0:
                    return (row + 1);
                case 1:
                    return qcCriteria.get(row);
                case 2:
                    String criterion = qcCriteria.get(row);
                    return psParameter.isQcPassed(criterion);
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
     * Indicates whether the validation level changed.
     *
     * @return a boolean indicating whether the validation level changed
     */
    public boolean isValidationChanged() {
        return validationChanged;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bitRecommendationLabel1 = new javax.swing.JLabel();
        bitLabel1 = new javax.swing.JLabel();
        backgroundPanel = new javax.swing.JPanel();
        validationLevelPanel = new javax.swing.JPanel();
        validationLevelJComboBox = new javax.swing.JComboBox();
        validationTypeLabel = new javax.swing.JLabel();
        databaseSearchPanel = new javax.swing.JPanel();
        bitRecommendationLabel3 = new javax.swing.JLabel();
        targetDecoyLbl = new javax.swing.JLabel();
        bitRecommendationLabel4 = new javax.swing.JLabel();
        nTargetLbl = new javax.swing.JLabel();
        targetDecoyGroupPanel = new javax.swing.JPanel();
        recommendedNumberOfTargetHitsLbl = new javax.swing.JLabel();
        matchesBeforeFirstDecoyLbl = new javax.swing.JLabel();
        recommendedResolutionLbl = new javax.swing.JLabel();
        confidenceResolutionLbl = new javax.swing.JLabel();
        targetDecoyPanel = new javax.swing.JPanel();
        validationThresholdLbl = new javax.swing.JLabel();
        confidenceLbl = new javax.swing.JLabel();
        confidenceThresholdLbl = new javax.swing.JLabel();
        validationStatusLbl = new javax.swing.JLabel();
        qualityFiltersPanel = new javax.swing.JPanel();
        qualityFiltersTableScrollPane = new javax.swing.JScrollPane();
        qualityFiltersTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return (String) validationTableToolTips.get(realIndex);
                    }
                };
            }
        };
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        editLbl = new javax.swing.JLabel();

        bitRecommendationLabel1.setFont(bitRecommendationLabel1.getFont().deriveFont((bitRecommendationLabel1.getFont().getStyle() | java.awt.Font.ITALIC)));
        bitRecommendationLabel1.setText("Recommended: Concatenated Target/Decoy");

        bitLabel1.setText("Concatenated Target/Decoy");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(700, 600));

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        validationLevelPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Validation Level"));
        validationLevelPanel.setOpaque(false);

        validationLevelJComboBox.setModel(new DefaultComboBoxModel(MatchValidationLevel.getValidationLevelsNames()));
        validationLevelJComboBox.setToolTipText("Validation Level");
        validationLevelJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validationLevelJComboBoxActionPerformed(evt);
            }
        });

        validationTypeLabel.setText("Type");

        javax.swing.GroupLayout validationLevelPanelLayout = new javax.swing.GroupLayout(validationLevelPanel);
        validationLevelPanel.setLayout(validationLevelPanelLayout);
        validationLevelPanelLayout.setHorizontalGroup(
            validationLevelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(validationLevelPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(validationTypeLabel)
                .addGap(20, 20, 20)
                .addComponent(validationLevelJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        validationLevelPanelLayout.setVerticalGroup(
            validationLevelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(validationLevelPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(validationLevelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(validationTypeLabel)
                    .addComponent(validationLevelJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        databaseSearchPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Database Search"));
        databaseSearchPanel.setOpaque(false);

        bitRecommendationLabel3.setFont(bitRecommendationLabel3.getFont().deriveFont((bitRecommendationLabel3.getFont().getStyle() | java.awt.Font.ITALIC)));
        bitRecommendationLabel3.setText("Recommended: Concatenated Target/Decoy");

        targetDecoyLbl.setText("Concatenated Target/Decoy");

        bitRecommendationLabel4.setFont(bitRecommendationLabel4.getFont().deriveFont((bitRecommendationLabel4.getFont().getStyle() | java.awt.Font.ITALIC)));
        bitRecommendationLabel4.setText("Recommended: between 1,000 and 100,000");

        nTargetLbl.setText("xx,xxx target sequences");

        javax.swing.GroupLayout databaseSearchPanelLayout = new javax.swing.GroupLayout(databaseSearchPanel);
        databaseSearchPanel.setLayout(databaseSearchPanelLayout);
        databaseSearchPanelLayout.setHorizontalGroup(
            databaseSearchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(databaseSearchPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(databaseSearchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(databaseSearchPanelLayout.createSequentialGroup()
                        .addComponent(targetDecoyLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(266, 266, 266)
                        .addComponent(bitRecommendationLabel3))
                    .addGroup(databaseSearchPanelLayout.createSequentialGroup()
                        .addComponent(nTargetLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(266, 266, 266)
                        .addComponent(bitRecommendationLabel4)))
                .addContainerGap())
        );
        databaseSearchPanelLayout.setVerticalGroup(
            databaseSearchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(databaseSearchPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(databaseSearchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(targetDecoyLbl)
                    .addComponent(bitRecommendationLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(databaseSearchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nTargetLbl)
                    .addComponent(bitRecommendationLabel4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        targetDecoyGroupPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Target/Decoy Group"));
        targetDecoyGroupPanel.setOpaque(false);

        recommendedNumberOfTargetHitsLbl.setFont(recommendedNumberOfTargetHitsLbl.getFont().deriveFont((recommendedNumberOfTargetHitsLbl.getFont().getStyle() | java.awt.Font.ITALIC)));
        recommendedNumberOfTargetHitsLbl.setText("Recommended: 100 matches before the first decoy hit");

        matchesBeforeFirstDecoyLbl.setText("xxx matches before the first decoy hit");

        recommendedResolutionLbl.setFont(recommendedResolutionLbl.getFont().deriveFont((recommendedResolutionLbl.getFont().getStyle() | java.awt.Font.ITALIC)));
        recommendedResolutionLbl.setText("Recommended: resolution < 1%");

        confidenceResolutionLbl.setText("PEP/Confidence resolution of x%");

        javax.swing.GroupLayout targetDecoyGroupPanelLayout = new javax.swing.GroupLayout(targetDecoyGroupPanel);
        targetDecoyGroupPanel.setLayout(targetDecoyGroupPanelLayout);
        targetDecoyGroupPanelLayout.setHorizontalGroup(
            targetDecoyGroupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(targetDecoyGroupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(targetDecoyGroupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(targetDecoyGroupPanelLayout.createSequentialGroup()
                        .addComponent(matchesBeforeFirstDecoyLbl, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 266, Short.MAX_VALUE)
                        .addComponent(recommendedNumberOfTargetHitsLbl))
                    .addGroup(targetDecoyGroupPanelLayout.createSequentialGroup()
                        .addComponent(confidenceResolutionLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(266, 266, 266)
                        .addComponent(recommendedResolutionLbl)))
                .addContainerGap())
        );
        targetDecoyGroupPanelLayout.setVerticalGroup(
            targetDecoyGroupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(targetDecoyGroupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(targetDecoyGroupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(matchesBeforeFirstDecoyLbl)
                    .addComponent(recommendedNumberOfTargetHitsLbl))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(targetDecoyGroupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(confidenceResolutionLbl)
                    .addComponent(recommendedResolutionLbl))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        targetDecoyPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Target/Decoy Results"));
        targetDecoyPanel.setOpaque(false);

        validationThresholdLbl.setFont(validationThresholdLbl.getFont().deriveFont((validationThresholdLbl.getFont().getStyle() | java.awt.Font.ITALIC)));
        validationThresholdLbl.setText("Validation threshold: x%");

        confidenceLbl.setText("Confidence: x%");

        confidenceThresholdLbl.setFont(confidenceThresholdLbl.getFont().deriveFont((confidenceThresholdLbl.getFont().getStyle() | java.awt.Font.ITALIC)));
        confidenceThresholdLbl.setText("Confidence threshold: x%");

        validationStatusLbl.setText("Validation Status: Validated");

        javax.swing.GroupLayout targetDecoyPanelLayout = new javax.swing.GroupLayout(targetDecoyPanel);
        targetDecoyPanel.setLayout(targetDecoyPanelLayout);
        targetDecoyPanelLayout.setHorizontalGroup(
            targetDecoyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(targetDecoyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(targetDecoyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, targetDecoyPanelLayout.createSequentialGroup()
                        .addComponent(confidenceLbl, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                        .addGap(301, 301, 301)
                        .addComponent(confidenceThresholdLbl))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, targetDecoyPanelLayout.createSequentialGroup()
                        .addComponent(validationStatusLbl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(validationThresholdLbl)))
                .addContainerGap())
        );
        targetDecoyPanelLayout.setVerticalGroup(
            targetDecoyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(targetDecoyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(targetDecoyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(validationThresholdLbl)
                    .addComponent(validationStatusLbl))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addGroup(targetDecoyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(confidenceThresholdLbl)
                    .addComponent(confidenceLbl))
                .addContainerGap())
        );

        qualityFiltersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Quality Filters"));
        qualityFiltersPanel.setOpaque(false);

        qualityFiltersTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        qualityFiltersTableScrollPane.setViewportView(qualityFiltersTable);

        javax.swing.GroupLayout qualityFiltersPanelLayout = new javax.swing.GroupLayout(qualityFiltersPanel);
        qualityFiltersPanel.setLayout(qualityFiltersPanelLayout);
        qualityFiltersPanelLayout.setHorizontalGroup(
            qualityFiltersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, qualityFiltersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(qualityFiltersTableScrollPane)
                .addContainerGap())
        );
        qualityFiltersPanelLayout.setVerticalGroup(
            qualityFiltersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(qualityFiltersPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(qualityFiltersTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                .addContainerGap())
        );

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

        editLbl.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        editLbl.setText("Validation filters can be edited via Edit > Validation Filters.");

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(validationLevelPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(editLbl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(qualityFiltersPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(databaseSearchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(targetDecoyGroupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(targetDecoyPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(validationLevelPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(databaseSearchPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(targetDecoyGroupPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(targetDecoyPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(qualityFiltersPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton)
                    .addComponent(editLbl))
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
     * Save any changes and close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        String newValue = validationLevelJComboBox.getSelectedItem().toString();

        if (!newValue.equals(psParameter.getMatchValidationLevel().getName())) {

            MatchValidationLevel matchValidationLevel = MatchValidationLevel.getMatchValidationLevel(newValue);
            psParameter.setMatchValidationLevel(matchValidationLevel);
            psParameter.setManualValidation(true);

            try {
                Metrics metrics = identificationFeaturesGenerator.getMetrics();

                if (type == Type.PROTEIN) {
                    identification.updateProteinMatchParameter(matchKey, psParameter);
                    if (matchValidationLevel == MatchValidationLevel.confident) {
                        metrics.setnConfidentProteins(metrics.getnConfidentProteins() + 1);
                    } else if (matchValidationLevel == MatchValidationLevel.doubtful) {
                        metrics.setnConfidentProteins(metrics.getnConfidentProteins() - 1);
                    }
                } else if (type == Type.PEPTIDE) {

                    identification.updatePeptideMatchParameter(matchKey, psParameter);
                    PSMaps pSMaps = new PSMaps();
                    pSMaps = (PSMaps) identification.getUrParam(pSMaps);
                    ProteinMap proteinMap = pSMaps.getProteinMap();
                    PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);

                    for (String accession : peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences())) {

                        ArrayList<String> proteinMatches = identification.getProteinMap().get(accession);

                        if (proteinMatches != null) {

                            identification.loadProteinMatchParameters(proteinMatches, psParameter, null);

                            for (String proteinMatchKey : proteinMatches) {

                                identificationFeaturesGenerator.updateNConfidentPeptides(proteinMatchKey);
                                PSParameter proteinPSParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, psParameter);
                                MatchValidationLevel proteinValidation = proteinPSParameter.getMatchValidationLevel();

                                if (proteinValidation.isValidated()) {

                                    MatchesValidator.updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, proteinMap, proteinMatchKey);
                                    proteinPSParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, proteinPSParameter);
                                    MatchValidationLevel newValidation = proteinPSParameter.getMatchValidationLevel();

                                    if (newValidation == MatchValidationLevel.confident && proteinValidation == MatchValidationLevel.doubtful) {
                                        metrics.setnConfidentProteins(metrics.getnConfidentProteins() + 1);
                                    } else if (newValidation == MatchValidationLevel.doubtful && proteinValidation == MatchValidationLevel.confident) {
                                        metrics.setnConfidentProteins(metrics.getnConfidentProteins() - 1);
                                    }
                                }
                            }
                        }
                    }
                } else if (type == Type.PSM) {

                    PSMaps pSMaps = new PSMaps();
                    pSMaps = (PSMaps) identification.getUrParam(pSMaps);
                    PeptideSpecificMap peptideMap = pSMaps.getPeptideSpecificMap();
                    ProteinMap proteinMap = pSMaps.getProteinMap();
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
                    if (spectrumMatch.getBestPeptideAssumption() != null) {
                        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                        String peptideKey = peptide.getMatchingKey(identificationParameters.getSequenceMatchingPreferences());
                        identificationFeaturesGenerator.updateNConfidentSpectraForPeptide(peptideKey);
                        PSParameter peptidePSParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

                        if (peptidePSParameter.getMatchValidationLevel().isValidated()) {

                            MatchesValidator.updatePeptideMatchValidationLevel(identification, identificationFeaturesGenerator, shotgunProtocol, identificationParameters, peptideMap, peptideKey);
                            identification.updateSpectrumMatchParameter(matchKey, psParameter);
                            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

                            for (String accession : peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences())) {

                                ArrayList<String> proteinMatches = identification.getProteinMap().get(accession);

                                if (proteinMatches != null) {

                                    identification.loadProteinMatchParameters(proteinMatches, psParameter, null);

                                    for (String proteinMatchKey : proteinMatches) {

                                        identificationFeaturesGenerator.updateNConfidentPeptides(proteinMatchKey);
                                        identificationFeaturesGenerator.updateNConfidentSpectra(proteinMatchKey);
                                        PSParameter proteinPSParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, psParameter);
                                        MatchValidationLevel proteinValidation = proteinPSParameter.getMatchValidationLevel();

                                        if (proteinValidation.isValidated()) {

                                            MatchesValidator.updateProteinMatchValidationLevel(identification, identificationFeaturesGenerator,
                                                    shotgunProtocol, identificationParameters, proteinMap, proteinMatchKey);
                                            proteinPSParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, proteinPSParameter);
                                            MatchValidationLevel newValidation = proteinPSParameter.getMatchValidationLevel();

                                            if (newValidation == MatchValidationLevel.confident && proteinValidation == MatchValidationLevel.doubtful) {
                                                metrics.setnConfidentProteins(metrics.getnConfidentProteins() + 1);
                                            } else if (newValidation == MatchValidationLevel.doubtful && proteinValidation == MatchValidationLevel.confident) {
                                                metrics.setnConfidentProteins(metrics.getnConfidentProteins() - 1);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                validationChanged = true;

            } catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }

        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Change the validation level or show a warning if the level cannot be
     * changed.
     *
     * @param evt
     */
    private void validationLevelJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validationLevelJComboBoxActionPerformed
        String newValue = validationLevelJComboBox.getSelectedItem().toString();
        MatchValidationLevel matchValidationLevel = MatchValidationLevel.getMatchValidationLevel(newValue);
        if (psParameter.getMatchValidationLevel().isValidated() && (matchValidationLevel == MatchValidationLevel.none || matchValidationLevel == MatchValidationLevel.not_validated)) {
            JOptionPane.showMessageDialog(this,
                    "The statistical validation level cannot be changed. Please use the\n"
                    + "non-statistical levels " + MatchValidationLevel.confident.getName() + " or " + MatchValidationLevel.doubtful.getName() + ".\n\n"
                    + "To change the statistical validation threshold, use the Validation tab.",
                    "Validation Level Error", JOptionPane.WARNING_MESSAGE);
            validationLevelJComboBox.setSelectedItem(psParameter.getMatchValidationLevel().getName());
        } else if (!psParameter.getMatchValidationLevel().isValidated() && matchValidationLevel != psParameter.getMatchValidationLevel()) {
            JOptionPane.showMessageDialog(this,
                    "The statistical validation level cannot be changed. To change\n"
                    + "the statistical validation threshold, use the Validation tab.",
                    "Validation Level Error", JOptionPane.WARNING_MESSAGE);
            validationLevelJComboBox.setSelectedItem(psParameter.getMatchValidationLevel().getName());
        }
    }//GEN-LAST:event_validationLevelJComboBoxActionPerformed

    /**
     * Close the dialog without saving any changes.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel bitLabel1;
    private javax.swing.JLabel bitRecommendationLabel1;
    private javax.swing.JLabel bitRecommendationLabel3;
    private javax.swing.JLabel bitRecommendationLabel4;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel confidenceLbl;
    private javax.swing.JLabel confidenceResolutionLbl;
    private javax.swing.JLabel confidenceThresholdLbl;
    private javax.swing.JPanel databaseSearchPanel;
    private javax.swing.JLabel editLbl;
    private javax.swing.JLabel matchesBeforeFirstDecoyLbl;
    private javax.swing.JLabel nTargetLbl;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel qualityFiltersPanel;
    private javax.swing.JTable qualityFiltersTable;
    private javax.swing.JScrollPane qualityFiltersTableScrollPane;
    private javax.swing.JLabel recommendedNumberOfTargetHitsLbl;
    private javax.swing.JLabel recommendedResolutionLbl;
    private javax.swing.JPanel targetDecoyGroupPanel;
    private javax.swing.JLabel targetDecoyLbl;
    private javax.swing.JPanel targetDecoyPanel;
    private javax.swing.JComboBox validationLevelJComboBox;
    private javax.swing.JPanel validationLevelPanel;
    private javax.swing.JLabel validationStatusLbl;
    private javax.swing.JLabel validationThresholdLbl;
    private javax.swing.JLabel validationTypeLabel;
    // End of variables declaration//GEN-END:variables
}
