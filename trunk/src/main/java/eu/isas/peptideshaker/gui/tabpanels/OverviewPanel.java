package eu.isas.peptideshaker.gui.tabpanels;

import eu.isas.peptideshaker.gui.tablemodels.ProteinTableModel;
import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.biology.AminoAcidPattern;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.*;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.GeneDetailsDialog;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.XYPlottingDialog;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.spectrum.*;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.math.statistics.distributions.NonSymmetricalNormalDistribution;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.SpecificAnnotationPreferences;
import eu.isas.peptideshaker.export.OutputGenerator;
import eu.isas.peptideshaker.gui.MatchValidationDialog;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.protein_inference.ProteinInferenceDialog;
import eu.isas.peptideshaker.gui.protein_inference.ProteinInferencePeptideLevelDialog;
import eu.isas.peptideshaker.gui.protein_sequence.ProteinSequencePanel;
import eu.isas.peptideshaker.gui.protein_sequence.ProteinSequencePanelParent;
import eu.isas.peptideshaker.gui.protein_sequence.ResidueAnnotation;
import eu.isas.peptideshaker.gui.tablemodels.PeptideTableModel;
import eu.isas.peptideshaker.gui.tablemodels.PsmTableModel;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences.SpectralCountingMethod;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.JSparklinesDataset;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.*;
import org.jfree.chart.*;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.Range;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The overview panel displaying the proteins, the peptides and the spectra.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class OverviewPanel extends javax.swing.JPanel implements ProteinSequencePanelParent {

    /**
     * Indexes for the three main data tables.
     */
    private enum TableIndex {

        PROTEIN_TABLE, PEPTIDE_TABLE, PSM_TABLE
    };
    /**
     * A list of the panels that where visible when the spectrum was maximized.
     * The order is: protein, peptides and PSMs, coverage.
     */
    private ArrayList<Boolean> panelsShownUponMaximze;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The current spectrum key.
     */
    private String currentSpectrumKey = "";
    /**
     * The current sequence coverage in a map: likelihood to find the peptide
     * &gt; start end indexes &gt; validation level
     */
    private HashMap<Double, HashMap<int[], Integer>> coverage;
    /**
     * The current protein accession number.
     */
    private String currentProteinAccession;
    /**
     * The current protein sequence.
     */
    private String currentProteinSequence = "";
    /**
     * The current spectrum panel.
     */
    private SpectrumPanel spectrumPanel;
    /**
     * Boolean indicating whether the spectrum shall be displayed.
     */
    private boolean displaySpectrum = true;
    /**
     * Boolean indicating whether the sequence coverage shall be displayed.
     */
    private boolean displayCoverage = true;
    /**
     * Boolean indicating whether the protein table shall be displayed.
     */
    private boolean displayProteins = true;
    /**
     * Boolean indicating whether the PSMs shall be displayed.
     */
    private boolean displayPeptidesAndPSMs = true;
    /**
     * A list of proteins in the protein table.
     */
    private ArrayList<String> proteinKeys = new ArrayList<String>();
    /**
     * A list of the peptides in the peptide table.
     */
    private ArrayList<String> peptideKeys = new ArrayList<String>();
    /**
     * A list of PSMs in the PSM table.
     */
    private ArrayList<String> psmKeys = new ArrayList<String>();
    /**
     * The main GUI.
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
     * The gene factory.
     */
    private GeneFactory geneFactory = GeneFactory.getInstance();
    /**
     * The sequence coverage chart.
     */
    private ChartPanel coverageChart;
    /**
     * The sequence PTM chart.
     */
    private ChartPanel ptmChart;
    /**
     * The last m/z maximum displayed.
     */
    private double lastMzMaximum = 0;
    /**
     * The location of the divider showing or hiding the spectrum sub plots.
     */
    private int spectrumSubPlotDividerLocation = 80;

    /**
     * Creates a new OverviewPanel.
     *
     * @param parent the PeptideShaker parent frame.
     */
    public OverviewPanel(PeptideShakerGUI parent) {

        this.peptideShakerGUI = parent;

        initComponents();

        // set main table properties
        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        psmTable.getTableHeader().setReorderingAllowed(false);

        // correct the color for the upper right corner
        JPanel proteinCorner = new JPanel();
        proteinCorner.setBackground(proteinTable.getTableHeader().getBackground());
        proteinScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, proteinCorner);
        JPanel peptideCorner = new JPanel();
        peptideCorner.setBackground(peptideTable.getTableHeader().getBackground());
        peptideScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, peptideCorner);
        JPanel psmCorner = new JPanel();
        psmCorner.setBackground(psmTable.getTableHeader().getBackground());
        spectraScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, psmCorner);
        JPanel ionTableCorner = new JPanel();
        ionTableCorner.setBackground(proteinTable.getTableHeader().getBackground());
        fragmentIonsJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, ionTableCorner);

        // add table sorting listeners
        SelfUpdatingTableModel.addSortListener(proteinTable, new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true));
        SelfUpdatingTableModel.addSortListener(peptideTable, new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true));
        SelfUpdatingTableModel.addSortListener(psmTable, new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true));

        // add table scrolling listeners
        SelfUpdatingTableModel.addScrollListeners(proteinTable, proteinScrollPane, proteinScrollPane.getVerticalScrollBar());
        SelfUpdatingTableModel.addScrollListeners(peptideTable, peptideScrollPane, peptideScrollPane.getVerticalScrollBar());
        SelfUpdatingTableModel.addScrollListeners(psmTable, spectraScrollPane, spectraScrollPane.getVerticalScrollBar());

        // make sure that the scroll panes are see-through
        proteinScrollPane.getViewport().setOpaque(false);
        peptideScrollPane.getViewport().setOpaque(false);
        spectraScrollPane.getViewport().setOpaque(false);
        fragmentIonsJScrollPane.getViewport().setOpaque(false);

        // make the tabs in the spectrum tabbed pane go from right to left
        spectrumJTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // set up the table header tooltips
        setUpTableHeaderToolTips();

        updateSeparators();
        formComponentResized(null);
    }

    /**
     * Set up the table header tooltips.
     */
    private void setUpTableHeaderToolTips() {
        proteinTableToolTips = new ArrayList<String>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Starred");
        proteinTableToolTips.add("Protein Inference Class");
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Chromosome Number");
        proteinTableToolTips.add("Protein Sequence Coverage (%) (Confident / Doubtful / Not Validated / Possible)");
        proteinTableToolTips.add("Number of Peptides (Confident / Doubtful / Not Validated)");
        proteinTableToolTips.add("Number of Spectra (Confident / Doubtful / Not Validated)");
        proteinTableToolTips.add("MS2 Quantification");
        proteinTableToolTips.add("Protein Molecular Weight (kDa)");

        if (peptideShakerGUI.getDisplayPreferences().showScores()) {
            proteinTableToolTips.add("Protein Score");
        } else {
            proteinTableToolTips.add("Protein Confidence");
        }

        proteinTableToolTips.add("Validated");

        peptideTableToolTips = new ArrayList<String>();
        peptideTableToolTips.add(null);
        peptideTableToolTips.add("Starred");
        peptideTableToolTips.add("Protein Inference Class");
        peptideTableToolTips.add("Peptide Sequence");
        peptideTableToolTips.add("Peptide Start Index");
        peptideTableToolTips.add("Number of Spectra (Confident / Doubtful / Not Validated)");
        peptideTableToolTips.add("Peptide Confidence");
        peptideTableToolTips.add("Validated");

        psmTableToolTips = new ArrayList<String>();
        psmTableToolTips.add(null);
        psmTableToolTips.add("Starred");
        psmTableToolTips.add("Identification Software Agreement");
        psmTableToolTips.add("Peptide Sequence");
        psmTableToolTips.add("Precursor Charge");
        psmTableToolTips.add("Mass Error");
        psmTableToolTips.add("Peptide Spectrum Match Confidence");
        psmTableToolTips.add("Validated");
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {
        setProteinTableProperties();
        setPeptideTableProperties();
        setPsmTableProperties();
    }

    /**
     * Set up the properties of the protein table.
     */
    private void setProteinTableProperties() {

        ProteinTableModel.setProteinTableProperties(proteinTable, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColorNonValidated(),
                peptideShakerGUI.getSparklineColorNotFound(), peptideShakerGUI.getUtilitiesUserPreferences().getSparklineColorDoubtful(),
                peptideShakerGUI.getScoreAndConfidenceDecimalFormat(), this.getClass(), peptideShakerGUI.getMetrics().getMaxProteinKeyLength());

        proteinTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        reselect();
                    }
                });
            }
        });
    }

    /**
     * Set up the properties of the peptide table.
     */
    private void setPeptideTableProperties() {

        // the index column
        peptideTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMinWidth(50);
        peptideTable.getColumn("Start").setMinWidth(50);

        try {
            peptideTable.getColumn("Confidence").setMaxWidth(90);
            peptideTable.getColumn("Confidence").setMinWidth(90);
        } catch (IllegalArgumentException w) {
            peptideTable.getColumn("Score").setMaxWidth(90);
            peptideTable.getColumn("Score").setMinWidth(90);
        }

        // the validated column
        peptideTable.getColumn("").setMaxWidth(30);

        // the selected columns
        peptideTable.getColumn("  ").setMaxWidth(30);
        peptideTable.getColumn("  ").setMinWidth(30);

        // the protein inference column
        peptideTable.getColumn("PI").setMaxWidth(37);
        peptideTable.getColumn("PI").setMinWidth(37);

        // set up the peptide inference color map
        HashMap<Integer, Color> peptideInferenceColorMap = new HashMap<Integer, Color>();
        peptideInferenceColorMap.put(PSParameter.NOT_GROUP, peptideShakerGUI.getSparklineColor());
        peptideInferenceColorMap.put(PSParameter.RELATED, Color.YELLOW);
        peptideInferenceColorMap.put(PSParameter.RELATED_AND_UNRELATED, Color.ORANGE);
        peptideInferenceColorMap.put(PSParameter.UNRELATED, Color.RED);

        // set up the peptide inference tooltip map
        HashMap<Integer, String> peptideInferenceTooltipMap = new HashMap<Integer, String>();
        peptideInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Unique to a single protein");
        peptideInferenceTooltipMap.put(PSParameter.RELATED, "Belongs to a group of related proteins");
        peptideInferenceTooltipMap.put(PSParameter.RELATED_AND_UNRELATED, "Belongs to a group of related and unrelated proteins");
        peptideInferenceTooltipMap.put(PSParameter.UNRELATED, "Belongs to unrelated proteins");

        peptideTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(peptideShakerGUI.getSparklineColor(), peptideInferenceColorMap, peptideInferenceTooltipMap));
        peptideTable.getColumn("Start").setCellRenderer(new JSparklinesMultiIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100d, 100d, peptideShakerGUI.getSparklineColor()));

        // use a gray color for no decoy searches
        Color nonValidatedColor = peptideShakerGUI.getSparklineColorNonValidated();
        if (!sequenceFactory.concatenatedTargetDecoy()) {
            nonValidatedColor = peptideShakerGUI.getUtilitiesUserPreferences().getSparklineColorNotFound();
        }
        ArrayList<Color> sparklineColors = new ArrayList<Color>();
        sparklineColors.add(peptideShakerGUI.getSparklineColor());
        sparklineColors.add(peptideShakerGUI.getUtilitiesUserPreferences().getSparklineColorDoubtful());
        sparklineColors.add(nonValidatedColor);
        peptideTable.getColumn("#Spectra").setCellRenderer(new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColors, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers));
        ((JSparklinesArrayListBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0"));
        peptideTable.getColumn("").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        peptideTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));

        try {
            peptideTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        } catch (IllegalArgumentException e) {
            peptideTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        }

        peptideTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        reselect();
                    }
                });
            }
        });
    }

    /**
     * Set up the properties of the PSM table.
     */
    private void setPsmTableProperties() {

        // the index column
        psmTable.getColumn(" ").setMaxWidth(50);
        psmTable.getColumn(" ").setMinWidth(50);

        try {
            psmTable.getColumn("Confidence").setMaxWidth(90);
            psmTable.getColumn("Confidence").setMinWidth(90);
        } catch (IllegalArgumentException w) {
            psmTable.getColumn("Score").setMaxWidth(90);
            psmTable.getColumn("Score").setMinWidth(90);
        }

        // the validated column
        psmTable.getColumn("").setMaxWidth(30);
        psmTable.getColumn("").setMinWidth(30);

        // the selected columns
        psmTable.getColumn("  ").setMaxWidth(30);
        psmTable.getColumn("  ").setMinWidth(30);

        // the protein inference column
        psmTable.getColumn("ID").setMaxWidth(37);
        psmTable.getColumn("ID").setMinWidth(37);

        // set up the psm color map
        HashMap<Integer, java.awt.Color> psmColorMap = new HashMap<Integer, java.awt.Color>();
        psmColorMap.put(SpectrumIdentificationPanel.AGREEMENT_WITH_MODS, peptideShakerGUI.getSparklineColor()); // id software agree with PTM certainty
        psmColorMap.put(SpectrumIdentificationPanel.AGREEMENT, java.awt.Color.CYAN); // id software agree on peptide but not ptm certainty
        psmColorMap.put(SpectrumIdentificationPanel.CONFLICT, java.awt.Color.YELLOW); // id software don't agree
        psmColorMap.put(SpectrumIdentificationPanel.PARTIALLY_MISSING, java.awt.Color.ORANGE); // some id software id'ed some didn't

        // set up the psm tooltip map
        HashMap<Integer, String> psmTooltipMap = new HashMap<Integer, String>();
        psmTooltipMap.put(SpectrumIdentificationPanel.AGREEMENT_WITH_MODS, "ID Software Agree");
        psmTooltipMap.put(SpectrumIdentificationPanel.AGREEMENT, "ID Software Agree - PTM Certainty Issues");
        psmTooltipMap.put(SpectrumIdentificationPanel.CONFLICT, "ID Software Disagree");
        psmTooltipMap.put(SpectrumIdentificationPanel.PARTIALLY_MISSING, "First Hit(s) Missing");

        psmTable.getColumn("ID").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.lightGray, psmColorMap, psmTooltipMap));
        psmTable.getColumn("Mass Error").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                -peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPrecursorAccuracy(), peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPrecursorAccuracy(), // @TODO: how to handle negative values..?
                peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        psmTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL,
                (double) ((PSMaps) peptideShakerGUI.getIdentification().getUrParam(new PSMaps())).getPsmSpecificMap().getMaxCharge(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);
        psmTable.getColumn("").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        psmTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));

        try {
            psmTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        } catch (IllegalArgumentException e) {
            psmTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        }

        if (peptideShakerGUI.getIdentificationParameters().getSearchParameters().isPrecursorAccuracyTypePpm()) {
            psmTableToolTips.set(psmTable.getColumn("Mass Error").getModelIndex(), "Mass Error (ppm)");
        } else {
            psmTableToolTips.set(psmTable.getColumn("Mass Error").getModelIndex(), "Mass Error (Da)");
        }

        psmTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        reselect();
                    }
                });
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectJPopupMenu = new javax.swing.JPopupMenu();
        selectAllMenuItem = new javax.swing.JMenuItem();
        deselectAllMenuItem = new javax.swing.JMenuItem();
        sequenceCoverageJPopupMenu = new javax.swing.JPopupMenu();
        coverageShowAllPeptidesJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        coverageShowPossiblePeptidesJCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        coveragePeptideTypesButtonGroup = new javax.swing.ButtonGroup();
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
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) proteinTableToolTips.get(realIndex);
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
        sequenceCoverageTitledPanel = new javax.swing.JPanel();
        sequenceCoverageInnerPanel = new javax.swing.JPanel();
        sequencePtmsPanel = new javax.swing.JPanel();
        sequenceCoveragetHelpJButton = new javax.swing.JButton();
        exportSequenceCoverageContextJButton = new javax.swing.JButton();
        hideCoverageJButton = new javax.swing.JButton();
        sequenceCoverageOptionsJButton = new javax.swing.JButton();
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
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) peptideTableToolTips.get(realIndex);
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
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) psmTableToolTips.get(realIndex);
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
        spectrumContainerJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumSplitPane = new javax.swing.JSplitPane();
        secondarySpectrumPlotsJPanel = new javax.swing.JPanel();
        spectrumOuterJPanel = new javax.swing.JPanel();
        spectrumPaddingPanel = new javax.swing.JPanel();
        spectrumJPanel = new javax.swing.JPanel();
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

        coveragePeptideTypesButtonGroup.add(coverageShowAllPeptidesJRadioButtonMenuItem);
        coverageShowAllPeptidesJRadioButtonMenuItem.setSelected(true);
        coverageShowAllPeptidesJRadioButtonMenuItem.setText("All Peptides");
        coverageShowAllPeptidesJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        sequenceCoverageJPopupMenu.add(coverageShowAllPeptidesJRadioButtonMenuItem);

        coveragePeptideTypesButtonGroup.add(coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem);
        coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem.setText("Enzymatic Peptides");
        coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        sequenceCoverageJPopupMenu.add(coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem);

        coveragePeptideTypesButtonGroup.add(coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem);
        coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem.setText("Non Enzymatic Peptides");
        coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItemActionPerformed(evt);
            }
        });
        sequenceCoverageJPopupMenu.add(coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem);
        sequenceCoverageJPopupMenu.add(jSeparator1);

        coverageShowPossiblePeptidesJCheckBoxMenuItem.setSelected(true);
        coverageShowPossiblePeptidesJCheckBoxMenuItem.setText("Possible Coverage");
        coverageShowPossiblePeptidesJCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coverageShowPossiblePeptidesJCheckBoxMenuItemActionPerformed(evt);
            }
        });
        sequenceCoverageJPopupMenu.add(coverageShowPossiblePeptidesJCheckBoxMenuItem);

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

        proteinTable.setModel(new ProteinTableModel());
        proteinTable.setOpaque(false);
        proteinTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        proteinTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                proteinTableMouseMoved(evt);
            }
        });
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                proteinTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinTableMouseReleased(evt);
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
            .addGap(0, 277, Short.MAX_VALUE)
            .addGroup(proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        proteinsLayeredPane.add(proteinsLayeredPanel);
        proteinsLayeredPanel.setBounds(0, 0, 950, 300);

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
        proteinsLayeredPane.add(proteinsHelpJButton);
        proteinsHelpJButton.setBounds(930, 0, 10, 19);
        proteinsLayeredPane.setLayer(proteinsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportProteinsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setToolTipText("Copy to File");
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
        proteinsLayeredPane.add(exportProteinsJButton);
        exportProteinsJButton.setBounds(920, 0, 10, 19);
        proteinsLayeredPane.setLayer(exportProteinsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        proteinsLayeredPane.add(hideProteinsJButton);
        hideProteinsJButton.setBounds(910, 0, 10, 19);
        proteinsLayeredPane.setLayer(hideProteinsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuProteinsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuProteinsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuProteinsBackgroundPanel);
        contextMenuProteinsBackgroundPanel.setLayout(contextMenuProteinsBackgroundPanelLayout);
        contextMenuProteinsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuProteinsBackgroundPanelLayout.setVerticalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        proteinsLayeredPane.add(contextMenuProteinsBackgroundPanel);
        contextMenuProteinsBackgroundPanel.setBounds(910, 0, 40, 19);
        proteinsLayeredPane.setLayer(contextMenuProteinsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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

        sequenceCoverageTitledPanel.setBackground(new java.awt.Color(255, 255, 255));
        sequenceCoverageTitledPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Protein Sequence Coverage"));
        sequenceCoverageTitledPanel.setOpaque(false);

        sequenceCoverageInnerPanel.setBackground(new java.awt.Color(255, 255, 255));
        sequenceCoverageInnerPanel.setLayout(new javax.swing.BoxLayout(sequenceCoverageInnerPanel, javax.swing.BoxLayout.LINE_AXIS));

        sequencePtmsPanel.setBackground(new java.awt.Color(255, 255, 255));
        sequencePtmsPanel.setOpaque(false);
        sequencePtmsPanel.setLayout(new javax.swing.BoxLayout(sequencePtmsPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout sequenceCoverageTitledPanelLayout = new javax.swing.GroupLayout(sequenceCoverageTitledPanel);
        sequenceCoverageTitledPanel.setLayout(sequenceCoverageTitledPanelLayout);
        sequenceCoverageTitledPanelLayout.setHorizontalGroup(
            sequenceCoverageTitledPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoverageTitledPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sequenceCoverageTitledPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sequenceCoverageInnerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 914, Short.MAX_VALUE)
                    .addComponent(sequencePtmsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 918, Short.MAX_VALUE))
                .addContainerGap())
        );
        sequenceCoverageTitledPanelLayout.setVerticalGroup(
            sequenceCoverageTitledPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoverageTitledPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sequenceCoverageInnerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(sequencePtmsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 7, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        sequenceCoverageLayeredPane.add(sequenceCoverageTitledPanel);
        sequenceCoverageTitledPanel.setBounds(0, 0, 950, 70);

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
        sequenceCoverageLayeredPane.add(sequenceCoveragetHelpJButton);
        sequenceCoveragetHelpJButton.setBounds(930, 0, 10, 19);
        sequenceCoverageLayeredPane.setLayer(sequenceCoveragetHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        sequenceCoverageLayeredPane.add(exportSequenceCoverageContextJButton);
        exportSequenceCoverageContextJButton.setBounds(920, 0, 10, 19);
        sequenceCoverageLayeredPane.setLayer(exportSequenceCoverageContextJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        sequenceCoverageLayeredPane.add(hideCoverageJButton);
        hideCoverageJButton.setBounds(910, 0, 10, 19);
        sequenceCoverageLayeredPane.setLayer(hideCoverageJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        sequenceCoverageOptionsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_gray.png"))); // NOI18N
        sequenceCoverageOptionsJButton.setToolTipText("Coverage Options");
        sequenceCoverageOptionsJButton.setBorder(null);
        sequenceCoverageOptionsJButton.setBorderPainted(false);
        sequenceCoverageOptionsJButton.setContentAreaFilled(false);
        sequenceCoverageOptionsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_black.png"))); // NOI18N
        sequenceCoverageOptionsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sequenceCoverageOptionsJButtonMouseReleased(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sequenceCoverageOptionsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sequenceCoverageOptionsJButtonMouseExited(evt);
            }
        });
        sequenceCoverageLayeredPane.add(sequenceCoverageOptionsJButton);
        sequenceCoverageOptionsJButton.setBounds(895, 5, 10, 19);
        sequenceCoverageLayeredPane.setLayer(sequenceCoverageOptionsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSequenceCoverageBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSequenceCoverageBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSequenceCoverageBackgroundPanel);
        contextMenuSequenceCoverageBackgroundPanel.setLayout(contextMenuSequenceCoverageBackgroundPanelLayout);
        contextMenuSequenceCoverageBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSequenceCoverageBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 50, Short.MAX_VALUE)
        );
        contextMenuSequenceCoverageBackgroundPanelLayout.setVerticalGroup(
            contextMenuSequenceCoverageBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        sequenceCoverageLayeredPane.add(contextMenuSequenceCoverageBackgroundPanel);
        contextMenuSequenceCoverageBackgroundPanel.setBounds(890, 0, 50, 19);
        sequenceCoverageLayeredPane.setLayer(contextMenuSequenceCoverageBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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

        peptideTable.setModel(new PeptideTableModel());
        peptideTable.setOpaque(false);
        peptideTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        peptideTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptideTableMouseMoved(evt);
            }
        });
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                peptideTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptideTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
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
            .addGap(0, 147, Short.MAX_VALUE)
            .addGroup(peptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(peptidesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        peptidesLayeredPane.add(peptidesPanel);
        peptidesPanel.setBounds(0, 0, 450, 170);

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
        peptidesLayeredPane.add(peptidesHelpJButton);
        peptidesHelpJButton.setBounds(430, 0, 10, 19);
        peptidesLayeredPane.setLayer(peptidesHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPeptidesJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesJButton.setToolTipText("Copy to File");
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
        peptidesLayeredPane.add(exportPeptidesJButton);
        exportPeptidesJButton.setBounds(420, 0, 10, 19);
        peptidesLayeredPane.setLayer(exportPeptidesJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        peptidesLayeredPane.add(hidePeptideAndPsmsJButton);
        hidePeptideAndPsmsJButton.setBounds(410, 0, 10, 19);
        peptidesLayeredPane.setLayer(hidePeptideAndPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPeptidesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPeptidesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPeptidesBackgroundPanel);
        contextMenuPeptidesBackgroundPanel.setLayout(contextMenuPeptidesBackgroundPanelLayout);
        contextMenuPeptidesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuPeptidesBackgroundPanelLayout.setVerticalGroup(
            contextMenuPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        peptidesLayeredPane.add(contextMenuPeptidesBackgroundPanel);
        contextMenuPeptidesBackgroundPanel.setBounds(400, 0, 40, 19);
        peptidesLayeredPane.setLayer(contextMenuPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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

        psmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide Spectrum Matches"));
        psmsPanel.setOpaque(false);

        spectraScrollPane.setOpaque(false);

        psmTable.setModel(new PsmTableModel());
        psmTable.setOpaque(false);
        psmTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                psmTableMouseMoved(evt);
            }
        });
        psmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                psmTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                psmTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                psmTableMouseReleased(evt);
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
            .addGap(0, 147, Short.MAX_VALUE)
            .addGroup(psmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(psmsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        psmsLayeredPane.add(psmsPanel);
        psmsPanel.setBounds(0, 0, 450, 170);

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
        psmsHelpJButton.setBounds(430, 0, 10, 19);
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
        exportPsmsJButton.setBounds(420, 0, 10, 19);
        psmsLayeredPane.setLayer(exportPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        psmsLayeredPane.add(hidePeptideAndPsmsJButton2);
        hidePeptideAndPsmsJButton2.setBounds(410, 0, 10, 19);
        psmsLayeredPane.setLayer(hidePeptideAndPsmsJButton2, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPsmsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPsmsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPsmsBackgroundPanel);
        contextMenuPsmsBackgroundPanel.setLayout(contextMenuPsmsBackgroundPanelLayout);
        contextMenuPsmsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuPsmsBackgroundPanelLayout.setVerticalGroup(
            contextMenuPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        psmsLayeredPane.add(contextMenuPsmsBackgroundPanel);
        contextMenuPsmsBackgroundPanel.setBounds(400, 0, 40, 19);
        psmsLayeredPane.setLayer(contextMenuPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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
        spectrumJTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spectrumJTabbedPaneStateChanged(evt);
            }
        });
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
                    .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
                    .addComponent(ionTableJToolBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE))
                .addContainerGap())
        );
        fragmentIonJPanelLayout.setVerticalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE)
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
                .addContainerGap(288, Short.MAX_VALUE)
                .addComponent(bubblePlotJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(bubblePlotTabJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(bubblePlotTabJPanelLayout.createSequentialGroup()
                    .addComponent(bubbleJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 263, Short.MAX_VALUE)
                    .addGap(25, 25, 25)))
        );

        spectrumJTabbedPane.addTab("Bubble Plot", bubblePlotTabJPanel);

        spectrumContainerJPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(spectrumAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumJToolBar.add(spectrumAnnotationMenuPanel);

        spectrumSplitPane.setBackground(new java.awt.Color(255, 255, 255));
        spectrumSplitPane.setBorder(null);
        spectrumSplitPane.setDividerLocation(80);
        spectrumSplitPane.setDividerSize(0);
        spectrumSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        secondarySpectrumPlotsJPanel.setMinimumSize(new java.awt.Dimension(0, 80));
        secondarySpectrumPlotsJPanel.setOpaque(false);
        secondarySpectrumPlotsJPanel.setLayout(new javax.swing.BoxLayout(secondarySpectrumPlotsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumSplitPane.setTopComponent(secondarySpectrumPlotsJPanel);

        spectrumOuterJPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectrumPaddingPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout spectrumPaddingPanelLayout = new javax.swing.GroupLayout(spectrumPaddingPanel);
        spectrumPaddingPanel.setLayout(spectrumPaddingPanelLayout);
        spectrumPaddingPanelLayout.setHorizontalGroup(
            spectrumPaddingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        spectrumPaddingPanelLayout.setVerticalGroup(
            spectrumPaddingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 17, Short.MAX_VALUE)
        );

        spectrumJPanel.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJPanel.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout spectrumOuterJPanelLayout = new javax.swing.GroupLayout(spectrumOuterJPanel);
        spectrumOuterJPanel.setLayout(spectrumOuterJPanelLayout);
        spectrumOuterJPanelLayout.setHorizontalGroup(
            spectrumOuterJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumPaddingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
        );
        spectrumOuterJPanelLayout.setVerticalGroup(
            spectrumOuterJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumOuterJPanelLayout.createSequentialGroup()
                .addComponent(spectrumPaddingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE))
        );

        spectrumSplitPane.setRightComponent(spectrumOuterJPanel);

        javax.swing.GroupLayout spectrumContainerJPanelLayout = new javax.swing.GroupLayout(spectrumContainerJPanel);
        spectrumContainerJPanel.setLayout(spectrumContainerJPanelLayout);
        spectrumContainerJPanelLayout.setHorizontalGroup(
            spectrumContainerJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumContainerJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumSplitPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        spectrumContainerJPanelLayout.setVerticalGroup(
            spectrumContainerJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumContainerJPanelLayout.createSequentialGroup()
                .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumJTabbedPane.addTab("Spectrum", spectrumContainerJPanel);

        spectrumJTabbedPane.setSelectedIndex(2);

        slidersSplitPane.setLeftComponent(spectrumJTabbedPane);

        slidersPanel.setOpaque(false);

        accuracySlider.setOrientation(javax.swing.JSlider.VERTICAL);
        accuracySlider.setPaintTicks(true);
        accuracySlider.setToolTipText("Annotation Accuracy");
        accuracySlider.setValue(100);
        accuracySlider.setOpaque(false);
        accuracySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                accuracySliderStateChanged(evt);
            }
        });
        accuracySlider.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                accuracySliderMouseWheelMoved(evt);
            }
        });

        intensitySlider.setOrientation(javax.swing.JSlider.VERTICAL);
        intensitySlider.setPaintTicks(true);
        intensitySlider.setToolTipText("Annotation Level");
        intensitySlider.setValue(75);
        intensitySlider.setOpaque(false);
        intensitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                intensitySliderStateChanged(evt);
            }
        });
        intensitySlider.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                intensitySliderMouseWheelMoved(evt);
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
                .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                .addGap(29, 29, 29)
                .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
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

        spectrumLayeredPane.add(spectrumMainPanel);
        spectrumMainPanel.setBounds(0, 0, 490, 350);

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
        spectrumHelpJButton.setBounds(460, 0, 10, 19);
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
        spectrumLayeredPane.add(exportSpectrumJButton);
        exportSpectrumJButton.setBounds(450, 0, 10, 19);
        spectrumLayeredPane.setLayer(exportSpectrumJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        spectrumLayeredPane.add(hideSpectrumPanelJButton);
        hideSpectrumPanelJButton.setBounds(440, 0, 10, 19);
        spectrumLayeredPane.setLayer(hideSpectrumPanelJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        maximizeSpectrumPanelJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/maximize_grey.png"))); // NOI18N
        maximizeSpectrumPanelJButton.setToolTipText("Maximize Spectrum");
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
        spectrumLayeredPane.add(maximizeSpectrumPanelJButton);
        maximizeSpectrumPanelJButton.setBounds(425, 5, 10, 19);
        spectrumLayeredPane.setLayer(maximizeSpectrumPanelJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuSpectrumBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuSpectrumBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuSpectrumBackgroundPanel);
        contextMenuSpectrumBackgroundPanel.setLayout(contextMenuSpectrumBackgroundPanelLayout);
        contextMenuSpectrumBackgroundPanelLayout.setHorizontalGroup(
            contextMenuSpectrumBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 60, Short.MAX_VALUE)
        );
        contextMenuSpectrumBackgroundPanelLayout.setVerticalGroup(
            contextMenuSpectrumBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        spectrumLayeredPane.add(contextMenuSpectrumBackgroundPanel);
        contextMenuSpectrumBackgroundPanel.setBounds(420, 0, 60, 19);
        spectrumLayeredPane.setLayer(contextMenuSpectrumBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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
                .addComponent(overviewJSplitPane)
                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane)
                .addContainerGap())
        );

        backgroundLayeredPane.add(overviewJPanel);
        overviewJPanel.setBounds(0, 0, 980, 720);

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

        backgroundLayeredPane.add(toolBar);
        toolBar.setBounds(0, 720, 980, 20);

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
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            proteinTableMouseReleased(null);
        }
    }//GEN-LAST:event_proteinTableKeyReleased

    /**
     * Updates the tables according to the currently selected peptide.
     *
     * @param evt
     */
    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased
        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            int peptideRow = peptideTable.getSelectedRow();

            if (peptideRow != -1) {
                peptideTableMouseReleased(null);
            }
        }
    }//GEN-LAST:event_peptideTableKeyReleased

    /**
     * Updates the tables according to the currently selected PSM.
     *
     * @param evt
     */
    private void psmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmTableKeyReleased
        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            final int row = psmTable.getSelectedRow();

            if (row != -1) {
                newItemSelection();
                updateSpectrum(row, false);
            }

            // update the annotation menu
            spectrumJTabbedPaneStateChanged(null);
        }
    }//GEN-LAST:event_psmTableKeyReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link.
     *
     * @param evt
     */
    private void proteinTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseMoved
        int row = proteinTable.rowAtPoint(evt.getPoint());
        int column = proteinTable.columnAtPoint(evt.getPoint());

        proteinTable.setToolTipText(null);

        if (row != -1 && column != -1 && column == proteinTable.getColumn("Accession").getModelIndex() && proteinTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == proteinTable.getColumn("PI").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (column == proteinTable.getColumn("Chr").getModelIndex() && proteinTable.getValueAt(row, column) != null && geneFactory.isMappingFileOpen()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (column == proteinTable.getColumn("").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (column == proteinTable.getColumn("Description").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            if (GuiUtilities.getPreferredWidthOfCell(proteinTable, row, column) > proteinTable.getColumn("Description").getWidth()) {
                proteinTable.setToolTipText("" + proteinTable.getValueAt(row, column));
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_proteinTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
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

            newItemSelection();
            updateSpectrum(row, false);

            // star/unstar a psm
            if (column == psmTable.getColumn("  ").getModelIndex()) {
                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
                int psmIndex = tableModel.getViewIndex(row);
                String psmKey = psmKeys.get(tableModel.getViewIndex(psmIndex));
                try {
                    PSParameter psParameter = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(psmKey, new PSParameter());
                    if (!psParameter.isStarred()) {
                        peptideShakerGUI.getStarHider().starPsm(psmKey, peptideShakerGUI.getSpectrumAnnotator());
                    } else {
                        peptideShakerGUI.getStarHider().unStarPsm(psmKey, peptideShakerGUI.getSpectrumAnnotator());
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }

            if (column == 2 && evt != null && evt.getButton() == 1) {
                peptideShakerGUI.jumpToTab(PeptideShakerGUI.SPECTRUM_ID_TAB_INDEX);
            }

            // open the match validation level dialog
            if (column == psmTable.getColumn("").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1) {
                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
                int psmIndex = tableModel.getViewIndex(row);
                String psmKey = psmKeys.get(tableModel.getViewIndex(psmIndex));
                Identification identification = peptideShakerGUI.getIdentification();
                PSMaps pSMaps = new PSMaps();
                pSMaps = (PSMaps) identification.getUrParam(pSMaps);
                try {
                    MatchValidationDialog matchValidationDialog = new MatchValidationDialog(peptideShakerGUI, peptideShakerGUI.getExceptionHandler(),
                            identification, peptideShakerGUI.getIdentificationFeaturesGenerator(), pSMaps.getPsmSpecificMap(), psmKey,
                            peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters());
                    if (matchValidationDialog.isValidationChanged()) {
                        updatePsmPanelTitle();
                        peptidesPanel.repaint();
                        proteinsLayeredPanel.repaint();
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }

        // update the annotation menu
        spectrumJTabbedPaneStateChanged(null);
    }//GEN-LAST:event_psmTableMouseReleased

    /**
     * Updates the tables according to the currently selected protein.
     *
     * @param evt
     */
    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased

        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        int proteinIndex = -1;

        if (row != -1) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            proteinIndex = tableModel.getViewIndex(row);
        }

        if (evt == null || (evt.getButton() == MouseEvent.BUTTON1 && (proteinIndex != -1 && column != -1))) {

            if (proteinIndex != -1) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                String proteinKey = proteinKeys.get(proteinIndex);
                peptideShakerGUI.setSelectedItems(proteinKeys.get(proteinIndex), PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION);

                // update the peptide selection
                updatePeptideSelection(proteinIndex);

                // remember the selection
                newItemSelection();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        proteinTable.requestFocus(); // @TODO: not really sure why this is now needed..?
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    }
                });

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // star/unstar a protein
                if (column == proteinTable.getColumn("  ").getModelIndex()) {
                    try {
                        PSParameter psParameter = (PSParameter) peptideShakerGUI.getIdentification().getProteinMatchParameter(proteinKey, new PSParameter());
                        if (!psParameter.isStarred()) {
                            peptideShakerGUI.getStarHider().starProtein(proteinKey);
                        } else {
                            peptideShakerGUI.getStarHider().unStarProtein(proteinKey);
                        }
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                }

                // open the gene details dialog
                if (column == proteinTable.getColumn("Chr").getModelIndex() && evt != null
                        && evt.getButton() == MouseEvent.BUTTON1 && geneFactory.isMappingFileOpen()) {
                    try {
                        new GeneDetailsDialog(peptideShakerGUI, proteinKey);
                    } catch (Exception ex) {
                        peptideShakerGUI.catchException(ex);
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
                    new ProteinInferenceDialog(peptideShakerGUI, proteinKey, peptideShakerGUI.getIdentification());
                }

                // open the match validation level dialog
                if (column == proteinTable.getColumn("").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1) {
                    Identification identification = peptideShakerGUI.getIdentification();
                    PSMaps pSMaps = new PSMaps();
                    pSMaps = (PSMaps) identification.getUrParam(pSMaps);
                    try {
                        MatchValidationDialog matchValidationDialog = new MatchValidationDialog(peptideShakerGUI, peptideShakerGUI.getExceptionHandler(),
                                identification, peptideShakerGUI.getIdentificationFeaturesGenerator(), pSMaps.getProteinMap(), proteinKey,
                                peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters());
                        if (matchValidationDialog.isValidationChanged()) {
                            updateProteinPanelTitle();
                        }
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
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

        int row = peptideTable.getSelectedRow();
        final int column = peptideTable.getSelectedColumn();

        if (row != -1) {

            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) peptideTable.getModel();
            int peptideIndex = tableModel.getViewIndex(row);
            String peptideKey = peptideKeys.get(peptideIndex);

            peptideShakerGUI.setSelectedItems(peptideShakerGUI.getSelectedProteinKey(), peptideKey, PeptideShakerGUI.NO_SELECTION);

            // update the psm selection
            updatePsmSelection(row, false);

            // new peptide, reset spectrum boundaries
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (psmTable.getSelectedRow() != -1) {
                        SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
                        updateSpectrum(tableModel.getViewIndex(psmTable.getSelectedRow()), true);
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                    }
                }
            });

            // star/unstar a peptide
            if (column == peptideTable.getColumn("  ").getModelIndex()) {
                try {
                    PSParameter psParameter = (PSParameter) peptideShakerGUI.getIdentification().getPeptideMatchParameter(peptideKey, new PSParameter());
                    if (!psParameter.isStarred()) {
                        peptideShakerGUI.getStarHider().starPeptide(peptideKey);
                    } else {
                        peptideShakerGUI.getStarHider().unStarPeptide(peptideKey);
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }

            // open the protein inference at the petide level dialog
            if (column == peptideTable.getColumn("PI").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1) {
                try {
                    tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
                    String proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));
                    new ProteinInferencePeptideLevelDialog(peptideShakerGUI, true, peptideKey, proteinKey);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }

            // open the match validation level dialog
            if (column == peptideTable.getColumn("").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1) {
                Identification identification = peptideShakerGUI.getIdentification();
                PSMaps pSMaps = new PSMaps();
                pSMaps = (PSMaps) identification.getUrParam(pSMaps);
                try {
                    MatchValidationDialog matchValidationDialog = new MatchValidationDialog(peptideShakerGUI, peptideShakerGUI.getExceptionHandler(),
                            identification, peptideShakerGUI.getIdentificationFeaturesGenerator(), pSMaps.getPeptideSpecificMap(), peptideKey,
                            peptideShakerGUI.getShotgunProtocol(), peptideShakerGUI.getIdentificationParameters());
                    if (matchValidationDialog.isValidationChanged()) {
                        updateProteinPanelTitle();
                        updatePeptidePanelTitle();
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        updateSelection(true);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                }
            });
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
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details if over the sequence
     * column.
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
            } else if (column == peptideTable.getColumn("").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                peptideTable.setToolTipText(null);
            } else if (column == peptideTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                // check if we ought to show a tooltip with mod details
                String sequence = (String) peptideTable.getValueAt(row, column);

                if (sequence.contains("<span")) {
                    try {
                        SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) peptideTable.getModel();
                        String peptideKey = peptideKeys.get(tableModel.getViewIndex(row));
                        String tooltip = peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey));
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
                peptideShakerGUI.updateAnnotationMenuBarVisableOptions(false, false, true, false, false);
            } else if (index == 1) {
                bubbleAnnotationMenuPanel.removeAll();
                bubbleAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
                peptideShakerGUI.updateAnnotationMenuBarVisableOptions(false, true, false, false, false);
            } else if (index == 2) {
                spectrumAnnotationMenuPanel.removeAll();
                spectrumAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
                peptideShakerGUI.updateAnnotationMenuBarVisableOptions(true, false, false, false, psmTable.getSelectedRowCount() == 1);
            }
        }

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (psmTable.getSelectedRowCount() > 1) {
                    spectrumSplitPane.setDividerLocation(0);
                } else {
                    spectrumSplitPane.setDividerLocation(spectrumSubPlotDividerLocation);
                }

                spectrumMainJPanel.revalidate();
                spectrumMainJPanel.repaint();
            }
        });
    }//GEN-LAST:event_spectrumJTabbedPaneStateChanged

    /**
     * Updates the slider value when the user scrolls.
     *
     * @param evt
     */
    private void spectrumJTabbedPaneMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_spectrumJTabbedPaneMouseWheelMoved

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
    }//GEN-LAST:event_spectrumJTabbedPaneMouseWheelMoved

    /**
     * Updates the intensity annotation limit.
     *
     * @param evt
     */
    private void intensitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_intensitySliderStateChanged
        AnnotationPreferences annotationPreferences = peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences();
        annotationPreferences.setAnnotationLevel(intensitySlider.getValue() / 100.0);
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

                if (sequence.contains("<span")) {
                    try {
                        SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) peptideTable.getModel();
                        int peptideIndex = tableModel.getViewIndex(peptideTable.getSelectedRow());
                        String peptideKey = peptideKeys.get(peptideIndex);
                        PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);

                        tableModel = (SelfUpdatingTableModel) psmTable.getModel();
                        String spectrumKey = psmKeys.get(tableModel.getViewIndex(row));
                        SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                        SequenceMatchingPreferences sequenceMatchingPreferences = peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences();
                        if (peptideAssumption.getPeptide().getMatchingKey(sequenceMatchingPreferences).equals(currentPeptideMatch.getTheoreticPeptide().getMatchingKey(sequenceMatchingPreferences))) {
                            String tooltip = peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(currentPeptideMatch);
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
            if (column == 2 || column == psmTable.getColumn("").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
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
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getIdentificationParameters().getSearchParameters().getFragmentIonAccuracy();
        AnnotationPreferences annotationPreferences = peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences();
        annotationPreferences.setFragmentIonAccuracy(accuracy);
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
                        -2,
                        spectrumLayeredPane.getComponent(3).getWidth(),
                        spectrumLayeredPane.getComponent(3).getHeight());

                spectrumLayeredPane.getComponent(4).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(4).getWidth() - 5,
                        -3,
                        spectrumLayeredPane.getComponent(4).getWidth(),
                        spectrumLayeredPane.getComponent(4).getHeight());

                // resize the plot area
                spectrumLayeredPane.getComponent(5).setBounds(0, 0, spectrumLayeredPane.getWidth(), spectrumLayeredPane.getHeight());

                if (psmTable.getSelectedRowCount() > 1) {
                    spectrumSplitPane.setDividerLocation(0);
                } else {
                    spectrumSplitPane.setDividerLocation(spectrumSubPlotDividerLocation);
                }

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
                        sequenceCoverageLayeredPane.getWidth() - sequenceCoverageLayeredPane.getComponent(3).getWidth() - 44,
                        0,
                        sequenceCoverageLayeredPane.getComponent(3).getWidth(),
                        sequenceCoverageLayeredPane.getComponent(3).getHeight());

                sequenceCoverageLayeredPane.getComponent(4).setBounds(
                        sequenceCoverageLayeredPane.getWidth() - sequenceCoverageLayeredPane.getComponent(4).getWidth() - 5,
                        -3,
                        sequenceCoverageLayeredPane.getComponent(4).getWidth(),
                        sequenceCoverageLayeredPane.getComponent(4).getHeight());

                // resize the plot area
                sequenceCoverageLayeredPane.getComponent(5).setBounds(0, 0, sequenceCoverageLayeredPane.getWidth(), sequenceCoverageLayeredPane.getHeight());
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#Proteins",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
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
        copyTableContentToClipboardOrFile(TableIndex.PROTEIN_TABLE);
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#Peptides",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
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
        copyTableContentToClipboardOrFile(TableIndex.PEPTIDE_TABLE);
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#PSMs",
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
        copyTableContentToClipboardOrFile(TableIndex.PSM_TABLE);
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#Spectrum",
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/OverviewTab.html"), "#SequenceCoverage",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
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
            if (proteinTable.getSelectedRow() != -1) {

                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
                String proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));
                Protein protein = sequenceFactory.getProtein(proteinKey);

                String clipboardString = protein.getSequence();
                StringSelection stringSelection = new StringSelection(clipboardString);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, peptideShakerGUI);

                JOptionPane.showMessageDialog(peptideShakerGUI, "Protein sequence copied to clipboard.", "Copied to Clipboard", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
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

        if (panelsShownUponMaximze != null) {

            displayProteins = panelsShownUponMaximze.get(0);
            displayPeptidesAndPSMs = panelsShownUponMaximze.get(1);
            displayCoverage = panelsShownUponMaximze.get(2);
            displaySpectrum = true;

            panelsShownUponMaximze = null;
        } else {
            panelsShownUponMaximze = new ArrayList<Boolean>();
            panelsShownUponMaximze.add(displayProteins);
            panelsShownUponMaximze.add(displayPeptidesAndPSMs);
            panelsShownUponMaximze.add(displayCoverage);

            displayProteins = false;
            displayPeptidesAndPSMs = false;
            displayCoverage = false;
            displaySpectrum = true;
        }

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

            String key = proteinKeys.get(i);
            PSParameter psParameter = new PSParameter();
            try {
                psParameter = (PSParameter) peptideShakerGUI.getIdentification().getProteinMatchParameter(key, psParameter);
                psParameter.setStarred(true);
                peptideShakerGUI.getIdentification().updateProteinMatchParameter(key, psParameter);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
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

            String proteinKey = proteinKeys.get(i);
            PSParameter psParameter = new PSParameter();
            try {
                psParameter = (PSParameter) peptideShakerGUI.getIdentification().getProteinMatchParameter(proteinKey, psParameter);
                psParameter.setStarred(false);
                peptideShakerGUI.getIdentification().updateProteinMatchParameter(proteinKey, psParameter);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
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
            JMenuItem menuItem = new JMenuItem("Spectrum as MGF");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSpectrumAsMgf();
                }
            });

            popupMenu.add(menuItem);
        } else if (index == 1) { // bubble plot
            JMenuItem menuItem = new JMenuItem("Bubble Plot");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportBubblePlotAsFigure();
                }
            });

            popupMenu.add(menuItem);

            menuItem = new JMenuItem("Spectrum as MGF");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSpectrumAsMgf();
                }
            });

            popupMenu.add(menuItem);
        } else if (index == 2) { // spectrum
            JMenuItem menuItem = new JMenuItem("Spectrum");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    peptideShakerGUI.exportSpectrumAsFigure();
                }
            });

            popupMenu.add(menuItem);

            if (psmTable.getSelectedRowCount() == 1) {

                menuItem = new JMenuItem("Spectrum & Plots");
                menuItem.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, spectrumSplitPane, peptideShakerGUI.getLastSelectedFolder());
                    }
                });

                popupMenu.add(menuItem);

                popupMenu.add(new JSeparator());

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

                menuItem = new JMenuItem("Spectrum as MGF");
                menuItem.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        peptideShakerGUI.exportSpectrumAsMgf();
                    }
                });
            }

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

    /**
     * Hide the spectrum panel.
     *
     * @param evt
     */
    private void hideSpectrumPanelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideSpectrumPanelJButtonActionPerformed
        displaySpectrum = false;
        peptideShakerGUI.setDisplayOptions(displayProteins, displayPeptidesAndPSMs, displayCoverage, displaySpectrum);
    }//GEN-LAST:event_hideSpectrumPanelJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void sequenceCoverageOptionsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sequenceCoverageOptionsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_sequenceCoverageOptionsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void sequenceCoverageOptionsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sequenceCoverageOptionsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_sequenceCoverageOptionsJButtonMouseExited

    /**
     * Show the sequence coverage options.
     *
     * @param evt
     */
    private void sequenceCoverageOptionsJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sequenceCoverageOptionsJButtonMouseReleased
        sequenceCoverageJPopupMenu.show(sequenceCoverageOptionsJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_sequenceCoverageOptionsJButtonMouseReleased

    /**
     * Update the sequence coverage map.
     *
     * @param evt
     */
    private void coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed
        if (proteinTable.getSelectedRow() != -1) {
            try {
                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
                String proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                updateSequenceCoverage(proteinKey, proteinMatch.getMainMatch(), true);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }
    }//GEN-LAST:event_coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed

    /**
     * @see #coverageShowAllPeptidesJRadioButtonMenuItem
     */
    private void coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItemActionPerformed
        coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(null);
    }//GEN-LAST:event_coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItemActionPerformed

    /**
     * @see #coverageShowAllPeptidesJRadioButtonMenuItem
     */
    private void coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItemActionPerformed
        coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(null);
    }//GEN-LAST:event_coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItemActionPerformed

    /**
     * @see #coverageShowAllPeptidesJRadioButtonMenuItem
     */
    private void coverageShowPossiblePeptidesJCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_coverageShowPossiblePeptidesJCheckBoxMenuItemActionPerformed
        coverageShowAllPeptidesJRadioButtonMenuItemActionPerformed(null);
    }//GEN-LAST:event_coverageShowPossiblePeptidesJCheckBoxMenuItemActionPerformed

    /**
     * Show the statistics popup menu.
     *
     * @param evt
     */
    private void proteinTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3 && proteinTable.getRowCount() > 0) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Statistics (beta)");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    new XYPlottingDialog(peptideShakerGUI, proteinTable, proteinTableToolTips,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true);
                }
            });
            popupMenu.add(menuItem);
            popupMenu.show(proteinTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_proteinTableMouseClicked

    /**
     * Show the statistics popup menu.
     *
     * @param evt
     */
    private void peptideTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3 && peptideTable.getRowCount() > 0) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Statistics (beta)");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    new XYPlottingDialog(peptideShakerGUI, peptideTable, peptideTableToolTips,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true);
                }
            });
            popupMenu.add(menuItem);
            popupMenu.show(peptideTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_peptideTableMouseClicked

    /**
     * Show the statistics popup menu.
     *
     * @param evt
     */
    private void psmTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3 && psmTable.getRowCount() > 0) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Statistics (beta)");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    new XYPlottingDialog(peptideShakerGUI, psmTable, psmTableToolTips,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true);
                }
            });
            popupMenu.add(menuItem);
            popupMenu.show(psmTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_psmTableMouseClicked

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void psmTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_psmTableMouseExited

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
    private javax.swing.ButtonGroup coveragePeptideTypesButtonGroup;
    private javax.swing.JRadioButtonMenuItem coverageShowAllPeptidesJRadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem;
    private javax.swing.JCheckBoxMenuItem coverageShowPossiblePeptidesJCheckBoxMenuItem;
    private javax.swing.JRadioButtonMenuItem coverageShowTruncatedPeptidesOnlyJRadioButtonMenuItem;
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
    private javax.swing.JPopupMenu.Separator jSeparator1;
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
    private javax.swing.JPanel sequenceCoverageInnerPanel;
    private javax.swing.JPanel sequenceCoverageJPanel;
    private javax.swing.JPopupMenu sequenceCoverageJPopupMenu;
    private javax.swing.JLayeredPane sequenceCoverageLayeredPane;
    private javax.swing.JButton sequenceCoverageOptionsJButton;
    private javax.swing.JPanel sequenceCoverageTitledPanel;
    private javax.swing.JButton sequenceCoveragetHelpJButton;
    private javax.swing.JPanel sequencePtmsPanel;
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
    private javax.swing.JPanel spectrumContainerJPanel;
    private javax.swing.JButton spectrumHelpJButton;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JTabbedPane spectrumJTabbedPane;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JLayeredPane spectrumLayeredPane;
    private javax.swing.JPanel spectrumMainJPanel;
    private javax.swing.JPanel spectrumMainPanel;
    private javax.swing.JPanel spectrumOuterJPanel;
    private javax.swing.JPanel spectrumPaddingPanel;
    private javax.swing.JSplitPane spectrumSplitPane;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables

    /**
     * Updates the protein panel title with the number of validated/confident
     * proteins.
     */
    public void updateProteinPanelTitle() {
        String title = PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Proteins (";
        try {
            int nValidated = peptideShakerGUI.getIdentificationFeaturesGenerator().getNValidatedProteins();
            int nConfident = peptideShakerGUI.getIdentificationFeaturesGenerator().getNConfidentProteins();
            int nProteins = proteinTable.getRowCount();
            if (nConfident > 0) {
                title += nValidated + "/" + nProteins + " - " + nConfident + " confident, " + (nValidated - nConfident) + " doubtful";
            } else {
                title += nValidated + "/" + nProteins;
            }
        } catch (Exception eNValidated) {
            peptideShakerGUI.catchException(eNValidated);
        }
        title += ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING;
        ((TitledBorder) proteinsLayeredPanel.getBorder()).setTitle(title);
        proteinsLayeredPanel.repaint();
    }

    /**
     * Updates the peptide panel title with the number of validated/confident
     * proteins.
     */
    public void updatePeptidePanelTitle() {
        String title = PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptides (";
        try {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            IdentificationFeaturesGenerator identificationFeaturesGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();
            String proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));
            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
            int nValidatedPeptides = identificationFeaturesGenerator.getNValidatedPeptides(proteinKey);
            int nConfidentPeptides = identificationFeaturesGenerator.getNConfidentPeptides(proteinKey);
            int nPeptides = proteinMatch.getPeptideCount();
            if (nConfidentPeptides > 0) {
                title += nValidatedPeptides + "/" + nPeptides + " - " + nConfidentPeptides + " confident, " + (nValidatedPeptides - nConfidentPeptides) + " doubtful";
            } else {
                title += nValidatedPeptides + "/" + nPeptides;
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        title += ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING;

        ((TitledBorder) peptidesPanel.getBorder()).setTitle(title);
        peptidesPanel.repaint();
    }

    /**
     * Updates the PSM panel title with the number of validated/confident
     * proteins.
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     */
    public void updatePsmPanelTitle() throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) peptideTable.getModel();
        IdentificationFeaturesGenerator identificationFeaturesGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();
        int peptideIndex = tableModel.getViewIndex(peptideTable.getSelectedRow());
        String peptideKey = peptideKeys.get(peptideIndex);
        int nValidatedPsms = identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideKey);
        int nConfidentPsms = identificationFeaturesGenerator.getNConfidentSpectraForPeptide(peptideKey);
        int nPsms = psmTable.getRowCount();
        String title = PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide Spectrum Matches (";
        if (nConfidentPsms > 0) {
            title += nValidatedPsms + "/" + nPsms + " - " + nConfidentPsms + " confident, " + (nValidatedPsms - nConfidentPsms) + " doubtful";
        } else {
            title += nValidatedPsms + "/" + nPsms;
        }
        title += ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING;
        ((TitledBorder) psmsPanel.getBorder()).setTitle(title);
        psmsPanel.repaint();
    }

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);

        try {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (IllegalArgumentException e) {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        }

        ((JSparklinesMultiIntervalChartTableCellRenderer) peptideTable.getColumn("Start").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesArrayListBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);

        try {
            ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (IllegalArgumentException e) {
            ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        }

        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Mass Error").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);

        try {
            ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
        } catch (IllegalArgumentException e) {
            ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        }

        proteinTable.revalidate();
        proteinTable.repaint();

        peptideTable.revalidate();
        peptideTable.repaint();

        psmTable.revalidate();
        psmTable.repaint();
    }

    /**
     * Returns a list of keys of the displayed proteins
     *
     * @return a list of keys of the displayed proteins
     */
    public ArrayList<String> getDisplayedProteins() {
        return proteinKeys;
    }

    /**
     * Returns a list of keys of the displayed peptides
     *
     * @return a list of keys of the displayed peptides
     */
    public ArrayList<String> getDisplayedPeptides() {
        return peptideKeys;
    }

    /**
     * Returns a list of keys of the displayed PSMs
     *
     * @return a list of keys of the displayed PSMs
     */
    public ArrayList<String> getDisplayedPsms() {
        return psmKeys;
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
     * Updates the split panel divider location for the peptide/psm and spectrum
     * panel.
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
     * Method called whenever the component is resized to maintain the look of
     * the GUI.
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
     * Sets the whether the protein coverage and the spectrum shall be
     * displayed.
     *
     * @param displayProteins boolean indicating whether the proteins shall be
     * displayed
     * @param displayPeptidesAndPSMs boolean indicating whether the peptides and
     * PSMs shall be displayed
     * @param displayCoverage boolean indicating whether the protein coverage
     * shall be displayed
     * @param displaySpectrum boolean indicating whether the spectrum shall be
     * displayed
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

                // get the currenly selected rows in the psm table
                int[] selectedPsmRows = psmTable.getSelectedRows();
                SelfUpdatingTableModel psmTableModel = (SelfUpdatingTableModel) psmTable.getModel();

                // iterate the selected psms rows
                for (int row : selectedPsmRows) {
                    int psmIndex = psmTableModel.getViewIndex(row);
                    String spectrumKey = psmKeys.get(psmIndex);
                    selectedPsmKeys.add(spectrumKey);
                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                    selectedIndexes.add((psmIndex + 1) + " " + spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().toString());
                }

                ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<ArrayList<IonMatch>>();
                ArrayList<MSnSpectrum> allSpectra = new ArrayList<MSnSpectrum>();
                IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
                AnnotationPreferences annotationPreferences = identificationParameters.getAnnotationPreferences();
                ArrayList<Peptide> peptides = new ArrayList<Peptide>();

                int maxCharge = 1;
                ArrayList<ModificationMatch> allModifications = new ArrayList<ModificationMatch>();

                // iterate the selected psms
                for (String spectrumKey : selectedPsmKeys) {

                    MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

                    if (currentSpectrum != null) {
                        SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                        Peptide peptide = peptideAssumption.getPeptide();
                        SpecificAnnotationPreferences specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                        peptides.add(peptide);
                        ArrayList<IonMatch> annotations = peptideShakerGUI.getSpectrumAnnotator().getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, currentSpectrum, peptide);
                        allAnnotations.add(annotations);
                        allSpectra.add(currentSpectrum);

                        int currentCharge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                        if (currentCharge > maxCharge) {
                            maxCharge = currentCharge;
                        }

                        allModifications.addAll(spectrumMatch.getBestPeptideAssumption().getPeptide().getModificationMatches());

                        currentSpectrumKey = spectrumKey;
                    }
                }

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
                bubbleJPanel.removeAll();
                bubbleJPanel.add(massErrorBubblePlot);
                bubbleJPanel.revalidate();
                bubbleJPanel.repaint();

                if (allSpectra.size() == 2) {
                    for (int i = 0; i < allSpectra.size(); i++) {
                        if (i == 0) {
                            spectrumPanel = new SpectrumPanel(
                                    allSpectra.get(i).getMzValuesAsArray(), allSpectra.get(i).getIntensityValuesNormalizedAsArray(),
                                    500, "2",
                                    "", 40, false, false, false, 2, false);
                            spectrumPanel.setAnnotateHighestPeak(!annotationPreferences.isHighResolutionAnnotation());
                            spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(allAnnotations.get(i)), !annotationPreferences.isHighResolutionAnnotation());

                            SpectrumPanel.setKnownMassDeltas(peptideShakerGUI.getCurrentMassDeltas());
                            spectrumPanel.setDeltaMassWindow(peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences().getFragmentIonAccuracy());
                            spectrumPanel.setBorder(null);
                            spectrumPanel.setDataPointAndLineColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakColor(), 0);
                            spectrumPanel.setPeakWaterMarkColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakColor());
                            spectrumPanel.setPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakWidth());
                            spectrumPanel.setBackgroundPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakWidth());

                            spectrumPanel.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                            spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(annotationPreferences.yAxisZoomExcludesBackgroundPeaks());

                            int forwardIon = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getIonSearched1();
                            int rewindIon = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getIonSearched2();

                            spectrumPanel.addAutomaticDeNovoSequencing(peptides.get(i), allAnnotations.get(i),
                                    forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                                    annotationPreferences.showForwardIonDeNovoTags(),
                                    annotationPreferences.showRewindIonDeNovoTags(), false);
                        } else {
                            spectrumPanel.addMirroredSpectrum(allSpectra.get(i).getMzValuesAsArray(), allSpectra.get(i).getIntensityValuesNormalizedAsArray(),
                                    500, "2", "", false, peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedMirroredPeakColor(),
                                    peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedMirroredPeakColor());
                            spectrumPanel.setAnnotateHighestPeak(!annotationPreferences.isHighResolutionAnnotation());
                            spectrumPanel.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(allAnnotations.get(i)));

                            int forwardIon = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getIonSearched1();
                            int rewindIon = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getIonSearched2();
                            spectrumPanel.addAutomaticDeNovoSequencing(peptides.get(i), allAnnotations.get(i),
                                    forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                                    annotationPreferences.showForwardIonDeNovoTags(),
                                    annotationPreferences.showRewindIonDeNovoTags(), true);
                        }
                    }

                    spectrumPanel.rescale(0.0, spectrumPanel.getMaxXAxisValue());

                    spectrumSplitPane.setDividerLocation(0);
                    spectrumContainerJPanel.revalidate();
                    spectrumContainerJPanel.repaint();

                    spectrumJPanel.removeAll();
                    spectrumJPanel.add(spectrumPanel);
                    spectrumJPanel.revalidate();
                    spectrumJPanel.repaint();
                } else {
                    spectrumSplitPane.setDividerLocation(spectrumSubPlotDividerLocation);
                    spectrumContainerJPanel.revalidate();
                    spectrumContainerJPanel.repaint();
                }

                peptideShakerGUI.updateAnnotationMenus(maxCharge, allModifications);

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Updates the sequence coverage panel. Only recreates the protein plot if
     * necessary.
     *
     * @param proteinKey the key of the selected protein group
     * @param proteinAccession the protein accession
     */
    private void updateSequenceCoverage(String proteinKey, String proteinAccession) {
        updateSequenceCoverage(proteinKey, proteinAccession, false);
    }

    /**
     * Updates the sequence coverage panel.
     *
     * @param proteinAccession the protein accession
     * @param updateProtein if true, force a complete recreation of the plot
     */
    private void updateSequenceCoverage(String proteinKey, String proteinAccession, boolean updateProtein) {

        // @TODO: should be in a separate thread that is possible to cancel if the selection changes
        try {
            // only need to redo this if the protein changes
            if (updateProtein || !proteinAccession.equalsIgnoreCase(currentProteinAccession) || coverage == null) {
                updateProteinSequenceCoveragePanelTitle(proteinAccession);
                updatePtmCoveragePlot(proteinAccession);
            }

            currentProteinAccession = proteinAccession;

            SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
            ArrayList<Integer> selectedPeptideStart = new ArrayList<Integer>();
            int selectionLength = 0;

            if (peptideTable.getSelectedRow() != -1) {

                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) peptideTable.getModel();
                int peptideIndex = tableModel.getViewIndex(peptideTable.getSelectedRow());
                String peptideKey = peptideKeys.get(peptideIndex);
                String peptideSequence = Peptide.getSequence(peptideKey);

                AminoAcidPattern aminoAcidPattern = new AminoAcidPattern(peptideSequence);
                for (int startIndex : aminoAcidPattern.getIndexes(currentProteinSequence, peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences())) {
                    selectedPeptideStart.add(startIndex - 1);
                }
                selectionLength = peptideSequence.length();
            }

            IdentificationFeaturesGenerator identificationFeaturesGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();

            int[] validationCoverage;
            if (coverageShowAllPeptidesJRadioButtonMenuItem.isSelected()) {
                validationCoverage = identificationFeaturesGenerator.getAACoverage(proteinKey);
            } else {
                validationCoverage = identificationFeaturesGenerator.estimateAACoverage(proteinKey, coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem.isSelected());
            }

            double minHeight = 0.2, maxHeight = 1;
            NonSymmetricalNormalDistribution peptideLengthDistribution = peptideShakerGUI.getMetrics().getPeptideLengthDistribution();
            if (peptideLengthDistribution != null) {
                double medianLength = peptideLengthDistribution.getMean();
                MathContext mathContext = new MathContext(10, RoundingMode.HALF_DOWN);
                maxHeight = (1 - minHeight) * peptideLengthDistribution.getProbabilityAt(medianLength, mathContext).doubleValue();
            }

            double[] coverageLikelihood = identificationFeaturesGenerator.getCoverableAA(proteinKey);
            double[] coverageHeight = new double[coverageLikelihood.length];
            for (int i = 0; i < coverageLikelihood.length; i++) {
                double p = coverageLikelihood[i];
                coverageHeight[i] = minHeight + p / maxHeight;
            }

            HashMap<Integer, Color> colors = new HashMap<Integer, Color>();
            colors.put(MatchValidationLevel.confident.getIndex(), peptideShakerGUI.getSparklineColor());
            colors.put(MatchValidationLevel.doubtful.getIndex(), peptideShakerGUI.getUtilitiesUserPreferences().getSparklineColorDoubtful());
            colors.put(MatchValidationLevel.not_validated.getIndex(), peptideShakerGUI.getSparklineColorNonValidated());
            colors.put(MatchValidationLevel.none.getIndex(), peptideShakerGUI.getSparklineColorNotFound());

            int userSelectionIndex = 0;
            while (colors.containsKey(userSelectionIndex)) {
                userSelectionIndex++;
            }
            colors.put(userSelectionIndex, Color.blue); //@TODO: use non hard coded value

            int[] coverageColor = validationCoverage.clone();
            for (int aaStart : selectedPeptideStart) {
                for (int aa = aaStart; aa < aaStart + selectionLength; aa++) {
                    coverageColor[aa] = userSelectionIndex;
                }
            }

            // Dirty fix until the width of the sparkline can change
            int transparentIndex = userSelectionIndex + 1;
            colors.put(userSelectionIndex + 1, new Color(0, 0, 0, 0));
            for (int aa = 0; aa < coverageHeight.length; aa++) {
                if (coverageColor[aa] == MatchValidationLevel.none.getIndex()) {
                    if (coverageLikelihood[aa] < 0.01
                            || !coverageShowPossiblePeptidesJCheckBoxMenuItem.isSelected()) { // NOTE: if the fix is removed, make sure that this line is kept!!!
                        coverageColor[aa] = transparentIndex;
                    }
                }
            }

            // create the coverage plot
            ArrayList<JSparklinesDataSeries> sparkLineDataSeriesCoverage = ProteinSequencePanel.getSparkLineDataSeriesCoverage(coverageHeight, coverageColor, colors);

            HashMap<Integer, ArrayList<ResidueAnnotation>> proteinTooltips = peptideShakerGUI.getDisplayFeaturesGenerator().getResidueAnnotation(
                    proteinKey, peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences(), identificationFeaturesGenerator,
                    peptideShakerGUI.getMetrics(), peptideShakerGUI.getIdentification(), coverageShowAllPeptidesJRadioButtonMenuItem.isSelected(),
                    searchParameters, coverageShowEnzymaticPeptidesOnlyJRadioButtonMenuItem.isSelected());

            // Dirty fix for a bloc-level annotation
            HashMap<Integer, ArrayList<ResidueAnnotation>> blocTooltips = new HashMap<Integer, ArrayList<ResidueAnnotation>>();
            int aaCpt = 0, blocCpt = 0;
            for (JSparklinesDataSeries jSparklinesDataSeries : sparkLineDataSeriesCoverage) {
                double sparkLineLength = jSparklinesDataSeries.getData().get(0);
                ArrayList<ResidueAnnotation> blocAnnotation = new ArrayList<ResidueAnnotation>();
                for (int j = 0; j < sparkLineLength; j++, aaCpt++) {
                    ArrayList<ResidueAnnotation> aaAnnotation = proteinTooltips.get(aaCpt);
                    if (aaAnnotation != null) {
                        for (ResidueAnnotation residueAnnotation : aaAnnotation) {
                            if (!blocAnnotation.contains(residueAnnotation)) {
                                blocAnnotation.add(residueAnnotation);
                            }
                        }
                    }
                }
                blocTooltips.put(blocCpt, blocAnnotation);
                blocCpt++;
            }

            coverageChart = new ProteinSequencePanel(Color.WHITE).getSequencePlot(this, new JSparklinesDataset(sparkLineDataSeriesCoverage), blocTooltips, true, true);

            // make sure that the range is the same for both charts (coverage and ptm)
            coverageChart.getChart().addChangeListener(new ChartChangeListener() {
                @Override
                public void chartChanged(ChartChangeEvent cce) {
                    if (ptmChart != null) {
                        Range range = ((CategoryPlot) coverageChart.getChart().getPlot()).getRangeAxis().getRange();
                        ((CategoryPlot) ptmChart.getChart().getPlot()).getRangeAxis().setRange(range);
                        ptmChart.revalidate();
                        ptmChart.repaint();
                    }
                }
            });

            sequenceCoverageInnerPanel.removeAll();
            sequenceCoverageInnerPanel.add(coverageChart);
            sequenceCoverageInnerPanel.revalidate();
            sequenceCoverageInnerPanel.repaint();

        } catch (ClassCastException e) {
            // ignore   @TODO: this should not happen, but can happen if the table does not update fast enough for the filtering
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
    }

    /**
     * Update the title of the protein sequence coverage panel.
     *
     * @param proteinAccession the accession of the protein of interest
     *
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void updateProteinSequenceCoveragePanelTitle(String proteinAccession) throws IOException, IllegalArgumentException, InterruptedException, FileNotFoundException, ClassNotFoundException, SQLException {

        Protein currentProtein = sequenceFactory.getProtein(proteinAccession);
        currentProteinSequence = currentProtein.getSequence();
        SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();

        if (proteinTable.getSelectedRow() != -1) {

            String proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));

            String title = PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Protein Sequence Coverage (";
            try {
                HashMap<Integer, Double> sequenceCoverage;
                try {
                    sequenceCoverage = peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(
                            proteinKey);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    sequenceCoverage = new HashMap<Integer, Double>();
                }
                Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                Double sequenceCoverageNotValidated = 100 * sequenceCoverage.get(MatchValidationLevel.not_validated.getIndex());
                Double validatedCoverage = sequenceCoverageConfident + sequenceCoverageDoubtful;
                Double totalCoverage = validatedCoverage + sequenceCoverageNotValidated;
                if (validatedCoverage > 0) {
                    title += Util.roundDouble(totalCoverage, 2) + "%"
                            + " - "
                            + Util.roundDouble(sequenceCoverageConfident, 2) + "% confident, "
                            + Util.roundDouble(sequenceCoverageDoubtful, 2) + "% doubtful, "
                            + Util.roundDouble(sequenceCoverageNotValidated, 2) + "% not validated";
                } else {
                    title += Util.roundDouble(sequenceCoverageNotValidated, 2) + "%";
                }
                title += " - ";
                try {
                    double possibleCoverarge = 100.0 * peptideShakerGUI.getIdentificationFeaturesGenerator().getObservableCoverage(proteinKey);
                    title += Util.roundDouble(possibleCoverarge, 2) + "% possible";
                } catch (Exception ePossibleCoverage) {
                    // skip the possible coverage
                    peptideShakerGUI.catchException(ePossibleCoverage);
                }
                title += " - ";
                title += currentProteinSequence.length() + " AA)";
                title += PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING;
            } catch (Exception eCoverage) {
                peptideShakerGUI.catchException(eCoverage);
                // Worst case, we did not even manage to get the coverage.
                title += currentProteinSequence.length() + " AA)";
                title += PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING;
            }

            ((TitledBorder) sequenceCoverageTitledPanel.getBorder()).setTitle(title);
            sequenceCoverageTitledPanel.repaint();
        }
    }

    /**
     * Update the protein coverage PTM plot.
     *
     * @param proteinAccession the protein accession
     */
    private void updatePtmCoveragePlot(String proteinAccession) {

        if (proteinTable.getSelectedRow() != -1) {

            try {
                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
                String proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));

                // get the ptms
                ArrayList<JSparklinesDataSeries> sparkLineDataSeriesPtm = new ArrayList<JSparklinesDataSeries>();
                HashMap<Integer, ArrayList<ResidueAnnotation>> proteinTooltips = new HashMap<Integer, ArrayList<ResidueAnnotation>>();

                // we need to add a first empty filler as the coverage table starts at 0
                ArrayList<Double> data = new ArrayList<Double>();
                data.add(new Double(1));
                JSparklinesDataSeries sparklineDataseriesPtm = new JSparklinesDataSeries(data, new Color(0, 0, 0, 0), null);
                sparkLineDataSeriesPtm.add(sparklineDataseriesPtm);

                PSPtmScores psPtmScores = new PSPtmScores();
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);

                String sequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();
                psPtmScores = (PSPtmScores) proteinMatch.getUrParam(psPtmScores);
                int unmodifiedCounter = 0;

                // get the fixed ptms
                HashMap<Integer, String> fixedPtms = new HashMap<Integer, String>(); // @TODO: note this this only supports one fixed ptm per residue

                Identification identification = peptideShakerGUI.getIdentification();
                identification.loadPeptideMatches(peptideKeys, progressDialog, false);
                DisplayPreferences displayPreferences = peptideShakerGUI.getDisplayPreferences();

                for (String peptideKey : peptideKeys) {
                    boolean modified = false;
                    for (String ptmName : displayPreferences.getDisplayedPtms()) {
                        if (Peptide.isModified(peptideKey, ptmName)) {
                            modified = true;
                            break;
                        }
                    }
                    if (modified) {
                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                        for (ModificationMatch modMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
                            if (!modMatch.isVariable()) {
                                String ptmName = modMatch.getTheoreticPtm();
                                if (displayPreferences.isDisplayedPTM(ptmName)) {
                                    ArrayList<Integer> indexes = sequenceFactory.getProtein(proteinAccession).getPeptideStart(Peptide.getSequence(peptideKey),
                                            peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());
                                    for (Integer index : indexes) {
                                        fixedPtms.put(modMatch.getModificationSite() + index - 2, ptmName);
                                    }
                                }
                            }
                        }
                    }
                }

                for (int aa = 0; aa < sequence.length(); aa++) {

                    String ptmName = fixedPtms.get(aa);
                    for (String variablePTM : psPtmScores.getPtmsAtRepresentativeSite(aa + 1)) {
                        if (displayPreferences.isDisplayedPTM(variablePTM)) {
                            ptmName = variablePTM;
                            break;
                        }
                    }
                    for (String variablePTM : psPtmScores.getConfidentModificationsAt(aa + 1)) {
                        if (displayPreferences.isDisplayedPTM(variablePTM)) {
                            ptmName = variablePTM;
                            break;
                        }
                    }

                    if (ptmName != null) {

                        // add the non-modified area
                        if (unmodifiedCounter > 0) {
                            data = new ArrayList<Double>(1);
                            data.add(new Double(unmodifiedCounter));
                            sparklineDataseriesPtm = new JSparklinesDataSeries(data, new Color(0, 0, 0, 0), null);
                            sparkLineDataSeriesPtm.add(sparklineDataseriesPtm);
                        }

                        // @TODO: what about multiple ptms on the same residue..?
//                        if (psPtmScores.getMainModificationsAt(aa).size() > 1) {
//                            for (int i=0; i<psPtmScores.getMainModificationsAt(aa).size(); i++) {
//                                psPtmScores.getMainModificationsAt(aa).get(i);
//                            }
//                        }
                        // @TODO: are peptide terminal mods excluded??  
                        Color ptmColor = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationProfile().getColor(ptmName);
                        if (ptmColor == null) {
                            ptmColor = Color.lightGray;
                        }

                        ArrayList<ResidueAnnotation> annotations = new ArrayList<ResidueAnnotation>(1);
                        annotations.add(new ResidueAnnotation(ptmName, null, false));
                        proteinTooltips.put(sparkLineDataSeriesPtm.size(), annotations);

                        data = new ArrayList<Double>(1);
                        data.add(new Double(1));
                        sparklineDataseriesPtm = new JSparklinesDataSeries(data, ptmColor, null);
                        sparkLineDataSeriesPtm.add(sparklineDataseriesPtm);

                        // reset the non-modified area counter
                        unmodifiedCounter = 0;

                    } else {
                        unmodifiedCounter++;
                    }
                }

                if (unmodifiedCounter > 0) {
                    // add the remaining non-modified area
                    data = new ArrayList<Double>();
                    data.add(new Double(unmodifiedCounter));
                    sparklineDataseriesPtm = new JSparklinesDataSeries(data, new Color(0, 0, 0, 0), null);
                    sparkLineDataSeriesPtm.add(sparklineDataseriesPtm);
                }

                ptmChart = new ProteinSequencePanel(Color.WHITE).getSequencePlot(this, new JSparklinesDataset(sparkLineDataSeriesPtm), proteinTooltips, false, false);
                sequencePtmsPanel.removeAll();
                sequencePtmsPanel.add(ptmChart);
                sequencePtmsPanel.revalidate();
                sequencePtmsPanel.repaint();
            } catch (ClassCastException e) {
                // ignore   @TODO: this should not happen, but can happen if the table does not update fast enough for the filtering
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Updates the spectrum annotation. Used when the user updates the
     * annotation accuracy.
     */
    public void updateSpectrum() {
        updateSpectrum(psmTable.getSelectedRow(), false);
    }

    /**
     * Update the spectrum to the currently selected PSM.
     *
     * @param row the row index of the PSM
     * @param resetMzRange if true the mz range is reset, if false the current
     * zoom range is kept
     */
    private void updateSpectrum(int row, boolean resetMzRange) {

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
            int psmIndex = tableModel.getViewIndex(row);
            String spectrumKey = psmKeys.get(psmIndex);

            if (displaySpectrum) {

                try {
                    MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

                    if (currentSpectrum != null) {

                        Collection<Peak> peaks = currentSpectrum.getPeakList();

                        if (peaks == null || peaks.isEmpty()) {
                            // do nothing, peaks list not found
                        } else {

                            boolean newMax = false;

                            if (resetMzRange) {
                                lastMzMaximum = 0;
                            }

                            if (peptideShakerGUI.getSelectedPeptideKey() != null) {
                                try {
                                    MSnSpectrum tempSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                                    if (tempSpectrum.getPeakList() != null) {
                                        double newMaximum = tempSpectrum.getMaxMz();
                                        if (lastMzMaximum < newMaximum) {
                                            lastMzMaximum = tempSpectrum.getMaxMz();
                                            newMax = true;
                                        }
                                    }
                                } catch (Exception e) {
                                    peptideShakerGUI.catchException(e);
                                }
                            }

                            double lowerMzZoomRange = 0;
                            double upperMzZoomRange = lastMzMaximum;
                            if (spectrumPanel != null && spectrumPanel.getXAxisZoomRangeLowerValue() != 0 && !newMax) { // @TODO: sometimes the range is reset when is should not be...
                                lowerMzZoomRange = spectrumPanel.getXAxisZoomRangeLowerValue();
                                upperMzZoomRange = spectrumPanel.getXAxisZoomRangeUpperValue();
                            }

                            // add the data to the spectrum panel
                            Precursor precursor = currentSpectrum.getPrecursor();
                            SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                            spectrumPanel = new SpectrumPanel(
                                    currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                                    precursor.getMz(), spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().toString(),
                                    "", 40, false, false, false, 2, false);
                            spectrumPanel.setKnownMassDeltas(peptideShakerGUI.getCurrentMassDeltas());
                            spectrumPanel.setDeltaMassWindow(peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences().getFragmentIonAccuracy());
                            spectrumPanel.setBorder(null);
                            spectrumPanel.setDataPointAndLineColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakColor(), 0);
                            spectrumPanel.setPeakWaterMarkColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakColor());
                            spectrumPanel.setPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakWidth());
                            spectrumPanel.setBackgroundPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakWidth());

                            // get the spectrum annotations
                            PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                            Peptide currentPeptide = peptideAssumption.getPeptide();
                            PeptideSpectrumAnnotator spectrumAnnotator = peptideShakerGUI.getSpectrumAnnotator();
                            AnnotationPreferences annotationPreferences = peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences();
                            peptideShakerGUI.setSpecificAnnotationPreferences(new SpecificAnnotationPreferences(spectrumKey, peptideAssumption));
                            peptideShakerGUI.updateAnnotationPreferences();
                            SpecificAnnotationPreferences specificAnnotationPreferences = peptideShakerGUI.getSpecificAnnotationPreferences();
                            ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, currentSpectrum, currentPeptide);
                            spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), !annotationPreferences.isHighResolutionAnnotation());
                            spectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);

                            // show all or just the annotated peaks
                            spectrumPanel.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                            spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(annotationPreferences.yAxisZoomExcludesBackgroundPeaks());

                            int forwardIon = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getIonSearched1();
                            int rewindIon = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getIonSearched2();

                            // add de novo sequencing
                            spectrumPanel.addAutomaticDeNovoSequencing(currentPeptide, annotations,
                                    forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                                    annotationPreferences.showForwardIonDeNovoTags(),
                                    annotationPreferences.showRewindIonDeNovoTags(), false);

                            // add the spectrum panel to the frame
                            spectrumJPanel.removeAll();
                            spectrumJPanel.add(spectrumPanel);
                            spectrumJPanel.revalidate();
                            spectrumJPanel.repaint();

                            // create and display the fragment ion table
                            ArrayList<ArrayList<IonMatch>> allAnnotations = getAnnotationsForAllSelectedSpectra();

                            if (!peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences().useIntensityIonTable()) {
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, allAnnotations, specificAnnotationPreferences.getFragmentIonTypes(),
                                        specificAnnotationPreferences.getNeutralLossesMap(),
                                        specificAnnotationPreferences.getSelectedCharges().contains(1),
                                        specificAnnotationPreferences.getSelectedCharges().contains(2)));
                            } else {
                                fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, allAnnotations, getSelectedSpectra(), specificAnnotationPreferences.getFragmentIonTypes(),
                                        specificAnnotationPreferences.getNeutralLossesMap(),
                                        specificAnnotationPreferences.getSelectedCharges().contains(1),
                                        specificAnnotationPreferences.getSelectedCharges().contains(2)));
                            }

                            // create the sequence fragment ion view
                            secondarySpectrumPlotsJPanel.removeAll();
                            SequenceFragmentationPanel sequenceFragmentationPanel = new SequenceFragmentationPanel(
                                    peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(spectrumMatch, false, false, false),
                                    annotations, true, peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationProfile(), forwardIon, rewindIon);
                            sequenceFragmentationPanel.setMinimumSize(new Dimension(sequenceFragmentationPanel.getPreferredSize().width, sequenceFragmentationPanel.getHeight()));
                            sequenceFragmentationPanel.setOpaque(true);
                            sequenceFragmentationPanel.setBackground(Color.WHITE);
                            secondarySpectrumPlotsJPanel.add(sequenceFragmentationPanel);

                            // create the intensity histograms
                            secondarySpectrumPlotsJPanel.add(new IntensityHistogram(
                                    annotations, currentSpectrum,
                                    annotationPreferences.getAnnotationIntensityLimit()));

                            // create the miniature mass error plot
                            MassErrorPlot massErrorPlot = new MassErrorPlot(
                                    annotations, currentSpectrum,
                                    specificAnnotationPreferences.getFragmentIonAccuracy(),
                                    peptideShakerGUI.useRelativeError());

                            if (massErrorPlot.getNumberOfDataPointsInPlot() > 0) {
                                secondarySpectrumPlotsJPanel.add(massErrorPlot);
                            }

                            // update the UI
                            secondarySpectrumPlotsJPanel.revalidate();
                            secondarySpectrumPlotsJPanel.repaint();

                            // update the bubble plot
                            updateBubblePlot();

                            // disable the spectrum tab if more than two psms are selected
                            spectrumJTabbedPane.setEnabledAt(2, psmTable.getSelectedRowCount() <= 2);
                            peptideShakerGUI.enableSpectrumExport(psmTable.getSelectedRowCount() <= 2);

                            // move to the bubble plot tab if more than two psms are selected and the spectrum tab was selected
                            if (psmTable.getSelectedRowCount() > 2 && spectrumJTabbedPane.getSelectedIndex() == 2) {
                                spectrumJTabbedPane.setSelectedIndex(1);
                            }

                            if (psmTable.getSelectedRowCount() > 2) {
                                spectrumJTabbedPane.setToolTipTextAt(2, "Available for single or double spectrum selection only");
                            } else {
                                spectrumJTabbedPane.setToolTipTextAt(2, null);
                            }

                            // update the panel border title
                            updateSpectrumPanelBorderTitle(currentSpectrum);

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
            spectrumJPanel.removeAll();
            spectrumJPanel.revalidate();
            spectrumJPanel.repaint();

            secondarySpectrumPlotsJPanel.removeAll();
            secondarySpectrumPlotsJPanel.revalidate();
            secondarySpectrumPlotsJPanel.repaint();

            fragmentIonsJScrollPane.setViewportView(null);
            fragmentIonsJScrollPane.revalidate();
            fragmentIonsJScrollPane.repaint();

            bubbleJPanel.removeAll();
            bubbleJPanel.revalidate();
            bubbleJPanel.repaint();

            ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum & Fragment Ions"
                    + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
            spectrumMainPanel.repaint();
        }
    }

    /**
     * Update the PSM selection according to the currently selected peptide.
     *
     * @param row the row index of the selected peptide
     * @param forcePsmOrderUpdate if true, the sorted listed is recreated even
     * if not needed
     */
    private void updatePsmSelection(int row, boolean forcePsmOrderUpdate) {

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            try {
                Identification identification = peptideShakerGUI.getIdentification();
                IdentificationFeaturesGenerator identificationFeaturesGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();
                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) peptideTable.getModel();
                int peptideIndex = tableModel.getViewIndex(row);
                String peptideKey = peptideKeys.get(peptideIndex);
                try {
                    psmKeys = identificationFeaturesGenerator.getSortedPsmKeys(peptideKey, peptideShakerGUI.getUtilitiesUserPreferences().getSortPsmsOnRt(), forcePsmOrderUpdate);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    try {
                        // ok let's try without order
                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                        psmKeys = peptideMatch.getSpectrumMatchesKeys();
                    } catch (Exception e1) {
                        // just hope the GUI holds...
                        peptideShakerGUI.catchException(e1);
                        psmKeys = new ArrayList<String>();
                    }
                }

                // clear the selection in case more than one row was selected for the last peptide
                psmTable.clearSelection();

                // update the table model
                if (psmTable.getModel() instanceof PsmTableModel) {
                    ((PsmTableModel) psmTable.getModel()).updateDataModel(peptideShakerGUI, psmKeys);
                    ((PsmTableModel) psmTable.getModel()).setSelfUpdating(true);
                    ((PsmTableModel) psmTable.getModel()).resetSorting(new ProgressDialogX(peptideShakerGUI,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true));
                } else {
                    PsmTableModel psmTableModel = new PsmTableModel(peptideShakerGUI, psmKeys);
                    psmTable.setModel(psmTableModel);
                }

                setPsmTableProperties();
                showSparkLines(peptideShakerGUI.showSparklines());

                int nValidatedPsms = identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideKey);
                int nConfidentPsms = identificationFeaturesGenerator.getNConfidentSpectraForPeptide(peptideKey);
                int nPsms = psmTable.getRowCount();
                String title = PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide Spectrum Matches (";
                if (nConfidentPsms > 0) {
                    title += nValidatedPsms + "/" + nPsms + " - " + nConfidentPsms + " confident, " + (nValidatedPsms - nConfidentPsms) + " doubtful";
                } else {
                    title += nValidatedPsms + "/" + nPsms;
                }
                title += ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING;
                ((TitledBorder) psmsPanel.getBorder()).setTitle(title);
                psmsPanel.repaint();

                updateSelection(true);
                newItemSelection();

                // update the sequence coverage map
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            int proteinIndex, selectedProteinRow = proteinTable.getSelectedRow();
                            if (selectedProteinRow != -1) {
                                SelfUpdatingTableModel proteinTableModel = (SelfUpdatingTableModel) proteinTable.getModel();
                                proteinIndex = proteinTableModel.getViewIndex(selectedProteinRow);
                            } else {
                                // Let's assume it is the first
                                proteinIndex = 0;
                            }
                            String proteinKey = proteinKeys.get(proteinIndex);
                            ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);

                            updateSequenceCoverage(proteinKey, proteinMatch.getMainMatch());
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                        }
                    }
                });

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Updates the peptide selection according to the currently selected
     * protein.
     *
     * @param row the row index of the protein
     */
    private void updatePeptideSelection(int proteinIndex) {

        if (proteinIndex != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            try {
                Identification identification = peptideShakerGUI.getIdentification();
                IdentificationFeaturesGenerator identificationFeaturesGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();

                String proteinMatchKey = proteinKeys.get(proteinIndex);
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinMatchKey);
                String accession = proteinMatch.getMainMatch();

                try {
                    peptideKeys = identificationFeaturesGenerator.getSortedPeptideKeys(proteinMatchKey);
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    try {
                        // Let's try without order
                        peptideKeys = proteinMatch.getPeptideMatchesKeys();
                    } catch (Exception e1) {
                        peptideShakerGUI.catchException(e1);
                        // ok you are really unlucky... Just hope the GUI holds...
                        peptideKeys = new ArrayList<String>();
                    }
                }

                // update the table model
                if (peptideTable.getModel() instanceof PeptideTableModel) {
                    ((PeptideTableModel) peptideTable.getModel()).updateDataModel(peptideShakerGUI, accession, peptideKeys);
                    ((PeptideTableModel) peptideTable.getModel()).setSelfUpdating(true);
                    ((PeptideTableModel) peptideTable.getModel()).resetSorting(new ProgressDialogX(peptideShakerGUI,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                            true));
                } else {
                    PeptideTableModel peptideTableModel = new PeptideTableModel(peptideShakerGUI, accession, peptideKeys);
                    peptideTable.setModel(peptideTableModel);
                }

                setPeptideTableProperties();
                showSparkLines(peptideShakerGUI.showSparklines());
                ((DefaultTableModel) peptideTable.getModel()).fireTableDataChanged();

                int maxPeptideSpectra = peptideShakerGUI.getIdentificationFeaturesGenerator().getMaxNSpectra();
                ((JSparklinesArrayListBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxPeptideSpectra);

                String tempSequence = sequenceFactory.getProtein(proteinMatch.getMainMatch()).getSequence();

                peptideTable.getColumn("Start").setCellRenderer(new JSparklinesMultiIntervalChartTableCellRenderer(
                        PlotOrientation.HORIZONTAL, (double) tempSequence.length(),
                        ((double) tempSequence.length()) / 50, peptideShakerGUI.getSparklineColor()));
                ((JSparklinesMultiIntervalChartTableCellRenderer) peptideTable.getColumn("Start").getCellRenderer()).showReferenceLine(true, 0.02, Color.BLACK);
                ((JSparklinesMultiIntervalChartTableCellRenderer) peptideTable.getColumn("Start").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 10);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            updateSelection(true);
                            updatePeptidePanelTitle();
                        } catch (Exception e) {
                            // Exception generally thrown at startup
                            e.printStackTrace();
                        }
                    }
                });

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Displays the results in the tables.
     */
    public void displayResults() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Overview. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {
            @Override
            public void run() {

                try {
                    peptideShakerGUI.getIdentificationFeaturesGenerator().setProteinKeys(peptideShakerGUI.getMetrics().getProteinKeys());
                    try {
                        proteinKeys = peptideShakerGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(progressDialog, peptideShakerGUI.getFilterPreferences());
                    } catch (Exception e) {
                        //Very bad news. Not totally sure how good an error at this point is handled
                        peptideShakerGUI.catchException(e);
                    }

                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setTitle("Preparing Overview. Please Wait..."); // @TODO: not sure why the progress bar seems to be stuck here...

                    // change the peptide shaker icon to a "waiting version" // @TODO: not really sure why we need to set this again here, but seems to be needed
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));

                    peptideShakerGUI.resetSelectedItems();

                    setTableProperties();

                    // update the table model
                    if (proteinTable.getRowCount() > 0) {
                        ((ProteinTableModel) proteinTable.getModel()).updateDataModel(peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getDisplayFeaturesGenerator(), peptideShakerGUI.getExceptionHandler(), proteinKeys);
                    } else {
                        ProteinTableModel proteinTableModel = new ProteinTableModel(peptideShakerGUI.getIdentification(), peptideShakerGUI.getIdentificationFeaturesGenerator(), peptideShakerGUI.getDisplayFeaturesGenerator(), peptideShakerGUI.getExceptionHandler(), proteinKeys);
                        proteinTable.setModel(proteinTableModel);
                    }

                    setTableProperties();
                    showSparkLines(peptideShakerGUI.showSparklines());
                    ((DefaultTableModel) proteinTable.getModel()).fireTableDataChanged();

                    // update spectrum counting column header tooltip
                    if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectralCountingMethod.EMPAI) {
                        proteinTableToolTips.set(proteinTable.getColumn("MS2 Quant.").getModelIndex(), "Protein MS2 Quantification - emPAI");
                    } else if (peptideShakerGUI.getSpectrumCountingPreferences().getSelectedMethod() == SpectralCountingMethod.NSAF) {
                        proteinTableToolTips.set(proteinTable.getColumn("MS2 Quant.").getModelIndex(), "Protein MS2 Quantification - NSAF");
                    } else {
                        proteinTableToolTips.set(proteinTable.getColumn("MS2 Quant.").getModelIndex(), "Protein MS2 Quantification");
                    }

                    if (peptideShakerGUI.getDisplayPreferences().showScores()) {
                        proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Score");
                    } else {
                        proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Confidence");
                    }

                    updateProteinPanelTitle();

                    updateProteinTableCellRenderers();

                    // update the slider tooltips
                    double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getIdentificationParameters().getSearchParameters().getFragmentIonAccuracy();
                    accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " Da");
                    intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");

                    // enable the contextual export options
                    exportProteinsJButton.setEnabled(true);
                    exportPeptidesJButton.setEnabled(true);
                    exportPsmsJButton.setEnabled(true);
                    exportSpectrumJButton.setEnabled(true);
                    exportSequenceCoverageContextJButton.setEnabled(true);

                    peptideShakerGUI.setUpdated(PeptideShakerGUI.OVER_VIEW_TAB_INDEX, true);

                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    progressDialog.setRunFinished();

                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                peptideShakerGUI.checkNewsFeed();
                                peptideShakerGUI.showNotesNotification();
                                peptideShakerGUI.showTipsNotification();
                                proteinTable.requestFocus();
                                updateSelection(true);
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                            }
                        }
                    }, "UpdateSelectionThread").start();

                } catch (Exception e) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    peptideShakerGUI.catchException(e);
                    progressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * Updates the results in the protein table and reselect the desired
     * protein.
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while retrieving a match from the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while retrieving a match from the database
     * @throws java.lang.InterruptedException exception thrown whenever an error
     * occurred while retrieving a match from the database
     */
    public void updateProteinTable() throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        DefaultTableModel dm = (DefaultTableModel) proteinTable.getModel();
        dm.fireTableDataChanged();
        updateSelection(true);
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
     * Returns an arraylist of the spectrum annotations for all the selected
     * PSMs.
     *
     * @return an arraylist of the spectrum annotations
     *
     * @throws MzMLUnmarshallerException exception thrown whenever encountering
     * an error while interacting with an mzML file
     */
    private ArrayList<ArrayList<IonMatch>> getAnnotationsForAllSelectedSpectra() throws MzMLUnmarshallerException {

        ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<ArrayList<IonMatch>>();

        int[] selectedRows = psmTable.getSelectedRows();
        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
        AnnotationPreferences annotationPreferences = identificationParameters.getAnnotationPreferences();

        try {
            for (int row : selectedRows) {

                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
                int psmIndex = tableModel.getViewIndex(row);
                String spectrumKey = psmKeys.get(psmIndex);
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

                if (currentSpectrum != null && peptideTable.getSelectedRow() != -1) {

                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);

                    // get the spectrum annotations
                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                    Peptide peptide = peptideAssumption.getPeptide();
                    SpecificAnnotationPreferences specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, peptideAssumption, identificationParameters.getSequenceMatchingPreferences(), identificationParameters.getPtmScoringPreferences().getSequenceMatchingPreferences());
                    ArrayList<IonMatch> annotations = peptideShakerGUI.getSpectrumAnnotator().getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, currentSpectrum, peptide);
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

        SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
        for (int row : selectedRows) {
            int psmIndex = tableModel.getViewIndex(row);
            String spectrumKey = psmKeys.get(psmIndex);
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
            return (Component) spectrumJPanel.getComponent(0);
        }

        return null;
    }

    /**
     * Returns the extended spectrum panel.
     *
     * @return the extended spectrum panel, or null if the spectrum tab is not
     * enabled
     */
    public Component getSpectrumAndPlots() {

        if (spectrumJTabbedPane.isEnabledAt(2)) {
            spectrumJTabbedPane.setSelectedIndex(2);
            return spectrumSplitPane;
        }

        return null;
    }

    /**
     * Returns the sequence fragmentation plot panel.
     *
     * @return the sequence fragmentation plot panel, or null if the spectrum
     * tab is not enabled
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
     * @return the intensity histogram plot panel, or null if the spectrum tab
     * is not enabled
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
     * @return the mass error plot panel, or null if the spectrum tab is not
     * enabled or the the mass error plot is not showing
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

            StringBuilder spectraAsMgf = new StringBuilder();

            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
            for (int row : selectedRows) {
                int psmIndex = tableModel.getViewIndex(row);
                String spectrumKey = psmKeys.get(psmIndex);
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                spectraAsMgf.append(currentSpectrum.asMgf());
            }

            return spectraAsMgf.toString();
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

        if (showSeparators) {
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(dividerSize);
            formComponentResized(null);
        } else {
            peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(0);
            formComponentResized(null);
        }
    }

    /**
     * Hides or displays the score columns in the protein and peptide tables.
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while retrieving a match from the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while reading a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while retrieving a match from the database
     * @throws java.lang.InterruptedException exception thrown whenever an error
     * occurred while retrieving a match from the database
     */
    public void updateScores() throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ((ProteinTableModel) proteinTable.getModel()).showScores(peptideShakerGUI.getDisplayPreferences().showScores());
        ((DefaultTableModel) proteinTable.getModel()).fireTableStructureChanged();
        ((DefaultTableModel) peptideTable.getModel()).fireTableStructureChanged();
        ((DefaultTableModel) psmTable.getModel()).fireTableStructureChanged();
        setTableProperties();

        if (peptideShakerGUI.getSelectedTab() == PeptideShakerGUI.OVER_VIEW_TAB_INDEX) {
            this.updateSelection(false);
        }

        if (peptideShakerGUI.getDisplayPreferences().showScores()) {
            proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Score");
            peptideTableToolTips.set(peptideTable.getColumnCount() - 2, "Peptide Score");
            psmTableToolTips.set(psmTable.getColumnCount() - 2, "PSM Score");
        } else {
            proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Confidence");
            peptideTableToolTips.set(peptideTable.getColumnCount() - 2, "Peptide Confidence");
            psmTableToolTips.set(psmTable.getColumnCount() - 2, "PSM Confidence");
        }

        updateProteinTableCellRenderers();
    }

    /**
     * Returns the current selected tab in the spectrum and fragment ions tabbed
     * pane.
     *
     * @return the current selected tab in the spectrum and fragment ions tabbed
     * pane
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
     * Update the PTM color coding.
     */
    public void updatePtmColors() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        try {
            // update the peptide table
            ((DefaultTableModel) peptideTable.getModel()).fireTableDataChanged();

            // update the peptide table
            ((DefaultTableModel) psmTable.getModel()).fireTableDataChanged();

            // update the sequence coverage map
            if (proteinTable.getSelectedRow() != -1) {
                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
                String proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                updateSequenceCoverage(proteinKey, proteinMatch.getMainMatch(), true);
            }

            // reset the row selections
            updateSelection(false);

        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the PSM order in the PSM table.
     */
    public void updatePsmOrder() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        try {
            updatePsmSelection(peptideTable.getSelectedRow(), true);

            // update the peptide table
            ((DefaultTableModel) psmTable.getModel()).fireTableDataChanged();

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif"))); // @TODO: not really sure why this is needed..?
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                }
            });

        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update the spectrum and fragment ions panel border title with information
     * about the currently selected PSM.
     *
     * @param currentPeptide the current peptide
     * @param currentSpectrum the current spectrum
     */
    private void updateSpectrumPanelBorderTitle(MSnSpectrum currentSpectrum) throws IOException {
        if (peptideTable.getSelectedRow() != -1
                && proteinTable.getSelectedRow() != -1
                && psmTable.getSelectedRow() != -1) {
            try {
                SelfUpdatingTableModel peptideTableModel = (SelfUpdatingTableModel) peptideTable.getModel();
                int peptideIndex = peptideTableModel.getViewIndex(peptideTable.getSelectedRow());
                String peptideKey = peptideKeys.get(peptideIndex);
                SelfUpdatingTableModel proteinTableModel = (SelfUpdatingTableModel) proteinTable.getModel();
                String proteinKey = proteinKeys.get(proteinTableModel.getViewIndex(proteinTable.getSelectedRow()));
                ProteinMatch proteinMatch = peptideShakerGUI.getIdentification().getProteinMatch(proteinKey);
                Protein currentProtein = sequenceFactory.getProtein(proteinMatch.getMainMatch());
                HashMap<Integer, String[]> aaSurrounding = currentProtein.getSurroundingAA(Peptide.getSequence(peptideKey),
                        peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides(), peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());

                String before = "", after = "";

                if (aaSurrounding.size() == 1) {
                    for (int index : aaSurrounding.keySet()) {
                        before = aaSurrounding.get(index)[0];
                        after = aaSurrounding.get(index)[1];
                    }
                } else {
                    boolean first = true;
                    ArrayList<Integer> indexes = new ArrayList<Integer>(aaSurrounding.keySet());
                    Collections.sort(indexes);
                    for (int index : indexes) {
                        if (first) {
                            first = false;
                        } else {
                            before += "|";
                            after += "|";
                        }
                        before += aaSurrounding.get(index)[0];
                        after += aaSurrounding.get(index)[1];
                    }
                }

                SelfUpdatingTableModel psmTableModel = (SelfUpdatingTableModel) psmTable.getModel();
                int psmIndex = psmTableModel.getViewIndex(psmTable.getSelectedRow());
                String spectrumKey = psmKeys.get(psmIndex);

                try {
                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (!before.equals("")) {
                        before += " - ";
                    }
                    if (!after.equals("")) {
                        after = " - " + after;
                    }

                    if (psmTable.getSelectedRowCount() == 1) {
                        String modifiedSequence = peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(spectrumMatch, false, false, true);
                        ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                                PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                                + "Spectrum & Fragment Ions (" + before + modifiedSequence + after
                                + "   " + peptideAssumption.getIdentificationCharge().toString() + "   "
                                + Util.roundDouble(currentSpectrum.getPrecursor().getMz(), 2) + " m/z)"
                                + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                    } else if (psmTable.getSelectedRowCount() == 2) {

                        int[] selectedRows = psmTable.getSelectedRows();

                        psmIndex = psmTableModel.getViewIndex(selectedRows[0]);
                        spectrumKey = psmKeys.get(psmIndex);
                        SpectrumMatch firstSpectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                        String firstModifiedSequence = peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(firstSpectrumMatch, false, false, true);

                        psmIndex = psmTableModel.getViewIndex(selectedRows[1]);
                        spectrumKey = psmKeys.get(psmIndex);
                        SpectrumMatch secondSpectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                        String secondModifiedSequence = peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(secondSpectrumMatch, false, false, true);

                        ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                                PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                                + "Spectrum & Fragment Ions (" + firstModifiedSequence + " vs. " + secondModifiedSequence + ")"
                                + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                    } else {
                        PeptideMatch currentPeptideMatch = peptideShakerGUI.getIdentification().getPeptideMatch(peptideKey);
                        String peptideSequence = peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(currentPeptideMatch, false, false, true);

                        ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                                PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                                + "Spectrum & Fragment Ions (" + peptideSequence + " " + psmTable.getSelectedRowCount() + " PSMs)"
                                + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                    }

                    spectrumMainPanel.repaint();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                            PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                            + "Spectrum & Fragment Ions"
                            + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                    spectrumMainPanel.repaint();
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                        PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                        + "Spectrum & Fragment Ions"
                        + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                spectrumMainPanel.repaint();
            }
        } else {
            ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(
                    PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                    + "Spectrum & Fragment Ions"
                    + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
            spectrumMainPanel.repaint();
        }
    }

    /**
     * Update the protein inference type for the currently selected peptide.
     *
     * @param proteinInferenceType the protein inference type
     */
    public void updatePeptideProteinInference(int proteinInferenceType) {
        peptideTable.setValueAt(proteinInferenceType, peptideTable.getSelectedRow(), peptideTable.getColumn("PI").getModelIndex());
    }

    /**
     * Export the table contents to the clipboard.
     *
     * @param index the index type
     */
    private void copyTableContentToClipboardOrFile(TableIndex index) {

        final TableIndex tableIndex = index;

        if (tableIndex == TableIndex.PROTEIN_TABLE
                || tableIndex == TableIndex.PEPTIDE_TABLE
                || tableIndex == TableIndex.PSM_TABLE) {

            OutputGenerator outputGenerator = new OutputGenerator(peptideShakerGUI);

            if (tableIndex == TableIndex.PROTEIN_TABLE) {
                ArrayList<String> selectedProteins = getDisplayedProteins();
                outputGenerator.getProteinsOutput(
                        null, selectedProteins, true, false, true, true, true,
                        true, true, true, true, false, true,
                        true, true, true, true, true, false, true, false, false, false, true, true);
            } else if (tableIndex == TableIndex.PEPTIDE_TABLE) {
                ArrayList<String> selectedPeptides = getDisplayedPeptides();
                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
                String proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));
                outputGenerator.getPeptidesOutput(
                        null, selectedPeptides, null, true, false, true, true, true, true, true,
                        true, true, true, true, true, true, true, true, false, false, false, proteinKey, true);
            } else if (tableIndex == TableIndex.PSM_TABLE) {
                ArrayList<String> selectedPsms = getDisplayedPsms();
                outputGenerator.getPSMsOutput(
                        null, selectedPsms, true, false, true, true, true, true,
                        true, true, true, true, true, true, true, false, false);
            }
        }
    }

    /**
     * Updates the visibility of the show panels buttons at the bottom of the
     * screen.
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
     * Reselect the protein, peptide and PSM.
     */
    private void reselect() {

        String proteinKey = peptideShakerGUI.getSelectedProteinKey();
        String peptideKey = peptideShakerGUI.getSelectedPeptideKey();
        String psmKey = peptideShakerGUI.getSelectedPsmKey();

        if (!proteinKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            int proteinRow = getProteinRow(proteinKey);
            proteinTable.setRowSelectionInterval(proteinRow, proteinRow);
        }

        if (!peptideKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            int peptideRow = getPeptideRow(peptideKey);
            peptideTable.setRowSelectionInterval(peptideRow, peptideRow);
        }

        if (!psmKey.equals(PeptideShakerGUI.NO_SELECTION)) {
            int psmRow = getPsmRow(psmKey);
            if (psmRow < psmTable.getRowCount()) {
                psmTable.setRowSelectionInterval(psmRow, psmRow);
            }
        }
    }

    /**
     * Update the selected protein and peptide.
     *
     * @param scrollToVisible if true the table also scrolls to make the
     * selected row visible
     */
    public void updateSelection(boolean scrollToVisible) {

        try {
            int proteinRow = 0;
            String proteinKey = peptideShakerGUI.getSelectedProteinKey();
            String peptideKey = peptideShakerGUI.getSelectedPeptideKey();
            String psmKey = peptideShakerGUI.getSelectedPsmKey();

            if (proteinKey.equals(PeptideShakerGUI.NO_SELECTION)
                    && peptideKey.equals(PeptideShakerGUI.NO_SELECTION)
                    && !psmKey.equals(PeptideShakerGUI.NO_SELECTION)) {
                if (peptideShakerGUI.getIdentification().matchExists(psmKey)) {
                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(psmKey);
                    if (spectrumMatch.getBestPeptideAssumption() != null) {
                        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                        peptideKey = peptide.getMatchingKey(peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());
                    }
                } else {
                    peptideShakerGUI.resetSelectedItems();
                }
            }

            if (proteinKey.equals(PeptideShakerGUI.NO_SELECTION) && !peptideKey.equals(PeptideShakerGUI.NO_SELECTION)) {

                ProteinMatchesIterator proteinMatchesIterator = peptideShakerGUI.getIdentification().getProteinMatchesIterator(null, false, null, false, null, null); // @TODO: add waiting handler?
                //proteinMatchesIterator.setBatchSize(20); // @TODO: add?

                while (proteinMatchesIterator.hasNext()) {
                    try {
                        ProteinMatch proteinMatch = proteinMatchesIterator.next();
                        if (proteinMatch.getPeptideMatchesKeys().contains(peptideKey)) {
                            proteinKey = proteinMatch.getKey();
                            peptideShakerGUI.setSelectedItems(proteinKey, peptideKey, psmKey);
                            break;
                        }
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                        return;
                    }
                }
            }

            if (!proteinKey.equals(PeptideShakerGUI.NO_SELECTION)) {
                proteinRow = getProteinRow(proteinKey);
            }

            if (proteinKeys.isEmpty()) {
                // For the silly people like me who happen to hide all proteins
                clearData();
                return;
            }

            if (proteinRow == -1) {
                peptideShakerGUI.resetSelectedItems();
                proteinTableMouseReleased(null);
            } else if (proteinTable.getSelectedRow() != proteinRow) {
                proteinTable.setRowSelectionInterval(proteinRow, proteinRow);
                if (scrollToVisible) {
                    proteinTable.scrollRectToVisible(proteinTable.getCellRect(proteinRow, 0, false));
                }
                proteinTableMouseReleased(null);
            }

            int peptideRow = 0;
            if (!peptideKey.equals(PeptideShakerGUI.NO_SELECTION)) {
                peptideRow = getPeptideRow(peptideKey);
            }

            if (peptideTable.getSelectedRow() != peptideRow && peptideRow != -1) {
                peptideTable.setRowSelectionInterval(peptideRow, peptideRow);
                if (scrollToVisible) {
                    peptideTable.scrollRectToVisible(peptideTable.getCellRect(peptideRow, 0, false));
                }
                peptideTableMouseReleased(null);
            }

            int psmRow = 0;
            if (!psmKey.equals(PeptideShakerGUI.NO_SELECTION)) {
                psmRow = getPsmRow(psmKey);
            }

            if (psmTable.getSelectedRow() != psmRow && psmRow != -1 && psmRow < psmTable.getRowCount()) {
                psmTable.setRowSelectionInterval(psmRow, psmRow);
                if (scrollToVisible) {
                    psmTable.scrollRectToVisible(psmTable.getCellRect(psmRow, 0, false));
                }
                psmTableMouseReleased(null);
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Updates and displays the current spectrum slider tooltip.
     */
    private void updateSpectrumSliderToolTip() {
        double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getIdentificationParameters().getSearchParameters().getFragmentIonAccuracy();

        spectrumJTabbedPane.setToolTipText("<html>Accuracy: " + Util.roundDouble(accuracy, 2) + " Da<br>"
                + "Level: " + intensitySlider.getValue() + "%</html>");

        // show the tooltip now
        ToolTipManager.sharedInstance().mouseMoved(
                new MouseEvent(spectrumJTabbedPane, 0, 0, 0,
                        spectrumJTabbedPane.getX() + spectrumJTabbedPane.getWidth() - 10, spectrumJTabbedPane.getY() + 90, // X-Y of the mouse for the tool tip
                        0, false));
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected protein,
     * peptide and PSM.
     */
    public void newItemSelection() {
        String proteinKey = PeptideShakerGUI.NO_SELECTION;
        String peptideKey = PeptideShakerGUI.NO_SELECTION;
        String psmKey = PeptideShakerGUI.NO_SELECTION;

        if (proteinTable.getSelectedRow() != -1) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            proteinKey = proteinKeys.get(tableModel.getViewIndex(proteinTable.getSelectedRow()));
        }
        if (peptideTable.getSelectedRow() != -1) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) peptideTable.getModel();
            peptideKey = peptideKeys.get(tableModel.getViewIndex(peptideTable.getSelectedRow()));
        }
        if (psmTable.getSelectedRow() != -1) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
            psmKey = psmKeys.get(tableModel.getViewIndex(psmTable.getSelectedRow()));
        }

        peptideShakerGUI.setSelectedItems(proteinKey, peptideKey, psmKey);
    }

    /**
     * Returns the row of a desired protein.
     *
     * @param proteinKey the key of the protein
     * @return the row of the desired protein
     */
    private int getProteinRow(String proteinKey) {
        int modelIndex = proteinKeys.indexOf(proteinKey);
        if (modelIndex >= 0) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            return tableModel.getRowNumber(modelIndex);
        } else {
            return -1;
        }
    }

    /**
     * Returns the row of a desired peptide.
     *
     * @param peptideKey the key of the peptide
     * @return the row of the desired peptide
     */
    private int getPeptideRow(String peptideKey) {
        int modelIndex = peptideKeys.indexOf(peptideKey);
        if (modelIndex >= 0) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) peptideTable.getModel();
            return tableModel.getRowNumber(modelIndex);
        } else {
            return -1;
        }
    }

    /**
     * Returns the row of a desired psm.
     *
     * @param psmKey the key of the psm
     * @return the row of the desired psm
     */
    private int getPsmRow(String psmKey) {
        int modelIndex = psmKeys.indexOf(psmKey);
        if (modelIndex >= 0) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
            return tableModel.getRowNumber(modelIndex);
        } else {
            return -1;
        }
    }

    /**
     * Clear all the data.
     */
    public void clearData() {

        displaySpectrum = true;
        displayCoverage = true;
        displayProteins = true;
        displayPeptidesAndPSMs = true;

        proteinKeys.clear();
        peptideKeys.clear();
        psmKeys.clear();

        DefaultTableModel psmTableModel = (DefaultTableModel) psmTable.getModel();
        psmTableModel.getDataVector().removeAllElements();
        DefaultTableModel peptideTableModel = (DefaultTableModel) peptideTable.getModel();
        peptideTableModel.getDataVector().removeAllElements();
        ProteinTableModel proteinTableModel = (ProteinTableModel) proteinTable.getModel();
        proteinTableModel.reset();

        psmTableModel.fireTableDataChanged();
        peptideTableModel.fireTableDataChanged();
        proteinTableModel.fireTableDataChanged();

        currentSpectrumKey = "";
        currentProteinSequence = "";
        spectrumPanel = null;

        sequenceCoverageInnerPanel.removeAll();
        sequencePtmsPanel.removeAll();

        fragmentIonsJScrollPane.setViewportView(null);
        bubbleJPanel.removeAll();
        spectrumJPanel.removeAll();
        secondarySpectrumPlotsJPanel.removeAll();

        ((TitledBorder) proteinsLayeredPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Proteins" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        proteinsLayeredPanel.repaint();
        ((TitledBorder) peptidesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptides" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        peptidesPanel.repaint();
        ((TitledBorder) psmsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide Spectrum Matches" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        psmsPanel.repaint();
        ((TitledBorder) spectrumMainPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Spectrum & Fragment Ions" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        spectrumMainPanel.repaint();
        ((TitledBorder) sequenceCoverageTitledPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Protein Sequence Coverage" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        sequenceCoverageTitledPanel.repaint();
    }

    @Override
    public void annotationClicked(ArrayList<ResidueAnnotation> allAnnotation, ChartMouseEvent cme) {

        try {
            final Range oldRange = ((CategoryPlot) coverageChart.getChart().getPlot()).getRangeAxis().getRange();

            if (allAnnotation.size() == 1 && allAnnotation.get(0).isClickable()) {

                // select the peptide
                peptideShakerGUI.setSelectedItems(peptideShakerGUI.getSelectedProteinKey(), allAnnotation.get(0).getIdentifier(), PeptideShakerGUI.NO_SELECTION);
                updateSelection(true);

                // update the psms
                peptideShakerGUI.setSelectedItems(peptideShakerGUI.getSelectedProteinKey(), allAnnotation.get(0).getIdentifier(), PeptideShakerGUI.NO_SELECTION);

                // update the psm selection
                updatePsmSelection(peptideTable.getSelectedRow(), false);

                // new peptide, reset spectrum boundaries
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
                            updateSpectrum(tableModel.getViewIndex(psmTable.getSelectedRow()), true);
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                        }
                    }
                });

                // reset the range
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            ((CategoryPlot) coverageChart.getChart().getPlot()).getRangeAxis().setRange(oldRange);
                            coverageChart.revalidate();
                            coverageChart.repaint();
                        } catch (Exception e) {
                            peptideShakerGUI.catchException(e);
                        }
                    }
                });

            } else {
                // show popup menu
                JPopupMenu peptidesPopupMenu = new JPopupMenu();

                for (ResidueAnnotation currentAnnotation : allAnnotation) {
                    if (currentAnnotation.isClickable()) {
                        String text = "<html>" + (peptidesPopupMenu.getComponentCount() + 1) + ": " + currentAnnotation.getAnnotation() + "</html>";
                        final String peptideKey = currentAnnotation.getIdentifier();
                        JMenuItem menuItem = new JMenuItem(text);
                        menuItem.addActionListener(new java.awt.event.ActionListener() {
                            public void actionPerformed(java.awt.event.ActionEvent evt) {

                                try {
                                    // select the peptide
                                    peptideShakerGUI.setSelectedItems(peptideShakerGUI.getSelectedProteinKey(), peptideKey, PeptideShakerGUI.NO_SELECTION);
                                    updateSelection(true);

                                    // update the psms
                                    peptideShakerGUI.setSelectedItems(peptideShakerGUI.getSelectedProteinKey(), peptideKey, PeptideShakerGUI.NO_SELECTION);

                                    // update the psm selection
                                    updatePsmSelection(peptideTable.getSelectedRow(), false);

                                    // new peptide, reset spectrum boundaries
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) psmTable.getModel();
                                                updateSpectrum(tableModel.getViewIndex(psmTable.getSelectedRow()), true);
                                            } catch (Exception e) {
                                                peptideShakerGUI.catchException(e);
                                            }
                                        }
                                    });

                                    // reset the range
                                    // invoke later to give time for components to update
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            try {
                                                ((CategoryPlot) coverageChart.getChart().getPlot()).getRangeAxis().setRange(oldRange);
                                                coverageChart.revalidate();
                                                coverageChart.repaint();
                                            } catch (Exception e) {
                                                peptideShakerGUI.catchException(e);
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    peptideShakerGUI.catchException(e);
                                }
                            }
                        });
                        peptidesPopupMenu.add(menuItem);
                    }
                }

                peptidesPopupMenu.show(cme.getTrigger().getComponent(), cme.getTrigger().getX(), cme.getTrigger().getY());
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Update the number of surrounding amino acids displayed.
     */
    public void updateSurroundingAminoAcids() {
        if (!peptideShakerGUI.getSelectedPsmKey().equals(PeptideShakerGUI.NO_SELECTION)) {
            try {
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(peptideShakerGUI.getSelectedPsmKey());
                updateSpectrumPanelBorderTitle(currentSpectrum);
            } catch (IOException e) {
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Update the protein table cell renderers.
     */
    private void updateProteinTableCellRenderers() {

        if (peptideShakerGUI.getIdentification() != null) {

            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxNPeptides());
            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxNSpectra());
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxSpectrumCounting());
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).setMaxValue(peptideShakerGUI.getMetrics().getMaxMW());

            try {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).setMaxValue(100.0);
            } catch (IllegalArgumentException e) {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
            }
        }
    }

    /**
     * Deactivates the self updating tables.
     */
    public void deactivateSelfUpdatingTableModels() {
        if (proteinTable.getModel() instanceof SelfUpdatingTableModel) {
            ((SelfUpdatingTableModel) proteinTable.getModel()).setSelfUpdating(false);
        }
        if (peptideTable.getModel() instanceof SelfUpdatingTableModel) {
            ((SelfUpdatingTableModel) peptideTable.getModel()).setSelfUpdating(false);
        }
        if (psmTable.getModel() instanceof SelfUpdatingTableModel) {
            ((SelfUpdatingTableModel) psmTable.getModel()).setSelfUpdating(false);
        }
    }
}
