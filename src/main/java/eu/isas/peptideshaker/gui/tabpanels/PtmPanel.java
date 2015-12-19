package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.protein.SequenceModificationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.parameters.PSPtmScores;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.export.OutputGenerator;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import eu.isas.peptideshaker.gui.protein_inference.ProteinInferencePeptideLevelDialog;
import eu.isas.peptideshaker.gui.PtmSiteInferenceDialog;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntervalChartTableCellRenderer;
import no.uib.jsparklines.renderers.util.GradientColorCoding.ColorGradient;
import org.jfree.chart.plot.PlotOrientation;

/**
 * The PTM tab.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PtmPanel extends javax.swing.JPanel {

    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;

    /**
     * Indexes for the data tables.
     */
    private enum TableIndex {

        MODIFIED_PEPTIDES_TABLE, RELATED_PEPTIDES_TABLE, MODIFIED_PSMS_TABLE, RELATED_PSMS_TABLE, PTM_TABLE, A_SCORES_TABLE, DELTA_SCORES_TABLE
    };
    /**
     * The currently selected row in the PTM table.
     */
    private int currentPtmRow = -1;
    /**
     * The selected peptides table column header tooltips.
     */
    private ArrayList<String> selectedPeptidesTableToolTips;
    /**
     * The related peptides table column header tooltips.
     */
    private ArrayList<String> relatedPeptidesTableToolTips;
    /**
     * The selected PSMs table column header tooltips.
     */
    private ArrayList<String> selectedPsmsTableToolTips;
    /**
     * The related PSMs table column header tooltips.
     */
    private ArrayList<String> relatedPsmsTableToolTips;
    /**
     * PTM table column header tooltips.
     */
    private ArrayList<String> ptmTableToolTips;
    /**
     * The main GUI.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * Map of all peptide keys indexed by their modification status.
     */
    private HashMap<String, ArrayList<String>> peptideMap = new HashMap<String, ArrayList<String>>();
    /**
     * The modification name for no modification.
     */
    public final static String NO_MODIFICATION = "no modification";
    /**
     * The displayed identification.
     */
    private Identification identification;
    /**
     * The keys of the peptides currently displayed.
     */
    private ArrayList<String> displayedPeptides = new ArrayList<String>();
    /**
     * The keys of the related peptides currently displayed.
     */
    private ArrayList<String> relatedPeptides = new ArrayList<String>();
    /**
     * Boolean indicating whether the related peptide is selected.
     */
    private boolean relatedSelected = false;
    /**
     * The current spectrum panel for the selected PSM.
     */
    private SpectrumPanel spectrum;
    /**
     * Protein inference map, key: pi type, element: pi as a string.
     */
    private HashMap<Integer, String> proteinInferenceTooltipMap;
    /**
     * PTM confidence tooltip map, key: PTM confidence type, element: PTM
     * confidence as a string.
     */
    private HashMap<Integer, String> ptmConfidenceTooltipMap;
    /**
     * The PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * Creates a new PTM tab.
     *
     * @param peptideShakerGUI the PeptideShaker parent frame
     */
    public PtmPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();

        // add Delta and A score gradient color panels
        addGradientScoreColors();

        peptidesTable.setAutoCreateRowSorter(true);
        relatedPeptidesTable.setAutoCreateRowSorter(true);
        selectedPsmsTable.setAutoCreateRowSorter(true);
        relatedPsmsTable.setAutoCreateRowSorter(true);

        relatedPeptidesTableJScrollPane.getViewport().setOpaque(false);
        psmsModifiedTableJScrollPane.getViewport().setOpaque(false);
        psmsRelatedTableJScrollPane.getViewport().setOpaque(false);
        peptidesTableJScrollPane.getViewport().setOpaque(false);
        ptmJScrollPane.getViewport().setOpaque(false);
        psmAScoresScrollPane.getViewport().setOpaque(false);
        psmDeltaScrollPane.getViewport().setOpaque(false);

        setTableProperties();

        // make the tabs in the spectrum tabbed pane go from right to left
        spectrumTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        updateSeparators();
        formComponentResized(null);
    }

    /**
     * Adds the gradient score colors for the A and Delta score tables.
     */
    private void addGradientScoreColors() {

        final Color startColor = Color.WHITE;
        final Color endColor = Color.BLUE;

        JPanel deltaScoreGradientJPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                Graphics2D g2d = (Graphics2D) grphcs;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(
                        0, ((float) getHeight()) / 2,
                        startColor,
                        getWidth(), ((float) getHeight()) / 2,
                        endColor);

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(grphcs);
            }
        };

        deltaScoreGradientJPanel.setOpaque(false);
        deltaScoreGradientPanel.add(deltaScoreGradientJPanel);

        JPanel aScoreGradientJPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics grphcs) {
                Graphics2D g2d = (Graphics2D) grphcs;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(
                        0, ((float) getHeight()) / 2,
                        startColor,
                        getWidth(), ((float) getHeight()) / 2,
                        endColor);

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(grphcs);
            }
        };

        aScoreGradientJPanel.setOpaque(false);
        aScoreGradientPanel.add(aScoreGradientJPanel);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        // correct the color for the upper right corner
        JPanel ptmCorner = new JPanel();
        ptmCorner.setBackground(ptmJTable.getTableHeader().getBackground());
        ptmJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, ptmCorner);
        JPanel modifiedPeptidesCorner = new JPanel();
        modifiedPeptidesCorner.setBackground(peptidesTable.getTableHeader().getBackground());
        peptidesTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, modifiedPeptidesCorner);
        JPanel relatedPeptidesCorner = new JPanel();
        relatedPeptidesCorner.setBackground(relatedPeptidesTable.getTableHeader().getBackground());
        relatedPeptidesTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, relatedPeptidesCorner);
        JPanel psmMatchesCorner = new JPanel();
        psmMatchesCorner.setBackground(selectedPsmsTable.getTableHeader().getBackground());
        psmsModifiedTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, psmMatchesCorner);
        JPanel psmRelatedMatchesCorner = new JPanel();
        psmRelatedMatchesCorner.setBackground(relatedPsmsTable.getTableHeader().getBackground());
        psmsRelatedTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, psmRelatedMatchesCorner);
        JPanel aScoreTableCorner = new JPanel();
        aScoreTableCorner.setBackground(relatedPsmsTable.getTableHeader().getBackground());
        psmAScoresScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, aScoreTableCorner);
        JPanel deltaScoreTableCorner = new JPanel();
        deltaScoreTableCorner.setBackground(relatedPsmsTable.getTableHeader().getBackground());
        psmDeltaScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, deltaScoreTableCorner);

        ptmJTable.getColumn("  ").setMaxWidth(35);
        ptmJTable.getColumn("  ").setMinWidth(35);

        peptidesTable.getColumn("   ").setMaxWidth(30);
        peptidesTable.getColumn("   ").setMinWidth(30);
        peptidesTable.getColumn(" ").setMaxWidth(45);
        peptidesTable.getColumn(" ").setMinWidth(45);
        peptidesTable.getColumn("PTM").setMaxWidth(45);
        peptidesTable.getColumn("PTM").setMinWidth(45);
        peptidesTable.getColumn("  ").setMaxWidth(30);
        peptidesTable.getColumn("  ").setMinWidth(30);
        peptidesTable.getColumn("PI").setMaxWidth(37);
        peptidesTable.getColumn("PI").setMinWidth(37);
        peptidesTable.getColumn("Peptide").setMaxWidth(80);
        peptidesTable.getColumn("Peptide").setMinWidth(80);

        relatedPeptidesTable.getColumn("   ").setMaxWidth(30);
        relatedPeptidesTable.getColumn("   ").setMinWidth(30);
        relatedPeptidesTable.getColumn(" ").setMaxWidth(45);
        relatedPeptidesTable.getColumn(" ").setMinWidth(45);
        relatedPeptidesTable.getColumn("PI").setMaxWidth(37);
        relatedPeptidesTable.getColumn("PI").setMinWidth(37);
        relatedPeptidesTable.getColumn("  ").setMaxWidth(30);
        relatedPeptidesTable.getColumn("  ").setMinWidth(30);
        relatedPeptidesTable.getColumn("PTM").setMaxWidth(45);
        relatedPeptidesTable.getColumn("PTM").setMinWidth(45);
        relatedPeptidesTable.getColumn("Peptide").setMaxWidth(80);
        relatedPeptidesTable.getColumn("Peptide").setMinWidth(80);

        selectedPsmsTable.getColumn("   ").setMaxWidth(30);
        selectedPsmsTable.getColumn("   ").setMinWidth(30);
        selectedPsmsTable.getColumn(" ").setMaxWidth(45);
        selectedPsmsTable.getColumn(" ").setMinWidth(45);
        selectedPsmsTable.getColumn("PTM").setMaxWidth(45);
        selectedPsmsTable.getColumn("PTM").setMinWidth(45);
        selectedPsmsTable.getColumn("Charge").setMaxWidth(90);
        selectedPsmsTable.getColumn("Charge").setMinWidth(90);
        selectedPsmsTable.getColumn("  ").setMaxWidth(30);
        selectedPsmsTable.getColumn("  ").setMinWidth(30);

        relatedPsmsTable.getColumn("   ").setMaxWidth(30);
        relatedPsmsTable.getColumn("   ").setMinWidth(30);
        relatedPsmsTable.getColumn(" ").setMaxWidth(45);
        relatedPsmsTable.getColumn(" ").setMinWidth(45);
        relatedPsmsTable.getColumn("PTM").setMaxWidth(45);
        relatedPsmsTable.getColumn("PTM").setMinWidth(45);
        relatedPsmsTable.getColumn("Charge").setMaxWidth(90);
        relatedPsmsTable.getColumn("Charge").setMinWidth(90);
        relatedPsmsTable.getColumn("  ").setMaxWidth(30);
        relatedPsmsTable.getColumn("  ").setMinWidth(30);

        peptidesTable.getTableHeader().setReorderingAllowed(false);
        relatedPeptidesTable.getTableHeader().setReorderingAllowed(false);
        selectedPsmsTable.getTableHeader().setReorderingAllowed(false);
        relatedPsmsTable.getTableHeader().setReorderingAllowed(false);
        psmAScoresTable.getTableHeader().setReorderingAllowed(false);
        psmDeltaScoresTable.getTableHeader().setReorderingAllowed(false);

        // centrally align the column headers 
        TableCellRenderer renderer = psmAScoresTable.getTableHeader().getDefaultRenderer();
        JLabel label = (JLabel) renderer;
        label.setHorizontalAlignment(JLabel.CENTER);
        renderer = psmDeltaScoresTable.getTableHeader().getDefaultRenderer();
        label = (JLabel) renderer;
        label.setHorizontalAlignment(JLabel.CENTER);

        // set up the protein inference color map
        HashMap<Integer, Color> proteinInferenceColorMap = new HashMap<Integer, Color>();
        proteinInferenceColorMap.put(PSParameter.NOT_GROUP, peptideShakerGUI.getSparklineColor());
        proteinInferenceColorMap.put(PSParameter.RELATED, Color.YELLOW);
        proteinInferenceColorMap.put(PSParameter.RELATED_AND_UNRELATED, Color.ORANGE);
        proteinInferenceColorMap.put(PSParameter.UNRELATED, Color.RED);

        // set up the protein inference tooltip map
        proteinInferenceTooltipMap = new HashMap<Integer, String>();
        proteinInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Unique to a single protein");
        proteinInferenceTooltipMap.put(PSParameter.RELATED, "Belongs to a group of related proteins");
        proteinInferenceTooltipMap.put(PSParameter.RELATED_AND_UNRELATED, "Belongs to a group of related and unrelated proteins");
        proteinInferenceTooltipMap.put(PSParameter.UNRELATED, "Belongs to unrelated proteins");

        peptidesTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.lightGray, proteinInferenceColorMap, proteinInferenceTooltipMap));
        relatedPeptidesTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.lightGray, proteinInferenceColorMap, proteinInferenceTooltipMap));
        peptidesTable.getColumn("   ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));
        relatedPeptidesTable.getColumn("   ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));

        // set up the PTM confidence color map
        HashMap<Integer, Color> ptmConfidenceColorMap = new HashMap<Integer, Color>();
        ptmConfidenceColorMap.put(PtmScoring.NOT_FOUND, Color.lightGray);
        ptmConfidenceColorMap.put(PtmScoring.RANDOM, Color.RED);
        ptmConfidenceColorMap.put(PtmScoring.DOUBTFUL, Color.ORANGE);
        ptmConfidenceColorMap.put(PtmScoring.CONFIDENT, Color.YELLOW);
        ptmConfidenceColorMap.put(PtmScoring.VERY_CONFIDENT, peptideShakerGUI.getSparklineColor());

        // set up the PTM confidence tooltip map
        ptmConfidenceTooltipMap = new HashMap<Integer, String>();
        ptmConfidenceTooltipMap.put(-1, "(No PTMs)");
        ptmConfidenceTooltipMap.put(PtmScoring.RANDOM, "Random Assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.DOUBTFUL, "Doubtful Assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.CONFIDENT, "Confident Assignment");
        ptmConfidenceTooltipMap.put(PtmScoring.VERY_CONFIDENT, "Very Confident Assignment");

        peptidesTable.getColumn("PTM").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.lightGray, ptmConfidenceColorMap, ptmConfidenceTooltipMap));
        relatedPeptidesTable.getColumn("PTM").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.lightGray, ptmConfidenceColorMap, ptmConfidenceTooltipMap));
        selectedPsmsTable.getColumn("PTM").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.lightGray, ptmConfidenceColorMap, ptmConfidenceTooltipMap));
        relatedPsmsTable.getColumn("PTM").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(Color.lightGray, ptmConfidenceColorMap, ptmConfidenceTooltipMap));

        peptidesTable.getColumn("Peptide").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Peptide").getCellRenderer()).showNumberAndChart(
                true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        peptidesTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));

        relatedPeptidesTable.getColumn("Peptide").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Peptide").getCellRenderer()).showNumberAndChart(
                true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        relatedPeptidesTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));

        selectedPsmsTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) selectedPsmsTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);
        selectedPsmsTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        selectedPsmsTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d,
                1000d, 10d, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);
        ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
        selectedPsmsTable.getColumn("   ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));

        relatedPsmsTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPsmsTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() - 30);
        relatedPsmsTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));
        relatedPsmsTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d,
                1000d, 10d, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);
        ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
        relatedPsmsTable.getColumn("   ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));

        // ptm color coding
        ptmJTable.getColumn("  ").setCellRenderer(new JSparklinesColorTableCellRenderer());

        // set up the table header tooltips
        selectedPeptidesTableToolTips = new ArrayList<String>();
        selectedPeptidesTableToolTips.add(null);
        selectedPeptidesTableToolTips.add("Starred");
        selectedPeptidesTableToolTips.add("Peptide Inference Class");
        selectedPeptidesTableToolTips.add("Peptide Sequence");
        selectedPeptidesTableToolTips.add("PTM Location Confidence");
        selectedPeptidesTableToolTips.add("Peptide Confidence");
        selectedPeptidesTableToolTips.add("Peptide Validated");

        relatedPeptidesTableToolTips = new ArrayList<String>();
        relatedPeptidesTableToolTips.add(null);
        relatedPeptidesTableToolTips.add("Starred");
        relatedPeptidesTableToolTips.add("Peptide Inference Class");
        relatedPeptidesTableToolTips.add("Peptide Sequence");
        relatedPeptidesTableToolTips.add("PTM Location Confidence");
        relatedPeptidesTableToolTips.add("Peptide Confidence");
        relatedPeptidesTableToolTips.add("Peptide Validated");

        selectedPsmsTableToolTips = new ArrayList<String>();
        selectedPsmsTableToolTips.add(null);
        selectedPsmsTableToolTips.add("Starred");
        selectedPsmsTableToolTips.add("Peptide Sequence");
        selectedPsmsTableToolTips.add("PTM Location Confidence");
        selectedPsmsTableToolTips.add("Precursor Charge");
        selectedPsmsTableToolTips.add("Precursor Retention Time");
        selectedPsmsTableToolTips.add("PSM Validated");

        relatedPsmsTableToolTips = new ArrayList<String>();
        relatedPsmsTableToolTips.add(null);
        relatedPsmsTableToolTips.add("Starred");
        relatedPsmsTableToolTips.add("Peptide Sequence");
        relatedPsmsTableToolTips.add("PTM Location Confidence");
        relatedPsmsTableToolTips.add("Precursor Charge");
        relatedPsmsTableToolTips.add("Precursor Retention Time");
        relatedPsmsTableToolTips.add("PSM Validated");

        ptmTableToolTips = new ArrayList<String>();
        ptmTableToolTips.add("PTM Color");
        ptmTableToolTips.add("PTM Name");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ptmAndPeptideSelectionPanel = new javax.swing.JPanel();
        ptmPanel = new javax.swing.JPanel();
        ptmLayeredLayeredPane = new javax.swing.JLayeredPane();
        ptmLayeredPanel = new javax.swing.JPanel();
        ptmJScrollPane = new javax.swing.JScrollPane();
        ptmJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) ptmTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        ptmSelectionHelpJButton = new javax.swing.JButton();
        contextMenuPtmBackgroundPanel = new javax.swing.JPanel();
        peptideTablesJSplitPane = new javax.swing.JSplitPane();
        modifiedPeptidesPanel = new javax.swing.JPanel();
        modifiedPeptidesLayeredPane = new javax.swing.JLayeredPane();
        selectedPeptidesJPanel = new javax.swing.JPanel();
        selectedPeptidesJSplitPane = new javax.swing.JSplitPane();
        peptidesTableJScrollPane = new javax.swing.JScrollPane();
        peptidesTable =         new JTable() {

            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) selectedPeptidesTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }

            /**
            * Returns false to indicate that <strong class="highlight">horizontal</strong> scrollbars are required
            * to display the table while honoring perferred column widths. Returns
            * true if the table can be displayed in viewport without <strong class="highlight">horizontal</strong>
            * scrollbars.
            *
            * @return true if an auto-resizing mode is enabled
            *   and the viewport width is larger than the table's
            *   preferred size, otherwise return false.
            * @see Scrollable#getScrollableTracksViewportWidth
            */
            public boolean getScrollableTracksViewportWidth() {
                if (autoResizeMode != AUTO_RESIZE_OFF) {
                    if (getParent() instanceof JViewport) {
                        return (((JViewport) getParent()).getWidth() > getPreferredSize().width);
                    }
                }
                return false;
            }
        };
        modificationProfileSelectedPeptideJPanel = new javax.swing.JPanel();
        modificationProfileHelpJButton = new javax.swing.JButton();
        exportModifiedPeptideProfileJButton = new javax.swing.JButton();
        contextMenuModifiedPeptidesBackgroundPanel = new javax.swing.JPanel();
        relatedPeptidesJPanel = new javax.swing.JPanel();
        relatedPeptiesLayeredPane = new javax.swing.JLayeredPane();
        relatedPeptidesPanel = new javax.swing.JPanel();
        relatedPeptidesJSplitPane = new javax.swing.JSplitPane();
        relatedPeptidesTableJScrollPane = new javax.swing.JScrollPane();
        relatedPeptidesTable =         new JTable() {

            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {

                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) relatedPeptidesTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }

            /**
            * Returns false to indicate that <strong class="highlight">horizontal</strong> scrollbars are required
            * to display the table while honoring perferred column widths. Returns
            * true if the table can be displayed in viewport without <strong class="highlight">horizontal</strong>
            * scrollbars.
            *
            * @return true if an auto-resizing mode is enabled
            *   and the viewport width is larger than the table's
            *   preferred size, otherwise return false.
            * @see Scrollable#getScrollableTracksViewportWidth
            */
            public boolean getScrollableTracksViewportWidth() {
                if (autoResizeMode != AUTO_RESIZE_OFF) {
                    if (getParent() instanceof JViewport) {
                        return (((JViewport) getParent()).getWidth() > getPreferredSize().width);
                    }
                }
                return false;
            }
        };
        modificationProfileRelatedPeptideJPanel = new javax.swing.JPanel();
        relatedProfileHelpJButton = new javax.swing.JButton();
        exportRelatedPeptideProfileJButton = new javax.swing.JButton();
        contextMenuRelatedPeptidesBackgroundPanel = new javax.swing.JPanel();
        psmSpectraSplitPane = new javax.swing.JSplitPane();
        spectrumAndFragmentIonJPanel = new javax.swing.JPanel();
        spectrumLayeredPane = new javax.swing.JLayeredPane();
        spectrumAndFragmentIonPanel = new javax.swing.JPanel();
        slidersSplitPane = new javax.swing.JSplitPane();
        spectrumTabbedPane = new javax.swing.JTabbedPane();
        psmAScoresJPanel = new javax.swing.JPanel();
        psmAScoresScrollPane = new javax.swing.JScrollPane();
        psmAScoresTable = new javax.swing.JTable();
        aScoreMinValueJLabel = new javax.swing.JLabel();
        aScoreGradientPanel = new javax.swing.JPanel();
        aScoreMaxValueJLabel = new javax.swing.JLabel();
        psmDeltaScoresJPanel = new javax.swing.JPanel();
        psmDeltaScrollPane = new javax.swing.JScrollPane();
        psmDeltaScoresTable = new javax.swing.JTable();
        deltaScoreGradientPanel = new javax.swing.JPanel();
        deltaScoreMinValueJLabel = new javax.swing.JLabel();
        deltaScoreMaxValueJLabel = new javax.swing.JLabel();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        spectrumAnnotationMenuPanel = new javax.swing.JPanel();
        spectrumChartJPanel = new javax.swing.JPanel();
        sliderPanel = new javax.swing.JPanel();
        accuracySlider = new javax.swing.JSlider();
        intensitySlider = new javax.swing.JSlider();
        spectrumHelpJButton = new javax.swing.JButton();
        exportSpectrumJButton = new javax.swing.JButton();
        contextMenuSpectrumBackgroundPanel = new javax.swing.JPanel();
        psmSplitPane = new javax.swing.JSplitPane();
        modPsmsPanel = new javax.swing.JPanel();
        psmsModPeptidesLayeredPane = new javax.swing.JLayeredPane();
        modsPsmsLayeredPanel = new javax.swing.JPanel();
        psmsModifiedTableJScrollPane = new javax.swing.JScrollPane();
        selectedPsmsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) selectedPsmsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                //Always toggle on single selection
                super.changeSelection(rowIndex, columnIndex, !extend, extend);
            }
        };
        modifiedPsmsHelpJButton = new javax.swing.JButton();
        exportModifiedPsmsJButton = new javax.swing.JButton();
        contextMenuModPsmsBackgroundPanel = new javax.swing.JPanel();
        relatedPsmsJPanel = new javax.swing.JPanel();
        psmsRelatedPeptidesJLayeredPane = new javax.swing.JLayeredPane();
        relatedPsmsPanel = new javax.swing.JPanel();
        psmsRelatedTableJScrollPane = new javax.swing.JScrollPane();
        relatedPsmsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) selectedPsmsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                //Always toggle on single selection
                super.changeSelection(rowIndex, columnIndex, !extend, extend);
            }
        };
        relatedPsmsHelpJButton = new javax.swing.JButton();
        exportRelatedPsmsJButton = new javax.swing.JButton();
        contextMenuRelatedPsmsBackgroundPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        ptmAndPeptideSelectionPanel.setOpaque(false);

        ptmPanel.setOpaque(false);

        ptmLayeredPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Variable Modifications"));
        ptmLayeredPanel.setOpaque(false);

        ptmJScrollPane.setOpaque(false);

        ptmJTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "  ", "PTM"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        ptmJTable.setOpaque(false);
        ptmJTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        ptmJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ptmJTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                ptmJTableMouseReleased(evt);
            }
        });
        ptmJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                ptmJTableMouseMoved(evt);
            }
        });
        ptmJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                ptmJTableKeyReleased(evt);
            }
        });
        ptmJScrollPane.setViewportView(ptmJTable);

        javax.swing.GroupLayout ptmLayeredPanelLayout = new javax.swing.GroupLayout(ptmLayeredPanel);
        ptmLayeredPanel.setLayout(ptmLayeredPanelLayout);
        ptmLayeredPanelLayout.setHorizontalGroup(
            ptmLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 248, Short.MAX_VALUE)
            .addGroup(ptmLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ptmLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(ptmJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        ptmLayeredPanelLayout.setVerticalGroup(
            ptmLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 375, Short.MAX_VALUE)
            .addGroup(ptmLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ptmLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(ptmJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        ptmLayeredLayeredPane.add(ptmLayeredPanel);
        ptmLayeredPanel.setBounds(0, 0, 260, 400);

        ptmSelectionHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        ptmSelectionHelpJButton.setToolTipText("Help");
        ptmSelectionHelpJButton.setBorder(null);
        ptmSelectionHelpJButton.setBorderPainted(false);
        ptmSelectionHelpJButton.setContentAreaFilled(false);
        ptmSelectionHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        ptmSelectionHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ptmSelectionHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ptmSelectionHelpJButtonMouseExited(evt);
            }
        });
        ptmSelectionHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ptmSelectionHelpJButtonActionPerformed(evt);
            }
        });
        ptmLayeredLayeredPane.add(ptmSelectionHelpJButton);
        ptmSelectionHelpJButton.setBounds(240, 0, 10, 19);
        ptmLayeredLayeredPane.setLayer(ptmSelectionHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPtmBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPtmBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPtmBackgroundPanel);
        contextMenuPtmBackgroundPanel.setLayout(contextMenuPtmBackgroundPanelLayout);
        contextMenuPtmBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPtmBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );
        contextMenuPtmBackgroundPanelLayout.setVerticalGroup(
            contextMenuPtmBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        ptmLayeredLayeredPane.add(contextMenuPtmBackgroundPanel);
        contextMenuPtmBackgroundPanel.setBounds(230, 0, 20, 19);
        ptmLayeredLayeredPane.setLayer(contextMenuPtmBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout ptmPanelLayout = new javax.swing.GroupLayout(ptmPanel);
        ptmPanel.setLayout(ptmPanelLayout);
        ptmPanelLayout.setHorizontalGroup(
            ptmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ptmLayeredLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
        );
        ptmPanelLayout.setVerticalGroup(
            ptmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ptmLayeredLayeredPane)
        );

        peptideTablesJSplitPane.setBorder(null);
        peptideTablesJSplitPane.setDividerLocation(170);
        peptideTablesJSplitPane.setDividerSize(0);
        peptideTablesJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptideTablesJSplitPane.setResizeWeight(0.5);
        peptideTablesJSplitPane.setOpaque(false);

        modifiedPeptidesPanel.setOpaque(false);

        selectedPeptidesJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Modified Peptides"));
        selectedPeptidesJPanel.setOpaque(false);

        selectedPeptidesJSplitPane.setBorder(null);
        selectedPeptidesJSplitPane.setDividerLocation(400);
        selectedPeptidesJSplitPane.setDividerSize(0);
        selectedPeptidesJSplitPane.setResizeWeight(0.5);
        selectedPeptidesJSplitPane.setOpaque(false);

        peptidesTableJScrollPane.setOpaque(false);

        peptidesTable.setModel(new PeptideTable());
        peptidesTable.setOpaque(false);
        peptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        peptidesTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptidesTableMouseMoved(evt);
            }
        });
        peptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptidesTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptidesTableMouseReleased(evt);
            }
        });
        peptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptidesTableKeyReleased(evt);
            }
        });
        peptidesTableJScrollPane.setViewportView(peptidesTable);

        selectedPeptidesJSplitPane.setLeftComponent(peptidesTableJScrollPane);

        modificationProfileSelectedPeptideJPanel.setBackground(new java.awt.Color(255, 255, 255));
        modificationProfileSelectedPeptideJPanel.setOpaque(false);
        modificationProfileSelectedPeptideJPanel.setLayout(new java.awt.GridLayout(3, 1));
        selectedPeptidesJSplitPane.setRightComponent(modificationProfileSelectedPeptideJPanel);

        javax.swing.GroupLayout selectedPeptidesJPanelLayout = new javax.swing.GroupLayout(selectedPeptidesJPanel);
        selectedPeptidesJPanel.setLayout(selectedPeptidesJPanelLayout);
        selectedPeptidesJPanelLayout.setHorizontalGroup(
            selectedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, selectedPeptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 774, Short.MAX_VALUE)
                .addContainerGap())
        );
        selectedPeptidesJPanelLayout.setVerticalGroup(
            selectedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectedPeptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)
                .addContainerGap())
        );

        modifiedPeptidesLayeredPane.add(selectedPeptidesJPanel);
        selectedPeptidesJPanel.setBounds(0, 0, 810, 170);

        modificationProfileHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        modificationProfileHelpJButton.setToolTipText("Help");
        modificationProfileHelpJButton.setBorder(null);
        modificationProfileHelpJButton.setBorderPainted(false);
        modificationProfileHelpJButton.setContentAreaFilled(false);
        modificationProfileHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        modificationProfileHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                modificationProfileHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                modificationProfileHelpJButtonMouseExited(evt);
            }
        });
        modificationProfileHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modificationProfileHelpJButtonActionPerformed(evt);
            }
        });
        modifiedPeptidesLayeredPane.add(modificationProfileHelpJButton);
        modificationProfileHelpJButton.setBounds(747, 0, 10, 19);
        modifiedPeptidesLayeredPane.setLayer(modificationProfileHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportModifiedPeptideProfileJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportModifiedPeptideProfileJButton.setToolTipText("Export");
        exportModifiedPeptideProfileJButton.setBorder(null);
        exportModifiedPeptideProfileJButton.setBorderPainted(false);
        exportModifiedPeptideProfileJButton.setContentAreaFilled(false);
        exportModifiedPeptideProfileJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportModifiedPeptideProfileJButton.setEnabled(false);
        exportModifiedPeptideProfileJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportModifiedPeptideProfileJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportModifiedPeptideProfileJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportModifiedPeptideProfileJButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportModifiedPeptideProfileJButtonMouseReleased(evt);
            }
        });
        modifiedPeptidesLayeredPane.add(exportModifiedPeptideProfileJButton);
        exportModifiedPeptideProfileJButton.setBounds(730, 0, 10, 19);
        modifiedPeptidesLayeredPane.setLayer(exportModifiedPeptideProfileJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuModifiedPeptidesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuModifiedPeptidesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuModifiedPeptidesBackgroundPanel);
        contextMenuModifiedPeptidesBackgroundPanel.setLayout(contextMenuModifiedPeptidesBackgroundPanelLayout);
        contextMenuModifiedPeptidesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuModifiedPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuModifiedPeptidesBackgroundPanelLayout.setVerticalGroup(
            contextMenuModifiedPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        modifiedPeptidesLayeredPane.add(contextMenuModifiedPeptidesBackgroundPanel);
        contextMenuModifiedPeptidesBackgroundPanel.setBounds(730, 0, 30, 19);
        modifiedPeptidesLayeredPane.setLayer(contextMenuModifiedPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout modifiedPeptidesPanelLayout = new javax.swing.GroupLayout(modifiedPeptidesPanel);
        modifiedPeptidesPanel.setLayout(modifiedPeptidesPanelLayout);
        modifiedPeptidesPanelLayout.setHorizontalGroup(
            modifiedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 813, Short.MAX_VALUE)
            .addGroup(modifiedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(modifiedPeptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 813, Short.MAX_VALUE))
        );
        modifiedPeptidesPanelLayout.setVerticalGroup(
            modifiedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 170, Short.MAX_VALUE)
            .addGroup(modifiedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(modifiedPeptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE))
        );

        peptideTablesJSplitPane.setLeftComponent(modifiedPeptidesPanel);

        relatedPeptidesJPanel.setOpaque(false);

        relatedPeptidesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Related Peptides"));
        relatedPeptidesPanel.setOpaque(false);

        relatedPeptidesJSplitPane.setBorder(null);
        relatedPeptidesJSplitPane.setDividerLocation(400);
        relatedPeptidesJSplitPane.setDividerSize(0);
        relatedPeptidesJSplitPane.setOpaque(false);

        relatedPeptidesTableJScrollPane.setOpaque(false);

        relatedPeptidesTable.setModel(new RelatedPeptidesTable());
        relatedPeptidesTable.setOpaque(false);
        relatedPeptidesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        relatedPeptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                relatedPeptidesTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                relatedPeptidesTableMouseReleased(evt);
            }
        });
        relatedPeptidesTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                relatedPeptidesTableMouseMoved(evt);
            }
        });
        relatedPeptidesTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                relatedPeptidesTableKeyReleased(evt);
            }
        });
        relatedPeptidesTableJScrollPane.setViewportView(relatedPeptidesTable);

        relatedPeptidesJSplitPane.setLeftComponent(relatedPeptidesTableJScrollPane);

        modificationProfileRelatedPeptideJPanel.setBackground(new java.awt.Color(255, 255, 255));
        modificationProfileRelatedPeptideJPanel.setOpaque(false);
        modificationProfileRelatedPeptideJPanel.setLayout(new java.awt.GridLayout(3, 1));
        relatedPeptidesJSplitPane.setRightComponent(modificationProfileRelatedPeptideJPanel);

        javax.swing.GroupLayout relatedPeptidesPanelLayout = new javax.swing.GroupLayout(relatedPeptidesPanel);
        relatedPeptidesPanel.setLayout(relatedPeptidesPanelLayout);
        relatedPeptidesPanelLayout.setHorizontalGroup(
            relatedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 798, Short.MAX_VALUE)
            .addGroup(relatedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(relatedPeptidesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(relatedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 774, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        relatedPeptidesPanelLayout.setVerticalGroup(
            relatedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 205, Short.MAX_VALUE)
            .addGroup(relatedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(relatedPeptidesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(relatedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        relatedPeptiesLayeredPane.add(relatedPeptidesPanel);
        relatedPeptidesPanel.setBounds(0, 0, 810, 230);

        relatedProfileHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        relatedProfileHelpJButton.setToolTipText("Help");
        relatedProfileHelpJButton.setBorder(null);
        relatedProfileHelpJButton.setBorderPainted(false);
        relatedProfileHelpJButton.setContentAreaFilled(false);
        relatedProfileHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        relatedProfileHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                relatedProfileHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                relatedProfileHelpJButtonMouseExited(evt);
            }
        });
        relatedProfileHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                relatedProfileHelpJButtonActionPerformed(evt);
            }
        });
        relatedPeptiesLayeredPane.add(relatedProfileHelpJButton);
        relatedProfileHelpJButton.setBounds(750, 0, 10, 19);
        relatedPeptiesLayeredPane.setLayer(relatedProfileHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportRelatedPeptideProfileJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportRelatedPeptideProfileJButton.setToolTipText("Export");
        exportRelatedPeptideProfileJButton.setBorder(null);
        exportRelatedPeptideProfileJButton.setBorderPainted(false);
        exportRelatedPeptideProfileJButton.setContentAreaFilled(false);
        exportRelatedPeptideProfileJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportRelatedPeptideProfileJButton.setEnabled(false);
        exportRelatedPeptideProfileJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportRelatedPeptideProfileJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportRelatedPeptideProfileJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportRelatedPeptideProfileJButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                exportRelatedPeptideProfileJButtonMouseReleased(evt);
            }
        });
        relatedPeptiesLayeredPane.add(exportRelatedPeptideProfileJButton);
        exportRelatedPeptideProfileJButton.setBounds(740, 0, 10, 19);
        relatedPeptiesLayeredPane.setLayer(exportRelatedPeptideProfileJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuRelatedPeptidesBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuRelatedPeptidesBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuRelatedPeptidesBackgroundPanel);
        contextMenuRelatedPeptidesBackgroundPanel.setLayout(contextMenuRelatedPeptidesBackgroundPanelLayout);
        contextMenuRelatedPeptidesBackgroundPanelLayout.setHorizontalGroup(
            contextMenuRelatedPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuRelatedPeptidesBackgroundPanelLayout.setVerticalGroup(
            contextMenuRelatedPeptidesBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        relatedPeptiesLayeredPane.add(contextMenuRelatedPeptidesBackgroundPanel);
        contextMenuRelatedPeptidesBackgroundPanel.setBounds(730, 0, 30, 19);
        relatedPeptiesLayeredPane.setLayer(contextMenuRelatedPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout relatedPeptidesJPanelLayout = new javax.swing.GroupLayout(relatedPeptidesJPanel);
        relatedPeptidesJPanel.setLayout(relatedPeptidesJPanelLayout);
        relatedPeptidesJPanelLayout.setHorizontalGroup(
            relatedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(relatedPeptiesLayeredPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 813, Short.MAX_VALUE)
        );
        relatedPeptidesJPanelLayout.setVerticalGroup(
            relatedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(relatedPeptiesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
        );

        peptideTablesJSplitPane.setRightComponent(relatedPeptidesJPanel);

        javax.swing.GroupLayout ptmAndPeptideSelectionPanelLayout = new javax.swing.GroupLayout(ptmAndPeptideSelectionPanel);
        ptmAndPeptideSelectionPanel.setLayout(ptmAndPeptideSelectionPanelLayout);
        ptmAndPeptideSelectionPanelLayout.setHorizontalGroup(
            ptmAndPeptideSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ptmAndPeptideSelectionPanelLayout.createSequentialGroup()
                .addComponent(ptmPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptideTablesJSplitPane))
        );
        ptmAndPeptideSelectionPanelLayout.setVerticalGroup(
            ptmAndPeptideSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ptmPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(peptideTablesJSplitPane)
        );

        psmSpectraSplitPane.setBorder(null);
        psmSpectraSplitPane.setDividerLocation(500);
        psmSpectraSplitPane.setDividerSize(0);
        psmSpectraSplitPane.setResizeWeight(0.5);
        psmSpectraSplitPane.setOpaque(false);

        spectrumAndFragmentIonJPanel.setOpaque(false);
        spectrumAndFragmentIonJPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                spectrumAndFragmentIonJPanelMouseWheelMoved(evt);
            }
        });

        spectrumAndFragmentIonPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum & Fragment Ions"));
        spectrumAndFragmentIonPanel.setOpaque(false);

        slidersSplitPane.setBorder(null);
        slidersSplitPane.setDividerLocation(500);
        slidersSplitPane.setDividerSize(0);
        slidersSplitPane.setOpaque(false);

        spectrumTabbedPane.setBackground(new java.awt.Color(255, 255, 255));
        spectrumTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
        spectrumTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spectrumTabbedPaneStateChanged(evt);
            }
        });

        psmAScoresJPanel.setOpaque(false);

        psmAScoresScrollPane.setOpaque(false);

        psmAScoresTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        psmAScoresTable.setOpaque(false);
        psmAScoresScrollPane.setViewportView(psmAScoresTable);

        aScoreMinValueJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        aScoreMinValueJLabel.setText("0%");

        aScoreGradientPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        aScoreGradientPanel.setToolTipText("A-Score Certainty");
        aScoreGradientPanel.setLayout(new java.awt.BorderLayout());

        aScoreMaxValueJLabel.setText("100%");

        javax.swing.GroupLayout psmAScoresJPanelLayout = new javax.swing.GroupLayout(psmAScoresJPanel);
        psmAScoresJPanel.setLayout(psmAScoresJPanelLayout);
        psmAScoresJPanelLayout.setHorizontalGroup(
            psmAScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmAScoresJPanelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(aScoreMinValueJLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(aScoreGradientPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(aScoreMaxValueJLabel)
                .addGap(20, 20, 20))
            .addGroup(psmAScoresJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmAScoresScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmAScoresJPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {aScoreMaxValueJLabel, aScoreMinValueJLabel});

        psmAScoresJPanelLayout.setVerticalGroup(
            psmAScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmAScoresJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmAScoresScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psmAScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(aScoreMinValueJLabel)
                    .addComponent(aScoreGradientPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aScoreMaxValueJLabel))
                .addContainerGap())
        );

        spectrumTabbedPane.addTab("Probabilistic Scores", psmAScoresJPanel);

        psmDeltaScoresJPanel.setOpaque(false);

        psmDeltaScrollPane.setOpaque(false);

        psmDeltaScoresTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        psmDeltaScoresTable.setOpaque(false);
        psmDeltaScrollPane.setViewportView(psmDeltaScoresTable);

        deltaScoreGradientPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        deltaScoreGradientPanel.setToolTipText("Delta Score Certainty");
        deltaScoreGradientPanel.setLayout(new java.awt.BorderLayout());

        deltaScoreMinValueJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        deltaScoreMinValueJLabel.setText("0%");

        deltaScoreMaxValueJLabel.setText("100%");

        javax.swing.GroupLayout psmDeltaScoresJPanelLayout = new javax.swing.GroupLayout(psmDeltaScoresJPanel);
        psmDeltaScoresJPanel.setLayout(psmDeltaScoresJPanelLayout);
        psmDeltaScoresJPanelLayout.setHorizontalGroup(
            psmDeltaScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmDeltaScoresJPanelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(deltaScoreMinValueJLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(deltaScoreGradientPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(deltaScoreMaxValueJLabel)
                .addGap(20, 20, 20))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmDeltaScoresJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmDeltaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmDeltaScoresJPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {deltaScoreMaxValueJLabel, deltaScoreMinValueJLabel});

        psmDeltaScoresJPanelLayout.setVerticalGroup(
            psmDeltaScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmDeltaScoresJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmDeltaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psmDeltaScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(deltaScoreMinValueJLabel)
                    .addComponent(deltaScoreGradientPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deltaScoreMaxValueJLabel))
                .addContainerGap())
        );

        spectrumTabbedPane.addTab("D-Scores", psmDeltaScoresJPanel);

        spectrumJPanel.setBackground(new java.awt.Color(255, 255, 255));

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setBorder(null);
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);
        spectrumJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(spectrumAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumJToolBar.add(spectrumAnnotationMenuPanel);

        spectrumChartJPanel.setOpaque(false);
        spectrumChartJPanel.setLayout(new javax.swing.BoxLayout(spectrumChartJPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 495, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumTabbedPane.setSelectedIndex(2);

        slidersSplitPane.setLeftComponent(spectrumTabbedPane);

        sliderPanel.setMaximumSize(new java.awt.Dimension(36, 312));
        sliderPanel.setOpaque(false);
        sliderPanel.setPreferredSize(new java.awt.Dimension(36, 312));

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

        javax.swing.GroupLayout sliderPanelLayout = new javax.swing.GroupLayout(sliderPanel);
        sliderPanel.setLayout(sliderPanelLayout);
        sliderPanelLayout.setHorizontalGroup(
            sliderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sliderPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(sliderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(intensitySlider, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(accuracySlider, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0))
        );
        sliderPanelLayout.setVerticalGroup(
            sliderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sliderPanelLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                .addGap(27, 27, 27)
                .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                .addGap(47, 47, 47))
        );

        slidersSplitPane.setRightComponent(sliderPanel);

        javax.swing.GroupLayout spectrumAndFragmentIonPanelLayout = new javax.swing.GroupLayout(spectrumAndFragmentIonPanel);
        spectrumAndFragmentIonPanel.setLayout(spectrumAndFragmentIonPanelLayout);
        spectrumAndFragmentIonPanelLayout.setHorizontalGroup(
            spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slidersSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 534, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumAndFragmentIonPanelLayout.setVerticalGroup(
            spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slidersSplitPane)
                .addContainerGap())
        );

        spectrumLayeredPane.add(spectrumAndFragmentIonPanel);
        spectrumAndFragmentIonPanel.setBounds(0, 0, 570, 440);

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
        spectrumHelpJButton.setBounds(540, 0, 10, 19);
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
        exportSpectrumJButton.setBounds(530, 0, 10, 19);
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
        contextMenuSpectrumBackgroundPanel.setBounds(530, 0, 30, 19);
        spectrumLayeredPane.setLayer(contextMenuSpectrumBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout spectrumAndFragmentIonJPanelLayout = new javax.swing.GroupLayout(spectrumAndFragmentIonJPanel);
        spectrumAndFragmentIonJPanel.setLayout(spectrumAndFragmentIonJPanelLayout);
        spectrumAndFragmentIonJPanelLayout.setHorizontalGroup(
            spectrumAndFragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumAndFragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 568, Short.MAX_VALUE))
        );
        spectrumAndFragmentIonJPanelLayout.setVerticalGroup(
            spectrumAndFragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 444, Short.MAX_VALUE)
        );

        psmSpectraSplitPane.setRightComponent(spectrumAndFragmentIonJPanel);

        psmSplitPane.setBorder(null);
        psmSplitPane.setDividerSize(0);
        psmSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        psmSplitPane.setOpaque(false);

        modPsmsPanel.setOpaque(false);

        modsPsmsLayeredPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide Spectrum Matches - Modified Peptide"));
        modsPsmsLayeredPanel.setOpaque(false);

        psmsModifiedTableJScrollPane.setOpaque(false);

        selectedPsmsTable.setModel(new PsmsTable(false));
        selectedPsmsTable.setOpaque(false);
        selectedPsmsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        selectedPsmsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                selectedPsmsTableMouseMoved(evt);
            }
        });
        selectedPsmsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                selectedPsmsTableMouseReleased(evt);
            }
        });
        selectedPsmsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                selectedPsmsTableKeyReleased(evt);
            }
        });
        psmsModifiedTableJScrollPane.setViewportView(selectedPsmsTable);

        javax.swing.GroupLayout modsPsmsLayeredPanelLayout = new javax.swing.GroupLayout(modsPsmsLayeredPanel);
        modsPsmsLayeredPanel.setLayout(modsPsmsLayeredPanelLayout);
        modsPsmsLayeredPanelLayout.setHorizontalGroup(
            modsPsmsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 488, Short.MAX_VALUE)
            .addGroup(modsPsmsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(modsPsmsLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        modsPsmsLayeredPanelLayout.setVerticalGroup(
            modsPsmsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 165, Short.MAX_VALUE)
            .addGroup(modsPsmsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(modsPsmsLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        psmsModPeptidesLayeredPane.add(modsPsmsLayeredPanel);
        modsPsmsLayeredPanel.setBounds(0, 0, 500, 190);

        modifiedPsmsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        modifiedPsmsHelpJButton.setToolTipText("Help");
        modifiedPsmsHelpJButton.setBorder(null);
        modifiedPsmsHelpJButton.setBorderPainted(false);
        modifiedPsmsHelpJButton.setContentAreaFilled(false);
        modifiedPsmsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        modifiedPsmsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                modifiedPsmsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                modifiedPsmsHelpJButtonMouseExited(evt);
            }
        });
        modifiedPsmsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modifiedPsmsHelpJButtonActionPerformed(evt);
            }
        });
        psmsModPeptidesLayeredPane.add(modifiedPsmsHelpJButton);
        modifiedPsmsHelpJButton.setBounds(480, 0, 10, 19);
        psmsModPeptidesLayeredPane.setLayer(modifiedPsmsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportModifiedPsmsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportModifiedPsmsJButton.setToolTipText("Copy to Clipboard");
        exportModifiedPsmsJButton.setBorder(null);
        exportModifiedPsmsJButton.setBorderPainted(false);
        exportModifiedPsmsJButton.setContentAreaFilled(false);
        exportModifiedPsmsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportModifiedPsmsJButton.setEnabled(false);
        exportModifiedPsmsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportModifiedPsmsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportModifiedPsmsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportModifiedPsmsJButtonMouseExited(evt);
            }
        });
        exportModifiedPsmsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportModifiedPsmsJButtonActionPerformed(evt);
            }
        });
        psmsModPeptidesLayeredPane.add(exportModifiedPsmsJButton);
        exportModifiedPsmsJButton.setBounds(470, 0, 10, 19);
        psmsModPeptidesLayeredPane.setLayer(exportModifiedPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuModPsmsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuModPsmsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuModPsmsBackgroundPanel);
        contextMenuModPsmsBackgroundPanel.setLayout(contextMenuModPsmsBackgroundPanelLayout);
        contextMenuModPsmsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuModPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuModPsmsBackgroundPanelLayout.setVerticalGroup(
            contextMenuModPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        psmsModPeptidesLayeredPane.add(contextMenuModPsmsBackgroundPanel);
        contextMenuModPsmsBackgroundPanel.setBounds(460, 0, 30, 19);
        psmsModPeptidesLayeredPane.setLayer(contextMenuModPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout modPsmsPanelLayout = new javax.swing.GroupLayout(modPsmsPanel);
        modPsmsPanel.setLayout(modPsmsPanelLayout);
        modPsmsPanelLayout.setHorizontalGroup(
            modPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmsModPeptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
        );
        modPsmsPanelLayout.setVerticalGroup(
            modPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmsModPeptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
        );

        psmSplitPane.setTopComponent(modPsmsPanel);

        relatedPsmsJPanel.setOpaque(false);

        relatedPsmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide Spectrum Matches - Releated Peptide"));
        relatedPsmsPanel.setOpaque(false);

        psmsRelatedTableJScrollPane.setOpaque(false);

        relatedPsmsTable.setModel(new PsmsTable(true));
        relatedPsmsTable.setOpaque(false);
        relatedPsmsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        relatedPsmsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                relatedPsmsTableMouseMoved(evt);
            }
        });
        relatedPsmsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                relatedPsmsTableMouseReleased(evt);
            }
        });
        relatedPsmsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                relatedPsmsTableKeyReleased(evt);
            }
        });
        psmsRelatedTableJScrollPane.setViewportView(relatedPsmsTable);

        javax.swing.GroupLayout relatedPsmsPanelLayout = new javax.swing.GroupLayout(relatedPsmsPanel);
        relatedPsmsPanel.setLayout(relatedPsmsPanelLayout);
        relatedPsmsPanelLayout.setHorizontalGroup(
            relatedPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 488, Short.MAX_VALUE)
            .addGroup(relatedPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(relatedPsmsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(psmsRelatedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        relatedPsmsPanelLayout.setVerticalGroup(
            relatedPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 225, Short.MAX_VALUE)
            .addGroup(relatedPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(relatedPsmsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(psmsRelatedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        psmsRelatedPeptidesJLayeredPane.add(relatedPsmsPanel);
        relatedPsmsPanel.setBounds(0, 0, 500, 250);

        relatedPsmsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        relatedPsmsHelpJButton.setToolTipText("Help");
        relatedPsmsHelpJButton.setBorder(null);
        relatedPsmsHelpJButton.setBorderPainted(false);
        relatedPsmsHelpJButton.setContentAreaFilled(false);
        relatedPsmsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        relatedPsmsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                relatedPsmsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                relatedPsmsHelpJButtonMouseExited(evt);
            }
        });
        relatedPsmsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                relatedPsmsHelpJButtonActionPerformed(evt);
            }
        });
        psmsRelatedPeptidesJLayeredPane.add(relatedPsmsHelpJButton);
        relatedPsmsHelpJButton.setBounds(480, 0, 10, 19);
        psmsRelatedPeptidesJLayeredPane.setLayer(relatedPsmsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportRelatedPsmsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportRelatedPsmsJButton.setToolTipText("Copy to Clipboard");
        exportRelatedPsmsJButton.setBorder(null);
        exportRelatedPsmsJButton.setBorderPainted(false);
        exportRelatedPsmsJButton.setContentAreaFilled(false);
        exportRelatedPsmsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportRelatedPsmsJButton.setEnabled(false);
        exportRelatedPsmsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportRelatedPsmsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportRelatedPsmsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportRelatedPsmsJButtonMouseExited(evt);
            }
        });
        exportRelatedPsmsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportRelatedPsmsJButtonActionPerformed(evt);
            }
        });
        psmsRelatedPeptidesJLayeredPane.add(exportRelatedPsmsJButton);
        exportRelatedPsmsJButton.setBounds(470, 0, 10, 19);
        psmsRelatedPeptidesJLayeredPane.setLayer(exportRelatedPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuRelatedPsmsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuRelatedPsmsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuRelatedPsmsBackgroundPanel);
        contextMenuRelatedPsmsBackgroundPanel.setLayout(contextMenuRelatedPsmsBackgroundPanelLayout);
        contextMenuRelatedPsmsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuRelatedPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuRelatedPsmsBackgroundPanelLayout.setVerticalGroup(
            contextMenuRelatedPsmsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        psmsRelatedPeptidesJLayeredPane.add(contextMenuRelatedPsmsBackgroundPanel);
        contextMenuRelatedPsmsBackgroundPanel.setBounds(460, 0, 30, 19);
        psmsRelatedPeptidesJLayeredPane.setLayer(contextMenuRelatedPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout relatedPsmsJPanelLayout = new javax.swing.GroupLayout(relatedPsmsJPanel);
        relatedPsmsJPanel.setLayout(relatedPsmsJPanelLayout);
        relatedPsmsJPanelLayout.setHorizontalGroup(
            relatedPsmsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmsRelatedPeptidesJLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
        );
        relatedPsmsJPanelLayout.setVerticalGroup(
            relatedPsmsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(psmsRelatedPeptidesJLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 247, Short.MAX_VALUE)
        );

        psmSplitPane.setRightComponent(relatedPsmsJPanel);

        psmSpectraSplitPane.setLeftComponent(psmSplitPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(psmSpectraSplitPane, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ptmAndPeptideSelectionPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ptmAndPeptideSelectionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmSpectraSplitPane)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @see #peptidesTableMouseReleased(java.awt.event.MouseEvent)
     */
    private void peptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidesTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            peptidesTableMouseReleased(null);
        }
    }//GEN-LAST:event_peptidesTableKeyReleased

    /**
     * @see #relatedPeptidesTableMouseReleased(java.awt.event.MouseEvent)
     */
    private void relatedPeptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_relatedPeptidesTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            relatedPeptidesTableMouseReleased(null);
        }
    }//GEN-LAST:event_relatedPeptidesTableKeyReleased

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void selectedPsmsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_selectedPsmsTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            selectedPsmsTableMouseReleased(null);
        }
    }//GEN-LAST:event_selectedPsmsTableKeyReleased

    /**
     * Resize the panels after frame resizing.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // update the splitters
        peptideTablesJSplitPane.setDividerLocation(peptideTablesJSplitPane.getHeight() / 2);
        psmSpectraSplitPane.setDividerLocation(psmSpectraSplitPane.getWidth() / 2);
        psmSplitPane.setDividerLocation(psmSplitPane.getHeight() / 2);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                selectedPeptidesJSplitPane.setDividerLocation(selectedPeptidesJSplitPane.getWidth() / 2);
                updateUI();
            }
        });

        // resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // move the icons
                modifiedPeptidesLayeredPane.getComponent(0).setBounds(
                        modifiedPeptidesLayeredPane.getWidth() - modifiedPeptidesLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        modifiedPeptidesLayeredPane.getComponent(0).getWidth(),
                        modifiedPeptidesLayeredPane.getComponent(0).getHeight());

                modifiedPeptidesLayeredPane.getComponent(1).setBounds(
                        modifiedPeptidesLayeredPane.getWidth() - modifiedPeptidesLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        modifiedPeptidesLayeredPane.getComponent(1).getWidth(),
                        modifiedPeptidesLayeredPane.getComponent(1).getHeight());

                modifiedPeptidesLayeredPane.getComponent(2).setBounds(
                        modifiedPeptidesLayeredPane.getWidth() - modifiedPeptidesLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        modifiedPeptidesLayeredPane.getComponent(2).getWidth(),
                        modifiedPeptidesLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                modifiedPeptidesLayeredPane.getComponent(3).setBounds(0, 0, modifiedPeptidesLayeredPane.getWidth(), modifiedPeptidesLayeredPane.getHeight());
                modifiedPeptidesLayeredPane.revalidate();
                modifiedPeptidesLayeredPane.repaint();

                // move the icons
                relatedPeptiesLayeredPane.getComponent(0).setBounds(
                        relatedPeptiesLayeredPane.getWidth() - relatedPeptiesLayeredPane.getComponent(0).getWidth() - 10,
                        -5,
                        relatedPeptiesLayeredPane.getComponent(0).getWidth(),
                        relatedPeptiesLayeredPane.getComponent(0).getHeight());

                relatedPeptiesLayeredPane.getComponent(1).setBounds(
                        relatedPeptiesLayeredPane.getWidth() - relatedPeptiesLayeredPane.getComponent(1).getWidth() - 20,
                        -5,
                        relatedPeptiesLayeredPane.getComponent(1).getWidth(),
                        relatedPeptiesLayeredPane.getComponent(1).getHeight());

                relatedPeptiesLayeredPane.getComponent(2).setBounds(
                        relatedPeptiesLayeredPane.getWidth() - relatedPeptiesLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        relatedPeptiesLayeredPane.getComponent(2).getWidth(),
                        relatedPeptiesLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                relatedPeptiesLayeredPane.getComponent(3).setBounds(0, 0, relatedPeptiesLayeredPane.getWidth(), relatedPeptiesLayeredPane.getHeight());
                relatedPeptiesLayeredPane.revalidate();
                relatedPeptiesLayeredPane.repaint();

                // move the icons
                ptmLayeredLayeredPane.getComponent(0).setBounds(
                        ptmLayeredLayeredPane.getWidth() - ptmLayeredLayeredPane.getComponent(0).getWidth() - 10,
                        -5,
                        ptmLayeredLayeredPane.getComponent(0).getWidth(),
                        ptmLayeredLayeredPane.getComponent(0).getHeight());

                ptmLayeredLayeredPane.getComponent(1).setBounds(
                        ptmLayeredLayeredPane.getWidth() - ptmLayeredLayeredPane.getComponent(1).getWidth() - 5,
                        -3,
                        ptmLayeredLayeredPane.getComponent(1).getWidth(),
                        ptmLayeredLayeredPane.getComponent(1).getHeight());

                // resize the plot area
                ptmLayeredLayeredPane.getComponent(2).setBounds(0, 0, ptmLayeredLayeredPane.getWidth(), ptmLayeredLayeredPane.getHeight());
                ptmLayeredLayeredPane.revalidate();
                ptmLayeredLayeredPane.repaint();

                // move the icons
                psmsModPeptidesLayeredPane.getComponent(0).setBounds(
                        psmsModPeptidesLayeredPane.getWidth() - psmsModPeptidesLayeredPane.getComponent(0).getWidth() - 10,
                        -5,
                        psmsModPeptidesLayeredPane.getComponent(0).getWidth(),
                        psmsModPeptidesLayeredPane.getComponent(0).getHeight());

                psmsModPeptidesLayeredPane.getComponent(1).setBounds(
                        psmsModPeptidesLayeredPane.getWidth() - psmsModPeptidesLayeredPane.getComponent(1).getWidth() - 20,
                        -5,
                        psmsModPeptidesLayeredPane.getComponent(1).getWidth(),
                        psmsModPeptidesLayeredPane.getComponent(1).getHeight());

                psmsModPeptidesLayeredPane.getComponent(2).setBounds(
                        psmsModPeptidesLayeredPane.getWidth() - psmsModPeptidesLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        psmsModPeptidesLayeredPane.getComponent(2).getWidth(),
                        psmsModPeptidesLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                psmsModPeptidesLayeredPane.getComponent(3).setBounds(0, 0, psmsModPeptidesLayeredPane.getWidth(), psmsModPeptidesLayeredPane.getHeight());
                psmsModPeptidesLayeredPane.revalidate();
                psmsModPeptidesLayeredPane.repaint();

                // move the icons
                psmsRelatedPeptidesJLayeredPane.getComponent(0).setBounds(
                        psmsRelatedPeptidesJLayeredPane.getWidth() - psmsRelatedPeptidesJLayeredPane.getComponent(0).getWidth() - 10,
                        -5,
                        psmsRelatedPeptidesJLayeredPane.getComponent(0).getWidth(),
                        psmsRelatedPeptidesJLayeredPane.getComponent(0).getHeight());

                psmsRelatedPeptidesJLayeredPane.getComponent(1).setBounds(
                        psmsRelatedPeptidesJLayeredPane.getWidth() - psmsRelatedPeptidesJLayeredPane.getComponent(1).getWidth() - 20,
                        -5,
                        psmsRelatedPeptidesJLayeredPane.getComponent(1).getWidth(),
                        psmsRelatedPeptidesJLayeredPane.getComponent(1).getHeight());

                psmsRelatedPeptidesJLayeredPane.getComponent(2).setBounds(
                        psmsRelatedPeptidesJLayeredPane.getWidth() - psmsRelatedPeptidesJLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        psmsRelatedPeptidesJLayeredPane.getComponent(2).getWidth(),
                        psmsRelatedPeptidesJLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                psmsRelatedPeptidesJLayeredPane.getComponent(3).setBounds(0, 0, psmsRelatedPeptidesJLayeredPane.getWidth(), psmsRelatedPeptidesJLayeredPane.getHeight());
                psmsRelatedPeptidesJLayeredPane.revalidate();
                psmsRelatedPeptidesJLayeredPane.repaint();

                // move the icons
                spectrumLayeredPane.getComponent(0).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(0).getWidth() - 10,
                        -5,
                        spectrumLayeredPane.getComponent(0).getWidth(),
                        spectrumLayeredPane.getComponent(0).getHeight());

                spectrumLayeredPane.getComponent(1).setBounds(
                        spectrumLayeredPane.getWidth() - spectrumLayeredPane.getComponent(1).getWidth() - 20,
                        -5,
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
     * Update the related peptides and modified peptide PSMs tables.
     *
     * @param evt
     */
    private void peptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseReleased

        final MouseEvent finalEvt = evt;

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setTitle("Getting Peptides. Please Wait...");

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
            public void run() {

                relatedSelected = false;
                if (!progressDialog.isRunCanceled()) {
                    updateRelatedPeptidesTable(progressDialog);
                }
                if (!progressDialog.isRunCanceled()) {
                    updateSelectedPsmTable(progressDialog, true);
                }
                if (!progressDialog.isRunCanceled()) {
                    updateRelatedPsmTable(progressDialog, false);
                }

                if (!progressDialog.isRunCanceled()) {
                    updateModificationProfiles(progressDialog);
                }
                if (!progressDialog.isRunCanceled()) {
                    updateModificationProfilesTable(progressDialog);
                }

                if (!progressDialog.isRunCanceled()) {
                    newItemSelection();
                }

                if (finalEvt != null) {

                    int row = peptidesTable.rowAtPoint(finalEvt.getPoint());
                    int column = peptidesTable.columnAtPoint(finalEvt.getPoint());

                    if (row != -1 && !progressDialog.isRunCanceled()) {

                        peptidesTable.setRowSelectionInterval(row, row);

                        // open the protein inference at the petide level dialog
                        if (column == peptidesTable.getColumn("PI").getModelIndex()) {
                            progressDialog.setRunFinished();
                            try {
                                String peptideKey = getSelectedPeptide(false);
                                new ProteinInferencePeptideLevelDialog(peptideShakerGUI, true, peptideKey, null, peptideShakerGUI.getGeneMaps());
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                            }
                        } else if (column == peptidesTable.getColumn("PTM").getModelIndex()) {
                            if (peptidesTable.getValueAt(row, column) != null
                                    && ((Integer) peptidesTable.getValueAt(row, column)).intValue() != -1) {
                                progressDialog.setRunFinished();
                                new PtmSiteInferenceDialog(peptideShakerGUI, getSelectedPeptide(), ptmFactory.getPTM(getSelectedModification()));
                            }
                        } else if (column == peptidesTable.getColumn("   ").getModelIndex()) {
                            try {
                                String peptideKey = getSelectedPeptide(false);
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
                    }
                }

                progressDialog.setRunFinished();
            }
        }.start();
    }//GEN-LAST:event_peptidesTableMouseReleased

    /**
     * Update the related peptides PSM table.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseReleased

        final MouseEvent finalEvt = evt;

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Getting Related Peptides. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }).start();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                relatedSelected = true;
                if (!progressDialog.isRunCanceled()) {
                    updateSelectedPsmTable(progressDialog, false);
                }
                if (!progressDialog.isRunCanceled()) {
                    updateRelatedPsmTable(progressDialog, true);
                }

                if (!progressDialog.isRunCanceled()) {
                    updateModificationProfiles(progressDialog);
                }
                if (!progressDialog.isRunCanceled()) {
                    updateModificationProfilesTable(progressDialog);
                }
                if (!progressDialog.isRunCanceled()) {
                    newItemSelection();
                }

                if (finalEvt != null && !progressDialog.isRunCanceled()) {

                    int row = relatedPeptidesTable.rowAtPoint(finalEvt.getPoint());
                    int column = relatedPeptidesTable.columnAtPoint(finalEvt.getPoint());

                    if (row != -1) {

                        relatedPeptidesTable.setRowSelectionInterval(row, row);

                        // open the protein inference at the petide level dialog
                        if (column == relatedPeptidesTable.getColumn("PI").getModelIndex()) {
                            try {
                                String peptideKey = getSelectedPeptide(true);
                                progressDialog.setRunFinished();
                                new ProteinInferencePeptideLevelDialog(peptideShakerGUI, true, peptideKey, null, peptideShakerGUI.getGeneMaps());
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                            }
                        } else if (column == relatedPeptidesTable.getColumn("PTM").getModelIndex()) {
                            if (relatedPeptidesTable.getValueAt(row, column) != null
                                    && ((Integer) relatedPeptidesTable.getValueAt(row, column)).intValue() != -1) {
                                progressDialog.setRunFinished();
                                new PtmSiteInferenceDialog(peptideShakerGUI, getSelectedPeptide(), ptmFactory.getPTM(getSelectedModification()));
                            }
                        } else if (column == peptidesTable.getColumn("   ").getModelIndex()) {
                            try {
                                String peptideKey = getSelectedPeptide(true);
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
                    }
                }

                progressDialog.setRunFinished();
            }
        });
    }//GEN-LAST:event_relatedPeptidesTableMouseReleased

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void selectedPsmsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectedPsmsTableMouseReleased

        if (evt != null) {

            int column = selectedPsmsTable.columnAtPoint(evt.getPoint());
            int row = selectedPsmsTable.rowAtPoint(evt.getPoint());

            // star/unstar a psm
            if (column == selectedPsmsTable.getColumn("   ").getModelIndex()) {
                try {
                    PeptideMatch peptideMatch = identification.getPeptideMatch(getSelectedPeptide(false));
                    String psmKey = peptideMatch.getSpectrumMatchesKeys().get(selectedPsmsTable.convertRowIndexToModel(row));
                    PSParameter psParameter = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(psmKey, new PSParameter());
                    if (!psParameter.isStarred()) {
                        peptideShakerGUI.getStarHider().starPsm(psmKey, peptideShakerGUI.getSpectrumAnnotator());
                    } else {
                        peptideShakerGUI.getStarHider().unStarPsm(psmKey, peptideShakerGUI.getSpectrumAnnotator());
                    }
                    selectedPsmsTable.revalidate();
                    selectedPsmsTable.repaint();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        try {
            relatedSelected = false;
            updateGraphics(null);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        // update the annotation menu
        spectrumTabbedPaneStateChanged(null);

        newItemSelection();
    }//GEN-LAST:event_selectedPsmsTableMouseReleased

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void peptidesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseMoved
        int row = peptidesTable.rowAtPoint(evt.getPoint());
        int column = peptidesTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && peptidesTable.getValueAt(row, column) != null) {
            if (column == peptidesTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                try {
                    peptidesTable.setToolTipText(
                            peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(
                                    identification.getPeptideMatch(displayedPeptides.get((Integer) peptidesTable.getValueAt(row, 0) - 1))));
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    e.printStackTrace();
                }

            } else if (column == peptidesTable.getColumn("PI").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else if (column == peptidesTable.getColumn("PTM").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                peptidesTable.setToolTipText(null);
            }
        } else {
            peptidesTable.setToolTipText(null);
        }
    }//GEN-LAST:event_peptidesTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptidesTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesTableMouseExited

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_relatedPeptidesTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an HTML
     * link. Or shows a tooltip with modification details is over the sequence
     * column.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseMoved
        int row = relatedPeptidesTable.rowAtPoint(evt.getPoint());
        int column = relatedPeptidesTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && relatedPeptidesTable.getValueAt(row, column) != null) {

            if (column == relatedPeptidesTable.getColumn("Sequence").getModelIndex()) {

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                try {
                    relatedPeptidesTable.setToolTipText(
                            peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(
                                    identification.getPeptideMatch(relatedPeptides.get((Integer) relatedPeptidesTable.getValueAt(row, 0) - 1))));
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    e.printStackTrace();
                }

            } else if (column == relatedPeptidesTable.getColumn("PI").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else if (column == relatedPeptidesTable.getColumn("PTM").getModelIndex()) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                relatedPeptidesTable.setToolTipText(null);
            }
        } else {
            relatedPeptidesTable.setToolTipText(null);
        }
    }//GEN-LAST:event_relatedPeptidesTableMouseMoved

    /**
     * Update the peptide table or opens a color chooser if the color column is
     * clicked.
     *
     * @param evt
     */
    private void ptmJTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmJTableMouseReleased

        final MouseEvent finalEvt = evt;
        final JPanel finalRef = this;

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setTitle("Getting Modifications. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {

                int row = ptmJTable.rowAtPoint(finalEvt.getPoint());
                int column = ptmJTable.columnAtPoint(finalEvt.getPoint());

                if (row != -1 && column == ptmJTable.getColumn("  ").getModelIndex()) {

                    if (row != currentPtmRow) {
                        updatePeptideTable(progressDialog);
                    }

                    Color newColor = JColorChooser.showDialog(finalRef, "Pick a Color", (Color) ptmJTable.getValueAt(row, column));

                    if (newColor != null) {

                        // update the color in the table
                        ptmJTable.setValueAt(newColor, row, column);

                        // update the profiles with the new colors
                        if (!((String) ptmJTable.getValueAt(row, ptmJTable.getColumn("PTM").getModelIndex())).equalsIgnoreCase("no modification")) {
                            peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPtmSettings().setColor(
                                    (String) ptmJTable.getValueAt(row, ptmJTable.getColumn("  ").getModelIndex()), newColor);
                            peptideShakerGUI.updatePtmColorCoding();
                        }
                    }
                } else {
                    updatePeptideTable(progressDialog);
                }

                currentPtmRow = row;

                progressDialog.setRunFinished();
                newItemSelection();
            }
        }).start();
    }//GEN-LAST:event_ptmJTableMouseReleased

    /**
     * Update the peptide table.
     *
     * @param evt
     */
    private void ptmJTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_ptmJTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            progressDialog = new ProgressDialogX(peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Getting Peptides. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }).start();

            new Thread("DisplayThread") {
                @Override
                public void run() {

                    updatePeptideTable(progressDialog);
                    progressDialog.setRunFinished();
                    newItemSelection();
                }
            }.start();
        }
    }//GEN-LAST:event_ptmJTableKeyReleased

    /**
     * Updates the slider value when the user scrolls.
     *
     * @param evt
     */
    private void intensitySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_intensitySliderMouseWheelMoved
        spectrumAndFragmentIonJPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_intensitySliderMouseWheelMoved

    /**
     * Updates the intensity annotation limit.
     *
     * @param evt
     */
    private void intensitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_intensitySliderStateChanged
        peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences().setIntensityLimit(((Integer) intensitySlider.getValue()) / 100.0);
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
    private void spectrumAndFragmentIonJPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_spectrumAndFragmentIonJPanelMouseWheelMoved

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
    }//GEN-LAST:event_spectrumAndFragmentIonJPanelMouseWheelMoved

    /**
     * Changes the cursor to a hand cursor if over the color column.
     *
     * @param evt
     */
    private void ptmJTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmJTableMouseMoved
        int row = ptmJTable.rowAtPoint(evt.getPoint());
        int column = ptmJTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column == ptmJTable.getColumn("  ").getModelIndex()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_ptmJTableMouseMoved

    /**
     * Changes the cursor back to the default cursor.
     *
     * @param evt
     */
    private void ptmJTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmJTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ptmJTableMouseExited

    /**
     * See if we ought to show a tooltip with modification details for the
     * sequences column.
     *
     * @param evt
     */
    private void selectedPsmsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectedPsmsTableMouseMoved

        int row = selectedPsmsTable.rowAtPoint(evt.getPoint());
        int column = selectedPsmsTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && selectedPsmsTable.getValueAt(row, column) != null) {

            if (column == selectedPsmsTable.getColumn("Sequence").getModelIndex()) {

                try {
                    String spectrumKey = identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatchesKeys().get(row);
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    selectedPsmsTable.setToolTipText(peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(spectrumMatch));
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    e.printStackTrace();
                }

//            } else if (column == selectedPsmTable.getColumn("PTM").getModelIndex()) {
//                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                selectedPsmsTable.setToolTipText(null);
            }
        } else {
            selectedPsmsTable.setToolTipText(null);
        }
    }//GEN-LAST:event_selectedPsmsTableMouseMoved

    /**
     * Update the fragment ion annotation accuracy.
     *
     * @param evt
     */
    private void accuracySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_accuracySliderStateChanged
        SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
        double accuracy = (accuracySlider.getValue() / 100.0) * searchParameters.getFragmentIonAccuracy();
        peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences().setFragmentIonAccuracy(accuracy);
        peptideShakerGUI.updateSpectrumAnnotations();
        peptideShakerGUI.setDataSaved(false);
        accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " " + searchParameters.getFragmentAccuracyType());
        updateSpectrumSliderToolTip();
    }//GEN-LAST:event_accuracySliderStateChanged

    /**
     * Updates the slider value when the user scrolls.
     *
     * @param evt
     */
    private void accuracySliderMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_accuracySliderMouseWheelMoved
        spectrumAndFragmentIonJPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_accuracySliderMouseWheelMoved

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void relatedPsmsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPsmsTableMouseReleased

        int column = relatedPsmsTable.columnAtPoint(evt.getPoint());
        int row = relatedPsmsTable.rowAtPoint(evt.getPoint());

        // star/unstar a psm
        if (column == relatedPsmsTable.getColumn("   ").getModelIndex()) {
            try {
                PeptideMatch peptideMatch = identification.getPeptideMatch(getSelectedPeptide(true));
                String psmKey = peptideMatch.getSpectrumMatchesKeys().get(relatedPsmsTable.convertRowIndexToModel(row));
                PSParameter psParameter = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(psmKey, new PSParameter());
                if (!psParameter.isStarred()) {
                    peptideShakerGUI.getStarHider().starPsm(psmKey, peptideShakerGUI.getSpectrumAnnotator());
                } else {
                    peptideShakerGUI.getStarHider().unStarPsm(psmKey, peptideShakerGUI.getSpectrumAnnotator());
                }
                relatedPsmsTable.revalidate();
                relatedPsmsTable.repaint();
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        try {
            relatedSelected = true;
            updateGraphics(null);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        // update the annotation menu
        spectrumTabbedPaneStateChanged(null);

        newItemSelection();
    }//GEN-LAST:event_relatedPsmsTableMouseReleased

    /**
     * See if we ought to show a tooltip with modification details for the
     * sequence column.
     *
     * @param evt
     */
    private void relatedPsmsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPsmsTableMouseMoved
        int row = relatedPsmsTable.rowAtPoint(evt.getPoint());
        int column = relatedPsmsTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && relatedPsmsTable.getValueAt(row, column) != null) {
            if (column == relatedPsmsTable.getColumn("Sequence").getModelIndex()) {
                try {
                    String spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatchesKeys().get(row);
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    relatedPsmsTable.setToolTipText(peptideShakerGUI.getDisplayFeaturesGenerator().getPeptideModificationTooltipAsHtml(spectrumMatch));
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    e.printStackTrace();
                }

//            } else if (column == relatedPsmsTable.getColumn("PTM").getModelIndex()) {
//                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                relatedPsmsTable.setToolTipText(null);
            }
        } else {
            relatedPsmsTable.setToolTipText(null);
        }
    }//GEN-LAST:event_relatedPsmsTableMouseMoved

    /**
     * Update the spectra.
     *
     * @param evt
     */
    private void relatedPsmsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_relatedPsmsTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            relatedPsmsTableMouseReleased(null);
        }
    }//GEN-LAST:event_relatedPsmsTableKeyReleased

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void modificationProfileHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modificationProfileHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#Peptides",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_modificationProfileHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void modificationProfileHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationProfileHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_modificationProfileHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void modificationProfileHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modificationProfileHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_modificationProfileHelpJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportModifiedPeptideProfileJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportModifiedPeptideProfileJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportModifiedPeptideProfileJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportModifiedPeptideProfileJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportModifiedPeptideProfileJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportModifiedPeptideProfileJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void relatedProfileHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedProfileHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_relatedProfileHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void relatedProfileHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedProfileHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_relatedProfileHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void relatedProfileHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_relatedProfileHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#RelatedPeptides",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_relatedProfileHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportRelatedPeptideProfileJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportRelatedPeptideProfileJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportRelatedPeptideProfileJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportRelatedPeptideProfileJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportRelatedPeptideProfileJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportRelatedPeptideProfileJButtonMouseExited

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void ptmSelectionHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmSelectionHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_ptmSelectionHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void ptmSelectionHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ptmSelectionHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ptmSelectionHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void ptmSelectionHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ptmSelectionHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#Selection",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ptmSelectionHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportModifiedPsmsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportModifiedPsmsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportModifiedPsmsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportModifiedPsmsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportModifiedPsmsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportModifiedPsmsJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportModifiedPsmsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportModifiedPsmsJButtonActionPerformed
        copyTableContentToFileOrClipboard(TableIndex.MODIFIED_PSMS_TABLE);
    }//GEN-LAST:event_exportModifiedPsmsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void modifiedPsmsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modifiedPsmsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_modifiedPsmsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void modifiedPsmsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_modifiedPsmsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_modifiedPsmsHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void modifiedPsmsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modifiedPsmsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#PSMs",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_modifiedPsmsHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportRelatedPsmsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportRelatedPsmsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportRelatedPsmsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportRelatedPsmsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportRelatedPsmsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportRelatedPsmsJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportRelatedPsmsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRelatedPsmsJButtonActionPerformed
        copyTableContentToFileOrClipboard(TableIndex.RELATED_PSMS_TABLE);
    }//GEN-LAST:event_exportRelatedPsmsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void relatedPsmsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPsmsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_relatedPsmsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void relatedPsmsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPsmsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_relatedPsmsHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void relatedPsmsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_relatedPsmsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#RelatedPSMs",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_relatedPsmsHelpJButtonActionPerformed

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

        int spectrumTabIndex = spectrumTabbedPane.getSelectedIndex();

        if (spectrumTabIndex == 0) {
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#AScore",
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "PeptideShaker - Help");
        } else if (spectrumTabIndex == 1) {
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#DeltsScore",
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "PeptideShaker - Help");
        } else if (spectrumTabIndex == 2) {
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#Spectrum",
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "PeptideShaker - Help");
        }

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_spectrumHelpJButtonActionPerformed

    /**
     * Export the spectrum to mgf or figure format.
     *
     * @param evt
     */
    private void exportSpectrumJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSpectrumJButtonMouseReleased

        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Spectrum");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideShakerGUI.exportSpectrumAsFigure();
            }
        });

        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Spectrum as MGF");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    peptideShakerGUI.exportSelectedSpectraAsMgf();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }
        });

        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Spectrum Annotation");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    peptideShakerGUI.exportAnnotatedSpectrum();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }
        });

        popupMenu.add(menuItem);

        int index = spectrumTabbedPane.getSelectedIndex();

        if (index == 0) { // a-scores table

            menuItem = new JMenuItem("Table to Clipboard");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    copyTableContentToFileOrClipboard(TableIndex.A_SCORES_TABLE);
                }
            });

            popupMenu.add(new JSeparator());
            popupMenu.add(menuItem);

        } else if (index == 1) { // delta scores table

            menuItem = new JMenuItem("Table to Clipboard");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    copyTableContentToFileOrClipboard(TableIndex.DELTA_SCORES_TABLE);
                }
            });

            popupMenu.add(new JSeparator());
            popupMenu.add(menuItem);
        }

        popupMenu.show(exportSpectrumJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportSpectrumJButtonMouseReleased

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportRelatedPeptideProfileJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportRelatedPeptideProfileJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Table to File");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyTableContentToFileOrClipboard(TableIndex.RELATED_PEPTIDES_TABLE);
            }
        });

        popupMenu.add(menuItem);

        if (modificationProfileRelatedPeptideJPanel.getComponentCount() == 2) {

            menuItem = new JMenuItem("Modification Profile Plot");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, modificationProfileRelatedPeptideJPanel.getComponent(1), peptideShakerGUI.getLastSelectedFolder());
                }
            });

            popupMenu.add(menuItem);
        }

        popupMenu.show(exportRelatedPeptideProfileJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportRelatedPeptideProfileJButtonMouseReleased

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportModifiedPeptideProfileJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportModifiedPeptideProfileJButtonMouseReleased
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Table to File");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyTableContentToFileOrClipboard(TableIndex.MODIFIED_PEPTIDES_TABLE);
            }
        });

        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Modification Profile Plot");
        menuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, modificationProfileSelectedPeptideJPanel.getComponent(1), peptideShakerGUI.getLastSelectedFolder());
            }
        });

        popupMenu.add(menuItem);

        popupMenu.show(exportModifiedPeptideProfileJButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_exportModifiedPeptideProfileJButtonMouseReleased

    /**
     * Move the annotation menu bar.
     *
     * @param evt
     */
    private void spectrumTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spectrumTabbedPaneStateChanged
        if (peptideShakerGUI.getAnnotationMenuBar() != null) {
            int index = spectrumTabbedPane.getSelectedIndex();
            if (index == 2) {
                spectrumAnnotationMenuPanel.removeAll();
                spectrumAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
                peptideShakerGUI.updateAnnotationMenuBarVisableOptions(true, false, false, false,
                        ((selectedPsmsTable.getSelectedRow() != -1 && relatedPsmsTable.getSelectedRow() == -1)
                        || (selectedPsmsTable.getSelectedRow() == -1 && relatedPsmsTable.getSelectedRow() != -1)));
            }
        }
    }//GEN-LAST:event_spectrumTabbedPaneStateChanged
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel aScoreGradientPanel;
    private javax.swing.JLabel aScoreMaxValueJLabel;
    private javax.swing.JLabel aScoreMinValueJLabel;
    private javax.swing.JSlider accuracySlider;
    private javax.swing.JPanel contextMenuModPsmsBackgroundPanel;
    private javax.swing.JPanel contextMenuModifiedPeptidesBackgroundPanel;
    private javax.swing.JPanel contextMenuPtmBackgroundPanel;
    private javax.swing.JPanel contextMenuRelatedPeptidesBackgroundPanel;
    private javax.swing.JPanel contextMenuRelatedPsmsBackgroundPanel;
    private javax.swing.JPanel contextMenuSpectrumBackgroundPanel;
    private javax.swing.JPanel deltaScoreGradientPanel;
    private javax.swing.JLabel deltaScoreMaxValueJLabel;
    private javax.swing.JLabel deltaScoreMinValueJLabel;
    private javax.swing.JButton exportModifiedPeptideProfileJButton;
    private javax.swing.JButton exportModifiedPsmsJButton;
    private javax.swing.JButton exportRelatedPeptideProfileJButton;
    private javax.swing.JButton exportRelatedPsmsJButton;
    private javax.swing.JButton exportSpectrumJButton;
    private javax.swing.JSlider intensitySlider;
    private javax.swing.JPanel modPsmsPanel;
    private javax.swing.JButton modificationProfileHelpJButton;
    private javax.swing.JPanel modificationProfileRelatedPeptideJPanel;
    private javax.swing.JPanel modificationProfileSelectedPeptideJPanel;
    private javax.swing.JLayeredPane modifiedPeptidesLayeredPane;
    private javax.swing.JPanel modifiedPeptidesPanel;
    private javax.swing.JButton modifiedPsmsHelpJButton;
    private javax.swing.JPanel modsPsmsLayeredPanel;
    private javax.swing.JSplitPane peptideTablesJSplitPane;
    private javax.swing.JTable peptidesTable;
    private javax.swing.JScrollPane peptidesTableJScrollPane;
    private javax.swing.JPanel psmAScoresJPanel;
    private javax.swing.JScrollPane psmAScoresScrollPane;
    private javax.swing.JTable psmAScoresTable;
    private javax.swing.JPanel psmDeltaScoresJPanel;
    private javax.swing.JTable psmDeltaScoresTable;
    private javax.swing.JScrollPane psmDeltaScrollPane;
    private javax.swing.JSplitPane psmSpectraSplitPane;
    private javax.swing.JSplitPane psmSplitPane;
    private javax.swing.JLayeredPane psmsModPeptidesLayeredPane;
    private javax.swing.JScrollPane psmsModifiedTableJScrollPane;
    private javax.swing.JLayeredPane psmsRelatedPeptidesJLayeredPane;
    private javax.swing.JScrollPane psmsRelatedTableJScrollPane;
    private javax.swing.JPanel ptmAndPeptideSelectionPanel;
    private javax.swing.JScrollPane ptmJScrollPane;
    private javax.swing.JTable ptmJTable;
    private javax.swing.JLayeredPane ptmLayeredLayeredPane;
    private javax.swing.JPanel ptmLayeredPanel;
    private javax.swing.JPanel ptmPanel;
    private javax.swing.JButton ptmSelectionHelpJButton;
    private javax.swing.JPanel relatedPeptidesJPanel;
    private javax.swing.JSplitPane relatedPeptidesJSplitPane;
    private javax.swing.JPanel relatedPeptidesPanel;
    private javax.swing.JTable relatedPeptidesTable;
    private javax.swing.JScrollPane relatedPeptidesTableJScrollPane;
    private javax.swing.JLayeredPane relatedPeptiesLayeredPane;
    private javax.swing.JButton relatedProfileHelpJButton;
    private javax.swing.JButton relatedPsmsHelpJButton;
    private javax.swing.JPanel relatedPsmsJPanel;
    private javax.swing.JPanel relatedPsmsPanel;
    private javax.swing.JTable relatedPsmsTable;
    private javax.swing.JPanel selectedPeptidesJPanel;
    private javax.swing.JSplitPane selectedPeptidesJSplitPane;
    private javax.swing.JTable selectedPsmsTable;
    private javax.swing.JPanel sliderPanel;
    private javax.swing.JSplitPane slidersSplitPane;
    private javax.swing.JPanel spectrumAndFragmentIonJPanel;
    private javax.swing.JPanel spectrumAndFragmentIonPanel;
    private javax.swing.JPanel spectrumAnnotationMenuPanel;
    private javax.swing.JPanel spectrumChartJPanel;
    private javax.swing.JButton spectrumHelpJButton;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JLayeredPane spectrumLayeredPane;
    private javax.swing.JTabbedPane spectrumTabbedPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns a list of the keys of the proteins of the currently displayed
     * peptides.
     *
     * @return a list of the keys of the proteins of the currently displayed
     * peptides
     */
    public ArrayList<String> getDisplayedProteinMatches() {
        ArrayList<String> result = new ArrayList<String>();
        try {
            for (String peptideKey : getDisplayedPeptides()) {
                ArrayList<String> proteins = identification.getPeptideMatch(peptideKey).getTheoreticPeptide().getParentProteins(peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());
                for (String protein : proteins) {
                    for (String proteinMatchKey : identification.getProteinMap().get(protein)) {
                        if (!result.contains(proteinMatchKey) && identification.matchExists(proteinMatchKey)) {
                            result.add(proteinMatchKey);
                        }
                    }
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        return result;
    }

    /**
     * Returns a list of the keys of the currently displayed peptides.
     *
     * @return a list of the keys of the currently displayed peptides
     */
    public ArrayList<String> getDisplayedPeptides() {
        ArrayList<String> result = new ArrayList<String>(displayedPeptides);
        result.addAll(relatedPeptides);
        return result;
    }

    /**
     * Returns a list of the PSM keys of the currently displayed assumptions.
     *
     * @return a list of the PSM keys of the currently displayed assumptions
     */
    public ArrayList<String> getDisplayedPsms() {
        ArrayList<String> result = new ArrayList<String>();
        try {
            for (String peptide : displayedPeptides) {
                result.addAll(identification.getPeptideMatch(peptide).getSpectrumMatchesKeys());
            }
            for (String peptide : relatedPeptides) {
                result.addAll(identification.getPeptideMatch(peptide).getSpectrumMatchesKeys());
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        return result;
    }

    /**
     * Displays or hide sparklines in tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        ((JSparklinesBarChartTableCellRenderer) peptidesTable.getColumn("Peptide").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Peptide").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) selectedPsmsTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) relatedPsmsTable.getColumn("Charge").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showNumbers(!showSparkLines);

        // set the cell renderers
        updatePsmScoresCellRenderers();

        psmAScoresTable.revalidate();
        psmAScoresTable.repaint();

        psmDeltaScoresTable.revalidate();
        psmDeltaScoresTable.repaint();

        peptidesTable.revalidate();
        peptidesTable.repaint();

        relatedPeptidesTable.revalidate();
        relatedPeptidesTable.repaint();

        selectedPsmsTable.revalidate();
        selectedPsmsTable.repaint();

        relatedPsmsTable.revalidate();
        relatedPsmsTable.repaint();
    }

    /**
     * Creates the peptide map.
     *
     * @param progressDialog a progress dialog. Can be null.
     */
    private void createPeptideMap(ProgressDialogX progressDialogX) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ArrayList<String> notModifiedPeptides = new ArrayList<String>();
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(null, false, null, progressDialogX);

        while (peptideMatchesIterator.hasNext()) {

            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();
            Peptide peptide = peptideMatch.getTheoreticPeptide();

            boolean modified = false;
            ArrayList<String> accountedModifications = new ArrayList<String>();

            if (peptide.isModified()) {
                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {

                    String ptmName = modificationMatch.getTheoreticPtm();

                    if (!accountedModifications.contains(ptmName)) {

                        ArrayList<String> peptideKeysForPtm = peptideMap.get(ptmName);
                        if (peptideKeysForPtm == null) {
                            peptideKeysForPtm = new ArrayList<String>();
                            peptideMap.put(ptmName, peptideKeysForPtm);
                        }

                        peptideKeysForPtm.add(peptideKey);

                        modified = true;
                        accountedModifications.add(ptmName);
                    }
                }
            }

            if (!modified) {
                notModifiedPeptides.add(peptideKey);
            }
            if (progressDialogX != null) {
                if (progressDialogX.isRunCanceled()) {
                    return;
                }
                progressDialogX.increasePrimaryProgressCounter();
            }
        }

        peptideMap.put(NO_MODIFICATION, notModifiedPeptides);
    }

    /**
     * Returns the selected PTM name.
     *
     * @return the selected PTM name
     */
    public String getSelectedModification() {
        return (String) ptmJTable.getValueAt(ptmJTable.getSelectedRow(), ptmJTable.getColumn("PTM").getModelIndex());
    }

    /**
     * Displays the results.
     */
    public void displayResults() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Updating Data. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }).start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                try {
                    // now we have date so we can update the RT cell renderers max and min values
                    selectedPsmsTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, SpectrumFactory.getInstance().getMinRT(),
                            SpectrumFactory.getInstance().getMaxRT(), SpectrumFactory.getInstance().getMaxRT() / 50, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
                    ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);
                    ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
                    relatedPsmsTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, SpectrumFactory.getInstance().getMinRT(),
                            SpectrumFactory.getInstance().getMaxRT(), SpectrumFactory.getInstance().getMaxRT() / 50, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
                    ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth() + 5);
                    ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);

                    identification = peptideShakerGUI.getIdentification();
                    createPeptideMap(progressDialog);

                    DefaultTableModel dm = (DefaultTableModel) ptmJTable.getModel();
                    dm.getDataVector().removeAllElements();
                    dm.fireTableDataChanged();

                    PtmSettings modificationProfile = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPtmSettings();

                    for (String modification : modificationProfile.getAllNotFixedModifications()) {
                        if (!modification.equalsIgnoreCase(NO_MODIFICATION)) {
                            ((DefaultTableModel) ptmJTable.getModel()).addRow(new Object[]{modificationProfile.getColor(modification), modification});
                        }
                    }

                    ((DefaultTableModel) ptmJTable.getModel()).addRow(new Object[]{Color.lightGray, NO_MODIFICATION});

                    // update the slider tooltips
                    SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
                    double accuracy = (accuracySlider.getValue() / 100.0) * searchParameters.getFragmentIonAccuracy();
                    accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " " + searchParameters.getFragmentAccuracyType());
                    intensitySlider.setToolTipText("Annotation Level: " + intensitySlider.getValue() + "%");

                    ((JSparklinesBarChartTableCellRenderer) selectedPsmsTable.getColumn("Charge").getCellRenderer()).setMaxValue(
                            (double) ((PSMaps) peptideShakerGUI.getIdentification().getUrParam(new PSMaps())).getPsmSpecificMap().getMaxCharge());
                    ((JSparklinesBarChartTableCellRenderer) relatedPsmsTable.getColumn("Charge").getCellRenderer()).setMaxValue(
                            (double) ((PSMaps) peptideShakerGUI.getIdentification().getUrParam(new PSMaps())).getPsmSpecificMap().getMaxCharge());

                    // enable the contextual export options
                    exportModifiedPeptideProfileJButton.setEnabled(true);
                    exportRelatedPeptideProfileJButton.setEnabled(true);
                    exportSpectrumJButton.setEnabled(true);
                    exportModifiedPsmsJButton.setEnabled(true);
                    exportRelatedPsmsJButton.setEnabled(true);

                    selectedPeptidesJSplitPane.setDividerLocation(0.5);
                    relatedPeptidesJSplitPane.setDividerLocation(0.5);

                    peptideShakerGUI.setUpdated(PeptideShakerGUI.MODIFICATIONS_TAB_INDEX, true);

                    if (currentPtmRow != -1) {
                        updatePeptideTable(progressDialog);
                    }

                    progressDialog.setRunFinished();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }
        }
        );
    }

    /**
     * Tries to find the last selected peptide.
     */
    private void updateSelection(ProgressDialogX progressDialog) {

        // @TODO: we need to move to the correct ptm type in the ptmJTable before updating the selection!!
        // @TODO: if the given peptide has more than one ptm -> the user must choose the ptm to display 
        String selectedKey = peptideShakerGUI.getSelectedPeptideKey();

        if (selectedKey.equals(PeptideShakerGUI.NO_SELECTION)
                && !peptideShakerGUI.getSelectedPsmKey().equals(PeptideShakerGUI.NO_SELECTION)) {
            String psmKey = peptideShakerGUI.getSelectedPsmKey();
            if (peptideShakerGUI.getIdentification().matchExists(psmKey)) {
                try {
                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(psmKey);
                    if (spectrumMatch.getBestPeptideAssumption() != null) {
                        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                        selectedKey = peptide.getMatchingKey(peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences());
                    }
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    return;
                }
            }
        }

        if (!selectedKey.equals(PeptideShakerGUI.NO_SELECTION)) {

            // @TODO: the selection should not be updated if it's the same as the current one, e.g, when moving back and forth between tabs
            try {
                int row = 0;
                for (String displayedPeptide : displayedPeptides) {
                    if (displayedPeptide.equals(selectedKey)) {
                        peptidesTable.setRowSelectionInterval(row, row);
                        peptidesTable.scrollRectToVisible(peptidesTable.getCellRect(row, 0, false));
                        relatedSelected = false;
                        updateRelatedPeptidesTable(progressDialog);
                        updateSelectedPsmTable(progressDialog, true);
                        updateRelatedPsmTable(progressDialog, false);
                        updateModificationProfiles(progressDialog);
                        updateModificationProfilesTable(progressDialog);
                        if (relatedPeptidesTable.getSelectedRow() >= 0) {
                            relatedPeptidesTable.removeRowSelectionInterval(relatedPeptidesTable.getSelectedRow(), relatedPeptidesTable.getSelectedRow());
                        }
                        row = 0;
                        selectedKey = peptideShakerGUI.getSelectedPsmKey();
                        PeptideMatch peptideMatch = identification.getPeptideMatch(getSelectedPeptide(false));
                        for (String displayedPsm : peptideMatch.getSpectrumMatchesKeys()) {
                            if (displayedPsm.equals(selectedKey)) {
                                selectedPsmsTable.setRowSelectionInterval(row, row);
                                selectedPsmsTable.scrollRectToVisible(selectedPsmsTable.getCellRect(row, 0, false));

                                while (relatedPsmsTable.getSelectedRow() >= 0) {
                                    relatedPsmsTable.removeRowSelectionInterval(relatedPsmsTable.getSelectedRow(), relatedPsmsTable.getSelectedRow());
                                }
                                selectedPsmsTableMouseReleased(null);
                                return;
                            }
                            row++;
                        }
                        selectedPsmsTable.setRowSelectionInterval(0, 0);
                        selectedPsmsTable.scrollRectToVisible(selectedPsmsTable.getCellRect(0, 0, false));
                        while (relatedPsmsTable.getSelectedRow() >= 0) {
                            relatedPsmsTable.removeRowSelectionInterval(relatedPsmsTable.getSelectedRow(), selectedPsmsTable.getSelectedRow());
                        }
                        selectedPsmsTableMouseReleased(null);
                        return;
                    }
                    row++;
                }
                row = 0;
                for (String displayedPeptide : relatedPeptides) {
                    if (displayedPeptide.equals(selectedKey)) {
                        relatedPeptidesTable.setRowSelectionInterval(row, row);
                        relatedPeptidesTable.scrollRectToVisible(relatedPeptidesTable.getCellRect(row, 0, false));
                        if (peptidesTable.getSelectedRow() >= 0) {
                            peptidesTable.removeRowSelectionInterval(peptidesTable.getSelectedRow(), peptidesTable.getSelectedRow());
                        }
                        relatedSelected = true;
                        updateSelectedPsmTable(progressDialog, false);
                        updateRelatedPsmTable(progressDialog, true);
                        updateModificationProfiles(progressDialog);
                        updateModificationProfilesTable(progressDialog);
                        row = 0;
                        selectedKey = peptideShakerGUI.getSelectedPsmKey();
                        PeptideMatch peptideMatch = identification.getPeptideMatch(getSelectedPeptide(false));
                        for (String displayedPsm : peptideMatch.getSpectrumMatchesKeys()) {
                            if (displayedPsm.equals(selectedKey)) {
                                relatedPsmsTable.setRowSelectionInterval(row, row);
                                relatedPsmsTable.scrollRectToVisible(relatedPsmsTable.getCellRect(row, 0, false));
                                while (selectedPsmsTable.getSelectedRow() >= 0) {
                                    selectedPsmsTable.removeRowSelectionInterval(selectedPsmsTable.getSelectedRow(), selectedPsmsTable.getSelectedRow());
                                }
                                relatedPsmsTableMouseReleased(null);
                                return;
                            }
                            row++;
                        }
                        relatedPsmsTable.setRowSelectionInterval(0, 0);
                        relatedPsmsTable.scrollRectToVisible(relatedPsmsTable.getCellRect(0, 0, false));
                        while (selectedPsmsTable.getSelectedRow() >= 0) {
                            selectedPsmsTable.removeRowSelectionInterval(selectedPsmsTable.getSelectedRow(), selectedPsmsTable.getSelectedRow());
                        }
                        relatedPsmsTableMouseReleased(null);
                        return;
                    }
                    row++;
                }

            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Tries to find the last selected peptide.
     */
    public void updateSelection() {

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Updating Selection. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }).start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateSelection(progressDialog);
                progressDialog.setRunFinished();
            }
        });
    }

    /**
     * Updates the peptide table.
     *
     * @param progressDialog the progress dialog
     */
    public void updatePeptideTable(ProgressDialogX progressDialog) {

        if (ptmJTable.getSelectedRow() != -1) {

            progressDialog.setTitle("Getting Peptides. Please Wait...");

            // clear the spectrum
            spectrumChartJPanel.removeAll();
            spectrumChartJPanel.revalidate();
            spectrumChartJPanel.repaint();

            try {
                HashMap<Double, ArrayList<String>> scoreToPeptideMap = new HashMap<Double, ArrayList<String>>();
                PSParameter probabilities = new PSParameter();

                if (peptideMap.get((String) ptmJTable.getValueAt(ptmJTable.getSelectedRow(), ptmJTable.getColumn("PTM").getModelIndex())) != null) {

                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                    progressDialog.setValue(0);
                    progressDialog.setMaxPrimaryProgressCounter(peptideMap.get((String) ptmJTable.getValueAt(ptmJTable.getSelectedRow(), ptmJTable.getColumn("PTM").getModelIndex())).size()); // @TOOD: null point if no peptides with the given ptm is found!

                    for (String peptideKey : peptideMap.get((String) ptmJTable.getValueAt(ptmJTable.getSelectedRow(), ptmJTable.getColumn("PTM").getModelIndex()))) {

                        progressDialog.increasePrimaryProgressCounter();

                        if (progressDialog.isRunCanceled()) {
                            break;
                        }

                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

                        if (!peptideMatch.getTheoreticPeptide().isDecoy(peptideShakerGUI.getIdentificationParameters().getSequenceMatchingPreferences())) {

                            probabilities = (PSParameter) identification.getPeptideMatchParameter(peptideKey, probabilities);
                            double p = probabilities.getPeptideProbability();

                            if (!probabilities.isHidden()) {

                                if (!scoreToPeptideMap.containsKey(p)) {
                                    scoreToPeptideMap.put(p, new ArrayList<String>());
                                }

                                scoreToPeptideMap.get(p).add(peptideKey);
                            }
                        }
                    }

                    if (progressDialog.isRunCanceled()) {
                        return;
                    }

                    ArrayList<Double> scores = new ArrayList<Double>(scoreToPeptideMap.keySet());
                    Collections.sort(scores);
                    displayedPeptides = new ArrayList<String>();

                    ArrayList<String> tempList;

                    for (double score : scores) {

                        if (progressDialog.isRunCanceled()) {
                            break;
                        }

                        tempList = scoreToPeptideMap.get(score);
                        Collections.sort(tempList);
                        displayedPeptides.addAll(tempList);
                    }

                    if (progressDialog.isRunCanceled()) {
                        return;
                    }
                } else {
                    displayedPeptides = new ArrayList<String>();
                }

                ((DefaultTableModel) peptidesTable.getModel()).fireTableDataChanged();

                if (peptidesTable.getRowCount() > 0) {
                    peptidesTable.setRowSelectionInterval(0, 0);
                    peptidesTable.scrollRectToVisible(peptidesTable.getCellRect(0, 0, false));
                    updateRelatedPeptidesTable(progressDialog);
                    updateModificationProfiles(progressDialog);
                    updateModificationProfilesTable(progressDialog);
                } else {
                    modificationProfileSelectedPeptideJPanel.removeAll();
                    modificationProfileSelectedPeptideJPanel.revalidate();
                    modificationProfileSelectedPeptideJPanel.repaint();

                    modificationProfileRelatedPeptideJPanel.removeAll();
                    modificationProfileRelatedPeptideJPanel.revalidate();
                    modificationProfileRelatedPeptideJPanel.repaint();

                    relatedPeptides = new ArrayList<String>();
                    ((DefaultTableModel) relatedPeptidesTable.getModel()).fireTableDataChanged();
                }

                String selectedPTM = "";

                if (ptmJTable.getSelectedRow() != -1) {
                    selectedPTM = "- " + ptmJTable.getValueAt(ptmJTable.getSelectedRow(), ptmJTable.getColumn("PTM").getModelIndex()) + " ";
                }

                ((TitledBorder) selectedPeptidesJPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Modified Peptides "
                        + selectedPTM + "(" + peptidesTable.getRowCount() + ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                selectedPeptidesJPanel.repaint();

                ((TitledBorder) relatedPeptidesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Related Peptides ("
                        + relatedPeptidesTable.getRowCount() + ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                relatedPeptidesPanel.repaint();

                // set the preferred size of the sequence columns
                int peptideTableWidth = peptideShakerGUI.getPreferredColumnWidth(peptidesTable, peptidesTable.getColumn("Sequence").getModelIndex(), 1);
                int relatedPeptideWidth = peptideShakerGUI.getPreferredColumnWidth(relatedPeptidesTable, relatedPeptidesTable.getColumn("Sequence").getModelIndex(), 1);

                int width = Math.max(peptideTableWidth, relatedPeptideWidth);

                peptidesTable.getColumn("Sequence").setMinWidth(width);
                relatedPeptidesTable.getColumn("Sequence").setMinWidth(width);

                updateSelectedPsmTable(progressDialog, true);

                if (progressDialog.isRunCanceled()) {
                    return;
                }

                updateRelatedPsmTable(progressDialog, false);

                if (progressDialog.isRunCanceled()) {
                    return;
                }

                updateSelection(progressDialog);

            } catch (Exception e) {
                progressDialog.dispose();
                System.out.println("Exception when updating selected peptides table...");
                peptideShakerGUI.catchException(e);
            }
        }
    }

    /**
     * Updates the related peptides table.
     *
     * @param progressDialog a progress dialog
     */
    public void updateRelatedPeptidesTable(ProgressDialogX progressDialog) {

        HashMap<Double, ArrayList<String>> scoreToKeyMap = new HashMap<Double, ArrayList<String>>();
        String peptideKey = displayedPeptides.get((Integer) peptidesTable.getValueAt(peptidesTable.getSelectedRow(), 0) - 1);
        String currentSequence, referenceSequence = Peptide.getSequence(peptideKey);
        PSParameter probabilities = new PSParameter();

        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setValue(0);
        progressDialog.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size());

        for (String newKey : identification.getPeptideIdentification()) {

            if (progressDialog.isRunCanceled()) {
                break;
            }

            progressDialog.increasePrimaryProgressCounter();

            currentSequence = Peptide.getSequence(newKey);

            if (currentSequence.contains(referenceSequence) || referenceSequence.contains(currentSequence)) {

                if (!newKey.equals(peptideKey)) {
                    try {
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(newKey, probabilities);
                    } catch (Exception e) {
                        peptideShakerGUI.catchException(e);
                    }
                    double p = probabilities.getPeptideProbability();

                    if (!probabilities.isHidden()) {

                        if (!scoreToKeyMap.containsKey(p)) {
                            scoreToKeyMap.put(p, new ArrayList<String>());
                        }

                        scoreToKeyMap.get(p).add(newKey);
                    }
                }
            }
        }

        if (!progressDialog.isRunCanceled()) {

            progressDialog.setTitle("Sorting Related Peptides. Please Wait...");
            progressDialog.setPrimaryProgressCounterIndeterminate(true);

            relatedPeptides = new ArrayList<String>();
            ArrayList<Double> scores = new ArrayList<Double>(scoreToKeyMap.keySet());
            Collections.sort(scores);

            progressDialog.setPrimaryProgressCounterIndeterminate(false);
            progressDialog.setValue(0);
            progressDialog.setMaxPrimaryProgressCounter(scores.size());

            for (Double score : scores) {
                relatedPeptides.addAll(scoreToKeyMap.get(score));
                progressDialog.increasePrimaryProgressCounter();
            }

            ((DefaultTableModel) relatedPeptidesTable.getModel()).fireTableDataChanged();

            if (relatedPeptides.size() > 0) {
                relatedPeptidesTable.setRowSelectionInterval(0, 0);
                relatedPeptidesTable.scrollRectToVisible(relatedPeptidesTable.getCellRect(0, 0, false));
                updateModificationProfiles(progressDialog);
            } else {
                modificationProfileRelatedPeptideJPanel.removeAll();
            }

            // invoke later to give time for components to update
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // set the preferred size of the accession column
                    int peptideTableWidth = peptideShakerGUI.getPreferredColumnWidth(peptidesTable, peptidesTable.getColumn("Sequence").getModelIndex(), 1);
                    int relatedPeptideWidth = peptideShakerGUI.getPreferredColumnWidth(relatedPeptidesTable, relatedPeptidesTable.getColumn("Sequence").getModelIndex(), 1);

                    int width = Math.max(peptideTableWidth, relatedPeptideWidth);

                    peptidesTable.getColumn("Sequence").setMinWidth(width);
                    relatedPeptidesTable.getColumn("Sequence").setMinWidth(width);
                }
            });

            ((TitledBorder) relatedPeptidesPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Related Peptides ("
                    + relatedPeptidesTable.getRowCount() + ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
            relatedPeptidesPanel.repaint();
        }
    }

    /**
     * Update the selected peptides PSM table.
     *
     * @param selectRow if true, the first row in the table is selected
     */
    private void updateSelectedPsmTable(ProgressDialogX progressDialog, boolean selectRow) {

        ((DefaultTableModel) selectedPsmsTable.getModel()).fireTableDataChanged();
        ((DefaultTableModel) relatedPsmsTable.getModel()).fireTableDataChanged();

        if (selectedPsmsTable.getRowCount() > 0) {

            selectedPsmsTable.setRowSelectionInterval(0, 0);
            selectedPsmsTable.scrollRectToVisible(selectedPsmsTable.getCellRect(0, 0, false));

            try {
                if (selectRow) {
                    if (selectedPsmsTable.getRowCount() > 0) {
                        selectedPsmsTable.setRowSelectionInterval(0, 0);
                        selectedPsmsTable.scrollRectToVisible(selectedPsmsTable.getCellRect(0, 0, false));
                        updateGraphics(progressDialog);
                    }

                    while (relatedPsmsTable.getSelectedRow() >= 0) {
                        relatedPsmsTable.removeRowSelectionInterval(relatedPsmsTable.getSelectedRow(), relatedPsmsTable.getSelectedRow());
                    }
                } else {
                    while (selectedPsmsTable.getSelectedRow() >= 0) {
                        selectedPsmsTable.removeRowSelectionInterval(selectedPsmsTable.getSelectedRow(), selectedPsmsTable.getSelectedRow());
                    }
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                e.printStackTrace();
            }
        }

        // update the annotation menu
        spectrumTabbedPaneStateChanged(null);

        ((TitledBorder) modsPsmsLayeredPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide Spectrum Matches - Modified Peptide ("
                + selectedPsmsTable.getRowCount() + ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        modsPsmsLayeredPanel.repaint();
    }

    /**
     * Update the related peptides PSM table.
     *
     * @param selectRow if true, the first row in the table is selected
     */
    private void updateRelatedPsmTable(ProgressDialogX progressDialog, boolean selectRow) {

        ((DefaultTableModel) relatedPsmsTable.getModel()).fireTableDataChanged();

        try {
            if (selectRow) {
                if (relatedPsmsTable.getRowCount() > 0) {
                    relatedPsmsTable.setRowSelectionInterval(0, 0);
                    relatedPsmsTable.scrollRectToVisible(relatedPsmsTable.getCellRect(0, 0, false));
                    updateGraphics(progressDialog);
                }

                while (selectedPsmsTable.getSelectedRow() >= 0) {
                    selectedPsmsTable.removeRowSelectionInterval(selectedPsmsTable.getSelectedRow(), selectedPsmsTable.getSelectedRow());
                }
            } else {
                while (relatedPsmsTable.getSelectedRow() >= 0) {
                    relatedPsmsTable.removeRowSelectionInterval(relatedPsmsTable.getSelectedRow(), relatedPsmsTable.getSelectedRow());
                }
            }

            // update the RT column renderer
            double lowRT = Double.MAX_VALUE;
            double highRT = Double.MIN_VALUE;
            boolean retentionTimeValues = false;

            if (relatedPsmsTable.getRowCount() > 0) {
                progressDialog.setTitle("Updating Selected PSMs. Please Wait...");
                progressDialog.setPrimaryProgressCounterIndeterminate(false);
                progressDialog.setValue(0);
                progressDialog.setMaxPrimaryProgressCounter(relatedPsmsTable.getRowCount());
            }

            for (int i = 0; i < relatedPsmsTable.getRowCount(); i++) {

                progressDialog.increasePrimaryProgressCounter();

                String spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatchesKeys().get(i);
                Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey);

                if (precursor != null) {

                    double retentionTime = precursor.getRt();

                    if (!retentionTimeValues && retentionTime != -1) {
                        retentionTimeValues = true;
                    }

                    if (lowRT > retentionTime) {
                        lowRT = retentionTime;
                    }

                    if (highRT < retentionTime) {
                        highRT = retentionTime;
                    }
                }
            }

            if (retentionTimeValues) {
                JSparklinesIntervalChartTableCellRenderer rtCellRenderer = new JSparklinesIntervalChartTableCellRenderer(
                        PlotOrientation.HORIZONTAL, SpectrumFactory.getInstance().getMinRT(), SpectrumFactory.getInstance().getMaxRT(), SpectrumFactory.getInstance().getMaxRT() / 50,
                        peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor());
                relatedPsmsTable.getColumn("RT").setCellRenderer(rtCellRenderer);
                rtCellRenderer.showNumberAndChart(true, TableProperties.getLabelWidth() + 5);
                ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }

        // update the annotation menu
        spectrumTabbedPaneStateChanged(null);

        ((TitledBorder) relatedPsmsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide Spectrum Matches - Related Peptide ("
                + relatedPsmsTable.getRowCount() + ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        relatedPsmsPanel.repaint();
    }

    /**
     * Updates the graphics components.
     *
     * @param progressDialog the progress dialog
     */
    public void updateGraphics(ProgressDialogX progressDialog) {

        if (progressDialog != null) {
            progressDialog.setTitle("Updating Graphics. Please Wait...");
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
        }

        try {
            if (!getSelectedPeptide().equals("")) {
                if (selectedPsmsTable.getSelectedRow() != -1 && relatedPsmsTable.getSelectedRow() != -1) {
                    updateSpectrum(getSelectedPsmsKeys(false).get(0), getSelectedPsmsKeys(true).get(0));
                } else if (selectedPsmsTable.getSelectedRow() != -1 && relatedPsmsTable.getSelectedRow() == -1) {
                    updateSpectrum(getSelectedPsmsKeys(false).get(0), null);
                } else if (selectedPsmsTable.getSelectedRow() == -1 && relatedPsmsTable.getSelectedRow() != -1) {
                    updateSpectrum(getSelectedPsmsKeys(true).get(0), null);
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
    }

    /**
     * Update the spectra according to the currently selected PSM.
     *
     * @param spectrumKey the main spectrum key
     * @param secondSpectrumKey the secondary spectrum key
     */
    public void updateSpectrum(String spectrumKey, String secondSpectrumKey) {

        try {
            spectrumChartJPanel.removeAll();
            spectrumChartJPanel.revalidate();
            spectrumChartJPanel.repaint();

            AnnotationSettings annotationPreferences = peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences();
            MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

            if (currentSpectrum != null && currentSpectrum.getMzValuesAsArray().length > 0) {

                Precursor precursor = currentSpectrum.getPrecursor();
                SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);

                double[] intensityArray;

                if (secondSpectrumKey == null) {
                    intensityArray = currentSpectrum.getIntensityValuesAsArray();
                } else {
                    intensityArray = currentSpectrum.getIntensityValuesNormalizedAsArray();
                }

                spectrum = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), intensityArray,
                        precursor.getMz(), spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrum.setKnownMassDeltas(peptideShakerGUI.getCurrentMassDeltas());
                spectrum.setDeltaMassWindow(peptideShakerGUI.getIdentificationParameters().getAnnotationPreferences().getFragmentIonAccuracy());
                spectrum.setBorder(null);
                spectrum.setDataPointAndLineColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakColor(), 0);
                spectrum.setPeakWaterMarkColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakColor());
                spectrum.setPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakWidth());
                spectrum.setBackgroundPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakWidth());

                int identificationChargeFirstPsm = 0;
                int identificationChargeSecondPsm = 0;
                ArrayList<ModificationMatch> allModifications = new ArrayList<ModificationMatch>();

                // get the spectrum annotations
                PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                Peptide peptide = peptideAssumption.getPeptide();
                identificationChargeFirstPsm = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                if (peptide.isModified()) {
                    allModifications.addAll(peptide.getModificationMatches());
                }
                peptideShakerGUI.setSpecificAnnotationPreferences(new SpecificAnnotationSettings(spectrumKey, peptideAssumption));
                peptideShakerGUI.updateAnnotationPreferences();
                SpecificAnnotationSettings specificAnnotationPreferences = peptideShakerGUI.getSpecificAnnotationPreferences();
                ArrayList<IonMatch> annotations = peptideShakerGUI.getSpectrumAnnotator().getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, currentSpectrum, peptide);

                // add the spectrum annotations
                spectrum.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), !annotationPreferences.isHighResolutionAnnotation());
                spectrum.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                spectrum.setYAxisZoomExcludesBackgroundPeaks(annotationPreferences.yAxisZoomExcludesBackgroundPeaks());

                // add de novo sequencing
                SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
                int forwardIon = searchParameters.getIonSearched1();
                int rewindIon = searchParameters.getIonSearched2();

                spectrum.addAutomaticDeNovoSequencing(peptide, annotations,
                        forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                        annotationPreferences.showForwardIonDeNovoTags(),
                        annotationPreferences.showRewindIonDeNovoTags(), false);

                // see if a second mirrored spectrum is to be added
                if (secondSpectrumKey != null) {
                    currentSpectrum = peptideShakerGUI.getSpectrum(secondSpectrumKey);

                    if (currentSpectrum != null && currentSpectrum.getMzValuesAsArray().length > 0) {

                        precursor = currentSpectrum.getPrecursor();
                        spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(secondSpectrumKey);

                        spectrum.addMirroredSpectrum(
                                currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesNormalizedAsArray(), precursor.getMz(),
                                spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().toString(), "", false,
                                peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedMirroredPeakColor(),
                                peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedMirroredPeakColor());

                        // get the spectrum annotations
                        peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                        peptide = peptideAssumption.getPeptide();
                        identificationChargeSecondPsm = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                        if (peptide.isModified()) {
                            allModifications.addAll(peptide.getModificationMatches());
                        }
                        peptideShakerGUI.setSpecificAnnotationPreferences(new SpecificAnnotationSettings(spectrumKey, peptideAssumption));
                        peptideShakerGUI.updateAnnotationPreferences();
                        specificAnnotationPreferences = peptideShakerGUI.getSpecificAnnotationPreferences();
                        annotations = peptideShakerGUI.getSpectrumAnnotator().getSpectrumAnnotation(annotationPreferences, specificAnnotationPreferences, currentSpectrum, peptide);

                        spectrum.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(annotations));

                        // add de novo sequencing
                        spectrum.addAutomaticDeNovoSequencing(peptide, annotations,
                                forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(),
                                annotationPreferences.showForwardIonDeNovoTags(),
                                annotationPreferences.showRewindIonDeNovoTags(), true);

                        spectrum.rescale(0.0, spectrum.getMaxXAxisValue());
                    }
                }

                spectrumChartJPanel.add(spectrum);
                peptideShakerGUI.updateAnnotationMenus(Math.max(identificationChargeFirstPsm, identificationChargeSecondPsm), allModifications);

                String modifiedSequence = peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey), false, false, true);

                if (secondSpectrumKey != null) {
                    modifiedSequence += " vs. " + peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideShakerGUI.getIdentification().getSpectrumMatch(secondSpectrumKey), false, false, true);
                }

                ((TitledBorder) spectrumAndFragmentIonPanel.getBorder()).setTitle(
                        PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                        + "Spectrum & Fragment Ions ("
                        + modifiedSequence
                        + ")"
                        + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                spectrumAndFragmentIonPanel.revalidate();
                spectrumAndFragmentIonPanel.repaint();
            }

            spectrumChartJPanel.revalidate();
            spectrumChartJPanel.repaint();
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
    }

    /**
     * Returns the content of a Modification Profile cell for a desired peptide.
     *
     * @param peptide The sequence of the peptide
     * @param scores The PTM scores
     * @return The modification profile
     */
    private ArrayList<com.compomics.util.gui.protein.ModificationProfile> getModificationProfile(Peptide peptide, PSPtmScores scores) {
        ArrayList<com.compomics.util.gui.protein.ModificationProfile> profiles = new ArrayList<com.compomics.util.gui.protein.ModificationProfile>();
        if (scores != null) {
            for (String ptmName : scores.getScoredPTMs()) {
                Color ptmColor = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPtmSettings().getColor(ptmName);
                com.compomics.util.gui.protein.ModificationProfile tempProfile = new com.compomics.util.gui.protein.ModificationProfile(ptmName, new double[peptide.getSequence().length()][2], ptmColor);
                PtmScoring locationScoring = scores.getPtmScoring(ptmName);
                for (int aa = 1; aa <= peptide.getSequence().length(); aa++) {
                    tempProfile.getProfile()[aa - 1][com.compomics.util.gui.protein.ModificationProfile.SCORE_1_ROW_INDEX] = locationScoring.getDeltaScore(aa);
                    tempProfile.getProfile()[aa - 1][com.compomics.util.gui.protein.ModificationProfile.SCORE_2_ROW_INDEX] = locationScoring.getProbabilisticScore(aa);
                }

                profiles.add(tempProfile);
            }
        }
        return profiles;
    }

    /**
     * Returns the key of the selected peptide
     *
     * @param relatedPeptide if true, the related peptide table is used,
     * otherwise the selected peptide table is used
     * @return the key of the selected peptide
     */
    private String getSelectedPeptide(boolean relatedPeptide) {
        if (relatedPeptide) {
            if (relatedPeptides.isEmpty()) {
                return "";
            }
            int index = relatedPeptidesTable.getSelectedRow();
            if (index == -1) {
                index = 0;
            }
            return relatedPeptides.get((Integer) relatedPeptidesTable.getValueAt(index, 0) - 1);
        } else {
            if (displayedPeptides.isEmpty()) {
                return "";
            }
            int index = peptidesTable.getSelectedRow();
            if (index == -1) {
                index = 0;
            }
            return displayedPeptides.get((Integer) peptidesTable.getValueAt(index, 0) - 1);
        }
    }

    /**
     * Returns the key of the selected peptide.
     *
     * @return the key of the selected peptide
     */
    private String getSelectedPeptide() {
        return getSelectedPeptide(relatedSelected);
    }

    /**
     * Returns the keys of the selected PSMs.
     *
     * @param relatedPeptide if true, the related PSM table is used, otherwise
     * the selected PSM table is used
     *
     * @return the keys of the selected PSMs
     */
    private ArrayList<String> getSelectedPsmsKeys(boolean relatedPeptide) {
        ArrayList<String> psmKey = new ArrayList<String>();
        try {
            PeptideMatch peptideMatch = identification.getPeptideMatch(getSelectedPeptide(relatedPeptide));
            if (relatedPeptide) {
                int[] selectedRows = relatedPsmsTable.getSelectedRows();
                for (int row : selectedRows) {
                    psmKey.add(peptideMatch.getSpectrumMatchesKeys().get(row));
                }
            } else {
                int[] selectedRows = selectedPsmsTable.getSelectedRows();
                for (int row : selectedRows) {
                    psmKey.add(peptideMatch.getSpectrumMatchesKeys().get(row));
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        return psmKey;
    }

    /**
     * Returns the keys of the selected PSMs.
     *
     * @return the keys of the selected PSMs
     */
    public ArrayList<String> getSelectedPsmsKeys() {
        return getSelectedPsmsKeys(relatedSelected);
    }

    /**
     * Returns a map of the selected spectrum identification assumptions as a
     * map: spectrum key | assumption
     *
     * @return an ArrayList of the keys of the selected spectra in the PSM table
     *
     * @throws SQLException exception thrown whenever an error occurred while
     * loading the object from the database
     * @throws IOException exception thrown whenever an error occurred while
     * reading the object in the database
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while casting the database input in the desired match class
     * @throws InterruptedException thrown whenever a threading issue occurred
     * while interacting with the database
     */
    public HashMap<String, ArrayList<SpectrumIdentificationAssumption>> getSelectedIdentificationAssumptions() throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        HashMap<String, ArrayList<SpectrumIdentificationAssumption>> result = new HashMap<String, ArrayList<SpectrumIdentificationAssumption>>(2);
        for (String spectrumKey : getSelectedPsmsKeys(false)) {
            SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
            ArrayList<SpectrumIdentificationAssumption> assumptions = new ArrayList<SpectrumIdentificationAssumption>(1);
            assumptions.add(spectrumMatch.getBestPeptideAssumption());
            result.put(spectrumKey, assumptions);
        }
        for (String spectrumKey : getSelectedPsmsKeys(true)) {
            SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
            ArrayList<SpectrumIdentificationAssumption> assumptions = new ArrayList<SpectrumIdentificationAssumption>(1);
            assumptions.add(spectrumMatch.getBestPeptideAssumption());
            result.put(spectrumKey, assumptions);
        }
        return result;
    }

    /**
     * Table model for the peptide table.
     */
    private class PeptideTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return displayedPeptides.size();
        }

        @Override
        public int getColumnCount() {
            return 7;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "   ";
                case 2:
                    return "PI"; // @TODO: add proteins?
                case 3:
                    return "Sequence";
                case 4:
                    return "PTM";
                case 5:
                    return "Peptide";
                case 6:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                if (row >= displayedPeptides.size()) {
                    return "";
                }
                PSParameter probabilities;
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(displayedPeptides.get(row), probabilities);
                        return probabilities.isStarred();
                    case 2:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(displayedPeptides.get(row), probabilities);
                        return probabilities.getProteinInferenceClass();
                    case 3:
                        String peptideKey = displayedPeptides.get(row);
                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                        return peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideMatch, true, true, true);
                    case 4:
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getPeptideMatch(displayedPeptides.get(row)).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            PtmScoring ptmScoring = ptmScores.getPtmScoring(getSelectedModification());
                            return ptmScoring.getMinimalLocalizationConfidence();
                        } else {
                            return PtmScoring.NOT_FOUND;
                        }
                    case 5:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(displayedPeptides.get(row), probabilities);
                        return probabilities.getPeptideConfidence();
                    case 6:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(displayedPeptides.get(row), probabilities);
                        return probabilities.getMatchValidationLevel().getIndex();
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
     * Table model for the related peptides table.
     */
    private class RelatedPeptidesTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return relatedPeptides.size();
        }

        @Override
        public int getColumnCount() {
            return 7;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "   ";
                case 2:
                    return "PI";
                case 3:
                    return "Sequence";
                case 4:
                    return "PTM";
                case 5:
                    return "Peptide";
                case 6:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                PSParameter probabilities;

                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(relatedPeptides.get(row), probabilities);
                        return probabilities.isStarred();
                    case 2:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(relatedPeptides.get(row), probabilities);
                        return probabilities.getProteinInferenceClass();
                    case 3:
                        String peptideKey = relatedPeptides.get(row);
                        PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                        return peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(peptideMatch, true, true, true);
                    case 4:
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getPeptideMatch(relatedPeptides.get(row)).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            PtmScoring ptmScoring = ptmScores.getPtmScoring(getSelectedModification());
                            return ptmScoring.getMinimalLocalizationConfidence();
                        } else {
                            return PtmScoring.NOT_FOUND;
                        }
                    case 5:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(relatedPeptides.get(row), probabilities);
                        return probabilities.getPeptideConfidence();
                    case 6:
                        probabilities = new PSParameter();
                        probabilities = (PSParameter) identification.getPeptideMatchParameter(relatedPeptides.get(row), probabilities);
                        return probabilities.getMatchValidationLevel().getIndex();
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
     * Table model for the modified PSMs table.
     */
    private class PsmsTable extends DefaultTableModel {

        /**
         * Indicates whether the table should display the main peptide or the
         * related one.
         */
        private Boolean relatedTable;

        /**
         * Constructor.
         *
         * @param relatedTable Indicates whether the table should display the
         * main peptide or the related one.
         */
        public PsmsTable(boolean relatedTable) {
            this.relatedTable = relatedTable;
        }

        @Override
        public int getRowCount() {
            if (relatedTable == null) {
                return 0;
            }
            if (!relatedTable && peptidesTable.getSelectedRow() != -1 || relatedTable && relatedPeptidesTable.getSelectedRow() != -1) {
                try {
                    return identification.getPeptideMatch(getSelectedPeptide(relatedTable)).getSpectrumCount();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                    return 0;
                }
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 7;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "   ";
                case 2:
                    return "Sequence";
                case 3:
                    return "PTM";
                case 4:
                    return "Charge";
                case 5:
                    return "RT";
                case 6:
                    return "  ";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                String spectrumKey;
                PSParameter probabilities = new PSParameter();
                switch (column) {
                    case 0:
                        return row + 1;
                    case 1:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(relatedTable)).getSpectrumMatchesKeys().get(row);
                        probabilities = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, probabilities);
                        return probabilities.isStarred();
                    case 2:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(relatedTable)).getSpectrumMatchesKeys().get(row);
                        return peptideShakerGUI.getDisplayFeaturesGenerator().getTaggedPeptideSequence(identification.getSpectrumMatch(spectrumKey), true, true, true);
                    case 3:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(relatedTable)).getSpectrumMatchesKeys().get(row);
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getSpectrumMatch(spectrumKey).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            PtmScoring ptmScoring = ptmScores.getPtmScoring(getSelectedModification());
                            return ptmScoring.getMinimalLocalizationConfidence();
                        } else {
                            return PtmScoring.NOT_FOUND;
                        }
                    case 4:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(relatedTable)).getSpectrumMatchesKeys().get(row);
                        return identification.getSpectrumMatch(spectrumKey).getBestPeptideAssumption().getIdentificationCharge().value;
                    case 5:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(relatedTable)).getSpectrumMatchesKeys().get(row);
                        try {
                            Precursor precursor = SpectrumFactory.getInstance().getPrecursor(spectrumKey); // @TODO: there is sometimes an IOException when closing the tool...

                            if (precursor != null) {
                                return precursor.getRt();
                            } else {
                                return null;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    case 6:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(relatedTable)).getSpectrumMatchesKeys().get(row);
                        probabilities = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, probabilities);
                        return probabilities.getMatchValidationLevel().getIndex();
                    default:
                        return null;
                }
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                return null;
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
     * Returns the spectrum panel.
     *
     * @return the spectrum panel
     */
    public Component getSpectrum() {
        return (Component) spectrumJPanel.getComponent(1);
    }

    /**
     * Updates the modification profile to the currently selected peptide.
     *
     * @param peptideMatch the peptide match to create the profile for
     * @param selectedPeptideProfile if true the selected peptide profile is
     * updated, otherwise the related peptide profile is updated
     */
    private void updateModificationProfile(PeptideMatch peptideMatch, boolean selectedPeptideProfile) {

        try {
            PSPtmScores scores = new PSPtmScores();
            scores = (PSPtmScores) peptideMatch.getUrParam(scores);
            ArrayList<com.compomics.util.gui.protein.ModificationProfile> profiles = getModificationProfile(peptideMatch.getTheoreticPeptide(), scores);

            SequenceModificationPanel sequenceModificationPanel
                    = new SequenceModificationPanel(peptideMatch.getTheoreticPeptide().getNTerminal() + "-"
                            + peptideMatch.getTheoreticPeptide().getSequence()
                            + "-" + peptideMatch.getTheoreticPeptide().getCTerminal(),
                            profiles, true, "D-score",
                            peptideShakerGUI.getIdentificationParameters().getPtmScoringPreferences().getSelectedProbabilisticScore().getName());

            if (selectedPeptideProfile) {
                modificationProfileSelectedPeptideJPanel.removeAll();
                sequenceModificationPanel.setOpaque(true);
                sequenceModificationPanel.setBackground(Color.WHITE);
                sequenceModificationPanel.setMinimumSize(new Dimension(sequenceModificationPanel.getPreferredSize().width, sequenceModificationPanel.getHeight()));
                JPanel tempPanel = new JPanel();
                tempPanel.setBackground(Color.WHITE);
                tempPanel.setOpaque(true);
                modificationProfileSelectedPeptideJPanel.add(tempPanel);
                modificationProfileSelectedPeptideJPanel.add(sequenceModificationPanel);
                modificationProfileSelectedPeptideJPanel.revalidate();
                modificationProfileSelectedPeptideJPanel.repaint();
            } else {
                modificationProfileRelatedPeptideJPanel.removeAll();
                sequenceModificationPanel.setOpaque(true);
                sequenceModificationPanel.setBackground(Color.WHITE);
                sequenceModificationPanel.setMinimumSize(new Dimension(sequenceModificationPanel.getPreferredSize().width, sequenceModificationPanel.getHeight()));
                JPanel tempPanel = new JPanel();
                tempPanel.setBackground(Color.WHITE);
                tempPanel.setOpaque(true);
                modificationProfileRelatedPeptideJPanel.add(tempPanel);
                modificationProfileRelatedPeptideJPanel.add(sequenceModificationPanel);
                modificationProfileRelatedPeptideJPanel.revalidate();
                modificationProfileRelatedPeptideJPanel.repaint();
            }

            double selectedPeptideProfileWidth = 1 - (sequenceModificationPanel.getPreferredSize().getWidth() / selectedPeptidesJSplitPane.getSize().getWidth());
            double relatedPeptideProfileWidth = 1 - (sequenceModificationPanel.getPreferredSize().getWidth() / relatedPeptidesJSplitPane.getSize().getWidth());

            if (modificationProfileSelectedPeptideJPanel.getComponentCount() == 2) {
                selectedPeptideProfileWidth = 1 - (modificationProfileSelectedPeptideJPanel.getComponent(1).getPreferredSize().getWidth() / selectedPeptidesJSplitPane.getSize().getWidth());
            }

            if (modificationProfileRelatedPeptideJPanel.getComponentCount() == 2) {
                relatedPeptideProfileWidth = 1 - (modificationProfileRelatedPeptideJPanel.getComponent(1).getPreferredSize().getWidth() / relatedPeptidesJSplitPane.getSize().getWidth());
            }

            double splitterLocation = Math.min(selectedPeptideProfileWidth, relatedPeptideProfileWidth);

            selectedPeptidesJSplitPane.setDividerLocation(splitterLocation);
            selectedPeptidesJSplitPane.revalidate();
            selectedPeptidesJSplitPane.repaint();

            relatedPeptidesJSplitPane.setDividerLocation(splitterLocation);
            relatedPeptidesJSplitPane.revalidate();
            relatedPeptidesJSplitPane.repaint();

        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
    }

    /**
     * Makes sure that the annotation menu bar is visible.
     */
    public void showSpectrumAnnotationMenu() {
        spectrumTabbedPaneStateChanged(null);
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

        for (int i = 0; i < ptmJTable.getRowCount(); i++) {
            ptmJTable.setValueAt(peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPtmSettings().getColor(
                    (String) ptmJTable.getValueAt(i, ptmJTable.getColumn("PTM").getModelIndex())), i, ptmJTable.getColumn("  ").getModelIndex());
        }

        updateModificationProfiles(null);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Redraws the modification profiles. For example if the PTM colors are
     * updated.
     *
     * @param progressDialog the progress dialog
     */
    public void updateModificationProfiles(ProgressDialogX progressDialog) {

        if (progressDialog != null) {
            progressDialog.setTitle("Updating Modification Profile. Please Wait...");
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
        }

        try {
            if (peptidesTable.getSelectedRow() != -1) {
                updateModificationProfile(identification.getPeptideMatch(displayedPeptides.get((Integer) peptidesTable.getValueAt(peptidesTable.getSelectedRow(), 0) - 1)), true);
            }
            if (relatedPeptidesTable.getSelectedRow() != -1) {
                updateModificationProfile(identification.getPeptideMatch(relatedPeptides.get((Integer) relatedPeptidesTable.getValueAt(relatedPeptidesTable.getSelectedRow(), 0) - 1)), false);
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
    }

    /**
     * Export the table contents to the clipboard.
     *
     * @param index
     */
    private void copyTableContentToFileOrClipboard(TableIndex index) {

        final TableIndex tableIndex = index;

        if (tableIndex == TableIndex.MODIFIED_PEPTIDES_TABLE
                || tableIndex == TableIndex.RELATED_PEPTIDES_TABLE
                || tableIndex == TableIndex.MODIFIED_PSMS_TABLE
                || tableIndex == TableIndex.RELATED_PSMS_TABLE
                || tableIndex == TableIndex.PTM_TABLE
                || tableIndex == TableIndex.A_SCORES_TABLE
                || tableIndex == TableIndex.DELTA_SCORES_TABLE) {

            progressDialog = new ProgressDialogX(peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Copying to File/Clipboard. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        OutputGenerator outputGenerator = new OutputGenerator(peptideShakerGUI);
                        String clipboardString = null;

                        if (tableIndex == TableIndex.MODIFIED_PEPTIDES_TABLE) {
                            outputGenerator.getPeptidesOutput(
                                    null, displayedPeptides, null, true, false, true, true, true, true, true,
                                    true, true, true, true, true, true, true, true, false, false, false, null, true);
                        } else if (tableIndex == TableIndex.RELATED_PEPTIDES_TABLE) {
                            outputGenerator.getPeptidesOutput(
                                    null, relatedPeptides, null, true, false, true, true, true, true, true,
                                    true, true, true, true, true, true, true, true, false, false, false, null, true);
                        } else if (tableIndex == TableIndex.MODIFIED_PSMS_TABLE) {
                            outputGenerator.getPSMsOutput(
                                    null, identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatchesKeys(),
                                    true, false, true, true, true, true,
                                    true, true, true, true, true, true, true, false, false);
                        } else if (tableIndex == TableIndex.RELATED_PSMS_TABLE) {
                            outputGenerator.getPSMsOutput(
                                    null, identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatchesKeys(),
                                    true, false, true, true, true, true,
                                    true, true, true, true, true, true, true, false, false);
                        } else if (tableIndex == TableIndex.PTM_TABLE) {
                            // @TODO: implement me!!
                            clipboardString = "not yet implemented...";
                        } else if (tableIndex == TableIndex.A_SCORES_TABLE) {
                            clipboardString = Util.tableToText(psmAScoresTable, "\t", progressDialog, false);
                        } else if (tableIndex == TableIndex.DELTA_SCORES_TABLE) {
                            clipboardString = Util.tableToText(psmDeltaScoresTable, "\t", progressDialog, false);
                        }

                        if (!progressDialog.isRunCanceled() && clipboardString != null) {
                            StringSelection stringSelection = new StringSelection(clipboardString);
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(stringSelection, peptideShakerGUI);
                        }

                        boolean processCancelled = progressDialog.isRunCanceled();
                        progressDialog.setRunFinished();

                        if (!processCancelled && clipboardString != null) {
                            JOptionPane.showMessageDialog(peptideShakerGUI, "Table content copied to clipboard.", "Copied to Clipboard", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /**
     * Method called whenever the component is resized to maintain the look of
     * the GUI.
     */
    public void updateSeparators() {

        formComponentResized(null);

        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // set the sliders split pane divider location
                if (peptideShakerGUI.getUserPreferences().showSliders()) {
                    slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth() - 30);
                } else {
                    slidersSplitPane.setDividerLocation(slidersSplitPane.getWidth());
                }
                ptmAndPeptideSelectionPanel.revalidate();
                ptmAndPeptideSelectionPanel.repaint();
                formComponentResized(null);
            }
        });

        formComponentResized(null);
    }

    /**
     * Updates and displays the current spectrum slider tooltip.
     */
    private void updateSpectrumSliderToolTip() {
        SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
        double accuracy = (accuracySlider.getValue() / 100.0) * searchParameters.getFragmentIonAccuracy();

        spectrumTabbedPane.setToolTipText("<html>Accuracy: " + Util.roundDouble(accuracy, 2) + " " + searchParameters.getFragmentAccuracyType() + "<br>"
                + "Level: " + intensitySlider.getValue() + "%</html>");

        // show the tooltip now
        ToolTipManager.sharedInstance().mouseMoved(
                new MouseEvent(spectrumTabbedPane, 0, 0, 0,
                        spectrumTabbedPane.getWidth() - 50, spectrumTabbedPane.getY() + 20, // X-Y of the mouse for the tool tip
                        0, false));
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected peptide
     * and PSM.
     */
    public void newItemSelection() {
        String peptideKey = getSelectedPeptide();
        if (peptideKey.equals("")) {
            peptideKey = PeptideShakerGUI.NO_SELECTION;
        }

        String psmKey = "";
        if (selectedPsmsTable.getSelectedRow() != -1) {
            psmKey = getSelectedPsmsKeys(false).get(0);
        } else if (relatedPsmsTable.getSelectedRow() != -1) {
            psmKey = getSelectedPsmsKeys(true).get(0);
        }

        if (psmKey.equals("")) {
            psmKey = PeptideShakerGUI.NO_SELECTION;
        }

        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, peptideKey, psmKey);
    }

    /**
     * Update the PSM modification profiles table.
     *
     * @param progressDialog
     */
    private void updateModificationProfilesTable(ProgressDialogX progressDialog) {

        if (spectrumTabbedPane.isEnabledAt(0)) {
            progressDialog.setTitle("Updating Modification Profile Table. Please Wait...");
            progressDialog.setPrimaryProgressCounterIndeterminate(true);

            psmAScoresTable.setModel(new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {

                    if (columnIndex == 0) {
                        return Double.class;
                    } else {
                        return Double.class;
                    }
                }
            });

            psmDeltaScoresTable.setModel(new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {

                    if (columnIndex == 0) {
                        return Double.class;
                    } else {
                        return Double.class;
                    }
                }
            });

            if (peptidesTable.getSelectedRow() != -1) {
                try {
                    PeptideMatch peptideMatch = identification.getPeptideMatch(displayedPeptides.get((Integer) peptidesTable.getValueAt(peptidesTable.getSelectedRow(), 0) - 1));
                    String sequence = peptideMatch.getTheoreticPeptide().getSequence();

                    ((DefaultTableModel) psmAScoresTable.getModel()).addColumn("");
                    ((DefaultTableModel) psmDeltaScoresTable.getModel()).addColumn("");

                    for (int i = 0; i < sequence.length(); i++) {
                        String columnName = "" + sequence.charAt(i) + (i + 1);
                        ((DefaultTableModel) psmAScoresTable.getModel()).addColumn(columnName);
                        ((DefaultTableModel) psmDeltaScoresTable.getModel()).addColumn(columnName);
                    }

                    String selectedPtmName = getSelectedModification();
                    PTM selectedPtm = ptmFactory.getPTM(selectedPtmName);

                    // add the psm scores (a score and delta score)
                    identification.loadSpectrumMatches(peptideMatch.getSpectrumMatchesKeys(), progressDialog, false);
                    for (int i = 0; i < peptideMatch.getSpectrumMatchesKeys().size(); i++) {

                        String spectrumKey = peptideMatch.getSpectrumMatchesKeys().get(i);
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getSpectrumMatch(spectrumKey).getUrParam(ptmScores);
                        ((DefaultTableModel) psmAScoresTable.getModel()).addRow(new Object[]{(i + 1)});
                        ((DefaultTableModel) psmDeltaScoresTable.getModel()).addRow(new Object[]{(i + 1)});

                        if (ptmScores != null) {

                            HashMap<Integer, Double> dScores = new HashMap<Integer, Double>();
                            HashMap<Integer, Double> pScores = new HashMap<Integer, Double>();

                            for (String ptmName : ptmScores.getScoredPTMs()) {

                                PTM ptm = ptmFactory.getPTM(ptmName);

                                if (ptm.getMass() == selectedPtm.getMass()) { // @TODO: compare against the accuracy

                                    PtmScoring ptmScoring = ptmScores.getPtmScoring(ptmName);
                                    for (int site : ptmScoring.getAllPtmLocations()) {
                                        double ptmDScore = ptmScoring.getDeltaScore(site);
                                        Double tableDScore = dScores.get(site);
                                        if (tableDScore == null || tableDScore < ptmDScore) {
                                            dScores.put(site, ptmDScore);
                                        }
                                        double ptmPScore = ptmScoring.getProbabilisticScore(site);
                                        Double tablePScore = pScores.get(site);
                                        if (tablePScore == null || tablePScore < ptmPScore) {
                                            pScores.put(site, ptmPScore);
                                        }
                                    }
                                }
                            }

                            for (int site : dScores.keySet()) {
                                psmDeltaScoresTable.setValueAt(dScores.get(site), i, site);
                            }
                            for (int site : pScores.keySet()) {
                                psmAScoresTable.setValueAt(pScores.get(site), i, site);
                            }
                        }
                    }

                    // set the cell renderers
                    updatePsmScoresCellRenderers();
                } catch (Exception e) {
                    peptideShakerGUI.catchException(e);
                }
            }
        }
    }

    /**
     * Update the PSM delta and a score table cell renderers.
     */
    private void updatePsmScoresCellRenderers() {
        for (int i = 1; i < psmAScoresTable.getColumnCount(); i++) {
            if (peptideShakerGUI.showSparklines()) {
                psmAScoresTable.getColumn(psmAScoresTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d, 100d));
                ((JSparklinesBarChartTableCellRenderer) psmAScoresTable.getColumn(psmAScoresTable.getColumnName(i)).getCellRenderer()).showAsHeatMap(ColorGradient.GreenWhiteBlue, false);
            } else {
                ((JSparklinesBarChartTableCellRenderer) psmAScoresTable.getColumn(psmAScoresTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }

        for (int i = 1; i < psmDeltaScoresTable.getColumnCount(); i++) {
            if (peptideShakerGUI.showSparklines()) {
                psmDeltaScoresTable.getColumn(psmDeltaScoresTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d, 100d));
                ((JSparklinesBarChartTableCellRenderer) psmDeltaScoresTable.getColumn(psmDeltaScoresTable.getColumnName(i)).getCellRenderer()).showAsHeatMap(ColorGradient.GreenWhiteBlue, false);
            } else {
                ((JSparklinesBarChartTableCellRenderer) psmDeltaScoresTable.getColumn(psmDeltaScoresTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }
    }
}
