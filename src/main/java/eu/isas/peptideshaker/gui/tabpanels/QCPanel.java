package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.quantification.spectrumcounting.SpectrumCountingMethod;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.util.experiment.identification.filtering.PeptideAssumptionFilter;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.utils.ModificationUtils;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.parameters.peptide_shaker.ProjectType;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * This panel will display QC statistics for the current project.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class QCPanel extends javax.swing.JPanel {

    /**
     * The main PeptideShaker GUI.
     */
    private final PeptideShakerGUI peptideShakerGUI;
    /**
     * color for the plots (validated targets, validated decoy, non validated
     * target, non validated decoy).
     */
    private final Color[] histogramColors;
    /**
     * A simple progress dialog.
     */
    private static ProgressDialogX progressDialog;
    /**
     * Values of the validated target hits.
     */
    public ArrayList<Double> validatedValues;
    /**
     * Values of the doubtful validated target hits.
     */
    public ArrayList<Double> validatedDoubtfulValues;
    /**
     * Values of the non validated target hits.
     */
    public ArrayList<Double> nonValidatedValues;
    /**
     * Values of the validated decoy hits.
     */
    public ArrayList<Double> validatedDecoyValues;
    /**
     * Values of the non validated decoy hits.
     */
    public ArrayList<Double> nonValidatedDecoyValues;
    /**
     * The current maximum value to be plotted.
     */
    private double maxValue = Double.MAX_VALUE;

    /**
     * The list of supported plot types.
     */
    private enum PlotType {

        Protein_Validated_Peptides,
        Protein_MS2_QuantScores,
        Protein_Sequence_Coverage,
        Protein_Sequence_Length,
        Peptide_Validated_PSMs,
        Peptide_Missed_Cleavages,
        Peptide_Cleavages_Sites,
        Peptide_Length,
        Peptide_Modifications,
        Peptide_Modification_Efficiency,
        Peptide_Modification_Specificity,
        PSM_Precursor_Mass_Error,
        PSM_Precursor_Charge,
        None
    }
    /**
     * The currently shown protein plot type.
     */
    private PlotType currentProteinPlotType = PlotType.None;
    /**
     * The currently shown peptide plot type.
     */
    private PlotType currentPeptidePlotType = PlotType.None;
    /**
     * The currently shown PSM plot type.
     */
    private PlotType currentPsmPlotType = PlotType.None;

    /**
     * Creates a new QCPanel.
     *
     * @param parent the PeptideShakerGUI parent
     */
    public QCPanel(PeptideShakerGUI parent) {

        this.peptideShakerGUI = parent;
        initComponents();

        // set the histogram colors
        histogramColors = new Color[5];
        histogramColors[0] = peptideShakerGUI.getSparklineColor(); // Confident True Positives
        histogramColors[1] = peptideShakerGUI.getUtilitiesUserParameters().getSparklineColorDoubtful(); // Doubtful True Positives
        histogramColors[2] = peptideShakerGUI.getUtilitiesUserParameters().getSparklineColorFalsePositives(); // False Positives
        histogramColors[3] = peptideShakerGUI.getUtilitiesUserParameters().getSparklineColorPossible(); // False Negatives
        histogramColors[4] = Color.lightGray; // True Negatives

        // make the tabs in the spectrum tabbed pane go from right to left
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        proteinButtonGroup = new javax.swing.ButtonGroup();
        peptideButtonGroup = new javax.swing.ButtonGroup();
        psmButtonGroup = new javax.swing.ButtonGroup();
        qcPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        psmPanel = new javax.swing.JPanel();
        psmPlotTypePanel = new javax.swing.JPanel();
        psmPrecursorMassErrorJRadioButton = new javax.swing.JRadioButton();
        psmPrecursorChargeJRadioButton = new javax.swing.JRadioButton();
        psmPlotLayeredPane = new javax.swing.JLayeredPane();
        psmQCPlotPanel = new javax.swing.JPanel();
        psmPlotHelpJButton = new javax.swing.JButton();
        exportPsmPlotJButton = new javax.swing.JButton();
        peptidePanel = new javax.swing.JPanel();
        peptidesPlotTypePanel = new javax.swing.JPanel();
        peptideValidatedPsmsJRadioButton = new javax.swing.JRadioButton();
        peptideMissedCleavagesJRadioButton = new javax.swing.JRadioButton();
        peptideLengthJRadioButton = new javax.swing.JRadioButton();
        peptideModificationEfficiencyJRadioButton = new javax.swing.JRadioButton();
        peptideModificationsJRadioButton = new javax.swing.JRadioButton();
        peptideModificationSpecificityJRadioButton = new javax.swing.JRadioButton();
        peptideCleavageSitesJRadioButton = new javax.swing.JRadioButton();
        peptidesPlotLayeredPane = new javax.swing.JLayeredPane();
        peptideQCPlotPanel = new javax.swing.JPanel();
        peptidesPlotHelpJButton = new javax.swing.JButton();
        exportPeptidesPlotJButton = new javax.swing.JButton();
        proteinPanel = new javax.swing.JPanel();
        proteinsPlotLayeredPane = new javax.swing.JLayeredPane();
        proteinQCPlotPanel = new javax.swing.JPanel();
        proteinsPlotHelpJButton = new javax.swing.JButton();
        exportProteinsPlotJButton = new javax.swing.JButton();
        proteinPlotTypePanel = new javax.swing.JPanel();
        proteinSpectrumCountingScoreJRadioButton = new javax.swing.JRadioButton();
        proteinNumberValidatedPeptidesJRadioButton = new javax.swing.JRadioButton();
        proteinSequenceCoverageJRadioButton = new javax.swing.JRadioButton();
        proteinSequenceLengthJRadioButton = new javax.swing.JRadioButton();

        setBackground(new java.awt.Color(255, 255, 255));

        qcPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Quality Control Plots", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("sansserif", 1, 14))); // NOI18N
        qcPanel.setOpaque(false);

        tabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
        tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabbedPaneStateChanged(evt);
            }
        });

        psmPanel.setBackground(new java.awt.Color(255, 255, 255));

        psmPlotTypePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Plot Type", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("sansserif", 1, 14))); // NOI18N
        psmPlotTypePanel.setOpaque(false);

        psmButtonGroup.add(psmPrecursorMassErrorJRadioButton);
        psmPrecursorMassErrorJRadioButton.setSelected(true);
        psmPrecursorMassErrorJRadioButton.setText("Precursor m/z Error");
        psmPrecursorMassErrorJRadioButton.setIconTextGap(10);
        psmPrecursorMassErrorJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmPrecursorMassErrorJRadioButtonActionPerformed(evt);
            }
        });

        psmButtonGroup.add(psmPrecursorChargeJRadioButton);
        psmPrecursorChargeJRadioButton.setText("Precursor Charge");
        psmPrecursorChargeJRadioButton.setIconTextGap(10);
        psmPrecursorChargeJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmPrecursorChargeJRadioButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout psmPlotTypePanelLayout = new javax.swing.GroupLayout(psmPlotTypePanel);
        psmPlotTypePanel.setLayout(psmPlotTypePanelLayout);
        psmPlotTypePanelLayout.setHorizontalGroup(
            psmPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmPlotTypePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmPrecursorMassErrorJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(psmPrecursorChargeJRadioButton)
                .addContainerGap(767, Short.MAX_VALUE))
        );
        psmPlotTypePanelLayout.setVerticalGroup(
            psmPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmPlotTypePanelLayout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addGroup(psmPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(psmPrecursorMassErrorJRadioButton)
                    .addComponent(psmPrecursorChargeJRadioButton))
                .addContainerGap())
        );

        psmPlotLayeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                psmPlotLayeredPaneComponentResized(evt);
            }
        });

        psmQCPlotPanel.setOpaque(false);
        psmQCPlotPanel.setLayout(new javax.swing.BoxLayout(psmQCPlotPanel, javax.swing.BoxLayout.LINE_AXIS));
        psmPlotLayeredPane.add(psmQCPlotPanel);
        psmQCPlotPanel.setBounds(0, 0, 650, 420);

        psmPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        psmPlotHelpJButton.setToolTipText("Help");
        psmPlotHelpJButton.setBorder(null);
        psmPlotHelpJButton.setBorderPainted(false);
        psmPlotHelpJButton.setContentAreaFilled(false);
        psmPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        psmPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                psmPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                psmPlotHelpJButtonMouseExited(evt);
            }
        });
        psmPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                psmPlotHelpJButtonActionPerformed(evt);
            }
        });
        psmPlotLayeredPane.setLayer(psmPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        psmPlotLayeredPane.add(psmPlotHelpJButton);
        psmPlotHelpJButton.setBounds(640, 0, 10, 25);

        exportPsmPlotJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPsmPlotJButton.setToolTipText("Export");
        exportPsmPlotJButton.setBorder(null);
        exportPsmPlotJButton.setBorderPainted(false);
        exportPsmPlotJButton.setContentAreaFilled(false);
        exportPsmPlotJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPsmPlotJButton.setEnabled(false);
        exportPsmPlotJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPsmPlotJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPsmPlotJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPsmPlotJButtonMouseExited(evt);
            }
        });
        exportPsmPlotJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPsmPlotJButtonActionPerformed(evt);
            }
        });
        psmPlotLayeredPane.setLayer(exportPsmPlotJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        psmPlotLayeredPane.add(exportPsmPlotJButton);
        exportPsmPlotJButton.setBounds(630, 0, 10, 25);

        javax.swing.GroupLayout psmPanelLayout = new javax.swing.GroupLayout(psmPanel);
        psmPanel.setLayout(psmPanelLayout);
        psmPanelLayout.setHorizontalGroup(
            psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(psmPlotTypePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(psmPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(psmPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1005, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        psmPanelLayout.setVerticalGroup(
            psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, psmPanelLayout.createSequentialGroup()
                .addContainerGap(446, Short.MAX_VALUE)
                .addComponent(psmPlotTypePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(psmPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(psmPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                    .addGap(93, 93, 93)))
        );

        tabbedPane.addTab("PSMs", psmPanel);

        peptidePanel.setBackground(new java.awt.Color(255, 255, 255));

        peptidesPlotTypePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot Type"));
        peptidesPlotTypePanel.setOpaque(false);

        peptideButtonGroup.add(peptideValidatedPsmsJRadioButton);
        peptideValidatedPsmsJRadioButton.setSelected(true);
        peptideValidatedPsmsJRadioButton.setText("# Validated PSMs");
        peptideValidatedPsmsJRadioButton.setIconTextGap(10);
        peptideValidatedPsmsJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideValidatedPsmsJRadioButtonActionPerformed(evt);
            }
        });

        peptideButtonGroup.add(peptideMissedCleavagesJRadioButton);
        peptideMissedCleavagesJRadioButton.setText("Missed Cleavages");
        peptideMissedCleavagesJRadioButton.setIconTextGap(10);
        peptideMissedCleavagesJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideMissedCleavagesJRadioButtonActionPerformed(evt);
            }
        });

        peptideButtonGroup.add(peptideLengthJRadioButton);
        peptideLengthJRadioButton.setText("Peptide Length");
        peptideLengthJRadioButton.setIconTextGap(10);
        peptideLengthJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideLengthJRadioButtonActionPerformed(evt);
            }
        });

        peptideButtonGroup.add(peptideModificationEfficiencyJRadioButton);
        peptideModificationEfficiencyJRadioButton.setText("Modification Efficiency");
        peptideModificationEfficiencyJRadioButton.setIconTextGap(10);
        peptideModificationEfficiencyJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideModificationEfficiencyJRadioButtonActionPerformed(evt);
            }
        });

        peptideButtonGroup.add(peptideModificationsJRadioButton);
        peptideModificationsJRadioButton.setText("# Modifications");
        peptideModificationsJRadioButton.setIconTextGap(10);
        peptideModificationsJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideModificationsJRadioButtonActionPerformed(evt);
            }
        });

        peptideButtonGroup.add(peptideModificationSpecificityJRadioButton);
        peptideModificationSpecificityJRadioButton.setText("Modification Specificity");
        peptideModificationSpecificityJRadioButton.setIconTextGap(10);
        peptideModificationSpecificityJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideModificationSpecificityJRadioButtonActionPerformed(evt);
            }
        });

        peptideButtonGroup.add(peptideCleavageSitesJRadioButton);
        peptideCleavageSitesJRadioButton.setText("Cleavages Sites");
        peptideCleavageSitesJRadioButton.setToolTipText("Not yet implemented");
        peptideCleavageSitesJRadioButton.setEnabled(false);
        peptideCleavageSitesJRadioButton.setIconTextGap(10);
        peptideCleavageSitesJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptideCleavageSitesJRadioButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout peptidesPlotTypePanelLayout = new javax.swing.GroupLayout(peptidesPlotTypePanel);
        peptidesPlotTypePanel.setLayout(peptidesPlotTypePanelLayout);
        peptidesPlotTypePanelLayout.setHorizontalGroup(
            peptidesPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesPlotTypePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideValidatedPsmsJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peptideMissedCleavagesJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peptideCleavageSitesJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peptideLengthJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peptideModificationsJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peptideModificationEfficiencyJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(peptideModificationSpecificityJRadioButton)
                .addContainerGap(84, Short.MAX_VALUE))
        );
        peptidesPlotTypePanelLayout.setVerticalGroup(
            peptidesPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptidesPlotTypePanelLayout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addGroup(peptidesPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(peptideValidatedPsmsJRadioButton)
                    .addComponent(peptideMissedCleavagesJRadioButton)
                    .addComponent(peptideLengthJRadioButton)
                    .addComponent(peptideModificationEfficiencyJRadioButton)
                    .addComponent(peptideModificationsJRadioButton)
                    .addComponent(peptideModificationSpecificityJRadioButton)
                    .addComponent(peptideCleavageSitesJRadioButton))
                .addContainerGap())
        );

        peptidesPlotLayeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                peptidesPlotLayeredPaneComponentResized(evt);
            }
        });

        peptideQCPlotPanel.setOpaque(false);
        peptideQCPlotPanel.setLayout(new javax.swing.BoxLayout(peptideQCPlotPanel, javax.swing.BoxLayout.LINE_AXIS));
        peptidesPlotLayeredPane.add(peptideQCPlotPanel);
        peptideQCPlotPanel.setBounds(0, 0, 660, 420);

        peptidesPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        peptidesPlotHelpJButton.setToolTipText("Help");
        peptidesPlotHelpJButton.setBorder(null);
        peptidesPlotHelpJButton.setBorderPainted(false);
        peptidesPlotHelpJButton.setContentAreaFilled(false);
        peptidesPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        peptidesPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                peptidesPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                peptidesPlotHelpJButtonMouseExited(evt);
            }
        });
        peptidesPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                peptidesPlotHelpJButtonActionPerformed(evt);
            }
        });
        peptidesPlotLayeredPane.setLayer(peptidesPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        peptidesPlotLayeredPane.add(peptidesPlotHelpJButton);
        peptidesPlotHelpJButton.setBounds(640, 0, 10, 25);

        exportPeptidesPlotJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesPlotJButton.setToolTipText("Export");
        exportPeptidesPlotJButton.setBorder(null);
        exportPeptidesPlotJButton.setBorderPainted(false);
        exportPeptidesPlotJButton.setContentAreaFilled(false);
        exportPeptidesPlotJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPeptidesPlotJButton.setEnabled(false);
        exportPeptidesPlotJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPeptidesPlotJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPeptidesPlotJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPeptidesPlotJButtonMouseExited(evt);
            }
        });
        exportPeptidesPlotJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPeptidesPlotJButtonActionPerformed(evt);
            }
        });
        peptidesPlotLayeredPane.setLayer(exportPeptidesPlotJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        peptidesPlotLayeredPane.add(exportPeptidesPlotJButton);
        exportPeptidesPlotJButton.setBounds(630, 0, 10, 25);

        javax.swing.GroupLayout peptidePanelLayout = new javax.swing.GroupLayout(peptidePanel);
        peptidePanel.setLayout(peptidePanelLayout);
        peptidePanelLayout.setHorizontalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptidePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptidesPlotTypePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(peptidePanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(peptidesPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1005, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        peptidePanelLayout.setVerticalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, peptidePanelLayout.createSequentialGroup()
                .addContainerGap(449, Short.MAX_VALUE)
                .addComponent(peptidesPlotTypePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(peptidePanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(peptidesPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                    .addGap(93, 93, 93)))
        );

        tabbedPane.addTab("Peptides", peptidePanel);

        proteinPanel.setBackground(new java.awt.Color(255, 255, 255));

        proteinsPlotLayeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                proteinsPlotLayeredPaneComponentResized(evt);
            }
        });

        proteinQCPlotPanel.setOpaque(false);
        proteinQCPlotPanel.setLayout(new javax.swing.BoxLayout(proteinQCPlotPanel, javax.swing.BoxLayout.LINE_AXIS));
        proteinsPlotLayeredPane.add(proteinQCPlotPanel);
        proteinQCPlotPanel.setBounds(0, 0, 0, 0);

        proteinsPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        proteinsPlotHelpJButton.setToolTipText("Help");
        proteinsPlotHelpJButton.setBorder(null);
        proteinsPlotHelpJButton.setBorderPainted(false);
        proteinsPlotHelpJButton.setContentAreaFilled(false);
        proteinsPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        proteinsPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                proteinsPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinsPlotHelpJButtonMouseExited(evt);
            }
        });
        proteinsPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinsPlotHelpJButtonActionPerformed(evt);
            }
        });
        proteinsPlotLayeredPane.setLayer(proteinsPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        proteinsPlotLayeredPane.add(proteinsPlotHelpJButton);
        proteinsPlotHelpJButton.setBounds(640, 0, 10, 25);

        exportProteinsPlotJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsPlotJButton.setToolTipText("Export");
        exportProteinsPlotJButton.setBorder(null);
        exportProteinsPlotJButton.setBorderPainted(false);
        exportProteinsPlotJButton.setContentAreaFilled(false);
        exportProteinsPlotJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsPlotJButton.setEnabled(false);
        exportProteinsPlotJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportProteinsPlotJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportProteinsPlotJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportProteinsPlotJButtonMouseExited(evt);
            }
        });
        exportProteinsPlotJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProteinsPlotJButtonActionPerformed(evt);
            }
        });
        proteinsPlotLayeredPane.setLayer(exportProteinsPlotJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        proteinsPlotLayeredPane.add(exportProteinsPlotJButton);
        exportProteinsPlotJButton.setBounds(630, 0, 10, 25);

        proteinPlotTypePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot Type"));
        proteinPlotTypePanel.setOpaque(false);

        proteinButtonGroup.add(proteinSpectrumCountingScoreJRadioButton);
        proteinSpectrumCountingScoreJRadioButton.setText("MS2 Quantification Scores");
        proteinSpectrumCountingScoreJRadioButton.setIconTextGap(10);
        proteinSpectrumCountingScoreJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinSpectrumCountingScoreJRadioButtonActionPerformed(evt);
            }
        });

        proteinButtonGroup.add(proteinNumberValidatedPeptidesJRadioButton);
        proteinNumberValidatedPeptidesJRadioButton.setSelected(true);
        proteinNumberValidatedPeptidesJRadioButton.setText("#Validated Peptides");
        proteinNumberValidatedPeptidesJRadioButton.setIconTextGap(10);
        proteinNumberValidatedPeptidesJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinNumberValidatedPeptidesJRadioButtonActionPerformed(evt);
            }
        });

        proteinButtonGroup.add(proteinSequenceCoverageJRadioButton);
        proteinSequenceCoverageJRadioButton.setText("Sequence Coverage");
        proteinSequenceCoverageJRadioButton.setIconTextGap(10);
        proteinSequenceCoverageJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinSequenceCoverageJRadioButtonActionPerformed(evt);
            }
        });

        proteinButtonGroup.add(proteinSequenceLengthJRadioButton);
        proteinSequenceLengthJRadioButton.setText("Sequence Length");
        proteinSequenceLengthJRadioButton.setIconTextGap(10);
        proteinSequenceLengthJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinSequenceLengthJRadioButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout proteinPlotTypePanelLayout = new javax.swing.GroupLayout(proteinPlotTypePanel);
        proteinPlotTypePanel.setLayout(proteinPlotTypePanelLayout);
        proteinPlotTypePanelLayout.setHorizontalGroup(
            proteinPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinPlotTypePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinNumberValidatedPeptidesJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(proteinSequenceCoverageJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(proteinSequenceLengthJRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(proteinSpectrumCountingScoreJRadioButton)
                .addContainerGap(434, Short.MAX_VALUE))
        );
        proteinPlotTypePanelLayout.setVerticalGroup(
            proteinPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinPlotTypePanelLayout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addGroup(proteinPlotTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proteinNumberValidatedPeptidesJRadioButton)
                    .addComponent(proteinSpectrumCountingScoreJRadioButton)
                    .addComponent(proteinSequenceCoverageJRadioButton)
                    .addComponent(proteinSequenceLengthJRadioButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout proteinPanelLayout = new javax.swing.GroupLayout(proteinPanel);
        proteinPanel.setLayout(proteinPanelLayout);
        proteinPanelLayout.setHorizontalGroup(
            proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(proteinsPlotLayeredPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1005, Short.MAX_VALUE)
                    .addComponent(proteinPlotTypePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        proteinPanelLayout.setVerticalGroup(
            proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinsPlotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
                .addGap(4, 4, 4)
                .addComponent(proteinPlotTypePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tabbedPane.addTab("Proteins", proteinPanel);

        tabbedPane.setSelectedIndex(2);

        javax.swing.GroupLayout qcPanelLayout = new javax.swing.GroupLayout(qcPanel);
        qcPanel.setLayout(qcPanelLayout);
        qcPanelLayout.setHorizontalGroup(
            qcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(qcPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane)
                .addContainerGap())
        );
        qcPanelLayout.setVerticalGroup(
            qcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(qcPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 564, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(qcPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(qcPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the protein QC plot.
     *
     * @param evt
     */
    private void proteinNumberValidatedPeptidesJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinNumberValidatedPeptidesJRadioButtonActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updateProteinQCPlot();
            exportProteinsPlotJButton.setEnabled(true);
        }
    }//GEN-LAST:event_proteinNumberValidatedPeptidesJRadioButtonActionPerformed

    /**
     * Update the protein QC plot.
     *
     * @param evt
     */
    private void proteinSpectrumCountingScoreJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinSpectrumCountingScoreJRadioButtonActionPerformed
        proteinNumberValidatedPeptidesJRadioButtonActionPerformed(evt);
    }//GEN-LAST:event_proteinSpectrumCountingScoreJRadioButtonActionPerformed

    /**
     * Update the protein QC plot.
     *
     * @param evt
     */
    private void proteinSequenceCoverageJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinSequenceCoverageJRadioButtonActionPerformed
        proteinNumberValidatedPeptidesJRadioButtonActionPerformed(evt);
    }//GEN-LAST:event_proteinSequenceCoverageJRadioButtonActionPerformed

    /**
     * Update the peptide QC plot.
     *
     * @param evt
     */
    private void peptideMissedCleavagesJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideMissedCleavagesJRadioButtonActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updatePeptideQCPlot();
            exportPeptidesPlotJButton.setEnabled(true);
        }
    }//GEN-LAST:event_peptideMissedCleavagesJRadioButtonActionPerformed

    /**
     * Update the peptide QC plot.
     *
     * @param evt
     */
    private void peptideValidatedPsmsJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideValidatedPsmsJRadioButtonActionPerformed
        peptideMissedCleavagesJRadioButtonActionPerformed(evt);
    }//GEN-LAST:event_peptideValidatedPsmsJRadioButtonActionPerformed

    /**
     * Update the PSM QC plot.
     *
     * @param evt
     */
    private void psmPrecursorMassErrorJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmPrecursorMassErrorJRadioButtonActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updatePsmQCPlot();
            exportPsmPlotJButton.setEnabled(true);
        }
    }//GEN-LAST:event_psmPrecursorMassErrorJRadioButtonActionPerformed

    /**
     * Update the PSM QC plot.
     *
     * @param evt
     */
    private void psmPrecursorChargeJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmPrecursorChargeJRadioButtonActionPerformed
        psmPrecursorMassErrorJRadioButtonActionPerformed(evt);
    }//GEN-LAST:event_psmPrecursorChargeJRadioButtonActionPerformed

    /**
     * Resize the PSM plot area.
     *
     * @param evt
     */
    private void psmPlotLayeredPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_psmPlotLayeredPaneComponentResized

        // resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // move the icons
                psmPlotLayeredPane.getComponent(0).setBounds(
                        psmPlotLayeredPane.getWidth() - psmPlotLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        psmPlotLayeredPane.getComponent(0).getWidth(),
                        psmPlotLayeredPane.getComponent(0).getHeight());

                psmPlotLayeredPane.getComponent(1).setBounds(
                        psmPlotLayeredPane.getWidth() - psmPlotLayeredPane.getComponent(1).getWidth() - 25,
                        -3,
                        psmPlotLayeredPane.getComponent(1).getWidth(),
                        psmPlotLayeredPane.getComponent(1).getHeight());

                // resize the plot area
                psmPlotLayeredPane.getComponent(2).setBounds(0, 0, psmPlotLayeredPane.getWidth(), psmPlotLayeredPane.getHeight());
                psmPlotLayeredPane.revalidate();
                psmPlotLayeredPane.repaint();
            }
        });

    }//GEN-LAST:event_psmPlotLayeredPaneComponentResized

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportPsmPlotJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPsmPlotJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPsmPlotJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportPsmPlotJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPsmPlotJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPsmPlotJButtonMouseExited

    /**
     * Export the plot.
     *
     * @param evt
     */
    private void exportPsmPlotJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPsmPlotJButtonActionPerformed
        new ExportGraphicsDialog(
                peptideShakerGUI,
                peptideShakerGUI.getNormalIcon(),
                peptideShakerGUI.getWaitingIcon(),
                true,
                psmQCPlotPanel,
                peptideShakerGUI.getLastSelectedFolder()
        );
    }//GEN-LAST:event_exportPsmPlotJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void psmPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_psmPlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void psmPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_psmPlotHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void psmPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_psmPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(
                peptideShakerGUI,
                getClass().getResource("/helpFiles/QCPlots.html"), "#PSM",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Quality Control Plots - Help"
        );
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_psmPlotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void peptidesPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_peptidesPlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void peptidesPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptidesPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesPlotHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void peptidesPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptidesPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(
                peptideShakerGUI,
                getClass().getResource("/helpFiles/QCPlots.html"),
                "#Peptide",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Quality Control Plots - Help"
        );
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_peptidesPlotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportPeptidesPlotJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPeptidesPlotJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPeptidesPlotJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportPeptidesPlotJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPeptidesPlotJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPeptidesPlotJButtonMouseExited

    /**
     * Export the plot.
     *
     * @param evt
     */
    private void exportPeptidesPlotJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPeptidesPlotJButtonActionPerformed
        new ExportGraphicsDialog(
                peptideShakerGUI,
                peptideShakerGUI.getNormalIcon(),
                peptideShakerGUI.getWaitingIcon(),
                true,
                peptideQCPlotPanel,
                peptideShakerGUI.getLastSelectedFolder()
        );
    }//GEN-LAST:event_exportPeptidesPlotJButtonActionPerformed

    /**
     * Resize the peptides plot area.
     *
     * @param evt
     */
    private void peptidesPlotLayeredPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_peptidesPlotLayeredPaneComponentResized

// resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // move the icons
                peptidesPlotLayeredPane.getComponent(0).setBounds(
                        peptidesPlotLayeredPane.getWidth() - peptidesPlotLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        peptidesPlotLayeredPane.getComponent(0).getWidth(),
                        peptidesPlotLayeredPane.getComponent(0).getHeight()
                );

                peptidesPlotLayeredPane.getComponent(1).setBounds(
                        peptidesPlotLayeredPane.getWidth() - peptidesPlotLayeredPane.getComponent(1).getWidth() - 25,
                        -3,
                        peptidesPlotLayeredPane.getComponent(1).getWidth(),
                        peptidesPlotLayeredPane.getComponent(1).getHeight()
                );

                // resize the plot area
                peptidesPlotLayeredPane.getComponent(2).setBounds(
                        0,
                        0,
                        peptidesPlotLayeredPane.getWidth(),
                        peptidesPlotLayeredPane.getHeight()
                );

                peptidesPlotLayeredPane.revalidate();
                peptidesPlotLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_peptidesPlotLayeredPaneComponentResized

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void proteinsPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_proteinsPlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void proteinsPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsPlotHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void proteinsPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinsPlotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(
                peptideShakerGUI, getClass().getResource("/helpFiles/QCPlots.html"),
                "#Protein",
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "Quality Control Plots - Help"
        );
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsPlotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportProteinsPlotJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsPlotJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportProteinsPlotJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportProteinsPlotJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsPlotJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportProteinsPlotJButtonMouseExited

    /**
     * Export the plot.
     *
     * @param evt
     */
    private void exportProteinsPlotJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProteinsPlotJButtonActionPerformed
        new ExportGraphicsDialog(
                peptideShakerGUI,
                peptideShakerGUI.getNormalIcon(),
                peptideShakerGUI.getWaitingIcon(),
                true,
                proteinQCPlotPanel,
                peptideShakerGUI.getLastSelectedFolder()
        );
    }//GEN-LAST:event_exportProteinsPlotJButtonActionPerformed

    /**
     * Resize the peptides plot area.
     *
     * @param evt
     */
    private void proteinsPlotLayeredPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_proteinsPlotLayeredPaneComponentResized

// resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                // move the icons
                proteinsPlotLayeredPane.getComponent(0).setBounds(
                        proteinsPlotLayeredPane.getWidth() - proteinsPlotLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        proteinsPlotLayeredPane.getComponent(0).getWidth(),
                        proteinsPlotLayeredPane.getComponent(0).getHeight()
                );

                proteinsPlotLayeredPane.getComponent(1).setBounds(
                        proteinsPlotLayeredPane.getWidth() - proteinsPlotLayeredPane.getComponent(1).getWidth() - 25,
                        -3,
                        proteinsPlotLayeredPane.getComponent(1).getWidth(),
                        proteinsPlotLayeredPane.getComponent(1).getHeight()
                );

                // resize the plot area
                proteinsPlotLayeredPane.getComponent(2).setBounds(
                        0,
                        0,
                        proteinsPlotLayeredPane.getWidth(),
                        proteinsPlotLayeredPane.getHeight()
                );

                proteinsPlotLayeredPane.revalidate();
                proteinsPlotLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_proteinsPlotLayeredPaneComponentResized

    /**
     * Update the peptide QC plot.
     *
     * @param evt
     */
    private void peptideLengthJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideLengthJRadioButtonActionPerformed
        peptideMissedCleavagesJRadioButtonActionPerformed(evt);
    }//GEN-LAST:event_peptideLengthJRadioButtonActionPerformed

    /**
     * Update the protein QC plot.
     *
     * @param evt
     */
    private void proteinSequenceLengthJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinSequenceLengthJRadioButtonActionPerformed
        proteinNumberValidatedPeptidesJRadioButtonActionPerformed(evt);
    }//GEN-LAST:event_proteinSequenceLengthJRadioButtonActionPerformed

    /**
     * Update the QC plot in the selected tab.
     *
     * @param evt
     */
    private void tabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabbedPaneStateChanged

        if (peptideShakerGUI.getIdentification() != null
                && peptideShakerGUI.getSelectedTab() == PeptideShakerGUI.QC_PLOTS_TAB_INDEX
                && validatedValues != null) {

            switch (tabbedPane.getSelectedIndex()) {
                case 0:
                    // psms
                    updatePsmQCPlot();
                    exportPsmPlotJButton.setEnabled(true);
                    break;

                case 1:
                    // peptides
                    updatePeptideQCPlot();
                    exportPeptidesPlotJButton.setEnabled(true);
                    break;

                case 2:
                    // proteins
                    updateProteinQCPlot();
                    exportProteinsPlotJButton.setEnabled(true);
                    break;

                default:
                    break;

            }
        }
    }//GEN-LAST:event_tabbedPaneStateChanged

    /**
     * Display the modification efficiency plot.
     *
     * @param evt
     */
    private void peptideModificationEfficiencyJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideModificationEfficiencyJRadioButtonActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updatePeptideQCPlot();
            exportPeptidesPlotJButton.setEnabled(true);
        }
    }//GEN-LAST:event_peptideModificationEfficiencyJRadioButtonActionPerformed

    /**
     * Display the modifications plot.
     *
     * @param evt
     */
    private void peptideModificationsJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideModificationsJRadioButtonActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updatePeptideQCPlot();
            exportPeptidesPlotJButton.setEnabled(true);
        }
    }//GEN-LAST:event_peptideModificationsJRadioButtonActionPerformed

    /**
     * Display the modification specificity plot.
     *
     * @param evt
     */
    private void peptideModificationSpecificityJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideModificationSpecificityJRadioButtonActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updatePeptideQCPlot();
            exportPeptidesPlotJButton.setEnabled(true);
        }
    }//GEN-LAST:event_peptideModificationSpecificityJRadioButtonActionPerformed

    /**
     * Display the peptide cleavage sites plot.
     *
     * @param evt
     */
    private void peptideCleavageSitesJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_peptideCleavageSitesJRadioButtonActionPerformed
        if (peptideShakerGUI.getIdentification() != null) {
            updatePeptideQCPlot();
            exportPeptidesPlotJButton.setEnabled(true);
        }
    }//GEN-LAST:event_peptideCleavageSitesJRadioButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton exportPeptidesPlotJButton;
    private javax.swing.JButton exportProteinsPlotJButton;
    private javax.swing.JButton exportPsmPlotJButton;
    private javax.swing.ButtonGroup peptideButtonGroup;
    private javax.swing.JRadioButton peptideCleavageSitesJRadioButton;
    private javax.swing.JRadioButton peptideLengthJRadioButton;
    private javax.swing.JRadioButton peptideMissedCleavagesJRadioButton;
    private javax.swing.JRadioButton peptideModificationEfficiencyJRadioButton;
    private javax.swing.JRadioButton peptideModificationSpecificityJRadioButton;
    private javax.swing.JRadioButton peptideModificationsJRadioButton;
    private javax.swing.JPanel peptidePanel;
    private javax.swing.JPanel peptideQCPlotPanel;
    private javax.swing.JRadioButton peptideValidatedPsmsJRadioButton;
    private javax.swing.JButton peptidesPlotHelpJButton;
    private javax.swing.JLayeredPane peptidesPlotLayeredPane;
    private javax.swing.JPanel peptidesPlotTypePanel;
    private javax.swing.ButtonGroup proteinButtonGroup;
    private javax.swing.JRadioButton proteinNumberValidatedPeptidesJRadioButton;
    private javax.swing.JPanel proteinPanel;
    private javax.swing.JPanel proteinPlotTypePanel;
    private javax.swing.JPanel proteinQCPlotPanel;
    private javax.swing.JRadioButton proteinSequenceCoverageJRadioButton;
    private javax.swing.JRadioButton proteinSequenceLengthJRadioButton;
    private javax.swing.JRadioButton proteinSpectrumCountingScoreJRadioButton;
    private javax.swing.JButton proteinsPlotHelpJButton;
    private javax.swing.JLayeredPane proteinsPlotLayeredPane;
    private javax.swing.ButtonGroup psmButtonGroup;
    private javax.swing.JPanel psmPanel;
    private javax.swing.JButton psmPlotHelpJButton;
    private javax.swing.JLayeredPane psmPlotLayeredPane;
    private javax.swing.JPanel psmPlotTypePanel;
    private javax.swing.JRadioButton psmPrecursorChargeJRadioButton;
    private javax.swing.JRadioButton psmPrecursorMassErrorJRadioButton;
    private javax.swing.JPanel psmQCPlotPanel;
    private javax.swing.JPanel qcPanel;
    private javax.swing.JTabbedPane tabbedPane;
    // End of variables declaration//GEN-END:variables

    /**
     * This method displays results on the panel.
     */
    public void displayResults() {

        if (peptideShakerGUI.getIdentification() != null) {

            if (peptideShakerGUI.getProjectType() == ProjectType.peptide) {
                tabbedPane.setEnabledAt(2, false);
                tabbedPane.setSelectedIndex(1);
            } else if (peptideShakerGUI.getProjectType() == ProjectType.psm) {
                tabbedPane.setEnabledAt(1, false);
                tabbedPane.setEnabledAt(2, false);
                tabbedPane.setSelectedIndex(0);
            }

            currentProteinPlotType = PlotType.None;
            currentPeptidePlotType = PlotType.None;
            currentPsmPlotType = PlotType.None;

            switch (tabbedPane.getSelectedIndex()) {

                case 0:
                    // psms
                    updatePsmQCPlot();
                    exportPsmPlotJButton.setEnabled(true);
                    break;

                case 1:
                    // peptides
                    updatePeptideQCPlot();
                    exportPeptidesPlotJButton.setEnabled(true);
                    break;

                case 2:
                    // proteins
                    updateProteinQCPlot();
                    exportProteinsPlotJButton.setEnabled(true);
                    break;

                default:
                    break;

            }

            peptideShakerGUI.setUpdated(PeptideShakerGUI.QC_PLOTS_TAB_INDEX, true);
        }
    }

    /**
     * Updates the protein QC plot.
     */
    private void updateProteinQCPlot() {

        // see if we need to update
        if ((proteinSpectrumCountingScoreJRadioButton.isSelected() && currentProteinPlotType != PlotType.Protein_MS2_QuantScores)
                || (proteinSequenceCoverageJRadioButton.isSelected() && currentProteinPlotType != PlotType.Protein_Sequence_Coverage)
                || (proteinNumberValidatedPeptidesJRadioButton.isSelected() && currentProteinPlotType != PlotType.Protein_Validated_Peptides)
                || (proteinSequenceLengthJRadioButton.isSelected() && currentProteinPlotType != PlotType.Protein_Sequence_Length)) {

            progressDialog = new ProgressDialogX(
                    peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true
            );

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Loading QC Plot. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }

                }
            }, "ProgressDialog").start();

            new Thread("UpdatePlotThread") {
                @Override
                public void run() {

                    progressDialog.setMaxPrimaryProgressCounter(peptideShakerGUI.getIdentification().getProteinIdentification().size());
                    progressDialog.setTitle("Getting Protein Dataset. Please Wait...");
                    getProteinDataset();
                    progressDialog.setTitle("Loading Protein QC Plots. Please Wait...");

                    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                    ArrayList<Double> bins = new ArrayList<>();

                    if (proteinSpectrumCountingScoreJRadioButton.isSelected()) {

                        double tempMaxValue; // @TODO: support scientific x-axis for spectrum counting plot?

                        // try to find a suitable range
                        if (maxValue < 0.25) {
                            tempMaxValue = 0.25;
                        } else if (maxValue < 0.5) {
                            tempMaxValue = 0.5;
                        } else if (maxValue < 1) {
                            tempMaxValue = 1;
                        } else if (maxValue < 5) {
                            tempMaxValue = 5;
                        } else if (maxValue < 10) {
                            tempMaxValue = 10;
                        } else if (maxValue < 25) {
                            tempMaxValue = 25;
                        } else if (maxValue < 50) {
                            tempMaxValue = 50;
                        } else if (maxValue < 100) {
                            tempMaxValue = 100;
                        } else {
                            tempMaxValue = Math.ceil(maxValue);
                        }

                        int nBins = 20;
                        for (int i = 0; i <= nBins; i++) {
                            double bin = i * tempMaxValue / nBins;
                            bins.add(Util.roundDouble(bin, 4));
                        }

//                        getBinData(bins, validatedValues, dataset, "Confident True Positives", false);
//                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful True Positives", false);
//                        getBinData(bins, validatedDecoyValues, dataset, "False Positives", false);
//                        getBinData(bins, nonValidatedValues, dataset, "False Negatives", false);
//                        getBinData(bins, nonValidatedDecoyValues, dataset, "True Negatives", false);
                        getBinData(bins, validatedValues, dataset, "Confident", false);
                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", false);
                        getBinData(bins, nonValidatedValues, dataset, "Not Validated", false);

                        currentProteinPlotType = PlotType.Protein_MS2_QuantScores;

                    } else if (proteinSequenceCoverageJRadioButton.isSelected()) {

                        bins.add(0.0);
                        bins.add(10.0);
                        bins.add(20.0);
                        bins.add(30.0);
                        bins.add(40.0);
                        bins.add(50.0);
                        bins.add(60.0);
                        bins.add(70.0);
                        bins.add(80.0);
                        bins.add(90.0);

//                        getBinData(bins, validatedValues, dataset, "Confident True Positives", "%", true);
//                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful True Positives", "%", true);
//                        getBinData(bins, validatedDecoyValues, dataset, "False Positives", "%", true);
//                        getBinData(bins, nonValidatedValues, dataset, "False Negatives", "%", true);
//                        getBinData(bins, nonValidatedDecoyValues, dataset, "True Negatives", "%", true);
                        getBinData(bins, validatedValues, dataset, "Confident", "%", true);
                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", "%", true);
                        getBinData(bins, nonValidatedValues, dataset, "Not Validated", "%", true);

                        currentProteinPlotType = PlotType.Protein_Sequence_Coverage;

                    } else if (proteinNumberValidatedPeptidesJRadioButton.isSelected()) {

                        bins.add(0.0);
                        bins.add(1.0);
                        bins.add(2.0);
                        bins.add(3.0);
                        bins.add(5.0);
                        bins.add(10.0);
                        bins.add(20.0);
                        bins.add(50.0);
                        bins.add(100.0);
                        bins.add(200.0);
                        bins.add(500.0);

//                        getBinData(bins, validatedValues, dataset, "Confident True Positives", true);
//                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful True Positives", true);
//                        getBinData(bins, validatedDecoyValues, dataset, "False Positives", true);
//                        getBinData(bins, nonValidatedValues, dataset, "False Negatives", true);
//                        getBinData(bins, nonValidatedDecoyValues, dataset, "True Negatives", true);
                        getBinData(bins, validatedValues, dataset, "Confident", true);
                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", true);
                        getBinData(bins, nonValidatedValues, dataset, "Not Validated", true);

                        currentProteinPlotType = PlotType.Protein_Validated_Peptides;

                    } else if (proteinSequenceLengthJRadioButton.isSelected()) {

                        bins.add(0.0);
                        bins.add(100.0);
                        bins.add(250.0);
                        bins.add(500.0);
                        bins.add(1000.0);
                        bins.add(1500.0);
                        bins.add(2000.0);
                        bins.add(2500.0);
                        bins.add(3000.0);

//                        getBinData(bins, validatedValues, dataset, "Confident True Positives", true);
//                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful True Positives", true);
//                        getBinData(bins, validatedDecoyValues, dataset, "False Positives", true);
//                        getBinData(bins, nonValidatedValues, dataset, "False Negatives", true);
//                        getBinData(bins, nonValidatedDecoyValues, dataset, "True Negatives", true);
                        getBinData(bins, validatedValues, dataset, "Confident", true);
                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", true);
                        getBinData(bins, nonValidatedValues, dataset, "Not Validated", true);

                        currentProteinPlotType = PlotType.Protein_Sequence_Length;
                    }

                    if (!progressDialog.isRunCanceled()) {

                        JFreeChart proteinChart = ChartFactory.createStackedBarChart(
                                null,
                                null,
                                "Number of Proteins",
                                dataset,
                                PlotOrientation.VERTICAL,
                                true,
                                true,
                                true
                        );

                        StackedBarRenderer renderer = new StackedBarRenderer();
                        renderer.setShadowVisible(false);
                        renderer.setSeriesPaint(0, histogramColors[0]);
                        renderer.setSeriesPaint(1, histogramColors[1]);
                        //renderer.setSeriesPaint(2, histogramColors[2]);
                        renderer.setSeriesPaint(2, histogramColors[4]);
                        //renderer.setSeriesPaint(3, histogramColors[3]);
                        //renderer.setSeriesPaint(4, histogramColors[4]);
                        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
                        proteinChart.getCategoryPlot().setRenderer(0, renderer);

                        ChartPanel chartPanel = new ChartPanel(
                                proteinChart,
                                false //peptideShakerGUI.getDisplayParameters().getLowResolutionCharts()
                        );

                        if (proteinNumberValidatedPeptidesJRadioButton.isSelected()) {
                            proteinChart.getCategoryPlot().getDomainAxis().setLabel("Number of Validated Peptides");
                            proteinChart.setTitle("Protein QC Plot - Number of Validated Peptides");
                        } else if (proteinSpectrumCountingScoreJRadioButton.isSelected()) {

                            proteinChart.setTitle("Protein QC Plot - MS2 Quantification Scores");

                            switch (peptideShakerGUI.getSpectrumCountingParameters().getSelectedMethod()) {

                                case EMPAI:
                                    proteinChart.getCategoryPlot().getDomainAxis().setLabel("MS2 Quantification (emPAI)");
                                    break;

                                case NSAF:
                                    proteinChart.getCategoryPlot().getDomainAxis().setLabel("MS2 Quantification (NSAF)");
                                    break;

                                case LFQ:
                                    proteinChart.getCategoryPlot().getDomainAxis().setLabel("Label-free MS1 Quantification (LFQ)");
                                    break;

                                default:
                                    break;

                            }

                        } else if (proteinSequenceCoverageJRadioButton.isSelected()) {
                            proteinChart.getCategoryPlot().getDomainAxis().setLabel("Sequence Coverage");
                            proteinChart.setTitle("Protein QC Plot - Sequence Coverage");
                        } else if (proteinSequenceLengthJRadioButton.isSelected()) {
                            proteinChart.getCategoryPlot().getDomainAxis().setLabel("Sequence Length");
                            proteinChart.setTitle("Protein QC Plot - Sequence Length");
                        }

                        // set background color
                        proteinChart.getPlot().setBackgroundPaint(Color.WHITE);
                        proteinChart.setBackgroundPaint(Color.WHITE);
                        chartPanel.setBackground(Color.WHITE);

                        // remove space before/after the domain axis
                        proteinChart.getCategoryPlot().getDomainAxis().setUpperMargin(0);
                        proteinChart.getCategoryPlot().getDomainAxis().setLowerMargin(0);

                        // rotate the x-axis labels to make sure that they are readable
                        if (proteinSpectrumCountingScoreJRadioButton.isSelected()) {
                            proteinChart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
                        }

                        // hide the outline
                        proteinChart.getPlot().setOutlineVisible(false);

                        proteinQCPlotPanel.removeAll();
                        proteinQCPlotPanel.add(chartPanel);
                        proteinQCPlotPanel.revalidate();
                        proteinQCPlotPanel.repaint();
                    }

                    progressDialog.setRunFinished();
                }
            }.start();
        }
    }

    /**
     * Updates the peptide QC plot.
     */
    private void updatePeptideQCPlot() {

        // see if we need to update
        if ((peptideValidatedPsmsJRadioButton.isSelected() && currentPeptidePlotType != PlotType.Peptide_Validated_PSMs)
                || (peptideMissedCleavagesJRadioButton.isSelected() && currentPeptidePlotType != PlotType.Peptide_Missed_Cleavages)
                || (peptideCleavageSitesJRadioButton.isSelected() && currentPeptidePlotType != PlotType.Peptide_Cleavages_Sites)
                || (peptideLengthJRadioButton.isSelected() && currentPeptidePlotType != PlotType.Peptide_Length)
                || (peptideModificationsJRadioButton.isSelected() && currentPeptidePlotType != PlotType.Peptide_Modifications)
                || (peptideModificationEfficiencyJRadioButton.isSelected() && currentPeptidePlotType != PlotType.Peptide_Modification_Efficiency)
                || (peptideModificationSpecificityJRadioButton.isSelected() && currentPeptidePlotType != PlotType.Peptide_Modification_Specificity)) {

            progressDialog = new ProgressDialogX(
                    peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true
            );

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Loading QC Plot. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("UpdatePlotThread") {
                @Override
                public void run() {

                    progressDialog.setMaxPrimaryProgressCounter(peptideShakerGUI.getIdentification().getPeptideIdentification().size());
                    progressDialog.setTitle("Getting Peptide Dataset. Please Wait...");
                    getPeptideDataset();
                    progressDialog.setTitle("Loading Peptide QC Plots. Please Wait...");

                    try {
                        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                        ArrayList<Double> bins = new ArrayList<>();

                        if (peptideValidatedPsmsJRadioButton.isSelected()) {

                            bins.add(0.0);
                            bins.add(1.0);
                            bins.add(2.0);
                            bins.add(3.0);
                            bins.add(5.0);
                            bins.add(10.0);
                            bins.add(20.0);
                            bins.add(50.0);
                            bins.add(100.0);
                            bins.add(200.0);
                            bins.add(500.0);

                            getBinData(bins, validatedValues, dataset, "Confident", true);
                            getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", true);
                            getBinData(bins, nonValidatedValues, dataset, "Not Validated", true);

                            currentPeptidePlotType = PlotType.Peptide_Validated_PSMs;

                        } else if (peptideMissedCleavagesJRadioButton.isSelected()) {

                            bins.add(0.0);
                            bins.add(1.0);
                            bins.add(2.0);
                            bins.add(3.0);

                            getBinData(bins, validatedValues, dataset, "Confident", true);
                            getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", true);
                            getBinData(bins, nonValidatedValues, dataset, "Not Validated", true);

                            currentPeptidePlotType = PlotType.Peptide_Missed_Cleavages;

                        } else if (peptideCleavageSitesJRadioButton.isSelected()) {

                            dataset = getPeptideCleavageSiteDataset();
                            currentPeptidePlotType = PlotType.Peptide_Cleavages_Sites;

                        } else if (peptideLengthJRadioButton.isSelected()) {

                            PeptideAssumptionFilter idFilter = peptideShakerGUI.getIdentificationParameters().getPeptideAssumptionFilter();
                            int min = idFilter.getMinPepLength();
                            int max = idFilter.getMaxPepLength();

                            for (int i = min; i < max; i++) {
                                bins.add(Double.valueOf(i));
                            }

                            getBinData(bins, validatedValues, dataset, "Confident", true);
                            getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", true);
                            getBinData(bins, nonValidatedValues, dataset, "Not Validated", true);

                            currentPeptidePlotType = PlotType.Peptide_Length;
                        } else if (peptideModificationsJRadioButton.isSelected()) {
                            dataset = getPeptideModificationsDataset();
                            currentPeptidePlotType = PlotType.Peptide_Modifications;
                        } else if (peptideModificationEfficiencyJRadioButton.isSelected()) {
                            dataset = getPeptideModificationEfficiencyDataset();
                            currentPeptidePlotType = PlotType.Peptide_Modification_Efficiency;
                        } else if (peptideModificationSpecificityJRadioButton.isSelected()) {
                            dataset = getPeptideModificationEnrichmentSpecificityDataset();
                            currentPeptidePlotType = PlotType.Peptide_Modification_Specificity;
                        }

                        if (!progressDialog.isRunCanceled()) {

                            JFreeChart peptideChart = ChartFactory.createStackedBarChart(
                                    null,
                                    null,
                                    "Number of Peptides",
                                    dataset,
                                    PlotOrientation.VERTICAL,
                                    true,
                                    true,
                                    true
                            );

                            StackedBarRenderer renderer = new StackedBarRenderer();
                            renderer.setShadowVisible(false);
                            if (peptideModificationEfficiencyJRadioButton.isSelected()
                                    || peptideModificationSpecificityJRadioButton.isSelected()) {
                                renderer.setSeriesPaint(0, histogramColors[0]);
                                renderer.setSeriesPaint(1, histogramColors[4]);
                            } else {
                                renderer.setSeriesPaint(0, histogramColors[0]);
                                renderer.setSeriesPaint(1, histogramColors[1]);
                                renderer.setSeriesPaint(2, histogramColors[4]);
                            }
                            renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
                            peptideChart.getCategoryPlot().setRenderer(0, renderer);

                            ChartPanel chartPanel = new ChartPanel(
                                    peptideChart,
                                    false //peptideShakerGUI.getDisplayParameters().getLowResolutionCharts()
                            );

                            if (peptideValidatedPsmsJRadioButton.isSelected()) {
                                peptideChart.getCategoryPlot().getDomainAxis().setLabel("Number of Validated PSMs");
                                peptideChart.setTitle("Peptides QC Plot - Number of Validated PSMs");
                            } else if (peptideMissedCleavagesJRadioButton.isSelected()) {
                                peptideChart.getCategoryPlot().getDomainAxis().setLabel("Missed Cleavages");
                                peptideChart.setTitle("Peptides QC Plot - Missed Cleavages");
                            } else if (peptideLengthJRadioButton.isSelected()) {
                                peptideChart.getCategoryPlot().getRangeAxis().setLabel("Frequency");
                                peptideChart.getCategoryPlot().getDomainAxis().setLabel("Peptide Length");
                                peptideChart.setTitle("Peptides QC Plot - Peptide Length");
                            } else if (peptideModificationsJRadioButton.isSelected()) {
                                peptideChart.getCategoryPlot().getRangeAxis().setLabel("#Peptides with the Modification");
                                peptideChart.setTitle("Peptides QC Plot - Peptide Modifications");
                                peptideChart.getCategoryPlot().getDomainAxis().setMaximumCategoryLabelLines(5);
                            } else if (peptideModificationEfficiencyJRadioButton.isSelected()) {
                                peptideChart.getCategoryPlot().getRangeAxis().setLabel("Share of Modified Sites [%]");
                                peptideChart.setTitle("Peptides QC Plot - Modification Efficiency");
                                peptideChart.getCategoryPlot().getRangeAxis().setRange(0, 100);
                                peptideChart.getCategoryPlot().getDomainAxis().setMaximumCategoryLabelLines(5);
                            } else if (peptideModificationSpecificityJRadioButton.isSelected()) {
                                peptideChart.getCategoryPlot().getRangeAxis().setLabel("Share of Modified Peptides [%]");
                                peptideChart.setTitle("Peptides QC Plot - Modification Specificity");
                                peptideChart.getCategoryPlot().getRangeAxis().setRange(0, 100);
                                peptideChart.getCategoryPlot().getDomainAxis().setMaximumCategoryLabelLines(5);
                            }

                            // set background color
                            peptideChart.getPlot().setBackgroundPaint(Color.WHITE);
                            peptideChart.setBackgroundPaint(Color.WHITE);
                            chartPanel.setBackground(Color.WHITE);

                            // remove space before/after the domain axis
                            peptideChart.getCategoryPlot().getDomainAxis().setUpperMargin(0);
                            peptideChart.getCategoryPlot().getDomainAxis().setLowerMargin(0);

                            // hide the outline
                            peptideChart.getPlot().setOutlineVisible(false);

                            peptideQCPlotPanel.removeAll();
                            peptideQCPlotPanel.add(chartPanel);
                            peptideQCPlotPanel.revalidate();
                            peptideQCPlotPanel.repaint();
                        }

                        progressDialog.setRunFinished();

                    } catch (Exception e) {
                        progressDialog.setRunCanceled();
                        progressDialog.setRunFinished();
                        peptideShakerGUI.catchException(e);
                    }
                }
            }.start();
        }
    }

    /**
     * Updates the PSM QC plot.
     */
    private void updatePsmQCPlot() {

        // see if we need to update
        if ((psmPrecursorMassErrorJRadioButton.isSelected() && currentPsmPlotType != PlotType.PSM_Precursor_Mass_Error)
                || (psmPrecursorChargeJRadioButton.isSelected() && currentPsmPlotType != PlotType.PSM_Precursor_Charge)) {

            progressDialog = new ProgressDialogX(
                    peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true
            );

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Loading QC Plot. Please Wait...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("UpdatePlotThread") {
                @Override
                public void run() {

                    progressDialog.setMaxPrimaryProgressCounter(peptideShakerGUI.getIdentification().getSpectrumIdentificationSize());

                    progressDialog.setTitle("Getting PSM Dataset. Please Wait...");
                    getPsmDataset();
                    progressDialog.setTitle("Loading PSM QC Plots. Please Wait...");

                    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                    ArrayList<Double> bins = new ArrayList<>();

                    if (psmPrecursorMassErrorJRadioButton.isSelected()) {

                        double prec = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getPrecursorAccuracy();
                        int nBins = 20;
                        for (int i = -nBins; i <= nBins; i++) {
                            double bin = i * prec / nBins;
                            bins.add(bin);
                        }

                        getBinData(bins, validatedValues, dataset, "Confident", false);
                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", false);
                        getBinData(bins, nonValidatedValues, dataset, "Not Validated", false);

                        currentPsmPlotType = PlotType.PSM_Precursor_Mass_Error;

                    } else if (psmPrecursorChargeJRadioButton.isSelected()) {

                        int maxCharge = peptideShakerGUI.getMetrics().getMaxCharge();

                        for (int i = 0; i <= maxCharge; i++) {
                            bins.add((double) i);
                        }

                        getBinData(bins, validatedValues, dataset, "Confident", true);
                        getBinData(bins, validatedDoubtfulValues, dataset, "Doubtful", true);
                        getBinData(bins, nonValidatedValues, dataset, "Not Validated", true);

                        currentPsmPlotType = PlotType.PSM_Precursor_Charge;
                    }

                    if (!progressDialog.isRunCanceled()) {

                        JFreeChart psmChart = ChartFactory.createStackedBarChart(
                                null,
                                null,
                                "Number of PSMs",
                                dataset,
                                PlotOrientation.VERTICAL,
                                true,
                                true,
                                true
                        );

                        StackedBarRenderer renderer = new StackedBarRenderer();
                        renderer.setShadowVisible(false);
                        renderer.setSeriesPaint(0, histogramColors[0]);
                        renderer.setSeriesPaint(1, histogramColors[1]);
                        renderer.setSeriesPaint(2, histogramColors[4]);
                        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
                        psmChart.getCategoryPlot().setRenderer(0, renderer);

                        ChartPanel chartPanel = new ChartPanel(
                                psmChart,
                                false //peptideShakerGUI.getDisplayParameters().getLowResolutionCharts()
                        );

                        if (psmPrecursorMassErrorJRadioButton.isSelected()) {
                            psmChart.getCategoryPlot().getDomainAxis().setLabel("Precursor m/z Error");
                            psmChart.setTitle("PSMs QC Plot - Precursor m/z Error");
                        } else if (psmPrecursorChargeJRadioButton.isSelected()) {
                            psmChart.getCategoryPlot().getDomainAxis().setLabel("Precursor Charge");
                            psmChart.setTitle("PSMs QC Plot - Precursor Charge");
                        }

                        // set background color
                        psmChart.getPlot().setBackgroundPaint(Color.WHITE);
                        psmChart.setBackgroundPaint(Color.WHITE);
                        chartPanel.setBackground(Color.WHITE);

                        // remove space before/after the domain axis
                        psmChart.getCategoryPlot().getDomainAxis().setUpperMargin(0);
                        psmChart.getCategoryPlot().getDomainAxis().setLowerMargin(0);

                        // rotate the x-axis labels to make sure that they are readable
                        if (psmPrecursorMassErrorJRadioButton.isSelected()) {
                            psmChart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
                        }

                        // hide the outline
                        psmChart.getPlot().setOutlineVisible(false);

                        psmQCPlotPanel.removeAll();
                        psmQCPlotPanel.add(chartPanel);
                        psmQCPlotPanel.revalidate();
                        psmQCPlotPanel.repaint();
                    }

                    progressDialog.setRunFinished();
                }
            }.start();
        }
    }

    /**
     * Returns the dataset to use for the protein QC plot.
     */
    private void getProteinDataset() {

        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setMaxPrimaryProgressCounter(peptideShakerGUI.getIdentification().getProteinIdentification().size());
        progressDialog.setValue(0);

        IdentificationFeaturesGenerator identificationFeaturesGenerator = peptideShakerGUI.getIdentificationFeaturesGenerator();

        maxValue = Double.MIN_VALUE;

        validatedValues = new ArrayList<>();
        validatedDoubtfulValues = new ArrayList<>();
        nonValidatedValues = new ArrayList<>();
        validatedDecoyValues = new ArrayList<>();
        nonValidatedDecoyValues = new ArrayList<>();

        ProteinMatchesIterator proteinMatchesIterator = peptideShakerGUI.getIdentification().getProteinMatchesIterator(progressDialog);
        ProteinMatch proteinMatch;

        while ((proteinMatch = proteinMatchesIterator.next()) != null) {

            long proteinKey = proteinMatch.getKey();

            if (progressDialog.isRunCanceled()) {
                break;
            }

            double value = 0;

            if (proteinNumberValidatedPeptidesJRadioButton.isSelected()) {
                value = identificationFeaturesGenerator.getNValidatedPeptides(proteinKey);
            } else if (proteinSpectrumCountingScoreJRadioButton.isSelected()) {
                value = identificationFeaturesGenerator.getSpectrumCounting(proteinKey);
            } else if (proteinSequenceCoverageJRadioButton.isSelected()) {
                HashMap<Integer, Double> sequenceCoverage = peptideShakerGUI.getIdentificationFeaturesGenerator().getSequenceCoverage(proteinKey);
                Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                value = sequenceCoverageConfident + sequenceCoverageDoubtful;
            } else if (proteinSequenceLengthJRadioButton.isSelected()) {
                String proteinSequence = peptideShakerGUI.getSequenceProvider().getSequence(proteinMatch.getLeadingAccession());
                value = proteinSequence.length();
            }

            PSParameter proteinParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);

            if (!proteinParameter.getHidden()) {

                if (value > maxValue) {
                    maxValue = value;
                }
                if (!proteinMatch.isDecoy()) {
                    if (proteinParameter.getMatchValidationLevel().isValidated()) {
                        if (proteinParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                            validatedValues.add(value);
                        } else {
                            validatedDoubtfulValues.add(value);
                        }
                    } else {
                        nonValidatedValues.add(value);
                    }
                }
            }

            progressDialog.increasePrimaryProgressCounter();
        }
    }

    /**
     * Returns the dataset to use for the peptide QC plot.
     */
    private void getPeptideDataset() {

        maxValue = Double.MIN_VALUE;

        SequenceProvider sequenceProvider = peptideShakerGUI.getSequenceProvider();

        if (peptideValidatedPsmsJRadioButton.isSelected()) {

            progressDialog.setPrimaryProgressCounterIndeterminate(false);
            progressDialog.setMaxPrimaryProgressCounter(peptideShakerGUI.getIdentification().getPeptideIdentification().size());
            progressDialog.setValue(0);

            // values for the number of validated PSMs
            validatedValues = new ArrayList<>();
            validatedDoubtfulValues = new ArrayList<>();
            nonValidatedValues = new ArrayList<>();
            validatedDecoyValues = new ArrayList<>();
            nonValidatedDecoyValues = new ArrayList<>();

            PeptideMatchesIterator peptideMatchesIterator = peptideShakerGUI.getIdentification().getPeptideMatchesIterator(progressDialog);
            PeptideMatch peptideMatch;

            while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                if (progressDialog.isRunCanceled()) {
                    break;
                }

                double value = 0;

                for (long spectrumMatchKey : peptideMatch.getSpectrumMatchesKeys()) {

                    if (progressDialog.isRunCanceled()) {
                        break;
                    }

                    SpectrumMatch spectrumMatch = peptideShakerGUI.getIdentification().getSpectrumMatch(spectrumMatchKey);

                    PSParameter spectrumParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                    if (spectrumParameter.getMatchValidationLevel().isValidated() && !spectrumParameter.getHidden()) {
                        value = value + 1;
                    }
                }
                if (value > maxValue) {
                    maxValue = value;
                }

                PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                if (!peptideParameter.getHidden()) {

                    if (!PeptideUtils.isDecoy(peptideMatch.getPeptide(), sequenceProvider)) {
                        if (peptideParameter.getMatchValidationLevel().isValidated()) {
                            if (peptideParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                                validatedValues.add(value);
                            } else {
                                validatedDoubtfulValues.add(value);
                            }
                        } else {
                            nonValidatedValues.add(value);
                        }
                    } else if (peptideParameter.getMatchValidationLevel().isValidated()) {
                        validatedDecoyValues.add(value);
                    } else {
                        nonValidatedDecoyValues.add(value);
                    }
                }

                progressDialog.increasePrimaryProgressCounter();
            }
        } else if (peptideMissedCleavagesJRadioButton.isSelected()) {

            progressDialog.setPrimaryProgressCounterIndeterminate(false);
            progressDialog.setMaxPrimaryProgressCounter(peptideShakerGUI.getIdentification().getPeptideIdentification().size());
            progressDialog.setValue(0);

            // Values for the missed cleavages
            validatedValues = new ArrayList<>();
            validatedDoubtfulValues = new ArrayList<>();
            nonValidatedValues = new ArrayList<>();
            validatedDecoyValues = new ArrayList<>();
            nonValidatedDecoyValues = new ArrayList<>();

            PeptideMatchesIterator peptideMatchesIterator = peptideShakerGUI.getIdentification().getPeptideMatchesIterator(progressDialog);
            PeptideMatch peptideMatch;

            while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                if (progressDialog.isRunCanceled()) {
                    break;
                }

                PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                if (!peptideParameter.getHidden()) {

                    Double value = null;
                    DigestionParameters digestionParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getDigestionParameters();

                    if (digestionParameters.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {

                        for (Enzyme enzyme : digestionParameters.getEnzymes()) {

                            int enzymeMissedCleavages = enzyme.getNmissedCleavages(peptideMatch.getPeptide().getSequence());

                            if (value == null || enzymeMissedCleavages < value) {
                                value = Double.valueOf(enzymeMissedCleavages);
                            }
                        }

                    }

                    if (value == null) {
                        value = 0.0;
                    }

                    if (value > 0) {
                        if (value > maxValue) {
                            maxValue = value;
                        }
                    }

                    if (!PeptideUtils.isDecoy(peptideMatch.getPeptide(), sequenceProvider)) {

                        if (peptideParameter.getMatchValidationLevel().isValidated()) {

                            if (peptideParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                                validatedValues.add(value);
                            } else {
                                validatedDoubtfulValues.add(value);
                            }

                        } else {
                            nonValidatedValues.add(value);
                        }

                    } else if (peptideParameter.getMatchValidationLevel().isValidated()) {
                        validatedDecoyValues.add(value);
                    } else {
                        nonValidatedDecoyValues.add(value);
                    }
                }

                progressDialog.increasePrimaryProgressCounter();
            }
        } else if (peptideLengthJRadioButton.isSelected()) {

            progressDialog.setPrimaryProgressCounterIndeterminate(false);
            progressDialog.setMaxPrimaryProgressCounter(peptideShakerGUI.getIdentification().getPeptideIdentification().size());
            progressDialog.setValue(0);

            // values for the peptide length
            validatedValues = new ArrayList<>();
            validatedDoubtfulValues = new ArrayList<>();
            nonValidatedValues = new ArrayList<>();
            validatedDecoyValues = new ArrayList<>();
            nonValidatedDecoyValues = new ArrayList<>();

            PeptideMatchesIterator peptideMatchesIterator = peptideShakerGUI.getIdentification().getPeptideMatchesIterator(progressDialog);
            PeptideMatch peptideMatch;

            while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                if (progressDialog.isRunCanceled()) {
                    break;
                }

                PSParameter peptideParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);

                if (!peptideParameter.getHidden()) {

                    double length = peptideMatch.getPeptide().getSequence().length();
                    if (length > 0) {
                        if (length > maxValue) {
                            maxValue = length;
                        }
                    }

                    if (!PeptideUtils.isDecoy(peptideMatch.getPeptide(), sequenceProvider)) {
                        if (peptideParameter.getMatchValidationLevel().isValidated()) {
                            if (peptideParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                                validatedValues.add(length);
                            } else {
                                validatedDoubtfulValues.add(length);
                            }
                        } else {
                            nonValidatedValues.add(length);
                        }
                    } else if (peptideParameter.getMatchValidationLevel().isValidated()) {
                        validatedDecoyValues.add(length);
                    } else {
                        nonValidatedDecoyValues.add(length);
                    }
                }

                progressDialog.increasePrimaryProgressCounter();
            }
        }
    }

    /**
     * Returns the dataset to use for the PSM QC plot.
     */
    private void getPsmDataset() {

        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setMaxPrimaryProgressCounter(peptideShakerGUI.getIdentification().getSpectrumIdentificationSize());
        progressDialog.setValue(0);

        maxValue = Double.MIN_VALUE;

        if (psmPrecursorMassErrorJRadioButton.isSelected()) {

            // values for the precursor mass deviation
            validatedValues = new ArrayList<>();
            validatedDoubtfulValues = new ArrayList<>();
            nonValidatedValues = new ArrayList<>();
            validatedDecoyValues = new ArrayList<>();
            nonValidatedDecoyValues = new ArrayList<>();

            SpectrumMatchesIterator psmIterator = peptideShakerGUI.getIdentification().getSpectrumMatchesIterator(progressDialog);
            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = psmIterator.next()) != null) {

                if (progressDialog.isRunCanceled()) {
                    break;
                }

                PSParameter psmParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                if (!psmParameter.getHidden() && spectrumMatch.getBestPeptideAssumption() != null) {

                    String spectrumFile = spectrumMatch.getSpectrumFile();
                    String spectrumTitle = spectrumMatch.getSpectrumTitle();
                    double precursorMz = peptideShakerGUI.getSpectrumProvider()
                            .getPrecursorMz(spectrumFile, spectrumTitle);

                    SearchParameters searchParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters();
                    double value = spectrumMatch.getBestPeptideAssumption().getDeltaMz(
                            precursorMz,
                            searchParameters.isPrecursorAccuracyTypePpm(),
                            searchParameters.getMinIsotopicCorrection(),
                            searchParameters.getMaxIsotopicCorrection()
                    );
                    if (value > maxValue) {
                        maxValue = value;
                    }

                    if (!PeptideUtils.isDecoy(spectrumMatch.getBestPeptideAssumption().getPeptide(), peptideShakerGUI.getSequenceProvider())) {
                        if (psmParameter.getMatchValidationLevel().isValidated()) {
                            if (psmParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                                validatedValues.add(value);
                            } else {
                                validatedDoubtfulValues.add(value);
                            }
                        } else {
                            nonValidatedValues.add(value);
                        }
                    } else if (psmParameter.getMatchValidationLevel().isValidated()) {
                        validatedDecoyValues.add(value);
                    } else {
                        nonValidatedDecoyValues.add(value);
                    }
                }

                progressDialog.increasePrimaryProgressCounter();
            }
        } else if (psmPrecursorChargeJRadioButton.isSelected()) {

            // values for the precursor charge
            validatedValues = new ArrayList<>();
            validatedDoubtfulValues = new ArrayList<>();
            nonValidatedValues = new ArrayList<>();
            validatedDecoyValues = new ArrayList<>();
            nonValidatedDecoyValues = new ArrayList<>();

            SpectrumMatchesIterator psmIterator = peptideShakerGUI.getIdentification().getSpectrumMatchesIterator(progressDialog);
            SpectrumMatch spectrumMatch;

            while ((spectrumMatch = psmIterator.next()) != null) {

                if (progressDialog.isRunCanceled()) {
                    break;
                }

                PSParameter psmParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);

                if (!psmParameter.getHidden() && spectrumMatch.getBestPeptideAssumption() != null) {

                    double value = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge();
                    if (value > maxValue) {
                        maxValue = value;
                    }

                    if (!PeptideUtils.isDecoy(spectrumMatch.getBestPeptideAssumption().getPeptide(), peptideShakerGUI.getSequenceProvider())) {
                        if (psmParameter.getMatchValidationLevel().isValidated()) {
                            if (psmParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                                validatedValues.add(value);
                            } else {
                                validatedDoubtfulValues.add(value);
                            }
                        } else {
                            nonValidatedValues.add(value);
                        }
                    } else if (psmParameter.getMatchValidationLevel().isValidated()) {
                        validatedDecoyValues.add(value);
                    } else {
                        nonValidatedDecoyValues.add(value);
                    }
                }

                progressDialog.increasePrimaryProgressCounter();
            }
        }
    }

    /**
     * Calculates the number of values in each bin given the values and the bin
     * sizes.
     *
     * @param bins the bins to use
     * @param values the values to put into the bins
     * @param dataset the dataset to add the values to
     * @param categoryLabel the category label
     * @param integerBins if true the values will be shown as integers
     */
    private void getBinData(
            ArrayList<Double> bins,
            ArrayList<Double> values,
            DefaultCategoryDataset dataset,
            String categoryLabel,
            boolean integerBins
    ) {
        getBinData(
                bins,
                values,
                dataset,
                categoryLabel,
                "",
                integerBins
        );
    }

    /**
     * Calculates the number of values in each bin given the values and the bin
     * sizes.
     *
     * @param bins the bins to use
     * @param values the values to put into the bins
     * @param dataset the dataset to add the values to
     * @param categoryLabel the category label
     * @param dataType added to the bin labels after the values, e.g. %
     * @param integerBins if true the values will be shown as integers
     */
    private void getBinData(
            ArrayList<Double> bins,
            ArrayList<Double> values,
            DefaultCategoryDataset dataset,
            String categoryLabel,
            String dataType,
            boolean integerBins
    ) {

        int[] binData = new int[bins.size() + 1];

        for (int i = 0; i < values.size() && !progressDialog.isRunCanceled(); i++) {

            boolean binFound = false;

            for (int j = 0; j < bins.size() && !binFound && !progressDialog.isRunCanceled(); j++) {
                if (values.get(i) <= bins.get(j)) {
                    binData[j]++;
                    binFound = true;
                }
            }

            if (!binFound) {
                binData[binData.length - 1]++;
            }
        }

        for (int i = 0; i < bins.size() + 1 && !progressDialog.isRunCanceled(); i++) {
            if (i == 0) {
                if (bins.get(i) > 0.0 || bins.get(i) < 0.0) {
                    if (integerBins) {
                        dataset.addValue(binData[i], categoryLabel, "<=" + bins.get(i).intValue() + dataType);
                    } else {
                        dataset.addValue(binData[i], categoryLabel, "<=" + bins.get(i) + dataType);
                    }
                } else if (integerBins) {
                    dataset.addValue(binData[i], categoryLabel, "" + bins.get(i).intValue() + dataType);
                } else {
                    dataset.addValue(binData[i], categoryLabel, "" + bins.get(i) + dataType);
                }
            } else if (i == bins.size()) {
                if (integerBins) {
                    dataset.addValue(binData[i], categoryLabel, ">=" + bins.get(bins.size() - 1).intValue() + dataType);
                } else {
                    dataset.addValue(binData[i], categoryLabel, ">=" + bins.get(bins.size() - 1) + dataType);
                }
            } else if (integerBins) {
                if (bins.get(i).intValue() == bins.get(i - 1).intValue() + 1) {
                    dataset.addValue(binData[i], categoryLabel, "" + bins.get(i).intValue() + dataType);
                } else {
                    dataset.addValue(binData[i], categoryLabel, bins.get(i - 1).intValue() + "-" + bins.get(i).intValue() + dataType);
                }
            } else {
                dataset.addValue(binData[i], categoryLabel, bins.get(i - 1) + "-" + bins.get(i) + dataType);
            }
        }
    }

    /**
     * Returns the dataset for the peptide modification QC plot.
     *
     * @return the dataset for the peptide modification QC plot
     */
    private DefaultCategoryDataset getPeptideModificationsDataset() {

        Identification identification = peptideShakerGUI.getIdentification();
        ModificationParameters modificationParameters = peptideShakerGUI.getIdentificationParameters().getSearchParameters().getModificationParameters();

        ArrayList<String> modificationNames = modificationParameters.getAllNotFixedModifications();
        HashMap<String, Integer> confidentModMap = new HashMap<>(modificationNames.size());
        HashMap<String, Integer> doubtfulModMap = new HashMap<>(modificationNames.size());
        HashMap<String, Integer> notValidatedModMap = new HashMap<>(modificationNames.size());

        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size());
        progressDialog.setValue(0);

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(progressDialog);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
            Peptide peptide = peptideMatch.getPeptide();

            for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {
                String modName = modificationMatch.getModification();
                switch (psParameter.getMatchValidationLevel()) {
                    case confident:
                        Integer occurrence = confidentModMap.get(modName);
                        if (occurrence == null) {
                            confidentModMap.put(modName, 1);
                        } else {
                            confidentModMap.put(modName, occurrence + 1);
                        }
                        break;
                    case doubtful:
                        occurrence = doubtfulModMap.get(modName);
                        if (occurrence == null) {
                            doubtfulModMap.put(modName, 1);
                        } else {
                            doubtfulModMap.put(modName, occurrence + 1);
                        }
                        break;
                    default:
                        occurrence = notValidatedModMap.get(modName);
                        if (occurrence == null) {
                            notValidatedModMap.put(modName, 1);
                        } else {
                            notValidatedModMap.put(modName, occurrence + 1);
                        }
                }
            }

            if (progressDialog.isRunCanceled()) {
                break;
            }
            progressDialog.increaseSecondaryProgressCounter();
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String modName : modificationParameters.getAllNotFixedModifications()) {
            Integer nConfident = confidentModMap.get(modName);
            if (nConfident == null) {
                nConfident = 0;
            }
            dataset.addValue(nConfident, "Confident", modName);
            Integer nDoubtful = confidentModMap.get(modName);
            if (nDoubtful == null) {
                nDoubtful = 0;
            }
            dataset.addValue(nDoubtful, "Doubtful", modName);
            Integer nNonValidated = notValidatedModMap.get(modName);
            if (nNonValidated == null) {
                nNonValidated = 0;
            }
            dataset.addValue(nNonValidated, "Not Validated", modName);
        }

        return dataset;
    }

    /**
     * Returns the dataset for the peptide cleavage site QC plot.
     *
     * @return the dataset for the peptide cleavage site QC plot
     */
    private DefaultCategoryDataset getPeptideCleavageSiteDataset() {

        // @TODO: not yet implemented
        return null;
    }

    /**
     * Returns the dataset for the peptide modification efficiency QC plot.
     *
     * @return the dataset for the peptide modification efficiency QC plot
     */
    private DefaultCategoryDataset getPeptideModificationEfficiencyDataset() {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        Identification identification = peptideShakerGUI.getIdentification();
        SequenceProvider sequenceProvider = peptideShakerGUI.getSequenceProvider();
        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingPreferences = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        ArrayList<String> modNames = modificationParameters.getAllNotFixedModifications();
        HashMap<String, Integer> modifiedSitesMap = new HashMap<>(modNames.size());
        HashMap<String, Integer> possibleSitesMap = new HashMap<>(modNames.size());

        PSParameter psParameter = new PSParameter();

        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size());
        progressDialog.setValue(0);

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(progressDialog);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            psParameter = (PSParameter) peptideMatch.getUrParam(psParameter);
            if (psParameter.getMatchValidationLevel().isValidated()) {
                Peptide peptide = peptideMatch.getPeptide();
                HashMap<String, Integer> peptideModificationsMap = new HashMap<>(peptide.getVariableModifications().length);
                for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {
                    String modName = modificationMatch.getModification();
                    Integer occurrence = peptideModificationsMap.get(modName);
                    if (occurrence == null) {
                        peptideModificationsMap.put(modName, 1);
                    } else {
                        peptideModificationsMap.put(modName, occurrence + 1);
                    }
                }
                for (String modName : modificationParameters.getAllNotFixedModifications()) {
                    Modification modification = modificationFactory.getModification(modName);
                    int[] possibleSites = ModificationUtils.getPossibleModificationSites(peptide, modification, sequenceProvider, modificationSequenceMatchingPreferences);
                    if (possibleSites.length != 0) {
                        Integer occurrencePeptide = peptideModificationsMap.get(modName);
                        if (occurrencePeptide != null) {
                            Integer occurrenceDataset = modifiedSitesMap.get(modName);
                            if (occurrenceDataset == null) {
                                modifiedSitesMap.put(modName, occurrencePeptide);
                            } else {
                                modifiedSitesMap.put(modName, occurrenceDataset + occurrencePeptide);
                            }
                        }
                        Integer possibleSitesDataset = possibleSitesMap.get(modName);
                        if (possibleSitesDataset == null) {
                            possibleSitesMap.put(modName, possibleSites.length);
                        } else {
                            possibleSitesMap.put(modName, possibleSitesDataset + possibleSites.length);
                        }
                    }
                }
            }

            if (progressDialog.isRunCanceled()) {
                break;
            }
            progressDialog.increaseSecondaryProgressCounter();
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String modName : modificationParameters.getAllNotFixedModifications()) {
            Integer nFound = modifiedSitesMap.get(modName);
            if (nFound == null) {
                nFound = 0;
            }
            Integer nPossible = possibleSitesMap.get(modName);
            Double rate = 0.0;
            if (nPossible != null) {
                rate = (100.0 * nFound) / nPossible;
            }
            dataset.addValue(rate, "Modified", modName);
            double rest = 100 - rate;
            dataset.addValue(rest, "Not Modified", modName);
        }

        return dataset;
    }

    /**
     * Returns the dataset for the peptide modification rate QC plot.
     *
     * @return the dataset for the peptide modification rate QC plot
     */
    private DefaultCategoryDataset getPeptideModificationEnrichmentSpecificityDataset() {

        ModificationFactory modificationFactory = ModificationFactory.getInstance();

        Identification identification = peptideShakerGUI.getIdentification();
        SequenceProvider sequenceProvider = peptideShakerGUI.getSequenceProvider();
        IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
        ModificationParameters modificationParameters = identificationParameters.getSearchParameters().getModificationParameters();
        SequenceMatchingParameters modificationSequenceMatchingParameters = identificationParameters.getModificationLocalizationParameters().getSequenceMatchingParameters();

        ArrayList<String> modNames = modificationParameters.getAllNotFixedModifications();
        HashMap<String, Integer> modifiedPeptidesMap = new HashMap<>(modNames.size());
        HashMap<String, Integer> possiblyModifiedPeptidesMap = new HashMap<>(modNames.size());

        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size());
        progressDialog.setValue(0);

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(progressDialog);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
            if (psParameter.getMatchValidationLevel().isValidated()) {
                Peptide peptide = peptideMatch.getPeptide();
                for (String modName : modificationParameters.getAllNotFixedModifications()) {
                    Modification modification = modificationFactory.getModification(modName);
                    int[] possibleSites = ModificationUtils.getPossibleModificationSites(peptide, modification, sequenceProvider, modificationSequenceMatchingParameters);
                    if (possibleSites.length != 0) {
                        Integer nPossiblePeptides = possiblyModifiedPeptidesMap.get(modName);
                        if (nPossiblePeptides == null) {
                            possiblyModifiedPeptidesMap.put(modName, 1);
                        } else {
                            possiblyModifiedPeptidesMap.put(modName, nPossiblePeptides + 1);
                        }
                        boolean modified = false;
                        for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {
                            if (modificationMatch.getModification().equals(modName)) {
                                modified = true;
                                break;
                            }
                        }
                        if (modified) {
                            Integer nModifiedPeptides = modifiedPeptidesMap.get(modName);
                            if (nModifiedPeptides == null) {
                                modifiedPeptidesMap.put(modName, 1);
                            } else {
                                modifiedPeptidesMap.put(modName, nModifiedPeptides + 1);
                            }
                        }
                    }
                }
            }

            if (progressDialog.isRunCanceled()) {
                break;
            }
            progressDialog.increaseSecondaryProgressCounter();
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String modName : modificationParameters.getAllNotFixedModifications()) {
            Integer nFound = modifiedPeptidesMap.get(modName);
            if (nFound == null) {
                nFound = 0;
            }
            Integer nPossible = possiblyModifiedPeptidesMap.get(modName);
            Double rate = 0.0;
            if (nPossible != null) {
                rate = (100.0 * nFound) / nPossible;
            }
            dataset.addValue(rate, "Modified", modName);
            double rest = 0.0;
            if (nPossible != null) {
                rest = 100 - rate;
            }
            dataset.addValue(rest, "Not Modified", modName);
        }

        return dataset;
    }
}
