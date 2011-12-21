package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import com.compomics.util.gui.spectrum.FragmentIonTable;
import com.compomics.util.gui.spectrum.IntensityHistogram;
import com.compomics.util.gui.spectrum.MassErrorBubblePlot;
import com.compomics.util.gui.spectrum.MassErrorPlot;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.export.FeaturesGenerator;
import eu.isas.peptideshaker.gui.ExportFeatureDialog;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.ProteinInferenceDialog;
import eu.isas.peptideshaker.gui.ProteinInferencePeptideLevelDialog;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.JSparklinesDataset;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The overview panel displaying the proteins, the peptides and the spectra.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class OverviewPanel extends javax.swing.JPanel {

    /**
     * Indexes for the three main data tables.
     */
    private enum TableIndex {

        PROTEIN_TABLE, PEPTIDE_TABLE, PSM_TABLE
    };
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The current spectrum key.
     */
    private String currentSpectrumKey = "";
    /**
     * A reference to the protein score column.
     */
    private TableColumn proteinScoreColumn;
    /**
     * A reference to the peptide score column.
     */
    private TableColumn peptideScoreColumn;
    /**
     * The current sequence coverage;
     */
    private int[] coverage;
    /**
     * The current protein sequence;
     */
    private String currentProteinSequence = "";
    /**
     * The maximum sequence length for display in the sequence coverage panel.
     */
    private final int MAX_SEQUENCE_LENGTH = 6000;
    /**
     * The maximum mz value in the current list of PSMs. Needed to make sure that
     * the PSMs for the same peptide all use the same mz range.
     */
    private double maxPsmMzValue = Double.MIN_VALUE;
    /**
     * The current spectrum panel.
     */
    private SpectrumPanel spectrum;
    /**
     * boolean indicating whether the spectrum shall be displayed
     */
    private boolean displaySpectrum = true;
    /**
     * Boolean indicating whether the sequence coverage shall be displayed
     */
    private boolean displayCoverage = true;
    /**
     * Boolean indicating whether the protein table shall be displayed
     */
    private boolean displayProteins = true;
    /**
     * Boolean indicating whether the PSMs shall be displayed
     */
    private boolean displayPeptidesAndPSMs = true;
    /**
     * A mapping of the protein table entries
     */
    private HashMap<Integer, String> proteinTableMap = new HashMap<Integer, String>();
    /**
     * A mapping of the peptide table entries
     */
    private HashMap<Integer, String> peptideTableMap = new HashMap<Integer, String>();
    /**
     * A mapping of the psm table entries
     */
    private HashMap<Integer, String> psmTableMap = new HashMap<Integer, String>();
    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The protein table column header tooltips.
     */
    private ArrayList<String> proteinTableToolTips;
    /**
     * The peptide table column header tooltips.
     */
    private ArrayList<String> peptideTableToolTips;
    /**
     * The PMS table column header tooltips.
     */
    private ArrayList<String> psmTableToolTips;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The max number of peptides in the protein table.
     */
    private int maxPeptides = 10;
    /**
     * The max number of spectra in the protein table.
     */
    private int maxSpectra = 10;
    /**
     * The max spectrum counting score in the protein table.
     */
    private double maxSpectrumCounting = 10, maxMW = 10;

    /**
     * Creates a new OverviewPanel.
     *
     * @param parent the PeptideShaker parent frame.
     */
    public OverviewPanel(PeptideShakerGUI parent) {

        this.peptideShakerGUI = parent;

        initComponents();

        sequenceCoverageTableScrollPane.setBorder(null);

        proteinScoreColumn = proteinTable.getColumn("Score");
        peptideScoreColumn = peptideTable.getColumn("Score");

        setTableProperties();

        // make sure that the scroll panes are see-through
        proteinScrollPane.getViewport().setOpaque(false);
        peptideScrollPane.getViewport().setOpaque(false);
        spectraScrollPane.getViewport().setOpaque(false);
        fragmentIonsJScrollPane.getViewport().setOpaque(false);
        sequenceCoverageTableScrollPane.getViewport().setOpaque(false);

        // make the tabs in the spectrum tabbed pane go from right to left
        spectrumJTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        updateSeparators();
        formComponentResized(null);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        // set table properties
        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        psmTable.getTableHeader().setReorderingAllowed(false);

        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);
        psmTable.setAutoCreateRowSorter(true);

        // the index column
        proteinTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMaxWidth(50);
        psmTable.getColumn(" ").setMaxWidth(50);
        proteinTable.getColumn(" ").setMinWidth(50);
        peptideTable.getColumn(" ").setMinWidth(50);
        psmTable.getColumn(" ").setMinWidth(50);
        peptideTable.getColumn("Start").setMinWidth(50);
        peptideTable.getColumn("Start").setMaxWidth(50);
        peptideTable.getColumn("End").setMinWidth(50);
        peptideTable.getColumn("End").setMaxWidth(50);

        proteinTable.getColumn("Confidence").setMaxWidth(90);
        proteinTable.getColumn("Confidence").setMinWidth(90);
        peptideTable.getColumn("Confidence").setMaxWidth(90);
        peptideTable.getColumn("Confidence").setMinWidth(90);
        psmTable.getColumn("Confidence").setMaxWidth(90);
        psmTable.getColumn("Confidence").setMinWidth(90);
        proteinTable.getColumn("Score").setMaxWidth(90);
        proteinTable.getColumn("Score").setMinWidth(90);
        peptideTable.getColumn("Score").setMaxWidth(90);
        peptideTable.getColumn("Score").setMinWidth(90);

        // the validated column
        proteinTable.getColumn("").setMaxWidth(30);
        peptideTable.getColumn("").setMaxWidth(30);
        psmTable.getColumn("").setMaxWidth(30);
        proteinTable.getColumn("").setMinWidth(30);
        peptideTable.getColumn("").setMinWidth(30);
        psmTable.getColumn("").setMinWidth(30);

        // the selected columns
        proteinTable.getColumn("  ").setMaxWidth(30);
        proteinTable.getColumn("  ").setMinWidth(30);
        peptideTable.getColumn("  ").setMaxWidth(30);
        peptideTable.getColumn("  ").setMinWidth(30);
        psmTable.getColumn("  ").setMaxWidth(30);
        psmTable.getColumn("  ").setMinWidth(30);

        // the protein inference column
        proteinTable.getColumn("PI").setMaxWidth(37);
        proteinTable.getColumn("PI").setMinWidth(37);
        peptideTable.getColumn("PI").setMaxWidth(37);
        peptideTable.getColumn("PI").setMinWidth(37);
        psmTable.getColumn("SE").setMaxWidth(37);
        psmTable.getColumn("SE").setMinWidth(37);

        // set up the protein inference color map
        HashMap<Integer, Color> proteinInferenceColorMap = new HashMap<Integer, Color>();
        proteinInferenceColorMap.put(PSParameter.NOT_GROUP, peptideShakerGUI.getSparklineColor()); // NOT_GROUP
        proteinInferenceColorMap.put(PSParameter.ISOFORMS, Color.YELLOW); // ISOFORMS
        proteinInferenceColorMap.put(PSParameter.ISOFORMS_UNRELATED, Color.ORANGE); // ISOFORMS_UNRELATED
        proteinInferenceColorMap.put(PSParameter.UNRELATED, Color.RED); // UNRELATED

        // set up the protein inference tooltip map
        HashMap<Integer, String> proteinInferenceTooltipMap = new HashMap<Integer, String>();
        proteinInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Single Protein");
        proteinInferenceTooltipMap.put(PSParameter.ISOFORMS, "Isoforms");
        proteinInferenceTooltipMap.put(PSParameter.ISOFORMS_UNRELATED, "Unrelated Isoforms");
        proteinInferenceTooltipMap.put(PSParameter.UNRELATED, "Unrelated Proteins");

        proteinTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(peptideShakerGUI.getSelectedRowHtmlTagFontColor(), peptideShakerGUI.getNotSelectedRowHtmlTagFontColor()));
        proteinTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), proteinInferenceColorMap, proteinInferenceTooltipMap));
        proteinTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("MS2 Quant.").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("MW").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        proteinTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        proteinTable.getColumn("Coverage").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth());
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).setMinimumChartValue(5d);
        proteinTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));
        proteinTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));

        // set up the peptide inference color map
        HashMap<Integer, Color> peptideInferenceColorMap = new HashMap<Integer, Color>();
        peptideInferenceColorMap.put(PSParameter.NOT_GROUP, peptideShakerGUI.getSparklineColor());
        peptideInferenceColorMap.put(PSParameter.ISOFORMS, Color.YELLOW);
        peptideInferenceColorMap.put(PSParameter.ISOFORMS_UNRELATED, Color.ORANGE);
        peptideInferenceColorMap.put(PSParameter.UNRELATED, Color.RED);

        // set up the peptide inference tooltip map
        HashMap<Integer, String> peptideInferenceTooltipMap = new HashMap<Integer, String>();
        peptideInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Unique to a single protein");
        peptideInferenceTooltipMap.put(PSParameter.ISOFORMS, "Belongs to a group of isoforms");
        peptideInferenceTooltipMap.put(PSParameter.ISOFORMS_UNRELATED, "Belongs to a group of isoforms and unrelated proteins");
        peptideInferenceTooltipMap.put(PSParameter.UNRELATED, "Belongs to unrelated proteins");

        peptideTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), peptideInferenceColorMap, peptideInferenceTooltipMap));
        peptideTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        peptideTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        peptideTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));
        peptideTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));


        // set up the psm color map
        HashMap<Integer, Color> psmColorMap = new HashMap<Integer, Color>();
        psmColorMap.put(SpectrumIdentificationPanel.AGREEMENT, peptideShakerGUI.getSparklineColor()); // search engines agree
        psmColorMap.put(SpectrumIdentificationPanel.CONFLICT, Color.YELLOW); // search engines don't agree

        // set up the psm tooltip map
        HashMap<Integer, String> psmTooltipMap = new HashMap<Integer, String>();
        psmTooltipMap.put(SpectrumIdentificationPanel.AGREEMENT, "Search Engines Agree");
        psmTooltipMap.put(SpectrumIdentificationPanel.CONFLICT, "Search Engines Disagree");

        psmTable.getColumn("SE").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.lightGray, psmColorMap, psmTooltipMap));
        psmTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        psmTable.getColumn("Mass Error").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                peptideShakerGUI.getSearchParameters().getPrecursorAccuracy(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        psmTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() - 30);
        psmTable.getColumn("").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));
        psmTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));

        coverageTable.getColumn(" ").setCellRenderer(new JSparklinesTableCellRenderer(JSparklinesTableCellRenderer.PlotType.proteinSequence, PlotOrientation.HORIZONTAL, 0.0, 100d));
        ((JSparklinesTableCellRenderer) coverageTable.getColumn(" ").getCellRenderer()).setBackgroundColor(Color.WHITE);

        try {
            proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
            peptideTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        } catch (IllegalArgumentException e) {
            // ignore error
        }


        // set up the table header tooltips
        proteinTableToolTips = new ArrayList<String>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Starred");
        proteinTableToolTips.add("Protein Inference Class");
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Protein Seqeunce Coverage (%)");
        proteinTableToolTips.add("Number of Peptides");
        proteinTableToolTips.add("Number of Spectra");
        proteinTableToolTips.add("MS2 Quantification");
        proteinTableToolTips.add("Protein Molecular Weight (kDa)");
        proteinTableToolTips.add("Protein Score");
        proteinTableToolTips.add("Protein Confidence");
        proteinTableToolTips.add("Validated");

        peptideTableToolTips = new ArrayList<String>();
        peptideTableToolTips.add(null);
        peptideTableToolTips.add("Starred");
        peptideTableToolTips.add("Protein Inference Class");
        peptideTableToolTips.add("Peptide Sequence");
        peptideTableToolTips.add("Peptide Start Index");
        peptideTableToolTips.add("Peptide End Index");
        peptideTableToolTips.add("Number of Spectra");
        peptideTableToolTips.add("Peptide Score");
        peptideTableToolTips.add("Peptide Confidence");
        peptideTableToolTips.add("Validated");

        psmTableToolTips = new ArrayList<String>();
        psmTableToolTips.add(null);
        psmTableToolTips.add("Starred");
        psmTableToolTips.add("Search Engine Agreement");
        psmTableToolTips.add("Peptide Sequence");
        psmTableToolTips.add("Precursor Charge");
        psmTableToolTips.add("Mass Error");
        psmTableToolTips.add("Peptide-Spectrum Match Confidence");
        psmTableToolTips.add("Validated");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectJPopupMenu = new javax.swing.JPopupMenu();
        selectAllMenuItem = new javax.swing.JMenuItem();
        deselectAllMenuItem = new javax.swing.JMenuItem();
        backgroundLayeredPane = new javax.swing.JLayeredPane();
        overviewJPanel = new javax.swing.JPanel();
        overviewJSplitPane = new javax.swing.JSplitPane();
        proteinsJPanel = new javax.swing.JPanel();
        proteinsLayeredPane = new javax.swing.JLayeredPane();
        proteinsLayeredPanel = new javax.swing.JPanel();
        proteinScrollPane = new javax.swing.JScrollPane();
        proteinTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) proteinTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        proteinsHelpJButton = new javax.swing.JButton();
        exportProteinsJButton = new javax.swing.JButton();
        hideProteinsJButton = new javax.swing.JButton();
        contextMenuProteinsBackgroundPanel = new javax.swing.JPanel();
        coverageJSplitPane = new javax.swing.JSplitPane();
        sequenceCoverageJPanel = new javax.swing.JPanel();
        sequenceCoverageLayeredPane = new javax.swing.JLayeredPane();
        sequenceCoveragePanel = new javax.swing.JPanel();
        sequenceCoverageTableScrollPane = new javax.swing.JScrollPane();
        coverageTable = new javax.swing.JTable();
        sequenceCoveragetHelpJButton = new javax.swing.JButton();
        exportSequenceCoverageContextJButton = new javax.swing.JButton();
        hideCoverageJButton = new javax.swing.JButton();
        contextMenuSequenceCoverageBackgroundPanel = new javax.swing.JPanel();
        peptidesPsmSpectrumFragmentIonsJSplitPane = new javax.swing.JSplitPane();
        peptidesPsmJSplitPane = new javax.swing.JSplitPane();
        peptidesJPanel = new javax.swing.JPanel();
        peptidesLayeredPane = new javax.swing.JLayeredPane();
        peptidesPanel = new javax.swing.JPanel();
        peptideScrollPane = new javax.swing.JScrollPane();
        peptideTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) peptideTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        peptidesHelpJButton = new javax.swing.JButton();
        exportPeptidesJButton = new javax.swing.JButton();
        hidePeptideAndPsmsJButton = new javax.swing.JButton();
        contextMenuPeptidesBackgroundPanel = new javax.swing.JPanel();
        psmJPanel = new javax.swing.JPanel();
        psmsLayeredPane = new javax.swing.JLayeredPane();
        psmsPanel = new javax.swing.JPanel();
        spectraScrollPane = new javax.swing.JScrollPane();
        psmTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        tip = (String) psmTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        psmsHelpJButton = new javax.swing.JButton();
        exportPsmsJButton = new javax.swing.JButton();
        hidePeptideAndPsmsJButton2 = new javax.swing.JButton();
        contextMenuPsmsBackgroundPanel = new javax.swing.JPanel();
        spectrumMainJPanel = new javax.swing.JPanel();
        spectrumLayeredPane = new javax.swing.JLayeredPane();
        spectrumMainPanel = new javax.swing.JPanel();
        slidersSplitPane = new javax.swing.JSplitPane();
        spectrumJTabbedPane = new javax.swing.JTabbedPane();
        fragmentIonJPanel = new javax.swing.JPanel();
        fragmentIonsJScrollPane = new javax.swing.JScrollPane();
        ionTableJToolBar = new javax.swing.JToolBar();
        ionTableAnnotationMenuPanel = new javax.swing.JPanel();
        bubblePlotTabJPanel = new javax.swing.JPanel();
        bubbleJPanel = new javax.swing.JPanel();
        bubblePlotJToolBar = new javax.swing.JToolBar();
        bubbleAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumSplitPane = new javax.swing.JSplitPane();
        secondarySpectrumPlotsJPanel = new javax.swing.JPanel();
        spectrumPanel = new javax.swing.JPanel();
        slidersPanel = new javax.swing.JPanel();
        accuracySlider = new javax.swing.JSlider();
        intensitySlider = new javax.swing.JSlider();
        spectrumHelpJButton = new javax.swing.JButton();
        exportSpectrumJButton = new javax.swing.JButton();
        hideSpectrumPanelJButton = new javax.swing.JButton();
        maximizeSpectrumPanelJButton = new javax.swing.JButton();
        contextMenuSpectrumBackgroundPanel = new javax.swing.JPanel();
        toolBar = new javax.swing.JToolBar();
        showProteinsBeforeSeparator = new javax.swing.JPopupMenu.Separator();
        showProteinsJButton = new javax.swing.JButton();
        showProteinsAfterSeparator = new javax.swing.JPopupMenu.Separator();
        showPeptidesAndPsmsJButton = new javax.swing.JButton();
        showPeptidesAfterSeparator = new javax.swing.JPopupMenu.Separator();
        showSpectrumJButton = new javax.swing.JButton();
        showSpectrumAfterSeparator = new javax.swing.JPopupMenu.Separator();
        showCoverageJButton = new javax.swing.JButton();
        showCoverageAfterSeparator = new javax.swing.JPopupMenu.Separator();

        selectAllMenuItem.setText("Select All");
        selectAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllMenuItemActionPerformed(evt);
            }
        });
        selectJPopupMenu.add(selectAllMenuItem);

        deselectAllMenuItem.setText("Deselect All");
        deselectAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deselectAllMenuItemActionPerformed(evt);
            }
        });
        selectJPopupMenu.add(deselectAllMenuItem);

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        overviewJPanel.setBackground(new java.awt.Color(255, 255, 255));
        overviewJPanel.setOpaque(false);
        overviewJPanel.setPreferredSize(new java.awt.Dimension(900, 800));

        overviewJSplitPane.setBorder(null);
        overviewJSplitPane.setDividerLocation(300);
        overviewJSplitPane.setDividerSize(0);
        overviewJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        overviewJSplitPane.setResizeWeight(0.5);
        overviewJSplitPane.setOpaque(false);

        proteinsJPanel.setOpaque(false);

        proteinsLayeredPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinsLayeredPanel.setOpaque(false);

        proteinScrollPane.setOpaque(false);

        proteinTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "  ", "PI", "Accession", "Description", "Coverage", "#Peptides", "#Spectra", "MS2 Quant.", "MW", "Score", "Confidence", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        proteinTable.setOpaque(false);
        proteinTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinTableMouseReleased(evt);
            }
        });
        proteinTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                proteinTableMouseMoved(evt);
            }
        });
        proteinTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinTableKeyReleased(evt);
            }
        });
        proteinScrollPane.setViewportView(proteinTable);
        proteinTable.getAccessibleContext().setAccessibleName("ProteinTable");

        javax.swing.GroupLayout proteinsLayeredPanelLayout = new javax.swing.GroupLayout(proteinsLayeredPanel);
        proteinsLayeredPanel.setLayout(proteinsLayeredPanelLayout);
        proteinsLayeredPanelLayout.setHorizontalGroup(
            proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 938, Short.MAX_VALUE)
            .addGroup(proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 918, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        proteinsLayeredPanelLayout.setVerticalGroup(
            proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 273, Short.MAX_VALUE)
            .addGroup(proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        proteinsLayeredPanel.setBounds(0, 0, 950, 300);
        proteinsLayeredPane.add(proteinsLayeredPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        proteinsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        proteinsHelpJButton.setToolTipText("Help");
        proteinsHelpJButton.setBorder(null);
        proteinsHelpJButton.setBorderPainted(false);
        proteinsHelpJButton.setContentAreaFilled(false);
        proteinsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        proteinsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                proteinsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinsHelpJButtonMouseExited(evt);
            }
        });
        proteinsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinsHelpJButtonActionPerformed(evt);
            }
        });
        proteinsHelpJButton.setBounds(930, 0, 10, 25);
        proteinsLayeredPane.add(proteinsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportProteinsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setToolTipText("Copy to Clipboard");
        exportProteinsJButton.setBorder(null);
        exportProteinsJButton.setBorderPainted(false);
        exportProteinsJButton.setContentAreaFilled(false);
        exportProteinsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setEnabled(false);
        exportProteinsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportProteinsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportProteinsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportProteinsJButtonMouseExited(evt);
            }
        });
        exportProteinsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProteinsJButtonActionPerformed(evt);
            }
        });
        exportProteinsJButton.setBounds(920, 0, 10, 25);
        proteinsLayeredPane.add(exportProteinsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        hideProteinsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide_grey.png"))); // NOI18N
        hideProteinsJButton.setToolTipText("Hide Proteins (Shift+Ctrl+P)");
        hideProteinsJButton.setBorder(null);
        hideProteinsJButton.setBorderPainted(false);
        hideProteinsJButton.setContentAreaFilled(false);
        hideProteinsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide.png"))); // NOI18N
        hideProteinsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                hideProteinsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                hideProteinsJButtonMouseExited(evt);
            }
        });
        hideProteinsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideProteinsJButtonActionPerformed(evt);
            }
        });
        hideProteinsJButton.setBounds(910, 0, 10, 25);
        proteinsLayeredPane.add(hideProteinsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuProteinsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuProteinsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuProteinsBackgroundPanel);
        contextMenuProteinsBackgroundPanel.setLayout(contextMenuProteinsBackgroundPanelLayout);
        contextMenuProteinsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuProteinsBackgroundPanelLayout.setVerticalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuProteinsBackgroundPanel.setBounds(910, 0, 40, 20);
        proteinsLayeredPane.add(contextMenuProteinsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout proteinsJPanelLayout = new javax.swing.GroupLayout(proteinsJPanel);
        proteinsJPanel.setLayout(proteinsJPanelLayout);
        proteinsJPanelLayout.setHorizontalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 960, Short.MAX_VALUE)
        );
        proteinsJPanelLayout.setVerticalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        overviewJSplitPane.setTopComponent(proteinsJPanel);

        coverageJSplitPane.setBorder(null);
        coverageJSplitPane.setDividerLocation(350);
        coverageJSplitPane.setDividerSize(0);
        coverageJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        coverageJSplitPane.setResizeWeight(1.0);
        coverageJSplitPane.setOpaque(false);

        sequenceCoverageJPanel.setOpaque(false);

        sequenceCoveragePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Protein Sequence Coverage"));
        sequenceCoveragePanel.setOpaque(false);

        sequenceCoverageTableScrollPane.setOpaque(false);

        coverageTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null}
            },
            new String [] {
                " "
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        coverageTable.setEnabled(false);
        coverageTable.setOpaque(false);
        coverageTable.setTableHeader(null);
        coverageTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                coverageTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                coverageTableMouseExited(evt);
            }
        });
        coverageTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                coverageTableMouseMoved(evt);
            }
        });
        sequenceCoverageTableScrollPane.setViewportView(coverageTable);

        javax.swing.GroupLayout sequenceCoveragePanelLayout = new javax.swing.GroupLayout(sequenceCoveragePanel);
        sequenceCoveragePanel.setLayout(sequenceCoveragePanelLayout);
        sequenceCoveragePanelLayout.setHorizontalGroup(
            sequenceCoveragePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoveragePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sequenceCoverageTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 918, Short.MAX_VALUE)
                .addContainerGap())
        );
        sequenceCoveragePanelLayout.setVerticalGroup(
            sequenceCoveragePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoveragePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sequenceCoverageTableScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        sequenceCoveragePanel.setBounds(0, 0, 950, 70);
        sequenceCoverageLayeredPane.add(sequenceCoveragePanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        sequenceCoveragetHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        sequenceCoveragetHelpJButton.setToolTipText("Help");
        sequenceCoveragetHelpJButton.setBorder(null);
        sequenceCoveragetHelpJButton.setBorderPainted(false);
        sequenceCoveragetHelpJButton.setContentAreaFilled(false);
        sequenceCoveragetHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        sequenceCoveragetHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sequenceCoveragetHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sequenceCoveragetHelpJButtonMouseExited(evt);
            }
        });
        sequenceCoveragetHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sequenceCoveragetHelpJButtonActionPerformed(evt);
            }
        });
        sequenceCoveragetHelpJButton.setBounds(930, 0, 10, 25);
        sequenceCoverageLayeredPane.add(sequenceCoveragetHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportSequenceCoverageContextJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSequenceCoverageContextJButton.setToolTipText("Copy to Clipboard");
        exportSequenceCoverageContextJButton.setBorder(null);
        exportSequenceCoverageContextJButton.setBorderPainted(false);
        exportSequenceCoverageContextJButton.setContentAreaFilled(false);
        exportSequenceCoverageContextJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportSequenceCoverageContextJButton.setEnabled(false);
        exportSequenceCoverageContextJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportSequenceCoverageContextJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportSequenceCoverageContextJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportSequenceCoverageContextJButtonMouseExited(evt);
            }
        });
        exportSequenceCoverageContextJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSequenceCoverageContextJButtonActionPerformed(evt);
            }
        });
        exportSequenceCoverageContextJButton.setBounds(920, 0, 10, 25);
        sequenceCoverageLayeredPane.add(exportSequenceCoverageContextJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        hideCoverageJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide_grey.png"))); // NOI18N
        hideCoverageJButton.setToolTipText("Hide Coverage (Shift+Ctrl+E)");
        hideCoverageJButton.setBorder(null);
        hideCoverageJButton.setBorderPainted(false);
        hideCoverageJButton.setContentAreaFilled(false);
        hideCoverageJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide.png"))); // NOI18N
        hideCoverageJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                hideCoverageJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                hideCoverageJButtonMouseExited(evt);
            }
        });
        hideCoverageJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideCoverageJButtonActionPerformed(evt);
            }
        });
        hideCoverageJButton.setBounds(910, 0, 10, 25);
        sequenceCoverageLayeredPane.add(hideCoverageJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSequenceCoverageBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSequenceCoverageBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSequenceCoverageBackgroundPanel);
        contextMenuSequenceCoverageBackgroundPanel.setLayout(contextMenuSequenceCoverageBackgroundPanelLayout);
        contextMenuSequenceCoverageBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSequenceCoverageBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuSequenceCoverageBackgroundPanelLayout.setVerticalGroup(
            contextMenuSequenceCoverageBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuSequenceCoverageBackgroundPanel.setBounds(900, 0, 40, 20);
        sequenceCoverageLayeredPane.add(contextMenuSequenceCoverageBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout sequenceCoverageJPanelLayout = new javax.swing.GroupLayout(sequenceCoverageJPanel);
        sequenceCoverageJPanel.setLayout(sequenceCoverageJPanelLayout);
        sequenceCoverageJPanelLayout.setHorizontalGroup(
            sequenceCoverageJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sequenceCoverageLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 960, Short.MAX_VALUE)
        );
        sequenceCoverageJPanelLayout.setVerticalGroup(
            sequenceCoverageJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sequenceCoverageLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
        );

        coverageJSplitPane.setRightComponent(sequenceCoverageJPanel);

        peptidesPsmSpectrumFragmentIonsJSplitPane.setBorder(null);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(450);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(0);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setResizeWeight(0.5);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setOpaque(false);

        peptidesPsmJSplitPane.setBorder(null);
        peptidesPsmJSplitPane.setDividerLocation(175);
        peptidesPsmJSplitPane.setDividerSize(0);
        peptidesPsmJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptidesPsmJSplitPane.setResizeWeight(0.5);
        peptidesPsmJSplitPane.setOpaque(false);

        peptidesJPanel.setOpaque(false);

        peptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        peptidesPanel.setOpaque(false);

        peptideScrollPane.setOpaque(false);

        peptideTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "  ", "PI", "Sequence", "Start", "End", "#Spectra", "Score", "Confidence", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        peptideTable.setOpaque(false);
        peptideTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
            }
        });
        peptideTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptideTableMouseMoved(evt);
            }
        });
        peptideTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideTableKeyReleased(evt);
            }
        });
        peptideScrollPane.setViewportView(peptideTable);

        javax.swing.GroupLayout peptidesPanelLayout = new javax.swing.GroupLayout(peptidesPanel);
        peptidesPanel.setLayout(peptidesPanelLayout);
        peptidesPanelLayout.setHorizontalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 438, Short.MAX_VALUE)
            .addGroup(peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(peptidesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        peptidesPanelLayout.setVerticalGroup(
            peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 143, Short.MAX_VALUE)
            .addGroup(peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(peptidesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        peptidesPanel.setBounds(0, 0, 450, 170);
        peptidesLayeredPane.add(peptidesPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        peptidesHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        peptidesHelpJButton.setToolTipText("Help");
        peptidesHelpJButton.setBorder(null);
        peptidesHelpJButton.setBorderPainted(false);
        peptidesHelpJButton.setContentAreaFilled(false);
        peptidesHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        peptidesHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                peptidesHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptidesHelpJButtonMouseExited(evt);
            }
        });
        peptidesHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptidesHelpJButtonActionPerformed(evt);
            }
        });
        peptidesHelpJButton.setBounds(430, 0, 10, 25);
        peptidesLayeredPane.add(peptidesHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPeptidesJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesJButton.setToolTipText("Copy to Clipboard");
        exportPeptidesJButton.setBorder(null);
        exportPeptidesJButton.setBorderPainted(false);
        exportPeptidesJButton.setContentAreaFilled(false);
        exportPeptidesJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesJButton.setEnabled(false);
        exportPeptidesJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPeptidesJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPeptidesJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPeptidesJButtonMouseExited(evt);
            }
        });
        exportPeptidesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPeptidesJButtonActionPerformed(evt);
            }
        });
        exportPeptidesJButton.setBounds(420, 0, 10, 25);
        peptidesLayeredPane.add(exportPeptidesJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        hidePeptideAndPsmsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide_grey.png"))); // NOI18N
        hidePeptideAndPsmsJButton.setToolTipText("Hide Peptides & PSMs (Shift+Ctrl+E)");
        hidePeptideAndPsmsJButton.setBorder(null);
        hidePeptideAndPsmsJButton.setBorderPainted(false);
        hidePeptideAndPsmsJButton.setContentAreaFilled(false);
        hidePeptideAndPsmsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide.png"))); // NOI18N
        hidePeptideAndPsmsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                hidePeptideAndPsmsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                hidePeptideAndPsmsJButtonMouseExited(evt);
            }
        });
        hidePeptideAndPsmsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hidePeptideAndPsmsJButtonActionPerformed(evt);
            }
        });
        hidePeptideAndPsmsJButton.setBounds(410, 0, 10, 25);
        peptidesLayeredPane.add(hidePeptideAndPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPeptidesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPeptidesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPeptidesBackgroundPanel);
        contextMenuPeptidesBackgroundPanel.setLayout(contextMenuPeptidesBackgroundPanelLayout);
        contextMenuPeptidesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuPeptidesBackgroundPanelLayout.setVerticalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuPeptidesBackgroundPanel.setBounds(400, 0, 40, 20);
        peptidesLayeredPane.add(contextMenuPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout peptidesJPanelLayout = new javax.swing.GroupLayout(peptidesJPanel);
        peptidesJPanel.setLayout(peptidesJPanelLayout);
        peptidesJPanelLayout.setHorizontalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(peptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
        );
        peptidesJPanelLayout.setVerticalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(peptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
        );

        peptidesPsmJSplitPane.setTopComponent(peptidesJPanel);

        psmJPanel.setOpaque(false);

        psmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide-Spectrum Matches"));
        psmsPanel.setOpaque(false);

        spectraScrollPane.setOpaque(false);

        psmTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "  ", "SE", "Sequence", "Charge", "Mass Error", "Confidence", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        psmTable.setOpaque(false);
        psmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                psmTableMouseReleased(evt);
            }
        });
        psmTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                psmTableMouseMoved(evt);
            }
        });
        psmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmTableKeyReleased(evt);
            }
        });
        spectraScrollPane.setViewportView(psmTable);

        javax.swing.GroupLayout psmsPanelLayout = new javax.swing.GroupLayout(psmsPanel);
        psmsPanel.setLayout(psmsPanelLayout);
        psmsPanelLayout.setHorizontalGroup(
            psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 438, Short.MAX_VALUE)
            .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(psmsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        psmsPanelLayout.setVerticalGroup(
            psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 143, Short.MAX_VALUE)
            .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(psmsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        psmsPanel.setBounds(0, 0, 450, 170);
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
        psmsHelpJButton.setBounds(430, 0, 10, 25);
        psmsLayeredPane.add(psmsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPsmsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPsmsJButton.setToolTipText("Copy to Clipboard");
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
        exportPsmsJButton.setBounds(420, 0, 10, 25);
        psmsLayeredPane.add(exportPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        hidePeptideAndPsmsJButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide_grey.png"))); // NOI18N
        hidePeptideAndPsmsJButton2.setToolTipText("Hide Peptides & PSMs (Shift+Ctrl+E)");
        hidePeptideAndPsmsJButton2.setBorder(null);
        hidePeptideAndPsmsJButton2.setBorderPainted(false);
        hidePeptideAndPsmsJButton2.setContentAreaFilled(false);
        hidePeptideAndPsmsJButton2.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide.png"))); // NOI18N
        hidePeptideAndPsmsJButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                hidePeptideAndPsmsJButton2MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                hidePeptideAndPsmsJButton2MouseExited(evt);
            }
        });
        hidePeptideAndPsmsJButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hidePeptideAndPsmsJButton2ActionPerformed(evt);
            }
        });
        hidePeptideAndPsmsJButton2.setBounds(410, 0, 10, 25);
        psmsLayeredPane.add(hidePeptideAndPsmsJButton2, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPsmsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPsmsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPsmsBackgroundPanel);
        contextMenuPsmsBackgroundPanel.setLayout(contextMenuPsmsBackgroundPanelLayout);
        contextMenuPsmsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuPsmsBackgroundPanelLayout.setVerticalGroup(
            contextMenuPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuPsmsBackgroundPanel.setBounds(400, 0, 40, 20);
        psmsLayeredPane.add(contextMenuPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout psmJPanelLayout = new javax.swing.GroupLayout(psmJPanel);
        psmJPanel.setLayout(psmJPanelLayout);
        psmJPanelLayout.setHorizontalGroup(
            psmJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
        );
        psmJPanelLayout.setVerticalGroup(
            psmJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
        );

        peptidesPsmJSplitPane.setRightComponent(psmJPanel);

        peptidesPsmSpectrumFragmentIonsJSplitPane.setLeftComponent(peptidesPsmJSplitPane);

        spectrumMainJPanel.setOpaque(false);

        spectrumMainPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum & Fragment Ions"));
        spectrumMainPanel.setOpaque(false);

        slidersSplitPane.setBorder(null);
        slidersSplitPane.setDividerLocation(430);
        slidersSplitPane.setDividerSize(0);
        slidersSplitPane.setOpaque(false);

        spectrumJTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
        spectrumJTabbedPane.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                spectrumJTabbedPaneMouseWheelMoved(evt);
            }
        });
        spectrumJTabbedPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                spectrumJTabbedPaneMouseEntered(evt);
            }
        });
        spectrumJTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spectrumJTabbedPaneStateChanged(evt);
            }
        });

        fragmentIonJPanel.setBackground(new java.awt.Color(255, 255, 255));

        fragmentIonsJScrollPane.setOpaque(false);
        fragmentIonsJScrollPane.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                fragmentIonsJScrollPaneMouseWheelMoved(evt);
            }
        });

        ionTableJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        ionTableJToolBar.setBorder(null);
        ionTableJToolBar.setFloatable(false);
        ionTableJToolBar.setRollover(true);
        ionTableJToolBar.setBorderPainted(false);

        ionTableAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(ionTableAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        ionTableJToolBar.add(ionTableAnnotationMenuPanel);

        javax.swing.GroupLayout fragmentIonJPanelLayout = new javax.swing.GroupLayout(fragmentIonJPanel);
        fragmentIonJPanel.setLayout(fragmentIonJPanelLayout);
        fragmentIonJPanelLayout.setHorizontalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
                    .addComponent(ionTableJToolBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE))
                .addContainerGap())
        );
        fragmentIonJPanelLayout.setVerticalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ionTableJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumJTabbedPane.addTab("Ion Table", fragmentIonJPanel);

        bubblePlotTabJPanel.setBackground(new java.awt.Color(255, 255, 255));

        bubbleJPanel.setOpaque(false);
        bubbleJPanel.setLayout(new javax.swing.BoxLayout(bubbleJPanel, javax.swing.BoxLayout.LINE_AXIS));

        bubblePlotJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        bubblePlotJToolBar.setBorder(null);
        bubblePlotJToolBar.setFloatable(false);
        bubblePlotJToolBar.setRollover(true);
        bubblePlotJToolBar.setBorderPainted(false);

        bubbleAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(bubbleAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        bubblePlotJToolBar.add(bubbleAnnotationMenuPanel);

        javax.swing.GroupLayout bubblePlotTabJPanelLayout = new javax.swing.GroupLayout(bubblePlotTabJPanel);
        bubblePlotTabJPanel.setLayout(bubblePlotTabJPanelLayout);
        bubblePlotTabJPanelLayout.setHorizontalGroup(
            bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(bubblePlotJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(bubbleJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE))
        );
        bubblePlotTabJPanelLayout.setVerticalGroup(
            bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                .addContainerGap(284, Short.MAX_VALUE)
                .addComponent(bubblePlotJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                    .addComponent(bubbleJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                    .addGap(25, 25, 25)))
        );

        spectrumJTabbedPane.addTab("Bubble Plot", bubblePlotTabJPanel);

        spectrumJPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(spectrumAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumJToolBar.add(spectrumAnnotationMenuPanel);

        spectrumSplitPane.setBorder(null);
        spectrumSplitPane.setDividerLocation(80);
        spectrumSplitPane.setDividerSize(0);
        spectrumSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spectrumSplitPane.setOpaque(false);

        secondarySpectrumPlotsJPanel.setOpaque(false);
        secondarySpectrumPlotsJPanel.setLayout(new javax.swing.BoxLayout(secondarySpectrumPlotsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumSplitPane.setTopComponent(secondarySpectrumPlotsJPanel);

        spectrumPanel.setOpaque(false);
        spectrumPanel.setLayout(new java.awt.BorderLayout());
        spectrumSplitPane.setRightComponent(spectrumPanel);

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumJTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumJTabbedPane.setSelectedIndex(2);

        slidersSplitPane.setLeftComponent(spectrumJTabbedPane);

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
        intensitySlider.setToolTipText("Annotation Level");
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
                    .addComponent(accuracySlider, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(intensitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );
        slidersPanelLayout.setVerticalGroup(
            slidersPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slidersPanelLayout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addGap(29, 29, 29)
                .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addGap(66, 66, 66))
        );

        slidersSplitPane.setRightComponent(slidersPanel);

        javax.swing.GroupLayout spectrumMainPanelLayout = new javax.swing.GroupLayout(spectrumMainPanel);
        spectrumMainPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
            spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slidersSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumMainPanelLayout.setVerticalGroup(
            spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                .addComponent(slidersSplitPane)
                .addContainerGap())
        );

        spectrumMainPanel.setBounds(0, 0, 490, 350);
        spectrumLayeredPane.add(spectrumMainPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
        spectrumHelpJButton.setBounds(460, 0, 10, 25);
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
        exportSpectrumJButton.setBounds(450, 0, 10, 25);
        spectrumLayeredPane.add(exportSpectrumJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        hideSpectrumPanelJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide_grey.png"))); // NOI18N
        hideSpectrumPanelJButton.setToolTipText("Hide Spectrum (Shift+Ctrl+E)");
        hideSpectrumPanelJButton.setBorder(null);
        hideSpectrumPanelJButton.setBorderPainted(false);
        hideSpectrumPanelJButton.setContentAreaFilled(false);
        hideSpectrumPanelJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hide.png"))); // NOI18N
        hideSpectrumPanelJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                hideSpectrumPanelJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                hideSpectrumPanelJButtonMouseExited(evt);
            }
        });
        hideSpectrumPanelJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideSpectrumPanelJButtonActionPerformed(evt);
            }
        });
        hideSpectrumPanelJButton.setBounds(440, 0, 10, 25);
        spectrumLayeredPane.add(hideSpectrumPanelJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        maximizeSpectrumPanelJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/maximize_grey.png"))); // NOI18N
        maximizeSpectrumPanelJButton.setToolTipText("Maximize Spectrum (Shift+Alt+E)");
        maximizeSpectrumPanelJButton.setBorder(null);
        maximizeSpectrumPanelJButton.setBorderPainted(false);
        maximizeSpectrumPanelJButton.setContentAreaFilled(false);
        maximizeSpectrumPanelJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/maximize.png"))); // NOI18N
        maximizeSpectrumPanelJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                maximizeSpectrumPanelJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                maximizeSpectrumPanelJButtonMouseExited(evt);
            }
        });
        maximizeSpectrumPanelJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maximizeSpectrumPanelJButtonActionPerformed(evt);
            }
        });
        maximizeSpectrumPanelJButton.setBounds(425, 5, 10, 20);
        spectrumLayeredPane.add(maximizeSpectrumPanelJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSpectrumBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSpectrumBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSpectrumBackgroundPanel);
        contextMenuSpectrumBackgroundPanel.setLayout(contextMenuSpectrumBackgroundPanelLayout);
        contextMenuSpectrumBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSpectrumBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 60, Short.MAX_VALUE)
        );
        contextMenuSpectrumBackgroundPanelLayout.setVerticalGroup(
            contextMenuSpectrumBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        contextMenuSpectrumBackgroundPanel.setBounds(420, 0, 60, 20);
        spectrumLayeredPane.add(contextMenuSpectrumBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout spectrumMainJPanelLayout = new javax.swing.GroupLayout(spectrumMainJPanel);
        spectrumMainJPanel.setLayout(spectrumMainJPanelLayout);
        spectrumMainJPanelLayout.setHorizontalGroup(
            spectrumMainJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumLayeredPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 510, Short.MAX_VALUE)
        );
        spectrumMainJPanelLayout.setVerticalGroup(
            spectrumMainJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
        );

        peptidesPsmSpectrumFragmentIonsJSplitPane.setRightComponent(spectrumMainJPanel);

        coverageJSplitPane.setLeftComponent(peptidesPsmSpectrumFragmentIonsJSplitPane);

        overviewJSplitPane.setRightComponent(coverageJSplitPane);

        javax.swing.GroupLayout overviewJPanelLayout = new javax.swing.GroupLayout(overviewJPanel);
        overviewJPanel.setLayout(overviewJPanelLayout);
        overviewJPanelLayout.setHorizontalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 960, Short.MAX_VALUE)
                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 698, Short.MAX_VALUE)
                .addContainerGap())
        );

        overviewJPanel.setBounds(0, 0, 980, 720);
        backgroundLayeredPane.add(overviewJPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        toolBar.setBackground(new java.awt.Color(255, 255, 255));
        toolBar.setBorder(null);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        showProteinsBeforeSeparator.setOrientation(javax.swing.SwingConstants.VERTICAL);
        showProteinsBeforeSeparator.setOpaque(true);
        toolBar.add(showProteinsBeforeSeparator);

        showProteinsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/list2.gif"))); // NOI18N
        showProteinsJButton.setText("Proteins");
        showProteinsJButton.setToolTipText("Click to Show (Shift+Ctrl+P)");
        showProteinsJButton.setFocusable(false);
        showProteinsJButton.setOpaque(false);
        showProteinsJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showProteinsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showProteinsJButtonActionPerformed(evt);
            }
        });
        toolBar.add(showProteinsJButton);

        showProteinsAfterSeparator.setOrientation(javax.swing.SwingConstants.VERTICAL);
        showProteinsAfterSeparator.setOpaque(true);
        toolBar.add(showProteinsAfterSeparator);

        showPeptidesAndPsmsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/list2.gif"))); // NOI18N
        showPeptidesAndPsmsJButton.setText("Peptides & PSMs");
        showPeptidesAndPsmsJButton.setToolTipText("Click to Show (Shift+Ctrl+E)");
        showPeptidesAndPsmsJButton.setFocusable(false);
        showPeptidesAndPsmsJButton.setOpaque(false);
        showPeptidesAndPsmsJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showPeptidesAndPsmsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showPeptidesAndPsmsJButtonActionPerformed(evt);
            }
        });
        toolBar.add(showPeptidesAndPsmsJButton);

        showPeptidesAfterSeparator.setOrientation(javax.swing.SwingConstants.VERTICAL);
        showPeptidesAfterSeparator.setOpaque(true);
        toolBar.add(showPeptidesAfterSeparator);

        showSpectrumJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/spectrum.GIF"))); // NOI18N
        showSpectrumJButton.setText("Spectrum & Fragment Ions");
        showSpectrumJButton.setToolTipText("Click to Show (Shift+Ctrl+S)");
        showSpectrumJButton.setFocusable(false);
        showSpectrumJButton.setOpaque(false);
        showSpectrumJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showSpectrumJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showSpectrumJButtonActionPerformed(evt);
            }
        });
        toolBar.add(showSpectrumJButton);

        showSpectrumAfterSeparator.setOrientation(javax.swing.SwingConstants.VERTICAL);
        showSpectrumAfterSeparator.setOpaque(true);
        toolBar.add(showSpectrumAfterSeparator);

        showCoverageJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/coverage3.gif"))); // NOI18N
        showCoverageJButton.setText("Protein Sequence Coverage");
        showCoverageJButton.setToolTipText("Click to Show (Shift+Ctrl+C)");
        showCoverageJButton.setFocusable(false);
        showCoverageJButton.setOpaque(false);
        showCoverageJButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showCoverageJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showCoverageJButtonActionPerformed(evt);
            }
        });
        toolBar.add(showCoverageJButton);

        showCoverageAfterSeparator.setOrientation(javax.swing.SwingConstants.VERTICAL);
        showCoverageAfterSeparator.setOpaque(true);
        toolBar.add(showCoverageAfterSeparator);

        toolBar.setBounds(0, 720, 980, 20);
        backgroundLayeredPane.add(toolBar, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 977, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(backgroundLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 977, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 742, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(backgroundLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 742, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @see #proteinTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            peptideShakerGUI.resetSelectedItems();
            proteinTableMouseReleased(null);
        }
}//GEN-LAST:event_proteinTableKeyReleased

    /**
     * Updates tha tables according to the currently selected peptide.
     *
     * @param evt
     */
    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased

        if (evt != null) {
            peptideShakerGUI.resetSelectedItems();
        }
        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) {
            int row = peptideTable.getSelectedRow();

            if (row != -1) {
                updatePsmSelection(row);

                // invoke later to give time for components to update
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        int row = psmTable.getSelectedRow();
                        updateSpectrum(row, true);
                    }
                });

                // remember the selection
                newItemSelection();
            }
        }
}//GEN-LAST:event_peptideTableKeyReleased

    /**
     * Updates the tables according to the currently selected PSM.
     *
     * @param evt
     */
    private void psmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmTableKeyReleased

        if (evt == null || evt.getKeyCode() != KeyEvent.VK_RIGHT || evt.getKeyCode() == KeyEvent.VK_LEFT) {

            int row = psmTable.getSelectedRow();

            if (row != -1) {
                updateSpectrum(row, false);
                newItemSelection();
            }
        }
}//GEN-LAST:event_psmTableKeyReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * html link.
     *
     * @param evt
     */
    private void proteinTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseMoved
        int row = proteinTable.rowAtPoint(evt.getPoint());
        int column = proteinTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && column == proteinTable.getColumn("Accession").getModelIndex() && proteinTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == proteinTable.getColumn("PI").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinTableMouseMoved

    /**
     * Changes the cursor back to the default cursor a hand.
     *
     * @param evt
     */
    private void proteinTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinTableMouseExited

    /**
     * Updates the spectrum, bubble plot and ion table.
     * 
     * @param evt 
     */
    private void psmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseReleased

        int row = psmTable.getSelectedRow();
        int column = psmTable.getSelectedColumn();

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            updateSpectrum(row, false);

            newItemSelection();

            if (column == psmTable.getColumn("  ").getModelIndex()) {
                String key = psmTableMap.get(getProteinIndex(row));
                if ((Boolean) psmTable.getValueAt(row, column)) {
                    peptideShakerGUI.starPsm(key);
                } else {
                    peptideShakerGUI.unStarPsm(key);
                }
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_psmTableMouseReleased

    /**
     * Updates the tables according to the currently selected protein.
     *
     * @param evt
     */
    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased

        if (evt != null) {
            peptideShakerGUI.resetSelectedItems();
        }

        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        if (evt == null || (evt.getButton() == MouseEvent.BUTTON1 && (row != -1 && column != -1))) {

            if (row != -1) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                // update the peptide selection
                updatedPeptideSelection(row);

                // update the sequence coverage map
                updateSequenceCoverageMap(row);

                // remember the selection
                newItemSelection();

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                if (column == proteinTable.getColumn("  ").getModelIndex()) {
                    String key = proteinTableMap.get(getProteinIndex(row));
                    if ((Boolean) proteinTable.getValueAt(row, column)) {
                        peptideShakerGUI.starProtein(key);
                    } else {
                        peptideShakerGUI.unStarProtein(key);
                    }
                }

                // open protein link in web browser
                if (column == proteinTable.getColumn("Accession").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) proteinTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                    String link = (String) proteinTable.getValueAt(row, column);
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }

                // open the protein inference dialog
                if (column == proteinTable.getColumn("PI").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1) {
                    String proteinKey = proteinTableMap.get(getProteinIndex(row));
                    new ProteinInferenceDialog(peptideShakerGUI, proteinKey, peptideShakerGUI.getIdentification());
                }
            }
        } else if (evt.getButton() == MouseEvent.BUTTON3) {
            if (proteinTable.columnAtPoint(evt.getPoint()) == proteinTable.getColumn("  ").getModelIndex()) {
                selectJPopupMenu.show(proteinTable, evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_proteinTableMouseReleased

    /**
     * Updates the tables according to the currently selected peptide.
     *
     * @param evt
     */
    private void peptideTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseReleased

        if (evt != null) {
            peptideShakerGUI.resetSelectedItems();
        }

        int row = peptideTable.getSelectedRow();
        int column = peptideTable.getSelectedColumn();

        if (row != -1) {

            updatePsmSelection(row);

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    int row = psmTable.getSelectedRow();
                    updateSpectrum(row, true);
                }
            });

            // remember the selection
            newItemSelection();

            if (column == peptideTable.getColumn("  ").getModelIndex()) {
                String key = peptideTableMap.get(getProteinIndex(row));
                if ((Boolean) peptideTable.getValueAt(row, column)) {
                    peptideShakerGUI.starPeptide(key);
                } else {
                    peptideShakerGUI.unStarPeptide(key);
                }
            }

            // open the protein inference at the petide level dialog
            if (column == peptideTable.getColumn("PI").getModelIndex()) {
                try {
                    String proteinKey = proteinTableMap.get(getProteinIndex(proteinTable.getSelectedRow()));
                    String peptideKey = peptideTableMap.get(getPeptideIndex(row));

                    new ProteinInferencePeptideLevelDialog(peptideShakerGUI, true, peptideKey, proteinKey);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }
        }
    }//GEN-LAST:event_peptideTableMouseReleased

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptideTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptideTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an
     * HTML link. Or shows a tooltip with modification details is over 
     * the sequence column.
     *
     * @param evt
     */
    private void peptideTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseMoved
        int row = peptideTable.rowAtPoint(evt.getPoint());
        int column = peptideTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && peptideTable.getValueAt(row, column) != null) {
            if (column == peptideTable.getColumn("PI").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                peptideTable.setToolTipText(null);
            } else if (column == peptideTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // check if we ought to show a tooltip with mod details
                String sequence = (String) peptideTable.getValueAt(row, column);

                if (sequence.indexOf("<span") != -1) {
                    try {
                        String peptideKey = peptideTableMap.get(getPeptideIndex(row));
                        Peptide peptide = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide();
                        String tooltip = peptideShakerGUI.getPeptideModificationTooltipAsHtml(peptide);
                        peptideTable.setToolTipText(tooltip);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        e.printStackTrace();
                    }
                } else {
                    peptideTable.setToolTipText(null);
                }
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptideTable.setToolTipText(null);
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            peptideTable.setToolTipText(null);
        }
    }//GEN-LAST:event_peptideTableMouseMoved

    /**
     * Try to get the protein sequence index from the protein sequence model.
     * 
     * @param evt 
     */
    private void coverageTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageTableMouseMoved

        if (currentProteinSequence != null) {

            int residueNumber = convertPointToResidueNumber(evt.getPoint().getX());

            String tooltipText = "<html>";

            for (int i = 0; i < peptideTable.getRowCount(); i++) {
                if (residueNumber >= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("Start").getModelIndex())
                        && residueNumber <= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("End").getModelIndex())
                        && (Boolean) peptideTable.getValueAt(i, peptideTable.getColumnCount() - 1)) {

                    String peptideKey = peptideTableMap.get(getPeptideIndex(i));
                    String modifiedSequence = "";

                    try {
                        modifiedSequence = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), false);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        e.printStackTrace();
                    }

                    tooltipText += peptideTable.getValueAt(i, peptideTable.getColumn("Start").getModelIndex()) + " - "
                            + modifiedSequence
                            + " - " + peptideTable.getValueAt(i, peptideTable.getColumn("End").getModelIndex()) + "<br>";
                }
            }

            if (!tooltipText.equalsIgnoreCase("<html>")) {
                coverageTable.setToolTipText(tooltipText);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                coverageTable.setToolTipText(null);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_coverageTableMouseMoved

    /**
     * Move the annotation menu bar.
     * 
     * @param evt 
     */
private void spectrumJTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spectrumJTabbedPaneStateChanged

    if (peptideShakerGUI.getAnnotationMenuBar() != null) {

        int index = spectrumJTabbedPane.getSelectedIndex();

        if (index == 0) {
            ionTableAnnotationMenuPanel.removeAll();
            ionTableAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
            peptideShakerGUI.updateAnnotationMenuBarVisableOptions(false, false, true, false);
        } else if (index == 1) {
            bubbleAnnotationMenuPanel.removeAll();
            bubbleAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
            peptideShakerGUI.updateAnnotationMenuBarVisableOptions(false, true, false, false);
        } else if (index == 2) {
            spectrumAnnotationMenuPanel.removeAll();
            spectrumAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
            peptideShakerGUI.updateAnnotationMenuBarVisableOptions(true, false, false, false);
        }
    }
}//GEN-LAST:event_spectrumJTabbedPaneStateChanged

    /**
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
private void spectrumJTabbedPaneMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_spectrumJTabbedPaneMouseWheelMoved

    // @TODO: figure out why the strange special cases are needed... if not included the slider gets stuck at the given values

    if (evt.isControlDown()) {
        if (evt.getWheelRotation() > 0) { // Down
            accuracySlider.setValue(accuracySlider.getValue() - 1);
        } else { // Up
            if (accuracySlider.getValue() == 28) {
                accuracySlider.setValue(accuracySlider.getValue() + 2);
            } else if (accuracySlider.getValue() == 56) {
                accuracySlider.setValue(accuracySlider.getValue() + 3);
            } else {
                accuracySlider.setValue(accuracySlider.getValue() + 1);
            }
        }
    } else {
        if (evt.getWheelRotation() > 0) { // Down
            intensitySlider.setValue(intensitySlider.getValue() - 1);
        } else { // Up
            if (intensitySlider.getValue() == 28) {
                intensitySlider.setValue(intensitySlider.getValue() + 2);
            } else if (intensitySlider.getValue() == 56) {
                intensitySlider.setValue(intensitySlider.getValue() + 3);
            } else {
                intensitySlider.setValue(intensitySlider.getValue() + 1);
            }
        }
    }

    updateSpectrumSliderToolTip();
}//GEN-LAST:event_spectrumJTabbedPaneMouseWheelMoved

    /**
     * Updates the intensity annotation limit.
     * 
     * @param evt 
     */
