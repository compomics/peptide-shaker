/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.gui;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.general.ExceptionHandler;
import com.compomics.util.gui.JOptionEditorPane;
import eu.isas.peptideshaker.filtering.MatchFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PeptideSpecificMap;
import eu.isas.peptideshaker.scoring.ProteinMap;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class displays information about the validation of a match
 *
 * @author Marc
 */
public class MatchValidationDialog extends javax.swing.JDialog {

    /**
     * The key of the match
     */
    private String matchKey;
    /**
     * The ps parameter of the match
     */
    private PSParameter psParameter;
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The exception handler
     */
    private ExceptionHandler exceptionHandler;
    /**
     * Indicates whether the validation status of the match changed
     */
    private boolean validationChanged = false;
    /**
     * The type of match selected
     */
    private Type type;
    /**
     * the color to use when writing in green
     */
    private static final Color green = new Color(0, 125, 0);
    /**
     * the color to use when writing in orange
     */
    private static final Color orange = new Color(220, 110, 0);

    /**
     * Type of match selected.
     */
    private enum Type {
        PROTEIN,
        PEPTIDE,
        PSM
    }

    /**
     * Constructor for a protein match validation dialog
     *
     * @param parent the parent frame
     * @param exceptionHandler the handler catches exception happening when
     * filling the table
     * @param identification the identification where to get the match from
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param proteinMap the protein map
     * @param proteinMatchKey the protein match key
     * @param searchParameters the search parameters
     *
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws java.lang.ClassNotFoundException
     */
    public MatchValidationDialog(java.awt.Frame parent, ExceptionHandler exceptionHandler, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, ProteinMap proteinMap, String proteinMatchKey, SearchParameters searchParameters) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        super(parent, true);
        initComponents();

