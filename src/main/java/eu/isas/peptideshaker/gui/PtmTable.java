package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.biology.ions.NeutralLoss;
import com.compomics.util.experiment.biology.ions.Ion;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.ions.impl.PeptideFragmentIon;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.modification.ModificationtableContent;
import com.compomics.util.experiment.massspectrometry.spectra.MSnSpectrum;
import com.compomics.util.experiment.mass_spectrometry.SpectrumFactory;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationParameters;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.JSparklinesDataset;
import no.uib.jsparklines.extra.CellHighlighterRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Table containing information about the peak annotation of a modified peptide.
 * Heavily based on the Fragment ion table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PtmTable extends JTable {

    /**
     * Instance of the main GUI class
     */
    private final PeptideShakerGUI peptideShakerGUI;
    /**
     * The table tooltips.
     */
    private ArrayList<String> tooltips = new ArrayList<>();
    /**
     * The peptide to display.
     */
    private final Peptide peptide;
    /**
     * The PTM to analyze.
     */
    private final Modification ptm;
    /**
     * Number of PTMs.
     */
    private int nPTM;
    /**
     * The spectrum keys.
     */
    private final ArrayList<String> spectrumKeys;
    /**
     * If true, area charts are used, false results in bar charts.
     */
    private boolean areaChart = false;
    /**
     * The max value for the area charts.
     */
    private double maxAreaChartValue = 0;
    /**
     * A list of the modification site indexes.
     */
    private final ArrayList<Integer> modificationSites;

    /**
     * Constructor.
     *
     * @param peptideShakerGUI PeptideShakerGUI parent
     * @param peptide the peptide
     * @param ptm the ptm
     * @param spectrumKeys the spectrum keys
     * @param areaChart if true an area chart version will be used, false
     * displays bar charts
     */
    public PtmTable(PeptideShakerGUI peptideShakerGUI, Peptide peptide, Modification ptm, ArrayList<String> spectrumKeys, boolean areaChart) {
        this.peptideShakerGUI = peptideShakerGUI;
        this.peptide = peptide;
        this.ptm = ptm;
        this.nPTM = 0;
        this.spectrumKeys = spectrumKeys;
        this.areaChart = areaChart;

        modificationSites = new ArrayList<>();

        if (peptide.isModified()) {
            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                if (modMatch.getModification().equals(ptm.getName())) {
                    modificationSites.add(modMatch.getModificationSite());
                    nPTM++;
                }
            }
        }

        setUpTable();

        // add the peptide sequence and indexes to the table
        addPeptideSequence();

        // add the values to the table
        if (areaChart) {
            insertAreaCharts();
        } else {
            insertBarCharts();
        }
    }

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {

            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return (String) tooltips.get(realIndex);
            }
        };
    }

    /**
     * Set up the table.
     */
    private void setUpTable() {

        // disallow column reordering
        getTableHeader().setReorderingAllowed(false);

        // control the cell selection
        setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // centrally align the column headers 
        TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
        JLabel label = (JLabel) renderer;
        label.setHorizontalAlignment(JLabel.CENTER);

        // set up the column headers, types and tooltips
        Vector columnHeaders = new Vector();
        ArrayList<Class> tempColumnTypes = new ArrayList<>();
        tooltips = new ArrayList<>();

        // the index column
        columnHeaders.add(" ");
        tempColumnTypes.add(java.lang.Integer.class);
        tooltips.add("a, b and c ion index");

        AnnotationParameters annotationPreferences = peptideShakerGUI.getIdentificationParameters().getAnnotationParameters();

        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.A_ION)) {
            columnHeaders.add("a");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("a-ion");
        }
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.B_ION)) {
            columnHeaders.add("b");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("b-ion");
        }
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.C_ION)) {
            columnHeaders.add("c");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("c-ion");
        }

        columnHeaders.add("AA");
        tempColumnTypes.add(java.lang.String.class);
        tooltips.add("amino acid sequence");

        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.X_ION)) {
            columnHeaders.add("x");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("x-ion");
        }
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Y_ION)) {
            columnHeaders.add("y");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("y-ion");
        }
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Z_ION)) {
            columnHeaders.add("z");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("z-ion");
        }

        // the second index column
        columnHeaders.add("  ");
        tempColumnTypes.add(java.lang.Integer.class);
        tooltips.add("x, y and z ion index");

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

        // set the max column widths
        int tempWidth = 30; // @TODO: maybe this should not be hardcoded?
        getColumn(" ").setMaxWidth(tempWidth);
        getColumn(" ").setMinWidth(tempWidth);
        getColumn("  ").setMaxWidth(tempWidth);
        getColumn("  ").setMinWidth(tempWidth);
        getColumn("AA").setMaxWidth(tempWidth);
        getColumn("AA").setMinWidth(tempWidth);

        // centrally align the columns in the fragment ions table
        getColumn(" ").setCellRenderer(new CellHighlighterRenderer(Color.LIGHT_GRAY, Color.YELLOW, SwingConstants.CENTER, "*"));
        getColumn("  ").setCellRenderer(new CellHighlighterRenderer(Color.LIGHT_GRAY, Color.YELLOW, SwingConstants.CENTER, "*"));
        getColumn("AA").setCellRenderer(new CellHighlighterRenderer(Color.LIGHT_GRAY, Color.YELLOW, SwingConstants.CENTER, "*"));
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

        // insert the sequence
        for (int i = 0; i < peptideSequence.length(); i++) {

            if (modificationSites.contains(i + 1)) {
                setValueAt(peptideSequence.charAt(i) + "*", i, getColumn("AA").getModelIndex());
            } else {
                setValueAt(peptideSequence.charAt(i), i, getColumn("AA").getModelIndex());
            }

            setValueAt(peptideSequence.length() - i, i, getColumn("  ").getModelIndex());
        }
    }

    /**
     * Insert area charts into the table.
     */
    private void insertAreaCharts() {

        AnnotationParameters annotationPreferences = peptideShakerGUI.getIdentificationParameters().getAnnotationParameters();
        ModificationtableContent tempContent;
        PtmtableContent tableContent = new ModificationtableContent();
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

        for (String spectrumKey : spectrumKeys) {
            try {
                MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                SpectrumMatch spectrumMatch = (SpectrumMatch)peptideShakerGUI.getIdentification().retrieveObject(spectrumKey);
                peptideShakerGUI.setSpecificAnnotationPreferences(new SpecificAnnotationParameters(spectrumKey, spectrumMatch.getBestPeptideAssumption()));
                peptideShakerGUI.updateAnnotationPreferences();
                tempContent = ModificationtableContent.getModificationTableContent(peptide, ptm, nPTM, spectrum, annotationPreferences, peptideShakerGUI.getSpecificAnnotationPreferences());
                tempContent.normalize();
                tableContent.addAll(tempContent);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }

        maxAreaChartValue = 0;

        for (int aa = 0; aa < peptide.getSequence().length(); aa++) {

            int column = 1;
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.A_ION)) {
                addAreaChart(tableContent, PeptideFragmentIon.A_ION, aa + 1, column);
                column++;
            }
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.B_ION)) {
                addAreaChart(tableContent, PeptideFragmentIon.B_ION, aa + 1, column);
                column++;
            }
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.C_ION)) {
                addAreaChart(tableContent, PeptideFragmentIon.C_ION, aa + 1, column);
                column++;
            }

            column++;
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.X_ION)) {
                addAreaChart(tableContent, PeptideFragmentIon.X_ION, peptide.getSequence().length() - aa, column);
                column++;
            }
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Y_ION)) {
                addAreaChart(tableContent, PeptideFragmentIon.Y_ION, peptide.getSequence().length() - aa, column);
                column++;
            }
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Z_ION)) {
                addAreaChart(tableContent, PeptideFragmentIon.Z_ION, peptide.getSequence().length() - aa, column);
                column++;
            }
        }

        // set the column renderers
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.A_ION)) {
            try {
                getColumn("a").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.B_ION)) {
            try {
                getColumn("b").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.C_ION)) {
            try {
                getColumn("c").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }

        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.X_ION)) {
            try {
                getColumn("x").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Y_ION)) {
            try {
                getColumn("y").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
        if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Z_ION)) {
            try {
                getColumn("z").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
    }

    /**
     * Set up and add area charts.
     *
     * @param tableContent
     * @param fragmentIonType
     * @param aa
     * @param column
     */
    private void addAreaChart(ModificationtableContent tableContent, int fragmentIonType, int aa, int column) {

        ArrayList<JSparklinesDataSeries> sparkLineDataSeriesAll = new ArrayList<>();
        ArrayList<Double> data;
        int[] histogram;
        String modification = "";
        String shortName = ptm.getShortName();

        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {

            if (modCpt > 0) {
                if (modCpt == 1) {
                    modification = " <" + shortName + ">";
                } else {
                    modification = " <" + modCpt + shortName + ">";
                }
            }

            // add the data points to display to an arraylist 
            data = new ArrayList<>();

            histogram = tableContent.getHistogram(modCpt, fragmentIonType, aa, 50);

            data.add(0.0);

            for (int frequency : histogram) {
                if (frequency > 0) {
                    data.add(new Double(frequency));

                    if (frequency > maxAreaChartValue) {
                        maxAreaChartValue = frequency;
                    }
                }
            }

            data.add(0.0);

            // find area color
            double colorCoef;
            if (nPTM == 0) {
                colorCoef = 1;
            } else {
                colorCoef = 1.0 - ((1.0 * modCpt) / nPTM);
            }

            Ion genericIon = Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, fragmentIonType, new NeutralLoss[0]);
            Color areaColor = SpectrumPanel.determineFragmentIonColor(genericIon, false);
            areaColor = new Color((int) (colorCoef * areaColor.getRed()), (int) (colorCoef * areaColor.getGreen()), (int) (colorCoef * areaColor.getBlue()));
            String tooltip = "<html>" + PeptideFragmentIon.getSubTypeAsString(fragmentIonType) + "<sub>" + aa + "</sub>" + modification + "</html>";

            // create a JSparklineDataSeries  
            JSparklinesDataSeries sparklineDataseries = new JSparklinesDataSeries(data, areaColor, tooltip);

            // add the data series to JSparklineDataset 
            sparkLineDataSeriesAll.add(sparklineDataseries);
        }
        JSparklinesDataset dataset = new JSparklinesDataset(sparkLineDataSeriesAll);

        // add the data to the table 
        setValueAt(dataset, aa - 1, column);
    }

    /**
     * Insert bar charts into the table.
     */
    private void insertBarCharts() {

        AnnotationParameters annotationPreferences = peptideShakerGUI.getIdentificationParameters().getAnnotationParameters();
        ModificationtableContent tempContent;
        PtmtableContent tableContent = new ModificationtableContent();
        MSnSpectrum spectrum;
        SpectrumMatch spectrumMatch;
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        String shortName = ptm.getShortName();

        for (String spectrumKey : spectrumKeys) {
            try {
                spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                spectrumMatch = (SpectrumMatch)peptideShakerGUI.getIdentification().retrieveObject(spectrumKey);
                peptideShakerGUI.setSpecificAnnotationPreferences(new SpecificAnnotationParameters(spectrumKey, spectrumMatch.getBestPeptideAssumption()));
                peptideShakerGUI.updateAnnotationPreferences();
                tempContent = ModificationtableContent.getModificationTableContent(peptide, ptm, nPTM, spectrum, annotationPreferences, peptideShakerGUI.getSpecificAnnotationPreferences());
                tempContent.normalize();
                tableContent.addAll(tempContent);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }

        for (int aa = 0; aa < peptide.getSequence().length(); aa++) {

            int column = 1;

            for (int modCpt = 0; modCpt <= nPTM; modCpt++) {
                if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.A_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIon.A_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.B_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIon.B_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.C_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIon.C_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
            }

            column++;

            for (int modCpt = 0; modCpt <= nPTM; modCpt++) {
                if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.X_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIon.X_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Y_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIon.Y_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Z_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIon.Z_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
            }
        }

        // set the column renderers
        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {

            String modification = "";

            if (modCpt > 0) {
                if (modCpt == 1) {
                    modification = " <" + shortName + ">";
                } else {
                    modification = " <" + modCpt + shortName + ">";
                }
            }

            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.A_ION)) {
                try {
                    getColumn("a" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(),
                            SpectrumPanel.determineFragmentIonColor(new PeptideFragmentIon(PeptideFragmentIon.A_ION), false)));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("a" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.B_ION)) {
                try {
                    getColumn("b" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(),
                            SpectrumPanel.determineFragmentIonColor(new PeptideFragmentIon(PeptideFragmentIon.B_ION), false)));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("b" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.C_ION)) {
                try {
                    getColumn("c" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(),
                            SpectrumPanel.determineFragmentIonColor(new PeptideFragmentIon(PeptideFragmentIon.C_ION), false)));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("c" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }

            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.X_ION)) {
                try {
                    getColumn("x" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(),
                            SpectrumPanel.determineFragmentIonColor(new PeptideFragmentIon(PeptideFragmentIon.X_ION), false)));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("x" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Y_ION)) {
                try {
                    getColumn("y" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(),
                            SpectrumPanel.determineFragmentIonColor(new PeptideFragmentIon(PeptideFragmentIon.Y_ION), false)));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("y" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getFragmentIonTypes().contains(PeptideFragmentIon.Z_ION)) {
                try {
                    getColumn("z" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(),
                            SpectrumPanel.determineFragmentIonColor(new PeptideFragmentIon(PeptideFragmentIon.Z_ION), false)));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("z" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
        }
    }
}
