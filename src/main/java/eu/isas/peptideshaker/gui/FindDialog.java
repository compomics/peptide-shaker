package eu.isas.peptideshaker.gui;

import com.compomics.util.Util;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import eu.isas.peptideshaker.filtering.MatchFilter;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author Marc Vaudel
 */
public class FindDialog extends javax.swing.JDialog {

    /**
     * The main GUI instance
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The sequence factory
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The current protein filter
     */
    private ProteinFilter proteinFilter = null;
    /**
     * The current peptide filter
     */
    private PeptideFilter peptideFilter = null;
    /**
     * The current PSM filter
     */
    private PsmFilter psmFilter = null;

    /**
     * Creates a new find dialog
     * @param peptideShakerGUI the main GUI
     */
    public FindDialog(PeptideShakerGUI peptideShakerGUI) {
        new FindDialog(peptideShakerGUI, null, null, null, 0);
    }

    /**
     * Creates a new find dialog
     * @param peptideShakerGUI  the main GUI
     * @param selectedTab       the tab to select
     */
    public FindDialog(PeptideShakerGUI peptideShakerGUI, int selectedTab) {
        new FindDialog(peptideShakerGUI, null, null, null, selectedTab);
    }

    /**
     * Creates a new find dialog
     * @param peptideShakerGUI  the main GUI
     * @param proteinFilter     the protein filter to edit (can be null)
     * @param peptideFilter     the peptide filter to edit (can be null)
     * @param psmFilter         the psm filter to edit (can be null)
     */
    public FindDialog(PeptideShakerGUI peptideShakerGUI, ProteinFilter proteinFilter, PeptideFilter peptideFilter, PsmFilter psmFilter) {
        new FindDialog(peptideShakerGUI, proteinFilter, peptideFilter, psmFilter, 0);
    }

    /**
     * Creates a new find dialog
     * @param peptideShakerGUI  the main GUI
     * @param proteinFilter     the protein filter to edit (can be null)
     * @param peptideFilter     the peptide filter to edit (can be null)
     * @param psmFilter         the psm filter to edit (can be null)
     * @param selectedTab       the tab to select
     */
    public FindDialog(PeptideShakerGUI peptideShakerGUI, ProteinFilter proteinFilter, PeptideFilter peptideFilter, PsmFilter psmFilter, int selectedTab) {
        super(peptideShakerGUI, true);
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        initComponents();
        for (String ptm : peptideShakerGUI.getFoundModifications()) {
            ((DefaultTableModel) modificationTable.getModel()).addRow(new Object[]{
                        true,
                        ptm
                    });
        }

        for (int charge : peptideShakerGUI.getSearchedCharges()) {
            ((DefaultTableModel) chargeTable.getModel()).addRow(new Object[]{
                        true,
                        charge
                    });
        }
        for (String file : peptideShakerGUI.getSearchParameters().getSpectrumFiles()) {
            ((DefaultTableModel) fileTable.getModel()).addRow(new Object[]{
                        true,
                        Util.getFileName(file)
                    });
        }
        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);
        psmTable.setAutoCreateRowSorter(true);

