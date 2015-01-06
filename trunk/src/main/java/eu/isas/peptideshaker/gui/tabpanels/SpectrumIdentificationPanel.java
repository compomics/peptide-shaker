package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.AminoAcidSequence;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.tags.TagComponent;
import com.compomics.util.experiment.identification.tags.tagcomponents.MassGap;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.XYPlottingDialog;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.util.gui.spectrum.MassErrorBubblePlot;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.ModificationProfile;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.scoring.PsmSpecificMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyMap;
import eu.isas.peptideshaker.scoring.targetdecoy.TargetDecoyResults;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Iterator;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * The Spectrum ID panel.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class SpectrumIdentificationPanel extends javax.swing.JPanel {

    /**
     * Turns of the gradient painting for the bar charts.
     */
    static {
        BarRenderer.setDefaultBarPainter(new StandardBarPainter());
    }
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

        SPECTRUM_FILES, PSM_TABLES
    };
    /**
     * Static index for the ID software agreement: no psm found.
     */
    public static final int NO_ID = 0;
    /**
     * Static index for the ID software agreement: the ID software have
     * different top ranking peptides.
     */
    public static final int CONFLICT = 1;
    /**
     * Static index for the ID software agreement: one or more of the softwares
     * did not identify the spectrum, while one or more of the others did.
     */
    public static final int PARTIALLY_MISSING = 2;
    /**
     * Static index for the ID software agreement: the ID softwares all have the
     * same top ranking peptide without accounting for modification
     * localization.
     */
    public static final int AGREEMENT = 3;
    /**
     * Static index for the ID software agreement: the ID softwares all have the
     * same top ranking peptide.
     */
    public static final int AGREEMENT_WITH_MODS = 4;
    /**
     * The peptide sequence tooltips for the search results table.
     */
    private ArrayList<String> searchResultsTablePeptideTooltips = null;
    /**
     * The peptide sequence tooltips for the search results table.
     */
    private String peptideShakerJTablePeptideTooltip = null;
    /**
     * The current spectrum key.
     */
    private String currentSpectrumKey = "";
    /**
     * The ID software table column header tooltips.
     */
    private ArrayList<String> idSoftwareTableToolTips;
    /**
     * The spectrum table column header tooltips.
     */
    private ArrayList<String> spectrumTableToolTips;
    /**
     * The peptide shaker table column header tooltips.
     */
    private ArrayList<String> peptideShakerTableToolTips;
    /**
     * The ID results table column header tooltips.
     */
    private ArrayList<String> idResultsTableToolTips;
    /**
     * The spectrum annotator for ID software specific results.
     */
    private PeptideSpectrumAnnotator specificAnnotator = new PeptideSpectrumAnnotator();
    /**
     * The list of search results.
     */
    private ArrayList<SpectrumIdentificationAssumption> currentAssumptionsList = new ArrayList<SpectrumIdentificationAssumption>();
    /**
     * The main GUI.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The file currently selected.
     */
    private String fileSelected = null;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The advocates used.
     */
    private ArrayList<Integer> advocatesUsed;
    /**
     * The number of validated PSMs per mgf file.
     */
    private HashMap<String, Integer> numberOfValidatedPsmsMap;
    /**
     * The current spectrum panel for the selected PSM.
     */
    private SpectrumPanel spectrum;

    /**
     * Create a new SpectrumIdentificationPanel.
     *
     * @param peptideShakerGUI the PeptideShaker parent frame
     */
    public SpectrumIdentificationPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();
        formComponentResized(null);

        spectrumTableJScrollPane.getViewport().setOpaque(false);
        peptideShakerJScrollPane.getViewport().setOpaque(false);
        idResultsTableJScrollPane.getViewport().setOpaque(false);

        fileNamesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        setUpGUI();
        setTableProperties();
    }

    /**
     * Set up the GUI.
     */
    private void setUpGUI() {
        ((TitledBorder) idSoftwarePanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum Identification Overview" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        ((TitledBorder) psmsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide Spectrum Matches" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        ((TitledBorder) spectrumPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum & Fragment Ions" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        ((TitledBorder) spectrumSelectionPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum Selection" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);

        spectrumSelectionDialog.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
        spectrumSelectionDialog.setModal(true);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        // correct the color for the upper right corner
        JPanel spectrumCorner = new JPanel();
        spectrumCorner.setBackground(spectrumTable.getTableHeader().getBackground());
        spectrumTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, spectrumCorner);
        JPanel idResultsCorner = new JPanel();
        idResultsCorner.setBackground(searchResultsTable.getTableHeader().getBackground());
        idResultsTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, idResultsCorner);

        peptideShakerJTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        searchResultsTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));

        peptideShakerJTable.getTableHeader().setReorderingAllowed(false);
        spectrumTable.getTableHeader().setReorderingAllowed(false);
        searchResultsTable.getTableHeader().setReorderingAllowed(false);

        spectrumTable.setAutoCreateRowSorter(true);

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
        spectrumTable.getColumn(" ").setMinWidth(50);
        spectrumTable.getColumn(" ").setMaxWidth(50);
        spectrumTable.getColumn("  ").setMinWidth(30);
        spectrumTable.getColumn("  ").setMaxWidth(30);
        spectrumTable.getColumn("Confidence").setMaxWidth(90);
        spectrumTable.getColumn("Confidence").setMinWidth(90);

        searchResultsTable.getColumn(" ").setMinWidth(30);
        searchResultsTable.getColumn(" ").setMaxWidth(30);
        searchResultsTable.getColumn("SE").setMinWidth(37);
        searchResultsTable.getColumn("SE").setMaxWidth(37);
        searchResultsTable.getColumn("Rnk").setMinWidth(37);
        searchResultsTable.getColumn("Rnk").setMaxWidth(37);
        searchResultsTable.getColumn("  ").setMinWidth(30);
        searchResultsTable.getColumn("  ").setMaxWidth(30);

        peptideShakerJTable.getColumn("ID").setMaxWidth(37);
        peptideShakerJTable.getColumn("ID").setMinWidth(37);
        spectrumTable.getColumn("ID").setMaxWidth(37);
        spectrumTable.getColumn("ID").setMinWidth(37);

        peptideShakerJTable.getColumn("Confidence").setMaxWidth(90);
        peptideShakerJTable.getColumn("Confidence").setMinWidth(90);
        searchResultsTable.getColumn("Confidence").setMaxWidth(90);
        searchResultsTable.getColumn("Confidence").setMinWidth(90);
        searchResultsTable.getColumn("Charge").setMaxWidth(90);
        searchResultsTable.getColumn("Charge").setMinWidth(90);

        // set up the psm color map
        HashMap<Integer, java.awt.Color> softwareAgreementColorMap = new HashMap<Integer, java.awt.Color>();
        softwareAgreementColorMap.put(AGREEMENT_WITH_MODS, peptideShakerGUI.getSparklineColor()); // id softwares agree with PTM certainty
        softwareAgreementColorMap.put(AGREEMENT, java.awt.Color.CYAN); // id softwares agree on peptide but not ptm certainty
        softwareAgreementColorMap.put(CONFLICT, java.awt.Color.YELLOW); // id softwares don't agree
        softwareAgreementColorMap.put(PARTIALLY_MISSING, java.awt.Color.ORANGE); // some id softwares id'ed some didn't

        // set up the psm tooltip map
        HashMap<Integer, String> idSoftwareTooltipMap = new HashMap<Integer, String>();
        idSoftwareTooltipMap.put(AGREEMENT_WITH_MODS, "ID Software Agree");
        idSoftwareTooltipMap.put(AGREEMENT, "ID Software Agree - PTM Certainty Issues");
        idSoftwareTooltipMap.put(CONFLICT, "ID Software Disagree");
        idSoftwareTooltipMap.put(PARTIALLY_MISSING, "First Hit(s) Missing");

        peptideShakerJTable.getColumn("ID").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(java.awt.Color.lightGray, softwareAgreementColorMap, idSoftwareTooltipMap));
        peptideShakerJTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        peptideShakerJTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());

        searchResultsTable.getColumn("SE").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(java.awt.Color.lightGray, Advocate.getAdvocateColorMap(), Advocate.getAdvocateToolTipMap()));

        searchResultsTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) searchResultsTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        searchResultsTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10d, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) searchResultsTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);

        // set up the psm color map
        HashMap<Integer, java.awt.Color> idSoftwareSpectrumLevelColorMap = new HashMap<Integer, java.awt.Color>();
        idSoftwareSpectrumLevelColorMap.put(AGREEMENT_WITH_MODS, peptideShakerGUI.getSparklineColor()); // id softwares agree with PTM certainty
        idSoftwareSpectrumLevelColorMap.put(AGREEMENT, java.awt.Color.CYAN); // id softwares agree on peptide but not ptm certainty
        idSoftwareSpectrumLevelColorMap.put(CONFLICT, java.awt.Color.YELLOW); // id softwares don't agree
        idSoftwareSpectrumLevelColorMap.put(PARTIALLY_MISSING, java.awt.Color.ORANGE); // some id softwares id'ed some didn't
        idSoftwareSpectrumLevelColorMap.put(NO_ID, java.awt.Color.lightGray); // no psm

        // set up the psm tooltip map
        HashMap<Integer, String> idSoftwareSpectrumLevelTooltipMap = new HashMap<Integer, String>();
        idSoftwareSpectrumLevelTooltipMap.put(AGREEMENT_WITH_MODS, "ID Software Agree");
        idSoftwareSpectrumLevelTooltipMap.put(AGREEMENT, "ID Software Agree - PTM Certainty Issues");
        idSoftwareSpectrumLevelTooltipMap.put(CONFLICT, "ID Software Disagree");
        idSoftwareSpectrumLevelTooltipMap.put(PARTIALLY_MISSING, "ID Software(s) Missing");
        idSoftwareSpectrumLevelTooltipMap.put(NO_ID, "(No PSM)");

        spectrumTable.getColumn("ID").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(java.awt.Color.lightGray, idSoftwareSpectrumLevelColorMap, idSoftwareSpectrumLevelTooltipMap));
        spectrumTable.getColumn("m/z").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 4d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("Int").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1000d, peptideShakerGUI.getSparklineColor()));
        spectrumTable.getColumn("RT (min)").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d,
                1000d, 10d, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT (min)").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT (min)").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);

        spectrumTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        spectrumTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        spectrumTable.getColumn("Protein(s)").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));

        // set up the table header tooltips
        idSoftwareTableToolTips = new ArrayList<String>();
        idSoftwareTableToolTips.add(null);
        idSoftwareTableToolTips.add("Identification Software");
        idSoftwareTableToolTips.add("Validated Peptide Spectrum Matches");
        idSoftwareTableToolTips.add("Unique Pepttide-Spectrum Matches");
        idSoftwareTableToolTips.add("Unassigned Spectra");
        idSoftwareTableToolTips.add("Identificaiton Rate (%)");

        spectrumTableToolTips = new ArrayList<String>();
        spectrumTableToolTips.add(null);
        spectrumTableToolTips.add("ID Software Agreement");
        spectrumTableToolTips.add("Spectrum Title");
        spectrumTableToolTips.add("Precursor m/z");
        spectrumTableToolTips.add("Precursor Charge");
        spectrumTableToolTips.add("Precursor Intensity");
        spectrumTableToolTips.add("Precursor Retention Time in Minutes");
        spectrumTableToolTips.add("Peptide Sequence");
        spectrumTableToolTips.add("Mapping Protein(s)");
        spectrumTableToolTips.add("Peptide Spectrum Match Confidence");
        spectrumTableToolTips.add("Validated");

        peptideShakerTableToolTips = new ArrayList<String>();
        peptideShakerTableToolTips.add(null);
        peptideShakerTableToolTips.add("ID Software Agreement");
        peptideShakerTableToolTips.add("Peptide Sequence");
        peptideShakerTableToolTips.add("Mapping Protein(s)");
        peptideShakerTableToolTips.add("Peptide Confidence");
        peptideShakerTableToolTips.add("Validated");

        idResultsTableToolTips = new ArrayList<String>();
        idResultsTableToolTips.add("Peptide Rank");
        idResultsTableToolTips.add("Search Engine / Identification Software");
        idResultsTableToolTips.add("Search Engine Rank / Identification Software Rank");
        idResultsTableToolTips.add("Peptide Sequence");
        idResultsTableToolTips.add("Precursor Charge");
        idResultsTableToolTips.add("Peptide Confidence");
        idResultsTableToolTips.add("Validated");
    }

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT (min)").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) peptideShakerJTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) searchResultsTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) searchResultsTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);

        spectrumTable.revalidate();
        spectrumTable.repaint();

        peptideShakerJTable.revalidate();
        peptideShakerJTable.repaint();

        searchResultsTable.revalidate();
        searchResultsTable.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        spectrumSelectionDialog = new javax.swing.JDialog();
        backgroundPanel = new javax.swing.JPanel();
        fileNamesCmb = new javax.swing.JComboBox();
        spectrumSelectionPsmSplitPane = new javax.swing.JSplitPane();
        spectrumSelectionJPanel = new javax.swing.JPanel();
        spectrumSelectionLayeredPane = new javax.swing.JLayeredPane();
        spectrumSelectionPanel = new javax.swing.JPanel();
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
        spectrumSelectionOptionsJButton = new javax.swing.JButton();
        spectrumSelectionHelpJButton = new javax.swing.JButton();
        exportSpectrumSelectionJButton = new javax.swing.JButton();
        contextMenuSpectrumSelectionBackgroundPanel = new javax.swing.JPanel();
        psmAndSpectrumSplitPane = new javax.swing.JSplitPane();
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
        idResultsPanel = new javax.swing.JPanel();
        spectrumIdResultsLabel = new javax.swing.JLabel();
        idResultsTableJScrollPane = new javax.swing.JScrollPane();
        searchResultsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) idResultsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        colorLegendLabel = new javax.swing.JLabel();
        psmsHelpJButton = new javax.swing.JButton();
        exportPsmsJButton = new javax.swing.JButton();
        contextMenuPsmsBackgroundPanel = new javax.swing.JPanel();
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
        idSoftwareJPanel = new javax.swing.JPanel();
        idSoftwareJLayeredPane = new javax.swing.JLayeredPane();
        idSoftwarePanel = new javax.swing.JPanel();
        overviewPlotsPanel = new javax.swing.JPanel();
        idSoftwareHelpJButton = new javax.swing.JButton();
        exportIdPerformancePerformanceJButton = new javax.swing.JButton();
        contextMenuIdSoftwareBackgroundPanel = new javax.swing.JPanel();

        spectrumSelectionDialog.setTitle("Spectrum Selection");
        spectrumSelectionDialog.setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        fileNamesCmb.setBorder(null);
        fileNamesCmb.setMinimumSize(new java.awt.Dimension(200, 20));
        fileNamesCmb.setPreferredSize(new java.awt.Dimension(400, 20));
        fileNamesCmb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileNamesCmbActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileNamesCmb, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileNamesCmb, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout spectrumSelectionDialogLayout = new javax.swing.GroupLayout(spectrumSelectionDialog.getContentPane());
        spectrumSelectionDialog.getContentPane().setLayout(spectrumSelectionDialogLayout);
        spectrumSelectionDialogLayout.setHorizontalGroup(
            spectrumSelectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumSelectionDialogLayout.setVerticalGroup(
            spectrumSelectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        spectrumSelectionPsmSplitPane.setBorder(null);
        spectrumSelectionPsmSplitPane.setDividerLocation(340);
        spectrumSelectionPsmSplitPane.setDividerSize(0);
        spectrumSelectionPsmSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spectrumSelectionPsmSplitPane.setResizeWeight(0.5);
        spectrumSelectionPsmSplitPane.setOpaque(false);

        spectrumSelectionJPanel.setOpaque(false);

        spectrumSelectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Selection"));
        spectrumSelectionPanel.setOpaque(false);
        spectrumSelectionPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumSelectionPanelMouseReleased(evt);
            }
        });
        spectrumSelectionPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                spectrumSelectionPanelMouseMoved(evt);
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
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                spectrumTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumTableMouseExited(evt);
            }
        });
        spectrumTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                spectrumTableMouseMoved(evt);
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
            .addGroup(spectrumSelectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1238, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumSelectionPanelLayout.setVerticalGroup(
            spectrumSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumSelectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                .addContainerGap())
        );

        spectrumSelectionLayeredPane.add(spectrumSelectionPanel);
        spectrumSelectionPanel.setBounds(0, 0, 1270, 320);

        spectrumSelectionOptionsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_gray.png"))); // NOI18N
        spectrumSelectionOptionsJButton.setToolTipText("Spectrum File Selection");
        spectrumSelectionOptionsJButton.setBorder(null);
        spectrumSelectionOptionsJButton.setBorderPainted(false);
        spectrumSelectionOptionsJButton.setContentAreaFilled(false);
        spectrumSelectionOptionsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_gray.png"))); // NOI18N
        spectrumSelectionOptionsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_black.png"))); // NOI18N
        spectrumSelectionOptionsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                spectrumSelectionOptionsJButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                spectrumSelectionOptionsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                spectrumSelectionOptionsJButtonMouseExited(evt);
            }
        });
        spectrumSelectionLayeredPane.add(spectrumSelectionOptionsJButton);
        spectrumSelectionOptionsJButton.setBounds(1230, 0, 10, 19);
        spectrumSelectionLayeredPane.setLayer(spectrumSelectionOptionsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        spectrumSelectionLayeredPane.add(spectrumSelectionHelpJButton);
        spectrumSelectionHelpJButton.setBounds(1250, 0, 10, 19);
        spectrumSelectionLayeredPane.setLayer(spectrumSelectionHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        spectrumSelectionLayeredPane.add(exportSpectrumSelectionJButton);
        exportSpectrumSelectionJButton.setBounds(1240, 0, 10, 19);
        spectrumSelectionLayeredPane.setLayer(exportSpectrumSelectionJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        spectrumSelectionLayeredPane.add(contextMenuSpectrumSelectionBackgroundPanel);
        contextMenuSpectrumSelectionBackgroundPanel.setBounds(1230, 0, 30, 19);
        spectrumSelectionLayeredPane.setLayer(contextMenuSpectrumSelectionBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout spectrumSelectionJPanelLayout = new javax.swing.GroupLayout(spectrumSelectionJPanel);
        spectrumSelectionJPanel.setLayout(spectrumSelectionJPanelLayout);
        spectrumSelectionJPanelLayout.setHorizontalGroup(
            spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumSelectionLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1284, Short.MAX_VALUE)
        );
        spectrumSelectionJPanelLayout.setVerticalGroup(
            spectrumSelectionJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumSelectionLayeredPane)
        );

        spectrumSelectionPsmSplitPane.setTopComponent(spectrumSelectionJPanel);

        psmAndSpectrumSplitPane.setBorder(null);
        psmAndSpectrumSplitPane.setDividerLocation(675);
        psmAndSpectrumSplitPane.setDividerSize(0);
        psmAndSpectrumSplitPane.setResizeWeight(0.5);
        psmAndSpectrumSplitPane.setOpaque(false);

        psmsJPanel.setOpaque(false);

        psmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide Spectrum Matches"));
        psmsPanel.setOpaque(false);

        peptideShakerJScrollPane.setOpaque(false);

        peptideShakerJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "ID", "Sequence", "Protein(s)", "Confidence", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Integer.class
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
        peptideShakerJTable.setFocusable(false);
        peptideShakerJTable.setOpaque(false);
        peptideShakerJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseExited(evt);
            }
        });
        peptideShakerJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptideShakerJTableMouseMoved(evt);
            }
        });
        peptideShakerJScrollPane.setViewportView(peptideShakerJTable);

        idResultsPanel.setOpaque(false);

        spectrumIdResultsLabel.setFont(spectrumIdResultsLabel.getFont().deriveFont((spectrumIdResultsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        spectrumIdResultsLabel.setText("Spectrum Identification Results");

        idResultsTableJScrollPane.setMinimumSize(new java.awt.Dimension(23, 87));
        idResultsTableJScrollPane.setOpaque(false);

        searchResultsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "SE", "Rnk", "Sequence", "Charge", "Confidence", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Integer.class
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
        searchResultsTable.setOpaque(false);
        searchResultsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        searchResultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                searchResultsTableMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                searchResultsTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchResultsTableMouseExited(evt);
            }
        });
        searchResultsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                searchResultsTableMouseMoved(evt);
            }
        });
        searchResultsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchResultsTableKeyReleased(evt);
            }
        });
        idResultsTableJScrollPane.setViewportView(searchResultsTable);

        javax.swing.GroupLayout idResultsPanelLayout = new javax.swing.GroupLayout(idResultsPanel);
        idResultsPanel.setLayout(idResultsPanelLayout);
        idResultsPanelLayout.setHorizontalGroup(
            idResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(idResultsTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 618, Short.MAX_VALUE)
            .addGroup(idResultsPanelLayout.createSequentialGroup()
                .addComponent(spectrumIdResultsLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        idResultsPanelLayout.setVerticalGroup(
            idResultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idResultsPanelLayout.createSequentialGroup()
                .addComponent(spectrumIdResultsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(idResultsTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
        );

        colorLegendLabel.setText(" ");

        javax.swing.GroupLayout psmsPanelLayout = new javax.swing.GroupLayout(psmsPanel);
        psmsPanel.setLayout(psmsPanelLayout);
        psmsPanelLayout.setHorizontalGroup(
            psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmsPanelLayout.createSequentialGroup()
                .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(psmsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(idResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(peptideShakerJScrollPane)))
                    .addGroup(psmsPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(colorLegendLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        psmsPanelLayout.setVerticalGroup(
            psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideShakerJScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(idResultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(colorLegendLabel)
                .addGap(6, 6, 6))
        );

        psmsLayeredPane.add(psmsPanel);
        psmsPanel.setBounds(0, 0, 650, 400);

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
        psmsLayeredPane.add(psmsHelpJButton);
        psmsHelpJButton.setBounds(630, 0, 10, 19);
        psmsLayeredPane.setLayer(psmsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        psmsLayeredPane.add(exportPsmsJButton);
        exportPsmsJButton.setBounds(620, 0, 10, 19);
        psmsLayeredPane.setLayer(exportPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        psmsLayeredPane.add(contextMenuPsmsBackgroundPanel);
        contextMenuPsmsBackgroundPanel.setBounds(610, 0, 30, 19);
        psmsLayeredPane.setLayer(contextMenuPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout psmsJPanelLayout = new javax.swing.GroupLayout(psmsJPanel);
        psmsJPanel.setLayout(psmsJPanelLayout);
        psmsJPanelLayout.setHorizontalGroup(
            psmsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmsJPanelLayout.createSequentialGroup()
                .addComponent(psmsLayeredPane)
                .addContainerGap())
        );
        psmsJPanelLayout.setVerticalGroup(
            psmsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
        );

        psmAndSpectrumSplitPane.setLeftComponent(psmsJPanel);

        spectrumJPanel.setOpaque(false);
        spectrumJPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                spectrumJPanelMouseWheelMoved(evt);
            }
        });

        spectrumPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum & Fragment Ions"));
        spectrumPanel.setMinimumSize(new java.awt.Dimension(200, 200));
        spectrumPanel.setOpaque(false);

        slidersSplitPane.setBorder(null);
        slidersSplitPane.setDividerLocation(558);
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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(intensitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(accuracySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );
        slidersPanelLayout.setVerticalGroup(
            slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, slidersPanelLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                .addGap(30, 30, 30)
                .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
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
                .addComponent(spectrumChartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
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
                .addComponent(slidersSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumPanelLayout.setVerticalGroup(
            spectrumPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumPanelLayout.createSequentialGroup()
                .addComponent(slidersSplitPane)
                .addContainerGap())
        );

        spectrumLayeredPane.add(spectrumPanel);
        spectrumPanel.setBounds(0, 0, 590, 420);

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
        spectrumLayeredPane.add(spectrumHelpJButton);
        spectrumHelpJButton.setBounds(600, 0, 10, 19);
        spectrumLayeredPane.setLayer(spectrumHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportSpectrumJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumJButton.setToolTipText("Export");
        exportSpectrumJButton.setBorder(null);
        exportSpectrumJButton.setBorderPainted(false);
        exportSpectrumJButton.setContentAreaFilled(false);
        exportSpectrumJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSpectrumJButton.setEnabled(false);
        exportSpectrumJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportSpectrumJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportSpectrumJButtonMouseExited(evt);
            }
        });
        spectrumLayeredPane.add(exportSpectrumJButton);
        exportSpectrumJButton.setBounds(590, 0, 10, 19);
        spectrumLayeredPane.setLayer(exportSpectrumJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        spectrumLayeredPane.add(contextMenuSpectrumBackgroundPanel);
        contextMenuSpectrumBackgroundPanel.setBounds(590, 0, 30, 19);
        spectrumLayeredPane.setLayer(contextMenuSpectrumBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumLayeredPane)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
        );

        psmAndSpectrumSplitPane.setRightComponent(spectrumJPanel);

        spectrumSelectionPsmSplitPane.setBottomComponent(psmAndSpectrumSplitPane);

        idSoftwareJPanel.setOpaque(false);

        idSoftwarePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Identification Overview"));
        idSoftwarePanel.setOpaque(false);

        overviewPlotsPanel.setOpaque(false);
        overviewPlotsPanel.setLayout(new javax.swing.BoxLayout(overviewPlotsPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout idSoftwarePanelLayout = new javax.swing.GroupLayout(idSoftwarePanel);
        idSoftwarePanel.setLayout(idSoftwarePanelLayout);
        idSoftwarePanelLayout.setHorizontalGroup(
            idSoftwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSoftwarePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewPlotsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1248, Short.MAX_VALUE)
                .addContainerGap())
        );
        idSoftwarePanelLayout.setVerticalGroup(
            idSoftwarePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSoftwarePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewPlotsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                .addContainerGap())
        );

        idSoftwareJLayeredPane.add(idSoftwarePanel);
        idSoftwarePanel.setBounds(0, 10, 1280, 150);

        idSoftwareHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        idSoftwareHelpJButton.setToolTipText("Help");
        idSoftwareHelpJButton.setBorder(null);
        idSoftwareHelpJButton.setBorderPainted(false);
        idSoftwareHelpJButton.setContentAreaFilled(false);
        idSoftwareHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        idSoftwareHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                idSoftwareHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                idSoftwareHelpJButtonMouseExited(evt);
            }
        });
        idSoftwareHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idSoftwareHelpJButtonActionPerformed(evt);
            }
        });
        idSoftwareJLayeredPane.add(idSoftwareHelpJButton);
        idSoftwareHelpJButton.setBounds(1270, 0, 10, 19);
        idSoftwareJLayeredPane.setLayer(idSoftwareHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportIdPerformancePerformanceJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportIdPerformancePerformanceJButton.setToolTipText("Copy to File");
        exportIdPerformancePerformanceJButton.setBorder(null);
        exportIdPerformancePerformanceJButton.setBorderPainted(false);
        exportIdPerformancePerformanceJButton.setContentAreaFilled(false);
        exportIdPerformancePerformanceJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportIdPerformancePerformanceJButton.setEnabled(false);
        exportIdPerformancePerformanceJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportIdPerformancePerformanceJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportIdPerformancePerformanceJButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportIdPerformancePerformanceJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportIdPerformancePerformanceJButtonMouseExited(evt);
            }
        });
        idSoftwareJLayeredPane.add(exportIdPerformancePerformanceJButton);
        exportIdPerformancePerformanceJButton.setBounds(1260, 0, 10, 19);
        idSoftwareJLayeredPane.setLayer(exportIdPerformancePerformanceJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuIdSoftwareBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuIdSoftwareBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuIdSoftwareBackgroundPanel);
        contextMenuIdSoftwareBackgroundPanel.setLayout(contextMenuIdSoftwareBackgroundPanelLayout);
        contextMenuIdSoftwareBackgroundPanelLayout.setHorizontalGroup(
            contextMenuIdSoftwareBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuIdSoftwareBackgroundPanelLayout.setVerticalGroup(
            contextMenuIdSoftwareBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        idSoftwareJLayeredPane.add(contextMenuIdSoftwareBackgroundPanel);
        contextMenuIdSoftwareBackgroundPanel.setBounds(1260, 0, 30, 19);
        idSoftwareJLayeredPane.setLayer(contextMenuIdSoftwareBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout idSoftwareJPanelLayout = new javax.swing.GroupLayout(idSoftwareJPanel);
        idSoftwareJPanel.setLayout(idSoftwareJPanelLayout);
        idSoftwareJPanelLayout.setHorizontalGroup(
            idSoftwareJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(idSoftwareJLayeredPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1284, Short.MAX_VALUE)
        );
        idSoftwareJPanelLayout.setVerticalGroup(
            idSoftwareJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(idSoftwareJLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1304, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(idSoftwareJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spectrumSelectionPsmSplitPane)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 938, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(761, Short.MAX_VALUE)
                    .addComponent(idSoftwareJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spectrumSelectionPsmSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 747, Short.MAX_VALUE)
                    .addGap(180, 180, 180)))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Reset the divider between the spectrum table and the spectrum.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        spectrumSelectionPsmSplitPane.setDividerLocation((spectrumSelectionPsmSplitPane.getHeight() / 100) * 34);
        psmAndSpectrumSplitPane.setDividerLocation(psmAndSpectrumSplitPane.getWidth() / 2);

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

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
                idSoftwareJLayeredPane.getComponent(0).setBounds(
                        idSoftwareJLayeredPane.getWidth() - idSoftwareJLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        idSoftwareJLayeredPane.getComponent(0).getWidth(),
                        idSoftwareJLayeredPane.getComponent(0).getHeight());

                idSoftwareJLayeredPane.getComponent(1).setBounds(
                        idSoftwareJLayeredPane.getWidth() - idSoftwareJLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        idSoftwareJLayeredPane.getComponent(1).getWidth(),
                        idSoftwareJLayeredPane.getComponent(1).getHeight());

                idSoftwareJLayeredPane.getComponent(2).setBounds(
                        idSoftwareJLayeredPane.getWidth() - idSoftwareJLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        idSoftwareJLayeredPane.getComponent(2).getWidth(),
                        idSoftwareJLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                idSoftwareJLayeredPane.getComponent(3).setBounds(0, 0, idSoftwareJLayeredPane.getWidth(), idSoftwareJLayeredPane.getHeight());
                idSoftwareJLayeredPane.revalidate();
                idSoftwareJLayeredPane.repaint();

                // move the icons
                spectrumSelectionLayeredPane.getComponent(0).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(0).getWidth() - 34,
                        0,
                        spectrumSelectionLayeredPane.getComponent(0).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(0).getHeight());

                spectrumSelectionLayeredPane.getComponent(1).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(1).getWidth() - 10,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(1).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(1).getHeight());

                spectrumSelectionLayeredPane.getComponent(2).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(2).getWidth() - 20,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(2).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(2).getHeight());

                spectrumSelectionLayeredPane.getComponent(3).setBounds(
                        spectrumSelectionLayeredPane.getWidth() - spectrumSelectionLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        spectrumSelectionLayeredPane.getComponent(3).getWidth(),
                        spectrumSelectionLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                spectrumSelectionLayeredPane.getComponent(4).setBounds(0, 0, spectrumSelectionLayeredPane.getWidth(), spectrumSelectionLayeredPane.getHeight());
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
     * Update the id results PSM selection.
     *
     * @param evt
     */
    private void searchResultsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchResultsTableKeyReleased
        searchResultsTableMouseClicked(null);
    }//GEN-LAST:event_searchResultsTableKeyReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void searchResultsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsTableMouseClicked
        if (searchResultsTable.getSelectedRow() != -1) {
            updateSpectrum();
        }
    }//GEN-LAST:event_searchResultsTableMouseClicked

    /**
     * Update the id results PSM selection.
     *
     * @param evt
     */
    private void searchResultsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsTableMouseReleased
        if (searchResultsTable.getSelectedRow() != -1) {

            updateSpectrum();

            // open protein links in web browser
            if (evt != null) {
                int row = searchResultsTable.rowAtPoint(evt.getPoint());
                int column = searchResultsTable.columnAtPoint(evt.getPoint());

                if (column == 1) {

                    // open protein links in web browser
                    if (evt.getButton() == MouseEvent.BUTTON1
                            && ((String) searchResultsTable.getValueAt(row, column)).lastIndexOf("a href=") != -1) {
                        peptideShakerGUI.openProteinLinks((String) searchResultsTable.getValueAt(row, column));
                    }
                }
            }

            // update the annotation menu
            showSpectrumAnnotationMenu();
        }
    }//GEN-LAST:event_searchResultsTableMouseReleased

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
     * Opens the protein web links if the protein(s) column is selected.
     *
     * @param evt
     */
    private void peptideShakerJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideShakerJTableMouseReleased

        int row = peptideShakerJTable.rowAtPoint(evt.getPoint());
        int column = peptideShakerJTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == 3) {

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
    private void searchResultsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_searchResultsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void searchResultsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchResultsTableMouseMoved
        int row = searchResultsTable.rowAtPoint(evt.getPoint());
        int column = searchResultsTable.columnAtPoint(evt.getPoint());

        if (searchResultsTable.getValueAt(row, column) != null) {

            if (column == searchResultsTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                searchResultsTable.setToolTipText(searchResultsTablePeptideTooltips.get(row));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                searchResultsTable.setToolTipText(null);
            }
        } else {
            searchResultsTable.setToolTipText(null);
        }
    }//GEN-LAST:event_searchResultsTableMouseMoved

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
        peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences().setAnnotationLevel(((Integer) intensitySlider.getValue()) / 100.0);
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
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getIdentificationParameters().getSearchParameters().getFragmentIonAccuracy();
        peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences().setFragmentIonAccuracy(accuracy);
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#PSM",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
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
    private void idSoftwareHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_idSoftwareHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_idSoftwareHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void idSoftwareHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_idSoftwareHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_idSoftwareHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void idSoftwareHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idSoftwareHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#IdSoftwarePerformance",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_idSoftwareHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportIdPerformancePerformanceJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportIdPerformancePerformanceJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportIdPerformancePerformanceJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportIdPerformancePerformanceJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportIdPerformancePerformanceJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportIdPerformancePerformanceJButtonMouseExited

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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#SpectrumSelection",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PSMs.html"), "#Spectrum",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
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
    private void exportIdPerformancePerformanceJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportIdPerformancePerformanceJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Plots");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, overviewPlotsPanel, peptideShakerGUI.getLastSelectedFolder());
            }
        });

        popupMenu.add(menuItem);

        popupMenu.show(exportIdPerformancePerformanceJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportIdPerformancePerformanceJButtonMouseReleased

    /**
     * Export the spectrum.
     *
     * @param evt
     */
    private void exportSpectrumJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem;

        if (searchResultsTable.getSelectedRowCount() <= 2) {
            menuItem = new JMenuItem("Spectrum");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSpectrumAsFigure();
                }
            });

            popupMenu.add(menuItem);
        }

        if (searchResultsTable.getSelectedRowCount() == 1) {
            menuItem = new JMenuItem("Spectrum as MGF");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSpectrumAsMgf();
                }
            });

            popupMenu.add(menuItem);
        } else if (searchResultsTable.getSelectedRowCount() > 2) {
            menuItem = new JMenuItem("Bubble Plot");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportBubblePlotAsFigure();
                }
            });

            popupMenu.add(menuItem);
        }

        popupMenu.show(exportSpectrumJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportSpectrumJButtonMouseReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void spectrumTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_spectrumTableKeyReleased
        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableKeyReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void spectrumTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseMoved

        int row = spectrumTable.rowAtPoint(evt.getPoint());
        int column = spectrumTable.columnAtPoint(evt.getPoint());

        if (spectrumTable.getValueAt(row, column) != null) {
            if (column == spectrumTable.getColumn("Protein(s)").getModelIndex()) {

                String tempValue = (String) spectrumTable.getValueAt(row, column);

                if (tempValue.lastIndexOf("a href=") != -1) {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                spectrumTable.setToolTipText(null);

            } else if (column == spectrumTable.getColumn("Sequence").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                String spectrumKey = Spectrum.getSpectrumKey(fileSelected, spectrumFactory.getSpectrumTitles(fileSelected).get(spectrumTable.convertRowIndexToModel(row)));

                // check if we ought to show a tooltip with mod details
                if (identification.matchExists(spectrumKey)) {
                    try {
                        DisplayFeaturesGenerator displayFeaturesGenerator = peptideShakerGUI.getDisplayFeaturesGenerator();
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            String tooltip = displayFeaturesGenerator.getPeptideModificationTooltipAsHtml(spectrumMatch);
                            spectrumTable.setToolTipText(tooltip);
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                            spectrumTable.setToolTipText(peptideShakerGUI.getDisplayFeaturesGenerator().getTagModificationTooltipAsHtml(tagAssumption.getTag()));
                        } else {
                            throw new IllegalArgumentException("No best match found for spectrum " + spectrumMatch.getKey() + ".");
                        }
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                }
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                spectrumTable.setToolTipText(null);
            }
        }
    }//GEN-LAST:event_spectrumTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumTableMouseExited

    /**
     * Show the statistics popup menu.
     *
     * @param evt
     */
    private void spectrumTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3 && spectrumTable.getRowCount() > 0) {

            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Statistics (beta)");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    new XYPlottingDialog(peptideShakerGUI, spectrumTable, spectrumTableToolTips,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true);
                }
            });
            popupMenu.add(menuItem);
            popupMenu.show(spectrumTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_spectrumTableMouseClicked

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void spectrumTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumTableMouseReleased

        int row = spectrumTable.getSelectedRow();
        int column = spectrumTable.getSelectedColumn();

        if (evt.getButton() == MouseEvent.BUTTON1 && row != -1 && column != -1) {

            // open protein link in web browser
            if (column == spectrumTable.getColumn("Protein(s)").getModelIndex()
                    && evt.getButton() == MouseEvent.BUTTON1
                    && spectrumTable.getValueAt(row, column) != null
                    && ((String) spectrumTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {
                peptideShakerGUI.openProteinLinks((String) spectrumTable.getValueAt(row, column));
            }
        }

        spectrumSelectionChanged();
    }//GEN-LAST:event_spectrumTableMouseReleased

    /**
     * Updates the spectrum table based on the currently selected mgf file.
     *
     * @param evt
     */
    private void fileNamesCmbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileNamesCmbActionPerformed

        spectrumSelectionDialog.setVisible(false);

        if (updateSelection) {
            clearItemSelection();
            fileSelectionChanged();
        }
    }//GEN-LAST:event_fileNamesCmbActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void spectrumSelectionOptionsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionOptionsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_spectrumSelectionOptionsJButtonMouseEntered

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void spectrumSelectionOptionsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionOptionsJButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumSelectionOptionsJButtonMouseExited

    /**
     * Open the spectrum file selection drop down menu.
     *
     * @param evt
     */
    private void spectrumSelectionOptionsJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionOptionsJButtonMouseReleased
        spectrumSelectionDialog.setSize(400, 65);
        spectrumSelectionDialog.setLocationRelativeTo(spectrumSelectionOptionsJButton);
        spectrumSelectionDialog.setVisible(true);
    }//GEN-LAST:event_spectrumSelectionOptionsJButtonMouseReleased

    private void spectrumSelectionPanelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionPanelMouseReleased
        if (evt.getX() > 180 && evt.getX() < 400 && evt.getY() < 25) {
            spectrumSelectionDialog.setSize(400, 65);
            spectrumSelectionDialog.setLocation(evt.getLocationOnScreen());
            spectrumSelectionDialog.setVisible(true);
        }
    }//GEN-LAST:event_spectrumSelectionPanelMouseReleased

    /**
     * Shows a tooltip when the mouse is over the spectrum selection link.
     *
     * @param evt
     */
    private void spectrumSelectionPanelMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumSelectionPanelMouseMoved
        if (evt.getX() > 180 && evt.getX() < 400 && evt.getY() < 25) {
            spectrumSelectionPanel.setToolTipText("Select Spectrum File");
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            spectrumSelectionPanel.setToolTipText(null);
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_spectrumSelectionPanelMouseMoved

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider accuracySlider;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel colorLegendLabel;
    private javax.swing.JPanel contextMenuIdSoftwareBackgroundPanel;
    private javax.swing.JPanel contextMenuPsmsBackgroundPanel;
    private javax.swing.JPanel contextMenuSpectrumBackgroundPanel;
    private javax.swing.JPanel contextMenuSpectrumSelectionBackgroundPanel;
    private javax.swing.JButton exportIdPerformancePerformanceJButton;
    private javax.swing.JButton exportPsmsJButton;
    private javax.swing.JButton exportSpectrumJButton;
    private javax.swing.JButton exportSpectrumSelectionJButton;
    private javax.swing.JComboBox fileNamesCmb;
    private javax.swing.JPanel idResultsPanel;
    private javax.swing.JScrollPane idResultsTableJScrollPane;
    private javax.swing.JButton idSoftwareHelpJButton;
    private javax.swing.JLayeredPane idSoftwareJLayeredPane;
    private javax.swing.JPanel idSoftwareJPanel;
    private javax.swing.JPanel idSoftwarePanel;
    private javax.swing.JSlider intensitySlider;
    private javax.swing.JPanel overviewPlotsPanel;
    private javax.swing.JScrollPane peptideShakerJScrollPane;
    private javax.swing.JTable peptideShakerJTable;
    private javax.swing.JSplitPane psmAndSpectrumSplitPane;
    private javax.swing.JButton psmsHelpJButton;
    private javax.swing.JPanel psmsJPanel;
    private javax.swing.JLayeredPane psmsLayeredPane;
    private javax.swing.JPanel psmsPanel;
    private javax.swing.JTable searchResultsTable;
    private javax.swing.JPanel slidersPanel;
    private javax.swing.JSplitPane slidersSplitPane;
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
    private javax.swing.JPanel spectrumChartPanel;
    private javax.swing.JButton spectrumHelpJButton;
    private javax.swing.JLabel spectrumIdResultsLabel;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JPanel spectrumJPanel1;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JLayeredPane spectrumLayeredPane;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JDialog spectrumSelectionDialog;
    private javax.swing.JButton spectrumSelectionHelpJButton;
    private javax.swing.JPanel spectrumSelectionJPanel;
    private javax.swing.JLayeredPane spectrumSelectionLayeredPane;
    private javax.swing.JButton spectrumSelectionOptionsJButton;
    private javax.swing.JPanel spectrumSelectionPanel;
    private javax.swing.JSplitPane spectrumSelectionPsmSplitPane;
    private javax.swing.JTable spectrumTable;
    private javax.swing.JScrollPane spectrumTableJScrollPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays the results in the panel.
     */
    public void displayResults() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Data. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {
            @Override
            public void run() {

                try {
                    identification = peptideShakerGUI.getIdentification();

                    spectrumTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                            (double) ((PSMaps) identification.getUrParam(new PSMaps())).getPsmSpecificMap().getMaxCharge(), peptideShakerGUI.getSparklineColor()));
                    spectrumTable.getColumn("Int").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                            SpectrumFactory.getInstance().getMaxIntensity(), peptideShakerGUI.getSparklineColor()));
                    spectrumTable.getColumn("RT (min)").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, SpectrumFactory.getInstance().getMinRT(),
                            (SpectrumFactory.getInstance().getMaxRT() / 60), (SpectrumFactory.getInstance().getMaxRT() / 60) / 50, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);
                    if (SpectrumFactory.getInstance().getMaxIntensity() > 100000) {
                        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 20, new DecimalFormat("0.00E00"));
                    } else {
                        ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 20);
                    }
                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("Int").getCellRenderer()).setLogScale(true);
                    ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT (min)").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
                    ((JSparklinesIntervalChartTableCellRenderer) spectrumTable.getColumn("RT (min)").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);

                    PSMaps pSMaps = new PSMaps();
                    pSMaps = (PSMaps) identification.getUrParam(pSMaps);
                    eu.isas.peptideshaker.scoring.InputMap inputMap = pSMaps.getInputMap();
                    if (inputMap == null || !inputMap.hasAdvocateContribution()) {
                        // Backward compatibility
                        loadDataFromIdentification();
                    } else {
                        advocatesUsed = new ArrayList<Integer>(inputMap.getInputAlgorithmsSorted());
                        ArrayList<String> spectrumFileNames = identification.getSpectrumFiles();
                        numberOfValidatedPsmsMap = new HashMap<String, Integer>();
                        for (String fileName : spectrumFileNames) {
                            numberOfValidatedPsmsMap.put(fileName, inputMap.getAdvocateContribution(Advocate.peptideShaker.getIndex(), fileName));
                        }
                        updateOverviewPlots(inputMap, pSMaps.getPsmSpecificMap());
                    }

                    // update the advocates color legend
                    ArrayList<Integer> usedAdvocatedAndPeptideShaker = new ArrayList<Integer>();
                    usedAdvocatedAndPeptideShaker.addAll(advocatesUsed);
                    if (!usedAdvocatedAndPeptideShaker.contains(Advocate.peptideShaker.getIndex())) {
                        usedAdvocatedAndPeptideShaker.add(Advocate.peptideShaker.getIndex());
                    }
                    String colorLegend = "<html>";
                    for (int tempAdvocate : usedAdvocatedAndPeptideShaker) {
                        colorLegend += "<font color=\"rgb(" + Advocate.getAdvocateColorMap().get(tempAdvocate).getRed() + ","
                                + Advocate.getAdvocateColorMap().get(tempAdvocate).getGreen() + ","
                                + Advocate.getAdvocateColorMap().get(tempAdvocate).getBlue() + ")\">&#9632;</font> "
                                + Advocate.getAdvocate(tempAdvocate).getName() + " &nbsp;";
                    }
                    colorLegend += "</html>";
                    colorLegendLabel.setText(colorLegend);

                    showSparkLines(peptideShakerGUI.showSparklines());
                    progressDialog.setTitle("Updating Spectrum Table. Please Wait...");
                    ArrayList<String> spectrumFileNames = identification.getSpectrumFiles();
                    String[] filesArray = new String[spectrumFileNames.size()];
                    int cpt = 0;

                    for (String tempName : spectrumFileNames) {
                        filesArray[cpt++] = tempName;
                    }
                    fileNamesCmb.setModel(new DefaultComboBoxModel(filesArray));

                    // update the slider tooltips
                    double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getIdentificationParameters().getSearchParameters().getFragmentIonAccuracy();
                    accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " Da");
                    intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");

                    //formComponentResized(null);
                    // enable the contextual export options
                    exportIdPerformancePerformanceJButton.setEnabled(true);
                    exportSpectrumSelectionJButton.setEnabled(true);
                    exportSpectrumJButton.setEnabled(true);
                    exportPsmsJButton.setEnabled(true);

                    peptideShakerGUI.setUpdated(PeptideShakerGUI.SPECTRUM_ID_TAB_INDEX, true);

                    boolean processCancelled = progressDialog.isRunCanceled();

                    progressDialog.setRunFinished();

                    if (!processCancelled) {
                        fileSelectionChanged();
                    }

                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    peptideShakerGUI.catchException(e);
                }
            }
        }.start();
    }

    /**
     * Loads the tab content from the identification.
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     */
    public void loadDataFromIdentification() throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        progressDialog.setTitle("Updating Project Data. Please Wait...");

        // see if the id software is saved
        boolean newInformation = true;
        if (peptideShakerGUI.getProjectDetails().hasIdentificationAlgorithms()) {
            newInformation = false;
        }

        // get the list of id software used
        advocatesUsed = peptideShakerGUI.getProjectDetails().getIdentificationAlgorithms();

        // set the dataset to not saved
        if (newInformation) {
            peptideShakerGUI.setDataSaved(false);
        }

        // order the advocates to have the same order is in the overview plots
        Collections.sort(advocatesUsed);

        HashMap<Integer, Double> totalAdvocateId = new HashMap<Integer, Double>();
        HashMap<Integer, Double> uniqueAdvocateId = new HashMap<Integer, Double>();

        for (int tempAdvocate : advocatesUsed) {
            totalAdvocateId.put(tempAdvocate, 0.0);
            uniqueAdvocateId.put(tempAdvocate, 0.0);
        }

        int totalNumberOfSpectra = 0, totalPeptideShakerIds = 0;

        int fileCounter = 1;
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        numberOfValidatedPsmsMap = new HashMap<String, Integer>();

        progressDialog.setSecondaryProgressCounterIndeterminate(false);
        progressDialog.setMaxSecondaryProgressCounter(identification.getSpectrumIdentificationSize());
        progressDialog.setValue(0);

        ArrayList<String> spectrumFiles = identification.getSpectrumFiles();
        for (String fileName : spectrumFiles) {

            int numberOfValidatedPsms = 0;
            totalNumberOfSpectra += spectrumFactory.getNSpectra(fileName);

            progressDialog.setTitle("Loading Data. Please Wait... (" + fileCounter++ + "/" + spectrumFiles.size() + ") ");
            PsmIterator psmIterator = identification.getPsmIterator(fileName, parameters, true);

            while (psmIterator.hasNext()) {
                if (progressDialog.isRunCanceled()) {
                    break;
                }

                SpectrumMatch spectrumMatch = psmIterator.next();
                String spectrumKey = spectrumMatch.getKey();

                if (spectrumMatch.getBestPeptideAssumption() != null) {

                    ArrayList<Integer> currentAdvocates = new ArrayList<Integer>();
                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                    if (psParameter.getMatchValidationLevel().isValidated()) {

                        totalPeptideShakerIds++;
                        numberOfValidatedPsms++;

                        HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);
                        for (Integer tempAdvocate : advocatesUsed) {
                            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(tempAdvocate);
                            if (advocateAssumptions != null) {
                                ArrayList<Double> eValues = new ArrayList<Double>(advocateAssumptions.keySet());
                                Collections.sort(eValues);
                                for (SpectrumIdentificationAssumption firstHit : advocateAssumptions.get(eValues.get(0))) {
                                    if ((firstHit instanceof PeptideAssumption) && ((PeptideAssumption) firstHit).getPeptide().isSameSequenceAndModificationStatus(spectrumMatch.getBestPeptideAssumption().getPeptide(),
                                            peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences())) {
                                        currentAdvocates.add(tempAdvocate);
                                    }
                                }
                            }
                        }
                    }

                    // overview plot data
                    for (Integer tempAdvocate : advocatesUsed) {
                        if (currentAdvocates.contains(tempAdvocate)) {
                            totalAdvocateId.put(tempAdvocate, totalAdvocateId.get(tempAdvocate) + 1);

                            if (currentAdvocates.size() == 1) {
                                uniqueAdvocateId.put(tempAdvocate, uniqueAdvocateId.get(tempAdvocate) + 1);
                            }
                        }
                    }

                    progressDialog.increaseSecondaryProgressCounter();
                }
            }

            numberOfValidatedPsmsMap.put(fileName, numberOfValidatedPsms);
        }

        if (!progressDialog.isRunCanceled()) {

            progressDialog.setSecondaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Updating Tables. Please Wait...");

            // add the peptide shaker results
            totalAdvocateId.put(Advocate.peptideShaker.getIndex(), (double) totalPeptideShakerIds);
            uniqueAdvocateId.put(Advocate.peptideShaker.getIndex(), 0.0);

            // update the id software performance plots
            updateOverviewPlots(totalAdvocateId, uniqueAdvocateId, totalNumberOfSpectra);
        }
    }

    /**
     * Method called whenever the file selection changed.
     */
    private void fileSelectionChanged() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Updating Spectrum Table. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                DefaultTableModel dm = (DefaultTableModel) spectrumTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                fileSelected = (String) fileNamesCmb.getSelectedItem();
                double maxMz = spectrumFactory.getMaxMz(fileSelected);
                try {
                    progressDialog.setTitle("Loading Spectrum Information for " + fileSelected + ". Please Wait..."); // @TODO: problem with progress bar??
                    identification.loadSpectrumMatchParameters(fileSelected, new PSParameter(), progressDialog);
                    identification.loadSpectrumMatches(fileSelected, progressDialog);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
                progressDialog.setPrimaryProgressCounterIndeterminate(true);
                if (!progressDialog.isRunCanceled()) {
                    ((TitledBorder) spectrumSelectionPanel.getBorder()).setTitle("<html>" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING_HTML + "Spectrum Selection ("
                            + numberOfValidatedPsmsMap.get(fileSelected) + "/" + spectrumFactory.getNSpectra(fileSelected) + " - "
                            + "<a href=\"dummy\">" + fileSelected + "</a>)"
                            + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING_HTML + "</html>");
                    spectrumSelectionPanel.repaint();

                    ((JSparklinesBarChartTableCellRenderer) spectrumTable.getColumn("m/z").getCellRenderer()).setMaxValue(maxMz);

                    updateSelection();
                    peptideShakerGUI.mgfFileSelectionChanged(fileSelected);
                    //repaint();
                    spectrumTable.requestFocus();
                }

                progressDialog.setRunFinished();
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
     * Updates the spectrum selected according to the user's last selection.
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
     * Provides to the PeptideShakerGUI instance the currently selected PSM.
     */
    private void newItemSelection() {
        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION, getSelectedSpectrumKey());
    }

    /**
     * Clears the currently selected PSM.
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
     * Method called whenever the spectrum selection changed.
     */
    private void spectrumSelectionChanged() {

        if (spectrumTable.getSelectedRow() != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            try {
                // empty the tables
                DefaultTableModel dm = (DefaultTableModel) peptideShakerJTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                dm = (DefaultTableModel) searchResultsTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                searchResultsTablePeptideTooltips = new ArrayList<String>();
                String key = getSelectedSpectrumKey();

                if (identification.matchExists(key)) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(key);
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getSpectrumMatchParameter(key, probabilities);

                    // fill peptideshaker table
                    DisplayFeaturesGenerator displayFeaturesGenerator = peptideShakerGUI.getDisplayFeaturesGenerator();
                    String proteins = "";
                    String sequence;
                    if (spectrumMatch.getBestPeptideAssumption() != null) {
                        proteins = displayFeaturesGenerator.addDatabaseLinks(spectrumMatch.getBestPeptideAssumption().getPeptide().getParentProteins(peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences()));
                        sequence = displayFeaturesGenerator.getTaggedPeptideSequence(spectrumMatch, true, true, true);
                        peptideShakerJTablePeptideTooltip = displayFeaturesGenerator.getPeptideModificationTooltipAsHtml(spectrumMatch);
                    } else if (spectrumMatch.getBestTagAssumption() != null) {
                        sequence = spectrumMatch.getBestTagAssumption().getTag().getTaggedModifiedSequence(peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationProfile(), true, true, true, false, false);
                        peptideShakerJTablePeptideTooltip = displayFeaturesGenerator.getTagModificationTooltipAsHtml(spectrumMatch.getBestTagAssumption().getTag());
                    } else {
                        throw new IllegalArgumentException("No best hit found for spectrum " + spectrumMatch.getKey());
                    }

                    HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(key);

                    ((DefaultTableModel) peptideShakerJTable.getModel()).addRow(new Object[]{
                        1,
                        isBestPsmEqualForAllIdSoftware(spectrumMatch, assumptions, peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences()),
                        sequence,
                        proteins,
                        probabilities.getPsmConfidence(),
                        probabilities.getMatchValidationLevel().getIndex()
                    });

                    currentAssumptionsList = new ArrayList<SpectrumIdentificationAssumption>();

                    // add the search results
                    for (Integer tempAdvocate : advocatesUsed) {
                        HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(tempAdvocate);
                        if (advocateAssumptions != null) {
                            ArrayList<Double> eValues = new ArrayList<Double>(advocateAssumptions.keySet());
                            Collections.sort(eValues);
                            for (double eValue : eValues) {
                                for (SpectrumIdentificationAssumption currentAssumption : advocateAssumptions.get(eValue)) {
                                    addIdResultsToTable(currentAssumption, probabilities, tempAdvocate);
                                }
                            }
                        }
                    }

                    // correct table index column
                    for (int i = 0; i < searchResultsTable.getRowCount(); i++) {
                        ((DefaultTableModel) searchResultsTable.getModel()).setValueAt(i + 1, i, 0);
                    }

                    ((DefaultTableModel) peptideShakerJTable.getModel()).fireTableDataChanged();
                    ((DefaultTableModel) searchResultsTable.getModel()).fireTableDataChanged();

                    // select one of the matches
                    if (searchResultsTable.getRowCount() > 0) {
                        searchResultsTable.setRowSelectionInterval(0, 0);
                    }

                    peptideShakerJTable.revalidate();
                    peptideShakerJTable.repaint();
                    searchResultsTable.revalidate();
                    searchResultsTable.repaint();
                }

                newItemSelection();

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // invoke later to give time for components to update
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        //update the spectrum
                        updateSpectrum();
                    }
                });

            } catch (Exception e) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Update the spectrum based on the currently selected PSM.
     */
    public void updateSpectrum() {

        if (spectrumTable.getSelectedRow() != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            try {
                spectrumChartPanel.removeAll();

                String spectrumkey = getSelectedSpectrumKey();
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumkey);
                AnnotationPreferences annotationPreferences = peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences();

                // get the selected spectrum
                if (currentSpectrum != null && currentSpectrum.getMzValuesAsArray().length > 0) {

                    Precursor precursor = currentSpectrum.getPrecursor();
                    String charge;

                    if (identification.matchExists(currentSpectrumKey)) {
                        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(currentSpectrumKey);
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().toString();
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            charge = spectrumMatch.getBestTagAssumption().getIdentificationCharge().toString();
                        } else {
                            throw new IllegalArgumentException("Best hit not found for spectrum " + spectrumkey + ".");
                        }
                    } else {
                        charge = precursor.getPossibleChargesAsString();
                    }

                    double[] intensitiesAsArray = currentSpectrum.getIntensityValuesAsArray();

                    if (searchResultsTable.getSelectedRowCount() == 2) {
                        intensitiesAsArray = currentSpectrum.getIntensityValuesNormalizedAsArray();
                    }

                    spectrum = new SpectrumPanel(
                            currentSpectrum.getMzValuesAsArray(), intensitiesAsArray,
                            precursor.getMz(), charge,
                            "", 40, false, false, false, 2, false);
                    spectrum.setKnownMassDeltas(peptideShakerGUI.getCurrentMassDeltas());
                    spectrum.setDeltaMassWindow(annotationPreferences.getFragmentIonAccuracy());
                    spectrum.setBorder(null);
                    spectrum.setDataPointAndLineColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakColor(), 0);
                    spectrum.setPeakWaterMarkColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakColor());
                    spectrum.setPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakWidth());
                    spectrum.setBackgroundPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakWidth());

                    // add the mirrored spectrum
                    if (searchResultsTable.getSelectedRowCount() == 2) {
                        spectrum.addMirroredSpectrum(
                                currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesNormalizedAsArray(), precursor.getMz(),
                                charge, "", false,
                                peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedMirroredPeakColor(),
                                peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedMirroredPeakColor());
                    }
                }

                // add spectrum annotations
                if (identification.matchExists(spectrumkey)) {

                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumkey);
                    SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
                    int forwardIon = searchParameters.getIonSearched1();
                    int rewindIon = searchParameters.getIonSearched2();
                    ModificationProfile modificationProfile = searchParameters.getModificationProfile();

                    if (currentSpectrum != null && spectrum != null) {

                        if (currentSpectrum.getMzValuesAsArray().length > 0 && currentSpectrum.getIntensityValuesAsArray().length > 0) {

                            int maxPrecursorCharge = 1;
                            String modifiedSequence = "";
                            ArrayList<ModificationMatch> allModifications = new ArrayList<ModificationMatch>();
                            ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<ArrayList<IonMatch>>();
                            ArrayList<MSnSpectrum> allSpectra = new ArrayList<MSnSpectrum>();
                            ArrayList<String> selectedIndexes = new ArrayList<String>();

                            for (int i = 0; i < searchResultsTable.getSelectedRowCount(); i++) {

                                SpectrumIdentificationAssumption currentAssumption = currentAssumptionsList.get(searchResultsTable.getSelectedRows()[i]);
                                selectedIndexes.add((i + 1) + " " + currentAssumption.getIdentificationCharge().toString());

                                if (currentAssumption != null) {
                                    currentSpectrumKey = spectrumMatch.getKey();
                                    if (currentAssumption instanceof PeptideAssumption) {
                                        PeptideAssumption currentPeptideAssumption = (PeptideAssumption) currentAssumption;
                                        Peptide peptide = currentPeptideAssumption.getPeptide();
                                        annotationPreferences.setCurrentSettings(currentPeptideAssumption, !currentSpectrumKey.equalsIgnoreCase(spectrumMatch.getKey()),
                                                peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());
                                        ArrayList<IonMatch> annotations = specificAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                                annotationPreferences.getNeutralLosses(),
                                                annotationPreferences.getValidatedCharges(),
                                                currentPeptideAssumption.getIdentificationCharge().value,
                                                currentSpectrum, peptide,
                                                currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                                annotationPreferences.getFragmentIonAccuracy(), false, annotationPreferences.isHighResolutionAnnotation());

                                        allAnnotations.add(annotations);
                                        allSpectra.add(currentSpectrum);

                                        // add the spectrum annotations
                                        if (i == 0) {
                                            spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));

                                            // add de novo sequencing
                                            spectrum.addAutomaticDeNovoSequencing(peptide, annotations,
                                                    forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                                                    annotationPreferences.showForwardIonDeNovoTags(),
                                                    annotationPreferences.showRewindIonDeNovoTags(), false);
                                        } else {
                                            spectrum.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(annotations));

                                            // add de novo sequencing
                                            spectrum.addAutomaticDeNovoSequencing(peptide, annotations,
                                                    forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                                                    annotationPreferences.showForwardIonDeNovoTags(),
                                                    annotationPreferences.showRewindIonDeNovoTags(), true);
                                        }

                                        if (currentPeptideAssumption.getIdentificationCharge().value > maxPrecursorCharge) {
                                            maxPrecursorCharge = currentPeptideAssumption.getIdentificationCharge().value;
                                        }

                                        if (!modifiedSequence.isEmpty()) {
                                            modifiedSequence += " vs. ";
                                        }

                                        modifiedSequence += peptide.getTaggedModifiedSequence(modificationProfile, false, false, true);

                                        allModifications.addAll(peptide.getModificationMatches());
                                    } else if (currentAssumption instanceof TagAssumption) {
                                        TagAssumption tagAssumption = (TagAssumption) currentAssumption;

                                        // add the annotations
                                        annotationPreferences.setCurrentSettings(tagAssumption, !currentSpectrumKey.equalsIgnoreCase(spectrumMatch.getKey()),
                                                peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());

                                        TagSpectrumAnnotator spectrumAnnotator = new TagSpectrumAnnotator();

                                        ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                                annotationPreferences.getNeutralLosses(),
                                                annotationPreferences.getValidatedCharges(),
                                                tagAssumption.getIdentificationCharge().value,
                                                currentSpectrum, tagAssumption.getTag(),
                                                currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                                annotationPreferences.getFragmentIonAccuracy(),
                                                false, annotationPreferences.isHighResolutionAnnotation());

                                        // add the spectrum annotations
                                        spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));

                                        // add de novo sequencing
                                        spectrum.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                                forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                                                annotationPreferences.showForwardIonDeNovoTags(),
                                                annotationPreferences.showRewindIonDeNovoTags(), false);

                                        // get the modifications for the tag
                                        for (TagComponent tagComponent : tagAssumption.getTag().getContent()) {
                                            if (tagComponent instanceof AminoAcidPattern) {
                                                AminoAcidPattern aminoAcidPattern = (AminoAcidPattern) tagComponent;
                                                for (int site = 1; site <= aminoAcidPattern.length(); site++) {
                                                    for (ModificationMatch modificationMatch : aminoAcidPattern.getModificationsAt(site)) {
                                                        allModifications.add(modificationMatch);
                                                    }
                                                }
                                            } else if (tagComponent instanceof AminoAcidSequence) {
                                                AminoAcidSequence aminoAcidSequence = (AminoAcidSequence) tagComponent;
                                                for (int site = 1; site <= aminoAcidSequence.length(); site++) {
                                                    for (ModificationMatch modificationMatch : aminoAcidSequence.getModificationsAt(site)) {
                                                        allModifications.add(modificationMatch);
                                                    }
                                                }
                                            } else if (tagComponent instanceof MassGap) {
                                                // Nothing to do here
                                            } else {
                                                throw new UnsupportedOperationException("Annotation not supported for the tag component " + tagComponent.getClass() + ".");
                                            }
                                        }

                                        if (tagAssumption.getIdentificationCharge().value > maxPrecursorCharge) {
                                            maxPrecursorCharge = tagAssumption.getIdentificationCharge().value;
                                        }

                                        if (!modifiedSequence.isEmpty()) {
                                            modifiedSequence += " vs. ";
                                        }

                                        modifiedSequence += tagAssumption.getTag().getTaggedModifiedSequence(
                                                peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationProfile(), false, false, true, false);
                                    }
                                }
                            }

                            spectrum.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                            spectrum.setYAxisZoomExcludesBackgroundPeaks(annotationPreferences.yAxisZoomExcludesBackgroundPeaks());

                            peptideShakerGUI.updateAnnotationMenus(maxPrecursorCharge, allModifications);

                            // update the spectrum title
                            if (searchResultsTable.getSelectedRowCount() == 1) {
                                ((TitledBorder) spectrumPanel.getBorder()).setTitle(
                                        PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                                        + "Spectrum & Fragment Ions (" + modifiedSequence
                                        + "   " + maxPrecursorCharge + "   "
                                        + Util.roundDouble(currentSpectrum.getPrecursor().getMz(), 2) + " m/z)"
                                        + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                            } else if (searchResultsTable.getSelectedRowCount() == 2) {
                                ((TitledBorder) spectrumPanel.getBorder()).setTitle(
                                        PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                                        + "Spectrum & Fragment Ions (" + modifiedSequence + ")"
                                        + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                            } else if (searchResultsTable.getSelectedRowCount() > 2) {
                                ((TitledBorder) spectrumPanel.getBorder()).setTitle(
                                        PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                                        + "Spectrum & Fragment Ions (" + searchResultsTable.getSelectedRowCount() + " PSMs)"
                                        + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                            }

                            spectrumPanel.repaint();

                            if (searchResultsTable.getSelectedRowCount() > 2) {
                                double bubbleScale = annotationPreferences.getFragmentIonAccuracy() * 10 * peptideShakerGUI.getBubbleScale();

                                if (peptideShakerGUI.useRelativeError()) {
                                    bubbleScale = annotationPreferences.getFragmentIonAccuracy() * 10000 * peptideShakerGUI.getBubbleScale();
                                }

                                MassErrorBubblePlot massErrorBubblePlot = new MassErrorBubblePlot(
                                        selectedIndexes, allAnnotations, allSpectra, annotationPreferences.getFragmentIonAccuracy(),
                                        bubbleScale, selectedIndexes.size() == 1, annotationPreferences.showBars(), peptideShakerGUI.useRelativeError());

                                // hide the legend if selecting more than 20 spectra // @TODO: 20 should not be hardcoded here..
                                if (selectedIndexes.size() > 20) {
                                    massErrorBubblePlot.getChartPanel().getChart().getLegend().setVisible(false);
                                }

                                // hide the outline
                                massErrorBubblePlot.getChartPanel().getChart().getPlot().setOutlineVisible(false);
                                spectrumChartPanel.add(massErrorBubblePlot);
                            }
                        }
                    }

                    if (searchResultsTable.getSelectedRowCount() <= 2) {
                        spectrumChartPanel.add(spectrum);
                    }

                } else {
                    // update the spectrum title
                    ((TitledBorder) spectrumPanel.getBorder()).setTitle(
                            PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                            + "Spectrum & Fragment Ions ("
                            + Util.roundDouble(currentSpectrum.getPrecursor().getMz(), 2) + " m/z)"
                            + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);

                    spectrumChartPanel.add(spectrum);
                    spectrumPanel.repaint();
                }

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            } catch (Exception e) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
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
        peptideShakerGUI.updateAnnotationMenuBarVisableOptions(searchResultsTable.getSelectedRowCount() <= 2, searchResultsTable.getSelectedRowCount() > 2, false, false, searchResultsTable.getSelectedRowCount() == 1);
    }

    /**
     * Returns the bubble plot.
     *
     * @return the bubble plot
     */
    public Component getBubblePlot() {

        if (searchResultsTable.getSelectedRowCount() > 2) {
            return ((MassErrorBubblePlot) spectrumChartPanel.getComponent(0)).getChartPanel();
        }

        return null;
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

        if (tableIndex == TableIndex.SPECTRUM_FILES || tableIndex == TableIndex.PSM_TABLES) {

            // get the file to send the output to
            final File selectedFile = peptideShakerGUI.getUserSelectedFile(".txt", "Tab separated text file (.txt)", "Export...", false);

            if (selectedFile != null) {
                try {
                    final BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile));

                    progressDialog = new ProgressDialogX(peptideShakerGUI,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true);
                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setTitle("Copying to File. Please Wait...");

                    new Thread(new Runnable() {
                        public void run() {
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
                                if (tableIndex == TableIndex.SPECTRUM_FILES) {
                                    Util.tableToFile(spectrumTable, "\t", progressDialog, true, writer);
                                } else if (tableIndex == TableIndex.PSM_TABLES) {
                                    Util.tableToFile(searchResultsTable, "\t", progressDialog, true, writer);
                                }

                                writer.close();

                                boolean processCancelled = progressDialog.isRunCanceled();

                                progressDialog.setRunFinished();

                                if (!processCancelled) {
                                    JOptionPane.showMessageDialog(peptideShakerGUI, "Table content copied to file:\n" + selectedFile.getPath(), "Copied to File", JOptionPane.INFORMATION_MESSAGE);
                                }

                            } catch (IOException e) {
                                progressDialog.setRunFinished();
                                JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                                e.printStackTrace();
                            }
                        }
                    }.start();

                } catch (IOException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
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
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getIdentificationParameters().getSearchParameters().getFragmentIonAccuracy();

        spectrumJPanel.setToolTipText("<html>Accuracy: " + Util.roundDouble(accuracy, 2) + " Da<br>"
                + "Level: " + intensitySlider.getValue() + "%</html>");

        // show the tooltip now
        ToolTipManager.sharedInstance().mouseMoved(
                new MouseEvent(spectrumJPanel, 0, 0, 0,
                        spectrumJPanel.getWidth() - 150, spectrumJPanel.getY() + 20, // X-Y of the mouse for the tool tip
                        0, false));
    }

    /**
     * Returns true if all the used id software agree on the top PSM without
     * accounting for modification localization, false otherwise.
     *
     * @param spectrumMatch the PSM to check
     * @param assumptions map of the assumptions for this spectrum
     * @param sequenceMatchingPreferences the sequence matching preferences
     *
     * @return true if all the used id software agree on the top PSM
     */
    public static int isBestPsmEqualForAllIdSoftware(SpectrumMatch spectrumMatch, HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions, SequenceMatchingPreferences sequenceMatchingPreferences) {

        HashMap<Integer, ArrayList<PeptideAssumption>> peptideAssumptions = new HashMap<Integer, ArrayList<PeptideAssumption>>();
        ArrayList<Integer> tempUsedAdvocates = new ArrayList<Integer>();

        for (Advocate tempAdvocate : Advocate.values()) {
            int advocateIndex = tempAdvocate.getIndex();
            HashMap<Double, ArrayList<SpectrumIdentificationAssumption>> advocateAssumptions = assumptions.get(tempAdvocate.getIndex());
            if (advocateAssumptions != null) {
                ArrayList<Double> eValues = new ArrayList<Double>(advocateAssumptions.keySet());
                Collections.sort(eValues);

                for (double eValue : eValues) {
                    for (SpectrumIdentificationAssumption assumption : advocateAssumptions.get(eValue)) {
                        if (assumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;
                            ArrayList<PeptideAssumption> advocatePeptides = peptideAssumptions.get(advocateIndex);
                            if (advocatePeptides == null) {
                                advocatePeptides = new ArrayList<PeptideAssumption>();
                                peptideAssumptions.put(advocateIndex, advocatePeptides);
                            }
                            advocatePeptides.add(peptideAssumption);
                        }
                        if (!tempUsedAdvocates.contains(advocateIndex)) {
                            tempUsedAdvocates.add(advocateIndex);
                        }
                    }
                }
            }
        }

        // check if all advocates are used
        boolean allAdvocatesFound = tempUsedAdvocates.size() == peptideAssumptions.size();

        if (peptideAssumptions.isEmpty()) {
            return NO_ID; // no ids found
        } else if (allAdvocatesFound && peptideAssumptions.size() == 1) {
            return AGREEMENT_WITH_MODS; // only one search engine used
        } else {

            if (allAdvocatesFound) {

                Iterator<Integer> iterator = peptideAssumptions.keySet().iterator();
                int firstAdvocate = iterator.next();
                ArrayList<PeptideAssumption> firstAssumptions = peptideAssumptions.get(firstAdvocate);
                boolean sameSequenceAndModificationStatus = true;
                boolean sameModifications = true;

                // iterate all the peptides and charges
                while (iterator.hasNext() && sameSequenceAndModificationStatus && sameModifications) {

                    int currentAdvocate = iterator.next();
                    boolean advocateSameSequence = false, advocateSameModifications = false;
                    for (PeptideAssumption firstAssumption : firstAssumptions) {
                        // check for same same sequence, modification and charge status
                        for (PeptideAssumption currentAssumption : peptideAssumptions.get(currentAdvocate)) {
                            if (firstAssumption.getPeptide().isSameSequenceAndModificationStatus(currentAssumption.getPeptide(), sequenceMatchingPreferences)) {
                                advocateSameSequence = true;
                                if (firstAssumption.getPeptide().sameModificationsAs(currentAssumption.getPeptide())) {
                                    advocateSameModifications = true;
                                }
                            }
                            if (advocateSameSequence && advocateSameModifications) {
                                break;
                            }
                        }
                    }
                    if (!advocateSameSequence) {
                        sameSequenceAndModificationStatus = false;
                    }
                    if (!advocateSameModifications) {
                        sameModifications = false;
                    }
                }

                if (sameSequenceAndModificationStatus) {
                    if (sameModifications) {
                        return AGREEMENT_WITH_MODS;
                    } else {
                        return AGREEMENT;
                    }
                } else {
                    return CONFLICT;
                }
            } else {
                return PARTIALLY_MISSING;
            }
        }
    }

    /**
     * Table model for the table listing all spectra.
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
            return 11;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "ID";
                case 2:
                    return "Title";
                case 3:
                    return "m/z";
                case 4:
                    return "Charge";
                case 5:
                    return "Int";
                case 6:
                    return "RT (min)";
                case 7:
                    return "Sequence";
                case 8:
                    return "Protein(s)";
                case 9:
                    return "Confidence";
                case 10:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            try {
                String spectrumTitle = spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                String spectrumKey = Spectrum.getSpectrumKey(fileSelected, spectrumTitle);

                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        int idSoftwareAgreement;
                        if (!identification.matchExists(spectrumKey)) {
                            idSoftwareAgreement = NO_ID;
                        } else {
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            HashMap<Integer, HashMap<Double, ArrayList<SpectrumIdentificationAssumption>>> assumptions = identification.getAssumptions(spectrumKey);
                            idSoftwareAgreement = isBestPsmEqualForAllIdSoftware(spectrumMatch, assumptions, peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());
                        }
                        return idSoftwareAgreement;
                    case 2:
                        return spectrumFactory.getSpectrumTitles(fileSelected).get(row);
                    case 3:
                        Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);
                        if (precursor != null) {
                            return precursor.getMz();
                        } else {
                            return null;
                        }
                    case 4:
                        precursor = spectrumFactory.getPrecursor(spectrumKey);
                        Integer charge = null;
                        if (precursor != null && !precursor.getPossibleCharges().isEmpty()) {
                            charge = precursor.getPossibleCharges().get(0).value; // @TODO: find a way of displaying multiple charges!!!
                        }
                        return charge;
                    case 5:
                        precursor = spectrumFactory.getPrecursor(spectrumKey);
                        if (precursor != null) {
                            return precursor.getIntensity();
                        } else {
                            return null;
                        }
                    case 6:
                        precursor = spectrumFactory.getPrecursor(spectrumKey);
                        if (precursor != null) {
                            double rt = precursor.getRtInMinutes();
                            if (rt < 0) {
                                rt = -1;
                            }
                            return rt; // @TODO: what about retention time windows?
                        } else {
                            return null;
                        }
                    case 7:
                        if (identification.matchExists(spectrumKey)) {
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            DisplayFeaturesGenerator displayFeaturesGenerator = peptideShakerGUI.getDisplayFeaturesGenerator();
                            if (spectrumMatch.getBestPeptideAssumption() != null) {
                                return displayFeaturesGenerator.getTaggedPeptideSequence(spectrumMatch, true, true, true);
                            } else if (spectrumMatch.getBestTagAssumption() != null) {
                                //TODO: include fixed ptms
                                return spectrumMatch.getBestTagAssumption().getTag().getTaggedModifiedSequence(peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationProfile(), true, true, true, false, false);
                            }
                        }
                        return null;
                    case 8:
                        if (identification.matchExists(spectrumKey)) {
                            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                            DisplayFeaturesGenerator displayFeaturesGenerator = peptideShakerGUI.getDisplayFeaturesGenerator();
                            if (spectrumMatch.getBestPeptideAssumption() != null) {
                                return displayFeaturesGenerator.addDatabaseLinks(spectrumMatch.getBestPeptideAssumption().getPeptide().getParentProteins(
                                        peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences()));
                            }
                        }
                        return null;
                    case 9:
                        if (identification.matchExists(spectrumKey)) {
                            PSParameter pSParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, new PSParameter());
                            if (pSParameter != null) {
                                return pSParameter.getPsmConfidence();
                            } else {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    case 10:
                        if (identification.matchExists(spectrumKey)) {
                            PSParameter pSParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, new PSParameter());
                            if (pSParameter != null) {
                                return pSParameter.getMatchValidationLevel().getIndex();
                            } else {
                                return null;
                            }
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

    /**
     * Updates the overview plots based on the information loaded when creating
     * the project.
     *
     * @param inputMap the input map
     */
    private void updateOverviewPlots(eu.isas.peptideshaker.scoring.InputMap inputMap, PsmSpecificMap psmSpecificMap) {

        // The selected file, null for the entire dataset
        String selectedFileName = null; //@TODO: let the user choose the file

        HashMap<Integer, Double> searchEngineTP = new HashMap<Integer, Double>();
        HashMap<Integer, Double> searchEngineFN = new HashMap<Integer, Double>();
        HashMap<Integer, Double> searchEngineContribution = new HashMap<Integer, Double>();
        HashMap<Integer, Double> searchEngineUniqueContribution = new HashMap<Integer, Double>();
        int totalNumberOfSpectra = 0;

        if (selectedFileName == null) {
            for (String spectrumFile : identification.getSpectrumFiles()) {
                totalNumberOfSpectra += spectrumFactory.getNSpectra(spectrumFile);
            }
            for (int advocateId : inputMap.getInputAlgorithmsSorted()) {
                TargetDecoyMap targetDecoyMap = inputMap.getTargetDecoyMap(advocateId);
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                double nTP;
                if (targetDecoyResults == null) {
                    nTP = 0;
                } else {
                    nTP = targetDecoyResults.getnTP();
                }
                searchEngineTP.put(advocateId, nTP);
                double nFN;
                if (targetDecoyResults == null) {
                    nFN = 0;
                } else {
                    nFN = targetDecoyResults.getnTPTotal() - nTP;
                }
                searchEngineFN.put(advocateId, nFN);
                double contribution = inputMap.getAdvocateContribution(advocateId);
                searchEngineContribution.put(advocateId, contribution);
                double uniqueContribution = inputMap.getAdvocateUniqueContribution(advocateId);
                searchEngineUniqueContribution.put(advocateId, uniqueContribution);
            }
            double nTP = 0;
            double totalTP = 0;
            for (TargetDecoyMap targetDecoyMap : psmSpecificMap.getTargetDecoyMaps()) {
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                nTP += targetDecoyResults.getnTP();
                totalTP += targetDecoyResults.getnTPTotal();
            }
            searchEngineTP.put(Advocate.peptideShaker.getIndex(), nTP);
            double nFN = totalTP - nTP;
            searchEngineFN.put(Advocate.peptideShaker.getIndex(), nFN);
        } else {
            totalNumberOfSpectra = spectrumFactory.getNSpectra(selectedFileName);
            for (int advocateId : inputMap.getInputAlgorithmsSorted()) {
                TargetDecoyMap targetDecoyMap = inputMap.getTargetDecoyMap(advocateId, selectedFileName);
                TargetDecoyResults targetDecoyResults = targetDecoyMap.getTargetDecoyResults();
                double nTP;
                if (targetDecoyResults == null) {
                    nTP = 0;
                } else {
                    nTP = targetDecoyResults.getnTP();
                }
                searchEngineTP.put(advocateId, nTP);
                double nFN;
                if (targetDecoyResults == null) {
                    nFN = 0;
                } else {
                    nFN = targetDecoyResults.getnTPTotal() - nTP;
                }
                searchEngineFN.put(advocateId, nFN);
                double contribution = inputMap.getAdvocateContribution(advocateId, selectedFileName);
                searchEngineContribution.put(advocateId, contribution);
                double uniqueContribution = inputMap.getAdvocateUniqueContribution(advocateId, selectedFileName);
                searchEngineUniqueContribution.put(advocateId, uniqueContribution);
            }
            //@TODO: any value for PeptideShaker here?
        }
        updateOverviewPlots(searchEngineTP, searchEngineUniqueContribution, totalNumberOfSpectra); //@TODO: make new plots
    }

    /**
     * Updates the ID software overview plots.
     */
    private void updateOverviewPlots(final HashMap<Integer, Double> totalAdvocateId, final HashMap<Integer, Double> uniqueAdvocateId, final int totalNumberOfSpectra) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                overviewPlotsPanel.removeAll();

                // create the number of psms plot
                createPlot(totalAdvocateId, "#PSMs", false);

                // create the number of unique psms plot
                createPlot(uniqueAdvocateId, "#Unique PSMs", false);

                // create the number of unassigned plot
                HashMap<Integer, Double> unassignedAdvocate = new HashMap<Integer, Double>();
                for (Integer tempAdvocate : advocatesUsed) {
                    if (totalAdvocateId.containsKey(tempAdvocate)) {
                        unassignedAdvocate.put(tempAdvocate, totalNumberOfSpectra - totalAdvocateId.get(tempAdvocate));
                    }
                }
                unassignedAdvocate.put(Advocate.peptideShaker.getIndex(), totalNumberOfSpectra - totalAdvocateId.get(Advocate.peptideShaker.getIndex()));
                createPlot(unassignedAdvocate, "#Unassigned", false);

                // create the id rate plot
                HashMap<Integer, Double> idRateAdvocate = new HashMap<Integer, Double>();
                for (Integer tempAdvocate : advocatesUsed) {
                    if (totalAdvocateId.containsKey(tempAdvocate)) {
                        idRateAdvocate.put(tempAdvocate, ((double) totalAdvocateId.get(tempAdvocate) / totalNumberOfSpectra) * 100);
                    }
                }
                idRateAdvocate.put(Advocate.peptideShaker.getIndex(), ((double) totalAdvocateId.get(Advocate.peptideShaker.getIndex()) / totalNumberOfSpectra) * 100);
                createPlot(idRateAdvocate, "ID Rate (%)", true);

                overviewPlotsPanel.revalidate();
                overviewPlotsPanel.repaint();
            }
        });
    }

    /**
     * Add the given assumption to the table.
     *
     * @param currentAssumption the currently selected assumption
     * @param aProbabilities the PS parameter associated to this assumption
     * @param software the identification software as indexed in the Advocate
     * class
     */
    private void addIdResultsToTable(SpectrumIdentificationAssumption currentAssumption, PSParameter aProbabilities, Integer software) {

        PSParameter probabilities = (PSParameter) currentAssumption.getUrParam(aProbabilities);
        Double confidence = probabilities.getSearchEngineConfidence();
        int currentRowNumber = 0;
        boolean addRowAtBottom = true;

        // find the correct row to insert the match
        if (searchResultsTable.getRowCount() > 0) {
            for (int i = 0; i < searchResultsTable.getRowCount(); i++) {
                if (confidence > (Double) searchResultsTable.getValueAt(i, 5)) {
                    currentRowNumber = i;
                    addRowAtBottom = false;
                    break;
                }
            }
        }

        Integer validationType = probabilities.getMatchValidationLevel().getIndex();

        String sequence;
        if (currentAssumption instanceof PeptideAssumption) {
            SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
            ModificationProfile modificationProfile = searchParameters.getModificationProfile();
            Peptide peptide = ((PeptideAssumption) currentAssumption).getPeptide();

            boolean showFixed = false; // @TODO: has to be a better way of doing this?
            for (String ptmName : peptideShakerGUI.getDisplayPreferences().getDisplayedPtms()) {
                if (modificationProfile.getFixedModifications().contains(ptmName)) {
                    showFixed = true;
                }
            }

            sequence = peptide.getTaggedModifiedSequence(modificationProfile, true, true, true, !showFixed);
            if (addRowAtBottom) {
                searchResultsTablePeptideTooltips.add(peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(peptide));
            } else {
                searchResultsTablePeptideTooltips.add(currentRowNumber, peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(peptide));
            }
        } else if (currentAssumption instanceof TagAssumption) {
            TagAssumption tagAssumption = (TagAssumption) currentAssumption;
            sequence = tagAssumption.getTag().getTaggedModifiedSequence(peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationProfile(), true, true, true, false, false);
            if (addRowAtBottom) {
                searchResultsTablePeptideTooltips.add(peptideShakerGUI.getDisplayFeaturesGenerator().getTagModificationTooltipAsHtml(tagAssumption.getTag()));
            } else {
                searchResultsTablePeptideTooltips.add(currentRowNumber, peptideShakerGUI.getDisplayFeaturesGenerator().getTagModificationTooltipAsHtml(tagAssumption.getTag()));
            }
        } else {
            throw new UnsupportedOperationException("Sequence display not implemented for assumption " + currentAssumption.getClass() + ".");
        }
        if (addRowAtBottom) {
            currentAssumptionsList.add(currentAssumption);
        } else {
            currentAssumptionsList.add(currentRowNumber, currentAssumption);
        }

        Object[] rowData = new Object[]{
            currentRowNumber,
            software,
            currentAssumption.getRank(),
            sequence,
            currentAssumption.getIdentificationCharge().value,
            confidence,
            validationType
        };
        if (addRowAtBottom) {
            ((DefaultTableModel) searchResultsTable.getModel()).addRow(rowData);
        } else {
            ((DefaultTableModel) searchResultsTable.getModel()).insertRow(currentRowNumber, rowData);
        }
    }

    /**
     * Create an overview plot.
     *
     * @param data the data to plot
     * @param xAxisLabel the xAxis label
     * @param roundDecimals if true, the decimals in the labels are rounded to
     * one decimal
     */
    private void createPlot(HashMap<Integer, Double> data, String xAxisLabel, boolean roundDecimals) {

        DefaultCategoryDataset psmDataset = new DefaultCategoryDataset();
        for (Integer tempAdvocate : advocatesUsed) {
            psmDataset.addValue(data.get(tempAdvocate), Advocate.getAdvocate(tempAdvocate).getName(), xAxisLabel);
        }
        psmDataset.addValue(data.get(Advocate.peptideShaker.getIndex()), Advocate.peptideShaker.getName(), xAxisLabel);

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, psmDataset, PlotOrientation.VERTICAL, false, false, false);
        CategoryPlot plot = chart.getCategoryPlot();
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(java.awt.Color.WHITE);
        plot.setBackgroundPaint(java.awt.Color.WHITE);
        chart.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLowerMargin(0.15);
        rangeAxis.setUpperMargin(0.15);
        plot.setOutlineVisible(false);
        BarRenderer renderer = new BarRenderer();
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardBarPainter());
        int dataSeriesCounter = 0;
        for (Integer tempAdvocate : advocatesUsed) {
            if (advocatesUsed.contains(tempAdvocate)) {
                renderer.setSeriesPaint(dataSeriesCounter++, Advocate.getAdvocateColorMap().get(tempAdvocate));
            }
        }
        renderer.setSeriesPaint(dataSeriesCounter, Advocate.getAdvocateColorMap().get(Advocate.peptideShaker.getIndex()));
        if (roundDecimals) {
            renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator(StandardCategoryItemLabelGenerator.DEFAULT_LABEL_FORMAT_STRING, new DecimalFormat("0.0")));
        } else {
            renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        }
        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        renderer.setBaseItemLabelsVisible(true);
        plot.setRenderer(renderer);

        // add the plot to the chart
        overviewPlotsPanel.add(chartPanel);
    }
}
