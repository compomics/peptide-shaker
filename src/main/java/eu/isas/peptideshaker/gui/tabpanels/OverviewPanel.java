/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * OverviewPanel.java
 *
 * Created on Apr 8, 2011, 12:08:09 PM
 */
package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon.PeptideFragmentIonType;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceDataBase;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.SpectrumAnnotator.SpectrumAnnotationMap;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumCollection;
import com.compomics.util.gui.protein.ProteinSequencePane;
import com.compomics.util.gui.spectrum.DefaultSpectrumAnnotation;
import com.compomics.util.gui.spectrum.FragmentIonTable;
import com.compomics.util.gui.spectrum.IntensityHistogram;
import com.compomics.util.gui.spectrum.MassErrorBubblePlot;
import com.compomics.util.gui.spectrum.MassErrorPlot;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.ProteinInferenceDialog;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.preferences.AnnotationPreferences;
import eu.isas.peptideshaker.preferences.SearchParameters;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 *
 * @author vaudel
 */
public class OverviewPanel extends javax.swing.JPanel {

    /** Creates new form OverviewPanel */
    public OverviewPanel(PeptideShakerGUI parent) {

        this.parent = parent;
        this.displayCoverage = false;
        this.displaySpectrum = false;

        initComponents();

        proteinScrollPane.getViewport().setOpaque(false);
        peptideScrollPane.getViewport().setOpaque(false);
        spectraScrollPane.getViewport().setOpaque(false);
        coverageScrollPane.getViewport().setOpaque(false);
        fragmentIonsJScrollPane.getViewport().setOpaque(false);

        // make the tabs in the spectrum tabbed pane go from right to left
        spectrumJTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        psmTable.getTableHeader().setReorderingAllowed(false);

        proteinTable.setAutoCreateRowSorter(true);
        peptideTable.setAutoCreateRowSorter(true);
        psmTable.setAutoCreateRowSorter(true);

        proteinTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMaxWidth(50);
        psmTable.getColumn(" ").setMaxWidth(50);

        int labelWidth = 35;

        proteinTable.getColumn("#Peptides").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, labelWidth);
        proteinTable.getColumn("#Spectra").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, labelWidth);
        proteinTable.getColumn("emPAI").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).showNumberAndChart(true, labelWidth);
        proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
        proteinTable.getColumn("Confidence [%]").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, labelWidth + 5);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence [%]").getCellRenderer()).showNumberAndChart(true, labelWidth + 5);
        proteinTable.getColumn("Coverage").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumberAndChart(true, labelWidth);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).setMinimumChartValue(5d);

        peptideTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
        peptideTable.getColumn("Confidence [%]").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumberAndChart(true, labelWidth + 5);
        ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence [%]").getCellRenderer()).showNumberAndChart(true, labelWidth + 5);

        udateSeparators();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        overviewJPanel = new javax.swing.JPanel();
        overviewJSplitPane = new javax.swing.JSplitPane();
        proteinsJPanel = new javax.swing.JPanel();
        proteinScrollPane = new javax.swing.JScrollPane();
        proteinTable = new javax.swing.JTable();
        coverageJSplitPane = new javax.swing.JSplitPane();
        sequenceCoverageJPanel = new javax.swing.JPanel();
        coverageScrollPane = new javax.swing.JScrollPane();
        coverageEditorPane = new javax.swing.JEditorPane();
        peptidesPsmSpectrumFragmentIonsJSplitPane = new javax.swing.JSplitPane();
        peptidesPsmJSplitPane = new javax.swing.JSplitPane();
        peptidesJPanel = new javax.swing.JPanel();
        peptideScrollPane = new javax.swing.JScrollPane();
        peptideTable = new javax.swing.JTable();
        psmJPanel = new javax.swing.JPanel();
        spectraScrollPane = new javax.swing.JScrollPane();
        psmTable = new javax.swing.JTable();
        spectrumMainPanel = new javax.swing.JPanel();
        spectrumJTabbedPane = new javax.swing.JTabbedPane();
        bubbleJPanel = new javax.swing.JPanel();
        fragmentIonJPanel = new javax.swing.JPanel();
        fragmentIonsJScrollPane = new javax.swing.JScrollPane();
        spectrumJPanel = new javax.swing.JPanel();
        spectrumJToolBar = new javax.swing.JToolBar();
        aIonToggleButton = new javax.swing.JToggleButton();
        bIonToggleButton = new javax.swing.JToggleButton();
        cIonToggleButton = new javax.swing.JToggleButton();
        xIonToggleButton = new javax.swing.JToggleButton();
        yIonToggleButton = new javax.swing.JToggleButton();
        zIonToggleButton = new javax.swing.JToggleButton();
        h2oToggleButton = new javax.swing.JToggleButton();
        nh3ToggleButton = new javax.swing.JToggleButton();
        otherToggleButton = new javax.swing.JToggleButton();
        oneChargeToggleButton = new javax.swing.JToggleButton();
        twoChargesToggleButton = new javax.swing.JToggleButton();
        moreThanTwoChargesToggleButton = new javax.swing.JToggleButton();
        spectrumSplitPane = new javax.swing.JSplitPane();
        sequenceFragmentIonPlotsJPanel = new javax.swing.JPanel();
        spectrumPanel = new javax.swing.JPanel();

        overviewJPanel.setOpaque(false);
        overviewJPanel.setPreferredSize(new java.awt.Dimension(900, 800));

        overviewJSplitPane.setBorder(null);
        overviewJSplitPane.setDividerLocation(300);
        overviewJSplitPane.setDividerSize(0);
        overviewJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        overviewJSplitPane.setResizeWeight(0.5);
        overviewJSplitPane.setOpaque(false);

        proteinsJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinsJPanel.setOpaque(false);

        proteinScrollPane.setOpaque(false);

        proteinTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Description", "Coverage", "emPAI", "#Peptides", "#Spectra", "Score", "Confidence [%]"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class
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
        proteinTable.setOpaque(false);
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                proteinTableMouseClicked(evt);
            }
        });
        proteinTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinTableKeyReleased(evt);
            }
        });
        proteinScrollPane.setViewportView(proteinTable);

        javax.swing.GroupLayout proteinsJPanelLayout = new javax.swing.GroupLayout(proteinsJPanel);
        proteinsJPanel.setLayout(proteinsJPanelLayout);
        proteinsJPanelLayout.setHorizontalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinsJPanelLayout.setVerticalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinsJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
                .addContainerGap())
        );

        overviewJSplitPane.setTopComponent(proteinsJPanel);

        coverageJSplitPane.setBorder(null);
        coverageJSplitPane.setDividerSize(0);
        coverageJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        coverageJSplitPane.setResizeWeight(0.5);
        coverageJSplitPane.setOpaque(false);

        sequenceCoverageJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Protein Sequence Coverage"));
        sequenceCoverageJPanel.setOpaque(false);

        coverageScrollPane.setBorder(null);
        coverageScrollPane.setOpaque(false);

        coverageEditorPane.setBorder(null);
        coverageEditorPane.setContentType("text/html");
        coverageEditorPane.setEditable(false);
        coverageEditorPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                coverageEditorPaneComponentResized(evt);
            }
        });
        coverageScrollPane.setViewportView(coverageEditorPane);

        javax.swing.GroupLayout sequenceCoverageJPanelLayout = new javax.swing.GroupLayout(sequenceCoverageJPanel);
        sequenceCoverageJPanel.setLayout(sequenceCoverageJPanelLayout);
        sequenceCoverageJPanelLayout.setHorizontalGroup(
            sequenceCoverageJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoverageJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coverageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)
                .addContainerGap())
        );
        sequenceCoverageJPanelLayout.setVerticalGroup(
            sequenceCoverageJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequenceCoverageJPanelLayout.createSequentialGroup()
                .addComponent(coverageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE)
                .addContainerGap())
        );

        coverageJSplitPane.setRightComponent(sequenceCoverageJPanel);

        peptidesPsmSpectrumFragmentIonsJSplitPane.setBorder(null);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(450);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerSize(0);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setResizeWeight(0.5);
        peptidesPsmSpectrumFragmentIonsJSplitPane.setOpaque(false);

        peptidesPsmJSplitPane.setBorder(null);
        peptidesPsmJSplitPane.setDividerLocation(150);
        peptidesPsmJSplitPane.setDividerSize(0);
        peptidesPsmJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        peptidesPsmJSplitPane.setResizeWeight(0.5);
        peptidesPsmJSplitPane.setOpaque(false);

        peptidesJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptides"));
        peptidesJPanel.setOpaque(false);

        peptideScrollPane.setOpaque(false);

        peptideTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Other Protein(s)", "Sequence", "Modifications", "#Spectra", "Score", "Confidence [%]"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class
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
        peptideTable.setOpaque(false);
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                peptideTableMouseClicked(evt);
            }
        });
        peptideTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideTableKeyReleased(evt);
            }
        });
        peptideScrollPane.setViewportView(peptideTable);

        javax.swing.GroupLayout peptidesJPanelLayout = new javax.swing.GroupLayout(peptidesJPanel);
        peptidesJPanel.setLayout(peptidesJPanelLayout);
        peptidesJPanelLayout.setHorizontalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
                .addContainerGap())
        );
        peptidesJPanelLayout.setVerticalGroup(
            peptidesJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(peptidesJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(peptideScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPsmJSplitPane.setTopComponent(peptidesJPanel);

        psmJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Peptide-Spectrum Matches"));
        psmJPanel.setOpaque(false);

        spectraScrollPane.setOpaque(false);

        psmTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Sequence", "Modifications", "Charge", "Mass Error", "File", "Title"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.String.class, java.lang.String.class
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
        psmTable.setOpaque(false);
        psmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                psmTableMouseClicked(evt);
            }
        });
        psmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmTableKeyReleased(evt);
            }
        });
        spectraScrollPane.setViewportView(psmTable);

        javax.swing.GroupLayout psmJPanelLayout = new javax.swing.GroupLayout(psmJPanel);
        psmJPanel.setLayout(psmJPanelLayout);
        psmJPanelLayout.setHorizontalGroup(
            psmJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
                .addContainerGap())
        );
        psmJPanelLayout.setVerticalGroup(
            psmJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(psmJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectraScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 55, Short.MAX_VALUE)
                .addContainerGap())
        );

        peptidesPsmJSplitPane.setRightComponent(psmJPanel);

        peptidesPsmSpectrumFragmentIonsJSplitPane.setLeftComponent(peptidesPsmJSplitPane);

        spectrumMainPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum & Fragment Ions"));
        spectrumMainPanel.setOpaque(false);

        spectrumJTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        bubbleJPanel.setOpaque(false);
        bubbleJPanel.setLayout(new java.awt.BorderLayout());
        spectrumJTabbedPane.addTab("Variability", bubbleJPanel);

        fragmentIonJPanel.setOpaque(false);

        fragmentIonsJScrollPane.setOpaque(false);

        javax.swing.GroupLayout fragmentIonJPanelLayout = new javax.swing.GroupLayout(fragmentIonJPanel);
        fragmentIonJPanel.setLayout(fragmentIonJPanelLayout);
        fragmentIonJPanelLayout.setHorizontalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)
                .addContainerGap())
        );
        fragmentIonJPanelLayout.setVerticalGroup(
            fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fragmentIonJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fragmentIonsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addContainerGap())
        );

        spectrumJTabbedPane.addTab("Fragment Ions", fragmentIonJPanel);

        spectrumJPanel.setOpaque(false);

        spectrumJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        spectrumJToolBar.setFloatable(false);
        spectrumJToolBar.setRollover(true);

        aIonToggleButton.setText("a");
        aIonToggleButton.setFocusable(false);
        aIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aIonToggleButton.setMinimumSize(new java.awt.Dimension(25, 21));
        aIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        aIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        aIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(aIonToggleButton);

        bIonToggleButton.setSelected(true);
        bIonToggleButton.setText("b");
        bIonToggleButton.setFocusable(false);
        bIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        bIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(bIonToggleButton);

        cIonToggleButton.setText("c");
        cIonToggleButton.setFocusable(false);
        cIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        cIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(cIonToggleButton);

        xIonToggleButton.setText("x");
        xIonToggleButton.setFocusable(false);
        xIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        xIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        xIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(xIonToggleButton);

        yIonToggleButton.setSelected(true);
        yIonToggleButton.setText("y");
        yIonToggleButton.setFocusable(false);
        yIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        yIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        yIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        yIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(yIonToggleButton);

        zIonToggleButton.setText("z");
        zIonToggleButton.setFocusable(false);
        zIonToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zIonToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        zIonToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        zIonToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zIonToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(zIonToggleButton);

        h2oToggleButton.setText("H2O");
        h2oToggleButton.setFocusable(false);
        h2oToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        h2oToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        h2oToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        h2oToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                h2oToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(h2oToggleButton);

        nh3ToggleButton.setText("NH3");
        nh3ToggleButton.setFocusable(false);
        nh3ToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nh3ToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        nh3ToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        nh3ToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nh3ToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(nh3ToggleButton);

        otherToggleButton.setText("Oth.");
        otherToggleButton.setFocusable(false);
        otherToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        otherToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        otherToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        otherToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                otherToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(otherToggleButton);

        oneChargeToggleButton.setSelected(true);
        oneChargeToggleButton.setText("+");
        oneChargeToggleButton.setFocusable(false);
        oneChargeToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        oneChargeToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        oneChargeToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        oneChargeToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oneChargeToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(oneChargeToggleButton);

        twoChargesToggleButton.setText("++");
        twoChargesToggleButton.setFocusable(false);
        twoChargesToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        twoChargesToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        twoChargesToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        twoChargesToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                twoChargesToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(twoChargesToggleButton);

        moreThanTwoChargesToggleButton.setText(">2 ");
        moreThanTwoChargesToggleButton.setFocusable(false);
        moreThanTwoChargesToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        moreThanTwoChargesToggleButton.setPreferredSize(new java.awt.Dimension(40, 25));
        moreThanTwoChargesToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        moreThanTwoChargesToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moreThanTwoChargesToggleButtonActionPerformed(evt);
            }
        });
        spectrumJToolBar.add(moreThanTwoChargesToggleButton);

        spectrumSplitPane.setBorder(null);
        spectrumSplitPane.setDividerLocation(80);
        spectrumSplitPane.setDividerSize(0);
        spectrumSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spectrumSplitPane.setOpaque(false);

        sequenceFragmentIonPlotsJPanel.setOpaque(false);
        sequenceFragmentIonPlotsJPanel.setLayout(new javax.swing.BoxLayout(sequenceFragmentIonPlotsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        spectrumSplitPane.setTopComponent(sequenceFragmentIonPlotsJPanel);

        spectrumPanel.setOpaque(false);
        spectrumPanel.setLayout(new java.awt.BorderLayout());
        spectrumSplitPane.setRightComponent(spectrumPanel);

        javax.swing.GroupLayout spectrumJPanelLayout = new javax.swing.GroupLayout(spectrumJPanel);
        spectrumJPanel.setLayout(spectrumJPanelLayout);
        spectrumJPanelLayout.setHorizontalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE)
        );
        spectrumJPanelLayout.setVerticalGroup(
            spectrumJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, spectrumJPanelLayout.createSequentialGroup()
                .addComponent(spectrumSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(spectrumJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spectrumJTabbedPane.addTab("Spectrum", spectrumJPanel);

        spectrumJTabbedPane.setSelectedIndex(2);

        javax.swing.GroupLayout spectrumMainPanelLayout = new javax.swing.GroupLayout(spectrumMainPanel);
        spectrumMainPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
            spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(spectrumJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
                .addContainerGap())
        );
        spectrumMainPanelLayout.setVerticalGroup(
            spectrumMainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spectrumJTabbedPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 227, Short.MAX_VALUE)
        );

        peptidesPsmSpectrumFragmentIonsJSplitPane.setRightComponent(spectrumMainPanel);

        coverageJSplitPane.setLeftComponent(peptidesPsmSpectrumFragmentIonsJSplitPane);

        overviewJSplitPane.setRightComponent(coverageJSplitPane);

        javax.swing.GroupLayout overviewJPanelLayout = new javax.swing.GroupLayout(overviewJPanel);
        overviewJPanel.setLayout(overviewJPanelLayout);
        overviewJPanelLayout.setHorizontalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 957, Short.MAX_VALUE)
                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 720, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 977, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(overviewJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 977, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 742, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(overviewJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 742, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void proteinTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseClicked

        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            // update the peptide selection
            updatedPeptideSelection(row);
            // update the sequence coverage map
            String proteinKey = proteinTableMap.get(getProteinKey(row));
            ProteinMatch proteinMatch = parent.getIdentification().getProteinIdentification().get(proteinKey);
            if (proteinMatch.getNProteins() == 1) {
                updateSequenceCoverage(proteinKey);
            } else {
                coverageEditorPane.setText("");
                if (column == 1) {
                    new ProteinInferenceDialog(parent, true, proteinMatch, parent.getIdentification(), parent.getSequenceDataBase());
                }
            }
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
}//GEN-LAST:event_proteinTableMouseClicked

    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased
        proteinTableMouseClicked(null);
}//GEN-LAST:event_proteinTableKeyReleased

    private void coverageEditorPaneComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_coverageEditorPaneComponentResized
        proteinTableMouseClicked(null);
}//GEN-LAST:event_coverageEditorPaneComponentResized

    private void peptideTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseClicked

        int row = peptideTable.rowAtPoint(evt.getPoint());

        if (row != -1) {
            updatePsmSelection(row);
            updateSpectrum(row);
        }
}//GEN-LAST:event_peptideTableMouseClicked

    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased
        int row = peptideTable.getSelectedRow();

        if (row != -1) {
            updatePsmSelection(row);
            row = psmTable.getSelectedRow();
            updateSpectrum(row);
        }
}//GEN-LAST:event_peptideTableKeyReleased

    private void psmTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseClicked
        int row = psmTable.rowAtPoint(evt.getPoint());

        if (row != -1) {
            updateSpectrum(row);
        }
}//GEN-LAST:event_psmTableMouseClicked

    private void psmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmTableKeyReleased
        int row = psmTable.getSelectedRow();

        if (row != -1) {
            updateSpectrum(row);
        }
}//GEN-LAST:event_psmTableKeyReleased

    private void aIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aIonToggleButtonActionPerformed
        if (spectrum != null) {
            // update the spectrum plots
            psmTableKeyReleased(null);
        }
}//GEN-LAST:event_aIonToggleButtonActionPerformed

    private void bIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_bIonToggleButtonActionPerformed

    private void cIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_cIonToggleButtonActionPerformed

    private void xIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_xIonToggleButtonActionPerformed

    private void yIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_yIonToggleButtonActionPerformed

    private void zIonToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zIonToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_zIonToggleButtonActionPerformed

    private void h2oToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_h2oToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_h2oToggleButtonActionPerformed

    private void nh3ToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nh3ToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_nh3ToggleButtonActionPerformed

    private void otherToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_otherToggleButtonActionPerformed

    private void oneChargeToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oneChargeToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_oneChargeToggleButtonActionPerformed

    private void twoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twoChargesToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_twoChargesToggleButtonActionPerformed

    private void moreThanTwoChargesToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moreThanTwoChargesToggleButtonActionPerformed
        aIonToggleButtonActionPerformed(null);
}//GEN-LAST:event_moreThanTwoChargesToggleButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton aIonToggleButton;
    private javax.swing.JToggleButton bIonToggleButton;
    private javax.swing.JPanel bubbleJPanel;
    private javax.swing.JToggleButton cIonToggleButton;
    private javax.swing.JEditorPane coverageEditorPane;
    private javax.swing.JSplitPane coverageJSplitPane;
    private javax.swing.JScrollPane coverageScrollPane;
    private javax.swing.JPanel fragmentIonJPanel;
    private javax.swing.JScrollPane fragmentIonsJScrollPane;
    private javax.swing.JToggleButton h2oToggleButton;
    private javax.swing.JToggleButton moreThanTwoChargesToggleButton;
    private javax.swing.JToggleButton nh3ToggleButton;
    private javax.swing.JToggleButton oneChargeToggleButton;
    private javax.swing.JToggleButton otherToggleButton;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JSplitPane overviewJSplitPane;
    private javax.swing.JScrollPane peptideScrollPane;
    private javax.swing.JTable peptideTable;
    private javax.swing.JPanel peptidesJPanel;
    private javax.swing.JSplitPane peptidesPsmJSplitPane;
    private javax.swing.JSplitPane peptidesPsmSpectrumFragmentIonsJSplitPane;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JPanel proteinsJPanel;
    private javax.swing.JPanel psmJPanel;
    private javax.swing.JTable psmTable;
    private javax.swing.JPanel sequenceCoverageJPanel;
    private javax.swing.JPanel sequenceFragmentIonPlotsJPanel;
    private javax.swing.JScrollPane spectraScrollPane;
    private javax.swing.JPanel spectrumJPanel;
    private javax.swing.JTabbedPane spectrumJTabbedPane;
    private javax.swing.JToolBar spectrumJToolBar;
    private javax.swing.JPanel spectrumMainPanel;
    private javax.swing.JPanel spectrumPanel;
    private javax.swing.JSplitPane spectrumSplitPane;
    private javax.swing.JToggleButton twoChargesToggleButton;
    private javax.swing.JToggleButton xIonToggleButton;
    private javax.swing.JToggleButton yIonToggleButton;
    private javax.swing.JToggleButton zIonToggleButton;
    // End of variables declaration//GEN-END:variables
    /**
     * The spectrum annotator
     */
    private SpectrumAnnotator spectrumAnnotator = new SpectrumAnnotator();
    /**
     * Boolean indicating whether the peak list error has already been displayed
     */
    private boolean peakListError = false;
    /**
     * The current spectrum annotations.
     */
    private Vector<DefaultSpectrumAnnotation> currentAnnotations;
    /**
     * The color used for the sparkline bar chart plots.
     */
    private Color sparklineColor = new Color(110, 196, 97);
    /**
     * The current spectrum panel.
     */
    private SpectrumPanel spectrum;
    /**
     * boolean indicating whether the spectrum shall be displayed
     */
    private boolean displaySpectrum;
    /**
     * Boolean indicating whether the sequence coverage shall be displayed
     */
    private boolean displayCoverage;
    /**
     * A mapping of the protein table entries
     */
    private HashMap<String, String> proteinTableMap = new HashMap<String, String>();
    /**
     * A mapping of the peptide table entries
     */
    private HashMap<String, String> peptideTableMap = new HashMap<String, String>();
    /**
     * A mapping of the psm table entries
     */
    private HashMap<String, String> psmTableMap = new HashMap<String, String>();
    /**
     * The main GUI
     */
    private PeptideShakerGUI parent;

    /**
     * Displays or hide sparklines in tables
     * @param showSparkLines    boolean indicating whether sparklines shall be displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence [%]").getCellRenderer()).showNumbers(!showSparkLines);
        proteinTable.revalidate();
        proteinTable.repaint();
    }

    private String getPsmKey(int row) {
        return (String) psmTable.getValueAt(row, 5) + (String) psmTable.getValueAt(row, 6);
    }

    private String getPeptideKey(int row) {
        return (String) peptideTable.getValueAt(row, 2);
    }

    private String getProteinKey(int row) {
        return (String) proteinTable.getValueAt(row, 1);
    }

    /**
     * Method called whenever the component is resized to maintain the look of the GUI
     */
    public void udateSeparators() {
        overviewJSplitPane.setDividerLocation(overviewJSplitPane.getHeight() / 100 * 30);
        overviewJSplitPane.validate();
        overviewJSplitPane.repaint();
        // invoke later to give time for components to update
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                peptidesPsmJSplitPane.setDividerLocation(peptidesPsmJSplitPane.getHeight() / 2);
                peptidesPsmJSplitPane.validate();
                peptidesPsmJSplitPane.repaint();
                // invoke later to give time for components to update
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        if (displayCoverage) {
                            coverageJSplitPane.setDividerLocation(coverageJSplitPane.getHeight() / 10 * 7);
                        } else {
                            coverageJSplitPane.setDividerLocation(Integer.MAX_VALUE);
                        }

                        coverageJSplitPane.validate();
                        coverageJSplitPane.repaint();

                        // invoke later to give time for components to update
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                if (displaySpectrum) {
                                    peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(peptidesPsmSpectrumFragmentIonsJSplitPane.getWidth() / 2);
                                } else {
                                    peptidesPsmSpectrumFragmentIonsJSplitPane.setDividerLocation(Integer.MAX_VALUE);
                                }

                                spectrumSplitPane.validate();
                                spectrumSplitPane.repaint();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Sets the whether the protein coverage and the spectrum shall be displayed
     * @param displayCoverage   boolean indicating whether the protein coverage shall be displayed
     * @param displaySpectrum   boolean indicating whether the spectrum shall be displayed
     */
    public void setDisplayOptions(boolean displayCoverage, boolean displaySpectrum) {
        this.displayCoverage = displayCoverage;
        this.displaySpectrum = displaySpectrum;
    }

    /**
     * Updated the bubble plot with the current PSMs.
     */
    private void updateBubblePlot() {
        try {
            ArrayList<SpectrumAnnotationMap> allAnnotations = new ArrayList<SpectrumAnnotationMap>();
            ArrayList<MSnSpectrum> allSpectra = new ArrayList<MSnSpectrum>();

            String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
            PeptideMatch selectedPeptideMatch = parent.getIdentification().getPeptideIdentification().get(peptideKey);
            SpectrumCollection spectrumCollection = parent.getSpectrumCollection();
            AnnotationPreferences annotationPreferences = parent.getAnnotationPreferences();
            SearchParameters searchParameters = parent.getSearchParameters();



            for (SpectrumMatch spectrumMatch : selectedPeptideMatch.getSpectrumMatches().values()) {

                MSnSpectrum currentSpectrum = (MSnSpectrum) spectrumCollection.getSpectrum(2, spectrumMatch.getKey());

                // get the spectrum annotations
                SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
                        selectedPeptideMatch.getTheoreticPeptide(), currentSpectrum, searchParameters.getFragmentIonMZTolerance(),
                        currentSpectrum.getIntensityLimit(annotationPreferences.shallAnnotateMostIntensePeaks()));

                allAnnotations.add(annotations);
                allSpectra.add(currentSpectrum);


            }

            MassErrorBubblePlot massErrorBubblePlot = new MassErrorBubblePlot(
                    allAnnotations, getCurrentFragmentIonTypes(), allSpectra, searchParameters.getFragmentIonMZTolerance(),
                    oneChargeToggleButton.isSelected(), twoChargesToggleButton.isSelected(),
                    moreThanTwoChargesToggleButton.isSelected(), true);

            bubbleJPanel.removeAll();
            bubbleJPanel.add(massErrorBubblePlot);



        } catch (MzMLUnmarshallerException e) {
            JOptionPane.showMessageDialog(this, "Error while importing mzML data.", "Peak Lists Error", JOptionPane.INFORMATION_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Updates the sequence coverage pane.
     *
     * @param proteinAccession
     */
    private void updateSequenceCoverage(String proteinAccession) {
        SequenceDataBase db = parent.getSequenceDataBase();

        if (db != null) {
            ArrayList<Integer> selectedPeptideStart = new ArrayList<Integer>();
            ArrayList<Integer> selectedPeptideEnd = new ArrayList<Integer>();

            String cleanSequence = db.getProtein(proteinAccession).getSequence();
            String tempSequence = cleanSequence;
            String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
            String peptideSequence = parent.getIdentification().getPeptideIdentification().get(peptideKey).getTheoreticPeptide().getSequence();

            int startIndex = 0;
            while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                startIndex += tempSequence.lastIndexOf(peptideSequence) + 1;
                selectedPeptideStart.add(startIndex);
                selectedPeptideEnd.add(startIndex + peptideSequence.length());
                tempSequence = cleanSequence.substring(0, startIndex);
            }
            // an array containing the coverage index for each residue
            int[] coverage = new int[cleanSequence.length() + 1];

            // iterate the peptide table and store the coverage for each peptide
            for (int i = 0; i < peptideTable.getRowCount(); i++) {
                peptideKey = peptideTableMap.get(getPeptideKey(i));
                peptideSequence = parent.getIdentification().getPeptideIdentification().get(peptideKey).getTheoreticPeptide().getSequence();
                tempSequence = cleanSequence;

                while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                    int peptideTempStart = cleanSequence.lastIndexOf(peptideSequence) + 1;
                    int peptideTempEnd = peptideTempStart + peptideSequence.length();
                    for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                        coverage[j]++;
                    }
                    tempSequence = cleanSequence.substring(0, peptideTempStart);
                }
            }

            double sequenceCoverage = ProteinSequencePane.formatProteinSequence(
                    coverageEditorPane, db.getProtein(proteinAccession).getSequence(), selectedPeptideStart, selectedPeptideEnd, coverage);

            ((TitledBorder) sequenceCoverageJPanel.getBorder()).setTitle("Proten Sequence Coverage (" + Util.roundDouble(sequenceCoverage, 2) + "%)");
        }
    }

    private double estimateSequenceCoverage(ProteinMatch proteinMatch, String sequence) {

        // an array containing the coverage index for each residue
        int[] coverage = new int[sequence.length() + 1];
        int peptideTempStart, peptideTempEnd;
        String tempSequence, peptideSequence;

        // iterate the peptide table and store the coverage for each peptide
        for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
            tempSequence = sequence;
            peptideSequence = peptideMatch.getTheoreticPeptide().getSequence();
            peptideTempStart = 0;
            while (tempSequence.lastIndexOf(peptideSequence) >= 0) {
                peptideTempStart += tempSequence.lastIndexOf(peptideSequence) + 1;
                peptideTempEnd = peptideTempStart + peptideSequence.length();
                for (int j = peptideTempStart; j < peptideTempEnd; j++) {
                    coverage[j] = 1;
                }
                tempSequence = sequence.substring(0, peptideTempStart);
            }
        }
        double covered = 0.0;
        for (int aa : coverage) {
            covered += aa;
        }
        return covered / ((double) sequence.length());
    }

    private void updateSpectrum(int row) {

        if (row != -1) {
            String spectrumKey = psmTableMap.get(getPsmKey(row));
            if (displaySpectrum) {
                try {
                    // These spectra should be MS2 spectra
                    MSnSpectrum currentSpectrum = (MSnSpectrum) parent.getSpectrumCollection().getSpectrum(2, spectrumKey);
                    HashSet<Peak> peaks = currentSpectrum.getPeakList();

                    if (peaks == null || peaks.isEmpty()) {
                        if (!peakListError) {
                            JOptionPane.showMessageDialog(this, "Peak lists not imported.", "Peak Lists Error", JOptionPane.INFORMATION_MESSAGE);
                            peakListError = true;
                        }
                    } else {

                        // add the data to the spectrum panel
                        Precursor precursor = currentSpectrum.getPrecursor();
                        spectrum = new SpectrumPanel(
                                currentSpectrum.getMzValuesAsArray(), currentSpectrum.getIntensityValuesAsArray(),
                                precursor.getMz(), precursor.getCharge().toString(),
                                "", 40, false, false, false, 2, false);
                        spectrum.setBorder(null);

                        // get the spectrum annotations
                        String peptideKey = peptideTableMap.get(getPeptideKey(peptideTable.getSelectedRow()));
                        Peptide currentPeptide = parent.getIdentification().getPeptideIdentification().get(peptideKey).getTheoreticPeptide();
                        SpectrumAnnotationMap annotations = spectrumAnnotator.annotateSpectrum(
                                currentPeptide, currentSpectrum, parent.getSearchParameters().getFragmentIonMZTolerance(),
                                currentSpectrum.getIntensityLimit(parent.getAnnotationPreferences().shallAnnotateMostIntensePeaks()));

                        // add the spectrum annotations
                        currentAnnotations = spectrumAnnotator.getSpectrumAnnotations(annotations);
                        spectrum.setAnnotations(filterAnnotations(currentAnnotations));

                        // add the spectrum panel to the frame
                        spectrumPanel.removeAll();
                        spectrumPanel.add(spectrum);
                        spectrumPanel.revalidate();
                        spectrumPanel.repaint();

                        // create and display the fragment ion table
                        fragmentIonsJScrollPane.setViewportView(new FragmentIonTable(currentPeptide, annotations));

                        // create the sequence fragment ion view
                        sequenceFragmentIonPlotsJPanel.removeAll();
                        SequenceFragmentationPanel sequenceFragmentationPanel =
                                new SequenceFragmentationPanel(currentPeptide.getSequence(), annotations, false); // @TODO: what about modified sequences?? -> We first need to assess confidently the position of the PTM
                        sequenceFragmentationPanel.setMinimumSize(new Dimension(sequenceFragmentationPanel.getPreferredSize().width, sequenceFragmentationPanel.getHeight()));
                        sequenceFragmentationPanel.setOpaque(false);
                        sequenceFragmentationPanel.setToolTipText("Sequence Fragmentation");
                        sequenceFragmentIonPlotsJPanel.add(sequenceFragmentationPanel);

                        // create the intensity histograms
                        sequenceFragmentIonPlotsJPanel.add(new IntensityHistogram(
                                annotations, getCurrentFragmentIonTypes(), currentSpectrum,
                                parent.getAnnotationPreferences().shallAnnotateMostIntensePeaks(),
                                oneChargeToggleButton.isSelected(), twoChargesToggleButton.isSelected(),
                                moreThanTwoChargesToggleButton.isSelected()));

                        // create the miniature mass error plot
                        MassErrorPlot massErrorPlot = new MassErrorPlot(
                                annotations, getCurrentFragmentIonTypes(), currentSpectrum,
                                parent.getSearchParameters().getFragmentIonMZTolerance(),
                                oneChargeToggleButton.isSelected(), twoChargesToggleButton.isSelected(),
                                moreThanTwoChargesToggleButton.isSelected());

                        if (massErrorPlot.getNumberOfDataPointsInPlot() > 0) {
                            sequenceFragmentIonPlotsJPanel.add(massErrorPlot);
                        }

                        // update the UI
                        sequenceFragmentIonPlotsJPanel.revalidate();
                        sequenceFragmentIonPlotsJPanel.repaint();
                        updateBubblePlot();
                    }
                } catch (MzMLUnmarshallerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Filters the annotations and returns the annotations matching the currently selected list.
     *
     * @param annotations the annotations to be filtered
     * @return the filtered annotations
     */
    private Vector<DefaultSpectrumAnnotation> filterAnnotations(Vector<DefaultSpectrumAnnotation> annotations) {

        return SpectrumPanel.filterAnnotations(annotations, getCurrentFragmentIonTypes(),
                h2oToggleButton.isSelected(),
                nh3ToggleButton.isSelected(),
                oneChargeToggleButton.isSelected(),
                twoChargesToggleButton.isSelected(),
                moreThanTwoChargesToggleButton.isSelected());
    }

    /**
     * Returns an arraylist of the currently selected fragment ion types.
     *
     * @return an arraylist of the currently selected fragment ion types
     */
    private ArrayList<PeptideFragmentIonType> getCurrentFragmentIonTypes() {

        ArrayList<PeptideFragmentIonType> fragmentIontypes = new ArrayList<PeptideFragmentIonType>();

        if (aIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.A_ION);
            if (h2oToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.AH2O_ION);
            }
            if (nh3ToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.ANH3_ION);
            }
        }

        if (bIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.B_ION);
            if (h2oToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.BH2O_ION);
            }
            if (nh3ToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.BNH3_ION);
            }
        }

        if (cIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.C_ION);
        }

        if (xIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.X_ION);
        }

        if (yIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.Y_ION);
            if (h2oToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.YH2O_ION);
            }
            if (nh3ToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.YNH3_ION);
            }
        }

        if (zIonToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.Z_ION);
        }

        if (otherToggleButton.isSelected()) {
            fragmentIontypes.add(PeptideFragmentIonType.IMMONIUM);
            fragmentIontypes.add(PeptideFragmentIonType.MH_ION);

            if (h2oToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.MHH2O_ION);
            }
            if (nh3ToggleButton.isSelected()) {
                fragmentIontypes.add(PeptideFragmentIonType.MHNH3_ION);
            }
        }

        return fragmentIontypes;


    }

    private void updatePsmSelection(int row) {

        if (row != -1) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            // update the sequence coverage map
            String proteinKey = proteinTableMap.get(getProteinKey(proteinTable.getSelectedRow()));
            ProteinMatch proteinMatch = parent.getIdentification().getProteinIdentification().get(proteinKey);


            if (proteinMatch.getNProteins() == 1) {
                updateSequenceCoverage(proteinKey);
            } else {
                coverageEditorPane.setText("");
            }

            while (psmTable.getRowCount() > 0) {
                ((DefaultTableModel) psmTable.getModel()).removeRow(0);
            }

            spectrumPanel.removeAll();
            spectrumPanel.revalidate();
            spectrumPanel.repaint();

            String peptideKey = peptideTableMap.get(getPeptideKey(row));
            PeptideMatch currentPeptideMatch = parent.getIdentification().getPeptideIdentification().get(peptideKey);

            int index = 1;
            psmTableMap = new HashMap<String, String>();

            for (SpectrumMatch spectrumMatch : currentPeptideMatch.getSpectrumMatches().values()) {
                PeptideAssumption peptideAssumption = spectrumMatch.getBestAssumption();
                String modifications = "";

                for (ModificationMatch mod : peptideAssumption.getPeptide().getModificationMatches()) {
                    if (mod.isVariable()) {
                        modifications += mod.getTheoreticPtm().getName() + ", ";
                    }
                }

                if (modifications.length() > 0) {
                    modifications = modifications.substring(0, modifications.length() - 2);
                } else {
                    modifications = null;
                }

                try {
                    String spectrumKey = spectrumMatch.getKey();
                    MSnSpectrum tempSpectrum = (MSnSpectrum) parent.getSpectrumCollection().getSpectrum(2, spectrumKey);

                    ((DefaultTableModel) psmTable.getModel()).addRow(new Object[]{
                                index,
                                peptideAssumption.getPeptide().getSequence(),
                                modifications,
                                tempSpectrum.getPrecursor().getCharge().toString(),
                                peptideAssumption.getDeltaMass(),
                                tempSpectrum.getFileName(),
                                tempSpectrum.getSpectrumTitle()
                            });
                    psmTableMap.put(getPsmKey(index - 1), spectrumKey);
                    index++;

                } catch (MzMLUnmarshallerException e) {
                    e.printStackTrace();
                }
            }

            // select the first spectrum in the table
            if (psmTable.getRowCount() > 0) {
                psmTable.setRowSelectionInterval(0, 0);
                psmTableKeyReleased(
                        null);
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Updates the peptide selection.
     *
     * @param row
     */
    private void updatedPeptideSelection(int row) {

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
            while (peptideTable.getRowCount() > 0) {
                ((DefaultTableModel) peptideTable.getModel()).removeRow(0);
            }

            while (psmTable.getRowCount() > 0) {
                ((DefaultTableModel) psmTable.getModel()).removeRow(0);
            }

            spectrumPanel.removeAll();
            spectrumPanel.revalidate();
            spectrumPanel.repaint();

            String proteinKey = proteinTableMap.get(getProteinKey(row));
            peptideTableMap = new HashMap<String, String>();

            ProteinMatch proteinMatch = parent.getIdentification().getProteinIdentification().get(proteinKey);
            HashMap<Double, ArrayList<PeptideMatch>> peptideMap = new HashMap<Double, ArrayList<PeptideMatch>>();
            PSParameter probabilities = new PSParameter();
            double peptideProbabilityScore;
            for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
                probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);
                peptideProbabilityScore = probabilities.getPeptideProbabilityScore();
                if (!peptideMap.containsKey(peptideProbabilityScore)) {
                    peptideMap.put(peptideProbabilityScore, new ArrayList<PeptideMatch>());
                }
                peptideMap.get(peptideProbabilityScore).add(peptideMatch);
            }
            ArrayList<Double> scores = new ArrayList<Double>(peptideMap.keySet());
            Collections.sort(scores);

            int index = 0;
            for (double score : scores) {
                for (PeptideMatch peptideMatch : peptideMap.get(score)) {
                    ArrayList<String> proteinAccessions = new ArrayList<String>();
                    for (Protein protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                        proteinAccessions.add(protein.getAccession());
                    }

                    String modifications = "";
                    for (ModificationMatch mod : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
                        if (mod.isVariable()) {
                            modifications += mod.getTheoreticPtm().getName() + ", ";
                        }
                    }

                    if (modifications.length() > 0) {
                        modifications = modifications.substring(0, modifications.length() - 2);
                    } else {
                        modifications = null;
                    }

                    probabilities = (PSParameter) peptideMatch.getUrParam(probabilities);
                    String otherProteins = "";
                    boolean newProtein;
                    for (Protein protein : peptideMatch.getTheoreticPeptide().getParentProteins()) {
                        newProtein = true;
                        for (String referenceAccession : proteinMatch.getTheoreticProteinsAccessions()) {
                            if (proteinMatch.getTheoreticProtein(referenceAccession).getAccession().equals(protein.getAccession())) {
                                newProtein = false;
                            }
                        }
                        if (newProtein) {
                            otherProteins += protein.getAccession() + " ";
                        }
                    }

                    ((DefaultTableModel) peptideTable.getModel()).addRow(new Object[]{
                                index + 1,
                                otherProteins,
                                peptideMatch.getTheoreticPeptide().getSequence(),
                                modifications,
                                peptideMatch.getSpectrumCount(),
                                probabilities.getPeptideScore(),
                                probabilities.getPeptideConfidence()
                            });

                    peptideTableMap.put(getPeptideKey(index), peptideMatch.getKey());
                    index++;
                }
            }

            // select the first peptide in the table

            if (peptideTable.getRowCount() > 0) {
                peptideTable.setRowSelectionInterval(0, 0);
                peptideTableKeyReleased(
                        null);


            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));


        }
    }

    /**
     * Displays the results in the result tables.
     * @throws MzMLUnmarshallerException
     */
    public void displayResults() throws MzMLUnmarshallerException {

        ProteomicAnalysis proteomicAnalysis = parent.getProteomicanalysis();
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        int index = 0, maxPeptides = 0, maxSpectra = 0;
        double sequenceCoverage = 0;
        double emPAI = 0, maxEmPAI = 0;
        String description = "";
        SequenceDataBase db = proteomicAnalysis.getSequenceDataBase();

        // sort the proteins according to the protein score, then number of peptides (inverted), then number of spectra (inverted).
        HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>> orderMap = new HashMap<Double, HashMap<Integer, HashMap<Integer, ArrayList<String>>>>(); // Maps are my passion
        ArrayList<Double> scores = new ArrayList<Double>();
        PSParameter probabilities = new PSParameter();
        ProteinMatch proteinMatch;
        double score;
        int nPeptides, nSpectra;
        for (String key : parent.getIdentification().getProteinIdentification().keySet()) {
            proteinMatch = parent.getIdentification().getProteinIdentification().get(key);
            probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);
            score = probabilities.getProteinProbabilityScore();
            nPeptides = -proteinMatch.getPeptideMatches().size();
            nSpectra = -proteinMatch.getSpectrumCount();
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
            orderMap.get(score).get(nPeptides).get(nSpectra).add(key);
        }
        Collections.sort(scores);
        proteinTableMap = new HashMap<String, String>();
        // add the proteins to the table
        ArrayList<Integer> nP, nS;
        ArrayList<String> keys;
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
                        proteinMatch = parent.getIdentification().getProteinIdentification().get(proteinKey);
                        probabilities = (PSParameter) proteinMatch.getUrParam(probabilities);
                        try {
                            Protein currentProtein = db.getProtein(proteinKey);
                            int nPossible = currentProtein.getNPossiblePeptides(parent.getSearchParameters().getEnzyme());
                            emPAI = (Math.pow(10, ((double) proteinMatch.getPeptideMatches().size()) / ((double) nPossible))) - 1;
                            description = currentProtein.getDescription();
                            sequenceCoverage = 100 * estimateSequenceCoverage(proteinMatch, currentProtein.getSequence());
                        } catch (Exception e) {
                            description = "";
                            emPAI = 0;
                            sequenceCoverage = 0;
                        } // only add non-decoy matches to the overview
                        if (!proteinMatch.isDecoy()) {
                            ((DefaultTableModel) proteinTable.getModel()).addRow(new Object[]{
                                        index + 1,
                                        proteinKey,
                                        description,
                                        sequenceCoverage,
                                        emPAI,
                                        proteinMatch.getPeptideMatches().size(),
                                        proteinMatch.getSpectrumCount(),
                                        probabilities.getProteinScore(),
                                        probabilities.getProteinConfidence()});
                            proteinTableMap.put(getProteinKey(index), proteinKey);
                            index++;
                        }
                        if (maxPeptides < proteinMatch.getPeptideMatches().size()) {
                            maxPeptides = proteinMatch.getPeptideMatches().size();
                        }
                        if (maxSpectra < proteinMatch.getSpectrumCount()) {
                            maxSpectra = proteinMatch.getSpectrumCount();
                        }
                        if (maxEmPAI < emPAI) {
                            maxEmPAI = emPAI;
                        }
                    }
                }
            }
        }
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(maxPeptides);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(maxSpectra);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("emPAI").getCellRenderer()).setMaxValue(maxEmPAI);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence [%]").getCellRenderer()).setMaxValue(100.0);

        // select the first row
        if (proteinTable.getRowCount() > 0) {
            proteinTable.setRowSelectionInterval(0, 0);
            proteinTableMouseClicked(
                    null);
            proteinTable.requestFocus();
        }
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }
}