        this.psmFilter = psmFilter;
        this.peptideFilter = peptideFilter;
        this.proteinFilter = proteinFilter;
        if (proteinFilter != null) {
            jTabbedPane1.setSelectedIndex(0);
        } else if (peptideFilter != null) {
            jTabbedPane1.setSelectedIndex(1);
        } else if (psmFilter != null) {
            jTabbedPane1.setSelectedIndex(2);
        } else {
            jTabbedPane1.setSelectedIndex(selectedTab);
        }
        if (proteinFilter != null) {
            if (proteinFilter.getName().equals(MatchFilter.MANUAL_SELECTION)) {
                proteinSplit.setDividerLocation(0);
                proteinManualSplit.setDividerLocation(1.0);
            } else if (proteinFilter.getExceptions().isEmpty()) {
                proteinSplit.setDividerLocation(1.0);
            } else {
                proteinManualSplit.setDividerLocation(0);
            }
            fillProteinTab();
            filterProteins();
        } else {
            proteinSplit.setDividerLocation(1.0);
        }
        if (peptideFilter != null) {
            if (peptideFilter.getName().equals(MatchFilter.MANUAL_SELECTION)) {
                peptideSplit.setDividerLocation(0);
                peptideManualSplit.setDividerLocation(1.0);
            } else if (peptideFilter.getExceptions().isEmpty()) {
                peptideSplit.setDividerLocation(1.0);
            } else {
                peptideManualSplit.setDividerLocation(0);
            }
            fillPeptideTab();
            filterPeptides();
        } else {
            peptideSplit.setDividerLocation(1.0);
        }
        if (psmFilter != null) {
            if (psmFilter.getName().equals(MatchFilter.MANUAL_SELECTION)) {
                psmSplit.setDividerLocation(0);
                psmManualSplit.setDividerLocation(1.0);
            } else if (psmFilter.getExceptions().isEmpty()) {
                psmSplit.setDividerLocation(1.0);
            } else {
                psmManualSplit.setDividerLocation(0);
            }
            fillPsmTab();
            filterPsms();
        } else {
            psmSplit.setDividerLocation(1.0);
        }
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setLocationRelativeTo(peptideShakerGUI);
        setVisible(true);
    }

    /**
     * Fills the protein tab
     */
    private void fillProteinTab() {
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
        if (proteinFilter.getnPeptides() != null) {
            nPeptidesTxt.setText(proteinFilter.getnPeptides() + "");
            nPeptidesCmb.setSelectedIndex(getComparisonIndex(proteinFilter.getnPeptidesComparison()));
        }
        if (proteinFilter.getProteinNSpectra() != null) {
            proteinsNSpectraTxt.setText(proteinFilter.getProteinNSpectra() + "");
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
     * Fills the peptide tab
     */
    private void fillPeptideTab() {
        if (peptideFilter.getProtein() != null) {
            peptideProteinTxt.setText(peptideFilter.getProtein());
        }
        if (peptideFilter.getSequence() != null) {
            peptideSequenceTxt.setText(peptideFilter.getSequence());
        }
        if (peptideFilter.getPi() < 5) {
            peptidePICmb.setSelectedIndex(peptideFilter.getPi() + 1);
            peptidePiComparisonCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getPiComparison()));
        }
        if (peptideFilter.getNSpectra() != null) {
            peptideNSpectraTxt.setText(peptideFilter.getNSpectra() + "");
            peptideNSpectraCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getnSpectraComparison()));
        }
        if (peptideFilter.getPeptideScore() != null) {
            peptideScoreTxt.setText(peptideFilter.getPeptideScore() + "");
            proteinScoreCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getPeptideScoreComparison()));
        }
        if (peptideFilter.getPeptideConfidence() != null) {
            peptideConfidenceTxt.setText(peptideFilter.getPeptideConfidence() + "");
            peptideConfidenceCmb.setSelectedIndex(getComparisonIndex(peptideFilter.getPeptideConfidenceComparison()));
        }
        for (int row = 0; row < modificationTable.getRowCount(); row++) {
            if (peptideFilter.getModificationStatus().contains(
                    (String) modificationTable.getValueAt(row, 1))) {
                modificationTable.setValueAt(true, row, 0);
            }
        }
        boolean first = true;
        String text = "";
        for (String key : peptideFilter.getManualValidation()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += key;
        }
        peptideManualValidationTxt.setText(text);
        first = true;
        text = "";
        for (String accession : peptideFilter.getExceptions()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += accession;
        }
        peptideExceptionsTxt.setText(text);
    }

    /**
     * Fills the psm tab
     */
    private void fillPsmTab() {
        if (psmFilter.getPrecursorRT() != null) {
            precursorRTTxt.setText(psmFilter.getPrecursorRT() + "");
            precursorRTCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPrecursorRTComparison()));
        }
        if (psmFilter.getPrecursorMz() != null) {
            precursorMzTxt.setText(psmFilter.getPrecursorMz() + "");
            precursorMzCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPrecursorMzComparison()));
        }
        if (psmFilter.getPrecursorMzError() != null) {
            precursorErrorTxt.setText(psmFilter.getPrecursorMzError() + "");
            precursorErrorCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPrecursorMzErrorComparison()));
        }
        if (psmFilter.getPsmConfidence() != null) {
            psmConfidenceTxt.setText(psmFilter.getPsmConfidence() + "");
            psmConfidenceCmb.setSelectedIndex(getComparisonIndex(psmFilter.getPsmConfidenceComparison()));
        }
        for (int row = 0; row < chargeTable.getRowCount(); row++) {
            if (psmFilter.getCharges().contains(
                    (Integer) chargeTable.getValueAt(row, 1))) {
                chargeTable.setValueAt(true, row, 0);
            }
        }
        for (int row = 0; row < fileTable.getRowCount(); row++) {
            if (psmFilter.getFileNames().contains(
                    (String) fileTable.getValueAt(row, 1))) {
                fileTable.setValueAt(true, row, 0);
            }
        }
        boolean first = true;
        String text = "";
        for (String key : psmFilter.getManualValidation()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += key;
        }
        psmManualValidationTxt.setText(text);
        first = true;
        text = "";
        for (String accession : psmFilter.getExceptions()) {
            if (first) {
                first = false;
            } else {
                text += "; ";
            }
            text += accession;
        }
        psmExceptionsTxt.setText(text);
    }

    /**
     * Validates the input
     * @return a boolean indicating whether the input is valid
     */
    public boolean validateInput() {
        if (!proteinCoverageTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(proteinCoverageTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein coverage.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!spectrumCountingTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(spectrumCountingTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for spectrum counting.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!nPeptidesTxt.getText().trim().equals("")) {
            try {
                Integer test = new Integer(nPeptidesTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for number of peptides.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinsNSpectraTxt.getText().trim().equals("")) {
            try {
                Integer test = new Integer(proteinsNSpectraTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein number of spectra.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinScoreTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(proteinScoreTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein score.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!proteinConfidenceTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(proteinConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for protein confidence.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!peptideNSpectraTxt.getText().trim().equals("")) {
            try {
                Integer test = new Integer(peptideNSpectraTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for peptide number of spectra.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!peptideScoreTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(peptideScoreTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for peptide score.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!peptideConfidenceTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(peptideConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for peptide confidence.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!precursorRTTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(precursorRTTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor retention time.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!precursorMzTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(precursorMzTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor m/z.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!precursorErrorTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(precursorErrorTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for precursor error.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!psmConfidenceTxt.getText().trim().equals("")) {
            try {
                Double test = new Double(psmConfidenceTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Please verify the input for PSM confidence.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * Convenience method returning the comparison type based on the selected item in the < = > combo boxes
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
     * Convenience method returning the combo box index based on a ComparisonType
     * @param comparisonType the comparison type
     * @return the corresponding index to select
     */
    private int getComparisonIndex(ComparisonType comparisonType) {
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
     * Filters the protein table according to the current filter settings.
     */
    private void filterProteins() {

        if (validateInput()) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

            // protein accession filter
            // @TODO can we make this not case sensitive?
            String text = proteinAccessionTxt.getText().trim();
            if (!text.equals("")) {
                try {
                    List<RowFilter<Object, Object>> accessionFilters = new ArrayList<RowFilter<Object, Object>>();
                    accessionFilters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Accession").getModelIndex()));
                    accessionFilters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Isoforms").getModelIndex()));
                    accessionFilters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Description").getModelIndex()));
                    filters.add(RowFilter.orFilter(accessionFilters));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for protein accession/description.", "Filter Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            if (proteinPICmb.getSelectedIndex() > 0) {
                int pi = proteinPICmb.getSelectedIndex() - 1;
                filters.add(RowFilter.numberFilter(getComparisonType(proteinPiComparisonCmb.getSelectedIndex()), pi, proteinTable.getColumn("PI").getModelIndex()));
            }

            text = spectrumCountingTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(spectrumCountingCmb.getSelectedIndex()), value, proteinTable.getColumn("Spectrum Counting").getModelIndex()));
            }

            text = proteinCoverageTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(proteinCoverageCmb.getSelectedIndex()), value, proteinTable.getColumn("Sequence coverage").getModelIndex()));
            }

            text = nPeptidesTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(nPeptidesCmb.getSelectedIndex()), value,
                        proteinTable.getColumn("# Peptides").getModelIndex()));
            }

            text = proteinsNSpectraTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(proteinNSpectraCmb.getSelectedIndex()), value,
                        proteinTable.getColumn("# Spectra").getModelIndex()));
            }

            text = proteinScoreTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(proteinScoreCmb.getSelectedIndex()), value,
                        proteinTable.getColumn("Score").getModelIndex()));
            }

            text = proteinConfidenceTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(proteinConfidenceCmb.getSelectedIndex()), value,
                        proteinTable.getColumn("Confidence").getModelIndex()));
            }

            // set the filters to the table
            RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);
            ((TableRowSorter) proteinTable.getRowSorter()).setRowFilter(allFilters);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Creates the protein filter according to the users input
     */
    public void createProteinFilter() {
        if (validateInput()) {
            Integer pi = null;
            if (proteinPICmb.getSelectedIndex() != 0) {
                pi = proteinPICmb.getSelectedIndex() - 1;
            }
            if (proteinFilter == null) {
                proteinFilter = new ProteinFilter("find protein filter");
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
                proteinFilter.setnPeptides(new Integer(nPeptidesTxt.getText().trim()));
                proteinFilter.setnPeptidesComparison(getComparisonType(nPeptidesCmb.getSelectedIndex()));
            }
            if (!proteinsNSpectraTxt.getText().trim().equals("")) {
                proteinFilter.setProteinNSpectra(new Integer(proteinsNSpectraTxt.getText().trim()));
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
    }

    /**
     * Table model for the protein table
     */
    private class ProteinTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return identification.getProteinIdentification().size();
        }

        @Override
        public int getColumnCount() {
            return 13;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Index";
                case 1:
                    return "Starred";
                case 2:
                    return "Hidden";
                case 3:
                    return "Accession";
                case 4:
                    return "Isoforms";
                case 5:
                    return "PI";
                case 6:
                    return "Description";
                case 7:
                    return "Sequence coverage";
                case 8:
                    return "# Peptides";
                case 9:
                    return "# PSMs";
                case 10:
                    return "Spectrum Counting";
                case 11:
                    return "Score";
                case 12:
                    return "Confidence";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String proteinKey = identification.getProteinIdentification().get(row);
                PSParameter psParameter;
                ProteinMatch proteinMatch;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.isStarred();
                    case 2:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.isHidden();
                    case 3:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        return proteinMatch.getMainMatch();
                    case 4:
                        String otherAccessions = "";
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        for (String accession : ProteinMatch.getAccessions(proteinKey)) {
                            if (!accession.equals(proteinMatch.getMainMatch())) {
                                otherAccessions += accession + " ";
                            }
                        }
                        return otherAccessions;
                    case 5:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.getGroupClass();
                    case 6:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        return sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
                    case 7:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
                        return 100 * peptideShakerGUI.estimateSequenceCoverage(proteinMatch, sequence);
                    case 8:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        return proteinMatch.getPeptideCount();
                    case 9:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        int cpt = 0;
                        PeptideMatch peptideMatch;
                        for (String peptideKey : proteinMatch.getPeptideMatches()) {
                            peptideMatch = identification.getPeptideMatch(peptideKey);
                            cpt += peptideMatch.getSpectrumCount();
                        }
                        return cpt;
                    case 10:
                        proteinMatch = identification.getProteinMatch(proteinKey);
                        return peptideShakerGUI.getSpectrumCounting(proteinMatch);
                    case 11:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.getProteinScore();
                    case 12:
                        psParameter = (PSParameter) identification.getMatchParameter(proteinKey, new PSParameter());
                        return psParameter.getProteinConfidence();
                    default:
                        return "";
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
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
     * Filters the peptide table according to the current filter settings.
     */
    private void filterPeptides() {

        if (validateInput()) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

            // protein filter
            String text = peptideProteinTxt.getText().trim();
            if (!text.equals("")) {
                try {
                    List<RowFilter<Object, Object>> accessionFilters = new ArrayList<RowFilter<Object, Object>>();
                    accessionFilters.add(RowFilter.regexFilter(text, peptideTable.getColumn("Proteins").getModelIndex()));
                    accessionFilters.add(RowFilter.regexFilter(text, peptideTable.getColumn("Descriptions").getModelIndex()));
                    filters.add(RowFilter.orFilter(accessionFilters));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for protein accession/description.", "Filter Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            text = peptideSequenceTxt.getText().trim();
            if (!text.equals("")) {
                try {
                    filters.add(RowFilter.regexFilter(text, peptideTable.getColumn("Sequence").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for peptide proteins.", "Filter Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            if (peptidePICmb.getSelectedIndex() > 0) {
                int pi = peptidePICmb.getSelectedIndex() - 1;
                filters.add(RowFilter.numberFilter(getComparisonType(peptidePiComparisonCmb.getSelectedIndex()), pi, peptideTable.getColumn("PI").getModelIndex()));
            }

            text = peptideNSpectraTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(peptideNSpectraCmb.getSelectedIndex()), value,
                        peptideTable.getColumn("# Spectra").getModelIndex()));
            }

            text = peptideScoreTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(peptideScoreCmb.getSelectedIndex()), value,
                        peptideTable.getColumn("Score").getModelIndex()));
            }

            text = peptideConfidenceTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(peptideConfidenceCmb.getSelectedIndex()), value,
                        peptideTable.getColumn("Confidence").getModelIndex()));
            }

            List<RowFilter<Object, Object>> ptmFilters = new ArrayList<RowFilter<Object, Object>>();
            List<RowFilter<Object, Object>> noPtmFilters = new ArrayList<RowFilter<Object, Object>>();
            boolean noPTMSelected = false;
            for (int row = 0; row < modificationTable.getRowCount(); row++) {
                text = (String) modificationTable.getValueAt(row, 1);
                if (!text.equals(PtmPanel.NO_MODIFICATION)) {
                    if ((Boolean) modificationTable.getValueAt(row, 0)) {
                        ptmFilters.add(RowFilter.regexFilter(text, peptideTable.getColumn("peptideKey").getModelIndex()));
                    }
                    noPtmFilters.add(RowFilter.regexFilter(text, peptideTable.getColumn("peptideKey").getModelIndex()));
                } else if ((Boolean) modificationTable.getValueAt(row, 0)) {
                    noPTMSelected = true;
                }
            }
            if (noPTMSelected) {
                ptmFilters.add(RowFilter.notFilter(RowFilter.orFilter(noPtmFilters)));
            }
            filters.add(RowFilter.orFilter(ptmFilters));

            // set the filters to the table
            RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);
            ((TableRowSorter) peptideTable.getRowSorter()).setRowFilter(allFilters);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Creates a peptide filter based on the users input
     */
    public void createPeptideFilter() {
        if (validateInput()) {
            Integer pi = null;
            if (peptidePICmb.getSelectedIndex() != 0) {
                pi = peptidePICmb.getSelectedIndex() - 1;
            }
            ArrayList<String> modifications = new ArrayList<String>();
            for (int row = 0; row < modificationTable.getRowCount(); row++) {
                if ((Boolean) modificationTable.getValueAt(row, 0)) {
                    modifications.add((String) modificationTable.getValueAt(row, 1));
                }
            }
            if (peptideFilter == null) {
                peptideFilter = new PeptideFilter("find peptide filter", modifications);
            } else {
                peptideFilter.setModificationStatus(modifications);
            }
            if (!peptideSequenceTxt.getText().trim().equals("")) {
                peptideFilter.setSequence(peptideSequenceTxt.getText().trim());
            }
            if (!peptideProteinTxt.getText().trim().equals("")) {
                peptideFilter.setProtein(peptideProteinTxt.getText().trim());
            }
            if (pi != null) {
                peptideFilter.setPi(pi);
            }
            if (!peptideNSpectraTxt.getText().trim().equals("")) {
                peptideFilter.setNSpectra(new Integer(peptideNSpectraTxt.getText().trim()));
                peptideFilter.setnSpectraComparison(getComparisonType(peptideNSpectraCmb.getSelectedIndex()));
            }
            if (!peptideScoreTxt.getText().trim().equals("")) {
                peptideFilter.setPeptideScore(new Double(peptideScoreTxt.getText().trim()));
                peptideFilter.setPeptideScoreComparison(getComparisonType(peptideScoreCmb.getSelectedIndex()));
            }
            if (!peptideConfidenceTxt.getText().trim().equals("")) {
                peptideFilter.setPeptideConfidence(new Double(peptideConfidenceTxt.getText().trim()));
                peptideFilter.setPeptideConfidenceComparison(getComparisonType(peptideConfidenceCmb.getSelectedIndex()));
            }
            peptideFilter.setManualValidation(parseAccessions(peptideManualValidationTxt.getText()));
            peptideFilter.setExceptions(parseAccessions(peptideExceptionsTxt.getText()));
        }
    }

    /**
     * Table model for the peptide table
     * @TODO: Can we hide the last column?
     */
    private class PeptideTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return identification.getPeptideIdentification().size();
        }

        @Override
        public int getColumnCount() {
            return 11;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Index";
                case 1:
                    return "Starred";
                case 2:
                    return "Hidden";
                case 3:
                    return "Proteins";
                case 4:
                    return "Descriptions";
                case 5:
                    return "PI";
                case 6:
                    return "Sequence";
                case 7:
                    return "# PSMs";
                case 8:
                    return "Score";
                case 9:
                    return "Confidence";
                case 10:
                    return "peptideKey";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String peptideKey = identification.getPeptideIdentification().get(row);
                PSParameter psParameter;
                PeptideMatch peptideMatch;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.isStarred();
                    case 2:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.isHidden();
                    case 3:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        String accessions = "";
                        for (String accession : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                            accessions += accession + " ";
                        }
                        return accessions;
                    case 4:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        String descriptions = "";
                        for (String accession : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                            descriptions += sequenceFactory.getHeader(accession).getDescription() + " ";
                        }
                        return descriptions;
                    case 5:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getGroupClass();
                    case 6:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        return peptideMatch.getTheoreticPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                    case 7:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        return peptideMatch.getSpectrumCount();
                    case 8:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getPeptideScore();
                    case 9:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getPeptideConfidence();
                    case 10:
                        return peptideKey;
                    default:
                        return "";
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
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
     * Filters the psm table according to the current filter settings.
     */
    private void filterPsms() {

        if (validateInput()) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

            String text = precursorRTTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(precursorRTCmb.getSelectedIndex()), value,
                        psmTable.getColumn("Retention Time").getModelIndex()));
            }

            text = precursorMzTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(precursorMzCmb.getSelectedIndex()), value,
                        psmTable.getColumn("Precursor m/z").getModelIndex()));
            }

            text = precursorErrorTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(precursorErrorCmb.getSelectedIndex()), value,
                        psmTable.getColumn("Precursor m/z error").getModelIndex()));
            }


            text = psmConfidenceTxt.getText().trim();
            if (!text.equals("")) {
                Double value = new Double(text);
                filters.add(RowFilter.numberFilter(getComparisonType(psmConfidenceCmb.getSelectedIndex()), value,
                        peptideTable.getColumn("Confidence").getModelIndex()));
            }

            List<RowFilter<Object, Object>> chargeFilters = new ArrayList<RowFilter<Object, Object>>();
            for (int row = 0; row < chargeTable.getRowCount(); row++) {
                if ((Boolean) chargeTable.getValueAt(row, 0)) {
                    Integer value = (Integer) chargeTable.getValueAt(row, 1);
                    chargeFilters.add(RowFilter.numberFilter(ComparisonType.EQUAL, value,
                            psmTable.getColumn("Identification Charge").getModelIndex()));
                }
            }
            filters.add(RowFilter.orFilter(chargeFilters));

            List<RowFilter<Object, Object>> filesFilters = new ArrayList<RowFilter<Object, Object>>();
            for (int row = 0; row < fileTable.getRowCount(); row++) {
                if ((Boolean) fileTable.getValueAt(row, 0)) {
                    text = (String) fileTable.getValueAt(row, 1);
                    filesFilters.add(RowFilter.regexFilter(text, fileTable.getColumn("File").getModelIndex()));
                }
            }
            filters.add(RowFilter.orFilter(chargeFilters));

            // set the filters to the table
            RowFilter<Object, Object> allFilters = RowFilter.andFilter(filters);
            ((TableRowSorter) psmTable.getRowSorter()).setRowFilter(allFilters);

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Creates the PSM filter based on the users input
     */
    public void createPsmFilter() {
        if (validateInput()) {
            ArrayList<Integer> charges = new ArrayList<Integer>();
            for (int row = 0; row < chargeTable.getRowCount(); row++) {
                if ((Boolean) chargeTable.getValueAt(row, 0)) {
                    charges.add((Integer) chargeTable.getValueAt(row, 1));
                }
            }
            ArrayList<String> files = new ArrayList<String>();
            for (int row = 0; row < fileTable.getRowCount(); row++) {
                if ((Boolean) fileTable.getValueAt(row, 0)) {
                    files.add((String) fileTable.getValueAt(row, 1));
                }
            }
            if (psmFilter == null) {
                psmFilter = new PsmFilter("find psm filter", charges, files);
            } else {
                psmFilter.setCharges(charges);
                psmFilter.setFileNames(files);
            }
            if (!precursorRTTxt.getText().trim().equals("")) {
                psmFilter.setPrecursorRT(new Double(precursorRTTxt.getText().trim()));
                psmFilter.setPrecursorRTComparison(getComparisonType(precursorRTCmb.getSelectedIndex()));
            }
            if (!precursorMzTxt.getText().trim().equals("")) {
                psmFilter.setPrecursorMz(new Double(precursorMzTxt.getText().trim()));
                psmFilter.setPrecursorMzComparison(getComparisonType(precursorMzCmb.getSelectedIndex()));
            }
            if (!precursorErrorTxt.getText().trim().equals("")) {
                psmFilter.setPrecursorMzError(new Double(precursorErrorTxt.getText().trim()));
                psmFilter.setPrecursorMzErrorComparison(getComparisonType(precursorErrorCmb.getSelectedIndex()));
            }
            if (!psmConfidenceTxt.getText().trim().equals("")) {
                psmFilter.setPsmConfidence(new Double(psmConfidenceTxt.getText().trim()));
                psmFilter.setPsmConfidenceComparison(getComparisonType(psmConfidenceCmb.getSelectedIndex()));
            }
            psmFilter.setManualValidation(parseAccessions(psmManualValidationTxt.getText()));
            psmFilter.setExceptions(parseAccessions(psmExceptionsTxt.getText()));
        }
    }

    /**
     * Table model for the psm table
     */
    private class PsmTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return identification.getSpectrumIdentification().size();
        }

        @Override
        public int getColumnCount() {
            return 10;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Index";
                case 1:
                    return "Starred";
                case 2:
                    return "Hidden";
                case 3:
                    return "File";
                case 4:
                    return "Title";
                case 5:
                    return "Retention Time";
                case 6:
                    return "Precursor m/z";
                case 7:
                    return "Identification Charge";
                case 8:
                    return "Precursor m/z error";
                case 9:
                    return "Confidence";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String spectrumKey = identification.getSpectrumIdentification().get(row);
                PSParameter psParameter;
                SpectrumMatch spectrumMatch;
                Precursor precursor;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, new PSParameter());
                        return psParameter.isStarred();
                    case 2:
                        psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, new PSParameter());
                        return psParameter.isHidden();
                    case 3:
                        return Spectrum.getSpectrumFile(spectrumKey);
                    case 4:
                        return Spectrum.getSpectrumTitle(spectrumKey);
                    case 5:
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey);
                        return precursor.getRt();
                    case 6:
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey);
                        return precursor.getMz();
                    case 7:
                        spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        return spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                    case 8:
                        spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey);
                        return Math.abs(spectrumMatch.getBestAssumption().getDeltaMass(precursor.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()));
                    case 9:
                        psParameter = (PSParameter) identification.getMatchParameter(spectrumKey, new PSParameter());
                        return psParameter.getPsmConfidence();
                    default:
                        return "";
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
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
     * Indicates whether something was input for the protein filter
     * @return a boolean indicating whether something was input for the protein filter
     */
    private boolean proteinInput() {
        return !proteinAccessionTxt.getText().trim().equals("")
                || proteinPICmb.getSelectedIndex() > 0
                || !spectrumCountingTxt.getText().trim().equals("")
                || !proteinCoverageTxt.getText().trim().equals("")
                || !nPeptidesTxt.getText().trim().equals("")
                || !proteinsNSpectraTxt.getText().trim().equals("")
                || !proteinScoreTxt.getText().trim().equals("")
                || !proteinCoverageTxt.getText().trim().equals("");
    }

    /**
     * Indicates whether something was input for the peptide filter
     * @return a boolean indicating whether something was input for the peptide filter
     */
    private boolean peptideInput() {
        if (!peptideProteinTxt.getText().trim().equals("")
                || !peptideSequenceTxt.getText().trim().equals("")
                || peptidePICmb.getSelectedIndex() > 0
                || !peptideNSpectraTxt.getText().trim().equals("")
                || !peptideScoreTxt.getText().trim().equals("")
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

    /**
     * Indicates whether something was input for the psm filter
     * @return a boolean indicating whether something was input for the psm filter
     */
    private boolean psmInput() {
        if (!precursorRTTxt.getText().trim().equals("")
                || !precursorMzTxt.getText().trim().equals("")
                || !precursorErrorTxt.getText().trim().equals("")
                || !psmConfidenceTxt.getText().trim().equals("")) {
            return true;
        }
        for (int row = 0; row < chargeTable.getRowCount(); row++) {
            if (!(Boolean) chargeTable.getValueAt(row, 0)) {
                return true;
            }
        }
        for (int row = 0; row < fileTable.getRowCount(); row++) {
            if (!(Boolean) fileTable.getValueAt(row, 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience method parsing keys in the manual validation/exception text fields
     * @param text  the text in the text field
     * @return      a list of the parsed keys
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

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        exitButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        proteinTable = new javax.swing.JTable();
        proteinSplit = new javax.swing.JSplitPane();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        spectrumCountingTxt = new javax.swing.JTextField();
        proteinCoverageCmb = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
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
        proteinManualSplit = new javax.swing.JSplitPane();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        proteinManualValidationTxt = new javax.swing.JTextArea();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        proteinExceptionsTxt = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        peptideTable = new javax.swing.JTable();
        peptideSplit = new javax.swing.JSplitPane();
        jPanel7 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        peptideProteinTxt = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        peptidePICmb = new javax.swing.JComboBox();
        jLabel15 = new javax.swing.JLabel();
        peptideNSpectraTxt = new javax.swing.JTextField();
        peptideNSpectraCmb = new javax.swing.JComboBox();
        jLabel16 = new javax.swing.JLabel();
        peptideConfidenceTxt = new javax.swing.JTextField();
        peptideConfidenceCmb = new javax.swing.JComboBox();
        peptideScoreCmb = new javax.swing.JComboBox();
        peptideScoreTxt = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        peptideSequenceTxt = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        modificationTable = new javax.swing.JTable();
        peptidePiComparisonCmb = new javax.swing.JComboBox();
        peptideManualSplit = new javax.swing.JSplitPane();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane9 = new javax.swing.JScrollPane();
        peptideManualValidationTxt = new javax.swing.JTextArea();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        peptideExceptionsTxt = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        psmTable = new javax.swing.JTable();
        psmSplit = new javax.swing.JSplitPane();
        jPanel6 = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        precursorRTTxt = new javax.swing.JTextField();
        precursorRTCmb = new javax.swing.JComboBox();
        jLabel23 = new javax.swing.JLabel();
        psmConfidenceTxt = new javax.swing.JTextField();
        psmConfidenceCmb = new javax.swing.JComboBox();
        jLabel24 = new javax.swing.JLabel();
        precursorMzTxt = new javax.swing.JTextField();
        precursorMzCmb = new javax.swing.JComboBox();
        jLabel25 = new javax.swing.JLabel();
        precursorErrorTxt = new javax.swing.JTextField();
        precursorErrorCmb = new javax.swing.JComboBox();
        jLabel26 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        chargeTable = new javax.swing.JTable();
        jScrollPane7 = new javax.swing.JScrollPane();
        fileTable = new javax.swing.JTable();
        jLabel27 = new javax.swing.JLabel();
        psmManualSplit = new javax.swing.JSplitPane();
        jPanel12 = new javax.swing.JPanel();
        jScrollPane11 = new javax.swing.JScrollPane();
        psmManualValidationTxt = new javax.swing.JTextArea();
        jPanel13 = new javax.swing.JPanel();
        jScrollPane12 = new javax.swing.JScrollPane();
        psmExceptionsTxt = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        exitButton.setText("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        saveButton.setText("Save Filter");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        proteinTable.setModel(new ProteinTable());
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinTableMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(proteinTable);

        proteinSplit.setDividerLocation(200);
        proteinSplit.setDividerSize(0);
        proteinSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));

        jLabel1.setText("Identifier:");

        jLabel3.setText("PI Status:");

        jLabel4.setText("Spectrum Counting:");

        jLabel5.setText("# Peptides:");

        jLabel6.setText("# Spectra:");

        jLabel7.setText("Score:");

        jLabel8.setText("Coverage:");

        jLabel9.setText("Confidence:");

        spectrumCountingTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                spectrumCountingTxtKeyReleased(evt);
            }
        });

        proteinCoverageCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinCoverageCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinCoverageCmbActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Tahoma", 2, 11));
        jLabel10.setText("RegExp");

        proteinAccessionTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinAccessionTxtActionPerformed(evt);
            }
        });
        proteinAccessionTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinAccessionTxtKeyReleased(evt);
            }
        });

        proteinPICmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No requirement", "Single Protein", "Isoforms", "Isoforms and Unrelated protein(s)", "Unrelated proteins" }));
        proteinPICmb.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinPICmbKeyReleased(evt);
            }
        });

        proteinConfidenceTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinConfidenceTxtKeyReleased(evt);
            }
        });

        proteinConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinConfidenceCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinConfidenceCmbActionPerformed(evt);
            }
        });

        proteinScoreTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinScoreTxtKeyReleased(evt);
            }
        });

        proteinScoreCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinScoreCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinScoreCmbActionPerformed(evt);
            }
        });

        proteinsNSpectraTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinsNSpectraTxtKeyReleased(evt);
            }
        });

        proteinNSpectraCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        proteinNSpectraCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinNSpectraCmbActionPerformed(evt);
            }
        });

        nPeptidesTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                nPeptidesTxtKeyReleased(evt);
            }
        });

        nPeptidesCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        nPeptidesCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nPeptidesCmbActionPerformed(evt);
            }
        });

        proteinCoverageTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinCoverageTxtKeyReleased(evt);
            }
        });

        spectrumCountingCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        spectrumCountingCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumCountingCmbActionPerformed(evt);
            }
        });

        proteinPiComparisonCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=" }));
        proteinPiComparisonCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinPiComparisonCmbActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel8)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(proteinAccessionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                        .addGap(62, 62, 62))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(proteinPICmb, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(proteinCoverageTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spectrumCountingTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(proteinCoverageCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spectrumCountingCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(proteinPiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nPeptidesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                    .addComponent(proteinsNSpectraTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                    .addComponent(proteinScoreTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                    .addComponent(proteinConfidenceTxt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(nPeptidesCmb, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinNSpectraCmb, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinScoreCmb, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(nPeptidesTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(proteinsNSpectraTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(proteinScoreTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(proteinConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel9)))
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                            .addComponent(nPeptidesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(proteinNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(proteinScoreCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(proteinConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel5Layout.createSequentialGroup()
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel1)
                                .addComponent(jLabel10)
                                .addComponent(proteinAccessionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel3)
                                .addComponent(proteinPICmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(proteinPiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel4)
                                .addComponent(spectrumCountingTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(spectrumCountingCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel8)
                                .addComponent(proteinCoverageTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(proteinCoverageCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(5, 5, 5))))
                .addContainerGap(48, Short.MAX_VALUE))
        );

        proteinSplit.setLeftComponent(jPanel5);

        proteinManualSplit.setDividerLocation(380);
        proteinManualSplit.setDividerSize(0);

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));

        proteinManualValidationTxt.setColumns(20);
        proteinManualValidationTxt.setRows(5);
        jScrollPane4.setViewportView(proteinManualValidationTxt);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinManualSplit.setLeftComponent(jPanel8);

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder("Exception(s)"));

        proteinExceptionsTxt.setColumns(20);
        proteinExceptionsTxt.setRows(5);
        jScrollPane8.setViewportView(proteinExceptionsTxt);

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinManualSplit.setRightComponent(jPanel9);

        proteinSplit.setRightComponent(proteinManualSplit);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinSplit, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 760, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinSplit, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Proteins", jPanel2);

        peptideTable.setModel(new PeptideTable());
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
            }
        });
        jScrollPane3.setViewportView(peptideTable);

        peptideSplit.setDividerLocation(200);
        peptideSplit.setDividerSize(0);
        peptideSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));

        jLabel12.setText("Protein:");

        peptideProteinTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideProteinTxtKeyReleased(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Tahoma", 2, 11));
        jLabel13.setText("RegExp");

        jLabel14.setText("PI Status:");

        peptidePICmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No requirement", "Single Protein", "Isoforms", "Isoforms and Unrelated protein(s)", "Unrelated proteins" }));
        peptidePICmb.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptidePICmbKeyReleased(evt);
            }
        });

        jLabel15.setText("# Spectra");

        peptideNSpectraTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideNSpectraTxtKeyReleased(evt);
            }
        });

        peptideNSpectraCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptideNSpectraCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideNSpectraCmbActionPerformed(evt);
            }
        });

        jLabel16.setText("Confidence:");

        peptideConfidenceTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideConfidenceTxtActionPerformed(evt);
            }
        });
        peptideConfidenceTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideConfidenceTxtKeyReleased(evt);
            }
        });

        peptideConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptideConfidenceCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideConfidenceCmbActionPerformed(evt);
            }
        });

        peptideScoreCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        peptideScoreCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideScoreCmbActionPerformed(evt);
            }
        });

        peptideScoreTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideScoreTxtKeyReleased(evt);
            }
        });

        jLabel17.setText("Score:");

        jLabel19.setText("Sequence:");

        peptideSequenceTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideSequenceTxtKeyReleased(evt);
            }
        });

        jLabel20.setFont(new java.awt.Font("Tahoma", 2, 11));
        jLabel20.setText("RegExp");

        jLabel21.setText("Modifications:");

        modificationTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "PTM"
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
        modificationTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                modificationTableMouseReleased(evt);
            }
        });
        jScrollPane5.setViewportView(modificationTable);

        peptidePiComparisonCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=" }));
        peptidePiComparisonCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptidePiComparisonCmbActionPerformed(evt);
            }
        });
        peptidePiComparisonCmb.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptidePiComparisonCmbKeyReleased(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addComponent(jLabel15))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel19)
                            .addComponent(jLabel12))
                        .addGap(52, 52, 52)))
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(peptideProteinTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideSequenceTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(peptidePICmb, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                            .addComponent(peptideNSpectraTxt, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(peptidePiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(56, 56, 56)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17)
                            .addComponent(jLabel16))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 119, Short.MAX_VALUE)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(peptideConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideScoreTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(peptideScoreCmb, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel21)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel19)
                        .addGap(31, 31, 31))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(peptideProteinTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(peptideSequenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel20))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(peptidePICmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptidePiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15)
                            .addComponent(peptideNSpectraTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideNSpectraCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(peptideScoreCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(peptideConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(peptideScoreTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel17))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(peptideConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel16))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel21)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)))
                .addContainerGap())
        );

        peptideSplit.setLeftComponent(jPanel7);

        peptideManualSplit.setDividerLocation(380);
        peptideManualSplit.setDividerSize(0);

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));

        peptideManualValidationTxt.setColumns(20);
        peptideManualValidationTxt.setRows(5);
        jScrollPane9.setViewportView(peptideManualValidationTxt);

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideManualSplit.setLeftComponent(jPanel10);

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder("Exception(s)"));

        peptideExceptionsTxt.setColumns(20);
        peptideExceptionsTxt.setRows(5);
        jScrollPane10.setViewportView(peptideExceptionsTxt);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptideManualSplit.setRightComponent(jPanel11);

        peptideSplit.setRightComponent(peptideManualSplit);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peptideSplit, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 760, Short.MAX_VALUE))
                .addContainerGap(15, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideSplit, javax.swing.GroupLayout.PREFERRED_SIZE, 400, Short.MAX_VALUE)
                .addGap(7, 7, 7)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Peptides", jPanel3);

        psmTable.setModel(new PsmTable());
        psmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                psmTableMouseReleased(evt);
            }
        });
        jScrollPane2.setViewportView(psmTable);

        psmSplit.setDividerLocation(200);
        psmSplit.setDividerSize(0);
        psmSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));

        jLabel22.setText("Precursor RT:");

        precursorRTTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                precursorRTTxtKeyReleased(evt);
            }
        });

        precursorRTCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorRTCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorRTCmbActionPerformed(evt);
            }
        });

        jLabel23.setText("Confidence:");

        psmConfidenceTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmConfidenceTxtKeyReleased(evt);
            }
        });

        psmConfidenceCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        psmConfidenceCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmConfidenceCmbActionPerformed(evt);
            }
        });

        jLabel24.setText("Precursor m/z:");

        precursorMzTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                precursorMzTxtKeyReleased(evt);
            }
        });

        precursorMzCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorMzCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorMzCmbActionPerformed(evt);
            }
        });

        jLabel25.setText("Precursor Error:");

        precursorErrorTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                precursorErrorTxtKeyReleased(evt);
            }
        });

        precursorErrorCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursorErrorCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorErrorCmbActionPerformed(evt);
            }
        });

        jLabel26.setText("Charge:");

        chargeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Charge"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Integer.class
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
        chargeTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                chargeTableMouseReleased(evt);
            }
        });
        jScrollPane6.setViewportView(chargeTable);

        fileTable.setModel(new javax.swing.table.DefaultTableModel(
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
        fileTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                fileTableMouseReleased(evt);
            }
        });
        jScrollPane7.setViewportView(fileTable);

        jLabel27.setText("File:");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addGap(32, 32, 32)
                        .addComponent(precursorRTTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(precursorRTCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel24)
                            .addComponent(jLabel25)
                            .addComponent(jLabel26))
                        .addGap(21, 21, 21)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                                .addComponent(precursorMzTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(precursorMzCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                                .addComponent(precursorErrorTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(precursorErrorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 156, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel23)
                    .addComponent(jLabel27))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(psmConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(psmConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane7, 0, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(psmConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel23))
                            .addComponent(psmConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel27)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 121, Short.MAX_VALUE))
                            .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE)))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(precursorRTTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorRTCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel24)
                            .addComponent(precursorMzTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorMzCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel25)
                            .addComponent(precursorErrorTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorErrorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jLabel26)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 59, Short.MAX_VALUE))
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE))))
                .addContainerGap())
        );

        psmSplit.setLeftComponent(jPanel6);

        psmManualSplit.setDividerLocation(380);
        psmManualSplit.setDividerSize(0);

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder("Manual Validation"));

        psmManualValidationTxt.setColumns(20);
        psmManualValidationTxt.setRows(5);
        jScrollPane11.setViewportView(psmManualValidationTxt);

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane11, javax.swing.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane11, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmManualSplit.setLeftComponent(jPanel12);

        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder("Exception(s)"));

        psmExceptionsTxt.setColumns(20);
        psmExceptionsTxt.setRows(5);
        jScrollPane12.setViewportView(psmExceptionsTxt);

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane12, javax.swing.GroupLayout.DEFAULT_SIZE, 345, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane12, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmManualSplit.setRightComponent(jPanel13);

        psmSplit.setRightComponent(psmManualSplit);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(psmSplit, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 760, Short.MAX_VALUE))
                .addContainerGap(15, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmSplit, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("PSMs", jPanel4);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(492, 492, 492)
                        .addComponent(saveButton, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(exitButton, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 790, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 799, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exitButton)
                    .addComponent(saveButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
        dispose();
    }//GEN-LAST:event_exitButtonActionPerformed

    private void psmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseReleased
        int row = psmTable.getSelectedRow();
        String spectrumKey = identification.getSpectrumIdentification().get(row);
        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION, spectrumKey);
        peptideShakerGUI.updateSelectionInCurrentTab();
}//GEN-LAST:event_psmTableMouseReleased

    private void peptideTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseReleased
        int row = peptideTable.getSelectedRow();
        String peptideKey = identification.getPeptideIdentification().get(row);
        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, peptideKey, PeptideShakerGUI.NO_SELECTION);
        peptideShakerGUI.updateSelectionInCurrentTab();
}//GEN-LAST:event_peptideTableMouseReleased

    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased
        int row = proteinTable.getSelectedRow();
        String proteinKey = identification.getProteinIdentification().get(row);
        peptideShakerGUI.setSelectedItems(proteinKey, PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION);
        peptideShakerGUI.updateSelectionInCurrentTab();
}//GEN-LAST:event_proteinTableMouseReleased

    private void proteinAccessionTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinAccessionTxtActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinAccessionTxtActionPerformed

    private void proteinAccessionTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinAccessionTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinAccessionTxtKeyReleased

    private void proteinPICmbKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinPICmbKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinPICmbKeyReleased

    private void spectrumCountingTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectrumCountingTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_spectrumCountingTxtKeyReleased

    private void proteinCoverageTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinCoverageTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinCoverageTxtKeyReleased

    private void nPeptidesTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nPeptidesTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_nPeptidesTxtKeyReleased

    private void proteinsNSpectraTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinsNSpectraTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinsNSpectraTxtKeyReleased

    private void proteinScoreTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinScoreTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinScoreTxtKeyReleased

    private void proteinConfidenceTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinConfidenceTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinConfidenceTxtKeyReleased

    private void proteinPiComparisonCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinPiComparisonCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinPiComparisonCmbActionPerformed

    private void spectrumCountingCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumCountingCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_spectrumCountingCmbActionPerformed

    private void proteinCoverageCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinCoverageCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinCoverageCmbActionPerformed

    private void nPeptidesCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nPeptidesCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_nPeptidesCmbActionPerformed

    private void proteinNSpectraCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinNSpectraCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinNSpectraCmbActionPerformed

    private void proteinScoreCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinScoreCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinScoreCmbActionPerformed

    private void proteinConfidenceCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinConfidenceCmbActionPerformed
        filterProteins();
    }//GEN-LAST:event_proteinConfidenceCmbActionPerformed

    private void peptideProteinTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideProteinTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideProteinTxtKeyReleased

    private void peptideSequenceTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideSequenceTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideSequenceTxtKeyReleased

    private void peptidePICmbKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidePICmbKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptidePICmbKeyReleased

    private void peptidePiComparisonCmbKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidePiComparisonCmbKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptidePiComparisonCmbKeyReleased

    private void peptideNSpectraTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideNSpectraTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideNSpectraTxtKeyReleased

    private void peptideNSpectraCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideNSpectraCmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptideNSpectraCmbActionPerformed

    private void peptidePiComparisonCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptidePiComparisonCmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptidePiComparisonCmbActionPerformed

    private void peptideScoreTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideScoreTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideScoreTxtKeyReleased

    private void peptideScoreCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideScoreCmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptideScoreCmbActionPerformed

    private void peptideConfidenceTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideConfidenceTxtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_peptideConfidenceTxtActionPerformed

    private void peptideConfidenceTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideConfidenceTxtKeyReleased
        filterPeptides();
    }//GEN-LAST:event_peptideConfidenceTxtKeyReleased

    private void peptideConfidenceCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideConfidenceCmbActionPerformed
        filterPeptides();
    }//GEN-LAST:event_peptideConfidenceCmbActionPerformed

    private void modificationTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationTableMouseReleased
        filterPeptides();
    }//GEN-LAST:event_modificationTableMouseReleased

    private void precursorRTTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_precursorRTTxtKeyReleased
        filterPsms();
    }//GEN-LAST:event_precursorRTTxtKeyReleased

    private void precursorMzTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_precursorMzTxtKeyReleased
        filterPsms();
    }//GEN-LAST:event_precursorMzTxtKeyReleased

    private void precursorErrorTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_precursorErrorTxtKeyReleased
        filterPsms();
    }//GEN-LAST:event_precursorErrorTxtKeyReleased

    private void psmConfidenceTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmConfidenceTxtKeyReleased
        filterPsms();
    }//GEN-LAST:event_psmConfidenceTxtKeyReleased

    private void precursorRTCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorRTCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_precursorRTCmbActionPerformed

    private void precursorMzCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorMzCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_precursorMzCmbActionPerformed

    private void precursorErrorCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorErrorCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_precursorErrorCmbActionPerformed

    private void psmConfidenceCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmConfidenceCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_psmConfidenceCmbActionPerformed

    private void chargeTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_chargeTableMouseReleased
        filterPsms();
    }//GEN-LAST:event_chargeTableMouseReleased

    private void fileTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fileTableMouseReleased
        filterPsms();
    }//GEN-LAST:event_fileTableMouseReleased

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        if (proteinFilter != null) {
            createProteinFilter();
            if (peptideShakerGUI.getFilterPreferences().getProteinHideFilters().containsKey(proteinFilter.getName())) {
                peptideShakerGUI.getFilterPreferences().addHidingFilter(proteinFilter);
            } else {
                peptideShakerGUI.getFilterPreferences().addStarringFilter(proteinFilter);
            }
        }
        if (peptideFilter != null) {
            createPeptideFilter();
            if (peptideShakerGUI.getFilterPreferences().getPeptideHideFilters().containsKey(peptideFilter.getName())) {
                peptideShakerGUI.getFilterPreferences().addHidingFilter(peptideFilter);
            } else {
                peptideShakerGUI.getFilterPreferences().addStarringFilter(peptideFilter);
            }
        }
        if (psmFilter != null) {
            createPsmFilter();
            if (peptideShakerGUI.getFilterPreferences().getPsmHideFilters().containsKey(psmFilter.getName())) {
                peptideShakerGUI.getFilterPreferences().addHidingFilter(psmFilter);
            } else {
                peptideShakerGUI.getFilterPreferences().addStarringFilter(psmFilter);
            }
        }
        if (proteinFilter == null && proteinInput()
                || peptideFilter == null && peptideInput()
                || psmFilter == null && psmInput()) {
            // Sorry that bit is really not elegant...
            ProteinFilter newProteinFilter = null;
            PeptideFilter newPeptideFilter = null;
            PsmFilter newPsmFilter = null;
            if (proteinFilter == null && proteinInput()) {
                createProteinFilter();
                newProteinFilter = proteinFilter;
            }
            if (peptideFilter == null && peptideInput()) {
                createPeptideFilter();
                newPeptideFilter = peptideFilter;
            }
            if (psmFilter == null && psmInput()) {
                createPsmFilter();
                newPsmFilter = psmFilter;
            }
            new CreateFilterDialog(peptideShakerGUI, newProteinFilter, newPeptideFilter, newPsmFilter);
        }
    }//GEN-LAST:event_saveButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable chargeTable;
    private javax.swing.JButton exitButton;
    private javax.swing.JTable fileTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane12;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable modificationTable;
    private javax.swing.JComboBox nPeptidesCmb;
    private javax.swing.JTextField nPeptidesTxt;
    private javax.swing.JComboBox peptideConfidenceCmb;
    private javax.swing.JTextField peptideConfidenceTxt;
    private javax.swing.JTextArea peptideExceptionsTxt;
    private javax.swing.JSplitPane peptideManualSplit;
    private javax.swing.JTextArea peptideManualValidationTxt;
    private javax.swing.JComboBox peptideNSpectraCmb;
    private javax.swing.JTextField peptideNSpectraTxt;
    private javax.swing.JComboBox peptidePICmb;
    private javax.swing.JComboBox peptidePiComparisonCmb;
    private javax.swing.JTextField peptideProteinTxt;
    private javax.swing.JComboBox peptideScoreCmb;
    private javax.swing.JTextField peptideScoreTxt;
    private javax.swing.JTextField peptideSequenceTxt;
    private javax.swing.JSplitPane peptideSplit;
    private javax.swing.JTable peptideTable;
    private javax.swing.JComboBox precursorErrorCmb;
    private javax.swing.JTextField precursorErrorTxt;
    private javax.swing.JComboBox precursorMzCmb;
    private javax.swing.JTextField precursorMzTxt;
    private javax.swing.JComboBox precursorRTCmb;
    private javax.swing.JTextField precursorRTTxt;
    private javax.swing.JTextField proteinAccessionTxt;
    private javax.swing.JComboBox proteinConfidenceCmb;
    private javax.swing.JTextField proteinConfidenceTxt;
    private javax.swing.JComboBox proteinCoverageCmb;
    private javax.swing.JTextField proteinCoverageTxt;
    private javax.swing.JTextArea proteinExceptionsTxt;
    private javax.swing.JSplitPane proteinManualSplit;
    private javax.swing.JTextArea proteinManualValidationTxt;
    private javax.swing.JComboBox proteinNSpectraCmb;
    private javax.swing.JComboBox proteinPICmb;
    private javax.swing.JComboBox proteinPiComparisonCmb;
    private javax.swing.JComboBox proteinScoreCmb;
    private javax.swing.JTextField proteinScoreTxt;
    private javax.swing.JSplitPane proteinSplit;
    private javax.swing.JTable proteinTable;
    private javax.swing.JTextField proteinsNSpectraTxt;
    private javax.swing.JComboBox psmConfidenceCmb;
    private javax.swing.JTextField psmConfidenceTxt;
    private javax.swing.JTextArea psmExceptionsTxt;
    private javax.swing.JSplitPane psmManualSplit;
    private javax.swing.JTextArea psmManualValidationTxt;
    private javax.swing.JSplitPane psmSplit;
    private javax.swing.JTable psmTable;
    private javax.swing.JButton saveButton;
    private javax.swing.JComboBox spectrumCountingCmb;
    private javax.swing.JTextField spectrumCountingTxt;
    // End of variables declaration//GEN-END:variables
}
