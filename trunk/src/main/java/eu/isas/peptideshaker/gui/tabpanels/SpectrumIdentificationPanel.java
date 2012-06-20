package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.VennDiagram;
import eu.isas.peptideshaker.export.OutputGenerator;
import eu.isas.peptideshaker.gui.ExportGraphicsDialog;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.awt.Component;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import com.compomics.util.preferences.AnnotationPreferences;

/**
 * The Spectrum ID panel.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SpectrumIdentificationPanel extends javax.swing.JPanel {

    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * Needed in order to not update the file selection too many times.
     */
    private boolean updateSelection = true;

    /**
     * Indexes for the three main data tables.
     */
    private enum TableIndex {

        SEARCH_ENGINE_PERFORMANCE, SPECTRUM_FILES, PSM_TABLES
    };
    /**
     * Static index for the search engine agreement: no psm found.
     */
    public static final int NO_ID = 0;
    /**
     * Static index for the search engine agreement: the search engines have
     * different top ranking peptides.
     */
    public static final int CONFLICT = 1;
    /**
     * Static index for the search engine agreement: one or more of the search
     * engines did not identifie the spectrum, while one or more of the others
     * did.
     */
    public static final int PARTIALLY_MISSING = 2;
    /**
     * Static index for the search engine agreement: the search engines all have
     * the same top ranking peptide.
     */
    public static final int AGREEMENT = 3;
    /**
     * The peptide sequence tooltips for the OMSSA table.
     */
    private HashMap<Integer, String> omssaTablePeptideTooltips = null;
    /**
     * The peptide sequence tooltips for the XTandem table.
     */
    private HashMap<Integer, String> xTandemTablePeptideTooltips = null;
    /**
     * The peptide sequence tooltips for the Mascot table.
     */
    private HashMap<Integer, String> mascotTablePeptideTooltips = null;
    /**
     * The peptide sequence tooltips for the OMSSA table.
     */
    private String peptideShakerJTablePeptideTooltip = null;
    /**
     * The current spectrum key.
     */
    private String currentSpectrumKey = "";
    /**
     * The search engine table column header tooltips.
     */
    private ArrayList<String> searchEngineTableToolTips;
    /**
     * The spectrum table column header tooltips.
     */
    private ArrayList<String> spectrumTableToolTips;
    /**
     * The peptide shaker table column header tooltips.
     */
    private ArrayList<String> peptideShakerTableToolTips;
    /**
     * The OMSSA table column header tooltips.
     */
    private ArrayList<String> omssaTableToolTips;
    /**
     * The X!Tandem table column header tooltips.
     */
    private ArrayList<String> xTandemTableToolTips;
    /**
     * The Mascot table column header tooltips.
     */
    private ArrayList<String> mascotTableToolTips;
    /**
     * The spectrum annotator for search engine specific results
     */
    private SpectrumAnnotator specificAnnotator = new SpectrumAnnotator();
    /**
     * The list of OMSSA peptide keys.
     */
    private HashMap<Integer, String> omssaPeptideKeys = new HashMap<Integer, String>();
    /**
     * The list of X!Tandem peptide keys.
     */
    private HashMap<Integer, String> xtandemPeptideKeys = new HashMap<Integer, String>();
    /**
     * The list of Mascot peptide keys.
     */
    private HashMap<Integer, String> mascotPeptideKeys = new HashMap<Integer, String>();
    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The file currently selected
     */
    private String fileSelected = null;
    /**
     * The spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * Shows if OMSSA is used as part of the search.
     */
    private static boolean omssaUsed = false;
    /**
     * Shows if X!Tandem is used as part of the search.
     */
    private static boolean xtandemUsed = false;
    /**
     * Shows if Mascot is used as part of the search.
     */
    private static boolean mascotUsed = false;

    /**
     * Create a new SpectrumIdentificationPanel.
     *
     * @param peptideShakerGUI the PeptideShaker parent frame
     */
    public SpectrumIdentificationPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();
        formComponentResized(null);

        searchEnginetableJScrollPane.getViewport().setOpaque(false);
        spectrumTableJScrollPane.getViewport().setOpaque(false);
        peptideShakerJScrollPane.getViewport().setOpaque(false);
        xTandemTableJScrollPane.getViewport().setOpaque(false);
        mascotTableJScrollPane.getViewport().setOpaque(false);
        omssaTableJScrollPane.getViewport().setOpaque(false);

        fileNamesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        setTableProperties();
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {
        
        // correct the color for the upper right corner
        JPanel spectrumCorner = new JPanel();
        spectrumCorner.setBackground(spectrumTable.getTableHeader().getBackground());
        spectrumTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, spectrumCorner);
        JPanel omssaCorner = new JPanel();
        omssaCorner.setBackground(omssaTable.getTableHeader().getBackground());
        omssaTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, omssaCorner);
        JPanel xtandemCorner = new JPanel();
        xtandemCorner.setBackground(xTandemTable.getTableHeader().getBackground());
        xTandemTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, xtandemCorner);
        JPanel mascotCorner = new JPanel();
        mascotCorner.setBackground(omssaTable.getTableHeader().getBackground());
        mascotTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, mascotCorner);
        

        peptideShakerJTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        searchEngineTable.getTableHeader().setReorderingAllowed(false);
        peptideShakerJTable.getTableHeader().setReorderingAllowed(false);
        spectrumTable.getTableHeader().setReorderingAllowed(false);
        omssaTable.getTableHeader().setReorderingAllowed(false);
        mascotTable.getTableHeader().setReorderingAllowed(false);
        xTandemTable.getTableHeader().setReorderingAllowed(false);

        spectrumTable.setAutoCreateRowSorter(true);
        searchEngineTable.setAutoCreateRowSorter(true);

        // make sure that the user is made aware that the tool is doing something during sorting of the spectrum table
        spectrumTable.getRowSorter().addRowSorterListener(new RowSorterListener() {

            @Override
            public void sorterChanged(RowSorterEvent e) {

                if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    spectrumTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
                    
                } else if (e.getType() == RowSorterEvent.Type.SORTED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    spectrumTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        
                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                }
            }
        });

        peptideShakerJTable.getColumn(" ").setMinWidth(30);
        peptideShakerJTable.getColumn(" ").setMaxWidth(30);
        peptideShakerJTable.getColumn("  ").setMinWidth(30);
        peptideShakerJTable.getColumn("  ").setMaxWidth(30);
        searchEngineTable.getColumn(" ").setMinWidth(30);
        searchEngineTable.getColumn(" ").setMaxWidth(30);
        spectrumTable.getColumn(" ").setMinWidth(50);
        spectrumTable.getColumn(" ").setMaxWidth(50);

        omssaTable.getColumn(" ").setMinWidth(30);
        omssaTable.getColumn(" ").setMaxWidth(30);
        mascotTable.getColumn(" ").setMinWidth(30);
        mascotTable.getColumn(" ").setMaxWidth(30);
        xTandemTable.getColumn(" ").setMinWidth(30);
        xTandemTable.getColumn(" ").setMaxWidth(30);

        peptideShakerJTable.getColumn("SE").setMaxWidth(37);
        peptideShakerJTable.getColumn("SE").setMinWidth(37);
        spectrumTable.getColumn("SE").setMaxWidth(37);
        spectrumTable.getColumn("SE").setMinWidth(37);

        peptideShakerJTable.getColumn("Confidence").setMaxWidth(90);
        peptideShakerJTable.getColumn("Confidence").setMinWidth(90);
        peptideShakerJTable.getColumn("Score").setMaxWidth(90);
        peptideShakerJTable.getColumn("Score").setMinWidth(90);
        omssaTable.getColumn("Confidence").setMaxWidth(90);
        omssaTable.getColumn("Confidence").setMinWidth(90);
        mascotTable.getColumn("Confidence").setMaxWidth(90);
        mascotTable.getColumn("Confidence").setMinWidth(90);
        xTandemTable.getColumn("Confidence").setMaxWidth(90);
        xTandemTable.getColumn("Confidence").setMinWidth(90);

        // set up the psm color map
        HashMap<Integer, java.awt.Color> searchEngineColorMap = new HashMap<Integer, java.awt.Color>();
        searchEngineColorMap.put(AGREEMENT, peptideShakerGUI.getSparklineColor()); // search engines agree
        searchEngineColorMap.put(CONFLICT, java.awt.Color.YELLOW); // search engines don't agree
        searchEngineColorMap.put(PARTIALLY_MISSING, java.awt.Color.ORANGE); // some search engines id'ed some didn't

        // set up the psm tooltip map
        HashMap<Integer, String> searchEngineTooltipMap = new HashMap<Integer, String>();
        searchEngineTooltipMap.put(AGREEMENT, "Search Engines Agree");
        searchEngineTooltipMap.put(CONFLICT, "Search Engines Disagree");
        searchEngineTooltipMap.put(PARTIALLY_MISSING, "First Hit(s) Missing");

        peptideShakerJTable.getColumn("SE").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(java.awt.Color.lightGray, searchEngineColorMap, searchEngineTooltipMap));
        peptideShakerJTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        peptideShakerJTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        peptideShakerJTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());

        omssaTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        xTandemTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        mascotTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));

        omssaTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) omssaTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        xTandemTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) xTandemTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        mascotTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) mascotTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());

        omssaTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) omssaTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() - 30);
        xTandemTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) xTandemTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() - 30);
        mascotTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) mascotTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() - 30);

        searchEngineTable.getColumn("Validated PSMs").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("Unique PSMs").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("OMSSA").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("X!Tandem").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("Mascot").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("All").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        searchEngineTable.getColumn("Unassigned").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unassigned").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());

        // set up the psm color map
        HashMap<Integer, java.awt.Color> searchEngineSpectrumLevelColorMap = new HashMap<Integer, java.awt.Color>();
        searchEngineSpectrumLevelColorMap.put(AGREEMENT, peptideShakerGUI.getSparklineColor()); // search engines agree
        searchEngineSpectrumLevelColorMap.put(CONFLICT, java.awt.Color.YELLOW); // search engines don't agree
        searchEngineSpectrumLevelColorMap.put(PARTIALLY_MISSING, java.awt.Color.ORANGE); // some search engines id'ed some didn't
        searchEngineSpectrumLevelColorMap.put(NO_ID, java.awt.Color.lightGray); // no psm

        // set up the psm tooltip map
        HashMap<Integer, String> searchEngineSpectrumLevelTooltipMap = new HashMap<Integer, String>();
        searchEngineSpectrumLevelTooltipMap.put(AGREEMENT, "Search Engines Agree");
        searchEngineSpectrumLevelTooltipMap.put(CONFLICT, "Search Engines Disagree");
        searchEngineSpectrumLevelTooltipMap.put(PARTIALLY_MISSING, "Search Engine(s) Missing");
        searchEngineSpectrumLevelTooltipMap.put(NO_ID, "(No PSM)");

        spectrumTable.getColumn("SE").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(java.awt.Color.lightGray, searchEngineSpectrumLevelColorMap, searchEngineSpectrumLevelTooltipMap));
        spectrumTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 4d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d,
                1000d, 10d, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() - 30);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);

        // set up the table header tooltips
        searchEngineTableToolTips = new ArrayList<String>();
        searchEngineTableToolTips.add(null);
        searchEngineTableToolTips.add("Search Engine");
        searchEngineTableToolTips.add("Validated Peptide-Spectrum Matches");
        searchEngineTableToolTips.add("Unique Pepttide-Spectrum Matches");
        searchEngineTableToolTips.add("Overlapping Peptide-Spectrum Matches with OMSSA");
        searchEngineTableToolTips.add("Overlapping Peptide-Spectrum Matches with X!Tandem");
        searchEngineTableToolTips.add("Overlapping Peptide-Spectrum Matches with Mascot");
        searchEngineTableToolTips.add("Overlapping Peptide-Spectrum Matches All Search Engines");
        searchEngineTableToolTips.add("Unassigned Spectra");

        spectrumTableToolTips = new ArrayList<String>();
        spectrumTableToolTips.add(null);
        spectrumTableToolTips.add("Search Engine Agreement");
        spectrumTableToolTips.add("Spectrum Title");
        spectrumTableToolTips.add("Precursor m/z");
        spectrumTableToolTips.add("Precursor Charge");
        spectrumTableToolTips.add("Precursor Retention Time");

        peptideShakerTableToolTips = new ArrayList<String>();
        peptideShakerTableToolTips.add(null);
        peptideShakerTableToolTips.add("Search Engine Agreement");
        peptideShakerTableToolTips.add("Mapping Protein(s)");
        peptideShakerTableToolTips.add("Peptide Sequence");
        peptideShakerTableToolTips.add("Peptide Score");
        peptideShakerTableToolTips.add("Peptide Confidence");
        peptideShakerTableToolTips.add("Validated");

        omssaTableToolTips = new ArrayList<String>();
        omssaTableToolTips.add("Search Engine Peptide Rank");
        omssaTableToolTips.add("Mapping Protein(s)");
        omssaTableToolTips.add("Peptide Sequence");
        omssaTableToolTips.add("Precursor Charge");
        omssaTableToolTips.add("Peptide e-value");
        omssaTableToolTips.add("Peptide Confidence");

        xTandemTableToolTips = new ArrayList<String>();
        xTandemTableToolTips.add("Search Engine Peptide Rank");
        xTandemTableToolTips.add("Mapping Protein(s)");
        xTandemTableToolTips.add("Peptide Sequence");
        xTandemTableToolTips.add("Precursor Charge");
        xTandemTableToolTips.add("Peptide e-value");
        xTandemTableToolTips.add("Peptide Confidence");

        mascotTableToolTips = new ArrayList<String>();
        mascotTableToolTips.add("Search Engine Peptide Rank");
        mascotTableToolTips.add("Mapping Protein(s)");
        mascotTableToolTips.add("Peptide Sequence");
        mascotTableToolTips.add("Precursor Charge");
        mascotTableToolTips.add("Peptide e-value");
        mascotTableToolTips.add("Peptide Confidence");
    }

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unassigned").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) omssaTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) xTandemTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) mascotTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) omssaTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) xTandemTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) mascotTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);

        searchEngineTable.revalidate();
        searchEngineTable.repaint();

        spectrumTable.revalidate();
        spectrumTable.repaint();

        peptideShakerJTable.revalidate();
        peptideShakerJTable.repaint();

        omssaTable.revalidate();
        omssaTable.repaint();

        xTandemTable.revalidate();
        xTandemTable.repaint();

        mascotTable.revalidate();
        mascotTable.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        searchEnginesJPanel = new javax.swing.JPanel();
        searchEnginesJLayeredPane = new javax.swing.JLayeredPane();
        searchEnginesPanel = new javax.swing.JPanel();
        searchEnginetableJScrollPane = new javax.swing.JScrollPane();
        searchEngineTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) searchEngineTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        vennDiagramButton = new javax.swing.JButton();
        searchEnginesHelpJButton = new javax.swing.JButton();
        exportSearchEnginePerformanceJButton = new javax.swing.JButton();
        contextMenuSearchEnginesBackgroundPanel = new javax.swing.JPanel();
        psmsJPanel = new javax.swing.JPanel();
        psmsLayeredPane = new javax.swing.JLayeredPane();
        psmsPanel = new javax.swing.JPanel();
        peptideShakerJScrollPane = new javax.swing.JScrollPane();
        peptideShakerJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) peptideShakerTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        jLabel1 = new javax.swing.JLabel();
        omssaPanel = new javax.swing.JPanel();
        omssaTableJScrollPane = new javax.swing.JScrollPane();
        omssaTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) omssaTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        jLabel3 = new javax.swing.JLabel();
        xTandemPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        xTandemTableJScrollPane = new javax.swing.JScrollPane();
        xTandemTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) xTandemTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        mascotPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        mascotTableJScrollPane = new javax.swing.JScrollPane();
        mascotTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) mascotTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        psmsHelpJButton = new javax.swing.JButton();
        exportPsmsJButton = new javax.swing.JButton();
        contextMenuPsmsBackgroundPanel = new javax.swing.JPanel();
        spectrumJSplitPane = new javax.swing.JSplitPane();
        spectrumSelectionJPanel = new javax.swing.JPanel();
        spectrumSelectionLayeredPane = new javax.swing.JLayeredPane();
        spectrumSelectionPanel = new javax.swing.JPanel();
        fileNamesCmb = new javax.swing.JComboBox();
        spectrumTableJScrollPane = new javax.swing.JScrollPane();
        spectrumTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) spectrumTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        spectrumSelectionHelpJButton = new javax.swing.JButton();
        exportSpectrumSelectionJButton = new javax.swing.JButton();
        contextMenuSpectrumSelectionBackgroundPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumLayeredPane = new javax.swing.JLayeredPane();
        spectrumPanel = new javax.swing.JPanel();
        slidersSplitPane = new javax.swing.JSplitPane();
        slidersPanel = new javax.swing.JPanel();
        accuracySlider = new javax.swing.JSlider();
        intensitySlider = new javax.swing.JSlider();
        spectrumJPanel1 = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumChartPanel = new javax.swing.JPanel();
        spectrumHelpJButton = new javax.swing.JButton();
        exportSpectrumJButton = new javax.swing.JButton();
        contextMenuSpectrumBackgroundPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        searchEnginesJPanel.setOpaque(false);

        searchEnginesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Search Engine Performance"));
        searchEnginesPanel.setOpaque(false);

        searchEnginetableJScrollPane.setOpaque(false);

        searchEngineTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Search Engine", "Validated PSMs", "Unique PSMs", "OMSSA", "X!Tandem", "Mascot", "All", "Unassigned"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        searchEngineTable.setOpaque(false);
        searchEnginetableJScrollPane.setViewportView(searchEngineTable);

        vennDiagramButton.setBackground(new java.awt.Color(255, 255, 255));
        vennDiagramButton.setBorderPainted(false);
        vennDiagramButton.setContentAreaFilled(false);
        vennDiagramButton.setFocusable(false);

        javax.swing.GroupLayout searchEnginesPanelLayout = new javax.swing.GroupLayout(searchEnginesPanel);
        searchEnginesPanel.setLayout(searchEnginesPanelLayout);
        searchEnginesPanelLayout.setHorizontalGroup(
            searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1318, Short.MAX_VALUE)
            .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, searchEnginesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(searchEnginetableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1115, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(vennDiagramButton, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );
        searchEnginesPanelLayout.setVerticalGroup(
            searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 123, Short.MAX_VALUE)
            .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, searchEnginesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(searchEnginesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(searchEnginetableJScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                        .addComponent(vennDiagramButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE))
                    .addContainerGap()))
        );

        searchEnginesPanel.setBounds(0, 0, 1330, 150);
        searchEnginesJLayeredPane.add(searchEnginesPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        searchEnginesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        searchEnginesHelpJButton.setToolTipText("Help");
        searchEnginesHelpJButton.setBorder(null);
        searchEnginesHelpJButton.setBorderPainted(false);
        searchEnginesHelpJButton.setContentAreaFilled(false);
        searchEnginesHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        searchEnginesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                searchEnginesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchEnginesHelpJButtonMouseExited(evt);
            }
        });
        searchEnginesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchEnginesHelpJButtonActionPerformed(evt);
            }
        });
        searchEnginesHelpJButton.setBounds(1290, 0, 10, 19);
        searchEnginesJLayeredPane.add(searchEnginesHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportSearchEnginePerformanceJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSearchEnginePerformanceJButton.setToolTipText("Copy to File");
        exportSearchEnginePerformanceJButton.setBorder(null);
        exportSearchEnginePerformanceJButton.setBorderPainted(false);
        exportSearchEnginePerformanceJButton.setContentAreaFilled(false);
        exportSearchEnginePerformanceJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSearchEnginePerformanceJButton.setEnabled(false);
        exportSearchEnginePerformanceJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportSearchEnginePerformanceJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportSearchEnginePerformanceJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportSearchEnginePerformanceJButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportSearchEnginePerformanceJButtonMouseReleased(evt);
            }
        });
        exportSearchEnginePerformanceJButton.setBounds(1280, 0, 10, 19);
        searchEnginesJLayeredPane.add(exportSearchEnginePerformanceJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSearchEnginesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSearchEnginesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSearchEnginesBackgroundPanel);
        contextMenuSearchEnginesBackgroundPanel.setLayout(contextMenuSearchEnginesBackgroundPanelLayout);
        contextMenuSearchEnginesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSearchEnginesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuSearchEnginesBackgroundPanelLayout.setVerticalGroup(
            contextMenuSearchEnginesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        contextMenuSearchEnginesBackgroundPanel.setBounds(1280, 0, 30, 19);
        searchEnginesJLayeredPane.add(contextMenuSearchEnginesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout searchEnginesJPanelLayout = new javax.swing.GroupLayout(searchEnginesJPanel);
        searchEnginesJPanel.setLayout(searchEnginesJPanelLayout);
        searchEnginesJPanelLayout.setHorizontalGroup(
            searchEnginesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(searchEnginesJLayeredPane)
        );
        searchEnginesJPanelLayout.setVerticalGroup(
            searchEnginesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(searchEnginesJLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE)
        );

        psmsJPanel.setOpaque(false);

        psmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide-Spectrum Matches"));
        psmsPanel.setOpaque(false);

        peptideShakerJScrollPane.setOpaque(false);

        peptideShakerJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "SE", "Protein(s)", "Sequence", "Score", "Confidence", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        peptideShakerJTable.setFocusable(false);
        peptideShakerJTable.setOpaque(false);
        peptideShakerJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseReleased(evt);
            }
        });
        peptideShakerJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseMoved(evt);
            }
        });
        peptideShakerJScrollPane.setViewportView(peptideShakerJTable);

        jLabel1.setFont(jLabel1.getFont().deriveFont((jLabel1.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel1.setText("PeptideShaker");

        omssaPanel.setOpaque(false);

        omssaTableJScrollPane.setOpaque(false);

        omssaTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Sequence", "Charge", "e-value", "Confidence"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        omssaTable.setOpaque(false);
        omssaTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        omssaTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                omssaTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                omssaTableMouseReleased(evt);
            }
        });
        omssaTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                omssaTableMouseMoved(evt);
            }
        });
        omssaTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                omssaTableKeyReleased(evt);
            }
        });
        omssaTableJScrollPane.setViewportView(omssaTable);

        jLabel3.setFont(jLabel3.getFont().deriveFont((jLabel3.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel3.setText("OMSSA");

        javax.swing.GroupLayout omssaPanelLayout = new javax.swing.GroupLayout(omssaPanel);
        omssaPanel.setLayout(omssaPanelLayout);
        omssaPanelLayout.setHorizontalGroup(
            omssaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(omssaPanelLayout.createSequentialGroup()
                .addComponent(jLabel3)
                .addContainerGap(389, Short.MAX_VALUE))
            .addComponent(omssaTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
        );
        omssaPanelLayout.setVerticalGroup(
            omssaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(omssaPanelLayout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(omssaTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE))
        );

        xTandemPanel.setOpaque(false);

        jLabel4.setFont(jLabel4.getFont().deriveFont((jLabel4.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel4.setText("X!Tandem");

        xTandemTableJScrollPane.setOpaque(false);

        xTandemTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Sequence", "Charge", "e-value", "Confidence"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        xTandemTable.setOpaque(false);
        xTandemTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        xTandemTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                xTandemTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                xTandemTableMouseReleased(evt);
            }
        });
        xTandemTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                xTandemTableMouseMoved(evt);
            }
        });
        xTandemTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                xTandemTableKeyReleased(evt);
            }
        });
        xTandemTableJScrollPane.setViewportView(xTandemTable);

        javax.swing.GroupLayout xTandemPanelLayout = new javax.swing.GroupLayout(xTandemPanel);
        xTandemPanel.setLayout(xTandemPanelLayout);
        xTandemPanelLayout.setHorizontalGroup(
            xTandemPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(xTandemPanelLayout.createSequentialGroup()
                .addComponent(jLabel4)
                .addContainerGap(389, Short.MAX_VALUE))
            .addComponent(xTandemTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
        );
        xTandemPanelLayout.setVerticalGroup(
            xTandemPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(xTandemPanelLayout.createSequentialGroup()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(xTandemTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE))
        );

        mascotPanel.setOpaque(false);

        jLabel2.setFont(jLabel2.getFont().deriveFont((jLabel2.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel2.setText("Mascot");

        mascotTableJScrollPane.setMinimumSize(new java.awt.Dimension(23, 87));
        mascotTableJScrollPane.setOpaque(false);

        mascotTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Protein(s)", "Sequence", "Charge", "e-value", "Confidence"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        mascotTable.setOpaque(false);
        mascotTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        mascotTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                mascotTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                mascotTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                mascotTableMouseReleased(evt);
            }
        });
        mascotTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                mascotTableMouseMoved(evt);
            }
        });
        mascotTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                mascotTableKeyReleased(evt);
            }
        });
        mascotTableJScrollPane.setViewportView(mascotTable);

        javax.swing.GroupLayout mascotPanelLayout = new javax.swing.GroupLayout(mascotPanel);
        mascotPanel.setLayout(mascotPanelLayout);
        mascotPanelLayout.setHorizontalGroup(
            mascotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mascotPanelLayout.createSequentialGroup()
                .addComponent(jLabel2)
                .addContainerGap(391, Short.MAX_VALUE))
            .addComponent(mascotTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
        );
        mascotPanelLayout.setVerticalGroup(
            mascotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mascotPanelLayout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mascotTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout psmsPanelLayout = new javax.swing.GroupLayout(psmsPanel);
        psmsPanel.setLayout(psmsPanelLayout);
        psmsPanelLayout.setHorizontalGroup(
            psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1318, Short.MAX_VALUE)
            .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(psmsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(peptideShakerJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1298, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addGroup(psmsPanelLayout.createSequentialGroup()
                            .addComponent(omssaPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(xTandemPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(mascotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addContainerGap()))
        );
        psmsPanelLayout.setVerticalGroup(
            psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 273, Short.MAX_VALUE)
            .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(psmsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel1)
                    .addGap(9, 9, 9)
                    .addComponent(peptideShakerJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(xTandemPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(mascotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(omssaPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap()))
        );

        psmsPanel.setBounds(0, 0, 1330, 300);
        psmsLayeredPane.add(psmsPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        psmsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        psmsHelpJButton.setToolTipText("Help");
        psmsHelpJButton.setBorder(null);
        psmsHelpJButton.setBorderPainted(false);
        psmsHelpJButton.setContentAreaFilled(false);
        psmsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        psmsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                psmsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                psmsHelpJButtonMouseExited(evt);
            }
        });
        psmsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmsHelpJButtonActionPerformed(evt);
            }
        });
        psmsHelpJButton.setBounds(1290, 0, 10, 19);
        psmsLayeredPane.add(psmsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPsmsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPsmsJButton.setToolTipText("Copy to File");
        exportPsmsJButton.setBorder(null);
        exportPsmsJButton.setBorderPainted(false);
        exportPsmsJButton.setContentAreaFilled(false);
        exportPsmsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPsmsJButton.setEnabled(false);
        exportPsmsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPsmsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPsmsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPsmsJButtonMouseExited(evt);
            }
        });
        exportPsmsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPsmsJButtonActionPerformed(evt);
            }
        });
        exportPsmsJButton.setBounds(1280, 0, 10, 19);
        psmsLayeredPane.add(exportPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPsmsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPsmsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPsmsBackgroundPanel);
        contextMenuPsmsBackgroundPanel.setLayout(contextMenuPsmsBackgroundPanelLayout);
        contextMenuPsmsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuPsmsBackgroundPanelLayout.setVerticalGroup(
            contextMenuPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        contextMenuPsmsBackgroundPanel.setBounds(1280, 0, 30, 19);
        psmsLayeredPane.add(contextMenuPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout psmsJPanelLayout = new javax.swing.GroupLayout(psmsJPanel);
        psmsJPanel.setLayout(psmsJPanelLayout);
        psmsJPanelLayout.setHorizontalGroup(
            psmsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmsLayeredPane)
        );
        psmsJPanelLayout.setVerticalGroup(
            psmsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
        );

        spectrumJSplitPane.setBorder(null);
        spectrumJSplitPane.setDividerLocation(700);
        spectrumJSplitPane.setDividerSize(0);
        spectrumJSplitPane.setResizeWeight(0.5);
        spectrumJSplitPane.setOpaque(false);

        spectrumSelectionJPanel.setOpaque(false);

        spectrumSelectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Selection"));
        spectrumSelectionPanel.setOpaque(false);

        fileNamesCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileNamesCmbActionPerformed(evt);
            }
        });

        spectrumTableJScrollPane.setOpaque(false);

        spectrumTable.setModel(new SpectrumTable());
        spectrumTable.setOpaque(false);
        spectrumTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        spectrumTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumTableMouseReleased(evt);
            }
        });
        spectrumTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                spectrumTableKeyReleased(evt);
            }
        });
        spectrumTableJScrollPane.setViewportView(spectrumTable);

        javax.swing.GroupLayout spectrumSelectionPanelLayout = new javax.swing.GroupLayout(spectrumSelectionPanel);
        spectrumSelectionPanel.setLayout(spectrumSelectionPanelLayout);
        spectrumSelectionPanelLayout.setHorizontalGroup(
            spectrumSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 678, Short.MAX_VALUE)
            .addGroup(spectrumSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(spectrumSelectionPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(spectrumSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 658, Short.MAX_VALUE)
                        .addComponent(fileNamesCmb, 0, 658, Short.MAX_VALUE))
                    .addContainerGap()))
        );
        spectrumSelectionPanelLayout.setVerticalGroup(
            spectrumSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 323, Short.MAX_VALUE)
            .addGroup(spectrumSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumSelectionPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(fileNamesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        spectrumSelectionPanel.setBounds(0, 0, 690, 350);
        spectrumSelectionLayeredPane.add(spectrumSelectionPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        spectrumSelectionHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        spectrumSelectionHelpJButton.setToolTipText("Help");
        spectrumSelectionHelpJButton.setBorder(null);
        spectrumSelectionHelpJButton.setBorderPainted(false);
        spectrumSelectionHelpJButton.setContentAreaFilled(false);
        spectrumSelectionHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        spectrumSelectionHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                spectrumSelectionHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumSelectionHelpJButtonMouseExited(evt);
            }
        });
        spectrumSelectionHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumSelectionHelpJButtonActionPerformed(evt);
            }
        });
        spectrumSelectionHelpJButton.setBounds(660, 0, 10, 19);
        spectrumSelectionLayeredPane.add(spectrumSelectionHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportSpectrumSelectionJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumSelectionJButton.setToolTipText("Copy to File");
        exportSpectrumSelectionJButton.setBorder(null);
        exportSpectrumSelectionJButton.setBorderPainted(false);
        exportSpectrumSelectionJButton.setContentAreaFilled(false);
        exportSpectrumSelectionJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumSelectionJButton.setEnabled(false);
        exportSpectrumSelectionJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportSpectrumSelectionJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportSpectrumSelectionJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportSpectrumSelectionJButtonMouseExited(evt);
            }
        });
        exportSpectrumSelectionJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSpectrumSelectionJButtonActionPerformed(evt);
            }
        });
        exportSpectrumSelectionJButton.setBounds(650, 0, 10, 19);
        spectrumSelectionLayeredPane.add(exportSpectrumSelectionJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSpectrumSelectionBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSpectrumSelectionBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSpectrumSelectionBackgroundPanel);
        contextMenuSpectrumSelectionBackgroundPanel.setLayout(contextMenuSpectrumSelectionBackgroundPanelLayout);
        contextMenuSpectrumSelectionBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSpectrumSelectionBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuSpectrumSelectionBackgroundPanelLayout.setVerticalGroup(
            contextMenuSpectrumSelectionBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        contextMenuSpectrumSelectionBackgroundPanel.setBounds(640, 0, 30, 19);
        spectrumSelectionLayeredPane.add(contextMenuSpectrumSelectionBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout spectrumSelectionJPanelLayout = new javax.swing.GroupLayout(spectrumSelectionJPanel);
        spectrumSelectionJPanel.setLayout(spectrumSelectionJPanelLayout);
        spectrumSelectionJPanelLayout.setHorizontalGroup(
            spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumSelectionJPanelLayout.createSequentialGroup()
                .addComponent(spectrumSelectionLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 690, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumSelectionJPanelLayout.setVerticalGroup(
            spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumSelectionLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE)
        );

        spectrumJSplitPane.setLeftComponent(spectrumSelectionJPanel);

        spectrumJPanel.setOpaque(false);
        spectrumJPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                spectrumJPanelMouseWheelMoved(evt);
            }
        });

        spectrumPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum"));
        spectrumPanel.setOpaque(false);

        slidersSplitPane.setBorder(null);
        slidersSplitPane.setDividerLocation(550);
        slidersSplitPane.setDividerSize(0);
        slidersSplitPane.setOpaque(false);

        slidersPanel.setOpaque(false);

        accuracySlider.setOrientation(javax.swing.JSlider.VERTICAL);
        accuracySlider.setPaintTicks(true);
        accuracySlider.setToolTipText("Annotation Accuracy");
        accuracySlider.setValue(100);
        accuracySlider.setOpaque(false);
        accuracySlider.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                accuracySliderMouseWheelMoved(evt);
            }
        });
        accuracySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                accuracySliderStateChanged(evt);
            }
        });

        intensitySlider.setOrientation(javax.swing.JSlider.VERTICAL);
        intensitySlider.setPaintTicks(true);
        intensitySlider.setToolTipText("Annotation Intensity Level");
        intensitySlider.setValue(75);
        intensitySlider.setOpaque(false);
        intensitySlider.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                intensitySliderMouseWheelMoved(evt);
            }
        });
        intensitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                intensitySliderStateChanged(evt);
            }
        });

        javax.swing.GroupLayout slidersPanelLayout = new javax.swing.GroupLayout(slidersPanel);
        slidersPanel.setLayout(slidersPanelLayout);
        slidersPanelLayout.setHorizontalGroup(
            slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, slidersPanelLayout.createSequentialGroup()
                .addContainerGap(547, Short.MAX_VALUE)
                .addGroup(slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(intensitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(accuracySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );
        slidersPanelLayout.setVerticalGroup(
            slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, slidersPanelLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                .addGap(30, 30, 30)
                .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                .addContainerGap())
        );

        slidersSplitPane.setRightComponent(slidersPanel);

        spectrumJPanel1.setBackground(new java.awt.Color(255, 255, 255));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(spectrumAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumJToolBar.add(spectrumAnnotationMenuPanel);

        spectrumChartPanel.setOpaque(false);
        spectrumChartPanel.setLayout(new javax.swing.BoxLayout(spectrumChartPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.GroupLayout spectrumJPanel1Layout = new javax.swing.GroupLayout(spectrumJPanel1);
        spectrumJPanel1.setLayout(spectrumJPanel1Layout);
        spectrumJPanel1Layout.setHorizontalGroup(
            spectrumJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumJPanel1Layout.setVerticalGroup(
            spectrumJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanel1Layout.createSequentialGroup()
                .addComponent(spectrumChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 312, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        slidersSplitPane.setLeftComponent(spectrumJPanel1);

        javax.swing.GroupLayout spectrumPanelLayout = new javax.swing.GroupLayout(spectrumPanel);
        spectrumPanel.setLayout(spectrumPanelLayout);
        spectrumPanelLayout.setHorizontalGroup(
            spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slidersSplitPane)
                .addContainerGap())
        );
        spectrumPanelLayout.setVerticalGroup(
            spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumPanelLayout.createSequentialGroup()
                .addComponent(slidersSplitPane)
                .addContainerGap())
        );

        spectrumPanel.setBounds(0, 0, 630, 350);
        spectrumLayeredPane.add(spectrumPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        spectrumHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        spectrumHelpJButton.setToolTipText("Help");
        spectrumHelpJButton.setBorder(null);
        spectrumHelpJButton.setBorderPainted(false);
        spectrumHelpJButton.setContentAreaFilled(false);
        spectrumHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        spectrumHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                spectrumHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumHelpJButtonMouseExited(evt);
            }
        });
        spectrumHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spectrumHelpJButtonActionPerformed(evt);
            }
        });
        spectrumHelpJButton.setBounds(610, 0, 10, 19);
        spectrumLayeredPane.add(spectrumHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportSpectrumJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumJButton.setToolTipText("Export");
        exportSpectrumJButton.setBorder(null);
        exportSpectrumJButton.setBorderPainted(false);
        exportSpectrumJButton.setContentAreaFilled(false);
        exportSpectrumJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumJButton.setEnabled(false);
        exportSpectrumJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportSpectrumJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseReleased(evt);
            }
        });
        exportSpectrumJButton.setBounds(600, 0, 10, 19);
        spectrumLayeredPane.add(exportSpectrumJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSpectrumBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSpectrumBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSpectrumBackgroundPanel);
        contextMenuSpectrumBackgroundPanel.setLayout(contextMenuSpectrumBackgroundPanelLayout);
        contextMenuSpectrumBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSpectrumBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuSpectrumBackgroundPanelLayout.setVerticalGroup(
            contextMenuSpectrumBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        contextMenuSpectrumBackgroundPanel.setBounds(590, 0, 30, 19);
        spectrumLayeredPane.add(contextMenuSpectrumBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 631, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE)
        );

        spectrumJSplitPane.setRightComponent(spectrumJPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(searchEnginesJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(psmsJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(spectrumJSplitPane, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchEnginesJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumJSplitPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmsJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Updates the spectrum table based on the currently selected mgf file.
     *
     * @param evt
     */
    private void fileNamesCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileNamesCmbActionPerformed
        if (updateSelection) {
            clearItemSelection();
            fileSelectionChanged();
        }
    }//GEN-LAST:event_fileNamesCmbActionPerformed

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void spectrumTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseReleased
        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableMouseReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void spectrumTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectrumTableKeyReleased
        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableKeyReleased

    /**
     * Reset the divider between the spectrum table and the spectrum.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        spectrumJSplitPane.setDividerLocation(spectrumJSplitPane.getWidth() / 2);

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                spectrumJSplitPane.setDividerLocation(spectrumJSplitPane.getWidth() / 2);


                // move the icons
                psmsLayeredPane.getComponent(0).setBounds(
                        psmsLayeredPane.getWidth() - psmsLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        psmsLayeredPane.getComponent(0).getWidth(),
                        psmsLayeredPane.getComponent(0).getHeight());

                psmsLayeredPane.getComponent(1).setBounds(
                        psmsLayeredPane.getWidth() - psmsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        psmsLayeredPane.getComponent(1).getWidth(),
                        psmsLayeredPane.getComponent(1).getHeight());

                psmsLayeredPane.getComponent(2).setBounds(
                        psmsLayeredPane.getWidth() - psmsLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        psmsLayeredPane.getComponent(2).getWidth(),
                        psmsLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                psmsLayeredPane.getComponent(3).setBounds(0, 0, psmsLayeredPane.getWidth(), psmsLayeredPane.getHeight());
                psmsLayeredPane.revalidate();
                psmsLayeredPane.repaint();


                // move the icons
                searchEnginesJLayeredPane.getComponent(0).setBounds(
                        searchEnginesJLayeredPane.getWidth() - searchEnginesJLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        searchEnginesJLayeredPane.getComponent(0).getWidth(),
                        searchEnginesJLayeredPane.getComponent(0).getHeight());

                searchEnginesJLayeredPane.getComponent(1).setBounds(
                        searchEnginesJLayeredPane.getWidth() - searchEnginesJLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        searchEnginesJLayeredPane.getComponent(1).getWidth(),
                        searchEnginesJLayeredPane.getComponent(1).getHeight());

                searchEnginesJLayeredPane.getComponent(2).setBounds(
                        searchEnginesJLayeredPane.getWidth() - searchEnginesJLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        searchEnginesJLayeredPane.getComponent(2).getWidth(),
                        searchEnginesJLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                searchEnginesJLayeredPane.getComponent(3).setBounds(0, 0, searchEnginesJLayeredPane.getWidth(), searchEnginesJLayeredPane.getHeight());
                searchEnginesJLayeredPane.revalidate();
                searchEnginesJLayeredPane.repaint();


                // move the icons
                spectrumSelectionLayeredPane.getComponent(0).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(0).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(0).getHeight());

                spectrumSelectionLayeredPane.getComponent(1).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(1).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(1).getHeight());

                spectrumSelectionLayeredPane.getComponent(2).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(2).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                spectrumSelectionLayeredPane.getComponent(3).setBounds(0, 0, spectrumSelectionLayeredPane.getWidth(), spectrumSelectionLayeredPane.getHeight());
                spectrumSelectionLayeredPane.revalidate();
                spectrumSelectionLayeredPane.repaint();


                // move the icons
                spectrumLayeredPane.getComponent(0).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        spectrumLayeredPane.getComponent(0).getWidth(),
                        spectrumLayeredPane.getComponent(0).getHeight());

                spectrumLayeredPane.getComponent(1).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        spectrumLayeredPane.getComponent(1).getWidth(),
                        spectrumLayeredPane.getComponent(1).getHeight());

                spectrumLayeredPane.getComponent(2).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        spectrumLayeredPane.getComponent(2).getWidth(),
                        spectrumLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                spectrumLayeredPane.getComponent(3).setBounds(0, 0, spectrumLayeredPane.getWidth(), spectrumLayeredPane.getHeight());
                spectrumLayeredPane.revalidate();
                spectrumLayeredPane.repaint();

                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        // set the sliders split pane divider location
                        if (peptideShakerGUI.getUserPreferences().showSliders()) {
                            slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth() - 30);
                        } else {
                            slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth());
                        }
                    }
                });
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Update the OMSSA psm selection.
     *
     * @param evt
     */
    private void omssaTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_omssaTableKeyReleased
        omssaTableMouseReleased(null);
    }//GEN-LAST:event_omssaTableKeyReleased

    /**
     * Update the X!Tandem psm selection.
     *
     * @param evt
     */
    private void xTandemTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_xTandemTableKeyReleased
        xTandemTableMouseReleased(null);
    }//GEN-LAST:event_xTandemTableKeyReleased

    /**
     * Update the Mascot psm selection.
     *
     * @param evt
     */
    private void mascotTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_mascotTableKeyReleased
        mascotTableMouseClicked(null);
    }//GEN-LAST:event_mascotTableKeyReleased

    private void mascotTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mascotTableMouseClicked
        if (mascotTable.getSelectedRow() != -1) {

            if (xTandemTable.getSelectedRow() != -1) {
                xTandemTable.removeRowSelectionInterval(xTandemTable.getSelectedRow(), xTandemTable.getSelectedRow());
            }

            if (omssaTable.getSelectedRow() != -1) {
                omssaTable.removeRowSelectionInterval(omssaTable.getSelectedRow(), omssaTable.getSelectedRow());
            }

            updateSpectrum();
        }
    }//GEN-LAST:event_mascotTableMouseClicked

    /**
     * Update the OMSSA psm selection.
     *
     * @param evt
     */
    private void omssaTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_omssaTableMouseReleased
        if (omssaTable.getSelectedRow() != -1) {

            if (xTandemTable.getSelectedRow() != -1) {
                xTandemTable.removeRowSelectionInterval(xTandemTable.getSelectedRow(), xTandemTable.getSelectedRow());
            }

            if (mascotTable.getSelectedRow() != -1) {
                mascotTable.removeRowSelectionInterval(mascotTable.getSelectedRow(), mascotTable.getSelectedRow());
            }

            updateSpectrum();

            // open protein links in web browser
            int row = omssaTable.rowAtPoint(evt.getPoint());
            int column = omssaTable.columnAtPoint(evt.getPoint());

            if (column == 1) {

                // open protein links in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) omssaTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) omssaTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_omssaTableMouseReleased

    /**
     * Update the X!Tandem psm selection.
     *
     * @param evt
     */
    private void xTandemTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_xTandemTableMouseReleased
        if (xTandemTable.getSelectedRow() != -1) {

            if (mascotTable.getSelectedRow() != -1) {
                mascotTable.removeRowSelectionInterval(mascotTable.getSelectedRow(), mascotTable.getSelectedRow());
            }

            if (omssaTable.getSelectedRow() != -1) {
                omssaTable.removeRowSelectionInterval(omssaTable.getSelectedRow(), omssaTable.getSelectedRow());
            }

            updateSpectrum();

            // open protein links in web browser
            int row = xTandemTable.rowAtPoint(evt.getPoint());
            int column = xTandemTable.columnAtPoint(evt.getPoint());

            if (column == 1) {

                // open protein links in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) xTandemTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) xTandemTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_xTandemTableMouseReleased

    /**
     * Update the Mascot psm selection.
     *
     * @param evt
     */
    private void mascotTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mascotTableMouseReleased
        if (mascotTable.getSelectedRow() != -1) {

            if (xTandemTable.getSelectedRow() != -1) {
                xTandemTable.removeRowSelectionInterval(xTandemTable.getSelectedRow(), xTandemTable.getSelectedRow());
            }

            if (omssaTable.getSelectedRow() != -1) {
                omssaTable.removeRowSelectionInterval(omssaTable.getSelectedRow(), omssaTable.getSelectedRow());
            }

            updateSpectrum();

            // open protein links in web browser
            int row = mascotTable.rowAtPoint(evt.getPoint());
            int column = mascotTable.columnAtPoint(evt.getPoint());

            if (column == 1) {

                // open protein links in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) mascotTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) mascotTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_mascotTableMouseReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void peptideShakerJTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerJTableMouseMoved
        int row = peptideShakerJTable.rowAtPoint(evt.getPoint());
        int column = peptideShakerJTable.columnAtPoint(evt.getPoint());

        if (peptideShakerJTable.getValueAt(row, column) != null) {
            if (column == peptideShakerJTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) peptideShakerJTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                peptideShakerJTable.setToolTipText(null);

            } else if (column == peptideShakerJTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // check if we ought to show a tooltip with mod details
                peptideShakerJTable.setToolTipText(peptideShakerJTablePeptideTooltip);
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptideShakerJTable.setToolTipText(null);
            }
        }
    }//GEN-LAST:event_peptideShakerJTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptideShakerJTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerJTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideShakerJTableMouseExited

    /**
     * Opens the protein web links if the protein(s) column is selcted.
     *
     * @param evt
     */
    private void peptideShakerJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerJTableMouseReleased

        int row = peptideShakerJTable.rowAtPoint(evt.getPoint());
        int column = peptideShakerJTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == 1) {

                // open protein links in web browser
                if (evt.getButton() == MouseEvent.BUTTON1
                        && ((String) peptideShakerJTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                    peptideShakerGUI.openProteinLinks((String) peptideShakerJTable.getValueAt(row, column));
                }
            }
        }
    }//GEN-LAST:event_peptideShakerJTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void omssaTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_omssaTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_omssaTableMouseExited

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void xTandemTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_xTandemTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_xTandemTableMouseExited

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void mascotTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mascotTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_mascotTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void omssaTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_omssaTableMouseMoved
        int row = omssaTable.rowAtPoint(evt.getPoint());
        int column = omssaTable.columnAtPoint(evt.getPoint());

        if (omssaTable.getValueAt(row, column) != null) {

            if (column == omssaTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) omssaTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                omssaTable.setToolTipText(null);

            } else if (column == omssaTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                omssaTable.setToolTipText(omssaTablePeptideTooltips.get((Integer) omssaTable.getValueAt(row, 0)));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                omssaTable.setToolTipText(null);
            }
        } else {
            omssaTable.setToolTipText(null);
        }
    }//GEN-LAST:event_omssaTableMouseMoved

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void xTandemTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_xTandemTableMouseMoved
        int row = xTandemTable.rowAtPoint(evt.getPoint());
        int column = xTandemTable.columnAtPoint(evt.getPoint());

        if (xTandemTable.getValueAt(row, column) != null) {

            if (column == xTandemTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) xTandemTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                xTandemTable.setToolTipText(null);

            } else if (column == xTandemTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                xTandemTable.setToolTipText(xTandemTablePeptideTooltips.get((Integer) xTandemTable.getValueAt(row, 0)));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                xTandemTable.setToolTipText(null);
            }
        } else {
            xTandemTable.setToolTipText(null);
        }
    }//GEN-LAST:event_xTandemTableMouseMoved

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void mascotTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mascotTableMouseMoved
        int row = mascotTable.rowAtPoint(evt.getPoint());
        int column = mascotTable.columnAtPoint(evt.getPoint());

        if (mascotTable.getValueAt(row, column) != null) {

            if (column == mascotTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) mascotTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                mascotTable.setToolTipText(null);

            } else if (column == mascotTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                mascotTable.setToolTipText(mascotTablePeptideTooltips.get((Integer) mascotTable.getValueAt(row, 0)));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                mascotTable.setToolTipText(null);
            }
        } else {
            mascotTable.setToolTipText(null);
        }
    }//GEN-LAST:event_mascotTableMouseMoved

    /**
     * Updates the slider value when the user scrolls.
     *
     * @param evt
     */
    private void intensitySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_intensitySliderMouseWheelMoved
        spectrumJPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_intensitySliderMouseWheelMoved

    /**
     * Updates the intensity annotation limit.
     *
     * @param evt
     */
    private void intensitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_intensitySliderStateChanged
        peptideShakerGUI.getAnnotationPreferences().setAnnotationLevel(((Integer) intensitySlider.getValue()) / 100.0);
        peptideShakerGUI.updateSpectrumAnnotations();
        peptideShakerGUI.setDataSaved(false);
        intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");
        updateSpectrumSliderToolTip();
    }//GEN-LAST:event_intensitySliderStateChanged

    /**
     * Updates the slider values when the user scrolls.
     *
     * @param evt
     */
    private void spectrumJPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_spectrumJPanelMouseWheelMoved

        // @TODO: figure out why the strange special cases are needed... 
        //          if not included the slider gets stuck at given values depending on the current max value

        if (evt.isControlDown()) {
            if (evt.getWheelRotation() > 0) { // Down
                accuracySlider.setValue(accuracySlider.getValue() - 1);
            } else { // Up
                int oldValue = accuracySlider.getValue();
                int newValue = accuracySlider.getValue() + 1;
                accuracySlider.setValue(newValue);

                while (oldValue == accuracySlider.getValue()) {
                    accuracySlider.setValue(newValue++);
                }
            }
        } else {
            if (evt.getWheelRotation() > 0) { // Down
                intensitySlider.setValue(intensitySlider.getValue() - 1);
            } else { // Up
                int oldValue = intensitySlider.getValue();
                int newValue = intensitySlider.getValue() + 1;
                intensitySlider.setValue(newValue);

                while (oldValue == intensitySlider.getValue()) {
                    intensitySlider.setValue(newValue++);
                }
            }
        }

        updateSpectrumSliderToolTip();
    }//GEN-LAST:event_spectrumJPanelMouseWheelMoved

    /**
     * Updates the slider value when the user scrolls.
     *
     * @param evt
     */
    private void accuracySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_accuracySliderMouseWheelMoved
        spectrumJPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_accuracySliderMouseWheelMoved

    /**
     * Update the fragment ion annotation accuracy.
     *
     * @param evt
     */
    private void accuracySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_accuracySliderStateChanged
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();
        peptideShakerGUI.getAnnotationPreferences().setFragmentIonAccuracy(accuracy);
        peptideShakerGUI.updateSpectrumAnnotations();
        peptideShakerGUI.setDataSaved(false);
        accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " Da");
        updateSpectrumSliderToolTip();
    }//GEN-LAST:event_accuracySliderStateChanged

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void psmsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_psmsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void psmsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_psmsHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void psmsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#PSM");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_psmsHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportPsmsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPsmsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPsmsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportPsmsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPsmsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPsmsJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportPsmsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPsmsJButtonActionPerformed
        copyTableContentToClipboardOrFile(TableIndex.PSM_TABLES);
    }//GEN-LAST:event_exportPsmsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void searchEnginesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchEnginesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_searchEnginesHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void searchEnginesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchEnginesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_searchEnginesHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void searchEnginesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchEnginesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#SearchEnginePerformance");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_searchEnginesHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportSearchEnginePerformanceJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSearchEnginePerformanceJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportSearchEnginePerformanceJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportSearchEnginePerformanceJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSearchEnginePerformanceJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportSearchEnginePerformanceJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void spectrumSelectionHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_spectrumSelectionHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumSelectionHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumSelectionHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void spectrumSelectionHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumSelectionHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#SpectrumSelection");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumSelectionHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportSpectrumSelectionJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumSelectionJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportSpectrumSelectionJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportSpectrumSelectionJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumSelectionJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportSpectrumSelectionJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportSpectrumSelectionJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSpectrumSelectionJButtonActionPerformed
        copyTableContentToClipboardOrFile(TableIndex.SPECTRUM_FILES);
    }//GEN-LAST:event_exportSpectrumSelectionJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void spectrumHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_spectrumHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void spectrumHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spectrumHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#Spectrum");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportSpectrumJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportSpectrumJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportSpectrumJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportSpectrumJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportSearchEnginePerformanceJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSearchEnginePerformanceJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Table to Clipboard");
        menuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyTableContentToClipboardOrFile(TableIndex.SEARCH_ENGINE_PERFORMANCE);
            }
        });

        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Venn Diagram");
        menuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new ExportGraphicsDialog(peptideShakerGUI, true, vennDiagramButton);
            }
        });

        popupMenu.add(menuItem);

        popupMenu.show(exportSearchEnginePerformanceJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportSearchEnginePerformanceJButtonMouseReleased

    /**
     * Export the spectrum.
     *
     * @param evt
     */
    private void exportSpectrumJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Spectrum As Figure");
        menuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideShakerGUI.exportSpectrumAsFigure();
            }
        });

        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Spectrum As MGF");
        menuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideShakerGUI.exportSpectrumAsMgf();
            }
        });

        popupMenu.add(menuItem);

        popupMenu.show(exportSpectrumJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportSpectrumJButtonMouseReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider accuracySlider;
    private javax.swing.JPanel contextMenuPsmsBackgroundPanel;
    private javax.swing.JPanel contextMenuSearchEnginesBackgroundPanel;
    private javax.swing.JPanel contextMenuSpectrumBackgroundPanel;
    private javax.swing.JPanel contextMenuSpectrumSelectionBackgroundPanel;
    private javax.swing.JButton exportPsmsJButton;
    private javax.swing.JButton exportSearchEnginePerformanceJButton;
    private javax.swing.JButton exportSpectrumJButton;
    private javax.swing.JButton exportSpectrumSelectionJButton;
    private javax.swing.JComboBox fileNamesCmb;
    private javax.swing.JSlider intensitySlider;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel mascotPanel;
    private javax.swing.JTable mascotTable;
    private javax.swing.JScrollPane mascotTableJScrollPane;
    private javax.swing.JPanel omssaPanel;
    private javax.swing.JTable omssaTable;
    private javax.swing.JScrollPane omssaTableJScrollPane;
    private javax.swing.JScrollPane peptideShakerJScrollPane;
    private javax.swing.JTable peptideShakerJTable;
    private javax.swing.JButton psmsHelpJButton;
    private javax.swing.JPanel psmsJPanel;
    private javax.swing.JLayeredPane psmsLayeredPane;
    private javax.swing.JPanel psmsPanel;
    private javax.swing.JTable searchEngineTable;
    private javax.swing.JButton searchEnginesHelpJButton;
    private javax.swing.JLayeredPane searchEnginesJLayeredPane;
    private javax.swing.JPanel searchEnginesJPanel;
    private javax.swing.JPanel searchEnginesPanel;
    private javax.swing.JScrollPane searchEnginetableJScrollPane;
    private javax.swing.JPanel slidersPanel;
    private javax.swing.JSplitPane slidersSplitPane;
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
    private javax.swing.JPanel spectrumChartPanel;
    private javax.swing.JButton spectrumHelpJButton;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JPanel spectrumJPanel1;
    private javax.swing.JSplitPane spectrumJSplitPane;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JLayeredPane spectrumLayeredPane;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JButton spectrumSelectionHelpJButton;
    private javax.swing.JPanel spectrumSelectionJPanel;
    private javax.swing.JLayeredPane spectrumSelectionLayeredPane;
    private javax.swing.JPanel spectrumSelectionPanel;
    private javax.swing.JTable spectrumTable;
    private javax.swing.JScrollPane spectrumTableJScrollPane;
    private javax.swing.JButton vennDiagramButton;
    private javax.swing.JPanel xTandemPanel;
    private javax.swing.JTable xTandemTable;
    private javax.swing.JScrollPane xTandemTableJScrollPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays the results in the panel.
     */
    public void displayResults() {

        progressDialog = new ProgressDialogX(peptideShakerGUI, true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setIndeterminate(true);
                progressDialog.setTitle("Loading Data. Please Wait...");
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {

            @Override
            public void run() {

                // change the peptide shaker icon to a "waiting version"
                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                try {
                    identification = peptideShakerGUI.getIdentification();

                    // now we have data and can update the jsparklines depending on this
                    spectrumTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                            new Integer(((PSMaps) identification.getUrParam(new PSMaps())).getPsmSpecificMap().getMaxCharge()).doubleValue(), peptideShakerGUI.getSparklineColor()));
                    spectrumTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, SpectrumFactory.getInstance().getMinRT(),
                            SpectrumFactory.getInstance().getMaxRT(), SpectrumFactory.getInstance().getMaxRT() / 50, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() - 30);
                    ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
                    ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);

                    int m = 0, o = 0, x = 0, mo = 0, mx = 0, ox = 0, omx = 0, no_m = 0, no_x = 0, no_o = 0;
                    boolean mascot, omssa, xTandem;
                    PSParameter probabilities = new PSParameter();
                    SpectrumMatch spectrumMatch;

                    progressDialog.setIndeterminate(false);
                    progressDialog.setMaxProgressValue(identification.getSpectrumIdentification().size());
                    progressDialog.setValue(0);


                    // @TODO: this should be moved to when the files are loaded and done only once...?

                    // get the list of search engines used
                    IdfileReaderFactory idFileReaderFactory = IdfileReaderFactory.getInstance();
                    ArrayList<File> idFiles = peptideShakerGUI.getProjectDetails().getIdentificationFiles();

                    omssaUsed = false;
                    xtandemUsed = false;
                    mascotUsed = false;

                    for (int i = 0; i < idFiles.size(); i++) {
                        if (idFileReaderFactory.getSearchEngine(idFiles.get(i)) == SearchEngine.OMSSA) {
                            omssaUsed = true;
                        } else if (idFileReaderFactory.getSearchEngine(idFiles.get(i)) == SearchEngine.XTANDEM) {
                            xtandemUsed = true;
                        } else if (idFileReaderFactory.getSearchEngine(idFiles.get(i)) == SearchEngine.MASCOT) {
                            mascotUsed = true;
                        }
                    }

                    // @TODO: hide the unused search engine columns in the Search Engine Performance table
                    // @TODO: hide the columns in the table for the search engines that are not used...
                    // @TODO: calculate the 'All' column values based on only the used search engines and not all three like now...
                    // @TODO: hide the unused search engine tables at the bottom of the screen? or rather use tabs instead?


                    for (String spectrumKey : identification.getSpectrumIdentification()) {

                        if (progressDialog.isRunCanceled()) {
                            break;
                        }

                        spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        mascot = false;
                        omssa = false;
                        xTandem = false;
                        probabilities = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, probabilities);

                        if (probabilities.isValidated()) {
                            if (spectrumMatch.getFirstHit(Advocate.MASCOT) != null) {
                                if (spectrumMatch.getFirstHit(Advocate.MASCOT).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                                    mascot = true;
                                }
                            }
                            if (spectrumMatch.getFirstHit(Advocate.OMSSA) != null) {
                                if (spectrumMatch.getFirstHit(Advocate.OMSSA).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                                    omssa = true;
                                }
                            }
                            if (spectrumMatch.getFirstHit(Advocate.XTANDEM) != null) {
                                if (spectrumMatch.getFirstHit(Advocate.XTANDEM).getPeptide().isSameAs(spectrumMatch.getBestAssumption().getPeptide())) {
                                    xTandem = true;
                                }
                            }
                        }

                        if (mascot && omssa && xTandem) {
                            omx++;
                        }
                        if (mascot && omssa) {
                            mo++;
                        }
                        if (omssa && xTandem) {
                            ox++;
                        }
                        if (mascot && xTandem) {
                            mx++;
                        }
                        if (mascot) {
                            m++;
                        }
                        if (omssa) {
                            o++;
                        }
                        if (xTandem) {
                            x++;
                        }

                        if (!mascot) {
                            no_m++;
                        }
                        if (!xTandem) {
                            no_x++;
                        }
                        if (!omssa) {
                            no_o++;
                        }

                        progressDialog.increaseProgressValue();
                    }

                    if (!progressDialog.isRunCanceled()) {

                        progressDialog.setIndeterminate(true);
                        progressDialog.setTitle("Updating Tables. Please Wait...");

                        int nMascot = m;
                        int nOMSSA = o;
                        int nXTandem = x;

                        double biggestValue = Math.max(Math.max(nMascot, nOMSSA), nXTandem);
                        biggestValue = Math.max(biggestValue, Math.max(Math.max(no_o, no_x), no_m));

                        if (omssaUsed && xtandemUsed && mascotUsed) {
                            updateThreeWayVennDiagram(vennDiagramButton, nOMSSA, nXTandem, nMascot,
                                    ox, mo, mx, omx,
                                    "OMSSA", "X!Tandem", "Mascot");
                        } else if (omssaUsed && xtandemUsed) {
                            updateTwoWayVennDiagram(vennDiagramButton, nOMSSA, nXTandem, ox, "OMSSA", "X!Tandem");
                        } else if (xtandemUsed && mascotUsed) {
                            updateTwoWayVennDiagram(vennDiagramButton, nXTandem, nMascot, mx, "X!Tandem", "Mascot");
                        } else if (omssaUsed && mascotUsed) {
                            updateTwoWayVennDiagram(vennDiagramButton, nOMSSA, nMascot, mo, "OMSSA", "Mascot");
                        } else {
                            vennDiagramButton.setText(null);
                            vennDiagramButton.setToolTipText(null);
                            vennDiagramButton.setIcon(null);
                        }

                        int searchEngineRowCounter = 0;

                        if (omssaUsed) {
                            ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                                        ++searchEngineRowCounter, "OMSSA",
                                        nOMSSA, nOMSSA - ox - mo + omx, nOMSSA, ox, mo, omx, no_o
                                    });
                        }

                        if (xtandemUsed) {
                            ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                                        ++searchEngineRowCounter, "X!Tandem",
                                        nXTandem, nXTandem - ox - mx + omx, ox, nXTandem, mx, omx, no_x
                                    });
                        }

                        if (mascotUsed) {
                            ((DefaultTableModel) searchEngineTable.getModel()).addRow(new Object[]{
                                        ++searchEngineRowCounter, "Mascot",
                                        nMascot, nMascot - mo - mx + omx, mo, mx, nMascot, omx, no_m
                                    });
                        }

                        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Validated PSMs").getCellRenderer()).setMaxValue(biggestValue);
                        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unique PSMs").getCellRenderer()).setMaxValue(biggestValue);
                        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("OMSSA").getCellRenderer()).setMaxValue(biggestValue);
                        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("X!Tandem").getCellRenderer()).setMaxValue(biggestValue);
                        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Mascot").getCellRenderer()).setMaxValue(biggestValue);
                        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("All").getCellRenderer()).setMaxValue(biggestValue);
                        ((JSparklinesBarChartTableCellRenderer) searchEngineTable.getColumn("Unassigned").getCellRenderer()).setMaxValue(biggestValue);

                        showSparkLines(peptideShakerGUI.showSparklines());

                        searchEngineTable.revalidate();
                        searchEngineTable.repaint();

                        progressDialog.setTitle("Updating Spectrum Table. Please Wait...");

                        ArrayList<String> fileNames = peptideShakerGUI.getSearchParameters().getSpectrumFiles();
                        String[] filesArray = new String[fileNames.size()];
                        int cpt = 0;

                        for (String tempName : fileNames) {
                            filesArray[cpt] = Util.getFileName(tempName);
                            cpt++;
                        }

                        fileNamesCmb.setModel(new DefaultComboBoxModel(filesArray));

                        // update the slider tooltips
                        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();
                        accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " Da");
                        intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");

                        formComponentResized(null);

                        // enable the contextual export options
                        exportSearchEnginePerformanceJButton.setEnabled(true);
                        exportSpectrumSelectionJButton.setEnabled(true);
                        exportSpectrumJButton.setEnabled(true);
                        exportPsmsJButton.setEnabled(true);

                        peptideShakerGUI.setUpdated(PeptideShakerGUI.SPECTRUM_ID_TAB_INDEX, true);
                    }

                    progressDialog.dispose();

                    // return the peptide shaker icon to the standard version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                    if (!progressDialog.isRunCanceled()) {
                        fileSelectionChanged();
                    }

                } catch (Exception e) {
                    progressDialog.dispose();
                    peptideShakerGUI.catchException(e);

                    // return the peptide shaker icon to the standard version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                }
            }
        }.start();
    }

    /**
     * Create a Venn diagram and add it to the given button.
     *
     * @param diagramButton the button to add the diagram to
     * @param a the size of A
     * @param b the size of B
     * @param c the size of C
     * @param ab the overlapp of A and B
     * @param ac the overlapp of A and C
     * @param bc the overlapp of B and C
     * @param abc the number of values in A, B and C
     * @param titleA the title of dataset A
     * @param titleB the title of dataset B
     * @param titleC the title of dataset C
     */
    private void updateThreeWayVennDiagram(JButton diagramButton, int a, int b, int c, int ab, int ac, int bc, int abc,
            String titleA, String titleB, String titleC) {

        double maxValue = Math.max(Math.max(a, b), c);
        if (maxValue < 1) {
            maxValue = 1;
        }

        // @TODO: move this method to utilities?

        final VennDiagram chart = GCharts.newVennDiagram(
                a / maxValue, b / maxValue, c / maxValue, ab / maxValue, ac / maxValue, bc / maxValue, abc / maxValue);

        // @TODO: remove the hardcoding below!!!

        if (diagramButton.getWidth() == 0) {
            chart.setSize(173, searchEngineTable.getHeight());
        } else {
            chart.setSize(diagramButton.getWidth(), diagramButton.getHeight() - 10);
        }

        chart.setCircleLegends(titleA, titleB, titleC);
        chart.setCircleColors(Color.GREEN, Color.RED, Color.BLUE);

        try {
            diagramButton.setText("");
            ImageIcon icon = new ImageIcon(new URL(chart.toURLString()));

            if (icon.getImageLoadStatus() == MediaTracker.ERRORED) {
                diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
                diagramButton.setToolTipText("Not available in off line mode");
            } else {
                diagramButton.setIcon(icon);

                diagramButton.setToolTipText("<html>"
                        + titleA + ": " + a + "<br>"
                        + titleB + ": " + b + "<br>"
                        + titleC + ": " + c + "<br><br>"
                        + titleA + " & " + titleB + ": " + ab + "<br>"
                        + titleA + " & " + titleC + ": " + ac + "<br>"
                        + titleB + " & " + titleC + ": " + bc + "<br><br>"
                        + titleA + " & " + titleB + " & " + titleC + ": " + abc
                        + "</html>");
            }
        } catch (IOException e) {
            e.printStackTrace();
            diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
            diagramButton.setToolTipText("Not available due to an error occuring");
        }
    }

    /**
     * Create a Venn diagram and add it to the given button.
     *
     * @param diagramButton the button to add the diagram to
     * @param a the size of A
     * @param b the size of B
     * @param ab the overlapp of A and B
     * @param titleA the title of dataset A
     * @param titleB the title of dataset B
     */
    private void updateTwoWayVennDiagram(JButton diagramButton, int a, int b, int ab, String titleA, String titleB) {

        double maxValue = Math.max(a, b);
        if (maxValue < 1) {
            maxValue = 1;
        }

        // @TODO: move this method to utilities?

        final VennDiagram chart = GCharts.newVennDiagram(
                a / maxValue, b / maxValue, 0, ab / maxValue, 0, 0, 0);

        // @TODO: remove the hardcoding below!!!

        if (diagramButton.getWidth() == 0) {
            chart.setSize(173, searchEngineTable.getHeight());
        } else {
            chart.setSize(diagramButton.getWidth(), diagramButton.getHeight() - 10);
        }

        chart.setCircleLegends(titleA, titleB, "");
        chart.setCircleColors(Color.GREEN, Color.RED, Color.newColor(Util.color2Hex(diagramButton.getBackground())));

        try {
            diagramButton.setText("");
            ImageIcon icon = new ImageIcon(new URL(chart.toURLString()));

            if (icon.getImageLoadStatus() == MediaTracker.ERRORED) {
                diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
                diagramButton.setToolTipText("Not available in off line mode");
            } else {
                diagramButton.setIcon(icon);

                diagramButton.setToolTipText("<html>"
                        + titleA + ": " + a + "<br>"
                        + titleB + ": " + b + "<br>"
                        + titleA + " & " + titleB + ": " + ab
                        + "</html>");
            }
        } catch (IOException e) {
            e.printStackTrace();
            diagramButton.setText("<html><p align=center><i>Venn Diagram<br>Not Available</i></html>");
            diagramButton.setToolTipText("Not available due to an error occuring");
        }
    }

    /**
     * Method called whenever the file selection changed.
     */
    private void fileSelectionChanged() {

        progressDialog = new ProgressDialogX(peptideShakerGUI, true);
        progressDialog.setIndeterminate(true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setVisible(true);
                progressDialog.setTitle("Updating Spectrum Table. Please Wait...");
            }
        }, "ProgressDialog").start();


        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // change the peptide shaker icon to a "waiting version"
                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                DefaultTableModel dm = (DefaultTableModel) spectrumTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                fileSelected = (String) fileNamesCmb.getSelectedItem();
                double maxMz = spectrumFactory.getMaxMz(fileSelected);
                int identifiedCounter = 0;

                progressDialog.setIndeterminate(false);
                progressDialog.setMaxProgressValue(identification.getSpectrumIdentification().size());
                progressDialog.setValue(0);

                for (String spectrumKey : identification.getSpectrumIdentification()) {

                    if (progressDialog.isRunCanceled()) {
                        break;
                    }

                    if (Spectrum.getSpectrumFile(spectrumKey).equals(fileSelected)) {
                        identifiedCounter++;
                    }
                    progressDialog.increaseProgressValue();
                }

                if (!progressDialog.isRunCanceled()) {
                    ((TitledBorder) spectrumSelectionPanel.getBorder()).setTitle("Spectrum Selection (" + (identifiedCounter) + "/" + spectrumFactory.getNSpectra(fileSelected) + ")");
                    spectrumSelectionPanel.repaint();

                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).setMaxValue(maxMz);

                    updateSelection();
                    peptideShakerGUI.mgfFileSelectionChanged(fileSelected);
                }

                progressDialog.dispose();

                // return the peptide shaker icon to the standard version
                peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
            }
        });
    }

    /**
     * Returns the key of the currently selected spectrum.
     *
     * @return the key of the currently selected spectrum
     */
    public String getSelectedSpectrumKey() {
        return Spectrum.getSpectrumKey(fileSelected, spectrumFactory.getSpectrumTitles(fileSelected).get(spectrumTable.convertRowIndexToModel(spectrumTable.getSelectedRow())));
    }

    /**
     * Updates the spectrum selected according to the last user's selection.
     */
    public void updateSelection() {

        String spectrumKey = peptideShakerGUI.getSelectedPsmKey();

        if (spectrumKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            spectrumTable.setRowSelectionInterval(0, 0);
            spectrumTable.scrollRectToVisible(spectrumTable.getCellRect(0, 0, false));
            spectrumSelectionChanged();
        } else {
            selectSpectrum(spectrumKey);
        }
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected psm.
     */
    private void newItemSelection() {
        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION, getSelectedSpectrumKey());
    }

    /**
     * Clears the currently selected psm.
     */
    private void clearItemSelection() {
        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION);
    }

    /**
     * Select the given spectrum.
     */
    private void selectSpectrum(String spectrumKey) {

        // change the peptide shaker icon to a "waiting version"
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

        String fileName = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);

        if (!((String) fileNamesCmb.getSelectedItem()).equalsIgnoreCase(fileName)) {

            updateSelection = false;
            fileNamesCmb.setSelectedItem(fileName);
            updateSelection = true;

            fileSelected = (String) fileNamesCmb.getSelectedItem();
        }

        int line = spectrumFactory.getSpectrumTitles(fileSelected).indexOf(spectrumTitle);

        if (line >= 0) {

            // @TODO: this does not work when the table is sorted!!!

            spectrumTable.setRowSelectionInterval(line, line);
            spectrumTable.scrollRectToVisible(spectrumTable.getCellRect(line, 0, false));
            spectrumSelectionChanged();
        }

        // change the peptide shaker icon to a "waiting version"
        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
    }

    /**
     * Method called whenever the spectrum selection changed
     */
    private void spectrumSelectionChanged() {

        if (spectrumTable.getSelectedRow() != -1) {
            try {

                // empty the tables
                DefaultTableModel dm = (DefaultTableModel) peptideShakerJTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                dm = (DefaultTableModel) omssaTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                dm = (DefaultTableModel) mascotTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                dm = (DefaultTableModel) xTandemTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                omssaTablePeptideTooltips = new HashMap<Integer, String>();
                xTandemTablePeptideTooltips = new HashMap<Integer, String>();
                mascotTablePeptideTooltips = new HashMap<Integer, String>();

                String key = getSelectedSpectrumKey();

                if (identification.matchExists(key)) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(key);
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getSpectrumMatchParameter(key, probabilities);

                    IdentificationFeaturesGenerator featuresGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();
                    // Fill peptide shaker table
                    String proteins = featuresGenerator.addDatabaseLinks(spectrumMatch.getBestAssumption().getPeptide().getParentProteins());

                    ((DefaultTableModel) peptideShakerJTable.getModel()).addRow(new Object[]{
                                1,
                                isBestPsmEqualForAllSearchEngines(spectrumMatch),
                                proteins,
                                spectrumMatch.getBestAssumption().getPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true),
                                probabilities.getPsmScore(),
                                probabilities.getPsmConfidence(),
                                probabilities.isValidated()
                            });

                    peptideShakerJTablePeptideTooltip = featuresGenerator.getPeptideModificationTooltipAsHtml(spectrumMatch.getBestAssumption().getPeptide());

                    // Fill Mascot table
                    if (spectrumMatch.getAllAssumptions(Advocate.MASCOT) != null) {
                        ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.MASCOT).keySet());
                        Collections.sort(eValues);
                        int rank = 0;
                        for (double eValue : eValues) {
                            for (PeptideAssumption currentAssumption : spectrumMatch.getAllAssumptions(Advocate.MASCOT).get(eValue)) {
                                probabilities = (PSParameter) currentAssumption.getUrParam(probabilities);
                                proteins = featuresGenerator.addDatabaseLinks(currentAssumption.getPeptide().getParentProteins());

                                ((DefaultTableModel) mascotTable.getModel()).addRow(new Object[]{
                                            ++rank,
                                            proteins,
                                            currentAssumption.getPeptide().getModifiedSequenceAsHtml(
                                            peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true),
                                            currentAssumption.getIdentificationCharge().value,
                                            currentAssumption.getEValue(),
                                            probabilities.getSearchEngineConfidence()
                                        });

                                mascotTablePeptideTooltips.put(rank, featuresGenerator.getPeptideModificationTooltipAsHtml(currentAssumption.getPeptide()));
                                mascotPeptideKeys.put(rank, currentAssumption.getPeptide().getKey());
                            }
                        }
                    }

                    // Fill OMSSA table
                    omssaPeptideKeys = new HashMap<Integer, String>();

                    if (spectrumMatch.getAllAssumptions(Advocate.OMSSA) != null) {
                        ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.OMSSA).keySet());
                        Collections.sort(eValues);
                        int rank = 0;
                        for (double eValue : eValues) {
                            for (PeptideAssumption currentAssumption : spectrumMatch.getAllAssumptions(Advocate.OMSSA).get(eValue)) {
                                probabilities = (PSParameter) currentAssumption.getUrParam(probabilities);
                                proteins = featuresGenerator.addDatabaseLinks(currentAssumption.getPeptide().getParentProteins());

                                ((DefaultTableModel) omssaTable.getModel()).addRow(new Object[]{
                                            ++rank,
                                            proteins,
                                            currentAssumption.getPeptide().getModifiedSequenceAsHtml(
                                            peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true),
                                            currentAssumption.getIdentificationCharge().value,
                                            currentAssumption.getEValue(),
                                            probabilities.getSearchEngineConfidence()
                                        });

                                omssaTablePeptideTooltips.put(rank, featuresGenerator.getPeptideModificationTooltipAsHtml(currentAssumption.getPeptide()));
                                omssaPeptideKeys.put(rank, currentAssumption.getPeptide().getKey());
                            }
                        }
                    }

                    // Fill X!Tandem table
                    xtandemPeptideKeys = new HashMap<Integer, String>();

                    if (spectrumMatch.getAllAssumptions(Advocate.XTANDEM) != null) {
                        ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.XTANDEM).keySet());
                        Collections.sort(eValues);
                        int rank = 0;
                        for (double eValue : eValues) {
                            for (PeptideAssumption currentAssumption : spectrumMatch.getAllAssumptions(Advocate.XTANDEM).get(eValue)) {
                                probabilities = (PSParameter) currentAssumption.getUrParam(probabilities);
                                proteins = featuresGenerator.addDatabaseLinks(currentAssumption.getPeptide().getParentProteins());

                                ((DefaultTableModel) xTandemTable.getModel()).addRow(new Object[]{
                                            ++rank,
                                            proteins,
                                            currentAssumption.getPeptide().getModifiedSequenceAsHtml(
                                            peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true),
                                            currentAssumption.getIdentificationCharge().value,
                                            currentAssumption.getEValue(),
                                            probabilities.getSearchEngineConfidence()
                                        });

                                xTandemTablePeptideTooltips.put(rank, featuresGenerator.getPeptideModificationTooltipAsHtml(currentAssumption.getPeptide()));
                                xtandemPeptideKeys.put(rank, currentAssumption.getPeptide().getKey());
                            }
                        }
                    }

                    ((DefaultTableModel) peptideShakerJTable.getModel()).fireTableDataChanged();
                    ((DefaultTableModel) omssaTable.getModel()).fireTableDataChanged();
                    ((DefaultTableModel) mascotTable.getModel()).fireTableDataChanged();
                    ((DefaultTableModel) xTandemTable.getModel()).fireTableDataChanged();

                    // select one of the matches
                    if (omssaTable.getRowCount() > 0) {
                        omssaTable.setRowSelectionInterval(0, 0);
                    } else if (xTandemTable.getRowCount() > 0) {
                        xTandemTable.setRowSelectionInterval(0, 0);
                    } else if (mascotTable.getRowCount() > 0) {
                        mascotTable.setRowSelectionInterval(0, 0);
                    }

                    peptideShakerJTable.revalidate();
                    peptideShakerJTable.repaint();
                    mascotTable.revalidate();
                    mascotTable.repaint();
                    xTandemTable.revalidate();
                    xTandemTable.repaint();
                    omssaTable.revalidate();
                    omssaTable.repaint();
                }


                newItemSelection();

                // invoke later to give time for components to update
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        //update the spectrum
                        updateSpectrum();
                    }
                });

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Update the spectrum based on the currently selected PSM.
     */
    public void updateSpectrum() {

        if (spectrumTable.getSelectedRow() != -1) {

            try {
                spectrumChartPanel.removeAll();

                String key = getSelectedSpectrumKey();
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(key);
                SpectrumPanel spectrum = null;
                AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();

                if (currentSpectrum != null) {
                    Precursor precursor = currentSpectrum.getPrecursor();
                    String charge;
                    if (identification.matchExists(currentSpectrumKey)) {
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(currentSpectrumKey);
                        charge = spectrumMatch.getBestAssumption().getIdentificationCharge().toString();
                    } else {
                        charge = precursor.getPossibleChargesAsString();
                    }
                    if (currentSpectrum.getMzValuesAsArray().length > 0 && currentSpectrum.getIntensityValuesAsArray().length > 0) {
                        spectrum = new SpectrumPanel(
                                currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                                precursor.getMz(), charge,
                                "", 40, false, false, false, 2, false);
                        spectrum.setKnownMassDeltas(peptideShakerGUI.getCurrentMassDeltas());
                        spectrum.setDeltaMassWindow(annotationPreferences.getFragmentIonAccuracy());
                        spectrum.setBorder(null);
                        spectrum.setDataPointAndLineColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakColor(), 0);
                        spectrum.setPeakWaterMarkColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakColor());
                        spectrum.setPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakWidth());
                        spectrum.setBackgroundPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakWidth());
                    }
                }

                if (identification.matchExists(key)) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(key);

                    if (currentSpectrum != null) {

                        if (currentSpectrum.getMzValuesAsArray().length > 0 && currentSpectrum.getIntensityValuesAsArray().length > 0) {

                            // omssa annotation (if any)
                            if (omssaTable.getSelectedRow() != -1) {

                                ArrayList<Double> omssaEValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.OMSSA).keySet());
                                Collections.sort(omssaEValues);
                                Peptide currentPeptide = null;
                                int cpt = 0;
                                boolean found = false;

                                for (double eValue : omssaEValues) {
                                    for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(Advocate.OMSSA).get(eValue)) {
                                        if (cpt == omssaTable.getSelectedRow()) {
                                            currentPeptide = peptideAssumption.getPeptide();
                                            found = true;
                                            break;
                                        }
                                        cpt++;
                                    }

                                    if (found) {
                                        break;
                                    }
                                }

                                int identificationCharge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                                annotationPreferences.setCurrentSettings(currentPeptide,
                                        identificationCharge, !currentSpectrumKey.equalsIgnoreCase(spectrumMatch.getKey()));
                                ArrayList<IonMatch> annotations = specificAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                        annotationPreferences.getNeutralLosses(),
                                        annotationPreferences.getValidatedCharges(),
                                        identificationCharge,
                                        currentSpectrum, currentPeptide,
                                        currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                        annotationPreferences.getFragmentIonAccuracy(), false);
                                currentSpectrumKey = spectrumMatch.getKey();

                                // add the spectrum annotations
                                spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                                spectrum.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                                spectrum.setYAxisZoomExcludesBackgroundPeaks(peptideShakerGUI.getAnnotationPreferences().yAxisZoomExcludesBackgroundPeaks());

                                // add de novo sequencing
                                peptideShakerGUI.addAutomaticDeNovoSequencing(currentPeptide, annotations, spectrum);

                                peptideShakerGUI.updateAnnotationMenus(identificationCharge, currentPeptide);
                            }

                            // xtandem annotation (if any)
                            if (xTandemTable.getSelectedRow() != -1) {

                                ArrayList<Double> xTandemEValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.XTANDEM).keySet());
                                Collections.sort(xTandemEValues);
                                Peptide currentPeptide = null;
                                int cpt = 0;
                                boolean found = false;

                                for (double eValue : xTandemEValues) {
                                    for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(Advocate.XTANDEM).get(eValue)) {
                                        if (cpt == xTandemTable.getSelectedRow()) {
                                            currentPeptide = peptideAssumption.getPeptide();
                                            found = true;
                                            break;
                                        }
                                        cpt++;
                                    }

                                    if (found) {
                                        break;
                                    }
                                }

                                int identificationCharge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                                annotationPreferences.setCurrentSettings(currentPeptide,
                                        identificationCharge, !currentSpectrumKey.equalsIgnoreCase(spectrumMatch.getKey()));
                                ArrayList<IonMatch> annotations = specificAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                        annotationPreferences.getNeutralLosses(),
                                        annotationPreferences.getValidatedCharges(),
                                        identificationCharge,
                                        currentSpectrum, currentPeptide,
                                        currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                        annotationPreferences.getFragmentIonAccuracy(), false);
                                currentSpectrumKey = spectrumMatch.getKey();

                                // add the spectrum annotations
                                spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                                spectrum.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                                spectrum.setYAxisZoomExcludesBackgroundPeaks(peptideShakerGUI.getAnnotationPreferences().yAxisZoomExcludesBackgroundPeaks());

                                // add de novo sequencing
                                peptideShakerGUI.addAutomaticDeNovoSequencing(currentPeptide, annotations, spectrum);

                                peptideShakerGUI.updateAnnotationMenus(identificationCharge, currentPeptide);

                            }

                            // mascot annotation (if any)
                            if (mascotTable.getSelectedRow() != -1) {

                                ArrayList<Double> mascotEValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.MASCOT).keySet());
                                Collections.sort(mascotEValues);
                                Peptide currentPeptide = null;
                                int cpt = 0;
                                boolean found = false;

                                for (double eValue : mascotEValues) {
                                    for (PeptideAssumption peptideAssumption : spectrumMatch.getAllAssumptions(Advocate.MASCOT).get(eValue)) {
                                        if (cpt == mascotTable.getSelectedRow()) {
                                            currentPeptide = peptideAssumption.getPeptide();
                                            found = true;
                                            break;
                                        }
                                        cpt++;
                                    }

                                    if (found) {
                                        break;
                                    }
                                }

                                int identificationCharge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                                annotationPreferences.setCurrentSettings(currentPeptide,
                                        identificationCharge, !currentSpectrumKey.equalsIgnoreCase(spectrumMatch.getKey()));
                                ArrayList<IonMatch> annotations = specificAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                        annotationPreferences.getNeutralLosses(),
                                        annotationPreferences.getValidatedCharges(),
                                        identificationCharge,
                                        currentSpectrum, currentPeptide,
                                        currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                        annotationPreferences.getFragmentIonAccuracy(), false);
                                currentSpectrumKey = spectrumMatch.getKey();

                                // add the spectrum annotations
                                spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                                spectrum.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                                spectrum.setYAxisZoomExcludesBackgroundPeaks(peptideShakerGUI.getAnnotationPreferences().yAxisZoomExcludesBackgroundPeaks());

                                // add de novo sequencing
                                peptideShakerGUI.addAutomaticDeNovoSequencing(currentPeptide, annotations, spectrum);

                                peptideShakerGUI.updateAnnotationMenus(identificationCharge, currentPeptide);
                            }
                        }
                    }
                }
                if (spectrum != null) {
                    spectrumChartPanel.add(spectrum);
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }

        spectrumChartPanel.revalidate();
        spectrumChartPanel.repaint();
    }

    /**
     * Returns the spectrum panel.
     *
     * @return the spectrum panel
     */
    public Component getSpectrum() {
        return (Component) spectrumChartPanel.getComponent(0);
    }

    /**
     * Returns the current spectrum as an mgf string.
     *
     * @return the current spectrum as an mgf string
     */
    public String getSpectrumAsMgf() {

        if (spectrumTable.getSelectedRow() != -1) {
            String spectrumKey = getSelectedSpectrumKey();
            MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

            if (currentSpectrum != null) {
                return currentSpectrum.asMgf();
            }
        }

        return null;
    }

    /**
     * Makes sure that the annotation menu bar is visible.
     */
    public void showSpectrumAnnotationMenu() {
        spectrumAnnotationMenuPanel.removeAll();
        spectrumAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
        peptideShakerGUI.updateAnnotationMenuBarVisableOptions(true, false, false, false);
    }

    /**
     * Set the intensity slider value.
     *
     * @param value the intensity slider value
     */
    public void setIntensitySliderValue(int value) {
        intensitySlider.setValue(value);
    }

    /**
     * Set the accuracy slider value.
     *
     * @param value the accuracy slider value
     */
    public void setAccuracySliderValue(int value) {
        accuracySlider.setValue(value);
    }

    /**
     * Update the PTM color coding.
     */
    public void updatePtmColors() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        spectrumSelectionChanged();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Export the table contents to the clipboard.
     *
     * @param index
     */
    private void copyTableContentToClipboardOrFile(TableIndex index) {

        final TableIndex tableIndex = index;

        if (tableIndex == TableIndex.SEARCH_ENGINE_PERFORMANCE
                || tableIndex == TableIndex.SPECTRUM_FILES
                || tableIndex == TableIndex.PSM_TABLES) {


            // get the file to send the output to
            final File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Tab separated text file (.txt)", "Export...", false);

            if (selectedFile != null) {
                try {
                    final BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile));

                    progressDialog = new ProgressDialogX(peptideShakerGUI, true);

                    new Thread(new Runnable() {

                        public void run() {
                            progressDialog.setIndeterminate(true);
                            progressDialog.setTitle("Copying to File. Please Wait...");
                            try {
                                progressDialog.setVisible(true);
                            } catch (IndexOutOfBoundsException e) {
                                // ignore
                            }
                        }
                    }, "ProgressDialog").start();

                    new Thread("ExportThread") {

                        @Override
                        public void run() {

                            try {

                                if (tableIndex == TableIndex.SEARCH_ENGINE_PERFORMANCE) {
                                    Util.tableToFile(searchEngineTable, "\t", progressDialog, true, writer);
                                } else if (tableIndex == TableIndex.SPECTRUM_FILES) {
                                    Util.tableToFile(spectrumTable, "\t", progressDialog, true, writer);
                                } else if (tableIndex == TableIndex.PSM_TABLES) {

                                    writer.write("PeptideShaker\n\n");
                                    writer.write("\tProtein(s)\tSequence\tVariable Modification\tLocation Confidence\tScore\tConfidence\tValidated\n");

                                    try {
                                        // the PeptideShaker PSM table
                                        String key = getSelectedSpectrumKey();
                                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(key);
                                        PSParameter probabilities = new PSParameter();
                                        probabilities = (PSParameter) identification.getSpectrumMatchParameter(key, probabilities);

                                        writer.write("1\t");

                                        ArrayList<String> parentProteins = spectrumMatch.getBestAssumption().getPeptide().getParentProteins();

                                        writer.write(parentProteins.get(0));

                                        for (int i = 1; i < parentProteins.size(); i++) {
                                            writer.write(", " + parentProteins.get(i));
                                        }

                                        writer.write("\t");

                                        writer.write(spectrumMatch.getBestAssumption().getPeptide().getModifiedSequenceAsString(true) + "\t");
                                        writer.write(OutputGenerator.getPeptideModificationsAsString(spectrumMatch.getBestAssumption().getPeptide()) + "\t");
                                        writer.write(OutputGenerator.getPeptideModificationLocations(spectrumMatch.getBestAssumption().getPeptide(),
                                                identification.getPeptideMatch(spectrumMatch.getBestAssumption().getPeptide().getKey())) + "\t");
                                        writer.write(probabilities.getPsmScore() + "\t");
                                        writer.write(probabilities.getPsmConfidence() + "\t");
                                        writer.write(probabilities.isValidated() + "\n");


                                        // the search engine tables
                                        writer.write("\n\nOMSSA\n\n");
                                        writer.write("\tProtein(s)\tSequence\tVariable Modification\tLocation Confidence\te-value\tConfidence\n");
                                        writer.write(getSearchEnginePsmTableAsString(spectrumMatch, probabilities, Advocate.OMSSA));

                                        writer.write("\n\nX!Tandem\n\n");
                                        writer.write("\tProtein(s)\tSequence\tVariable Modification\tLocation Confidence\te-value\tConfidence\n");
                                        writer.write(getSearchEnginePsmTableAsString(spectrumMatch, probabilities, Advocate.XTANDEM));

                                        writer.write("\n\nMascot\n\n");
                                        writer.write("\tProtein(s)\tSequence\tVariable Modification\tLocation Confidence\te-value\tConfidence\n");
                                        writer.write(getSearchEnginePsmTableAsString(spectrumMatch, probabilities, Advocate.MASCOT));
                                    } catch (Exception e) {
                                        peptideShakerGUI.catchException(e);
                                    }
                                }

                                writer.close();

                                progressDialog.dispose();

                                if (!progressDialog.isRunCanceled()) {
                                    JOptionPane.showMessageDialog(peptideShakerGUI, "Table content copied to file:\n" + selectedFile.getPath(), "Copied to File", JOptionPane.INFORMATION_MESSAGE);
                                }

                            } catch (IOException e) {
                                progressDialog.dispose();
                                JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                                e.printStackTrace();
                            }
                        }
                    }.start();

                } catch (IOException e) {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns the contents of the given search engine psm table as a string.
     *
     * @param spectrumMatch the current spectrum match
     * @param probabilities the current probabilities
     * @param advocate the type, OMSSA, XTandem or Mascot, as coded in the
     * Advocate class
     * @return the contents of the given search engine psm table as a string
     */
    private String getSearchEnginePsmTableAsString(SpectrumMatch spectrumMatch, PSParameter probabilities, int advocate) {

        String result = "";

        if (spectrumMatch.getAllAssumptions(advocate) != null) {

            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(advocate).keySet());
            Collections.sort(eValues);
            int rank = 0;
            for (double eValue : eValues) {
                for (PeptideAssumption currentAssumption : spectrumMatch.getAllAssumptions(advocate).get(eValue)) {
                    probabilities = (PSParameter) currentAssumption.getUrParam(probabilities);

                    result += ++rank + "\t";

                    ArrayList<String> parentProteins = currentAssumption.getPeptide().getParentProteins();
                    result += parentProteins.get(0);

                    for (int i = 1; i < parentProteins.size(); i++) {
                        result += ", " + parentProteins.get(i);
                    }

                    result += "\t";

                    result += currentAssumption.getPeptide().getModifiedSequenceAsString(true) + "\t";
                    result += OutputGenerator.getPeptideModificationsAsString(currentAssumption.getPeptide()) + "\t";
                    try {
                    result += OutputGenerator.getPeptideModificationLocations(currentAssumption.getPeptide(),
                            identification.getPeptideMatch(currentAssumption.getPeptide().getKey())) + "\t";
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    result+= "error\t";
                }

                    result += currentAssumption.getEValue() + "\t";
                    result += probabilities.getSearchEngineConfidence() + "\t";
                }
            }
        }

        return result;
    }

    /**
     * Method called whenever the component is resized to maintain the look of
     * the GUI.
     */
    public void updateSeparators() {
        // set the sliders split pane divider location
        if (peptideShakerGUI.getUserPreferences().showSliders()) {
            slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth() - 30);
        } else {
            slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth());
        }
    }

    /**
     * Updates and displays the current spectrum slider tooltip.
     */
    private void updateSpectrumSliderToolTip() {
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();

        spectrumJPanel.setToolTipText("<html>Accuracy: " + Util.roundDouble(accuracy, 2) + " Da<br>"
                + "Level: " + intensitySlider.getValue() + "%</html>");

        // show the tooltip now
        ToolTipManager.sharedInstance().mouseMoved(
                new MouseEvent(spectrumJPanel, 0, 0, 0,
                spectrumJPanel.getWidth() - 150, spectrumJPanel.getY() + 20, // X-Y of the mouse for the tool tip
                0, false));
    }

    /**
     * Returns true if all the used search engines agree on the top PSM, false
     * otherwise.
     *
     * @param spectrumMatch the PSM to check
     * @return true if all the used search engines agree on the top PSM
     */
    public static int isBestPsmEqualForAllSearchEngines(SpectrumMatch spectrumMatch) {

        // @TODO: there's probably an easier more elegant way of doing all of this (yes but it would ruin the backward compatibility, we'll wait a bit)

        String omssaMatch = null;
        String xtandemMatch = null;
        String mascotMatch = null;
        int omssaCharge = -1;
        int xtandemCharge = -1;
        int mascotCharge = -1;

        if (spectrumMatch.getAllAssumptions(Advocate.OMSSA) != null) {
            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.OMSSA).keySet());
            Collections.sort(eValues);

            if (eValues.size() > 0) {
                if (spectrumMatch.getAllAssumptions(Advocate.OMSSA).get(eValues.get(0)).size() > 0) {
                    omssaMatch = spectrumMatch.getAllAssumptions(Advocate.OMSSA).get(eValues.get(0)).get(0).getPeptide().getModifiedSequenceAsString(true);
                    omssaCharge = spectrumMatch.getAllAssumptions(Advocate.OMSSA).get(eValues.get(0)).get(0).getIdentificationCharge().value;
                }
            }
        }

        if (spectrumMatch.getAllAssumptions(Advocate.XTANDEM) != null) {
            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.XTANDEM).keySet());
            Collections.sort(eValues);

            if (eValues.size() > 0) {
                if (spectrumMatch.getAllAssumptions(Advocate.XTANDEM).get(eValues.get(0)).size() > 0) {
                    xtandemMatch = spectrumMatch.getAllAssumptions(Advocate.XTANDEM).get(eValues.get(0)).get(0).getPeptide().getModifiedSequenceAsString(true);
                    xtandemCharge = spectrumMatch.getAllAssumptions(Advocate.XTANDEM).get(eValues.get(0)).get(0).getIdentificationCharge().value;
                }
            }
        }

        if (spectrumMatch.getAllAssumptions(Advocate.MASCOT) != null) {
            ArrayList<Double> eValues = new ArrayList<Double>(spectrumMatch.getAllAssumptions(Advocate.MASCOT).keySet());
            Collections.sort(eValues);

            if (eValues.size() > 0) {
                if (spectrumMatch.getAllAssumptions(Advocate.MASCOT).get(eValues.get(0)).size() > 0) {
                    mascotMatch = spectrumMatch.getAllAssumptions(Advocate.MASCOT).get(eValues.get(0)).get(0).getPeptide().getModifiedSequenceAsString(true);
                    mascotCharge = spectrumMatch.getAllAssumptions(Advocate.MASCOT).get(eValues.get(0)).get(0).getIdentificationCharge().value;
                }
            }
        }


        if (omssaMatch != null && xtandemMatch != null && mascotMatch != null) {
            if ((omssaMatch.equalsIgnoreCase(xtandemMatch) && omssaMatch.equalsIgnoreCase(mascotMatch))
                    && (omssaCharge == xtandemCharge && omssaCharge == mascotCharge)) {
                return AGREEMENT;
            } else {
                return CONFLICT;
            }
        } else if (omssaMatch != null && xtandemMatch != null) {
            if (!mascotUsed) {
                if (omssaMatch.equalsIgnoreCase(xtandemMatch)
                        && omssaCharge == xtandemCharge) {
                    return AGREEMENT;
                } else {
                    return CONFLICT;
                }
            } else {
                return PARTIALLY_MISSING;
            }
        } else if (omssaMatch != null && mascotMatch != null) {
            if (!xtandemUsed) {
                if (omssaMatch.equalsIgnoreCase(mascotMatch)
                        && omssaCharge == mascotCharge) {
                    return AGREEMENT;
                } else {
                    return CONFLICT;
                }
            } else {
                return PARTIALLY_MISSING;
            }
        } else if (xtandemMatch != null && mascotMatch != null) {
            if (!omssaUsed) {
                if (xtandemMatch.equalsIgnoreCase(mascotMatch)
                        && xtandemCharge == mascotCharge) {
                    return AGREEMENT;
                } else {
                    return CONFLICT;
                }
            } else {
                return PARTIALLY_MISSING;
            }
        } else if (omssaMatch != null) {
            if (xtandemUsed || mascotUsed) {
                return PARTIALLY_MISSING;
            } else {
                return AGREEMENT;
            }

        } else if (xtandemMatch != null) {
            if (omssaUsed || mascotUsed) {
                return PARTIALLY_MISSING;
            } else {
                return AGREEMENT;
            }

        } else if (mascotMatch != null) {
            if (omssaUsed || xtandemUsed) {
                return PARTIALLY_MISSING;
            } else {
                return AGREEMENT;
            }

        } else {
            return NO_ID;
        }
    }

    /**
     * Table model for the table listing all spectra
     */
    private class SpectrumTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (fileSelected != null) {
                return spectrumFactory.getNSpectra(fileSelected);
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "SE";
                case 2:
                    return "Title";
                case 3:
                    return "m/z";
                case 4:
                    return "Charge";
                case 5:
                    return "RT";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            try {
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        int searchEngineAgreement;
                        String spectrumTitle = spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                        String spectrumKey = Spectrum.getSpectrumKey(fileSelected, spectrumTitle);
                        if (!identification.matchExists(spectrumKey)) {
                            searchEngineAgreement = NO_ID;
                        } else {
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            searchEngineAgreement = isBestPsmEqualForAllSearchEngines(spectrumMatch);
                        }
                        return searchEngineAgreement;
                    case 2:
                        return spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                    case 3:
                        spectrumTitle = spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                        spectrumKey = Spectrum.getSpectrumKey(fileSelected, spectrumTitle);
                        Precursor precursor = peptideShakerGUI.getPrecursor(spectrumKey, false);
                        if (precursor != null) {
                            return precursor.getMz();
                        } else {
                            return null;
                        }
                    case 4:
                        spectrumTitle = spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                        spectrumKey = Spectrum.getSpectrumKey(fileSelected, spectrumTitle);
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey, false);
                        Integer charge = null;
                        if (precursor != null && !precursor.getPossibleCharges().isEmpty()) {
                            charge = precursor.getPossibleCharges().get(0).value;
                        }
                        return charge;
                    case 5:
                        spectrumTitle = spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                        spectrumKey = Spectrum.getSpectrumKey(fileSelected, spectrumTitle);
                        precursor = peptideShakerGUI.getPrecursor(spectrumKey, false);
                        if (precursor != null) {
                            return precursor.getRt();
                        } else {
                            return null;
                        }
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
}
