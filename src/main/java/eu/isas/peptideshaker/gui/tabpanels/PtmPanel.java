package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.protein.SequenceModificationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import com.compomics.util.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.scoring.PtmScoring;
import com.compomics.util.gui.protein.ModificationProfile;
import eu.isas.peptideshaker.export.OutputGenerator;
import eu.isas.peptideshaker.gui.ExportGraphicsDialog;
import eu.isas.peptideshaker.gui.HelpDialog;
import eu.isas.peptideshaker.gui.ProteinInferencePeptideLevelDialog;
import eu.isas.peptideshaker.gui.PtmSiteInferenceDialog;
import eu.isas.peptideshaker.gui.PtmTable;
import eu.isas.peptideshaker.myparameters.PSMaps;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
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
     * The selected psms table column header tooltips.
     */
    private ArrayList<String> selectedPsmsTableToolTips;
    /**
     * The related psms table column header tooltips.
     */
    private ArrayList<String> relatedPsmsTableToolTips;
    /**
     * PTM table column header tooltips.
     */
    private ArrayList<String> ptmTableToolTips;
    /**
     * The spectrum annotator for the first spectrum
     */
    private SpectrumAnnotator annotator = new SpectrumAnnotator();
    /**
     * The main GUI
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * Map of all peptide keys indexed by their modification status
     */
    private HashMap<String, ArrayList<String>> peptideMap = new HashMap<String, ArrayList<String>>();
    /**
     * The modification name for no modification
     */
    public final static String NO_MODIFICATION = "no modification";
    /**
     * The displayed identification
     */
    private Identification identification;
    /**
     * The keys of the peptides currently displayed
     */
    private ArrayList<String> displayedPeptides = new ArrayList<String>();
    /**
     * The keys of the related peptides currently displayed
     */
    private ArrayList<String> relatedPeptides = new ArrayList<String>();
    /**
     * boolean indicating whether the related peptide is selected
     */
    private boolean relatedSelected = false;
    /**
     * The current spectrum panel for the upper psm.
     */
    private SpectrumPanel spectrum;
    /**
     * The current spectrum key.
     */
    private String currentSpectrumKey = "";
    /**
     * Protein inference map, key: pi type, element: pi as a string.
     */
    private HashMap<Integer, String> proteinInferenceTooltipMap;
    /**
     * PTM confidence tooltip map, key: ptm confidence type, element: ptm
     * confidence as a string.
     */
    private HashMap<Integer, String> ptmConfidenceTooltipMap;
    /**
     * The ptm factory
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
        formComponentResized(null);

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
        ptmTableJScrollPane.getViewport().setOpaque(false);
        psmAScoresScrollPane.getViewport().setOpaque(false);
        psmDeltaScrollPane.getViewport().setOpaque(false);

        // @TODO: remove when the ptm table is implemented
        //spectrumTabbedPane.setEnabledAt(0, false);

        setTableProperties();

        // make the tabs in the spectrum tabbed pane go from right to left
        spectrumTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
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
                        0, getHeight() / 2,
                        startColor,
                        getWidth(), getHeight() / 2,
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
                        0, getHeight() / 2,
                        startColor,
                        getWidth(), getHeight() / 2,
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
        JPanel ptmTableCorner = new JPanel();
        ptmTableCorner.setBackground(relatedPsmsTable.getTableHeader().getBackground());
        ptmTableJScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, ptmTableCorner);
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
        proteinInferenceColorMap.put(PSParameter.NOT_GROUP, peptideShakerGUI.getSparklineColor()); // NOT_GROUP
        proteinInferenceColorMap.put(PSParameter.ISOFORMS, Color.YELLOW); // ISOFORMS
        proteinInferenceColorMap.put(PSParameter.ISOFORMS_UNRELATED, Color.BLUE); // ISOFORMS_UNRELATED
        proteinInferenceColorMap.put(PSParameter.UNRELATED, Color.RED); // UNRELATED

        // set up the protein inference tooltip map
        proteinInferenceTooltipMap = new HashMap<Integer, String>();
        proteinInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Unique to a single protein");
        proteinInferenceTooltipMap.put(PSParameter.ISOFORMS, "Belongs to a group of isoforms");
        proteinInferenceTooltipMap.put(PSParameter.ISOFORMS_UNRELATED, "Belongs to a group of isoforms and unrelated proteins");
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
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        peptidesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        relatedPeptidesTable.getColumn("Peptide").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPeptidesTable.getColumn("Peptide").getCellRenderer()).showNumberAndChart(
                true, peptideShakerGUI.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        relatedPeptidesTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));

        selectedPsmsTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) selectedPsmsTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() - 30);
        selectedPsmsTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));
        selectedPsmsTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d,
                1000d, 10d, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
        ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
        selectedPsmsTable.getColumn("   ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/star_yellow.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                new ImageIcon(this.getClass().getResource("/icons/star_grey.png")),
                "Starred", null, null));

        relatedPsmsTable.getColumn("Charge").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) relatedPsmsTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() - 30);
        relatedPsmsTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                new ImageIcon(this.getClass().getResource("/icons/Error_3.png")),
                "Validated", "Not Validated"));
        relatedPsmsTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d,
                1000d, 10d, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
        ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
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
        ptmTableJPanel = new javax.swing.JPanel();
        ptmTableJScrollPane = new javax.swing.JScrollPane();
        ptmTableJToolBar = new javax.swing.JToolBar();
        ptmTableAnnotationMenuPanel = new javax.swing.JPanel();
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
                    .addComponent(ptmJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        ptmLayeredPanelLayout.setVerticalGroup(
            ptmLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 373, Short.MAX_VALUE)
            .addGroup(ptmLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ptmLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(ptmJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        ptmLayeredPanel.setBounds(0, 0, 260, 400);
        ptmLayeredLayeredPane.add(ptmLayeredPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
        ptmSelectionHelpJButton.setBounds(240, 0, 10, 19);
        ptmLayeredLayeredPane.add(ptmSelectionHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        contextMenuPtmBackgroundPanel.setBounds(230, 0, 20, 19);
        ptmLayeredLayeredPane.add(contextMenuPtmBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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
        peptidesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptidesTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptidesTableMouseReleased(evt);
            }
        });
        peptidesTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                peptidesTableMouseMoved(evt);
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
                .addComponent(selectedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 778, Short.MAX_VALUE)
                .addContainerGap())
        );
        selectedPeptidesJPanelLayout.setVerticalGroup(
            selectedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectedPeptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                .addContainerGap())
        );

        selectedPeptidesJPanel.setBounds(0, 0, 810, 170);
        modifiedPeptidesLayeredPane.add(selectedPeptidesJPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
        modificationProfileHelpJButton.setBounds(747, 0, 10, 19);
        modifiedPeptidesLayeredPane.add(modificationProfileHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        exportModifiedPeptideProfileJButton.setBounds(730, 0, 10, 19);
        modifiedPeptidesLayeredPane.add(exportModifiedPeptideProfileJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        contextMenuModifiedPeptidesBackgroundPanel.setBounds(730, 0, 30, 19);
        modifiedPeptidesLayeredPane.add(contextMenuModifiedPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout modifiedPeptidesPanelLayout = new javax.swing.GroupLayout(modifiedPeptidesPanel);
        modifiedPeptidesPanel.setLayout(modifiedPeptidesPanelLayout);
        modifiedPeptidesPanelLayout.setHorizontalGroup(
            modifiedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 812, Short.MAX_VALUE)
            .addGroup(modifiedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(modifiedPeptidesLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 812, Short.MAX_VALUE))
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
                    .addComponent(relatedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 778, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        relatedPeptidesPanelLayout.setVerticalGroup(
            relatedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 203, Short.MAX_VALUE)
            .addGroup(relatedPeptidesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(relatedPeptidesPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(relatedPeptidesJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        relatedPeptidesPanel.setBounds(0, 0, 810, 230);
        relatedPeptiesLayeredPane.add(relatedPeptidesPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
        relatedProfileHelpJButton.setBounds(750, 0, 10, 19);
        relatedPeptiesLayeredPane.add(relatedProfileHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        exportRelatedPeptideProfileJButton.setBounds(740, 0, 10, 19);
        relatedPeptiesLayeredPane.add(exportRelatedPeptideProfileJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        contextMenuRelatedPeptidesBackgroundPanel.setBounds(730, 0, 30, 19);
        relatedPeptiesLayeredPane.add(contextMenuRelatedPeptidesBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout relatedPeptidesJPanelLayout = new javax.swing.GroupLayout(relatedPeptidesJPanel);
        relatedPeptidesJPanel.setLayout(relatedPeptidesJPanelLayout);
        relatedPeptidesJPanelLayout.setHorizontalGroup(
            relatedPeptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(relatedPeptiesLayeredPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 812, Short.MAX_VALUE)
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

        ptmTableJPanel.setBackground(new java.awt.Color(255, 255, 255));

        ptmTableJScrollPane.setOpaque(false);
        ptmTableJScrollPane.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                ptmTableJScrollPaneMouseWheelMoved(evt);
            }
        });

        ptmTableJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        ptmTableJToolBar.setBorder(null);
        ptmTableJToolBar.setFloatable(false);
        ptmTableJToolBar.setRollover(true);
        ptmTableJToolBar.setBorderPainted(false);

        ptmTableAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(ptmTableAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        ptmTableJToolBar.add(ptmTableAnnotationMenuPanel);

        javax.swing.GroupLayout ptmTableJPanelLayout = new javax.swing.GroupLayout(ptmTableJPanel);
        ptmTableJPanel.setLayout(ptmTableJPanelLayout);
        ptmTableJPanelLayout.setHorizontalGroup(
            ptmTableJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ptmTableJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ptmTableJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(ptmTableJScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                    .addComponent(ptmTableJToolBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE))
                .addContainerGap())
        );
        ptmTableJPanelLayout.setVerticalGroup(
            ptmTableJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ptmTableJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ptmTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ptmTableJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumTabbedPane.addTab("PTM Table", ptmTableJPanel);

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
                .addComponent(psmAScoresScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmAScoresJPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {aScoreMaxValueJLabel, aScoreMinValueJLabel});

        psmAScoresJPanelLayout.setVerticalGroup(
            psmAScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmAScoresJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmAScoresScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psmAScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(aScoreMinValueJLabel)
                    .addComponent(aScoreGradientPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aScoreMaxValueJLabel))
                .addContainerGap())
        );

        spectrumTabbedPane.addTab("A-Scores", psmAScoresJPanel);

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
                .addComponent(psmDeltaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addContainerGap())
        );

        psmDeltaScoresJPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {deltaScoreMaxValueJLabel, deltaScoreMinValueJLabel});

        psmDeltaScoresJPanelLayout.setVerticalGroup(
            psmDeltaScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmDeltaScoresJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmDeltaScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(psmDeltaScoresJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(deltaScoreMinValueJLabel)
                    .addComponent(deltaScoreGradientPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deltaScoreMaxValueJLabel))
                .addContainerGap())
        );

        spectrumTabbedPane.addTab("Delta Scores", psmDeltaScoresJPanel);

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
                .addComponent(spectrumChartJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumTabbedPane.setSelectedIndex(3);

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
                .addComponent(accuracySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                .addGap(27, 27, 27)
                .addComponent(intensitySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
                .addGap(47, 47, 47))
        );

        slidersSplitPane.setRightComponent(sliderPanel);

        javax.swing.GroupLayout spectrumAndFragmentIonPanelLayout = new javax.swing.GroupLayout(spectrumAndFragmentIonPanel);
        spectrumAndFragmentIonPanel.setLayout(spectrumAndFragmentIonPanelLayout);
        spectrumAndFragmentIonPanelLayout.setHorizontalGroup(
            spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slidersSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 538, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumAndFragmentIonPanelLayout.setVerticalGroup(
            spectrumAndFragmentIonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumAndFragmentIonPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slidersSplitPane)
                .addContainerGap())
        );

        spectrumAndFragmentIonPanel.setBounds(0, 0, 570, 440);
        spectrumLayeredPane.add(spectrumAndFragmentIonPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
        spectrumHelpJButton.setBounds(540, 0, 10, 19);
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
        exportSpectrumJButton.setBounds(530, 0, 10, 19);
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

        contextMenuSpectrumBackgroundPanel.setBounds(530, 0, 30, 19);
        spectrumLayeredPane.add(contextMenuSpectrumBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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

        modsPsmsLayeredPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide-Spectrum Matches - Modified Peptide"));
        modsPsmsLayeredPanel.setOpaque(false);

        psmsModifiedTableJScrollPane.setOpaque(false);

        selectedPsmsTable.setModel(new SelectedPsmsTable());
        selectedPsmsTable.setOpaque(false);
        selectedPsmsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectedPsmsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                selectedPsmsTableMouseReleased(evt);
            }
        });
        selectedPsmsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                selectedPsmsTableMouseMoved(evt);
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
                    .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        modsPsmsLayeredPanelLayout.setVerticalGroup(
            modsPsmsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 163, Short.MAX_VALUE)
            .addGroup(modsPsmsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(modsPsmsLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(psmsModifiedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        modsPsmsLayeredPanel.setBounds(0, 0, 500, 190);
        psmsModPeptidesLayeredPane.add(modsPsmsLayeredPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
        modifiedPsmsHelpJButton.setBounds(480, 0, 10, 19);
        psmsModPeptidesLayeredPane.add(modifiedPsmsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        exportModifiedPsmsJButton.setBounds(470, 0, 10, 19);
        psmsModPeptidesLayeredPane.add(exportModifiedPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        contextMenuModPsmsBackgroundPanel.setBounds(460, 0, 30, 19);
        psmsModPeptidesLayeredPane.add(contextMenuModPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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

        relatedPsmsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide-Spectrum Matches - Releated Peptide"));
        relatedPsmsPanel.setOpaque(false);

        psmsRelatedTableJScrollPane.setOpaque(false);

        relatedPsmsTable.setModel(new RelatedPsmsTable());
        relatedPsmsTable.setOpaque(false);
        relatedPsmsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        relatedPsmsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                relatedPsmsTableMouseReleased(evt);
            }
        });
        relatedPsmsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                relatedPsmsTableMouseMoved(evt);
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
                    .addComponent(psmsRelatedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        relatedPsmsPanelLayout.setVerticalGroup(
            relatedPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 223, Short.MAX_VALUE)
            .addGroup(relatedPsmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(relatedPsmsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(psmsRelatedTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        relatedPsmsPanel.setBounds(0, 0, 500, 250);
        psmsRelatedPeptidesJLayeredPane.add(relatedPsmsPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
        relatedPsmsHelpJButton.setBounds(480, 0, 10, 19);
        psmsRelatedPeptidesJLayeredPane.add(relatedPsmsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        exportRelatedPsmsJButton.setBounds(470, 0, 10, 19);
        psmsRelatedPeptidesJLayeredPane.add(exportRelatedPsmsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        contextMenuRelatedPsmsBackgroundPanel.setBounds(460, 0, 30, 19);
        psmsRelatedPeptidesJLayeredPane.add(contextMenuRelatedPsmsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

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
     * @see #peptidesTableMouseClicked(java.awt.event.MouseEvent)
     */
    private void peptidesTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptidesTableKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            peptidesTableMouseReleased(null);
        }
    }//GEN-LAST:event_peptidesTableKeyReleased

    /**
     * @see #relatedPeptidesTableMouseClicked(java.awt.event.MouseEvent)
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
     * Update the related peptides and modified peptide psms tables.
     *
     * @param evt
     */
    private void peptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesTableMouseReleased

        final MouseEvent finalEvt = evt;

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setIndeterminate(true);
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

        final Thread displayThread = new Thread("DisplayThread") {

            public void run() {

                if (finalEvt != null) {

                    int row = peptidesTable.rowAtPoint(finalEvt.getPoint());
                    int column = peptidesTable.columnAtPoint(finalEvt.getPoint());

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

                    if (row != -1 && !progressDialog.isRunCanceled()) {

                        peptidesTable.setRowSelectionInterval(row, row);

                        // open the protein inference at the petide level dialog
                        if (column == peptidesTable.getColumn("PI").getModelIndex()) {
                            progressDialog.setVisible(false);
                            try {
                                String peptideKey = getSelectedPeptide(false);
                                new ProteinInferencePeptideLevelDialog(peptideShakerGUI, true, peptideKey, null);
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                            }
                        } else if (column == peptidesTable.getColumn("PTM").getModelIndex()) {
                            if (peptidesTable.getValueAt(row, column) != null
                                    && ((Integer) peptidesTable.getValueAt(row, column)).intValue() != -1) {
                                progressDialog.setVisible(false);
                                new PtmSiteInferenceDialog(peptideShakerGUI, getSelectedPeptide(), ptmFactory.getPTM(getSelectedModification()));
                            }
                        }
                    }
                } else {
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

                progressDialog.setRunFinished();
            }
        };

        Thread appThread = new Thread() {

            public void run() {
                try {
                    SwingUtilities.invokeAndWait(displayThread);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        appThread.start();
    }//GEN-LAST:event_peptidesTableMouseReleased

    /**
     * Update the related peptides psm table.
     *
     * @param evt
     */
    private void relatedPeptidesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPeptidesTableMouseReleased

        final MouseEvent finalEvt = evt;

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setIndeterminate(true);
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

                if (finalEvt != null && !progressDialog.isRunCanceled()) {

                    int row = relatedPeptidesTable.rowAtPoint(finalEvt.getPoint());
                    int column = relatedPeptidesTable.columnAtPoint(finalEvt.getPoint());

                    if (row != -1) {

                        relatedPeptidesTable.setRowSelectionInterval(row, row);

                        // open the protein inference at the petide level dialog
                        if (column == relatedPeptidesTable.getColumn("PI").getModelIndex()) {
                            progressDialog.setVisible(false);
                            try {
                                String peptideKey = getSelectedPeptide(true);
                                new ProteinInferencePeptideLevelDialog(peptideShakerGUI, true, peptideKey, null);
                            } catch (Exception e) {
                                peptideShakerGUI.catchException(e);
                            }
                        } else if (column == relatedPeptidesTable.getColumn("PTM").getModelIndex()) {
                            if (relatedPeptidesTable.getValueAt(row, column) != null
                                    && ((Integer) relatedPeptidesTable.getValueAt(row, column)).intValue() != -1) {
                                progressDialog.setVisible(false);
                                new PtmSiteInferenceDialog(peptideShakerGUI, getSelectedPeptide(), ptmFactory.getPTM(getSelectedModification()));
                            }
                        }
                    }
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

        if (selectedPsmsTable.getSelectedRow() != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            try {
                relatedSelected = false;
                updateGraphics(null);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                e.printStackTrace();
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            while (relatedPsmsTable.getSelectedRow() >= 0) {
                relatedPsmsTable.removeRowSelectionInterval(relatedPsmsTable.getSelectedRow(), relatedPsmsTable.getSelectedRow());
            }

            newItemSelection();
        }
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
                            peptideShakerGUI.getIdentificationFeaturesGenerator().getPeptideModificationTooltipAsHtml(identification.getPeptideMatch(displayedPeptides.get(
                            (Integer) peptidesTable.getValueAt(row, 0) - 1)).getTheoreticPeptide()));
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
                            peptideShakerGUI.getIdentificationFeaturesGenerator().getPeptideModificationTooltipAsHtml(identification.getPeptideMatch(
                            relatedPeptides.get((Integer) relatedPeptidesTable.getValueAt(row, 0) - 1)).getTheoreticPeptide()));
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
        progressDialog.setIndeterminate(true);
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

        SwingUtilities.invokeLater(new Runnable() {

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
                            peptideShakerGUI.getSearchParameters().getModificationProfile().setColor(
                                    (String) ptmJTable.getValueAt(row, ptmJTable.getColumn("  ").getModelIndex()), newColor);
                            peptideShakerGUI.updatePtmColorCoding();
                        }
                    }
                } else {
                    updatePeptideTable(progressDialog);
                }

                currentPtmRow = row;

                progressDialog.setRunFinished();
            }
        });
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
            progressDialog.setIndeterminate(true);
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

            final Thread displayThread = new Thread("DisplayThread") {

                @Override
                public void run() {

                    updatePeptideTable(progressDialog);
                    progressDialog.setRunFinished();
                }
            };

            Thread appThread = new Thread() {

                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(displayThread);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            appThread.start();
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
     * sequenence column.
     *
     * @param evt
     */
    private void selectedPsmsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectedPsmsTableMouseMoved

        int row = selectedPsmsTable.rowAtPoint(evt.getPoint());
        int column = selectedPsmsTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column != -1 && selectedPsmsTable.getValueAt(row, column) != null) {

            if (column == selectedPsmsTable.getColumn("Sequence").getModelIndex()) {

                try {
                    String spectrumKey = identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatches().get(row);
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    selectedPsmsTable.setToolTipText(
                            peptideShakerGUI.getIdentificationFeaturesGenerator().getPeptideModificationTooltipAsHtml(spectrumMatch.getBestAssumption().getPeptide()));
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
        spectrumAndFragmentIonJPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_accuracySliderMouseWheelMoved

    /**
     * Update the spectrum.
     *
     * @param evt
     */
    private void relatedPsmsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_relatedPsmsTableMouseReleased

        if (relatedPsmsTable.getSelectedRow() != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            try {
                relatedSelected = true;
                updateGraphics(null);
            } catch (Exception e) {
                peptideShakerGUI.catchException(e);
                e.printStackTrace();
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

            while (selectedPsmsTable.getSelectedRow() >= 0) {
                selectedPsmsTable.removeRowSelectionInterval(selectedPsmsTable.getSelectedRow(), selectedPsmsTable.getSelectedRow());
            }

            newItemSelection();
        }
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
                    String spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches().get(row);
                    SpectrumMatch spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                    relatedPsmsTable.setToolTipText(
                            peptideShakerGUI.getIdentificationFeaturesGenerator().getPeptideModificationTooltipAsHtml(spectrumMatch.getBestAssumption().getPeptide()));
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#Peptides");
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#RelatedPeptides");
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#Selection");
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
        copyTableContentToClipboard(TableIndex.MODIFIED_PSMS_TABLE);
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#PSMs");
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
        copyTableContentToClipboard(TableIndex.RELATED_PSMS_TABLE);
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
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#RelatedPSMs");
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
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#PTM_Table");
        } else if (spectrumTabIndex == 1) {
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#AScore");
        } else if (spectrumTabIndex == 2) {
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#DeltsScore");
        } else if (spectrumTabIndex == 3) {
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/PTMPanel.html"), "#Spectrum");
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

        int index = spectrumTabbedPane.getSelectedIndex();

        if (index == 0) { // ptm table
            // @TODO: implement me!
//            menuItem = new JMenuItem("Table to Clipboard");
//            menuItem.addActionListener(new java.awt.event.ActionListener() {
//
//                public void actionPerformed(java.awt.event.ActionEvent evt) {
//                    copyTableContentToClipboard(TableIndex.PTM_TABLE);
//                }
//            });
//
//            popupMenu.add(new JSeparator());
//            popupMenu.add(menuItem);
        } else if (index == 1) { // a-scores table

            menuItem = new JMenuItem("Table to Clipboard");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    copyTableContentToClipboard(TableIndex.A_SCORES_TABLE);
                }
            });

            popupMenu.add(new JSeparator());
            popupMenu.add(menuItem);

        } else if (index == 2) { // delta scores table

            menuItem = new JMenuItem("Table to Clipboard");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    copyTableContentToClipboard(TableIndex.DELTA_SCORES_TABLE);
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

        JMenuItem menuItem = new JMenuItem("Table to Clipboard");
        menuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyTableContentToClipboard(TableIndex.RELATED_PEPTIDES_TABLE);
            }
        });

        popupMenu.add(menuItem);

        if (modificationProfileRelatedPeptideJPanel.getComponentCount() == 2) {

            menuItem = new JMenuItem("Modification Profile Plot");
            menuItem.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    new ExportGraphicsDialog(peptideShakerGUI, true, modificationProfileRelatedPeptideJPanel.getComponent(1));
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

        JMenuItem menuItem = new JMenuItem("Table to Clipboard");
        menuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyTableContentToClipboard(TableIndex.MODIFIED_PEPTIDES_TABLE);
            }
        });

        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Modification Profile Plot");
        menuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new ExportGraphicsDialog(peptideShakerGUI, true, modificationProfileSelectedPeptideJPanel.getComponent(1));
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
            if (index == 3) {
                spectrumAnnotationMenuPanel.removeAll();
                spectrumAnnotationMenuPanel.add(peptideShakerGUI.getAnnotationMenuBar());
                peptideShakerGUI.updateAnnotationMenuBarVisableOptions(true, false, false, false);
            }
        }
    }//GEN-LAST:event_spectrumTabbedPaneStateChanged

    /**
     * Updates the slider value when the user scrolls.
     *
     * @param evt
     */
    private void ptmTableJScrollPaneMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_ptmTableJScrollPaneMouseWheelMoved
        spectrumAndFragmentIonJPanelMouseWheelMoved(evt);
    }//GEN-LAST:event_ptmTableJScrollPaneMouseWheelMoved
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
    private javax.swing.JPanel ptmTableAnnotationMenuPanel;
    private javax.swing.JPanel ptmTableJPanel;
    private javax.swing.JScrollPane ptmTableJScrollPane;
    private javax.swing.JToolBar ptmTableJToolBar;
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
                ArrayList<String> proteins = identification.getPeptideMatch(peptideKey).getTheoreticPeptide().getParentProteins();
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
     * Returns a list of the psms keys of the currently displayed assumptions.
     *
     * @return a list of the psms keys of the currently displayed assumptions
     */
    public ArrayList<String> getDisplayedPsms() {
        ArrayList<String> result = new ArrayList<String>();
        try {
            for (String peptide : displayedPeptides) {
                result.addAll(identification.getPeptideMatch(peptide).getSpectrumMatches());
            }
            for (String peptide : relatedPeptides) {
                result.addAll(identification.getPeptideMatch(peptide).getSpectrumMatches());
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
    private void createPeptideMap(ProgressDialogX progressDialogX) {

        boolean modified;
        ArrayList<String> accountedModifications;

        for (String peptideKey : identification.getPeptideIdentification()) {

            modified = false;
            accountedModifications = new ArrayList<String>();

            for (String modificationName : Peptide.getModificationFamily(peptideKey)) {

                if (!accountedModifications.contains(modificationName)) {

                    if (!peptideMap.containsKey(modificationName)) {
                        peptideMap.put(modificationName, new ArrayList<String>());
                    }

                    peptideMap.get(modificationName).add(peptideKey);
                    modified = true;
                    accountedModifications.add(modificationName);
                }
            }

            if (!modified) {
                if (!peptideMap.containsKey(NO_MODIFICATION)) {
                    peptideMap.put(NO_MODIFICATION, new ArrayList<String>());
                }
                peptideMap.get(NO_MODIFICATION).add(peptideKey);
            }
            if (progressDialogX != null) {
                progressDialogX.increaseProgressValue();
            }
        }
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
        progressDialog.setIndeterminate(true);
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

                // now we have date so we can update the RT cell renderers max and min values
                selectedPsmsTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, SpectrumFactory.getInstance().getMinRT(),
                        SpectrumFactory.getInstance().getMaxRT(), SpectrumFactory.getInstance().getMaxRT() / 50, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
                ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
                ((JSparklinesIntervalChartTableCellRenderer) selectedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
                relatedPsmsTable.getColumn("RT").setCellRenderer(new JSparklinesIntervalChartTableCellRenderer(PlotOrientation.HORIZONTAL, SpectrumFactory.getInstance().getMinRT(),
                        SpectrumFactory.getInstance().getMaxRT(), SpectrumFactory.getInstance().getMaxRT() / 50, peptideShakerGUI.getSparklineColor(), peptideShakerGUI.getSparklineColor()));
                ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
                ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);

                identification = peptideShakerGUI.getIdentification();
                createPeptideMap(progressDialog);
                String[] modifications = new String[peptideMap.size()];
                int cpt = 0;

                for (String modification : peptideMap.keySet()) {
                    modifications[cpt] = modification;
                    cpt++;
                }

                Arrays.sort(modifications);

                DefaultTableModel dm = (DefaultTableModel) ptmJTable.getModel();
                dm.getDataVector().removeAllElements();
                dm.fireTableDataChanged();

                for (int i = 0; i < modifications.length; i++) {

                    if (!modifications[i].equalsIgnoreCase(NO_MODIFICATION)) {
                        ((DefaultTableModel) ptmJTable.getModel()).addRow(new Object[]{
                                    peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(modifications[i]),
                                    modifications[i]
                                });
                    }
                }

                ((DefaultTableModel) ptmJTable.getModel()).addRow(new Object[]{
                            Color.lightGray,
                            NO_MODIFICATION});

                // update the slider tooltips
                double accuracy = (accuracySlider.getValue() / 100.0) * peptideShakerGUI.getSearchParameters().getFragmentIonAccuracy();
                accuracySlider.setToolTipText("Annotation Accuracy: " + Util.roundDouble(accuracy, 2) + " Da");
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
            }
        });
    }

    /**
     * Tries to find the last selected peptide.
     *
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
                    selectedKey = spectrumMatch.getBestAssumption().getPeptide().getKey();
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
                        for (String displayedPsm : peptideMatch.getSpectrumMatches()) {
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
                        for (String displayedPsm : peptideMatch.getSpectrumMatches()) {
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
        progressDialog.setIndeterminate(true);
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
     * @param progressDialog
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

                for (String peptideKey : peptideMap.get((String) ptmJTable.getValueAt(ptmJTable.getSelectedRow(), ptmJTable.getColumn("PTM").getModelIndex()))) {

                    if (progressDialog.isRunCanceled()) {
                        break;
                    }

                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

                    if (!peptideMatch.isDecoy()) {

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

        progressDialog.setIndeterminate(false);
        progressDialog.setValue(0);
        progressDialog.setMaxProgressValue(identification.getPeptideIdentification().size());

        for (String newKey : identification.getPeptideIdentification()) {

            if (progressDialog.isRunCanceled()) {
                break;
            }

            progressDialog.increaseProgressValue();

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
            progressDialog.setIndeterminate(true);

            relatedPeptides = new ArrayList<String>();
            ArrayList<Double> scores = new ArrayList<Double>(scoreToKeyMap.keySet());
            Collections.sort(scores);

            progressDialog.setIndeterminate(false);
            progressDialog.setValue(0);
            progressDialog.setMaxProgressValue(scores.size());

            for (Double score : scores) {
                relatedPeptides.addAll(scoreToKeyMap.get(score));
                progressDialog.increaseProgressValue();
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

        ((TitledBorder) modsPsmsLayeredPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide-Spectrum Matches - Modified Peptide (" 
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
                progressDialog.setIndeterminate(false);
                progressDialog.setValue(0);
                progressDialog.setMaxProgressValue(relatedPsmsTable.getRowCount());
            }

            for (int i = 0; i < relatedPsmsTable.getRowCount(); i++) {

                progressDialog.increaseProgressValue();

                String spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches().get(i);
                Precursor precursor = peptideShakerGUI.getPrecursor(spectrumKey, false);

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
                rtCellRenderer.showNumberAndChart(true, peptideShakerGUI.getLabelWidth() + 5);
                ((JSparklinesIntervalChartTableCellRenderer) relatedPsmsTable.getColumn("RT").getCellRenderer()).showReferenceLine(true, 0.02, java.awt.Color.BLACK);
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }

        ((TitledBorder) relatedPsmsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Peptide-Spectrum Matches - Related Peptide (" 
                + relatedPsmsTable.getRowCount() + ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        relatedPsmsPanel.repaint();
    }

    /**
     * Updates the graphics components.
     *
     * @param progressDialog
     */
    public void updateGraphics(ProgressDialogX progressDialog) {

        if (progressDialog != null) {
            progressDialog.setTitle("Updating Graphics. Please Wait...");
            progressDialog.setIndeterminate(true);
        }

        try {

            if (!getSelectedPeptide().equals("")) {
                if (relatedSelected && relatedPsmsTable.getSelectedRow() != -1) {
                    if (getSelectedPsm().size() == 1) {
                        spectrumTabbedPane.setEnabledAt(3, true);
                        updateSpectrum(getSelectedPsm().get(0));
                    } else {
                        spectrumTabbedPane.setEnabledAt(3, false);
                        spectrumTabbedPane.setSelectedIndex(0);
                    }
                } else if (selectedPsmsTable.getSelectedRow() != -1 && selectedPsmsTable.getSelectedRow() != -1) {
                    if (getSelectedPsm().size() == 1) {
                        updateSpectrum(getSelectedPsm().get(0));
                        spectrumTabbedPane.setEnabledAt(3, true);
                    } else {
                        spectrumTabbedPane.setEnabledAt(3, false);
                        spectrumTabbedPane.setSelectedIndex(0);
                    }
                }

                updatePtmTable();
                //@TODO: update psm modification profile
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            e.printStackTrace();
        }
    }

    /**
     * Updates the ptm table.
     */
    public void updatePtmTable() {
        try {
            Peptide selectedPeptide = identification.getPeptideMatch(getSelectedPeptide()).getTheoreticPeptide();
            PTM ptm = ptmFactory.getPTM(getSelectedModification());
            PtmTable ptmTable = new PtmTable(peptideShakerGUI, selectedPeptide, ptm, getSelectedPsm(), true);
            ptmTable.setRowHeight((int) (ptmTable.getRowHeight() * 1.5));
            ptmTableJScrollPane.setViewportView(ptmTable);
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            return;
        }
    }

    /**
     * Update the spectra according to the currently selected PSM.
     *
     * @param spectrumKey
     */
    public void updateSpectrum(String spectrumKey) {

        try {
            spectrumChartJPanel.removeAll();
            spectrumChartJPanel.revalidate();
            spectrumChartJPanel.repaint();

            AnnotationPreferences annotationPreferences = peptideShakerGUI.getAnnotationPreferences();
            MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);

            if (currentSpectrum != null && currentSpectrum.getMzValuesAsArray().length > 0) {

                Precursor precursor = currentSpectrum.getPrecursor();
                SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumKey);
                spectrum = new SpectrumPanel(
                        currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                        precursor.getMz(), spectrumMatch.getBestAssumption().getIdentificationCharge().toString(),
                        "", 40, false, false, false, 2, false);
                spectrum.setKnownMassDeltas(peptideShakerGUI.getCurrentMassDeltas());
                spectrum.setDeltaMassWindow(peptideShakerGUI.getAnnotationPreferences().getFragmentIonAccuracy());
                spectrum.setBorder(null);
                spectrum.setDataPointAndLineColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakColor(), 0);
                spectrum.setPeakWaterMarkColor(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakColor());
                spectrum.setPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumAnnotatedPeakWidth());
                spectrum.setBackgroundPeakWidth(peptideShakerGUI.getUtilitiesUserPreferences().getSpectrumBackgroundPeakWidth());

                // get the spectrum annotations
                Peptide currentPeptide = spectrumMatch.getBestAssumption().getPeptide();
                int identificationCharge = spectrumMatch.getBestAssumption().getIdentificationCharge().value;
                annotationPreferences.setCurrentSettings(currentPeptide,
                        identificationCharge, !currentSpectrumKey.equalsIgnoreCase(spectrumMatch.getKey()));
                ArrayList<IonMatch> annotations = annotator.getSpectrumAnnotation(annotationPreferences.getIonTypes(),
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
                spectrum.setYAxisZoomExcludesBackgroundPeaks(annotationPreferences.yAxisZoomExcludesBackgroundPeaks());

                int forwardIon = peptideShakerGUI.getSearchParameters().getIonSearched1();
                int rewindIon = peptideShakerGUI.getSearchParameters().getIonSearched2();
                
                // add de novo sequencing
                spectrum.addAutomaticDeNovoSequencing(currentPeptide, annotations, 
                                    forwardIon, rewindIon, annotationPreferences.getDeNovoCharge(), 
                                    annotationPreferences.showForwardIonDeNovoTags(), 
                                    annotationPreferences.showRewindIonDeNovoTags());

                spectrumChartJPanel.add(spectrum);
                peptideShakerGUI.updateAnnotationMenus(identificationCharge, currentPeptide);

                ((TitledBorder) spectrumAndFragmentIonPanel.getBorder()).setTitle(
                        PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + 
                        "Spectrum & Fragment Ions (" + currentPeptide.getModifiedSequenceAsString(true) + ")"
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
    private ArrayList<ModificationProfile> getModificationProfile(Peptide peptide, PSPtmScores scores) {

        ArrayList<ModificationProfile> profiles = new ArrayList<ModificationProfile>();

        if (scores != null) {
            for (String ptmName : scores.getScoredPTMs()) {

                Color ptmColor = peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(ptmName);
                ModificationProfile tempProfile = new ModificationProfile(ptmName, new double[peptide.getSequence().length()][2], ptmColor);

                PtmScoring locationScoring = scores.getPtmScoring(ptmName);

                // delta score
                String bestLocation = locationScoring.getBestDeltaScoreLocations();
                double bestScore;
                if (bestLocation != null) {
                    bestScore = locationScoring.getDeltaScore(bestLocation);
                    ArrayList<Integer> modificationSites = PtmScoring.getLocations(bestLocation);
                    for (int aa = 1; aa <= peptide.getSequence().length(); aa++) {
                        if (modificationSites.contains(aa)) {
                            tempProfile.getProfile()[aa - 1][ModificationProfile.DELTA_SCORE_ROW_INDEX] = bestScore;
                        }
                    }
                }

                // A-score
                bestLocation = locationScoring.getBestAScoreLocations();
                if (bestLocation != null) {
                    bestScore = locationScoring.getAScore(bestLocation);
                    ArrayList<Integer> modificationSites = PtmScoring.getLocations(bestLocation);
                    for (int aa = 1; aa <= peptide.getSequence().length(); aa++) {
                        if (modificationSites.contains(aa)) {
                            tempProfile.getProfile()[aa - 1][ModificationProfile.A_SCORE_ROW_INDEX] = bestScore;
                        }
                    }
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
     * otherwise the selecte peptide table is used
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
     * Returns the keys of the selected psms.
     *
     * @param relatedPeptide if true, the related psm table is used, otherwise
     * the selecte psm table is used
     * @return the keys of the selected psms
     */
    private ArrayList<String> getSelectedPsm(boolean relatedPeptide) {
        ArrayList<String> psmKey = new ArrayList<String>();
        try {
            PeptideMatch peptideMatch = identification.getPeptideMatch(getSelectedPeptide(relatedPeptide));
            if (relatedPeptide) {
                int[] selectedRows = relatedPsmsTable.getSelectedRows();
                for (int row : selectedRows) {
                    psmKey.add(peptideMatch.getSpectrumMatches().get(row));
                }
            } else {
                int[] selectedRows = selectedPsmsTable.getSelectedRows();
                for (int row : selectedRows) {
                    psmKey.add(peptideMatch.getSpectrumMatches().get(row));
                }
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        return psmKey;
    }

    /**
     * Returns the keys of the selected psms.
     *
     * @return the keys of the selected psms
     */
    private ArrayList<String> getSelectedPsm() {
        return getSelectedPsm(relatedSelected);
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
                        return probabilities.getGroupClass();
                    case 3:
                        return peptideShakerGUI.getIdentificationFeaturesGenerator().getColoredPeptideSequence(displayedPeptides.get(row), true);
                    case 4:
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getPeptideMatch(displayedPeptides.get(row)).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            return ptmScores.getPtmScoring(getSelectedModification()).getPtmSiteConfidence();
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
                        return probabilities.isValidated();
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
                        return probabilities.getGroupClass();
                    case 3:
                        return peptideShakerGUI.getIdentificationFeaturesGenerator().getColoredPeptideSequence(relatedPeptides.get(row), true);
                    case 4:
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getPeptideMatch(relatedPeptides.get(row)).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            return ptmScores.getPtmScoring(getSelectedModification()).getPtmSiteConfidence();
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
                        return probabilities.isValidated();
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
     * Table model for the modified PSMs table
     */
    private class SelectedPsmsTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (peptidesTable.getSelectedRow() != -1) {
                try {
                    return identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumCount();
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
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatches().get(row);
                        probabilities = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, probabilities);
                        return probabilities.isStarred();
                    case 2:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatches().get(row);
                        return identification.getSpectrumMatch(spectrumKey).getBestAssumption().getPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                    case 3:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatches().get(row);
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getSpectrumMatch(spectrumKey).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            return ptmScores.getPtmScoring(getSelectedModification()).getPtmSiteConfidence();
                        } else {
                            return PtmScoring.NOT_FOUND;
                        }
                    case 4:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatches().get(row);
                        return identification.getSpectrumMatch(spectrumKey).getBestAssumption().getIdentificationCharge().value;
                    case 5:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatches().get(row);
                        try {
                            Precursor precursor = peptideShakerGUI.getPrecursor(spectrumKey); // @TODO: there is sometimes an IOException when closing the tool...

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
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatches().get(row);
                        probabilities = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, probabilities);
                        return probabilities.isValidated();
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
     * Table model for the related PSMs table
     */
    private class RelatedPsmsTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (relatedPeptidesTable.getSelectedRow() != -1) {
                try {
                    return identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumCount();
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
            switch (column) { //@TODO: add scores, confidence and validation?
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
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches().get(row);
                        probabilities = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, probabilities);
                        return probabilities.isStarred();
                    case 2:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches().get(row);
                        return identification.getSpectrumMatch(spectrumKey).getBestAssumption().getPeptide().getModifiedSequenceAsHtml(
                                peptideShakerGUI.getSearchParameters().getModificationProfile().getPtmColors(), true);
                    case 3:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches().get(row);
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getSpectrumMatch(spectrumKey).getUrParam(ptmScores);
                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {
                            return ptmScores.getPtmScoring(getSelectedModification()).getPtmSiteConfidence();
                        } else {
                            return PtmScoring.NOT_FOUND;
                        }
                    case 4:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches().get(row);
                        return identification.getSpectrumMatch(spectrumKey).getBestAssumption().getIdentificationCharge().value;
                    case 5:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches().get(row);
                        Precursor precursor = peptideShakerGUI.getPrecursor(spectrumKey);
                        return precursor.getRt();
                    case 6:
                        spectrumKey = identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches().get(row);
                        probabilities = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, probabilities);
                        return probabilities.isValidated();
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
     * Returns the spectrum panel.
     *
     * @return the spectrum panel
     */
    public Component getSpectrum() {
        return (Component) spectrumJPanel.getComponent(1);
    }

    /**
     * Returns the current spectrum as an mgf string.
     *
     * @return the current spectrum as an mgf string
     */
    public String getSpectrumAsMgf() {

        String spectrumAsMgf = "";
        try {
            for (String spectrumKey : getSelectedPsm()) {
                MSnSpectrum currentSpectrum = peptideShakerGUI.getSpectrum(spectrumKey);
                spectrumAsMgf += currentSpectrum.asMgf() + System.getProperty("line.separator");
            }
            if (!spectrumAsMgf.isEmpty()) {
                return spectrumAsMgf;
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
        }
        return null;
    }

    /**
     * Updates the modification profile to the currently selected peptide.
     *
     * @param peptideMatch the peptide match to create the profile for
     * @param selectedPeptideProfile if true the selected peptide profile is
     * updated, otherwise the releated peptide profile is updated
     */
    private void updateModificationProfile(PeptideMatch peptideMatch, boolean selectedPeptideProfile) {

        try {
            PSPtmScores scores = new PSPtmScores();
            scores = (PSPtmScores) peptideMatch.getUrParam(scores);
            ArrayList<ModificationProfile> profiles = getModificationProfile(peptideMatch.getTheoreticPeptide(), scores);

            SequenceModificationPanel sequenceModificationPanel =
                    new SequenceModificationPanel(peptideMatch.getTheoreticPeptide().getNTerminal() + "-"
                    + peptideMatch.getTheoreticPeptide().getSequence()
                    + "-" + peptideMatch.getTheoreticPeptide().getCTerminal(),
                    profiles, true);

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
            ptmJTable.setValueAt(peptideShakerGUI.getSearchParameters().getModificationProfile().getColor(
                    (String) ptmJTable.getValueAt(i, ptmJTable.getColumn("PTM").getModelIndex())), i, ptmJTable.getColumn("  ").getModelIndex());
        }

        updateModificationProfiles(null);

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Redraws the modification profiles. For example if the ptm colors are
     * updated.
     *
     * @param progressDialog
     */
    public void updateModificationProfiles(ProgressDialogX progressDialog) {

        if (progressDialog != null) {
            progressDialog.setTitle("Updating Modification Profile. Please Wait...");
            progressDialog.setIndeterminate(true);
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
    private void copyTableContentToClipboard(TableIndex index) {

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
            progressDialog.setIndeterminate(true);
            progressDialog.setTitle("Copying to Clipboard. Please Wait...");

            new Thread(new Runnable() {

                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }).start();

            final Thread exportThread = new Thread("ExportThread") {

                @Override
                public void run() {
                    try {

                        OutputGenerator outputGenerator = new OutputGenerator(peptideShakerGUI);
                        String clipboardString = null;
                        
                        if (tableIndex == TableIndex.MODIFIED_PEPTIDES_TABLE) {
                            outputGenerator.getPeptidesOutput(
                                    null, displayedPeptides, null, true, false, true, true, true, true,
                                    true, true, true, true, true, true, true, true, false, false, false, null);
                        } else if (tableIndex == TableIndex.RELATED_PEPTIDES_TABLE) {
                            outputGenerator.getPeptidesOutput(
                                    null, relatedPeptides, null, true, false, true, true, true, true,
                                    true, true, true, true, true, true, true, true, false, false, false, null);
                        } else if (tableIndex == TableIndex.MODIFIED_PSMS_TABLE) {
                            outputGenerator.getPSMsOutput(
                                    null, identification.getPeptideMatch(getSelectedPeptide(false)).getSpectrumMatches(),
                                    true, false, true, true, true, true,
                                    true, true, true, true, true, true, true, false, false);
                        } else if (tableIndex == TableIndex.RELATED_PSMS_TABLE) {
                            outputGenerator.getPSMsOutput(
                                    null, identification.getPeptideMatch(getSelectedPeptide(true)).getSpectrumMatches(),
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
            };

            Thread appThread = new Thread() {

                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(exportThread);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            appThread.start();
        }
    }

    /**
     * Returns the given peptides table as a string.
     *
     * @param modifiedPeptides if true the modified peptides table is converted,
     * false converts the related peptides table.
     * @return the peptide table convered to a string
     */
    private String getPeptidesTableAsString(boolean modifiedPeptides) {

        String results = "\tPI\tSequence\tVariable Modification\tLocation Confidence\tPeptide Confidence\tPeptide Validated" + System.getProperty("line.separator");
        try {
            JTable table;
            ArrayList<String> peptides;

            if (modifiedPeptides) {
                table = peptidesTable;
                peptides = displayedPeptides;
            } else {
                table = relatedPeptidesTable;
                peptides = relatedPeptides;
            }

            progressDialog.setMaxProgressValue(table.getRowCount());

            for (int i = 0; i < table.getRowCount(); i++) {

                if (progressDialog.isRunCanceled()) {
                    return "";
                }

                results += (i + 1) + "\t";
                progressDialog.increaseProgressValue();

                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideMatchParameter(peptides.get(i), probabilities);
                results += proteinInferenceTooltipMap.get(probabilities.getGroupClass()) + "\t";

                results += identification.getPeptideMatch(peptides.get(i)).getTheoreticPeptide().getModifiedSequenceAsString(true) + "\t";
                results += OutputGenerator.getPeptideModificationsAsString(identification.getPeptideMatch(peptides.get(i)).getTheoreticPeptide()) + "\t";
                results += OutputGenerator.getPeptideModificationLocations(identification.getPeptideMatch(peptides.get(i)).getTheoreticPeptide(),
                        identification.getPeptideMatch(identification.getPeptideMatch(peptides.get(i)).getTheoreticPeptide().getKey())) + "\t";

                probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideMatchParameter(peptides.get(i), probabilities);
                results += probabilities.getPeptideConfidence() + "\t";

                probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getPeptideMatchParameter(peptides.get(i), probabilities);
                results += probabilities.isValidated() + System.getProperty("line.separator");
            }

        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            results = "Error";
        }
        return results;
    }

    /**
     * Returns the given psm table as a string.
     *
     * @param modifiedPeptides if true the modified psms table is converted,
     * false converts the related psms table.
     * @return the psm table convered to a string
     */
    private String getPsmTableAsString(boolean modifiedPeptides) {

        String results = "\tSequence\tVariable Modification\tLocation Confidence\tCharge\tPSM Validated" + System.getProperty("line.separator");

        try {

            JTable table;

            if (modifiedPeptides) {
                table = selectedPsmsTable;
            } else {
                table = relatedPsmsTable;
            }

            progressDialog.setMaxProgressValue(table.getRowCount());

            for (int i = 0; i < table.getRowCount(); i++) {

                if (progressDialog.isRunCanceled()) {
                    return "";
                }

                results += (i + 1) + "\t";
                progressDialog.increaseProgressValue();

                String spectrumKey = identification.getPeptideMatch(getSelectedPeptide(!modifiedPeptides)).getSpectrumMatches().get(i);
                results += identification.getSpectrumMatch(spectrumKey).getBestAssumption().getPeptide().getModifiedSequenceAsString(true) + "\t";
                results += OutputGenerator.getPeptideModificationsAsString(identification.getSpectrumMatch(spectrumKey).getBestAssumption().getPeptide()) + "\t";
                results += OutputGenerator.getPeptideModificationLocations(identification.getSpectrumMatch(spectrumKey).getBestAssumption().getPeptide(),
                        identification.getPeptideMatch(identification.getSpectrumMatch(spectrumKey).getBestAssumption().getPeptide().getKey())) + "\t";
                results += identification.getSpectrumMatch(spectrumKey).getBestAssumption().getIdentificationCharge().value + "\t";

                PSParameter probabilities = new PSParameter();
                spectrumKey = identification.getPeptideMatch(getSelectedPeptide(!modifiedPeptides)).getSpectrumMatches().get(i);
                probabilities = (PSParameter) peptideShakerGUI.getIdentification().getSpectrumMatchParameter(spectrumKey, probabilities);
                results += probabilities.isValidated() + System.getProperty("line.separator");
            }
        } catch (Exception e) {
            peptideShakerGUI.catchException(e);
            results = "Error";
        }

        return results;
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

        spectrumTabbedPane.setToolTipText("<html>Accuracy: " + Util.roundDouble(accuracy, 2) + " Da<br>"
                + "Level: " + intensitySlider.getValue() + "%</html>");

        // show the tooltip now
        ToolTipManager.sharedInstance().mouseMoved(
                new MouseEvent(spectrumTabbedPane, 0, 0, 0,
                spectrumTabbedPane.getWidth() - 50, spectrumTabbedPane.getY() + 20, // X-Y of the mouse for the tool tip
                0, false));
    }

    /**
     * Provides to the PeptideShakerGUI instance the currently selected peptide
     * and psm
     */
    public void newItemSelection() {
        String peptideKey = getSelectedPeptide();
        if (peptideKey.equals("")) {
            peptideKey = PeptideShakerGUI.NO_SELECTION;
        }
        String psmKey = getSelectedPsm().get(0);
        if (psmKey.equals("")) {
            psmKey = PeptideShakerGUI.NO_SELECTION;
        }
        peptideShakerGUI.setSelectedItems(PeptideShakerGUI.NO_SELECTION, peptideKey, psmKey);
    }

    /**
     * Update the psm modification profiles table.
     *
     * @param progressDialog
     */
    private void updateModificationProfilesTable(ProgressDialogX progressDialog) {

        if (spectrumTabbedPane.isEnabledAt(0)) {
            progressDialog.setTitle("Updating Modification Profile Table. Please Wait...");
            progressDialog.setIndeterminate(true);

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

                    // add the psm scores (a score and delta score)
                    for (int i = 0; i < selectedPsmsTable.getRowCount(); i++) {

                        String spectrumKey = peptideMatch.getSpectrumMatches().get(i);
                        PSPtmScores ptmScores = new PSPtmScores();
                        ptmScores = (PSPtmScores) identification.getSpectrumMatch(spectrumKey).getUrParam(ptmScores);

                        if (ptmScores != null && ptmScores.getPtmScoring(getSelectedModification()) != null) {

                            ((DefaultTableModel) psmAScoresTable.getModel()).addRow(new Object[]{(i + 1)});
                            ((DefaultTableModel) psmDeltaScoresTable.getModel()).addRow(new Object[]{(i + 1)});

                            ArrayList<String> deltaScoreLocations = ptmScores.getPtmScoring(getSelectedModification()).getDeltaScorelocations();

                            for (int j = 0; j < deltaScoreLocations.size(); j++) {

                                String[] locations = deltaScoreLocations.get(j).split("\\|");

                                for (int k = 0; k < locations.length; k++) {
                                    int location = new Integer(locations[k]);
                                    psmDeltaScoresTable.setValueAt(ptmScores.getPtmScoring(getSelectedModification()).getDeltaScore(deltaScoreLocations.get(j)), i, location);
                                }
                            }

                            ArrayList<String> aScoreLocations = ptmScores.getPtmScoring(getSelectedModification()).getAScoreLocations();

                            for (int j = 0; j < aScoreLocations.size(); j++) {

                                String[] locations = aScoreLocations.get(j).split("\\|");

                                for (int k = 0; k < locations.length; k++) {
                                    int location = new Integer(locations[k]);
                                    psmAScoresTable.setValueAt(ptmScores.getPtmScoring(getSelectedModification()).getAScore(aScoreLocations.get(j)), i, location);
                                }
                            }
                        } else {
                            ((DefaultTableModel) psmAScoresTable.getModel()).addRow(new Object[]{(i + 1)});
                            ((DefaultTableModel) psmDeltaScoresTable.getModel()).addRow(new Object[]{(i + 1)});
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
     * Update the psm delt and a score table cell renderers.
     */
    private void updatePsmScoresCellRenderers() {
        for (int i = 1; i < psmAScoresTable.getColumnCount(); i++) {
            if (peptideShakerGUI.showSparklines()) {
                psmAScoresTable.getColumn(psmAScoresTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d, 100d));
                ((JSparklinesBarChartTableCellRenderer) psmAScoresTable.getColumn(psmAScoresTable.getColumnName(i)).getCellRenderer()).showAsHeatMap(ColorGradient.GreenWhiteBlue);
            } else {
                ((JSparklinesBarChartTableCellRenderer) psmAScoresTable.getColumn(psmAScoresTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }

        for (int i = 1; i < psmDeltaScoresTable.getColumnCount(); i++) {
            if (peptideShakerGUI.showSparklines()) {
                psmDeltaScoresTable.getColumn(psmDeltaScoresTable.getColumnName(i)).setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 0d, 100d));
                ((JSparklinesBarChartTableCellRenderer) psmDeltaScoresTable.getColumn(psmDeltaScoresTable.getColumnName(i)).getCellRenderer()).showAsHeatMap(ColorGradient.GreenWhiteBlue);
            } else {
                ((JSparklinesBarChartTableCellRenderer) psmDeltaScoresTable.getColumn(psmDeltaScoresTable.getColumnName(i)).getCellRenderer()).showNumbers(true);
            }
        }
    }
}