        this.matchKey = proteinMatchKey;
        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, new PSParameter());
        this.exceptionHandler = exceptionHandler;
        this.identification = identification;
        type = Type.PROTEIN;

        ArrayList<MatchFilter> filters = new ArrayList<MatchFilter>();
        for (ProteinFilter proteinFilter : proteinMap.getDoubtfulMatchesFilters()) {
            filters.add(proteinFilter);
        }

        TargetDecoyMap targetDecoyMap = proteinMap.getTargetDecoyMap();
        populateGUI(identificationFeaturesGenerator, searchParameters, targetDecoyMap, filters, "Proteins");
        
        setTitle("Protein Group Validation Quality");
        
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Creates a peptide match validation dialog
     *
     * @param parent the parent frame
     * @param exceptionHandler the handler catches exception happening when
     * filling the table
     * @param identification the identification where to get the match from
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param searchParameters the search parameters
     * @param peptideSpecificMap the peptide specific target decoy map
     * @param peptideMatchKey the peptide match key
     *
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public MatchValidationDialog(java.awt.Frame parent, ExceptionHandler exceptionHandler, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, PeptideSpecificMap peptideSpecificMap, String peptideMatchKey, SearchParameters searchParameters) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        super(parent, true);
        initComponents();

        this.matchKey = peptideMatchKey;
        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideMatchKey, new PSParameter());
        this.exceptionHandler = exceptionHandler;
        this.identification = identification;
        type = Type.PEPTIDE;

        ArrayList<MatchFilter> filters = new ArrayList<MatchFilter>();
        for (PeptideFilter peptideFilter : peptideSpecificMap.getDoubtfulMatchesFilters()) {
            filters.add(peptideFilter);
        }

        String peptideGroupKey = psParameter.getSpecificMapKey();
        TargetDecoyMap targetDecoyMap = peptideSpecificMap.getTargetDecoyMap(peptideSpecificMap.getCorrectedKey(peptideGroupKey));
        String groupName = "";
        if (peptideSpecificMap.getKeys().size() > 1) {
            groupName += PeptideSpecificMap.getKeyName(searchParameters.getModificationProfile(), peptideGroupKey) + " ";
        }
        groupName += "Peptides";
        populateGUI(identificationFeaturesGenerator, searchParameters, targetDecoyMap, filters, groupName);
        
        setTitle("Peptide Validation Quality");
        
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Creates a peptide match validation dialog
     *
     * @param parent the parent frame
     * @param exceptionHandler the handler catches exception happening when
     * filling the table
     * @param identification the identification where to get the match from
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param searchParameters the search parameters
     * @param psmSpecificMap the PSM specific target decoy map
     * @param psmMatchKey the spectrum match key
     *
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public MatchValidationDialog(java.awt.Frame parent, ExceptionHandler exceptionHandler, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, PsmSpecificMap psmSpecificMap, String psmMatchKey, SearchParameters searchParameters) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        super(parent, true);
        initComponents();

        this.matchKey = psmMatchKey;
        psParameter = (PSParameter) identification.getSpectrumMatchParameter(psmMatchKey, new PSParameter());
        this.exceptionHandler = exceptionHandler;
        this.identification = identification;
        type = Type.PSM;

        ArrayList<MatchFilter> filters = new ArrayList<MatchFilter>();
        for (PsmFilter psmFilter : psmSpecificMap.getDoubtfulMatchesFilters()) {
            filters.add(psmFilter);
        }

        int psmGroupKey = psmSpecificMap.getCorrectedKey(psParameter.getSpecificMapKey());
        TargetDecoyMap targetDecoyMap = psmSpecificMap.getTargetDecoyMap(psmGroupKey);
        String groupName = "";
        if (psmSpecificMap.getKeys().size() > 1) {
            groupName = "Charge " + psmSpecificMap.getKeys().get(psmGroupKey);
        }
        groupName += " PSMs";
        populateGUI(identificationFeaturesGenerator, searchParameters, targetDecoyMap, filters, groupName);
        
        setTitle("PSM Validation Quality");
        
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * Populates the gui with information on a protein match.
     *
     * @param identificationFeaturesGenerator
     * @param proteinMap
     * @param searchParameters
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    private void populateGUI(IdentificationFeaturesGenerator identificationFeaturesGenerator, SearchParameters searchParameters, TargetDecoyMap targetDecoyMap, ArrayList<MatchFilter> filters, String targetDecoyCategory) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        // Validation level
        validationLevelJComboBox.setSelectedItem(psParameter.getMatchValidationLevel().getName());

        // Database info
        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        boolean targetDecoy = sequenceFactory.concatenatedTargetDecoy();
        if (!targetDecoy) {
            targetDecoyLbl.setText("Target only");
            targetDecoyLbl.setForeground(Color.red);
        } else {
            targetDecoyLbl.setForeground(green);
        }
        int nTarget = sequenceFactory.getNTargetSequences();
        nTargetLbl.setText(nTarget + " target sequences.");
        if (nTarget < 10000) {
            nTargetLbl.setForeground(Color.red);
        } else if (nTarget > 100000) {
            nTargetLbl.setForeground(orange);
        } else {
            nTargetLbl.setForeground(green);
        }

        // Target/Decoy group
        ((TitledBorder) targetDecoyGroupPanel.getBorder()).setTitle(targetDecoyCategory + " target/decoy distributions");
        targetDecoyGroupPanel.repaint();
        if (targetDecoy) {
            int nTargetOnly = targetDecoyMap.getnTargetOnly();
            matchesBeforeFirstDecoyLbl.setText(nTargetOnly + " matches before the first decoy hit");
            if (nTargetOnly < 100) {
                matchesBeforeFirstDecoyLbl.setForeground(Color.red);
            } else {
                matchesBeforeFirstDecoyLbl.setForeground(green);
            }
            double resolution = targetDecoyMap.getResolution();
            confidenceResolutionLbl.setText("PEP/Confidence resolution of " + Util.roundDouble(resolution, 2) + "%");
            if (resolution > 5) {
                confidenceResolutionLbl.setForeground(Color.red);
            } else if (resolution > 1) {
                confidenceResolutionLbl.setForeground(orange);
            } else {
                confidenceResolutionLbl.setForeground(green);
            }
        } else {
            matchesBeforeFirstDecoyLbl.setText("No decoy");
            matchesBeforeFirstDecoyLbl.setForeground(Color.gray);
            confidenceResolutionLbl.setText("Impossible to estimate confidence resolution");
            confidenceResolutionLbl.setForeground(Color.gray);
        }

        // Target/decoy results
        if (targetDecoy) {
            double confidence = psParameter.getProteinConfidence();
            confidenceLbl.setText("Confidence: " + Util.roundDouble(confidence, 2) + "%");
            TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
            double validationThreshold = targetDecoyResults.getConfidenceLimit();
            validationThresoldLbl.setText("Validation threshold: " + Util.roundDouble(validationThreshold, 2) + "%");
            double resolution = targetDecoyMap.getResolution();
            double confidenceThreshold = validationThreshold + resolution;
            if (confidenceThreshold > 100) {
                confidenceThreshold = 100;
            }
            confidenceThresholdLbl.setText("Confidence threshold: " + Util.roundDouble(confidenceThreshold, 2) + "%");
            if (confidence < validationThreshold) {
                confidenceLbl.setForeground(Color.red);
            } else if (confidence < confidenceThreshold) {
                confidenceLbl.setForeground(orange);
            } else {
                confidenceLbl.setForeground(green);
            }
        } else {
            confidenceLbl.setText("Impossible to estimate confidence");
            validationThresoldLbl.setText("Impossible to estimate validation threshold");
            confidenceThresholdLbl.setText("Impossible to estimate confidence threshold");
            confidenceLbl.setForeground(Color.gray);
        }

        // Quality filters
        final DefaultTableModel tableModel = new FiltersTableModel(identification, identificationFeaturesGenerator, filters, searchParameters);
        qualityFiltersTable.setModel(tableModel);
        qualityFiltersTable.getColumn("").setMaxWidth(50);
        qualityFiltersTable.getColumn(" ").setMaxWidth(50);
        qualityFiltersTable.getColumn(" ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
                null,
                "Yes", "No"));
        
        int valid = 0;
        for (MatchFilter matchFilter : filters) {
            if (matchFilter.isValidated(matchKey, identification, identificationFeaturesGenerator, searchParameters)) {
                valid++;
            }
        }
        ((TitledBorder) tablePanel.getBorder()).setTitle("Quality Filters (" + valid + "/" + filters.size() + ")");
    }

    /**
     * Table model for the filters.
     */
    private class FiltersTableModel extends DefaultTableModel {

        /**
         * List of filters
         */
        private ArrayList<MatchFilter> filters;
        /**
         * The identification
         */
        private Identification identification;
        /**
         * The identification features generator
         */
        private IdentificationFeaturesGenerator identificationFeaturesGenerator;
        /**
         * The identification parameters used for the search
         */
        private SearchParameters searchParameters;

        /**
         * Constructor
         *
         * @param identification the identification
         * @param identificationFeaturesGenerator the identification features
         * generator
         * @param filters the filters used to assess the quality of the match
         * @param searchParameters the identification parameters used for the
         * search
         */
        public FiltersTableModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, ArrayList<MatchFilter> filters, SearchParameters searchParameters) {
            this.identification = identification;
            this.identificationFeaturesGenerator = identificationFeaturesGenerator;
            this.filters = filters;
            this.searchParameters = searchParameters;
        }

        @Override
        public int getRowCount() {
            if (filters == null) {
                return 0;
            }
            return filters.size();
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
                    MatchFilter filter = filters.get(row);
                    return filter.getName();
                case 2:
                    filter = filters.get(row);
                    try {
                        return filter.isValidated(matchKey, identification, identificationFeaturesGenerator, searchParameters);
                    } catch (Exception e) {
                        exceptionHandler.catchException(e);
                        return "";
                    }
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
     * Indicates whether the validation level changed
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
        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        validationLevelJComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        tablePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        qualityFiltersTable = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        bitRecommendationLabel3 = new javax.swing.JLabel();
        targetDecoyLbl = new javax.swing.JLabel();
        bitRecommendationLabel4 = new javax.swing.JLabel();
        nTargetLbl = new javax.swing.JLabel();
        targetDecoyGroupPanel = new javax.swing.JPanel();
        bitRecommendationLabel5 = new javax.swing.JLabel();
        matchesBeforeFirstDecoyLbl = new javax.swing.JLabel();
        bitRecommendationLabel6 = new javax.swing.JLabel();
        confidenceResolutionLbl = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        validationThresoldLbl = new javax.swing.JLabel();
        confidenceLbl = new javax.swing.JLabel();
        confidenceThresholdLbl = new javax.swing.JLabel();

        bitRecommendationLabel1.setFont(bitRecommendationLabel1.getFont().deriveFont((bitRecommendationLabel1.getFont().getStyle() | java.awt.Font.ITALIC)));
        bitRecommendationLabel1.setText("Recommended: Concatenated Target/Decoy");

        bitLabel1.setText("Concatenated Target/Decoy");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jButton1.setText("Cancel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("OK");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Validation Level"));

        validationLevelJComboBox.setModel(new DefaultComboBoxModel(MatchValidationLevel.getValidationLevelsNames()));
        validationLevelJComboBox.setToolTipText("Validation Level");
        validationLevelJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validationLevelJComboBoxActionPerformed(evt);
            }
        });

        jLabel2.setText("Type:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(20, 20, 20)
                .addComponent(validationLevelJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 295, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(validationLevelJComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tablePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Quality Filters"));

        qualityFiltersTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(qualityFiltersTable);

        javax.swing.GroupLayout tablePanelLayout = new javax.swing.GroupLayout(tablePanel);
        tablePanel.setLayout(tablePanelLayout);
        tablePanelLayout.setHorizontalGroup(
            tablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        tablePanelLayout.setVerticalGroup(
            tablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Database Search"));

        bitRecommendationLabel3.setFont(bitRecommendationLabel3.getFont().deriveFont((bitRecommendationLabel3.getFont().getStyle() | java.awt.Font.ITALIC)));
        bitRecommendationLabel3.setText("Recommended: Concatenated Target/Decoy");

        targetDecoyLbl.setText("Concatenated Target/Decoy");

        bitRecommendationLabel4.setFont(bitRecommendationLabel4.getFont().deriveFont((bitRecommendationLabel4.getFont().getStyle() | java.awt.Font.ITALIC)));
        bitRecommendationLabel4.setText("Recommended: between 10,000 and 100,000");

        nTargetLbl.setText("xx,xxx target sequences");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(targetDecoyLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(266, 266, 266)
                        .addComponent(bitRecommendationLabel3))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(nTargetLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(266, 266, 266)
                        .addComponent(bitRecommendationLabel4)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(targetDecoyLbl)
                    .addComponent(bitRecommendationLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nTargetLbl)
                    .addComponent(bitRecommendationLabel4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        targetDecoyGroupPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Target/Decoy Group"));

        bitRecommendationLabel5.setFont(bitRecommendationLabel5.getFont().deriveFont((bitRecommendationLabel5.getFont().getStyle() | java.awt.Font.ITALIC)));
        bitRecommendationLabel5.setText("Recommended: 100 matches before the first decoy hit");

        matchesBeforeFirstDecoyLbl.setText("xxx matches before the first decoy hit");

        bitRecommendationLabel6.setFont(bitRecommendationLabel6.getFont().deriveFont((bitRecommendationLabel6.getFont().getStyle() | java.awt.Font.ITALIC)));
        bitRecommendationLabel6.setText("Recommended: resolution < 1%");

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
                        .addGap(266, 266, 266)
                        .addComponent(bitRecommendationLabel5))
                    .addGroup(targetDecoyGroupPanelLayout.createSequentialGroup()
                        .addComponent(confidenceResolutionLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(266, 266, 266)
                        .addComponent(bitRecommendationLabel6)))
                .addContainerGap())
        );
        targetDecoyGroupPanelLayout.setVerticalGroup(
            targetDecoyGroupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(targetDecoyGroupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(targetDecoyGroupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(matchesBeforeFirstDecoyLbl)
                    .addComponent(bitRecommendationLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(targetDecoyGroupPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(confidenceResolutionLbl)
                    .addComponent(bitRecommendationLabel6))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Target/Decoy Results"));

        validationThresoldLbl.setFont(validationThresoldLbl.getFont().deriveFont((validationThresoldLbl.getFont().getStyle() | java.awt.Font.ITALIC)));
        validationThresoldLbl.setText("Validation threshold: x%");

        confidenceLbl.setText("Confidence: x%");

        confidenceThresholdLbl.setFont(confidenceThresholdLbl.getFont().deriveFont((confidenceThresholdLbl.getFont().getStyle() | java.awt.Font.ITALIC)));
        confidenceThresholdLbl.setText("Confidence threshold: x%");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(confidenceLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(266, 266, 266)
                        .addComponent(validationThresoldLbl))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(confidenceThresholdLbl)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(confidenceLbl)
                    .addComponent(validationThresoldLbl))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(confidenceThresholdLbl)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1))
                    .addComponent(tablePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(targetDecoyGroupPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(targetDecoyGroupPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tablePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        String newValue = validationLevelJComboBox.getSelectedItem().toString();
        if (!newValue.equals(psParameter.getMatchValidationLevel().getName())) {
            MatchValidationLevel matchValidationLevel = MatchValidationLevel.getMatchValidationLevel(newValue);
            psParameter.setMatchValidationLevel(matchValidationLevel);
            try {
                if (type == Type.PROTEIN) {
                    identification.updateProteinMatchParameter(matchKey, psParameter);
                } else if (type == Type.PEPTIDE) {
                    identification.updatePeptideMatchParameter(matchKey, psParameter);
                } else if (type == Type.PSM) {
                    identification.updateSpectrumMatchParameter(matchKey, psParameter);
                }
                validationChanged = true;
            } catch (Exception e) {
                exceptionHandler.catchException(e);
            }
        }
        dispose();

    }//GEN-LAST:event_jButton2ActionPerformed

    private void validationLevelJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validationLevelJComboBoxActionPerformed
        String newValue = validationLevelJComboBox.getSelectedItem().toString();
        MatchValidationLevel matchValidationLevel = MatchValidationLevel.getMatchValidationLevel(newValue);
        if (psParameter.getMatchValidationLevel().isValidated() && (matchValidationLevel == MatchValidationLevel.none || matchValidationLevel == MatchValidationLevel.not_validated)) {
            JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                    "The statistical validation level cannot be changed. Please use non-statistical levels" + MatchValidationLevel.confident.getName() + " or " + MatchValidationLevel.doubtful.getName() + "."
                    + " To change the statistical validation threshold, use the Validation tab."),
                    "Validation level error", JOptionPane.ERROR_MESSAGE);
            validationLevelJComboBox.setSelectedItem(psParameter.getMatchValidationLevel().getName());
        } else if (!psParameter.getMatchValidationLevel().isValidated() && matchValidationLevel != psParameter.getMatchValidationLevel()) {
            JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                    "The statistical validation level cannot be changed."
                    + " To change the statistical validation threshold, use the Validation tab."),
                    "Validation level error", JOptionPane.ERROR_MESSAGE);
            validationLevelJComboBox.setSelectedItem(psParameter.getMatchValidationLevel().getName());
        }
    }//GEN-LAST:event_validationLevelJComboBoxActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bitLabel1;
    private javax.swing.JLabel bitRecommendationLabel1;
    private javax.swing.JLabel bitRecommendationLabel3;
    private javax.swing.JLabel bitRecommendationLabel4;
    private javax.swing.JLabel bitRecommendationLabel5;
    private javax.swing.JLabel bitRecommendationLabel6;
    private javax.swing.JLabel confidenceLbl;
    private javax.swing.JLabel confidenceResolutionLbl;
    private javax.swing.JLabel confidenceThresholdLbl;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel matchesBeforeFirstDecoyLbl;
    private javax.swing.JLabel nTargetLbl;
    private javax.swing.JTable qualityFiltersTable;
    private javax.swing.JPanel tablePanel;
    private javax.swing.JPanel targetDecoyGroupPanel;
    private javax.swing.JLabel targetDecoyLbl;
    private javax.swing.JComboBox validationLevelJComboBox;
    private javax.swing.JLabel validationThresoldLbl;
    // End of variables declaration//GEN-END:variables
}
