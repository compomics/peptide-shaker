/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FindDialog.java
 *
 * Created on Nov 30, 2011, 3:24:41 PM
 */
package eu.isas.peptideshaker.gui;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import eu.isas.peptideshaker.filtering.PeptideFilter;
import eu.isas.peptideshaker.filtering.ProteinFilter;
import eu.isas.peptideshaker.filtering.PsmFilter;
import eu.isas.peptideshaker.gui.tabpanels.PtmPanel;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author vaudel
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

    /** Creates new form FindDialog */
    public FindDialog(PeptideShakerGUI peptideShakerGUI) {
        super(peptideShakerGUI, true);
        this.peptideShakerGUI = peptideShakerGUI;
        identification = peptideShakerGUI.getIdentification();

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
        setVisible(true);
    }

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
     * @param selectedItem the item selected
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
     * Filters the protein table according to the current filter settings.
     */
    public void filterProteins() {

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
                    filters.add(RowFilter.orFilter(accessionFilters));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for protein accession.", "Filter Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            // protein description filter
            text = proteinDescriptionTxt.getText().trim();
            if (!text.equals("")) {
                try {
                    filters.add(RowFilter.regexFilter(text, proteinTable.getColumn("Description").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for protein description.", "Filter Error", JOptionPane.ERROR_MESSAGE);
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

    public void createProteinFilter() {
        if (validateInput()) {
            Integer pi = null;
            if (proteinPICmb.getSelectedIndex() != 0) {
                pi = proteinPICmb.getSelectedIndex() - 1;
            }
            proteinFilter = new ProteinFilter("find protein filter");
            if (!proteinDescriptionTxt.getText().trim().equals("")) {
                proteinFilter.setDescriptionRegex(proteinDescriptionTxt.getText().trim());
            }
            if (pi != null) {
                proteinFilter.setPi(pi);
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
    public void filterPeptides() {

        if (validateInput()) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            List<RowFilter<Object, Object>> filters = new ArrayList<RowFilter<Object, Object>>();

            // protein accession filter
            String text = proteinAccessionTxt.getText().trim();
            if (!text.equals("")) {
                try {
                    filters.add(RowFilter.regexFilter(text, peptideTable.getColumn("Proteins").getModelIndex()));
                } catch (PatternSyntaxException pse) {
                    JOptionPane.showMessageDialog(this, "Incorrect regex pattern for peptide proteins.", "Filter Error", JOptionPane.ERROR_MESSAGE);
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
                    return "Proteins";
                case 4:
                    return "PI";
                case 5:
                    return "Sequence";
                case 6:
                    return "# PSMs";
                case 7:
                    return "Score";
                case 8:
                    return "Confidence";
                case 9:
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
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getGroupClass();
                    case 5:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        return peptideMatch.getTheoreticPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                    case 6:
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        return peptideMatch.getSpectrumCount();
                    case 7:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getPeptideScore();
                    case 8:
                        psParameter = (PSParameter) identification.getMatchParameter(peptideKey, new PSParameter());
                        return psParameter.getPeptideConfidence();
                    case 9:
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
    public void filterPsms() {

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
                filters.add(RowFilter.numberFilter(getComparisonType(precursrorMzCmb.getSelectedIndex()), value,
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
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
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
        proteinDescriptionTxt = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
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
        jPanel3 = new javax.swing.JPanel();
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
        jScrollPane3 = new javax.swing.JScrollPane();
        peptideTable = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        precursorRTTxt = new javax.swing.JTextField();
        precursorRTCmb = new javax.swing.JComboBox();
        jLabel23 = new javax.swing.JLabel();
        psmConfidenceTxt = new javax.swing.JTextField();
        psmConfidenceCmb = new javax.swing.JComboBox();
        jLabel24 = new javax.swing.JLabel();
        precursorMzTxt = new javax.swing.JTextField();
        precursrorMzCmb = new javax.swing.JComboBox();
        jLabel25 = new javax.swing.JLabel();
        precursorErrorTxt = new javax.swing.JTextField();
        precursorErrorCmb = new javax.swing.JComboBox();
        jLabel26 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        chargeTable = new javax.swing.JTable();
        jScrollPane7 = new javax.swing.JScrollPane();
        fileTable = new javax.swing.JTable();
        jLabel27 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        psmTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        exitButton.setText("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        saveButton.setText("Save Filter");

        proteinTable.setModel(new ProteinTable());
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinTableMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(proteinTable);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));

        jLabel1.setText("Accession:");

        jLabel2.setText("Description:");

        jLabel3.setText("PI status:");

        jLabel4.setText("Spectrum counting:");

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

        proteinDescriptionTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinDescriptionTxtActionPerformed(evt);
            }
        });
        proteinDescriptionTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinDescriptionTxtKeyReleased(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Tahoma", 2, 11));
        jLabel11.setText("RegExp");

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
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinPICmb, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinCoverageTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinDescriptionTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                    .addComponent(proteinAccessionTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                    .addComponent(spectrumCountingTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinPiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spectrumCountingCmb, 0, 48, Short.MAX_VALUE)
                    .addComponent(proteinCoverageCmb, 0, 48, Short.MAX_VALUE)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE))
                .addGap(62, 62, 62)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nPeptidesTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(proteinsNSpectraTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(proteinScoreTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(proteinConfidenceTxt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(nPeptidesCmb, javax.swing.GroupLayout.Alignment.LEADING, 0, 52, Short.MAX_VALUE)
                    .addComponent(proteinNSpectraCmb, javax.swing.GroupLayout.Alignment.LEADING, 0, 52, Short.MAX_VALUE)
                    .addComponent(proteinScoreCmb, javax.swing.GroupLayout.Alignment.LEADING, 0, 52, Short.MAX_VALUE)
                    .addComponent(proteinConfidenceCmb, 0, 52, Short.MAX_VALUE))
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
                    .addGroup(jPanel5Layout.createSequentialGroup()
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
                                    .addComponent(proteinAccessionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel10))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel2)
                                    .addComponent(proteinDescriptionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel11))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel3)
                                    .addComponent(proteinPICmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(proteinPiComparisonCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel4)
                                    .addComponent(spectrumCountingTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spectrumCountingCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(proteinCoverageTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(proteinCoverageCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Proteins", jPanel2);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Parameters"));

        jLabel12.setText("Protein:");

        peptideProteinTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideProteinTxtKeyReleased(evt);
            }
        });

        jLabel13.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        jLabel13.setText("RegExp");

        jLabel14.setText("PI status:");

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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(peptideConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideScoreTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(peptideScoreCmb, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel21)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE))
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
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE)))
                .addContainerGap())
        );

        peptideTable.setModel(new PeptideTable());
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
            }
        });
        jScrollPane3.setViewportView(peptideTable);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Peptides", jPanel3);

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

        precursrorMzCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "=", "!=", "<", ">" }));
        precursrorMzCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursrorMzCmbActionPerformed(evt);
            }
        });

        jLabel25.setText("Precursor error:");

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
                        .addComponent(precursorRTCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                            .addComponent(jLabel26)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(jScrollPane6, 0, 0, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel24)
                                .addComponent(jLabel25))
                            .addGap(21, 21, 21)
                            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                                    .addComponent(precursorMzTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(precursrorMzCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                                    .addComponent(precursorErrorTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(precursorErrorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addGap(40, 40, 40)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addGap(18, 18, 18)
                        .addComponent(psmConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(psmConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(psmConfidenceTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel23))
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(psmConfidenceCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(precursorRTTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorRTCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel24)
                            .addComponent(precursorMzTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursrorMzCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel25)
                            .addComponent(precursorErrorTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(precursorErrorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel26)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE)))
                    .addComponent(jLabel27))
                .addContainerGap())
        );

        psmTable.setModel(new PsmTable());
        psmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                psmTableMouseReleased(evt);
            }
        });
        jScrollPane2.setViewportView(psmTable);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("PSMs", jPanel4);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(492, 492, 492)
                .addComponent(saveButton, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(exitButton, javax.swing.GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jTabbedPane1)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 601, Short.MAX_VALUE)
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
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
    }//GEN-LAST:event_proteinAccessionTxtActionPerformed

    private void proteinDescriptionTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinDescriptionTxtActionPerformed
    }//GEN-LAST:event_proteinDescriptionTxtActionPerformed

    private void proteinAccessionTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinAccessionTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinAccessionTxtKeyReleased

    private void proteinDescriptionTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinDescriptionTxtKeyReleased
        filterProteins();
    }//GEN-LAST:event_proteinDescriptionTxtKeyReleased

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

    private void precursrorMzCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursrorMzCmbActionPerformed
        filterPsms();
    }//GEN-LAST:event_precursrorMzCmbActionPerformed

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
        // TODO add your handling code here:
    }//GEN-LAST:event_fileTableMouseReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable chargeTable;
    private javax.swing.JButton exitButton;
    private javax.swing.JTable fileTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
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
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable modificationTable;
    private javax.swing.JComboBox nPeptidesCmb;
    private javax.swing.JTextField nPeptidesTxt;
    private javax.swing.JComboBox peptideConfidenceCmb;
    private javax.swing.JTextField peptideConfidenceTxt;
    private javax.swing.JComboBox peptideNSpectraCmb;
    private javax.swing.JTextField peptideNSpectraTxt;
    private javax.swing.JComboBox peptidePICmb;
    private javax.swing.JComboBox peptidePiComparisonCmb;
    private javax.swing.JTextField peptideProteinTxt;
    private javax.swing.JComboBox peptideScoreCmb;
    private javax.swing.JTextField peptideScoreTxt;
    private javax.swing.JTextField peptideSequenceTxt;
    private javax.swing.JTable peptideTable;
    private javax.swing.JComboBox precursorErrorCmb;
    private javax.swing.JTextField precursorErrorTxt;
    private javax.swing.JTextField precursorMzTxt;
    private javax.swing.JComboBox precursorRTCmb;
    private javax.swing.JTextField precursorRTTxt;
    private javax.swing.JComboBox precursrorMzCmb;
    private javax.swing.JTextField proteinAccessionTxt;
    private javax.swing.JComboBox proteinConfidenceCmb;
    private javax.swing.JTextField proteinConfidenceTxt;
    private javax.swing.JComboBox proteinCoverageCmb;
    private javax.swing.JTextField proteinCoverageTxt;
    private javax.swing.JTextField proteinDescriptionTxt;
    private javax.swing.JComboBox proteinNSpectraCmb;
    private javax.swing.JComboBox proteinPICmb;
    private javax.swing.JComboBox proteinPiComparisonCmb;
    private javax.swing.JComboBox proteinScoreCmb;
    private javax.swing.JTextField proteinScoreTxt;
    private javax.swing.JTable proteinTable;
    private javax.swing.JTextField proteinsNSpectraTxt;
    private javax.swing.JComboBox psmConfidenceCmb;
    private javax.swing.JTextField psmConfidenceTxt;
    private javax.swing.JTable psmTable;
    private javax.swing.JButton saveButton;
    private javax.swing.JComboBox spectrumCountingCmb;
    private javax.swing.JTextField spectrumCountingTxt;
    // End of variables declaration//GEN-END:variables
}
