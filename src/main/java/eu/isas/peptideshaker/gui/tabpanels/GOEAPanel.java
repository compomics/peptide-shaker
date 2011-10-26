package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.myparameters.PSParameter;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.util.BarChartColorRenderer;
import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * The PeptideShaker GO Enrichment Analysis (GO EA) tab.
 * 
 * @author Harald Barsnes
 */
public class GOEAPanel extends javax.swing.JPanel {

    private PeptideShakerGUI peptideShakerGUI;

    /** Creates new form GOEAPanel */
    public GOEAPanel(PeptideShakerGUI peptideShakerGUI) {

        this.peptideShakerGUI = peptideShakerGUI;

        initComponents();

        proteinGoMappingsScrollPane.getViewport().setOpaque(false);
        proteinGoMappingsTable.getTableHeader().setReorderingAllowed(false);
        proteinGoMappingsTable.setAutoCreateRowSorter(true);

        // the index column
        proteinGoMappingsTable.getColumn("").setMaxWidth(60);
        proteinGoMappingsTable.getColumn("").setMinWidth(60);
        proteinGoMappingsTable.getColumn(" ").setMaxWidth(30);
        proteinGoMappingsTable.getColumn(" ").setMinWidth(30);
        
        proteinGoMappingsTable.getColumn(" ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/accept.png")),
                null,
                "Validated", "Not Validated"));

        // make the tabs in the tabbed pane go from right to left
        goPlotsTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    public void displayResults(ProgressDialogX progressDialog) {

        // clear old table
        while (proteinGoMappingsTable.getRowCount() > 0) {
            ((DefaultTableModel) proteinGoMappingsTable.getModel()).removeRow(0);
        }

        String path = peptideShakerGUI.getJarFilePath() + File.separator + "conf"
                + File.separator + "gene_ontology" + File.separator + "ensembl_26062011.txt";

        File goMappingsFile = new File(path);

        TreeMap<String, Integer> totalGoTermUsage = new TreeMap<String, Integer>();
        TreeMap<String, Integer> datasetGoTermUsage = new TreeMap<String, Integer>();
        HashMap<String, String> goNameToAccessionMap = new HashMap<String, String>();
        HashMap<String, ArrayList<String>> proteinToGoMappings = new HashMap<String, ArrayList<String>>();

        int totalNumberOfProteins = 0;

        try {
            FileReader r = new FileReader(goMappingsFile);
            BufferedReader br = new BufferedReader(r);

            // read and ignore the header
            br.readLine();

            String line = br.readLine();

            while (line != null) {

                if (!line.startsWith(",,")) {

                    String[] elements = line.split(",");

                    if (elements.length == 3 && !line.endsWith("\",")) {
                        String goAccession = elements[0];
                        String goName = elements[1].toLowerCase();
                        String proteinAccession = elements[2];

                        if (!proteinToGoMappings.containsKey(proteinAccession)) {
                            ArrayList<String> proteinGoMappings = new ArrayList<String>();
                            proteinGoMappings.add(goName);
                            proteinToGoMappings.put(proteinAccession, proteinGoMappings);
                        } else {
                            proteinToGoMappings.get(proteinAccession).add(goName);
                        }

//                        ((DefaultTableModel) proteinGoMappingsTable.getModel()).addRow(new Object[]{
//                                    proteinGoMappingsTable.getRowCount() + 1,
//                                    proteinAccession,
//                                    goAccession,
//                                    goName
//                                });

                        goNameToAccessionMap.put(goName, goAccession);

                        if (totalGoTermUsage.containsKey(goName)) {
                            totalGoTermUsage.put(goName, totalGoTermUsage.get(goName) + 1);
                        } else {
                            totalGoTermUsage.put(goName, 1);
                        }

                        totalNumberOfProteins++;

                    } else if (line.indexOf("\"") != -1) {

                        if (!line.endsWith("\",")) {

                            String goAccession = line.substring(0, line.indexOf(","));
                            String goName = line.substring(line.indexOf(",") + 1, line.lastIndexOf(",")).toLowerCase();

                            if (goName.startsWith("\"")) {
                                goName = goName.substring(1);
                            }
                            if (goName.endsWith("\"")) {
                                goName = goName.substring(0, goName.length() - 1);
                            }

                            String proteinAccession = line.substring(line.lastIndexOf(",") + 1);

                            if (!proteinToGoMappings.containsKey(proteinAccession)) {
                                ArrayList<String> proteinGoMappings = new ArrayList<String>();
                                proteinGoMappings.add(goName);
                                proteinToGoMappings.put(proteinAccession, proteinGoMappings);
                            } else {
                                proteinToGoMappings.get(proteinAccession).add(goName);
                            }

//                            ((DefaultTableModel) proteinGoMappingsTable.getModel()).addRow(new Object[]{
//                                        proteinGoMappingsTable.getRowCount() + 1,
//                                        proteinAccession,
//                                        goAccession,
//                                        goName
//                                    });

                            goNameToAccessionMap.put(goAccession, goName);

                            if (totalGoTermUsage.containsKey(goName)) {
                                totalGoTermUsage.put(goName, totalGoTermUsage.get(goName) + 1);
                            } else {
                                totalGoTermUsage.put(goName, 1);
                            }

                            totalNumberOfProteins++;
                        }

                    } else {
                        // ignore, as there are no protein mappings
                        //System.out.println(line);
                    }
                }

                line = br.readLine();
            }

            
            // get go term for dataset
            Identification identification = peptideShakerGUI.getIdentification();
            ArrayList<String> allProjectProteins = identification.getProteinIdentification();
            PSParameter proteinPSParameter = new PSParameter();

            for (int i = 0; i < allProjectProteins.size(); i++) {

                try {
                    proteinPSParameter = (PSParameter) identification.getMatchParameter(allProjectProteins.get(i), proteinPSParameter);

                    // @TODO: what about protein groups?? use main match?
                    
                    if (proteinPSParameter.isValidated() && !ProteinMatch.isDecoy(allProjectProteins.get(i))) {

                        if (proteinToGoMappings.containsKey(allProjectProteins.get(i))) {

                            ArrayList<String> goTerms = proteinToGoMappings.get(allProjectProteins.get(i));

                            for (int j = 0; j < goTerms.size(); j++) {
                                if (datasetGoTermUsage.containsKey(goTerms.get(j))) {
                                    datasetGoTermUsage.put(goTerms.get(j), datasetGoTermUsage.get(goTerms.get(j)) + 1);
                                } else {
                                    datasetGoTermUsage.put(goTerms.get(j), 1);
                                }
                            }
                        } else {
                            System.out.println("not found: " + allProjectProteins.get(i));
                        }

                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            
            

            // display the plot
            DefaultCategoryDataset frquencyPlotDataset = new DefaultCategoryDataset();
            DefaultCategoryDataset significancePlotDataset = new DefaultCategoryDataset();
            
            ArrayList<Color> significanceColors = new ArrayList<Color>();

            for (Map.Entry<String, Integer> entry : totalGoTermUsage.entrySet()) {
                String goName = entry.getKey();
                Integer frequencyAll = entry.getValue();

                String goAccession = goNameToAccessionMap.get(goName);
                Integer frequencyDataset = 0;
                Double percentDataset = 0.0;
                
                if (datasetGoTermUsage.get(goName) != null) {
                    frequencyDataset = datasetGoTermUsage.get(goName);
                    percentDataset = ((double) frequencyDataset / allProjectProteins.size()) * 100;
                }
                
                Double percentAll = ((double) frequencyAll / proteinToGoMappings.size()) * 100;
                Double pValue = new HypergeometricDistributionImpl(proteinToGoMappings.size(), frequencyAll, allProjectProteins.size()).probability(frequencyDataset);
                Double log2Diff = Math.log(percentAll / percentDataset) / Math.log(2);
                
                Double significanceLevel = 0.05;

                ((DefaultTableModel) proteinGoMappingsTable.getModel()).addRow(new Object[]{
                            proteinGoMappingsTable.getRowCount() + 1,
                            goAccession,
                            goName,
                            frequencyAll,
                            percentAll,
                            frequencyDataset,
                            percentDataset,
                            log2Diff,
                            pValue,
                            pValue < significanceLevel / totalGoTermUsage.keySet().size()
                        });
                
                frquencyPlotDataset.addValue(percentAll, "All", goName);
                frquencyPlotDataset.addValue(percentDataset, "Dataset", goName);
                
                if (!log2Diff.isInfinite()) {
                    significancePlotDataset.addValue(log2Diff, "Difference", goName);
                } else {
                    significancePlotDataset.addValue(0, "Difference", goName);
                }
                
                if (pValue < significanceLevel / totalGoTermUsage.keySet().size()) {
                    if (log2Diff > 0) {
                        significanceColors.add(peptideShakerGUI.getSparklineColor());
                    } else {
                        significanceColors.add(new Color(255, 51, 51));
                    }
                } else {
                    significanceColors.add(Color.lightGray);
                }
            }
            
            JFreeChart barChart = ChartFactory.createBarChart(null, "GO Terms", "Frequency (%)", frquencyPlotDataset, PlotOrientation.VERTICAL, false, true, true);
            ChartPanel chartPanel = new ChartPanel(barChart);

            ((CategoryPlot) chartPanel.getChart().getPlot()).getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

            BarRenderer3D renderer = new BarRenderer3D(0, 0);
            renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
            barChart.getCategoryPlot().setRenderer(renderer);


            // set background color
            barChart.getPlot().setBackgroundPaint(Color.WHITE);
            barChart.setBackgroundPaint(Color.WHITE);
            chartPanel.setBackground(Color.WHITE);

            // hide the outline
            barChart.getPlot().setOutlineVisible(false);

            goFrequencyPlotPanel.removeAll();
            goFrequencyPlotPanel.add(chartPanel);
            
            
            
            JFreeChart significanceChart = ChartFactory.createBarChart(null, "GO Terms", "Log2 Difference", significancePlotDataset, PlotOrientation.VERTICAL, false, true, true);
            ChartPanel signChartPanel = new ChartPanel(significanceChart);

            ((CategoryPlot) signChartPanel.getChart().getPlot()).getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

            BarChartColorRenderer signRenderer = new BarChartColorRenderer(significanceColors);
            signRenderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
            significanceChart.getCategoryPlot().setRenderer(signRenderer);


            // set background color
            significanceChart.getPlot().setBackgroundPaint(Color.WHITE);
            significanceChart.setBackgroundPaint(Color.WHITE);
            signChartPanel.setBackground(Color.WHITE);

            // hide the outline
            significanceChart.getPlot().setOutlineVisible(false);

            goSignificancePlotPanel.removeAll();
            goSignificancePlotPanel.add(signChartPanel);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        proteinGoMappingsPanel = new javax.swing.JPanel();
        proteinGoMappingsScrollPane = new javax.swing.JScrollPane();
        proteinGoMappingsTable = new javax.swing.JTable();
        plotPanel = new javax.swing.JPanel();
        goPlotsTabbedPane = new javax.swing.JTabbedPane();
        goFrequencyPlotPanel = new javax.swing.JPanel();
        goSignificancePlotPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));

        proteinGoMappingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Gene Ontology Mappings"));
        proteinGoMappingsPanel.setOpaque(false);

        proteinGoMappingsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", "GO Term", "GO Name", "Frequency All", "Percent All", "Frequency Dataset", "Percent Dataset", "Log2 Diff", "p-value", " "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        proteinGoMappingsScrollPane.setViewportView(proteinGoMappingsTable);

        javax.swing.GroupLayout proteinGoMappingsPanelLayout = new javax.swing.GroupLayout(proteinGoMappingsPanel);
        proteinGoMappingsPanel.setLayout(proteinGoMappingsPanelLayout);
        proteinGoMappingsPanelLayout.setHorizontalGroup(
            proteinGoMappingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinGoMappingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinGoMappingsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 987, Short.MAX_VALUE)
                .addContainerGap())
        );
        proteinGoMappingsPanelLayout.setVerticalGroup(
            proteinGoMappingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinGoMappingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinGoMappingsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
                .addContainerGap())
        );

        plotPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Gene Ontology - Enrichment Analysis"));
        plotPanel.setOpaque(false);

        goPlotsTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        goFrequencyPlotPanel.setOpaque(false);
        goFrequencyPlotPanel.setLayout(new javax.swing.BoxLayout(goFrequencyPlotPanel, javax.swing.BoxLayout.LINE_AXIS));
        goPlotsTabbedPane.addTab("Distribution", goFrequencyPlotPanel);

        goSignificancePlotPanel.setOpaque(false);
        goSignificancePlotPanel.setLayout(new javax.swing.BoxLayout(goSignificancePlotPanel, javax.swing.BoxLayout.LINE_AXIS));
        goPlotsTabbedPane.addTab("Significance", goSignificancePlotPanel);

        javax.swing.GroupLayout plotPanelLayout = new javax.swing.GroupLayout(plotPanel);
        plotPanel.setLayout(plotPanelLayout);
        plotPanelLayout.setHorizontalGroup(
            plotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(goPlotsTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 987, Short.MAX_VALUE)
                .addContainerGap())
        );
        plotPanelLayout.setVerticalGroup(
            plotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotPanelLayout.createSequentialGroup()
                .addComponent(goPlotsTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 337, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(proteinGoMappingsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(plotPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinGoMappingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(plotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel goFrequencyPlotPanel;
    private javax.swing.JTabbedPane goPlotsTabbedPane;
    private javax.swing.JPanel goSignificancePlotPanel;
    private javax.swing.JPanel plotPanel;
    private javax.swing.JPanel proteinGoMappingsPanel;
    private javax.swing.JScrollPane proteinGoMappingsScrollPane;
    private javax.swing.JTable proteinGoMappingsTable;
    // End of variables declaration//GEN-END:variables
}
