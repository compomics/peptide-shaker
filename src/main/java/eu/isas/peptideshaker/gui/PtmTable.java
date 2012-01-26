package eu.isas.peptideshaker.gui;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import com.compomics.util.experiment.identification.ptm.PTMLocationScores;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.ptm.PtmtableContent;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
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
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The table tooltips.
     */
    private ArrayList<String> tooltips = new ArrayList<String>();
    /**
     * The peptide to display.
     */
    private Peptide peptide;
    /**
     * The PTM to analyse.
     */
    private PTM ptm;
    /**
     * Number of PTMs.
     */
    private int nPTM;
    /**
     * The spectrum keys.
     */
    private ArrayList<String> spectrumKeys;
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
    private ArrayList<Integer> modificationSites;

    /**
     * Constructor.
     * 
     * @param peptideShakerGUI  PeptideShakerGUI parent
     * @param peptide           the peptide
     * @param ptm               the ptm
     * @param spectrumKeys      the spectrum keys
     * @param areaChart         if true an area chart version will be used, false displays bar charts
     */
    public PtmTable(PeptideShakerGUI peptideShakerGUI, Peptide peptide, PTM ptm, ArrayList<String> spectrumKeys, boolean areaChart) {
        this.peptideShakerGUI = peptideShakerGUI;
        this.peptide = peptide;
        this.ptm = ptm;
        this.nPTM = 0;
        this.spectrumKeys = spectrumKeys;
        this.areaChart = areaChart;

        modificationSites = new ArrayList<Integer>();

        for (ModificationMatch modMatch : peptide.getModificationMatches()) {
            if (modMatch.getTheoreticPtm().equals(ptm.getName())) {
                modificationSites.add(modMatch.getModificationSite());
                nPTM++;
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
                String tip = null;
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                tip = (String) tooltips.get(realIndex);
                return tip;
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
        ArrayList<Class> tempColumnTypes = new ArrayList<Class>();
        tooltips = new ArrayList<String>();

        // the index column
        columnHeaders.add(" ");
        tempColumnTypes.add(java.lang.Integer.class);
        tooltips.add("a, b and c ion index");

        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();

        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.A_ION)) {
            columnHeaders.add("a");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("a-ion");
        }
        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.B_ION)) {
            columnHeaders.add("b");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("b-ion");
        }
        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.C_ION)) {
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


        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.X_ION)) {
            columnHeaders.add("x");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("x-ion");
        }
        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Y_ION)) {
            columnHeaders.add("y");
            if (areaChart) {
                tempColumnTypes.add(JSparklinesDataset.class);
            } else {
                tempColumnTypes.add(Double.class);
            }
            tooltips.add("y-ion");
        }
        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Z_ION)) {
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

        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        PtmtableContent tempContent, tableContent = new PtmtableContent();
        MSnSpectrum spectrum;
        SpectrumMatch spectrumMatch;
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

        for (String spectrumKey : spectrumKeys) {
            try {
                spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                tempContent = PTMLocationScores.getPTMTableContent(peptide, ptm, nPTM, spectrum, annotationPreferences.getIonTypes(),
                        annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(),
                        spectrumMatch.getBestAssumption().getIdentificationCharge().value,
                        annotationPreferences.getFragmentIonAccuracy(), spectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()));
                tempContent.normalize();
                tableContent.addAll(tempContent);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }

        maxAreaChartValue = 0;

        for (int aa = 0; aa < peptide.getSequence().length(); aa++) {

            int column = 1;
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.A_ION)) {
                addAreaChart(tableContent, PeptideFragmentIonType.A_ION, aa + 1, column);
                column++;
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.B_ION)) {
                addAreaChart(tableContent, PeptideFragmentIonType.B_ION, aa + 1, column);
                column++;
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.C_ION)) {
                addAreaChart(tableContent, PeptideFragmentIonType.C_ION, aa + 1, column);
                column++;
            }

            column++;
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.X_ION)) {
                addAreaChart(tableContent, PeptideFragmentIonType.X_ION, peptide.getSequence().length() - aa, column);
                column++;
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Y_ION)) {
                addAreaChart(tableContent, PeptideFragmentIonType.Y_ION, peptide.getSequence().length() - aa, column);
                column++;
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Z_ION)) {
                addAreaChart(tableContent, PeptideFragmentIonType.Z_ION, peptide.getSequence().length() - aa, column);
                column++;
            }
        }

        // set the column renderers

        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.A_ION)) {
            try {
                getColumn("a").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.B_ION)) {
            try {
                getColumn("b").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.C_ION)) {
            try {
                getColumn("c").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }

        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.X_ION)) {
            try {
                getColumn("x").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Y_ION)) {
            try {
                getColumn("y").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.lineChart, PlotOrientation.VERTICAL, maxAreaChartValue));
            } catch (IllegalArgumentException e) {
                // do nothing
            }
        }
        if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Z_ION)) {
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
    private void addAreaChart(PtmtableContent tableContent, PeptideFragmentIonType fragmentIonType, int aa, int column) {

        ArrayList<JSparklinesDataSeries> sparkLineDataSeriesAll = new ArrayList<JSparklinesDataSeries>();
        ArrayList<Double> data;
        int[] histogram;
        String modification = "";

        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {

            if (modCpt > 0) {
                if (modCpt == 1) {
                    modification = " <" + ptm.getShortName() + ">";
                } else {
                    modification = " <" + modCpt + ptm.getShortName() + ">";
                }
            }

            // add the data points to display to an arraylist 
            data = new ArrayList<Double>();

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
            Color areaColor = Color.lightGray;
            double colorCoef;
            if (nPTM == 0) {
                colorCoef = 1;
            } else {
                colorCoef = 1.0 - ((1.0 * modCpt) / nPTM);
            }
            String tooltip = null;

            switch (fragmentIonType) {
                case A_ION:
                    areaColor = SpectrumPanel.determineFragmentIonColor("a");
                    areaColor = new Color((int) (colorCoef * areaColor.getRed()), (int) (colorCoef * areaColor.getGreen()), (int) (colorCoef * areaColor.getBlue()));
                    tooltip = "<html>a<sub>" + aa + "</sub>" + modification + "</html>";
                    break;
                case B_ION:
                    areaColor = SpectrumPanel.determineFragmentIonColor("b");
                    areaColor = new Color((int) (colorCoef * areaColor.getRed()), (int) (colorCoef * areaColor.getGreen()), (int) (colorCoef * areaColor.getBlue()));
                    tooltip = "<html>b<sub>" + aa + "</sub>" + modification + "</html>";
                    break;
                case C_ION:
                    areaColor = SpectrumPanel.determineFragmentIonColor("c");
                    areaColor = new Color((int) (colorCoef * areaColor.getRed()), (int) (colorCoef * areaColor.getGreen()), (int) (colorCoef * areaColor.getBlue()));
                    tooltip = "<html>c<sub>" + aa + "</sub>" + modification + "</html>";
                    break;
                case X_ION:
                    areaColor = SpectrumPanel.determineFragmentIonColor("x");
                    areaColor = new Color((int) (colorCoef * areaColor.getRed()), (int) (colorCoef * areaColor.getGreen()), (int) (colorCoef * areaColor.getBlue()));
                    tooltip = "<html>x<sub>" + (peptide.getSequence().length() - aa + 1) + "</sub>" + modification + "</html>";
                    break;
                case Y_ION:
                    areaColor = SpectrumPanel.determineFragmentIonColor("y");
                    areaColor = new Color((int) (colorCoef * areaColor.getRed()), (int) (colorCoef * areaColor.getGreen()), (int) (colorCoef * areaColor.getBlue()));
                    tooltip = "<html>y<sub>" + (peptide.getSequence().length() - aa + 1) + "</sub>" + modification + "</html>";
                    break;
                case Z_ION:
                    areaColor = SpectrumPanel.determineFragmentIonColor("z");
                    areaColor = new Color((int) (colorCoef * areaColor.getRed()), (int) (colorCoef * areaColor.getGreen()), (int) (colorCoef * areaColor.getBlue()));
                    tooltip = "<html>z<sub>" + (peptide.getSequence().length() - aa + 1) + "</sub>" + modification + "</html>";
                    break;
            }

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

        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        PtmtableContent tempContent, tableContent = new PtmtableContent();
        MSnSpectrum spectrum;
        SpectrumMatch spectrumMatch;
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

        for (String spectrumKey : spectrumKeys) {
            try {
                spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey);
                spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                tempContent = PTMLocationScores.getPTMTableContent(peptide, ptm, nPTM, spectrum, annotationPreferences.getIonTypes(),
                        annotationPreferences.getNeutralLosses(), annotationPreferences.getValidatedCharges(),
                        spectrumMatch.getBestAssumption().getIdentificationCharge().value,
                        annotationPreferences.getFragmentIonAccuracy(), spectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()));
                tempContent.normalize();
                tableContent.addAll(tempContent);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }

        for (int aa = 0; aa < peptide.getSequence().length(); aa++) {

            int column = 1;

            for (int modCpt = 0; modCpt <= nPTM; modCpt++) {
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.A_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.A_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.B_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.B_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.C_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.C_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
            }

            column++;

            for (int modCpt = 0; modCpt <= nPTM; modCpt++) {
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.X_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.X_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Y_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.Y_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
                if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Z_ION)) {
                    setValueAt(tableContent.getQuantile(modCpt, PeptideFragmentIonType.Z_ION, aa + 1, 0.75), aa, column);
                    column++;
                }
            }
        }

        // set the column renderers
        for (int modCpt = 0; modCpt <= nPTM; modCpt++) {

            String modification = "";

            if (modCpt > 0) {
                if (modCpt == 1) {
                    modification = " <" + ptm.getShortName() + ">";
                } else {
                    modification = " <" + modCpt + ptm.getShortName() + ">";
                }
            }

            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.A_ION)) {
                try {
                    getColumn("a" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("a")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("a" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.B_ION)) {
                try {
                    getColumn("b" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("b")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("b" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.C_ION)) {
                try {
                    getColumn("c" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("c")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("c" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }

            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.X_ION)) {
                try {
                    getColumn("x" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("x")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("x" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Y_ION)) {
                try {
                    getColumn("y" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("y")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("y" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
            if (annotationPreferences.getIonTypes().contains(PeptideFragmentIonType.Z_ION)) {
                try {
                    getColumn("z" + modification).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, tableContent.getMaxIntensity(), SpectrumPanel.determineFragmentIonColor("z")));
                    ((JSparklinesBarChartTableCellRenderer) getColumn("z" + modification).getCellRenderer()).setMinimumChartValue(0);
                } catch (IllegalArgumentException e) {
                    // do nothing
                }
            }
        }
    }
}
