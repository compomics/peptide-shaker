package eu.isas.peptideshaker.gui.protein_sequence;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.JSparklinesDataset;
import org.jfree.chart.*;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.IntervalCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * This class can be used to create a protein sequence plot.
 *
 * @author Harald Barsnes
 */
public class ProteinSequencePanel {

    /**
     * The protein sequence panel parent.
     */
    private ProteinSequencePanelParent proteinSequencePanelParent;
    /**
     * The background color for the plot.
     */
    private Color backgroundColor;
    /**
     * The reference line width.
     */
    private double referenceLineWidth = 0.03;
    /**
     * The reference line color.
     */
    private Color referenceLineColor = Color.BLACK;

    /**
     * Created a new ProteinSequencePanel object. Use getSequencePlot() to get
     * the plot/chart.
     *
     * @param backgroundColor the plot/chart background color
     */
    public ProteinSequencePanel(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Set the reference line properties. Will not have any effect until
     * getSequencePlot(..) is called again with 'addReferenceLine' set to true.
     *
     * @param referenceLineWidth reference line width
     * @param referenceLineColor reference line color
     */
    public void setReferenceLine(double referenceLineWidth, Color referenceLineColor) {
        this.referenceLineWidth = referenceLineWidth;
        this.referenceLineColor = referenceLineColor;
    }

    /**
     * Returns a sequence plot as a ChartPanel.
     *
     * @param aProteinSequencePanelParent the protein sequence panel parent
     * @param sparklineDataset the dataset
     * @param proteinAnnotations the protein annotations
     * @param addReferenceLine if true, a reference line is added
     * @param allowZooming if true, the user can zoom in the created plot/chart
     * @return a sequence plot
     */
    public ChartPanel getSequencePlot(ProteinSequencePanelParent aProteinSequencePanelParent, JSparklinesDataset sparklineDataset,
            HashMap<Integer, ArrayList<ResidueAnnotation>> proteinAnnotations,
            boolean addReferenceLine, boolean allowZooming) {

        this.proteinSequencePanelParent = aProteinSequencePanelParent;
        DefaultCategoryDataset barChartDataset = new DefaultCategoryDataset();
        StackedBarRenderer renderer = new StackedBarRenderer();
        renderer.setShadowVisible(false);
        CategoryToolTipGenerator myTooltips = new ProteinAnnotations(proteinAnnotations);

        // add the data
        for (int i = 0; i < sparklineDataset.getData().size(); i++) {

            JSparklinesDataSeries sparklineDataSeries = sparklineDataset.getData().get(i);

            for (int j = 0; j < sparklineDataSeries.getData().size(); j++) {
                barChartDataset.addValue(sparklineDataSeries.getData().get(j), "" + i, "" + j);
                renderer.setSeriesPaint(i, sparklineDataSeries.getSeriesColor());
                renderer.setSeriesToolTipGenerator(i, myTooltips);
            }
        }

        // create the chart
        JFreeChart chart = ChartFactory.createStackedBarChart(null, null, null, barChartDataset, PlotOrientation.HORIZONTAL, false, false, false);

        // fine tune the chart properites
        CategoryPlot plot = chart.getCategoryPlot();

        // remove space before/after the domain axis
        plot.getDomainAxis().setUpperMargin(0);
        plot.getDomainAxis().setLowerMargin(0);

        // remove space before/after the range axis
        plot.getRangeAxis().setUpperMargin(0);
        plot.getRangeAxis().setLowerMargin(0);

        renderer.setRenderAsPercentages(true);
        renderer.setBaseToolTipGenerator(new IntervalCategoryToolTipGenerator());

        // add the dataset to the plot
        plot.setDataset(barChartDataset);

        // hide unwanted chart details
        plot.getRangeAxis().setVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);


        // add a reference line in the middle of the dataset
        if (addReferenceLine) {
            DefaultCategoryDataset referenceLineDataset = new DefaultCategoryDataset();
            referenceLineDataset.addValue(1.0, "A", "B");
            plot.setDataset(1, referenceLineDataset);
            LayeredBarRenderer referenceLineRenderer = new LayeredBarRenderer();
            referenceLineRenderer.setSeriesBarWidth(0, referenceLineWidth);
            referenceLineRenderer.setSeriesFillPaint(0, referenceLineColor);
            referenceLineRenderer.setSeriesPaint(0, referenceLineColor);
            plot.setRenderer(1, referenceLineRenderer);
        }

        // set up the chart renderer
        plot.setRenderer(0, renderer);

        // hide the outline
        chart.getPlot().setOutlineVisible(false);

        // make sure the background is the same as the panel
        chart.getPlot().setBackgroundPaint(backgroundColor);
        chart.setBackgroundPaint(backgroundColor);

        final HashMap<Integer, ArrayList<ResidueAnnotation>> blockTooltips = proteinAnnotations;

        // create the chart panel
        ChartPanel chartPanel = new ChartPanel(chart);

        chartPanel.addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseClicked(ChartMouseEvent cme) {
                if (cme.getEntity() != null && cme.getTrigger().getButton() == MouseEvent.BUTTON1) {

                    ((CategoryItemEntity) cme.getEntity()).getDataset();
                    Integer blockNumber = new Integer((String) ((CategoryItemEntity) cme.getEntity()).getRowKey());

                    if (blockTooltips.containsKey(blockNumber)) {
                        ArrayList<ResidueAnnotation> annotation = blockTooltips.get(blockNumber);
                        proteinSequencePanelParent.annotationClicked(annotation, cme);
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {

                cme.getTrigger().getComponent().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                if (cme.getEntity() != null && cme.getEntity() instanceof CategoryItemEntity) {
                    ((CategoryItemEntity) cme.getEntity()).getDataset();
                    Integer blockNumber = new Integer((String) ((CategoryItemEntity) cme.getEntity()).getRowKey());

                    if (blockTooltips.containsKey(blockNumber)) {
                        if (blockTooltips.get(blockNumber).get(0).isClickable()) {
                            cme.getTrigger().getComponent().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                        }
                    }
                }
            }
        });

        if (!allowZooming) {
            chartPanel.setPopupMenu(null);
            chartPanel.setRangeZoomable(false);
        }


        chartPanel.setBackground(Color.WHITE);

        return chartPanel;
    }
}