private void intensitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_intensitySliderStateChanged
    peptideShakerGUI.getAnnotationPreferences().setAnnotationLevel(intensitySlider.getValue() / 100.0);
    peptideShakerGUI.updateSpectrumAnnotations();
    peptideShakerGUI.setDataSaved(false);
    intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");
    updateSpectrumSliderToolTip();
}//GEN-LAST:event_intensitySliderStateChanged

    /**
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
private void intensitySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_intensitySliderMouseWheelMoved
    spectrumJTabbedPaneMouseWheelMoved(evt);
}//GEN-LAST:event_intensitySliderMouseWheelMoved

    /**
     * Opens the selected peptide in the coverage table.
     * 
     * @param evt 
     */
private void coverageTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageTableMouseClicked

    if (currentProteinSequence != null) {

        if (evt.getButton() == MouseEvent.BUTTON1) {

            int residueNumber = convertPointToResidueNumber(evt.getPoint().getX());

            ArrayList<Integer> peptideIndexes = new ArrayList<Integer>();

            for (int i = 0; i < peptideTable.getRowCount(); i++) {
                if (residueNumber >= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("Start").getModelIndex())
                        && residueNumber <= (Integer) peptideTable.getValueAt(i, peptideTable.getColumn("End").getModelIndex())
                        && (Boolean) peptideTable.getValueAt(i, peptideTable.getColumnCount() - 1)) {
                    peptideIndexes.add(i);
                }
            }

            if (!peptideIndexes.isEmpty()) {

                if (peptideIndexes.size() == 1) {
                    peptideTable.setRowSelectionInterval(peptideIndexes.get(0), peptideIndexes.get(0));
                    peptideTable.scrollRectToVisible(peptideTable.getCellRect(peptideIndexes.get(0), peptideIndexes.get(0), false));
                    peptideShakerGUI.resetSelectedItems();
                    peptideTableKeyReleased(null);
                } else {
                    JPopupMenu peptidesPopupMenu = new JPopupMenu();

                    // needs to be made final to be used below
                    final ArrayList<Integer> tempPeptideIndexes = peptideIndexes;

                    for (int i = 0; i < tempPeptideIndexes.size(); i++) {

                        String peptideKey = peptideTableMap.get(getPeptideIndex(tempPeptideIndexes.get(i)));
                        String modifiedSequence = "";

                        try {
                            modifiedSequence = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide().getModifiedSequenceAsHtml(
                                    peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), false);
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                            e.printStackTrace();
                        }

                        String text = "<html>" + (i + 1) + ": " + peptideTable.getValueAt(tempPeptideIndexes.get(i), peptideTable.getColumn("Start").getModelIndex()) + " - "
                                + modifiedSequence
                                + " - " + peptideTable.getValueAt(tempPeptideIndexes.get(i), peptideTable.getColumn("End").getModelIndex())
                                + "</html>";

                        final int tempInt = i;

                        JMenuItem menuItem = new JMenuItem(text);
                        menuItem.addActionListener(new java.awt.event.ActionListener() {

                            public void actionPerformed(java.awt.event.ActionEvent evt) {
                                peptideTable.setRowSelectionInterval(tempPeptideIndexes.get(tempInt), tempPeptideIndexes.get(tempInt));
                                peptideTable.scrollRectToVisible(peptideTable.getCellRect(tempPeptideIndexes.get(tempInt), tempPeptideIndexes.get(tempInt), false));
                                peptideShakerGUI.resetSelectedItems();
                                peptideTableKeyReleased(null);
                            }
                        });

                        peptidesPopupMenu.add(menuItem);
                    }

                    peptidesPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }
        } else if (evt.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu peptidesPopupMenu = new JPopupMenu();

            JMenuItem menuItem = new JMenuItem("Export Sequence");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {

                    String proteinKey = proteinTableMap.get(getProteinIndex(proteinTable.getSelectedRow()));

                    try {
                        Protein protein = sequenceFactory.getProtein(proteinKey);
                        new ExportFeatureDialog(peptideShakerGUI, true, protein.getSequence(), "Sequence", true);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        e.printStackTrace();
                    }
                }
            });

            peptidesPopupMenu.add(menuItem);
            peptidesPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }
}//GEN-LAST:event_coverageTableMouseClicked

    /**
     * Switch the mouse cursor back to the default cursor.
     * 
     * @param evt 
     */
