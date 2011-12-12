/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import com.compomics.util.experiment.identification.PTMLocationScores;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.ptm.PtmtableContent;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.renderers.AlignedTableCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

/**
 * Table containing information about the peak annotation of a modified peptide. Heavily copied from the Fragment ion table.
 *
 * @author Marc
 */
public class PtmTable extends JTable {

    /**
     * Instance of the main GUI class
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The table tooltips.
     */
    private ArrayList<String> tooltips = new ArrayList<String>();
    private Peptide peptide;
    private PTM ptm;
    private int nPTM;
    ArrayList<String> spectrumKeys;

    public PtmTable(PeptideShakerGUI peptideShakerGUI, Peptide peptide, PTM ptm, ArrayList<String> spectrumKeys) {
        this.peptideShakerGUI = peptideShakerGUI;
        this.peptide = peptide;
        this.ptm = ptm;
        nPTM = 0;
        this.spectrumKeys = spectrumKeys;
        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
            if (modMatch.getTheoreticPtm().equals(ptm.getName())) {
                nPTM++;
            }
        }

        setUpTable();

        // add the peptide sequence and indexes to the table
        addPeptideSequence();

        // add the values to the table
        insertBarCharts();
    }

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {

            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                tip = (String) tooltips.get(realIndex);
                return tip;
            }
        };
    }

    private void setUpTable() {

        // disallow column reordering
        getTableHeader().setReorderingAllowed(false);

        // controll the cell selection
        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(false);
        setCellSelectionEnabled(true);
        setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // centrally align the column headers in the fragment ions table
        TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
        JLabel label = (JLabel) renderer;
        label.setHorizontalAlignment(JLabel.CENTER);

        // set up the column headers, types and tooltips
        Vector columnHeaders = new Vector();
        ArrayList<Class> tempColumnTypes = new ArrayList<Class>();
        tooltips = new ArrayList<String>();

        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();

        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {

            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.A_ION)) {
                columnHeaders.add("a " + modCpt);
                tempColumnTypes.add(Double.class);
                tooltips.add("a-ion");
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.B_ION)) {
                columnHeaders.add("b " + modCpt);
                tempColumnTypes.add(Double.class);
                tooltips.add("b-ion");
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.C_ION)) {
                columnHeaders.add("c " + modCpt);
                tempColumnTypes.add(Double.class);
                tooltips.add("c-ion");
            }

            columnHeaders.add("AA " + modCpt);
            tempColumnTypes.add(java.lang.String.class);
            tooltips.add("amino acid sequence");

            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.X_ION)) {
                columnHeaders.add("x " + modCpt);
                tempColumnTypes.add(Double.class);
                tooltips.add("x-ion");
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Y_ION)) {
                columnHeaders.add("y " + modCpt);
                tempColumnTypes.add(Double.class);
                tooltips.add("y-ion");
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Z_ION)) {
                columnHeaders.add("z " + modCpt);
                tempColumnTypes.add(Double.class);
                tooltips.add("z-ion");
            }

        }

        final ArrayList<Class> columnTypes = tempColumnTypes;

        // set the table model
        setModel(new javax.swing.table.DefaultTableModel(
                new Vector(),
                columnHeaders) {

            public Class getColumnClass(int columnIndex) {
                return columnTypes.get(columnIndex);
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });

        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {
            // set the max column widths
            int tempWidth = 30; // @TODO: maybe this should not be hardcoded?
            getColumn("AA " + modCpt).setMaxWidth(tempWidth);
            getColumn("AA " + modCpt).setMinWidth(tempWidth);

            // centrally align the columns in the fragment ions table
            getColumn("AA " + modCpt).setCellRenderer(new AlignedTableCellRenderer(SwingConstants.CENTER, Color.LIGHT_GRAY));
        }
    }

    /**
     * Add the peptide and sequence indexes to the table.
     */
    private void addPeptideSequence() {

        String peptideSequence = peptide.getSequence();

        // add the peptide sequence and indexes to the table
        for (int i = 0; i < peptideSequence.length(); i++) {
            ((DefaultTableModel) getModel()).addRow(new Object[]{(i + 1)});
        }

        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {
            for (int i = 0; i < peptideSequence.length(); i++) {
                setValueAt(peptideSequence.charAt(i), i, getColumn("AA " + modCpt).getModelIndex());
            }
        }
    }

    /**
     * Insert bar charts into the table.
     */
    private void insertBarCharts() {

        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        PtmtableContent tempContent, tableContent = new PtmtableContent();
        MSnSpectrum spectrum;
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        for (String spectrumKey : spectrumKeys) {
            try {
                spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                tempContent = PTMLocationScores.getPTMTableContent(peptide, ptm, nPTM, spectrum, annotationPreferences.getIonTypes(),
                        annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(),
                        annotationPreferences.getFragmentIonAccuracy(), annotationPreferences.getAnnotationIntensityLimit());
                tempContent.normalize();
                tableContent.addAll(tempContent);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }
            for (int aa = 0; aa < peptide.getSequence().length(); aa++) {
        int column = 0;
        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.A_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.A_ION, aa+1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.B_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.B_ION, aa+1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.C_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.C_ION, aa+1, 0.75), aa, column);
                    column++;
                }

                column++;

                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.X_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.X_ION, aa+1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Y_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.Y_ION, aa+1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Z_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.Z_ION, aa+1, 0.75), aa, column);
                    column++;
                }
            }

        }

        // set the column renderers

        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {

            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.A_ION)) {
                try {
                    getColumn("a " + modCpt).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("a")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("a " + modCpt).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.B_ION)) {
                try {
                    getColumn("b " + modCpt).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("b")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("b " + modCpt).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.C_ION)) {
                try {
                    getColumn("c " + modCpt).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("c")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("c " + modCpt).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }

            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.X_ION)) {
                try {
                    getColumn("x " + modCpt).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("x")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("x " + modCpt).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Y_ION)) {
                try {
                    getColumn("y " + modCpt).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("y")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("y " + modCpt).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Z_ION)) {
                try {
                    getColumn("z " + modCpt).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("z")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("z " + modCpt).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
        }
    }
}