private void coverageTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coverageTableMouseExited
    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_coverageTableMouseExited

    /**
     * See if we ought to show a tooltip with modification details for the 
     * sequence column.
     * 
     * @param evt 
     */
    private void psmTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseMoved

        int row = psmTable.rowAtPoint(evt.getPoint());
        int column = psmTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && psmTable.getValueAt(row, column) != null) {
            if (column == psmTable.getColumn("Sequence").getModelIndex()) {

                // check if we ought to show a tooltip with mod details
                String sequence = (String) psmTable.getValueAt(row, column);

                if (sequence.indexOf("<span") != -1) {
                    try {
                        String peptideKey = peptideTableMap.get(getPeptideIndex(peptideTable.getSelectedRow()));
                        PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);

                        String spectrumKey = psmTableMap.get(getPsmIndex(row));
                        SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                        PeptideAssumption peptideAssumption = spectrumMatch.getBestAssumption();

                        if (peptideAssumption.getPeptide().isSameAs(currentPeptideMatch.getTheoreticPeptide())) {
                            Peptide peptide = peptideAssumption.getPeptide();
                            String tooltip = peptideShakerGUI.getPeptideModificationTooltipAsHtml(peptide);
                            psmTable.setToolTipText(tooltip);
                        } else {
                            // @TODO: do we have to do anything here??
                        }
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        e.printStackTrace();
                    }
                } else {
                    psmTable.setToolTipText(null);
                }
            } else {
                psmTable.setToolTipText(null);
            }
        } else {
            psmTable.setToolTipText(null);
        }
    }//GEN-LAST:event_psmTableMouseMoved

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
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
    private void accuracySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_accuracySliderMouseWheelMoved
        spectrumJTabbedPaneMouseWheelMoved(evt);
    }//GEN-LAST:event_accuracySliderMouseWheelMoved

    /**
     * Update the layered panes.
     * 
     * @param evt 
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        boolean nonHidden = updateHiddenPanels();

        if (nonHidden) {
            // resize the background panel
            backgroundLayeredPane.getComponent(0).setBounds(0, 0, backgroundLayeredPane.getWidth(), backgroundLayeredPane.getHeight());
            backgroundLayeredPane.revalidate();
            backgroundLayeredPane.repaint();

            backgroundLayeredPane.getComponent(1).setBounds(10, backgroundLayeredPane.getHeight() - 25, backgroundLayeredPane.getWidth(), 25);
            backgroundLayeredPane.revalidate();
            backgroundLayeredPane.repaint();
        } else {
            // resize the background panel
            backgroundLayeredPane.getComponent(0).setBounds(0, 0, backgroundLayeredPane.getWidth(), backgroundLayeredPane.getHeight() - 15);
            backgroundLayeredPane.revalidate();
            backgroundLayeredPane.repaint();

            backgroundLayeredPane.getComponent(1).setBounds(10, backgroundLayeredPane.getHeight() - 25, backgroundLayeredPane.getWidth(), 25);
            backgroundLayeredPane.revalidate();
            backgroundLayeredPane.repaint();
        }

        // resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                // move the icons
                proteinsLayeredPane.getComponent(0).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        proteinsLayeredPane.getComponent(0).getWidth(),
                        proteinsLayeredPane.getComponent(0).getHeight());

                proteinsLayeredPane.getComponent(1).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        proteinsLayeredPane.getComponent(1).getWidth(),
                        proteinsLayeredPane.getComponent(1).getHeight());

                proteinsLayeredPane.getComponent(2).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(2).getWidth() - 32,
                        -5,
                        proteinsLayeredPane.getComponent(2).getWidth(),
                        proteinsLayeredPane.getComponent(2).getHeight());

                proteinsLayeredPane.getComponent(3).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        proteinsLayeredPane.getComponent(3).getWidth(),
                        proteinsLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                proteinsLayeredPane.getComponent(4).setBounds(0, 0, proteinsLayeredPane.getWidth(), proteinsLayeredPane.getHeight());
                proteinsLayeredPane.revalidate();
                proteinsLayeredPane.repaint();


                // move the icons
                peptidesLayeredPane.getComponent(0).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        peptidesLayeredPane.getComponent(0).getWidth(),
                        peptidesLayeredPane.getComponent(0).getHeight());

                peptidesLayeredPane.getComponent(1).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        peptidesLayeredPane.getComponent(1).getWidth(),
                        peptidesLayeredPane.getComponent(1).getHeight());

                peptidesLayeredPane.getComponent(2).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(2).getWidth() - 32,
                        -5,
                        peptidesLayeredPane.getComponent(2).getWidth(),
                        peptidesLayeredPane.getComponent(2).getHeight());

                peptidesLayeredPane.getComponent(3).setBounds(
                        peptidesLayeredPane.getWidth() - peptidesLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        peptidesLayeredPane.getComponent(3).getWidth(),
                        peptidesLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                peptidesLayeredPane.getComponent(4).setBounds(0, 0, peptidesLayeredPane.getWidth(), peptidesLayeredPane.getHeight());
                peptidesLayeredPane.revalidate();
                peptidesLayeredPane.repaint();


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
                        psmsLayeredPane.getWidth() - psmsLayeredPane.getComponent(2).getWidth() - 32,
                        -5,
                        psmsLayeredPane.getComponent(2).getWidth(),
                        psmsLayeredPane.getComponent(2).getHeight());

                psmsLayeredPane.getComponent(3).setBounds(
                        psmsLayeredPane.getWidth() - psmsLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        psmsLayeredPane.getComponent(3).getWidth(),
                        psmsLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                psmsLayeredPane.getComponent(4).setBounds(0, 0, psmsLayeredPane.getWidth(), psmsLayeredPane.getHeight());
                psmsLayeredPane.revalidate();
                psmsLayeredPane.repaint();


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
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(2).getWidth() - 32,
                        -5,
                        spectrumLayeredPane.getComponent(2).getWidth(),
                        spectrumLayeredPane.getComponent(2).getHeight());

                spectrumLayeredPane.getComponent(3).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(2).getWidth() - 44,
                        0,
                        spectrumLayeredPane.getComponent(3).getWidth(),
                        spectrumLayeredPane.getComponent(3).getHeight());

                spectrumLayeredPane.getComponent(4).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(4).getWidth() - 5,
                        -3,
                        spectrumLayeredPane.getComponent(4).getWidth(),
                        spectrumLayeredPane.getComponent(4).getHeight());

                // resize the plot area
                spectrumLayeredPane.getComponent(5).setBounds(0, 0, spectrumLayeredPane.getWidth(), spectrumLayeredPane.getHeight());
                spectrumLayeredPane.revalidate();
                spectrumLayeredPane.repaint();

                // set the sliders split pane divider location
                if (peptideShakerGUI.getUserPreferences().showSliders()) {
                    slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth() - 30);
                } else {
                    slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth());
                }

                // move the icons
                sequenceCoverageLayeredPane.getComponent(0).setBounds(
                        sequenceCoverageLayeredPane.getWidth() - sequenceCoverageLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        sequenceCoverageLayeredPane.getComponent(0).getWidth(),
                        sequenceCoverageLayeredPane.getComponent(0).getHeight());

                sequenceCoverageLayeredPane.getComponent(1).setBounds(
                        sequenceCoverageLayeredPane.getWidth() - sequenceCoverageLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        sequenceCoverageLayeredPane.getComponent(1).getWidth(),
                        sequenceCoverageLayeredPane.getComponent(1).getHeight());

                sequenceCoverageLayeredPane.getComponent(2).setBounds(
                        sequenceCoverageLayeredPane.getWidth() - sequenceCoverageLayeredPane.getComponent(2).getWidth() - 32,
                        -5,
                        sequenceCoverageLayeredPane.getComponent(2).getWidth(),
                        sequenceCoverageLayeredPane.getComponent(2).getHeight());

                sequenceCoverageLayeredPane.getComponent(3).setBounds(
                        sequenceCoverageLayeredPane.getWidth() - sequenceCoverageLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        sequenceCoverageLayeredPane.getComponent(3).getWidth(),
                        sequenceCoverageLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                sequenceCoverageLayeredPane.getComponent(4).setBounds(0, 0, sequenceCoverageLayeredPane.getWidth(), sequenceCoverageLayeredPane.getHeight());
                sequenceCoverageLayeredPane.revalidate();
                sequenceCoverageLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void proteinsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void proteinsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void proteinsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#Proteins");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void exportProteinsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportProteinsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void exportProteinsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportProteinsJButtonMouseExited

    /**
     * Export the table contents.
     * 
     * @param evt 
     */
    private void exportProteinsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProteinsJButtonActionPerformed
        copyTableContentToClipboard(TableIndex.PROTEIN_TABLE);
    }//GEN-LAST:event_exportProteinsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void peptidesHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void peptidesHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void peptidesHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptidesHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#Peptides");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void exportPeptidesJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPeptidesJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void exportPeptidesJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPeptidesJButtonMouseExited

    /**
     * Export the table contents.
     * 
     * @param evt 
     */
    private void exportPeptidesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPeptidesJButtonActionPerformed
        copyTableContentToClipboard(TableIndex.PEPTIDE_TABLE);
    }//GEN-LAST:event_exportPeptidesJButtonActionPerformed

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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#PSMs");
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
        copyTableContentToClipboard(TableIndex.PSM_TABLE);
    }//GEN-LAST:event_exportPsmsJButtonActionPerformed

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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#Spectrum");
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
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void sequenceCoveragetHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sequenceCoveragetHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_sequenceCoveragetHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void sequenceCoveragetHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sequenceCoveragetHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_sequenceCoveragetHelpJButtonMouseExited

    /**
     * Open the help dialog.
     * 
     * @param evt 
     */
    private void sequenceCoveragetHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sequenceCoveragetHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#SequenceCoverage");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_sequenceCoveragetHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void exportSequenceCoverageContextJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSequenceCoverageContextJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportSequenceCoverageContextJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void exportSequenceCoverageContextJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSequenceCoverageContextJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportSequenceCoverageContextJButtonMouseExited

    /**
     * Export the table contents.
     * 
     * @param evt 
     */
    private void exportSequenceCoverageContextJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSequenceCoverageContextJButtonActionPerformed
        try {
            String proteinKey = proteinTableMap.get(getProteinIndex(proteinTable.getSelectedRow()));
            Protein protein = sequenceFactory.getProtein(proteinKey);

            String clipboardString = protein.getSequence();
            StringSelection stringSelection = new StringSelection(clipboardString);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, peptideShakerGUI);

            JOptionPane.showMessageDialog(peptideShakerGUI, "Protein sequence copied to clipboard.", "Copied to Clipboard", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
    }//GEN-LAST:event_exportSequenceCoverageContextJButtonActionPerformed

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void showProteinsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showProteinsJButtonActionPerformed
        displayProteins = !displayProteins;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_showProteinsJButtonActionPerformed

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void showPeptidesAndPsmsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showPeptidesAndPsmsJButtonActionPerformed
        displayPeptidesAndPSMs = !displayPeptidesAndPSMs;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_showPeptidesAndPsmsJButtonActionPerformed

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void showSpectrumJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showSpectrumJButtonActionPerformed
        displaySpectrum = !displaySpectrum;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_showSpectrumJButtonActionPerformed

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void showCoverageJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showCoverageJButtonActionPerformed
        displayCoverage = !displayCoverage;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_showCoverageJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void hideProteinsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hideProteinsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_hideProteinsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void hideProteinsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hideProteinsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_hideProteinsJButtonMouseExited

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void hideProteinsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideProteinsJButtonActionPerformed
        displayProteins = false;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_hideProteinsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void hidePeptideAndPsmsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hidePeptideAndPsmsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_hidePeptideAndPsmsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void hidePeptideAndPsmsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hidePeptideAndPsmsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_hidePeptideAndPsmsJButtonMouseExited

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void hidePeptideAndPsmsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hidePeptideAndPsmsJButtonActionPerformed
        displayPeptidesAndPSMs = false;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_hidePeptideAndPsmsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void hidePeptideAndPsmsJButton2MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hidePeptideAndPsmsJButton2MouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_hidePeptideAndPsmsJButton2MouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void hidePeptideAndPsmsJButton2MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hidePeptideAndPsmsJButton2MouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_hidePeptideAndPsmsJButton2MouseExited

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void hidePeptideAndPsmsJButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hidePeptideAndPsmsJButton2ActionPerformed
        displayPeptidesAndPSMs = false;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_hidePeptideAndPsmsJButton2ActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void maximizeSpectrumPanelJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_maximizeSpectrumPanelJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_maximizeSpectrumPanelJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void maximizeSpectrumPanelJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_maximizeSpectrumPanelJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_maximizeSpectrumPanelJButtonMouseExited

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void maximizeSpectrumPanelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maximizeSpectrumPanelJButtonActionPerformed
        displayProteins = false;
        displayPeptidesAndPSMs = false;
        displayCoverage = false;
        peptideShakerGUI.getUserPreferences().setShowSliders(false);
        displaySpectrum = true;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_maximizeSpectrumPanelJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void hideCoverageJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hideCoverageJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_hideCoverageJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void hideCoverageJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hideCoverageJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_hideCoverageJButtonMouseExited

    /**
     * Update the display panels options.
     * 
     * @param evt 
     */
    private void hideCoverageJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideCoverageJButtonActionPerformed
        displayCoverage = false;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_hideCoverageJButtonActionPerformed

    /**
     * Select all rows in the current selection column.
     * 
     * @param evt 
     */
    private void selectAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllMenuItemActionPerformed
        for (int i = 0; i < proteinTable.getRowCount(); i++) {
            proteinTable.setValueAt(true, i, proteinTable.getColumn("  ").getModelIndex());

            String key = proteinTableMap.get(getProteinIndex(i));
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(key, psParameter);
            psParameter.setStarred(true);
        }
    }//GEN-LAST:event_selectAllMenuItemActionPerformed

    /**
     * Deselect all rows in the current selection column.
     * 
     * @param evt 
     */
    private void deselectAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deselectAllMenuItemActionPerformed
        for (int i = 0; i < proteinTable.getRowCount(); i++) {
            proteinTable.setValueAt(false, i, proteinTable.getColumn("  ").getModelIndex());

            String key = proteinTableMap.get(getProteinIndex(i));
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(key, psParameter);
            psParameter.setStarred(false);
        }
    }//GEN-LAST:event_deselectAllMenuItemActionPerformed

    /**
     * Export the spectrum to mgf or figure format.
     * 
     * @param evt 
     */
    private void exportSpectrumJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        int index = spectrumJTabbedPane.getSelectedIndex();

        if (index == 0) { // fragment ion
            JMenuItem menuItem = new JMenuItem("Spectrum As MGF");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSpectrumAsMgf();
                }
            });

            popupMenu.add(menuItem);
        } else if (index == 1) { // bubble plot
            JMenuItem menuItem = new JMenuItem("Bubble Plot As Figure");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportBubblePlotAsFigure();
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
        } else if (index == 2) { // spectrum
            JMenuItem menuItem = new JMenuItem("Spectrum As Figure");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSpectrumAsFigure();
                }
            });

            popupMenu.add(menuItem);

            menuItem = new JMenuItem("Sequence Fragmentation");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSequenceFragmentationAsFigure();
                }
            });

            popupMenu.add(menuItem);

            menuItem = new JMenuItem("Intensity Histogram");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportIntensityHistogramAsFigure();
                }
            });

            popupMenu.add(menuItem);

            menuItem = new JMenuItem("Mass Error Plot");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportMassErrorPlotAsFigure();
                }
            });

            popupMenu.add(menuItem);

            popupMenu.add(new JSeparator());

            menuItem = new JMenuItem("Spectrum As MGF");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSpectrumAsMgf();
                }
            });

            popupMenu.add(menuItem);
        }

        popupMenu.show(exportSpectrumJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportSpectrumJButtonMouseReleased

    /**
     * Clear the tooltip text.
     * 
     * @param evt 
     */
    private void spectrumJTabbedPaneMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_spectrumJTabbedPaneMouseEntered
        spectrumJTabbedPane.setToolTipText(null);
    }//GEN-LAST:event_spectrumJTabbedPaneMouseEntered

    /**
     * Updates the slider value when the user scrolls.
     * 
     * @param evt 
     */
    private void fragmentIonsJScrollPaneMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_fragmentIonsJScrollPaneMouseWheelMoved
        spectrumJTabbedPaneMouseWheelMoved(evt);
    }//GEN-LAST:event_fragmentIonsJScrollPaneMouseWheelMoved
    /**
     * Change the cursor to a hand cursor.
     * 
     * @param evt 
     */
    private void hideSpectrumPanelJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hideSpectrumPanelJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_hideSpectrumPanelJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     * 
     * @param evt 
     */
    private void hideSpectrumPanelJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_hideSpectrumPanelJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_hideSpectrumPanelJButtonMouseExited

    private void hideSpectrumPanelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideSpectrumPanelJButtonActionPerformed
        displaySpectrum = false;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_hideSpectrumPanelJButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider accuracySlider;
    private javax.swing.JLayeredPane backgroundLayeredPane;
    private javax.swing.JPanel bubbleAnnotationMenuPanel;
    private javax.swing.JPanel bubbleJPanel;
    private javax.swing.JToolBar bubblePlotJToolBar;
    private javax.swing.JPanel bubblePlotTabJPanel;
    private javax.swing.JPanel contextMenuPeptidesBackgroundPanel;
    private javax.swing.JPanel contextMenuProteinsBackgroundPanel;
    private javax.swing.JPanel contextMenuPsmsBackgroundPanel;
    private javax.swing.JPanel contextMenuSequenceCoverageBackgroundPanel;
    private javax.swing.JPanel contextMenuSpectrumBackgroundPanel;
    private javax.swing.JSplitPane coverageJSplitPane;
    private javax.swing.JTable coverageTable;
    private javax.swing.JMenuItem deselectAllMenuItem;
    private javax.swing.JButton exportPeptidesJButton;
    private javax.swing.JButton exportProteinsJButton;
    private javax.swing.JButton exportPsmsJButton;
    private javax.swing.JButton exportSequenceCoverageContextJButton;
    private javax.swing.JButton exportSpectrumJButton;
    private javax.swing.JPanel fragmentIonJPanel;
    private javax.swing.JScrollPane fragmentIonsJScrollPane;
    private javax.swing.JButton hideCoverageJButton;
    private javax.swing.JButton hidePeptideAndPsmsJButton;
    private javax.swing.JButton hidePeptideAndPsmsJButton2;
    private javax.swing.JButton hideProteinsJButton;
    private javax.swing.JButton hideSpectrumPanelJButton;
    private javax.swing.JSlider intensitySlider;
    private javax.swing.JPanel ionTableAnnotationMenuPanel;
    private javax.swing.JToolBar ionTableJToolBar;
    private javax.swing.JButton maximizeSpectrumPanelJButton;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JSplitPane overviewJSplitPane;
    private javax.swing.JScrollPane peptideScrollPane;
    private javax.swing.JTable peptideTable;
    private javax.swing.JButton peptidesHelpJButton;
    private javax.swing.JPanel peptidesJPanel;
    private javax.swing.JLayeredPane peptidesLayeredPane;
    private javax.swing.JPanel peptidesPanel;
    private javax.swing.JSplitPane peptidesPsmJSplitPane;
    private javax.swing.JSplitPane peptidesPsmSpectrumFragmentIonsJSplitPane;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JButton proteinsHelpJButton;
    private javax.swing.JPanel proteinsJPanel;
    private javax.swing.JLayeredPane proteinsLayeredPane;
    private javax.swing.JPanel proteinsLayeredPanel;
    private javax.swing.JPanel psmJPanel;
    private javax.swing.JTable psmTable;
    private javax.swing.JButton psmsHelpJButton;
    private javax.swing.JLayeredPane psmsLayeredPane;
    private javax.swing.JPanel psmsPanel;
    private javax.swing.JPanel secondarySpectrumPlotsJPanel;
    private javax.swing.JMenuItem selectAllMenuItem;
    private javax.swing.JPopupMenu selectJPopupMenu;
    private javax.swing.JPanel sequenceCoverageJPanel;
    private javax.swing.JLayeredPane sequenceCoverageLayeredPane;
    private javax.swing.JPanel sequenceCoveragePanel;
    private javax.swing.JScrollPane sequenceCoverageTableScrollPane;
    private javax.swing.JButton sequenceCoveragetHelpJButton;
    private javax.swing.JPopupMenu.Separator showCoverageAfterSeparator;
    private javax.swing.JButton showCoverageJButton;
    private javax.swing.JPopupMenu.Separator showPeptidesAfterSeparator;
    private javax.swing.JButton showPeptidesAndPsmsJButton;
    private javax.swing.JPopupMenu.Separator showProteinsAfterSeparator;
    private javax.swing.JPopupMenu.Separator showProteinsBeforeSeparator;
    private javax.swing.JButton showProteinsJButton;
    private javax.swing.JPopupMenu.Separator showSpectrumAfterSeparator;
    private javax.swing.JButton showSpectrumJButton;
    private javax.swing.JPanel slidersPanel;
    private javax.swing.JSplitPane slidersSplitPane;
    private javax.swing.JScrollPane spectraScrollPane;
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
    private javax.swing.JButton spectrumHelpJButton;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JTabbedPane spectrumJTabbedPane;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JLayeredPane spectrumLayeredPane;
    private javax.swing.JPanel spectrumMainJPanel;
    private javax.swing.JPanel spectrumMainPanel;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JSplitPane spectrumSplitPane;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables

    /**
     * Displays or hide sparklines in the tables.
     * 
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);

        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);

        try {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (IllegalArgumentException e) {
            // ignore error
        }

        proteinTable.revalidate();
        proteinTable.repaint();

        peptideTable.revalidate();
        peptideTable.repaint();

        psmTable.revalidate();
        psmTable.repaint();
    }

    /**
     * Returns the index of the protein at the given row in the protein table.
     *
     * @param row the row of interest
     * @return  the index of the corresponding protein
     */
    private Integer getProteinIndex(int row) {

        if (row != -1) {
            return (Integer) proteinTable.getValueAt(row, 0);
        } else {
            return -1;
        }
    }

    /**
     * Returns the index of the peptide at the given row in the peptide table.
     *
     * @param row the row of interest
     * @return  the index of the corresponding peptide
     */
    private Integer getPeptideIndex(int row) {

        if (row != -1) {
            return (Integer) peptideTable.getValueAt(row, 0);
        } else {
            return -1;
        }
    }

    /**
     * Returns the index of the psm at the given row in the psm table.
     *
     * @param row the row of interest
     * @return  the index of the corresponding psm
     */
    private Integer getPsmIndex(int row) {
        return (Integer) psmTable.getValueAt(row, 0);
    }

    /**
     * Returns a list of keys of the displayed proteins
     * @return a list of keys of the displayed proteins 
     */
    public ArrayList<String> getDisplayedProteins() {
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < proteinTable.getRowCount(); i++) {
            result.add(proteinTableMap.get(getProteinIndex(i)));
        }
        return new ArrayList<String>(proteinTableMap.values());
    }

    /**
     * Returns a list of keys of the displayed peptides
     * @return a list of keys of the displayed peptides 
     */
    public ArrayList<String> getDisplayedPeptides() {
        return new ArrayList<String>(peptideTableMap.values());
    }

    /**
     * Returns a list of keys of the displayed PSMs
     * @return a list of keys of the displayed PSMs
     */
    public ArrayList<String> getDisplayedPsms() {
        return new ArrayList<String>(psmTableMap.values());
    }

    /**
     * Updates the split panel divider location for the protein panel.
     */
    private void updateProteinTableSeparator() {

        if (displayProteins) {
            overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight() / 100 * 30);
        } else {
            overviewJSplitPane.setDividerLocation(0);
        }

        if (!displayPeptidesAndPSMs && !displaySpectrum) {
            overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight() / 100 * 70);
            coverageJSplitPane.setDividerLocation(0);
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateSequenceCoverageSeparator();
                peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
                formComponentResized(null);
            }
        });
    }

    /**
     * Updates the split panel divider location for the peptide/psm and spectrum panel.
     */
    private void updatePeptidesAndPsmsSeparator() {

        if (displayPeptidesAndPSMs && displaySpectrum) {
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(peptidesPsmSpectrumFragmentIonsJSplitPane.getWidth() / 2);
        } else if (displayPeptidesAndPSMs && !displaySpectrum) {
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(peptidesPsmSpectrumFragmentIonsJSplitPane.getWidth());
        } else if (!displayPeptidesAndPSMs && displaySpectrum) {
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(0);
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateSequenceCoverageSeparator();
                peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
                formComponentResized(null);
            }
        });
    }

    /**
     * Updates the split panel divider location for the coverage pane.
     */
    private void updateSequenceCoverageSeparator() {

        if (displayCoverage) {
            coverageJSplitPane.setDividerLocation(coverageJSplitPane.getHeight() - 75);
        } else {
            coverageJSplitPane.setDividerLocation(Integer.MAX_VALUE);
        }

        if (!displayPeptidesAndPSMs && !displaySpectrum) {

            if (!displayCoverage) {
                overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight());
            } else {
                overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight() - 75);
                coverageJSplitPane.setDividerLocation(0);
            }
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
                formComponentResized(null);
            }
        });
    }

    /**
     * Method called whenever the component is resized to maintain the look of the GUI.
     */
    public void updateSeparators() {

        formComponentResized(null);
        updateProteinTableSeparator();
        updatePeptidesAndPsmsSeparator();
        peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
        formComponentResized(null);

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateSequenceCoverageSeparator();
                updateProteinTableSeparator();
                updatePeptidesAndPsmsSeparator();
                overviewJPanel.revalidate();
                overviewJPanel.repaint();
                updateBubblePlot();
                formComponentResized(null);
            }
        });

        formComponentResized(null);
    }

    /**
     * Sets the whether the protein coverage and the spectrum shall be displayed
     * 
     * @param displayProteins           boolean indicating whether the proteins shall be displayed
     * @param displayPeptidesAndPSMs    boolean indicating whether the peptides and psms shall be displayed
     * @param displayCoverage           boolean indicating whether the protein coverage shall be displayed
     * @param displaySpectrum           boolean indicating whether the spectrum shall be displayed
     */
    public void setDisplayOptions(boolean displayProteins, boolean displayPeptidesAndPSMs,
            boolean displayCoverage, boolean displaySpectrum) {
        this.displayProteins = displayProteins;
        this.displayPeptidesAndPSMs = displayPeptidesAndPSMs;
        this.displayCoverage = displayCoverage;
        this.displaySpectrum = displaySpectrum;
    }

    /**
     * Updated the bubble plot with the current PSMs.
     */
    public void updateBubblePlot() {

        if (peptideTable.getSelectedRow() != -1 && displaySpectrum) {
            try {
                ArrayList<String> selectedIndexes = new ArrayList<String>();

                // get the list of currently selected psms
                ArrayList<String> selectedPsmKeys = new ArrayList<String>();

                int[] selectedRows = psmTable.getSelectedRows();

                for (int i = 0; i < selectedRows.length; i++) {
                    selectedPsmKeys.add(psmTableMap.get((Integer) psmTable.getValueAt(selectedRows[i], 0)));
                    selectedIndexes.add(psmTable.getValueAt(selectedRows[i], 0) + " "
                            + psmTable.getValueAt(selectedRows[i], psmTable.getColumn("Charge").getModelIndex()) + "+");
                }

                ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<ArrayList<IonMatch>>();
                ArrayList<MSnSpectrum> allSpectra = new ArrayList<MSnSpectrum>();
                SpectrumAnnotator miniAnnotator = new SpectrumAnnotator();

                String peptideKey = peptideTableMap.get(getPeptideIndex(peptideTable.getSelectedRow()));
                PeptideMatch selectedPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();

                for (String spectrumKey : selectedPeptideMatch.getSpectrumMatches()) {

                    if (selectedPsmKeys.contains(spectrumKey)) {

                        MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                        if (currentSpectrum != null) {
                            annotationPreferences.setCurrentSettings(
                                    selectedPeptideMatch.getTheoreticPeptide(),
                                    currentSpectrum.getPrecursor().getCharge().value, !currentSpectrumKey.equalsIgnoreCase(spectrumKey));
                            ArrayList<IonMatch> annotations = miniAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                    annotationPreferences.getNeutralLosses(),
                                    annotationPreferences.getValidatedCharges(),
                                    currentSpectrum,
                                    selectedPeptideMatch.getTheoreticPeptide(),
                                    currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                    annotationPreferences.getFragmentIonAccuracy());
                            allAnnotations.add(annotations);
                            allSpectra.add(currentSpectrum);

                            currentSpectrumKey = spectrumKey;
                        }
                    }
                }

                // @TODO: rewrite the charge selection below when the new ion selection gui has been implemented!

                double bubbleScale = annotationPreferences.getFragmentIonAccuracy() * 10 * peptideShakerGUI.getBubbleScale();

                if (peptideShakerGUI.useRelativeError()) {
                    bubbleScale = annotationPreferences.getFragmentIonAccuracy() * 10000 * peptideShakerGUI.getBubbleScale();
                }

                MassErrorBubblePlot massErrorBubblePlot = new MassErrorBubblePlot(
                        selectedIndexes,
                        allAnnotations, annotationPreferences.getIonTypes(), allSpectra, annotationPreferences.getFragmentIonAccuracy(),
                        bubbleScale,
                        annotationPreferences.getValidatedCharges().contains(new Integer(1)), annotationPreferences.getValidatedCharges().contains(new Integer(2)),
                        annotationPreferences.getValidatedCharges().contains(new Integer(3)) || annotationPreferences.getValidatedCharges().contains(new Integer(4)),
                        selectedIndexes.size() == 1, annotationPreferences.showBars(),
                        peptideShakerGUI.useRelativeError());

                // hide the outline
                massErrorBubblePlot.getChartPanel().getChart().getPlot().setOutlineVisible(false);

                bubbleJPanel.removeAll();
                bubbleJPanel.add(massErrorBubblePlot);
                bubbleJPanel.revalidate();
                bubbleJPanel.repaint();
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Updates the sequence coverage pane.
     *
     * @param proteinAccession
     */
    private void updateSequenceCoverage(String proteinAccession) {

        ArrayList<Integer> selectedPeptideStart = new ArrayList<Integer>();
        ArrayList<Integer> selectedPeptideEnd = new ArrayList<Integer>();

        try {
            currentProteinSequence = sequenceFactory.getProtein(proteinAccession).getSequence();

            ((TitledBorder) sequenceCoveragePanel.getBorder()).setTitle("Protein Sequence Coverage ("
                    + Util.roundDouble((Double) proteinTable.getValueAt(proteinTable.getSelectedRow(), proteinTable.getColumn("Coverage").getModelIndex()), 2)
                    + "%, " + currentProteinSequence.length() + " AA)");
            sequenceCoveragePanel.repaint();

            if (currentProteinSequence.length() < MAX_SEQUENCE_LENGTH) {

                String tempSequence = currentProteinSequence;

                if (peptideTable.getSelectedRow() != -1) {

                    String peptideKey = peptideTableMap.get(getPeptideIndex(peptideTable.getSelectedRow()));
                    String peptideSequence = Peptide.getSequence(peptideKey);

                    int startIndex = 0;
                    while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                        startIndex = tempSequence.lastIndexOf(peptideSequence) + 1;
                        selectedPeptideStart.add(startIndex);
                        selectedPeptideEnd.add(startIndex + peptideSequence.length());
                        tempSequence = currentProteinSequence.substring(0, startIndex);
                    }
                }

                // an array containing the coverage index for each residue
                coverage = new int[currentProteinSequence.length() + 1];

                PSParameter pSParameter = new PSParameter();
                // iterate the peptide table and store the coverage for each validated peptide
                for (int i = 0; i < peptideTable.getRowCount(); i++) {
                    String peptideKey = peptideTableMap.get(getPeptideIndex(i));
                    pSParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(peptideKey, pSParameter);
                    if (pSParameter.isValidated()) {
                        String peptideSequence = Peptide.getSequence(peptideKey);
                        tempSequence = currentProteinSequence;

                        while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                            int peptideTempStart = tempSequence.lastIndexOf(peptideSequence) + 1;
                            int peptideTempEnd = peptideTempStart + peptideSequence.length();
                            for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                                coverage[j]++;
                            }
                            tempSequence = currentProteinSequence.substring(0, peptideTempStart);
                        }
                    }
                }

                // create the coverage plot
                ArrayList<JSparklinesDataSeries> sparkLineDataSeriesCoverage = new ArrayList<JSparklinesDataSeries>();

                for (int i = 0; i < coverage.length; i++) {

                    boolean covered = coverage[i] > 0;
                    int counter = 1;

                    if (covered) {
                        while (i + 1 < coverage.length && coverage[i + 1] > 0) {
                            counter++;
                            i++;

                            // we need to start a new peptide in order to highlight
                            if (selectedPeptideEnd.contains(new Integer(i + 1)) || selectedPeptideStart.contains(new Integer(i + 1))) {
                                break;
                            }
                        }
                    } else {
                        while (i + 1 < coverage.length && coverage[i + 1] == 0) {
                            counter++;
                            i++;
                        }
                    }

                    ArrayList<Double> data = new ArrayList<Double>();
                    data.add(new Double(counter));

                    JSparklinesDataSeries sparklineDataseries;

                    if (covered) {

                        if (selectedPeptideEnd.contains(new Integer(i + 1))) {
                            sparklineDataseries = new JSparklinesDataSeries(data, new Color(255, 0, 0), null);
                        } else {
                            sparklineDataseries = new JSparklinesDataSeries(data, peptideShakerGUI.getSparklineColor(), null);
                        }

                    } else {
                        sparklineDataseries = new JSparklinesDataSeries(data, new Color(0, 0, 0, 0), null);
                    }

                    sparkLineDataSeriesCoverage.add(sparklineDataseries);
                }

                JSparklinesDataset dataset = new JSparklinesDataset(sparkLineDataSeriesCoverage);
                coverageTable.setValueAt(dataset, 0, 0);
            } else {
                ((TitledBorder) sequenceCoveragePanel.getBorder()).setTitle("Protein Sequence Coverage ("
                        + Util.roundDouble((Double) proteinTable.getValueAt(proteinTable.getSelectedRow(), proteinTable.getColumn("Coverage").getModelIndex()), 2)
                        + "%, " + currentProteinSequence.length() + " AA)" + " - Too long to display...");
                sequenceCoveragePanel.repaint();
            }
        } catch (ClassCastException e) {
            // ignore   @TODO: this should not happen, but can happen if the table does not update fast enough for the filtering
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
    }

    /**
     * Updates the spectrum annotation. Used when the user updates the annotation 
     * accuracy.
     */
    public void updateSpectrum() {
        updateSpectrum(psmTable.getSelectedRow(), false);
    }

    /**
     * Update the spectrum to the currently selected PSM.
     *
     * @param row           the row index of the PSM
     * @param resetMzRange  if true the mz range is reset, if false the current zoom range is kept
     */
    private void updateSpectrum(int row, boolean resetMzRange) {

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            String spectrumKey = psmTableMap.get(getPsmIndex(row));

            if (displaySpectrum) {

                try {
                    MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

                    if (currentSpectrum != null) {

                        HashSet<Peak> peaks = currentSpectrum.getPeakList();

                        if (peaks == null || peaks.isEmpty()) {
                            // do nothing, peaks list not found
                        } else {

                            double lowerMzZoomRange = 0;
                            double upperMzZoomRange = maxPsmMzValue;

                            if (spectrum != null && spectrum.getXAxisZoomRangeLowerValue() != 0 && !resetMzRange) {
                                lowerMzZoomRange = spectrum.getXAxisZoomRangeLowerValue();
                                upperMzZoomRange = spectrum.getXAxisZoomRangeUpperValue();
                            }

                            // add the data to the spectrum panel
                            Precursor precursor = currentSpectrum.getPrecursor();
                            spectrum = new SpectrumPanel(
                                    currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                                    precursor.getMz(), precursor.getCharge().toString(),
                                    "", 40, false, false, false, 2, false);
                            spectrum.setKnownMassDeltas(peptideShakerGUI.getCurrentMassDeltas());
                            spectrum.setDeltaMassWindow(peptideShakerGUI.getAnnotationPreferences().getFragmentIonAccuracy());
                            spectrum.setBorder(null);

                            // get the spectrum annotations
                            String peptideKey = peptideTableMap.get(getPeptideIndex(peptideTable.getSelectedRow()));
                            Peptide currentPeptide = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide();
                            SpectrumAnnotator spectrumAnnotator = peptideShakerGUI.getSpectrumAnnorator();
                            AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
                            annotationPreferences.setCurrentSettings(
                                    currentPeptide, currentSpectrum.getPrecursor().getCharge().value,
                                    !currentSpectrumKey.equalsIgnoreCase(spectrumKey));
                            ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                                    annotationPreferences.getNeutralLosses(),
                                    annotationPreferences.getValidatedCharges(),
                                    currentSpectrum, currentPeptide,
                                    currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                                    annotationPreferences.getFragmentIonAccuracy());
                            spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations));
                            spectrum.rescale(lowerMzZoomRange, upperMzZoomRange);

                            if (!currentSpectrumKey.equalsIgnoreCase(spectrumKey)) {
                                if (annotationPreferences.useAutomaticAnnotation()) {
                                    annotationPreferences.setNeutralLossesSequenceDependant(true);
                                }
                            }

                            peptideShakerGUI.updateAnnotationMenus();

                            currentSpectrumKey = spectrumKey;

                            // show all or just the annotated peaks
                            spectrum.showAnnotatedPeaksOnly(!peptideShakerGUI.getAnnotationPreferences().showAllPeaks());

                            spectrum.setYAxisZoomExcludesBackgroundPeaks(peptideShakerGUI.getAnnotationPreferences().yAxisZoomExcludesBackgroundPeaks());

                            // add the spectrum panel to the frame
                            spectrumPanel.removeAll();
                            spectrumPanel.add(spectrum);
                            spectrumPanel.revalidate();
                            spectrumPanel.repaint();

                            // create and display the fragment ion table
                            if (psmTable.getSelectedRowCount() == 1 && !peptideShakerGUI.getAnnotationPreferences().useIntensityIonTable()) {
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, annotations, annotationPreferences.getIonTypes(),
                                        annotationPreferences.getNeutralLosses(),
                                        peptideShakerGUI.getAnnotationPreferences().getValidatedCharges().contains(new Integer(1)),
                                        peptideShakerGUI.getAnnotationPreferences().getValidatedCharges().contains(new Integer(2))));
                            } else {
                                ArrayList<ArrayList<IonMatch>> allAnnotations = getAnnotationsForAllSelectedSpectra();
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, allAnnotations, getSelectedSpectra(), annotationPreferences.getIonTypes(),
                                        annotationPreferences.getNeutralLosses(),
                                        peptideShakerGUI.getAnnotationPreferences().getValidatedCharges().contains(new Integer(1)),
                                        peptideShakerGUI.getAnnotationPreferences().getValidatedCharges().contains(new Integer(2))));
                            }

                            // create the sequence fragment ion view
                            secondarySpectrumPlotsJPanel.removeAll();
                            SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                            PeptideAssumption peptideAssumption = spectrumMatch.getBestAssumption();

                            SequenceFragmentationPanel sequenceFragmentationPanel = new SequenceFragmentationPanel(
                                    peptideAssumption.getPeptide().getModifiedSequenceAsString(true),
                                    annotations, true, true,
                                    peptideAssumption.getPeptide().getPTMShortNameColorMap(peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors()),
                                    peptideAssumption.getPeptide().getPTMShortNameMap());
                            sequenceFragmentationPanel.setMinimumSize(new Dimension(sequenceFragmentationPanel.getPreferredSize().width, sequenceFragmentationPanel.getHeight()));
                            sequenceFragmentationPanel.setOpaque(true);
                            sequenceFragmentationPanel.setBackground(Color.WHITE);
                            secondarySpectrumPlotsJPanel.add(sequenceFragmentationPanel);

                            // create the intensity histograms
                            secondarySpectrumPlotsJPanel.add(new IntensityHistogram(
                                    annotations, annotationPreferences.getIonTypes(), currentSpectrum,
                                    peptideShakerGUI.getAnnotationPreferences().getAnnotationIntensityLimit(),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(1)), annotationPreferences.getValidatedCharges().contains(new Integer(2)),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(3)) || annotationPreferences.getValidatedCharges().contains(new Integer(4))));

                            // @TODO: rewrite the charge selection above and below when the new ion selection gui has been implemented!

                            // create the miniature mass error plot
                            MassErrorPlot massErrorPlot = new MassErrorPlot(
                                    annotations, annotationPreferences.getIonTypes(), currentSpectrum,
                                    annotationPreferences.getFragmentIonAccuracy(),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(1)), annotationPreferences.getValidatedCharges().contains(new Integer(2)),
                                    annotationPreferences.getValidatedCharges().contains(new Integer(3)) || annotationPreferences.getValidatedCharges().contains(new Integer(4)),
                                    peptideShakerGUI.useRelativeError());

                            if (massErrorPlot.getNumberOfDataPointsInPlot() > 0) {
                                secondarySpectrumPlotsJPanel.add(massErrorPlot);
                            }

                            // update the UI
                            secondarySpectrumPlotsJPanel.revalidate();
                            secondarySpectrumPlotsJPanel.repaint();

                            // update the bubble plot
                            updateBubblePlot();

                            // disable the spectrum tab if more than one psm is selected
                            spectrumJTabbedPane.setEnabledAt(2, psmTable.getSelectedRowCount() == 1);
                            peptideShakerGUI.enableSpectrumExport(psmTable.getSelectedRowCount() == 1);

                            // move to the bubble plot tab if more than one psm is selected and the spectrum tab was selected
                            if (psmTable.getSelectedRowCount() > 1 && spectrumJTabbedPane.getSelectedIndex() == 2) {
                                spectrumJTabbedPane.setSelectedIndex(1);
                            }

                            if (psmTable.getSelectedRowCount() > 1) {
                                spectrumJTabbedPane.setToolTipTextAt(2, "Available for single spectrum selection only");
                                peptideShakerGUI.getAnnotationPreferences().setIntensityIonTable(true);
                            } else {
                                spectrumJTabbedPane.setToolTipTextAt(2, null);
                            }

                            // update the panel border title
                            if (psmTable.getSelectedRowCount() == 1) {
                                updateSpectrumPanelBorderTitle(currentPeptide, currentSpectrum);
                            } else {

                                // get the current charges
                                ArrayList<Integer> currentCharges = new ArrayList<Integer>();
                                int[] selectedRows = psmTable.getSelectedRows();

                                for (int i = 0; i < selectedRows.length; i++) {
                                    Integer tempCharge = (Integer) psmTable.getValueAt(selectedRows[i], psmTable.getColumn("Charge").getModelIndex());

                                    if (!currentCharges.contains(tempCharge)) {
                                        currentCharges.add(tempCharge);
                                    }
                                }

                                Collections.sort(currentCharges);

                                String chargeStates = currentCharges.get(0) + "+";

                                for (int i = 1; i < currentCharges.size(); i++) {
                                    chargeStates += " and " + currentCharges.get(i) + "+";
                                }

                                ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                                        "Spectrum & Fragment Ions (" + currentPeptide.getSequence()
                                        + "   " + chargeStates
                                        + "   " + selectedRows.length + " spectra)");
                            }

                            spectrumMainPanel.revalidate();
                            spectrumMainPanel.repaint();
                        }
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        } else {

            // nothing to display, empty previous results
            spectrumPanel.removeAll();
            spectrumPanel.revalidate();
            spectrumPanel.repaint();

            secondarySpectrumPlotsJPanel.removeAll();
            secondarySpectrumPlotsJPanel.revalidate();
            secondarySpectrumPlotsJPanel.repaint();

            fragmentIonsJScrollPane.setViewportView(null);
            fragmentIonsJScrollPane.revalidate();
            fragmentIonsJScrollPane.repaint();

            bubbleJPanel.removeAll();
            bubbleJPanel.revalidate();
            bubbleJPanel.repaint();

            ((TitledBorder) spectrumMainPanel.getBorder()).setTitle("Spectrum & Fragment Ions");
            spectrumMainPanel.repaint();
        }
    }

    /**
     * Update the PSM selection according to the currently selected peptide.
     *
     * @param row the row index of the selected peptide
     */
    private void updatePsmSelection(int row) {

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            try {

                // update the sequence coverage map
                String proteinKey = proteinTableMap.get(getProteinIndex(proteinTable.getSelectedRow()));
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);

                updateSequenceCoverage(proteinMatch.getMainMatch());

                DefaultTableModel dm = (DefaultTableModel) psmTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                spectrumPanel.removeAll();
                spectrumPanel.revalidate();
                spectrumPanel.repaint();

                String peptideKey = peptideTableMap.get(getPeptideIndex(row));

                int index = 1;
                psmTableMap = new HashMap<Integer, String>();

                maxPsmMzValue = Double.MIN_VALUE;

                int validatedPsmCounter = 0;

                PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                SpectrumMatch spectrumMatch;
                for (String spectrumKey : currentPeptideMatch.getSpectrumMatches()) {
                    spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                    PeptideAssumption peptideAssumption = spectrumMatch.getBestAssumption();
                    Precursor precursor = peptideShakerGUI.getPrecursor(spectrumKey);
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(spectrumKey, probabilities);

                    if (!probabilities.isHidden()) {

                        ((DefaultTableModel) psmTable.getModel()).addRow(new Object[]{
                                    index,
                                    probabilities.isStarred(),
                                    SpectrumIdentificationPanel.isBestPsmEqualForAllSearchEngines(spectrumMatch),
                                    peptideAssumption.getPeptide().getModifiedSequenceAsHtml(
                                    peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true),
                                    peptideAssumption.getIdentificationCharge().value,
                                    Math.abs(peptideAssumption.getDeltaMass(precursor.getMz(), peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm())),
                                    probabilities.getPsmConfidence(),
                                    probabilities.isValidated()
                                });

                        psmTableMap.put(index, spectrumKey);
                        index++;

                        if (probabilities.isValidated()) {
                            validatedPsmCounter++;
                        }

                        MSnSpectrum tempSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

                        if (tempSpectrum.getPeakList() != null && maxPsmMzValue < tempSpectrum.getMaxMz()) {
                            maxPsmMzValue = tempSpectrum.getMaxMz();
                        }
                    }
                }

                ((TitledBorder) psmsPanel.getBorder()).setTitle("Peptide-Spectrum Matches (" + validatedPsmCounter + "/" + psmTable.getRowCount() + ")");
                psmsPanel.repaint();

                // select the psm in the table
                if (psmTable.getRowCount() > 0) {
                    int psmRow = 0;
                    String psmKey = peptideShakerGUI.getSelectedPsmKey();
                    if (!psmKey.equals(PeptideShakerGUI.NO_SELECTION)) {
                        psmRow = getPsmRow(psmKey);
                    }

                    if (psmRow != -1) {
                        psmTable.setRowSelectionInterval(psmRow, psmRow);
                        psmTable.scrollRectToVisible(psmTable.getCellRect(psmRow, 0, false));
                        psmTableKeyReleased(null);
                    }
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Updates the peptide selection according to the currently selected protein.
     *
     * @param row the row index of the protein
     */
    private void updatedPeptideSelection(int row) {

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            try {

                DefaultTableModel dm = (DefaultTableModel) peptideTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                dm = (DefaultTableModel) psmTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                spectrumPanel.removeAll();
                spectrumPanel.revalidate();
                spectrumPanel.repaint();

                String proteinKey = proteinTableMap.get(getProteinIndex(row));
                peptideTableMap = new HashMap<Integer, String>();

                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                HashMap<Double, HashMap<Integer, HashMap<String, PeptideMatch>>> peptideMap = new HashMap<Double, HashMap<Integer, HashMap<String, PeptideMatch>>>();
                PSParameter probabilities = new PSParameter();
                PSParameter secondaryPSParameter = new PSParameter();
                double peptideProbabilityScore;
                PeptideMatch peptideMatch;
                int spectrumCount;

                for (String peptideKey : proteinMatch.getPeptideMatches()) {
                    probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(peptideKey, probabilities);
                    peptideProbabilityScore = probabilities.getPeptideProbabilityScore();

                    if (!peptideMap.containsKey(peptideProbabilityScore)) {
                        peptideMap.put(peptideProbabilityScore, new HashMap<Integer, HashMap<String, PeptideMatch>>());
                    }
                    peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                    spectrumCount = peptideMatch.getSpectrumCount();
                    if (!peptideMap.get(peptideProbabilityScore).containsKey(spectrumCount)) {
                        peptideMap.get(peptideProbabilityScore).put(spectrumCount, new HashMap<String, PeptideMatch>());
                    }
                    peptideMap.get(peptideProbabilityScore).get(spectrumCount).put(peptideKey, peptideMatch);
                }

                ArrayList<Double> scores = new ArrayList<Double>(peptideMap.keySet());
                Collections.sort(scores);
                ArrayList<Integer> nSpectra;
                ArrayList<String> keys;
                PeptideMatch currentMatch;

                double maxPeptideSpectra = 0;

                int index = 0;
                int validatedPeptideCounter = 0;
                int validatedSpectraCounter = 0;

                for (double score : scores) {
                    nSpectra = new ArrayList<Integer>(peptideMap.get(score).keySet());
                    Collections.sort(nSpectra);

                    for (int i = nSpectra.size() - 1; i >= 0; i--) {
                        spectrumCount = nSpectra.get(i);
                        keys = new ArrayList<String>(peptideMap.get(score).get(spectrumCount).keySet());
                        Collections.sort(keys);

                        for (String key : keys) {
                            currentMatch = peptideMap.get(score).get(spectrumCount).get(key);
                            probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(key, probabilities);

                            if (!probabilities.isHidden()) {

                                ArrayList<String> otherProteins = new ArrayList<String>();
                                List<String> proteinProteins = Arrays.asList(ProteinMatch.getAccessions(proteinKey));
                                for (String accession : currentMatch.getTheoreticPeptide().getParentProteins()) {
                                    if (!proteinProteins.contains(accession)) {
                                        otherProteins.add(accession);
                                    }
                                }


                                validatedSpectraCounter = 0;
                                for (String spectrumKey : currentMatch.getSpectrumMatches()) {
                                    secondaryPSParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(spectrumKey, secondaryPSParameter);
                                    if (secondaryPSParameter.isValidated()) {
                                        validatedSpectraCounter++;
                                    }
                                }


                                // find and add the peptide start and end indexes
                                int peptideStart = 0;
                                int peptideEnd = 0;
                                String peptideSequence = currentMatch.getTheoreticPeptide().getSequence();
                                try {
                                    String proteinAccession = proteinMatch.getMainMatch();
                                    String proteinSequence = sequenceFactory.getProtein(proteinAccession).getSequence();
                                    peptideStart = proteinSequence.lastIndexOf(peptideSequence) + 1;
                                    peptideEnd = peptideStart + peptideSequence.length() - 1;
                                } catch (Exception e) {
                                    peptideShakerGUI.catchException(e);
                                    e.printStackTrace();
                                }
                                int proteinInferenceType = probabilities.getGroupClass();

                                ((DefaultTableModel) peptideTable.getModel()).addRow(new Object[]{
                                            index + 1,
                                            probabilities.isStarred(),
                                            proteinInferenceType,
                                            currentMatch.getTheoreticPeptide().getModifiedSequenceAsHtml(
                                            peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true),
                                            peptideStart,
                                            peptideEnd,
                                            //validatedSpectraCounter, @TODO: use this together with spectrumCount in a stacked bar chart
                                            spectrumCount,
                                            probabilities.getPeptideScore(),
                                            probabilities.getPeptideConfidence(),
                                            probabilities.isValidated()
                                        });

                                if (probabilities.isValidated()) {
                                    validatedPeptideCounter++;
                                }

                                if (maxPeptideSpectra < spectrumCount) {
                                    maxPeptideSpectra = spectrumCount;
                                }

                                peptideTableMap.put(index + 1, currentMatch.getKey());
                                index++;
                            }
                        }
                    }
                }

                ((DefaultTableModel) peptideTable.getModel()).fireTableDataChanged();

                ((TitledBorder) peptidesPanel.getBorder()).setTitle("Peptides (" + validatedPeptideCounter + "/" + peptideTable.getRowCount() + ")");
                peptidesPanel.repaint();

                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxPeptideSpectra);

                // select the peptide in the table
                if (peptideTable.getRowCount() > 0) {
                    int peptideRow = 0;
                    String peptideKey = peptideShakerGUI.getSelectedPeptideKey();
                    if (!peptideKey.equals(PeptideShakerGUI.NO_SELECTION)) {
                        peptideRow = getPeptideRow(peptideKey);
                    }

                    if (peptideRow != -1) {
                        peptideTable.setRowSelectionInterval(peptideRow, peptideRow);
                        peptideTable.scrollRectToVisible(peptideTable.getCellRect(peptideRow, 0, false));
                        peptideTableKeyReleased(null);
                    }
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Displays the results in the result tables.
     */
    public void displayResults() {

        progressDialog = new ProgressDialogX(peptideShakerGUI, peptideShakerGUI, true);
        progressDialog.doNothingOnClose();

        progressDialog.setIndeterminate(false);
        progressDialog.setTitle("Loading Overview. Please Wait...");
        progressDialog.setMax(peptideShakerGUI.getIdentification().getProteinIdentification().size());
        progressDialog.setValue(0);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {

            @Override
            public void run() {

                try {
                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                    // update spectrum counting column header tooltip
                    if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectralCountingMethod.EMPAI) {
                        proteinTableToolTips.set(proteinTable.getColumn("MS2 Quant.").getModelIndex(), "Protein MS2 Quantification - emPAI");
                    } else if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectralCountingMethod.NSAF) {
                        proteinTableToolTips.set(proteinTable.getColumn("MS2 Quant.").getModelIndex(), "Protein MS2 Quantification - NSAF");
                    } else {
                        proteinTableToolTips.set(proteinTable.getColumn("MS2 Quant.").getModelIndex(), "Protein MS2 Quantification");
                    }

                    int index = 0;
                    maxPeptides = 0;
                    maxSpectra = 0;
                    double sequenceCoverage = 0;
                    double spectrumCounting = 0;
                    maxSpectrumCounting = 0;
                    maxMW = 0;
                    String description = "";

                    // sort the proteins according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
                    HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap =
                            new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>();
                    ArrayList<Double> scores = new ArrayList<Double>();
                    PSParameter probabilities = new PSParameter();
                    PSParameter secondaryPSParameter = new PSParameter();
                    ProteinMatch proteinMatch;
                    double score;
                    int nPeptides, nSpectra;

                    for (String proteinKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {

                        if (!SequenceFactory.isDecoy(proteinKey)) {
                            proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                            probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(proteinKey, probabilities);
                            score = probabilities.getProteinProbabilityScore();
                            nPeptides = -proteinMatch.getPeptideMatches().size();
                            nSpectra = -peptideShakerGUI.getNSpectra(proteinMatch);

                            if (!orderMap.containsKey(score)) {
                                orderMap.put(score, new HashMap<Integer, HashMap<Integer, ArrayList<String>>>());
                                scores.add(score);
                            }

                            if (!orderMap.get(score).containsKey(nPeptides)) {
                                orderMap.get(score).put(nPeptides, new HashMap<Integer, ArrayList<String>>());
                            }

                            if (!orderMap.get(score).get(nPeptides).containsKey(nSpectra)) {
                                orderMap.get(score).get(nPeptides).put(nSpectra, new ArrayList<String>());
                            }

                            orderMap.get(score).get(nPeptides).get(nSpectra).add(proteinKey);
                        }

                        progressDialog.incrementValue();
                    }

                    progressDialog.setIndeterminate(true);
                    progressDialog.setTitle("Sorting Proteins. Please Wait...");

                    Collections.sort(scores);
                    proteinTableMap = new HashMap<Integer, String>();
                    // add the proteins to the table
                    ArrayList<Integer> nP, nS;
                    ArrayList<String> keys;

                    int validatedProteinsCounter = 0;
                    int nValidatedPeptides = 0, nValidatedSpectra = 0;

                    progressDialog.setIndeterminate(false);
                    progressDialog.setTitle("Loading Protein Table. Please Wait...");
                    progressDialog.setMax(scores.size());
                    progressDialog.setValue(0);

                    for (double currentScore : scores) {

                        nP = new ArrayList(orderMap.get(currentScore).keySet());
                        Collections.sort(nP);

                        for (int currentNP : nP) {

                            nS = new ArrayList(orderMap.get(currentScore).get(currentNP).keySet());
                            Collections.sort(nS);

                            for (int currentNS : nS) {

                                keys = orderMap.get(currentScore).get(currentNP).get(currentNS);
                                Collections.sort(keys);

                                for (String proteinKey : keys) {

                                    nValidatedPeptides = 0;
                                    nValidatedSpectra = 0;

                                    proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                                    probabilities = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(proteinKey, probabilities);

                                    if (!probabilities.isHidden()) {

                                        Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());

                                        // get the number of validated peptides
                                        for (String peptideKey : proteinMatch.getPeptideMatches()) {
                                            secondaryPSParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(peptideKey, secondaryPSParameter);
                                            if (secondaryPSParameter.isValidated()) {
                                                nValidatedPeptides++;
                                            }
                                        }

                                        // get the number of validated spectra
                                        for (String peptideKey : proteinMatch.getPeptideMatches()) {
                                            PeptideMatch peptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                                            for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                                                secondaryPSParameter = (PSParameter) peptideShakerGUI.getIdentification().getMatchParameter(spectrumKey, secondaryPSParameter);
                                                if (secondaryPSParameter.isValidated()) {
                                                    nValidatedSpectra++;
                                                }
                                            }
                                        }

                                        if (peptideShakerGUI.getSearchParameters().getEnzyme() == null) {
                                            throw new IllegalArgumentException("Unknown enzyme!");
                                        }

                                        if (currentProtein == null) {
                                            throw new IllegalArgumentException("Protein not found! Accession: " + proteinMatch.getMainMatch());
                                        }
                                        spectrumCounting = peptideShakerGUI.getSpectrumCounting(proteinMatch);
                                        description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription();
                                        sequenceCoverage = 100 * peptideShakerGUI.estimateSequenceCoverage(proteinMatch, currentProtein.getSequence());

                                        ((DefaultTableModel) proteinTable.getModel()).addRow(new Object[]{
                                                    index + 1,
                                                    probabilities.isStarred(),
                                                    probabilities.getGroupClass(),
                                                    peptideShakerGUI.addDatabaseLink(proteinMatch.getMainMatch()),
                                                    description,
                                                    sequenceCoverage,
                                                    //nValidatedPeptides, @TODO: use these together with currentNP and currentNS in stacked bar charts
                                                    //nValidatedSpectra,
                                                    -currentNP,
                                                    -currentNS,
                                                    spectrumCounting,
                                                    currentProtein.computeMolecularWeight() / 1000,
                                                    probabilities.getProteinScore(),
                                                    probabilities.getProteinConfidence(),
                                                    probabilities.isValidated()
                                                });

                                        proteinTableMap.put(index + 1, proteinKey);
                                        index++;

                                        if (probabilities.isValidated()) {
                                            validatedProteinsCounter++;
                                        }
                                        if (getMaxSpectrumCounting() < spectrumCounting) {
                                            maxSpectrumCounting = spectrumCounting;
                                        }
                                        if (getMaxMW() < currentProtein.computeMolecularWeight() / 1000) {
                                            maxMW = currentProtein.computeMolecularWeight() / 1000;
                                        }
                                    }
                                }
                                if (getMaxSpectra() < -currentNS) {
                                    maxSpectra = -currentNS;
                                }
                            }
                            if (getMaxPeptides() < -currentNP) {
                                maxPeptides = -currentNP;
                            }
                        }

                        progressDialog.incrementValue();
                    }

                    // invoke later to give time for components to update
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            // set the preferred size of the accession column
                            int width = peptideShakerGUI.getPreferredColumnWidth(proteinTable, proteinTable.getColumn("Accession").getModelIndex(), 6);
                            proteinTable.getColumn("Accession").setMinWidth(width);
                            proteinTable.getColumn("Accession").setMaxWidth(width);
                        }
                    });

                    ((TitledBorder) proteinsLayeredPanel.getBorder()).setTitle("Proteins (" + validatedProteinsCounter + "/" + proteinTable.getRowCount() + ")");
                    proteinsLayeredPanel.repaint();

                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(maxPeptides);
                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);
                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).setMaxValue(maxSpectrumCounting);
                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).setMaxValue(maxMW);

                    try {
                        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
                    } catch (IllegalArgumentException e) {
                        // ignore error
                    }

                    ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).setMaxValue(100.0);

                    // update the slider tooltips
                    double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();
                    accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " Da");
                    intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");

                    ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).setMaxValue(
                            peptideShakerGUI.getSearchParameters().getPrecursorAccuracy());
                    ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).setMaxValue(
                            (double) ((PSMaps) peptideShakerGUI.getIdentification().getUrParam(new PSMaps())).getPsmSpecificMap().getMaxCharge());

                    if (peptideShakerGUI.getSearchParameters().isPrecursorAccuracyTypePpm()) {
                        psmTableToolTips.set(3, "Mass Error (ppm)");
                    } else {
                        psmTableToolTips.set(3, "Mass Error (Da)");
                    }

                    // enable the contextual export options
                    exportProteinsJButton.setEnabled(true);
                    exportPeptidesJButton.setEnabled(true);
                    exportPsmsJButton.setEnabled(true);
                    exportSpectrumJButton.setEnabled(true);
                    exportSequenceCoverageContextJButton.setEnabled(true);

                    peptideShakerGUI.setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, true);
                    
                    // change the peptide shaker icon back to the default version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));

                    progressDialog.setVisible(false);
                    progressDialog.dispose();

                    // invoke later to give time for components to update
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            updateSelection();
                            proteinTable.requestFocus();
                        }
                    });   

                } catch (Exception e) {
                    // change the peptide shaker icon back to the default version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    peptideShakerGUI.catchException(e);
                    progressDialog.setVisible(false);
                    progressDialog.dispose();
                }
            }
        }.start();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the sequence coverage map according to the selected protein (and peptide).
     *
     * @param row       the row index of the protein
     * @param column    the column index in the protein table
     */
    private void updateSequenceCoverageMap(int row) {
        try {
            String proteinKey = proteinTableMap.get(getProteinIndex(row));
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
            updateSequenceCoverage(proteinMatch.getMainMatch());
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Returns the protein table.
     * 
     * @return the protein table
     */
    public JTable getProteinTable() {
        return proteinTable;
    }

    /**
     * Returns an arraylist of the spectrum annotations for all the selected PSMs.
     * 
     * @return an arraylist of the spectrum annotations 
     * @throws MzMLUnmarshallerException 
     */
    private ArrayList<ArrayList<IonMatch>> getAnnotationsForAllSelectedSpectra() throws MzMLUnmarshallerException {

        ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<ArrayList<IonMatch>>();

        int[] selectedRows = psmTable.getSelectedRows();

        SpectrumAnnotator miniAnnotator = new SpectrumAnnotator();
        AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
        try {
            for (int i = 0; i < selectedRows.length; i++) {

                String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(selectedRows[i], 0));
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                if (currentSpectrum != null) {
                    // get the spectrum annotations
                    String peptideKey = peptideTableMap.get(getPeptideIndex(peptideTable.getSelectedRow()));
                    Peptide currentPeptide = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide();
                    annotationPreferences.setCurrentSettings(currentPeptide,
                            currentSpectrum.getPrecursor().getCharge().value, !currentSpectrumKey.equalsIgnoreCase(spectrumKey));
                    ArrayList<IonMatch> annotations = miniAnnotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
                            annotationPreferences.getNeutralLosses(),
                            annotationPreferences.getValidatedCharges(),
                            currentSpectrum, currentPeptide,
                            currentSpectrum.getIntensityLimit(annotationPreferences.getAnnotationIntensityLimit()),
                            annotationPreferences.getFragmentIonAccuracy());
                    allAnnotations.add(annotations);
                    currentSpectrumKey = spectrumKey;
                }
            }
            return allAnnotations;
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return null;
        }
    }

    /**
     * Returns an arraylist of all the selected spectra in the PSM table.
     * 
     * @return an arraylist of all the selected spectra
     * @throws MzMLUnmarshallerException 
     */
    private ArrayList<MSnSpectrum> getSelectedSpectra() throws MzMLUnmarshallerException {

        ArrayList<MSnSpectrum> allSpectra = new ArrayList<MSnSpectrum>();

        int[] selectedRows = psmTable.getSelectedRows();
        MSnSpectrum tempSpectrum;

        for (int i = 0; i < selectedRows.length; i++) {
            String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(selectedRows[i], 0));
            tempSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
            if (tempSpectrum != null) {
                allSpectra.add(tempSpectrum);
            }
        }

        return allSpectra;
    }

    /**
     * Returns the spectrum panel.
     * 
     * @return the spectrum panel, or null if the spectrum tab is not enabled
     */
    public Component getSpectrum() {

        if (spectrumJTabbedPane.isEnabledAt(2)) {
            spectrumJTabbedPane.setSelectedIndex(2);
            return (Component) spectrumPanel.getComponent(0);
        }

        return null;
    }

    /**
     * Returns the sequence fragmentation plot panel.
     * 
     * @return the sequence fragmentation plot panel, or null if the spectrum tab is not enabled
     */
    public Component getSequenceFragmentationPlot() {

        if (spectrumJTabbedPane.isEnabledAt(2)) {
            spectrumJTabbedPane.setSelectedIndex(2);
            return (Component) secondarySpectrumPlotsJPanel.getComponent(0);
        }

        return null;
    }

    /**
     * Returns the intensity histogram plot panel.
     * 
     * @return the intensity histogram plot panel, or null if the spectrum tab is not enabled
     */
    public IntensityHistogram getIntensityHistogramPlot() {

        if (spectrumJTabbedPane.isEnabledAt(2)) {
            spectrumJTabbedPane.setSelectedIndex(2);
            return (IntensityHistogram) secondarySpectrumPlotsJPanel.getComponent(1);
        }

        return null;
    }

    /**
     * Returns the mass error plot panel.
     * 
     * @return the mass error plot panel, or null if the spectrum tab is not enabled 
     *         or the the mass error plot is not showing 
     */
    public MassErrorPlot getMassErrorPlot() {

        if (spectrumJTabbedPane.isEnabledAt(2)) {
            spectrumJTabbedPane.setSelectedIndex(2);

            if (secondarySpectrumPlotsJPanel.getComponentCount() == 3) {
                return (MassErrorPlot) secondarySpectrumPlotsJPanel.getComponent(2);
            }
        }

        return null;
    }

    /**
     * Returns the current spectrum as an mgf string.
     * 
     * @return the current spectrum as an mgf string
     */
    public String getSpectrumAsMgf() {

        int[] selectedRows = psmTable.getSelectedRows();

        if (selectedRows.length > 0) {

            String spectraAsMgf = "";

            for (int i = 0; i < selectedRows.length; i++) {
                String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(selectedRows[i], 0));
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                spectraAsMgf += currentSpectrum.asMgf();
            }

            return spectraAsMgf;
        }

        return null;
    }

    /**
     * Returns the bubble plot.
     * 
     * @return the bubble plot
     */
    public Component getBubblePlot() {

        if (spectrumJTabbedPane.isEnabledAt(1)) {
            spectrumJTabbedPane.setSelectedIndex(1);
            return ((MassErrorBubblePlot) bubbleJPanel.getComponent(0)).getChartPanel();
        }

        return null;
    }

    /**
     * Returns true of the spectrum tab is enabled.
     * 
     * @return true of the spectrum tab is enabled
     */
    public boolean isSpectrumEnabled() {
        return spectrumJTabbedPane.isEnabledAt(2);
    }

    /**
     * Enable or disable the separators.
     * 
     * @param showSeparators if true the separators are enabled
     */
    public void showSeparators(boolean showSeparators) {

        int dividerSize = 5;

        // @TODO: the coverage splitter sometimes causes problems and has therefore been disabled!!
        //        (the reason has to be the redrawing of the coverage panel...)

        if (showSeparators) {
            //overviewJSplitPane.setDividerSize(dividerSize);
            //coverageJSplitPane.setDividerSize(dividerSize);
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(dividerSize);
            //peptidesPsmJSplitPane.setDividerSize(dividerSize);
            formComponentResized(null);
        } else {
            //overviewJSplitPane.setDividerSize(0);
            //coverageJSplitPane.setDividerSize(0);
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(0);
            //peptidesPsmJSplitPane.setDividerSize(0);
            formComponentResized(null);
        }
    }

    /**
     * Update the main match for the given row in the protein table.
     * 
     * @param mainMatch             the accession of the protein match to use
     * @param proteinInferenceType  the protein inference group type
     */
    public void updateMainMatch(String mainMatch, int proteinInferenceType) {

        proteinTable.setValueAt(peptideShakerGUI.addDatabaseLink(mainMatch), proteinTable.getSelectedRow(), proteinTable.getColumn("Accession").getModelIndex());
        proteinTable.setValueAt(proteinInferenceType, proteinTable.getSelectedRow(), proteinTable.getColumn("PI").getModelIndex());
        String description = "unknown protein";
        try {
            description = sequenceFactory.getHeader(mainMatch).getDescription();
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        proteinTable.setValueAt(description, proteinTable.getSelectedRow(), proteinTable.getColumn("Description").getModelIndex());
    }

    /**
     * Returns the current sequence coverage.
     * 
     * @return the current sequence coverage
     */
    public int[] getCoverage() {
        return coverage;
    }

    /**
     * Hides or displays the score columns in the protein and peptide tables.
     */
    public void updateScores() {

        try {
            if (!peptideShakerGUI.getDisplayPreferences().showScores()) {
                try {
                    proteinTable.removeColumn(proteinTable.getColumn("Score"));
                    peptideTable.removeColumn(peptideTable.getColumn("Score"));
                } catch (IllegalArgumentException e) {
                    // ignore error
                }
            } else {
                proteinTable.addColumn(proteinScoreColumn);
                proteinTable.moveColumn(12, 11);

                peptideTable.addColumn(peptideScoreColumn);
                peptideTable.moveColumn(9, 7);
            }
        } catch (IllegalArgumentException e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
    }

    /**
     * Returns the current selected tab in the spectrum and fragment ions 
     * tabbed pane.
     * 
     * @return the current selected tab in the spectrum and fragment ions tabbed pane
     */
    public int getSelectedSpectrumTabIndex() {
        return spectrumJTabbedPane.getSelectedIndex();
    }

    /**
     * Makes sure that the annotation menu bar is shown in the currently visible 
     * spectrum and fragment ions tabbed pane.
     */
    public void showSpectrumAnnotationMenu() {
        spectrumJTabbedPaneStateChanged(null);
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
     * Converts the x-axis coordinate in the coverage table into a residue 
     * index in the sequence.
     * 
     * @param coverageTableX    the x-axis coordinate in the coverage table
     * @return                  the residue index
     */
    private int convertPointToResidueNumber(double coverageTableX) {

        // @TODO: does not work perfectly...

        double paddingInPercent = 0.0115; // @TODO: this hardcoded value should not be required!
        double cellWidth = coverageTable.getWidth();

        double width = (coverageTableX - cellWidth * paddingInPercent) / (cellWidth - 2 * cellWidth * paddingInPercent);

        return (int) (width * currentProteinSequence.length());
    }

    /**
     * Update the PTM color coding.
     */
    public void updatePtmColors() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        HashMap<String, Color> ptmColors = peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors();

        try {

            // update the peptide table
            for (int i = 0; i < peptideTable.getRowCount(); i++) {
                String peptideKey = peptideTableMap.get(getPeptideIndex(i));
                String modifiedSequence = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey).getTheoreticPeptide().getModifiedSequenceAsHtml(ptmColors, true);
                peptideTable.setValueAt(modifiedSequence, i, peptideTable.getColumn("Sequence").getModelIndex());
            }

            // update the psm table
            if (peptideTable.getSelectedRow() != -1) {

                String peptideKey = peptideTableMap.get(getPeptideIndex(peptideTable.getSelectedRow()));
                PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);

                for (int i = 0; i < psmTable.getRowCount(); i++) {

                    String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(i, 0));
                    PeptideAssumption peptideAssumption = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey).getBestAssumption();

                    if (peptideAssumption.getPeptide().isSameAs(currentPeptideMatch.getTheoreticPeptide())) {
                        String modifiedSequence = peptideAssumption.getPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                        psmTable.setValueAt(modifiedSequence, i, psmTable.getColumn("Sequence").getModelIndex());
                    } else {
                        // @TODO: do we need to do something here??
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the spectrum and fragment ions panel border title with information 
     * about the currently selected psm.
     * 
     * @param currentPeptide
     * @param currentSpectrum 
     */
    private void updateSpectrumPanelBorderTitle(Peptide currentPeptide, MSnSpectrum currentSpectrum) {

        int selectedRow = peptideTable.getSelectedRow();
        int start = (Integer) peptideTable.getValueAt(selectedRow, peptideTable.getColumn("Start").getModelIndex()) - 1;
        int end = (Integer) peptideTable.getValueAt(selectedRow, peptideTable.getColumn("End").getModelIndex());

        String before = "";
        String after = "";

        // @TODO: make the number of residues shown before/after up to the user?

        if (start - 2 >= 0) {
            before = currentProteinSequence.substring(start - 2, start) + " - ";
        } else if (start - 1 >= 0) {
            before = currentProteinSequence.substring(start - 1, start) + " - ";
        }

        if (end + 2 <= currentProteinSequence.length()) {
            after = " - " + currentProteinSequence.substring(end, end + 2);
        } else if (end + 1 <= currentProteinSequence.length()) {
            after = " - " + currentProteinSequence.substring(end, end + 1);
        }

        String spectrumKey = psmTableMap.get((Integer) psmTable.getValueAt(psmTable.getSelectedRow(), 0));
        String modifiedSequence = "";

        try {
            PeptideAssumption peptideAssumption = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey).getBestAssumption();

            if (peptideAssumption.getPeptide().isSameAs(currentPeptide)) {
                modifiedSequence = peptideAssumption.getPeptide().getModifiedSequenceAsString(false);
            } else {
                // @TODO: do we need to do something here??
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }

        ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                "Spectrum & Fragment Ions (" + before + modifiedSequence + after
                + "   " + currentSpectrum.getPrecursor().getCharge().toString() + "   "
                + Util.roundDouble(currentSpectrum.getPrecursor().getMz(), 4) + " m/z)");
    }

    /**
     * Update the protein inference type for the currently selected peptide.
     * 
     * @param proteinInferenceType 
     */
    public void updatePeptideProteinInference(int proteinInferenceType) {
        peptideTable.setValueAt(proteinInferenceType, peptideTable.getSelectedRow(), peptideTable.getColumn("PI").getModelIndex());
    }

    /**
     * Export the table contents to the clipboard.
     * 
     * @param index 
     */
    private void copyTableContentToClipboard(TableIndex index) {

        final TableIndex tableIndex = index;

        if (tableIndex == TableIndex.PROTEIN_TABLE
                || tableIndex == TableIndex.PEPTIDE_TABLE
                || tableIndex == TableIndex.PSM_TABLE) {

            progressDialog = new ProgressDialogX(peptideShakerGUI, peptideShakerGUI, true);
            progressDialog.doNothingOnClose();

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setIndeterminate(true);
                    progressDialog.setTitle("Copying to Clipboard. Please Wait...");
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            new Thread("ExportThread") {

                @Override
                public void run() {
                    try {
                        String clipboardString = "";
                        FeaturesGenerator outputGenerator = new FeaturesGenerator(peptideShakerGUI);

                        if (tableIndex == TableIndex.PROTEIN_TABLE) {
                            ArrayList<String> selectedProteins = getDisplayedProteins();
                            clipboardString = outputGenerator.getProteinsOutput(
                                    progressDialog, selectedProteins, true, false, true, true,
                                    true, true, true, true, false, true,
                                    true, true, true, true, false, false);
                        } else if (tableIndex == TableIndex.PEPTIDE_TABLE) {
                            ArrayList<String> selectedPeptides = getDisplayedPeptides();
                            clipboardString = outputGenerator.getPeptidesOutput(
                                    progressDialog, selectedPeptides, null, true, false, true, true,
                                    true, true, true, true, true, true, true, false, false);
                        } else if (tableIndex == TableIndex.PSM_TABLE) {
                            ArrayList<String> selectedPsms = getDisplayedPsms();
                            clipboardString = outputGenerator.getPSMsOutput(
                                    progressDialog, selectedPsms, true, false, true, true, true,
                                    true, true, true, true, true, true, true, false, false);
                        }

                        StringSelection stringSelection = new StringSelection(clipboardString);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(stringSelection, peptideShakerGUI);

                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "Table content copied to clipboard.", "Copied to Clipboard", JOptionPane.INFORMATION_MESSAGE);

                    } catch (Exception e) {
                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    /**
     * Updates the visability of the show panels buttons at the bottom of 
     * the screen.
     * 
     * @return true of all panels are currently displayed
     */
    private boolean updateHiddenPanels() {

        showProteinsJButton.setVisible(!displayProteins);
        showProteinsAfterSeparator.setVisible(!displayProteins);

        showPeptidesAndPsmsJButton.setVisible(!displayPeptidesAndPSMs);
        showPeptidesAfterSeparator.setVisible(!displayPeptidesAndPSMs);

        showCoverageJButton.setVisible(!displayCoverage);
        showCoverageAfterSeparator.setVisible(!displayCoverage);

        showSpectrumJButton.setVisible(!displaySpectrum);
        showSpectrumAfterSeparator.setVisible(!displaySpectrum);

        if (displayProteins && displayPeptidesAndPSMs && displayCoverage && displaySpectrum) {
            showProteinsBeforeSeparator.setVisible(false);
        } else {
            showProteinsBeforeSeparator.setVisible(true);
        }

        return displayProteins && displayPeptidesAndPSMs && displayCoverage && displaySpectrum;
    }

    /**
     * Update the selected protein and peptide.
     */
    public void updateSelection() {

        int proteinRow = 0;
        String proteinKey = peptideShakerGUI.getSelectedProteinKey();
        String peptideKey = peptideShakerGUI.getSelectedPeptideKey();
        String psmKey = peptideShakerGUI.getSelectedPsmKey();
        if (proteinKey.equals(PeptideShakerGUI.NO_SELECTION)
                && peptideKey.equals(PeptideShakerGUI.NO_SELECTION)
                && !psmKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            if (peptideShakerGUI.getIdentification().matchExists(psmKey)) {
                SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(psmKey);
                peptideKey = spectrumMatch.getBestAssumption().getPeptide().getKey();
            } else {
                peptideShakerGUI.resetSelectedItems();
            }
        }
        if (proteinKey.equals(PeptideShakerGUI.NO_SELECTION)
                && !peptideKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            ProteinMatch proteinMatch;
            for (String possibleKey : peptideShakerGUI.getIdentification().getProteinIdentification()) {
                proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(possibleKey);
                if (proteinMatch.getPeptideMatches().contains(peptideKey)) {
                    proteinKey = possibleKey;
                    peptideShakerGUI.setSelectedItems(proteinKey, peptideKey, psmKey);
                    break;
                }
            }
        }
        if (!proteinKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            proteinRow = getProteinRow(proteinKey);
        }
        if (proteinRow != -1) {
            proteinTable.setRowSelectionInterval(proteinRow, proteinRow);
            proteinTable.scrollRectToVisible(proteinTable.getCellRect(proteinRow, 0, false));
            proteinTableMouseReleased(null);
        }
    }

    /**
     * Updates and displays the current spectrum slider tooltip.
     */
    private void updateSpectrumSliderToolTip() {
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();

        spectrumJTabbedPane.setToolTipText("<html>Accuracy: " + Util.roundDouble(accuracy, 2) + " Da<br>"
                + "Level: " + intensitySlider.getValue() + "%</html>");

        // show the tooltip now
        ToolTipManager.sharedInstance().mouseMoved(
                new MouseEvent(spectrumJTabbedPane, 0, 0, 0,
                spectrumJTabbedPane.getX() + spectrumJTabbedPane.getWidth() - 10, spectrumJTabbedPane.getY() + 90, // X-Y of the mouse for the tool tip
                0, false));
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected protein, peptide and psm
     */
    public void newItemSelection() {
        String proteinKey = PeptideShakerGUI.NO_SELECTION;
        String peptideKey = PeptideShakerGUI.NO_SELECTION;
        String psmKey = PeptideShakerGUI.NO_SELECTION;

        if (proteinTable.getSelectedRow() != -1) {
            proteinKey = proteinTableMap.get(getProteinIndex(proteinTable.getSelectedRow()));
        }
        if (peptideTable.getSelectedRow() != -1) {
            peptideKey = peptideTableMap.get(getPeptideIndex(peptideTable.getSelectedRow()));
        }
        if (psmTable.getSelectedRow() != -1) {
            psmKey = psmTableMap.get((Integer) psmTable.getValueAt(psmTable.getSelectedRow(), 0));
        } 
        
        peptideShakerGUI.setSelectedItems(proteinKey, peptideKey, psmKey);
    }

    /**
     * Returns the row of a desired protein
     * @param proteinKey the key of the protein
     * @return the row of the desired protein
     */
    private int getProteinRow(String proteinKey) {
        int index = -1;
        for (int key : proteinTableMap.keySet()) {
            if (proteinTableMap.get(key).equals(proteinKey)) {
                index = key;
                break;
            }
        }
        for (int row = 0; row < proteinTable.getRowCount(); row++) {
            if ((Integer) proteinTable.getValueAt(row, 0) == index) {
                return row;
            }
        }
        return -1;
    }

    /**
     * Returns the row of a desired peptide
     * @param peptideKey the key of the peptide
     * @return the row of the desired peptide
     */
    private int getPeptideRow(String peptideKey) {
        int index = -1;
        for (int key : peptideTableMap.keySet()) {
            if (peptideTableMap.get(key).equals(peptideKey)) {
                index = key;
                break;
            }
        }
        for (int row = 0; row < peptideTable.getRowCount(); row++) {
            if ((Integer) peptideTable.getValueAt(row, 0) == index) {
                return row;
            }
        }
        return -1;
    }

    /**
     * Returns the row of a desired psm.
     * 
     * @param psmKey the key of the psm
     * @return the row of the desired psm
     */
    private int getPsmRow(String psmKey) {

        int index = -1;
        for (int key : psmTableMap.keySet()) {
            if (psmTableMap.get(key).equals(psmKey)) {
                index = key;
                break;
            }
        }

        for (int row = 0; row < psmTable.getRowCount(); row++) {
            if ((Integer) psmTable.getValueAt(row, 0) == index) {
                return row;
            }
        }
        return -1;
    }

    /**
     * Clear all the data.
     */
    public void clearData() {

        displaySpectrum = true;
        displayCoverage = true;
        displayProteins = true;
        displayPeptidesAndPSMs = true;

        DefaultTableModel dm = (DefaultTableModel) proteinTable.getModel();
        dm.getDataVector().removeAllElements();
        dm.fireTableDataChanged();

        dm = (DefaultTableModel) peptideTable.getModel();
        dm.getDataVector().removeAllElements();
        dm.fireTableDataChanged();

        dm = (DefaultTableModel) psmTable.getModel();
        dm.getDataVector().removeAllElements();
        dm.fireTableDataChanged();

        proteinTableMap = new HashMap<Integer, String>();
        peptideTableMap = new HashMap<Integer, String>();
        psmTableMap = new HashMap<Integer, String>();

        currentSpectrumKey = "";
        currentProteinSequence = "";
        maxPsmMzValue = Double.MIN_VALUE;
        spectrum = null;

        coverageTable.setValueAt(null, 0, 0);

        fragmentIonsJScrollPane.setViewportView(null);
        bubbleJPanel.removeAll();
        spectrumPanel.removeAll();
        secondarySpectrumPlotsJPanel.removeAll();

        ((TitledBorder) proteinsLayeredPanel.getBorder()).setTitle("Proteins");
        ((TitledBorder) peptidesPanel.getBorder()).setTitle("Peptides");
        ((TitledBorder) psmsPanel.getBorder()).setTitle("Peptide-Spectrum Matches");
        ((TitledBorder) spectrumMainPanel.getBorder()).setTitle("Spectrum & Fragment Ions");
        ((TitledBorder) sequenceCoveragePanel.getBorder()).setTitle("Protein Sequence Coverage");
    }

    /**
     * @return the maxPeptides
     */
    public int getMaxPeptides() {
        return maxPeptides;
    }

    /**
     * @return the maxSpectra
     */
    public int getMaxSpectra() {
        return maxSpectra;
    }

    /**
     * @return the maxSpectrumCounting
     */
    public double getMaxSpectrumCounting() {
        return maxSpectrumCounting;
    }

    /**
     * @return the maxMW
     */
    public double getMaxMW() {
        return maxMW;
    }
}
