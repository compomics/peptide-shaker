package eu.isas.peptideshaker.gui.tabpanels;

import com.compomics.util.Util;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.genes.GeneFactory;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.biology.genes.go.GoDomains;
import com.compomics.util.experiment.biology.genes.go.GoMapping;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.XYPlottingDialog;
import com.compomics.util.gui.XYPlottingDialog.PlottingDialogPlotType;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.export.graphics.ExportGraphicsDialog;
import com.compomics.util.gui.parameters.identification_parameters.GenePreferencesDialog;
import com.compomics.util.preferences.GenePreferences;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.peptideshaker.gui.PeptideShakerGUI;
import eu.isas.peptideshaker.gui.tablemodels.ProteinGoTableModel;
import eu.isas.peptideshaker.gui.tablemodels.ProteinTableModel;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.JSparklinesDataset;
import no.uib.jsparklines.data.ValueAndBooleanDataPoint;
import no.uib.jsparklines.data.XYDataPoint;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesTableCellRenderer;
import no.uib.jsparklines.renderers.util.BarChartColorRenderer;
import org.apache.commons.math.distribution.HypergeometricDistributionImpl;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.Layer;
import org.w3c.dom.Document;

/**
 * The PeptideShaker GO Enrichment Analysis tab.
 *
 * @author Harald Barsnes
 */
public class GOEAPanel extends javax.swing.JPanel {

    /**
     * The protein table column header tooltips.
     */
    private ArrayList<String> proteinTableToolTips;
    /**
     * The progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * PeptideShaker GUI parent.
     */
    private PeptideShakerGUI peptideShakerGUI;
    /**
     * The GO mappings table column header tooltips.
     */
    private ArrayList<String> mappingsTableToolTips;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The distribution chart panel.
     */
    private ChartPanel distributionChartPanel = null;
    /**
     * The significance chart panel.
     */
    private ChartPanel signChartPanel = null;
    /**
     * The currently selected column when opening the pop up menu for the GO
     * table.
     */
    private String currentGoMappingsColumn = null;

    /**
     * Creates a new GOEAPanel.
     *
     * @param peptideShakerGUI the PeptideShakerGUI parent
     */
    public GOEAPanel(PeptideShakerGUI peptideShakerGUI) {
        this.peptideShakerGUI = peptideShakerGUI;
        initComponents();
        setUpGUI();
    }

    /**
     * Set up the GUI details.
     */
    private void setUpGUI() {

        // correct the color for the upper right corner
        JPanel proteinCorner = new JPanel();
        proteinCorner.setBackground(goMappingsTable.getTableHeader().getBackground());
        proteinsScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, proteinCorner);
        JPanel goMappingsCorner = new JPanel();
        goMappingsCorner.setBackground(goMappingsTable.getTableHeader().getBackground());
        proteinGoMappingsScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, goMappingsCorner);

        JTableHeader header = goMappingsTable.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if (peptideShakerGUI.getIdentification() != null) {
                    updateGoPlots();
                }
            }
        });

        goMappingsTable.getTableHeader().setReorderingAllowed(false);
        proteinTable.getTableHeader().setReorderingAllowed(false);
        goMappingsTable.setAutoCreateRowSorter(true);
        proteinTable.setAutoCreateRowSorter(true);

        // make sure that the scroll panes are see-through
        proteinGoMappingsScrollPane.getViewport().setOpaque(false);
        proteinsScrollPane.getViewport().setOpaque(false);

        // the index column
        goMappingsTable.getColumn("").setMaxWidth(60);
        goMappingsTable.getColumn("").setMinWidth(60);
        goMappingsTable.getColumn("  ").setMaxWidth(30);
        goMappingsTable.getColumn("  ").setMinWidth(30);

        double significanceLevel = 0.05;

        if (onePercentRadioButton.isSelected()) {
            significanceLevel = 0.01;
        }

        // cell renderers
        goMappingsTable.getColumn("GO Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        goMappingsTable.getColumn("Frequency All (%)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, Color.RED));
        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("Frequency All (%)").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        goMappingsTable.getColumn("Frequency Dataset (%)").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("Frequency Dataset (%)").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        goMappingsTable.getColumn("p-value").setCellRenderer(
                new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 1.0, peptideShakerGUI.getSparklineColor(), Color.lightGray, significanceLevel));
        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("p-value").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        goMappingsTable.getColumn("Log2 Diff").setCellRenderer(new JSparklinesBarChartTableCellRenderer(
                PlotOrientation.HORIZONTAL, -10.0, 10.0, Color.RED, peptideShakerGUI.getSparklineColor(), Color.lightGray, 0));
        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("Log2 Diff").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());
        goMappingsTable.getColumn("Frequency (%)").setCellRenderer(new JSparklinesTableCellRenderer(
                JSparklinesTableCellRenderer.PlotType.barChart,
                PlotOrientation.HORIZONTAL, 0.0, 100.0));
        goMappingsTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
                null,
                "Selected", null));

        // make the tabs in the tabbed pane go from right to left
        goPlotsTabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // set up the table header tooltips
        mappingsTableToolTips = new ArrayList<String>();
        mappingsTableToolTips.add(null);
        mappingsTableToolTips.add("Gene Ontology Accession");
        mappingsTableToolTips.add("Gene Ontology Term");
        mappingsTableToolTips.add("Gene Ontology Domain");
        mappingsTableToolTips.add("Frequency All (%)");
        mappingsTableToolTips.add("Frequency Dataset (%)");
        mappingsTableToolTips.add("Frequency (%) (All & Dataset))");
        mappingsTableToolTips.add("Log2 Difference (Dataset / All)");
        mappingsTableToolTips.add("<html>Hypergeometic Test<br>FDR-Corrected</html>");
        mappingsTableToolTips.add("Selected for Plots");

        proteinTableToolTips = new ArrayList<String>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Protein Sequence Coverage (%) (Confident / Doubtful / Not Validated / Possible)");
        proteinTableToolTips.add("Number of Peptides (Validated / Doubtful / Not Validated)");
        proteinTableToolTips.add("Number of Spectra (Validated / Doubtful / Not Validated)");
        proteinTableToolTips.add("MS2 Quantification");
        proteinTableToolTips.add("Protein Confidence");
        proteinTableToolTips.add("Validated");
    }

    /**
     * Set the properties of the GO protein table.
     */
    private void setProteinGoTableProperties() {
        proteinTable.getColumn(" ").setMaxWidth(60);
        proteinTable.getColumn(" ").setMinWidth(60);
        proteinTable.getColumn("  ").setMaxWidth(30);
        proteinTable.getColumn("  ").setMinWidth(30);

        try {
            proteinTable.getColumn("Confidence").setMaxWidth(90);
            proteinTable.getColumn("Confidence").setMinWidth(90);
        } catch (IllegalArgumentException w) {
            proteinTable.getColumn("Score").setMaxWidth(90);
            proteinTable.getColumn("Score").setMinWidth(90);
        }

        // set the preferred size of the accession column
        Integer width = ProteinTableModel.getPreferredAccessionColumnWidth(proteinTable, proteinTable.getColumn("Accession").getModelIndex(), 6, peptideShakerGUI.getMetrics().getMaxProteinKeyLength());
        if (width != null) {
            proteinTable.getColumn("Accession").setMinWidth(width);
            proteinTable.getColumn("Accession").setMaxWidth(width);
        } else {
            proteinTable.getColumn("Accession").setMinWidth(15);
            proteinTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
        }

        proteinTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));

        // use a gray color for no decoy searches
        Color nonValidatedColor = peptideShakerGUI.getSparklineColorNonValidated();
        if (!sequenceFactory.concatenatedTargetDecoy()) {
            nonValidatedColor = peptideShakerGUI.getUtilitiesUserPreferences().getSparklineColorNotFound();
        }
        ArrayList<Color> sparklineColors = new ArrayList<Color>();
        sparklineColors.add(peptideShakerGUI.getSparklineColor());
        sparklineColors.add(peptideShakerGUI.getUtilitiesUserPreferences().getSparklineColorDoubtful());
        sparklineColors.add(nonValidatedColor);

        proteinTable.getColumn("#Peptides").setCellRenderer(new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColors, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers));
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0"));
        proteinTable.getColumn("#Spectra").setCellRenderer(new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColors, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers));
        ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0"));
        proteinTable.getColumn("MS2 Quant.").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, peptideShakerGUI.getSparklineColor()));
        ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0.00E00"));

        try {
            proteinTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        } catch (IllegalArgumentException e) {
            proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, peptideShakerGUI.getSparklineColor()));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, peptideShakerGUI.getScoreAndConfidenceDecimalFormat());
        }

        proteinTable.getColumn("  ").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(this.getClass()), MatchValidationLevel.getTooltipMap()));

        sparklineColors = new ArrayList<Color>();
        sparklineColors.add(peptideShakerGUI.getSparklineColor());
        sparklineColors.add(peptideShakerGUI.getUtilitiesUserPreferences().getSparklineColorDoubtful());
        sparklineColors.add(nonValidatedColor);
        sparklineColors.add(peptideShakerGUI.getUtilitiesUserPreferences().getSparklineColorNotFound());

        JSparklinesArrayListBarChartTableCellRenderer coverageCellRendered = new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColors, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumExceptLastNumber);
        coverageCellRendered.showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0.00"));
        proteinTable.getColumn("Coverage").setCellRenderer(coverageCellRendered);

        // make sure that the user is made aware that the tool is doing something during sorting of the protein table
        proteinTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            @Override
            public void sorterChanged(RowSorterEvent e) {

                if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    proteinTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                    // change the peptide shaker icon to a "waiting version"
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")));
                } else if (e.getType() == RowSorterEvent.Type.SORTED) {
                    peptideShakerGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    proteinTable.getTableHeader().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                    // change the peptide shaker icon back to the normal version
                    peptideShakerGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")));
                }
            }
        });
    }

    /**
     * Update the GO mappings.
     */
    public void displayResults() {

        if (peptideShakerGUI.getIdentification() != null) {

            GeneMaps geneMaps = peptideShakerGUI.getGeneMaps();

            if (geneMaps.hasGoMappings()) {

                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
                progressDialog.setTitle("Getting GO Mappings. Please Wait...");

                progressDialog.setPrimaryProgressCounterIndeterminate(true);

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            progressDialog.setVisible(true);
                        } catch (IndexOutOfBoundsException e) {
                            // ignore
                        }
                    }
                }, "ProgressDialog").start();

                new Thread("GoThread") {
                    @Override
                    public void run() {

                        // clear old table
                        DefaultTableModel dm = (DefaultTableModel) goMappingsTable.getModel();
                        dm.getDataVector().removeAllElements();
                        dm.fireTableDataChanged();

                        TreeMap<String, Integer> datasetGoTermUsage = new TreeMap<String, Integer>();

                        try {
                            progressDialog.setTitle("Importing GO (1/3). Please Wait...");
                            GoMapping backgroundGoMapping = new GoMapping();
                            Integer taxon = null;
                            IdentificationParameters identificationParameters = peptideShakerGUI.getIdentificationParameters();
                            GenePreferences genePreferences = identificationParameters.getGenePreferences();
                            if (genePreferences != null) {
                                taxon = genePreferences.getSelectedBackgroundSpecies();
                            }
                            if (taxon == null) {
                                GenePreferencesDialog genePreferencesDialog = new GenePreferencesDialog(peptideShakerGUI, genePreferences, identificationParameters.getSearchParameters(), false);
                                if (!genePreferencesDialog.isCanceled()) {
                                    genePreferences = genePreferencesDialog.getGenePreferences();
                                    identificationParameters.setGenePreferences(genePreferences);
                                    taxon = genePreferences.getSelectedBackgroundSpecies();
                                }
                            }
                            if (taxon != null) {
                                SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
                                String ensemblDatasetName = speciesFactory.getEnsemblDataset(taxon);
                                File goMappingFile = GeneFactory.getGoMappingFile(ensemblDatasetName);
                                backgroundGoMapping.loadMappingsFromFile(goMappingFile, progressDialog);

                                GoDomains goDomains = new GoDomains();
                                File goDomainsFile = GeneFactory.getGoDomainsFile();
                                goDomains.laodMappingFromFile(goDomainsFile, progressDialog);

                                Identification identification = peptideShakerGUI.getIdentification();

                                progressDialog.setTitle("Getting GO Mappings (2/3). Please Wait...");
                                progressDialog.setPrimaryProgressCounterIndeterminate(false);
                                progressDialog.setMaxPrimaryProgressCounter(identification.getProteinIdentification().size());
                                progressDialog.setValue(0);

                                int totalNumberOfGoMappedProteinsInProject = 0;
                                PSParameter psParameter = new PSParameter();
                                ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
                                parameters.add(psParameter);
                                ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, false, null, false, null, progressDialog);

                                ProteinMatch proteinMatch;
                                while ((proteinMatch = proteinMatchesIterator.next()) != null) {

                                    String proteinKey = proteinMatch.getKey();
                                    psParameter = (PSParameter) peptideShakerGUI.getIdentification().getProteinMatchParameter(proteinKey, psParameter);

                                    if (psParameter.getMatchValidationLevel().isValidated() && !ProteinMatch.isDecoy(proteinKey) && !psParameter.isHidden()) {

                                        String mainMatch = proteinMatch.getMainMatch();
                                        HashSet<String> goTerms = backgroundGoMapping.getGoAccessions(mainMatch);
                                        if (goTerms != null && !goTerms.isEmpty()) {
                                            totalNumberOfGoMappedProteinsInProject++;
                                            for (String goTerm : goTerms) {
                                                Integer usage = datasetGoTermUsage.get(goTerm);
                                                if (usage == null) {
                                                    usage = 0;
                                                }
                                                datasetGoTermUsage.put(goTerm, usage + 1);
                                            }
                                        }
                                    }
                                    if (progressDialog.isRunCanceled()) {
                                        return;
                                    }
                                    progressDialog.increasePrimaryProgressCounter();
                                }

                                ArrayList<String> termNamesMapped = backgroundGoMapping.getSortedTermNames();
                                int nBackgroundProteins = backgroundGoMapping.getProteinToGoMap().size();
                                progressDialog.setTitle("Creating GO Plots (3/3). Please Wait...");
                                progressDialog.setValue(0);
                                progressDialog.setMaxPrimaryProgressCounter(termNamesMapped.size());

                                // update the table
                                Double maxLog2Diff = 0.0;
                                ArrayList<Integer> indexes = new ArrayList<Integer>();
                                ArrayList<Double> pValues = new ArrayList<Double>();

                                // display the number of go mapped proteins
                                goProteinCountLabel.setText("[GO Proteins: Ensembl: " + nBackgroundProteins
                                        + ", Project: " + totalNumberOfGoMappedProteinsInProject + "]");

                                boolean goDomainChanged = false;

                                for (String goTermName : termNamesMapped) {

                                    if (progressDialog.isRunCanceled()) {
                                        break;
                                    }

                                    String goAccession = backgroundGoMapping.getTermAccession(goTermName);

                                    Integer frequencyBackground = backgroundGoMapping.getProteinAccessions(goAccession).size();

                                    Integer frequencyDataset = 0;
                                    Double percentDataset = 0.0;

                                    if (datasetGoTermUsage.get(goAccession) != null) {
                                        frequencyDataset = datasetGoTermUsage.get(goAccession);
                                        percentDataset = ((double) frequencyDataset) * 100 / totalNumberOfGoMappedProteinsInProject;
                                    }

                                    Double percentAll = ((double) frequencyBackground) * 100 / nBackgroundProteins;
                                    Double pValue = new HypergeometricDistributionImpl(
                                            nBackgroundProteins, // population size
                                            frequencyBackground, // number of successes
                                            totalNumberOfGoMappedProteinsInProject // sample size
                                    ).probability(frequencyDataset);
                                    Double log2Diff = Math.log(percentDataset / percentAll) / Math.log(2);

                                    if (!log2Diff.isInfinite() && Math.abs(log2Diff) > maxLog2Diff) {
                                        maxLog2Diff = Math.abs(log2Diff);
                                    }

                                    String goDomain = goDomains.getTermDomain(goAccession);

                                    if (goDomain == null) {

                                        // URL a GO Term in OBO xml format
                                        URL u = new URL("http://www.ebi.ac.uk/QuickGO/GTerm?id=" + goAccession + "&format=oboxml");

                                        // connect
                                        HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();

                                        // parse an XML document from the connection
                                        InputStream inputStream = urlConnection.getInputStream();
                                        Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
                                        inputStream.close();

                                        // XPath is here used to locate parts of an XML document
                                        XPath xpath = XPathFactory.newInstance().newXPath();

                                        // locate the domain
                                        goDomain = xpath.compile("/obo/term/namespace").evaluate(xml);

                                        goDomains.addDomain(goAccession, goDomain);
                                        goDomainChanged = true;
                                    }

                                    // add the data points for the first data series 
                                    ArrayList<Double> dataAll = new ArrayList<Double>();
                                    dataAll.add(percentAll);
                                    ArrayList<Double> dataDataset = new ArrayList<Double>();
                                    dataDataset.add(percentDataset);

                                    // create a JSparklineDataSeries  
                                    JSparklinesDataSeries sparklineDataseriesAll = new JSparklinesDataSeries(dataAll, Color.RED, "All");
                                    JSparklinesDataSeries sparklineDataseriesDataset = new JSparklinesDataSeries(dataDataset, peptideShakerGUI.getSparklineColor(), "Dataset");

                                    // add the data series to JSparklineDataset 
                                    ArrayList<JSparklinesDataSeries> sparkLineDataSeries = new ArrayList<JSparklinesDataSeries>();
                                    sparkLineDataSeries.add(sparklineDataseriesAll);
                                    sparkLineDataSeries.add(sparklineDataseriesDataset);

                                    JSparklinesDataset dataset = new JSparklinesDataset(sparkLineDataSeries);

                                    pValues.add(pValue);
                                    indexes.add(goMappingsTable.getRowCount());

                                    ((DefaultTableModel) goMappingsTable.getModel()).addRow(new Object[]{
                                        goMappingsTable.getRowCount() + 1,
                                        peptideShakerGUI.getDisplayFeaturesGenerator().addGoLink(goAccession),
                                        goTermName,
                                        goDomain,
                                        percentAll,
                                        percentDataset,
                                        dataset,
                                        new ValueAndBooleanDataPoint(log2Diff, false),
                                        pValue,
                                        true
                                    });

                                    progressDialog.increasePrimaryProgressCounter();
                                }

                                if (indexes.isEmpty()) {
                                    progressDialog.setRunCanceled();
                                }

                                int significantCounter = 0;
                                double significanceLevel = 0.05;

                                if (onePercentRadioButton.isSelected()) {
                                    significanceLevel = 0.01;
                                }

                                if (!progressDialog.isRunCanceled()) {

                                    ((DefaultTableModel) goMappingsTable.getModel()).fireTableDataChanged();

                                    // correct the p-values for multiple testing using benjamini-hochberg
                                    sortPValues(pValues, indexes);

                                    ((ValueAndBooleanDataPoint) ((DefaultTableModel) goMappingsTable.getModel()).getValueAt(
                                            indexes.get(0), goMappingsTable.getColumn("Log2 Diff").getModelIndex())).setSignificant(
                                            pValues.get(0) < significanceLevel);
                                    ((DefaultTableModel) goMappingsTable.getModel()).setValueAt(new XYDataPoint(pValues.get(0), pValues.get(0)), indexes.get(0),
                                            goMappingsTable.getColumn("p-value").getModelIndex());

                                    if (pValues.get(0) < significanceLevel) {
                                        significantCounter++;
                                    }

                                    for (int i = 1; i < pValues.size(); i++) {

                                        if (progressDialog.isRunCanceled()) {
                                            break;
                                        }

                                        double tempPvalue = pValues.get(i) * pValues.size() / (pValues.size() - i);

                                        // have to check if the correction results in a p-value bigger than 1
                                        if (tempPvalue > 1) {
                                            tempPvalue = 1;
                                        }

                                        ((ValueAndBooleanDataPoint) ((DefaultTableModel) goMappingsTable.getModel()).getValueAt(
                                                indexes.get(i), goMappingsTable.getColumn("Log2 Diff").getModelIndex())).setSignificant(tempPvalue < significanceLevel);
                                        ((DefaultTableModel) goMappingsTable.getModel()).setValueAt(new XYDataPoint(tempPvalue, tempPvalue), indexes.get(i),
                                                goMappingsTable.getColumn("p-value").getModelIndex());

                                        if (tempPvalue < significanceLevel) {
                                            significantCounter++;
                                        }
                                    }
                                }

                                if (!progressDialog.isRunCanceled()) {

                                    ((TitledBorder) mappingsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING
                                            + "Gene Ontology Mappings (" + significantCounter + "/" + goMappingsTable.getRowCount() + ")"
                                            + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
                                    mappingsPanel.repaint();

                                    progressDialog.setPrimaryProgressCounterIndeterminate(true);

                                    // set the preferred size of the accession column
                                    Integer width = ProteinTableModel.getPreferredAccessionColumnWidth(goMappingsTable, goMappingsTable.getColumn("GO Accession").getModelIndex(), 6, peptideShakerGUI.getMetrics().getMaxProteinKeyLength());
                                    if (width != null) {
                                        goMappingsTable.getColumn("GO Accession").setMinWidth(width);
                                        goMappingsTable.getColumn("GO Accession").setMaxWidth(width);
                                    } else {
                                        goMappingsTable.getColumn("GO Accession").setMinWidth(15);
                                        goMappingsTable.getColumn("GO Accession").setMaxWidth(Integer.MAX_VALUE);
                                    }

                                    maxLog2Diff = Math.ceil(maxLog2Diff);

                                    goMappingsTable.getColumn("Log2 Diff").setCellRenderer(new JSparklinesBarChartTableCellRenderer(
                                            PlotOrientation.HORIZONTAL, -maxLog2Diff, maxLog2Diff, Color.RED, peptideShakerGUI.getSparklineColor(), Color.lightGray, 0));
                                    ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("Log2 Diff").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());

                                    // update the plots
                                    updateGoPlots();

                                    // enable the contextual export options
                                    exportMappingsJButton.setEnabled(true);
                                    exportPlotsJButton.setEnabled(true);

                                    peptideShakerGUI.setUpdated(PeptideShakerGUI.GO_ANALYSIS_TAB_INDEX, true);
                                }

                                if (goDomainChanged && goDomainsFile.exists()) {
                                    goDomains.saveMapping(goDomainsFile);
                                }

                                progressDialog.setRunFinished();
                            }

                        } catch (Exception e) {
                            progressDialog.setRunFinished();
                            peptideShakerGUI.catchException(e);
                        }
                    }
                }.start();
            }
        }
    }

    /**
     * Update the GO plots.
     */
    private void updateGoPlots() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        DefaultCategoryDataset frquencyPlotDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset significancePlotDataset = new DefaultCategoryDataset();
        ArrayList<Color> significanceColors = new ArrayList<Color>();
        Double maxLog2Diff = 0.0;

        for (int i = 0; i < goMappingsTable.getRowCount(); i++) {

            boolean selected = (Boolean) goMappingsTable.getValueAt(i, goMappingsTable.getColumn("  ").getModelIndex());
            boolean significant = ((ValueAndBooleanDataPoint) goMappingsTable.getValueAt(i, goMappingsTable.getColumn("Log2 Diff").getModelIndex())).isSignificant();

            if (selected) {

                String goTerm = (String) goMappingsTable.getValueAt(i, goMappingsTable.getColumn("GO Term").getModelIndex());
                Double percentAll = (Double) goMappingsTable.getValueAt(i, goMappingsTable.getColumn("Frequency All (%)").getModelIndex());
                Double percentDataset = (Double) goMappingsTable.getValueAt(i, goMappingsTable.getColumn("Frequency Dataset (%)").getModelIndex());
                Double log2Diff = ((ValueAndBooleanDataPoint) goMappingsTable.getValueAt(i, goMappingsTable.getColumn("Log2 Diff").getModelIndex())).getValue();

                frquencyPlotDataset.addValue(percentAll, "All", goTerm);
                frquencyPlotDataset.addValue(percentDataset, "Dataset", goTerm);

                if (!log2Diff.isInfinite()) {
                    significancePlotDataset.addValue(log2Diff, "Difference", goTerm);
                } else {
                    significancePlotDataset.addValue(0, "Difference", goTerm);
                }

                if (significant) {
                    if (log2Diff > 0) {
                        significanceColors.add(peptideShakerGUI.getSparklineColor());
                    } else {
                        significanceColors.add(new Color(255, 51, 51));
                    }
                } else {
                    significanceColors.add(Color.lightGray);
                }

                if (!log2Diff.isInfinite() && Math.abs(log2Diff) > maxLog2Diff) {
                    maxLog2Diff = Math.abs(log2Diff);
                }
            }
        }

        maxLog2Diff = Math.ceil(maxLog2Diff);

        JFreeChart distributionChart = ChartFactory.createBarChart(null, "GO Terms", "Frequency (%)", frquencyPlotDataset, PlotOrientation.VERTICAL, false, true, true);
        distributionChartPanel = new ChartPanel(distributionChart);

        ((CategoryPlot) distributionChartPanel.getChart().getPlot()).getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

        BarRenderer3D renderer = new BarRenderer3D(0, 0);
        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesPaint(1, peptideShakerGUI.getSparklineColor());
        distributionChart.getCategoryPlot().setRenderer(renderer);

        // add mouse listener
        distributionChartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent cme) {

                if (cme.getEntity() instanceof CategoryItemEntity) {
                    CategoryItemEntity categoryItem = (CategoryItemEntity) cme.getEntity();
                    String columnKey = (String) categoryItem.getColumnKey();

                    // select and highlight category
                    boolean categoryFound = false;

                    for (int i = 0; i < goMappingsTable.getRowCount() && !categoryFound; i++) {
                        if (((String) goMappingsTable.getValueAt(
                                i, goMappingsTable.getColumn("GO Term").getModelIndex())).equalsIgnoreCase(columnKey)) {
                            goMappingsTable.setRowSelectionInterval(i, i);
                            goMappingsTable.scrollRectToVisible(goMappingsTable.getCellRect(i, 0, false));
                            goMappingsTableMouseReleased(null);
                        }
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {
                // do nothing
            }
        });

        // set background color
        distributionChart.getPlot().setBackgroundPaint(Color.WHITE);
        distributionChart.setBackgroundPaint(Color.WHITE);
        distributionChartPanel.setBackground(Color.WHITE);

        // hide the outline
        distributionChart.getPlot().setOutlineVisible(false);

        goFrequencyPlotPanel.removeAll();
        goFrequencyPlotPanel.add(distributionChartPanel);
        goFrequencyPlotPanel.revalidate();
        goFrequencyPlotPanel.repaint();

        JFreeChart significanceChart = ChartFactory.createBarChart(null, "GO Terms", "Log2 Difference", significancePlotDataset, PlotOrientation.VERTICAL, false, true, true);
        signChartPanel = new ChartPanel(significanceChart);

        ((CategoryPlot) signChartPanel.getChart().getPlot()).getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

        ((CategoryPlot) signChartPanel.getChart().getPlot()).getRangeAxis().setUpperBound(maxLog2Diff);
        ((CategoryPlot) signChartPanel.getChart().getPlot()).getRangeAxis().setLowerBound(-maxLog2Diff);

        BarChartColorRenderer signRenderer = new BarChartColorRenderer(significanceColors);
        signRenderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        significanceChart.getCategoryPlot().setRenderer(signRenderer);

        // add mouse listener
        signChartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent cme) {

                if (cme.getEntity() instanceof CategoryItemEntity) {
                    CategoryItemEntity categoryItem = (CategoryItemEntity) cme.getEntity();
                    String columnKey = (String) categoryItem.getColumnKey();

                    // select and highlight category
                    boolean categoryFound = false;

                    for (int i = 0; i < goMappingsTable.getRowCount() && !categoryFound; i++) {
                        if (((String) goMappingsTable.getValueAt(
                                i, goMappingsTable.getColumn("GO Term").getModelIndex())).equalsIgnoreCase(columnKey)) {
                            goMappingsTable.setRowSelectionInterval(i, i);
                            goMappingsTable.scrollRectToVisible(goMappingsTable.getCellRect(i, 0, false));
                            goMappingsTableMouseReleased(null);
                        }
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {
                // do nothing
            }
        });

        // set background color
        significanceChart.getPlot().setBackgroundPaint(Color.WHITE);
        significanceChart.setBackgroundPaint(Color.WHITE);
        signChartPanel.setBackground(Color.WHITE);

        // hide the outline
        significanceChart.getPlot().setOutlineVisible(false);

        goSignificancePlotPanel.removeAll();
        goSignificancePlotPanel.add(signChartPanel);
        goSignificancePlotPanel.revalidate();
        goSignificancePlotPanel.repaint();

        updatePlotMarkers();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectTermsJPopupMenu = new javax.swing.JPopupMenu();
        selectAllMenuItem = new javax.swing.JMenuItem();
        deselectAllMenuItem = new javax.swing.JMenuItem();
        selectSignificantMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        statisticsMenuItem = new javax.swing.JMenuItem();
        significanceLevelButtonGroup = new javax.swing.ButtonGroup();
        mappingsTableLayeredPane = new javax.swing.JLayeredPane();
        mappingsPanel = new javax.swing.JPanel();
        proteinGoMappingsScrollPane = new javax.swing.JScrollPane();
        goMappingsTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) mappingsTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        significanceJLabel = new javax.swing.JLabel();
        fivePercentRadioButton = new javax.swing.JRadioButton();
        onePercentRadioButton = new javax.swing.JRadioButton();
        goProteinCountLabel = new javax.swing.JLabel();
        mappingsHelpJButton = new javax.swing.JButton();
        exportMappingsJButton = new javax.swing.JButton();
        contextMenuMappingsBackgroundPanel = new javax.swing.JPanel();
        plotLayeredPane = new javax.swing.JLayeredPane();
        plotPanel = new javax.swing.JPanel();
        goPlotsTabbedPane = new javax.swing.JTabbedPane();
        proteinsPanel = new javax.swing.JPanel();
        proteinsScrollPane = new javax.swing.JScrollPane();
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
        goFrequencyPlotPanel = new javax.swing.JPanel();
        goSignificancePlotPanel = new javax.swing.JPanel();
        plotHelpJButton = new javax.swing.JButton();
        exportPlotsJButton = new javax.swing.JButton();
        contextMenuPlotsBackgroundPanel = new javax.swing.JPanel();

        selectAllMenuItem.setText("Select All");
        selectAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllMenuItemActionPerformed(evt);
            }
        });
        selectTermsJPopupMenu.add(selectAllMenuItem);

        deselectAllMenuItem.setText("Deselect All");
        deselectAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deselectAllMenuItemActionPerformed(evt);
            }
        });
        selectTermsJPopupMenu.add(deselectAllMenuItem);

        selectSignificantMenuItem.setText("Select Significant");
        selectSignificantMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectSignificantMenuItemActionPerformed(evt);
            }
        });
        selectTermsJPopupMenu.add(selectSignificantMenuItem);
        selectTermsJPopupMenu.add(jSeparator1);

        statisticsMenuItem.setText("Statistics (beta)");
        statisticsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statisticsMenuItemActionPerformed(evt);
            }
        });
        selectTermsJPopupMenu.add(statisticsMenuItem);

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        mappingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Gene Ontology Mappings"));
        mappingsPanel.setOpaque(false);

        proteinGoMappingsScrollPane.setOpaque(false);

        goMappingsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", "GO Accession", "GO Term", "GO Domain", "Frequency All (%)", "Frequency Dataset (%)", "Frequency (%)", "Log2 Diff", "p-value", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, ValueAndBooleanDataPoint.class, java.lang.Object.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean[]{
                false, false, false, false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        goMappingsTable.setOpaque(false);
        goMappingsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        goMappingsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                goMappingsTableMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                goMappingsTableMouseExited(evt);
            }
        });
        goMappingsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                goMappingsTableMouseMoved(evt);
            }
        });
        goMappingsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                goMappingsTableKeyReleased(evt);
            }
        });
        proteinGoMappingsScrollPane.setViewportView(goMappingsTable);

        significanceJLabel.setText("Significance Level:");

        significanceLevelButtonGroup.add(fivePercentRadioButton);
        fivePercentRadioButton.setSelected(true);
        fivePercentRadioButton.setText("0.05");
        fivePercentRadioButton.setOpaque(false);
        fivePercentRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fivePercentRadioButtonActionPerformed(evt);
            }
        });

        significanceLevelButtonGroup.add(onePercentRadioButton);
        onePercentRadioButton.setText("0.01");
        onePercentRadioButton.setOpaque(false);
        onePercentRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onePercentRadioButtonActionPerformed(evt);
            }
        });

        goProteinCountLabel.setText("[GO Proteins: Ensembl: -, Project: -]");
        goProteinCountLabel.setToolTipText("Number of GO mapped proteins");

        javax.swing.GroupLayout mappingsPanelLayout = new javax.swing.GroupLayout(mappingsPanel);
        mappingsPanel.setLayout(mappingsPanelLayout);
        mappingsPanelLayout.setHorizontalGroup(
            mappingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mappingsPanelLayout.createSequentialGroup()
                .addGroup(mappingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mappingsPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(proteinGoMappingsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 988, Short.MAX_VALUE))
                    .addGroup(mappingsPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(goProteinCountLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(significanceJLabel)
                        .addGap(18, 18, 18)
                        .addComponent(onePercentRadioButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fivePercentRadioButton)
                        .addGap(13, 13, 13)))
                .addContainerGap())
        );
        mappingsPanelLayout.setVerticalGroup(
            mappingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mappingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinGoMappingsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addGroup(mappingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(significanceJLabel)
                    .addComponent(onePercentRadioButton)
                    .addComponent(fivePercentRadioButton)
                    .addComponent(goProteinCountLabel))
                .addContainerGap())
        );

        mappingsTableLayeredPane.add(mappingsPanel);
        mappingsPanel.setBounds(0, 0, 1020, 364);

        mappingsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        mappingsHelpJButton.setToolTipText("Help");
        mappingsHelpJButton.setBorder(null);
        mappingsHelpJButton.setBorderPainted(false);
        mappingsHelpJButton.setContentAreaFilled(false);
        mappingsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        mappingsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                mappingsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                mappingsHelpJButtonMouseExited(evt);
            }
        });
        mappingsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mappingsHelpJButtonActionPerformed(evt);
            }
        });
        mappingsTableLayeredPane.add(mappingsHelpJButton);
        mappingsHelpJButton.setBounds(990, 0, 10, 19);
        mappingsTableLayeredPane.setLayer(mappingsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportMappingsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportMappingsJButton.setToolTipText("Copy to Clipboard");
        exportMappingsJButton.setBorder(null);
        exportMappingsJButton.setBorderPainted(false);
        exportMappingsJButton.setContentAreaFilled(false);
        exportMappingsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportMappingsJButton.setEnabled(false);
        exportMappingsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportMappingsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportMappingsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportMappingsJButtonMouseExited(evt);
            }
        });
        exportMappingsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMappingsJButtonActionPerformed(evt);
            }
        });
        mappingsTableLayeredPane.add(exportMappingsJButton);
        exportMappingsJButton.setBounds(980, 0, 10, 19);
        mappingsTableLayeredPane.setLayer(exportMappingsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuMappingsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuMappingsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuMappingsBackgroundPanel);
        contextMenuMappingsBackgroundPanel.setLayout(contextMenuMappingsBackgroundPanelLayout);
        contextMenuMappingsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuMappingsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuMappingsBackgroundPanelLayout.setVerticalGroup(
            contextMenuMappingsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        mappingsTableLayeredPane.add(contextMenuMappingsBackgroundPanel);
        contextMenuMappingsBackgroundPanel.setBounds(980, 0, 30, 19);
        mappingsTableLayeredPane.setLayer(contextMenuMappingsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        plotPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Gene Ontology Enrichment Analysis"));
        plotPanel.setOpaque(false);

        goPlotsTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
        goPlotsTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                goPlotsTabbedPaneStateChanged(evt);
            }
        });

        proteinsPanel.setOpaque(false);

        proteinsScrollPane.setOpaque(false);

        proteinTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                " ", "Accession", "Description", "Coverage", "#Peptides", "#Spectra", "MS2 Quant.", "Confidence", "  "
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Boolean.class
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
        proteinTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinTableMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                proteinTableMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinTableMouseExited(evt);
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
        proteinsScrollPane.setViewportView(proteinTable);

        javax.swing.GroupLayout proteinsPanelLayout = new javax.swing.GroupLayout(proteinsPanel);
        proteinsPanel.setLayout(proteinsPanelLayout);
        proteinsPanelLayout.setHorizontalGroup(
            proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 982, Short.MAX_VALUE)
        );
        proteinsPanelLayout.setVerticalGroup(
            proteinsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proteinsPanelLayout.createSequentialGroup()
                .addComponent(proteinsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
                .addContainerGap())
        );

        goPlotsTabbedPane.addTab("Proteins", proteinsPanel);

        goFrequencyPlotPanel.setBackground(new java.awt.Color(255, 255, 255));
        goFrequencyPlotPanel.setLayout(new javax.swing.BoxLayout(goFrequencyPlotPanel, javax.swing.BoxLayout.LINE_AXIS));
        goPlotsTabbedPane.addTab("Frequency", goFrequencyPlotPanel);

        goSignificancePlotPanel.setBackground(new java.awt.Color(255, 255, 255));
        goSignificancePlotPanel.setLayout(new javax.swing.BoxLayout(goSignificancePlotPanel, javax.swing.BoxLayout.LINE_AXIS));
        goPlotsTabbedPane.addTab("Significance", goSignificancePlotPanel);

        goPlotsTabbedPane.setSelectedIndex(2);

        javax.swing.GroupLayout plotPanelLayout = new javax.swing.GroupLayout(plotPanel);
        plotPanel.setLayout(plotPanelLayout);
        plotPanelLayout.setHorizontalGroup(
            plotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plotPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(goPlotsTabbedPane)
                .addContainerGap())
        );
        plotPanelLayout.setVerticalGroup(
            plotPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, plotPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(goPlotsTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                .addContainerGap())
        );

        plotLayeredPane.add(plotPanel);
        plotPanel.setBounds(0, 0, 1019, 370);

        plotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        plotHelpJButton.setToolTipText("Help");
        plotHelpJButton.setBorder(null);
        plotHelpJButton.setBorderPainted(false);
        plotHelpJButton.setContentAreaFilled(false);
        plotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        plotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                plotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                plotHelpJButtonMouseExited(evt);
            }
        });
        plotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotHelpJButtonActionPerformed(evt);
            }
        });
        plotLayeredPane.add(plotHelpJButton);
        plotHelpJButton.setBounds(990, 0, 10, 19);
        plotLayeredPane.setLayer(plotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        exportPlotsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPlotsJButton.setToolTipText("Export");
        exportPlotsJButton.setBorder(null);
        exportPlotsJButton.setBorderPainted(false);
        exportPlotsJButton.setContentAreaFilled(false);
        exportPlotsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportPlotsJButton.setEnabled(false);
        exportPlotsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportPlotsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportPlotsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportPlotsJButtonMouseExited(evt);
            }
        });
        exportPlotsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportPlotsJButtonActionPerformed(evt);
            }
        });
        plotLayeredPane.add(exportPlotsJButton);
        exportPlotsJButton.setBounds(980, 0, 10, 19);
        plotLayeredPane.setLayer(exportPlotsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        contextMenuPlotsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuPlotsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuPlotsBackgroundPanel);
        contextMenuPlotsBackgroundPanel.setLayout(contextMenuPlotsBackgroundPanelLayout);
        contextMenuPlotsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuPlotsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        contextMenuPlotsBackgroundPanelLayout.setVerticalGroup(
            contextMenuPlotsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        plotLayeredPane.add(contextMenuPlotsBackgroundPanel);
        contextMenuPlotsBackgroundPanel.setBounds(980, 0, 30, 19);
        plotLayeredPane.setLayer(contextMenuPlotsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(plotLayeredPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1019, Short.MAX_VALUE)
                    .addComponent(mappingsTableLayeredPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1019, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mappingsTableLayeredPane, javax.swing.GroupLayout.PREFERRED_SIZE, 365, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(plotLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Changes the cursor back to the default cursor a hand.
     *
     * @param evt
     */
    private void goMappingsTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_goMappingsTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_goMappingsTableMouseExited

    /**
     * Changes the cursor into a hand cursor if the table cell contains an html
     * link.
     *
     * @param evt
     */
    private void goMappingsTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_goMappingsTableMouseMoved
        int row = goMappingsTable.rowAtPoint(evt.getPoint());
        int column = goMappingsTable.columnAtPoint(evt.getPoint());

        if (column == goMappingsTable.getColumn("GO Accession").getModelIndex() && goMappingsTable.getValueAt(row, column) != null) {

            String tempValue = (String) goMappingsTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }
    }//GEN-LAST:event_goMappingsTableMouseMoved

    /**
     * If the user clicks the GO term column the GO term is opened in the web
     * browser.
     *
     * @param evt
     */
    private void goMappingsTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_goMappingsTableMouseReleased

        int row = goMappingsTable.getSelectedRow();
        int column = goMappingsTable.getSelectedColumn();

        if (row != -1) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

            if (evt == null || evt.getButton() == MouseEvent.BUTTON1) {

                // open protein link in web browser
                if (column == goMappingsTable.getColumn("GO Accession").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) goMappingsTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                    String link = (String) goMappingsTable.getValueAt(row, column);
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                } else if (column == goMappingsTable.getColumn("  ").getModelIndex()) {
                    updateGoPlots();
                }

                updatePlotMarkers();
                updateProteinTable();
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        }

        if (evt != null && goMappingsTable.getRowCount() > 0 && evt.getButton() == MouseEvent.BUTTON3) {
            currentGoMappingsColumn = goMappingsTable.getColumnName(goMappingsTable.columnAtPoint(evt.getPoint()));
            selectTermsJPopupMenu.show(goMappingsTable, evt.getX(), evt.getY());
        } else {
            currentGoMappingsColumn = null;
        }
    }//GEN-LAST:event_goMappingsTableMouseReleased

    /**
     * Select all the mappings and update the plot.
     *
     * @param evt
     */
    private void selectAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllMenuItemActionPerformed

        for (int i = 0; i < goMappingsTable.getRowCount(); i++) {
            ((DefaultTableModel) goMappingsTable.getModel()).setValueAt(true, i, goMappingsTable.getColumn("  ").getModelIndex());
        }

        updateGoPlots();
    }//GEN-LAST:event_selectAllMenuItemActionPerformed

    /**
     * Deselect all the mappings and update the plot.
     *
     * @param evt
     */
    private void deselectAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deselectAllMenuItemActionPerformed

        for (int i = 0; i < goMappingsTable.getRowCount(); i++) {
            ((DefaultTableModel) goMappingsTable.getModel()).setValueAt(false, i, goMappingsTable.getColumn("  ").getModelIndex());
        }

        updateGoPlots();
    }//GEN-LAST:event_deselectAllMenuItemActionPerformed

    /**
     * Select all the significant mappings and update the plot.
     *
     * @param evt
     */
    private void selectSignificantMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectSignificantMenuItemActionPerformed

        for (int i = 0; i < goMappingsTable.getRowCount(); i++) {
            if (((ValueAndBooleanDataPoint) goMappingsTable.getModel().getValueAt(i, goMappingsTable.getColumn("Log2 Diff").getModelIndex())).isSignificant()) {
                ((DefaultTableModel) goMappingsTable.getModel()).setValueAt(true, i, goMappingsTable.getColumn("  ").getModelIndex());
            } else {
                ((DefaultTableModel) goMappingsTable.getModel()).setValueAt(false, i, goMappingsTable.getColumn("  ").getModelIndex());
            }
        }

        updateGoPlots();
    }//GEN-LAST:event_selectSignificantMenuItemActionPerformed

    /**
     * Update the plot markers and the protein table.
     *
     * @param evt
     */
    private void goMappingsTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_goMappingsTableKeyReleased
        updatePlotMarkers();
        updateProteinTable();
    }//GEN-LAST:event_goMappingsTableKeyReleased

    /**
     * Resize the layered panes.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // move the icons
        mappingsTableLayeredPane.getComponent(0).setBounds(
                mappingsTableLayeredPane.getWidth() - mappingsTableLayeredPane.getComponent(0).getWidth() - 10,
                -3,
                mappingsTableLayeredPane.getComponent(0).getWidth(),
                mappingsTableLayeredPane.getComponent(0).getHeight());

        mappingsTableLayeredPane.getComponent(1).setBounds(
                mappingsTableLayeredPane.getWidth() - mappingsTableLayeredPane.getComponent(1).getWidth() - 20,
                -3,
                mappingsTableLayeredPane.getComponent(1).getWidth(),
                mappingsTableLayeredPane.getComponent(1).getHeight());

        mappingsTableLayeredPane.getComponent(2).setBounds(
                mappingsTableLayeredPane.getWidth() - mappingsTableLayeredPane.getComponent(2).getWidth() - 5,
                -3,
                mappingsTableLayeredPane.getComponent(2).getWidth(),
                mappingsTableLayeredPane.getComponent(2).getHeight());

        // resize the plot area
        mappingsTableLayeredPane.getComponent(3).setBounds(0, 0, mappingsTableLayeredPane.getWidth(), mappingsTableLayeredPane.getHeight());
        mappingsTableLayeredPane.revalidate();
        mappingsTableLayeredPane.repaint();

        // move the icons
        plotLayeredPane.getComponent(0).setBounds(
                plotLayeredPane.getWidth() - plotLayeredPane.getComponent(0).getWidth() - 10,
                -3,
                plotLayeredPane.getComponent(0).getWidth(),
                plotLayeredPane.getComponent(0).getHeight());

        plotLayeredPane.getComponent(1).setBounds(
                plotLayeredPane.getWidth() - plotLayeredPane.getComponent(1).getWidth() - 20,
                -3,
                plotLayeredPane.getComponent(1).getWidth(),
                plotLayeredPane.getComponent(1).getHeight());

        plotLayeredPane.getComponent(2).setBounds(
                plotLayeredPane.getWidth() - plotLayeredPane.getComponent(2).getWidth() - 5,
                -3,
                plotLayeredPane.getComponent(2).getWidth(),
                plotLayeredPane.getComponent(2).getHeight());

        // resize the plot area
        plotLayeredPane.getComponent(3).setBounds(0, 0, plotLayeredPane.getWidth(), plotLayeredPane.getHeight());
        plotLayeredPane.revalidate();
        plotLayeredPane.repaint();
    }//GEN-LAST:event_formComponentResized

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void mappingsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mappingsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_mappingsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void mappingsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mappingsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_mappingsHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void mappingsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mappingsHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/GOEA.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                "PeptideShaker - Help");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_mappingsHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportMappingsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportMappingsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportMappingsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportMappingsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportMappingsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportMappingsJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportMappingsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMappingsJButtonActionPerformed

        progressDialog = new ProgressDialogX(peptideShakerGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Copying to Clipboard. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        new Thread("ExportThread") {
            @Override
            public void run() {
                try {
                    String clipboardString = Util.tableToText(goMappingsTable, "\t", progressDialog, true);
                    StringSelection stringSelection = new StringSelection(clipboardString);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, peptideShakerGUI);

                    boolean processCancelled = progressDialog.isRunCanceled();
                    progressDialog.setRunFinished();

                    if (!processCancelled) {
                        JOptionPane.showMessageDialog(peptideShakerGUI, "Table content copied to clipboard.", "Copied to Clipboard", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(peptideShakerGUI, "An error occurred while generating the output.", "Output Error.", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }//GEN-LAST:event_exportMappingsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void plotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_plotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_plotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void plotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_plotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_plotHelpJButtonMouseExited

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void plotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotHelpJButtonActionPerformed
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        if (goPlotsTabbedPane.getSelectedIndex() == 0) {
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/GOEA.html"), "#Proteins",
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "PeptideShaker - Help");
        } else {
            new HelpDialog(peptideShakerGUI, getClass().getResource("/helpFiles/GOEA.html"), "#GO_Plots",
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    "PeptideShaker - Help");
        }

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_plotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportPlotsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPlotsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportPlotsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportPlotsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportPlotsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportPlotsJButtonMouseExited

    /**
     * Export the table contents.
     *
     * @param evt
     */
    private void exportPlotsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportPlotsJButtonActionPerformed

        int index = goPlotsTabbedPane.getSelectedIndex();

        if (index == 1) {
            // frequency plot
            new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, (ChartPanel) goFrequencyPlotPanel.getComponent(0), peptideShakerGUI.getLastSelectedFolder());
        } else if (index == 2) {
            // significance plot
            new ExportGraphicsDialog(peptideShakerGUI, peptideShakerGUI.getNormalIcon(), peptideShakerGUI.getWaitingIcon(), true, (ChartPanel) goSignificancePlotPanel.getComponent(0), peptideShakerGUI.getLastSelectedFolder());
        } else {
            // protein table

            // get the file to send the output to
            final File selectedFile = peptideShakerGUI.getUserSelectedFile("proteins.txt", ".txt", "Tab separated text file (.txt)", "Export...", false);

            if (selectedFile != null) {

                progressDialog = new ProgressDialogX(peptideShakerGUI,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                        true);
                progressDialog.setPrimaryProgressCounterIndeterminate(true);
                progressDialog.setTitle("Exporting to File. Please Wait...");

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            progressDialog.setVisible(true);
                        } catch (IndexOutOfBoundsException e) {
                            // ignore error
                        }
                    }
                }, "ProgressDialog").start();

                new Thread("ExportThread") {
                    @Override
                    public void run() {

                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile));
                            Util.tableToFile(proteinTable, "\t", progressDialog, true, writer);
                            writer.close();

                            boolean processCancelled = progressDialog.isRunCanceled();
                            progressDialog.setRunFinished();

                            if (!processCancelled) {
                                JOptionPane.showMessageDialog(peptideShakerGUI, "Data copied to file:\n" + selectedFile.getAbsolutePath(), "Data Exported.", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (IOException e) {
                            progressDialog.setRunFinished();
                            JOptionPane.showMessageDialog(null, "An error occurred when exporting the table content.", "Export Failed", JOptionPane.ERROR_MESSAGE);
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    }//GEN-LAST:event_exportPlotsJButtonActionPerformed

    /**
     * Update the analysis with the new significance threshold.
     *
     * @param evt
     */
    private void fivePercentRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fivePercentRadioButtonActionPerformed
        updateMappings();
    }//GEN-LAST:event_fivePercentRadioButtonActionPerformed

    /**
     * Update the analysis with the new significance threshold.
     *
     * @param evt
     */
    private void onePercentRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onePercentRadioButtonActionPerformed
        updateMappings();
    }//GEN-LAST:event_onePercentRadioButtonActionPerformed

    /**
     * Changes the cursor into a hand cursor if the table cell contains an html
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
     * Opens the protein accession number link in a browser.
     *
     * @param evt
     */
    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased

        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        if (row != -1 && evt.getButton() == MouseEvent.BUTTON1) {

            // update the protein selection
            proteinTableKeyReleased(null);

            // open protein link in web browser
            if (column == proteinTable.getColumn("Accession").getModelIndex()
                    && ((String) proteinTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                String link = (String) proteinTable.getValueAt(row, column);
                link = link.substring(link.indexOf("\"") + 1);
                link = link.substring(0, link.indexOf("\""));

                this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                BareBonesBrowserLaunch.openURL(link);
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        }
    }//GEN-LAST:event_proteinTableMouseReleased

    /**
     * Update the protein selection.
     *
     * @param evt
     */
    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased

        int row = proteinTable.getSelectedRow();

        if (row != -1) {
            // update the protein selection
            String selectedProtein = (String) proteinTable.getValueAt(row, proteinTable.getColumn("Accession").getModelIndex());
            selectedProtein = selectedProtein.substring(selectedProtein.lastIndexOf("\">") + "\">".length(), selectedProtein.lastIndexOf("</font>"));
            String psmKey = PeptideShakerGUI.NO_SELECTION;

            // try to select the "best" peptide for the selected peptide
            String peptideKey = peptideShakerGUI.getDefaultPeptideSelection(selectedProtein);

            // try to select the "best" psm for the selected peptide
            if (!peptideKey.equalsIgnoreCase(PeptideShakerGUI.NO_SELECTION)) {
                psmKey = peptideShakerGUI.getDefaultPsmSelection(peptideKey);
            }

            peptideShakerGUI.setSelectedItems(selectedProtein, peptideKey, psmKey);
        }
    }//GEN-LAST:event_proteinTableKeyReleased

    /**
     * Open the XY plotting dialog.
     *
     * @param evt
     */
    private void statisticsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statisticsMenuItemActionPerformed
        new XYPlottingDialog(peptideShakerGUI, goMappingsTable, currentGoMappingsColumn, PlottingDialogPlotType.densityPlot, mappingsTableToolTips,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true);
    }//GEN-LAST:event_statisticsMenuItemActionPerformed

    /**
     * Make sure that a go term is selected.
     *
     * @param evt
     */
    private void goPlotsTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_goPlotsTabbedPaneStateChanged
        if (goPlotsTabbedPane.getSelectedIndex() == 0) {
            if (goMappingsTable.getSelectedRow() == -1 && goMappingsTable.getRowCount() > 0) {
                goMappingsTable.setRowSelectionInterval(0, 0);
                goMappingsTableKeyReleased(null);
            }
        }
    }//GEN-LAST:event_goPlotsTabbedPaneStateChanged

    /**
     * Show the statistics popup menu.
     *
     * @param evt
     */
    private void proteinTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3 && proteinTable.getRowCount() > 0) {
            final MouseEvent event = evt;
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Statistics (beta)");
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    new XYPlottingDialog(peptideShakerGUI, proteinTable, proteinTable.getColumnName(proteinTable.columnAtPoint(event.getPoint())), PlottingDialogPlotType.densityPlot, proteinTableToolTips,
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")), true);
                }
            });
            popupMenu.add(menuItem);
            popupMenu.show(proteinTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_proteinTableMouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contextMenuMappingsBackgroundPanel;
    private javax.swing.JPanel contextMenuPlotsBackgroundPanel;
    private javax.swing.JMenuItem deselectAllMenuItem;
    private javax.swing.JButton exportMappingsJButton;
    private javax.swing.JButton exportPlotsJButton;
    private javax.swing.JRadioButton fivePercentRadioButton;
    private javax.swing.JPanel goFrequencyPlotPanel;
    private javax.swing.JTable goMappingsTable;
    private javax.swing.JTabbedPane goPlotsTabbedPane;
    private javax.swing.JLabel goProteinCountLabel;
    private javax.swing.JPanel goSignificancePlotPanel;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JButton mappingsHelpJButton;
    private javax.swing.JPanel mappingsPanel;
    private javax.swing.JLayeredPane mappingsTableLayeredPane;
    private javax.swing.JRadioButton onePercentRadioButton;
    private javax.swing.JButton plotHelpJButton;
    private javax.swing.JLayeredPane plotLayeredPane;
    private javax.swing.JPanel plotPanel;
    private javax.swing.JScrollPane proteinGoMappingsScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JPanel proteinsPanel;
    private javax.swing.JScrollPane proteinsScrollPane;
    private javax.swing.JMenuItem selectAllMenuItem;
    private javax.swing.JMenuItem selectSignificantMenuItem;
    private javax.swing.JPopupMenu selectTermsJPopupMenu;
    private javax.swing.JLabel significanceJLabel;
    private javax.swing.ButtonGroup significanceLevelButtonGroup;
    private javax.swing.JMenuItem statisticsMenuItem;
    // End of variables declaration//GEN-END:variables

    /**
     * Update the plot markers.
     */
    private void updatePlotMarkers() {

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        if (signChartPanel != null && goMappingsTable.getSelectedRow() != -1) {

            removePlotMarkers();

            signChartPanel.getChart().getCategoryPlot().addDomainMarker(
                    new CategoryMarker((String) goMappingsTable.getValueAt(goMappingsTable.getSelectedRow(), goMappingsTable.getColumn("GO Term").getModelIndex()),
                            Color.LIGHT_GRAY, new BasicStroke(1.0f), Color.LIGHT_GRAY, new BasicStroke(1.0f), 0.2f), Layer.BACKGROUND);

            distributionChartPanel.getChart().getCategoryPlot().addDomainMarker(
                    new CategoryMarker((String) goMappingsTable.getValueAt(goMappingsTable.getSelectedRow(), goMappingsTable.getColumn("GO Term").getModelIndex()),
                            Color.LIGHT_GRAY, new BasicStroke(1.0f), Color.LIGHT_GRAY, new BasicStroke(1.0f), 0.2f), Layer.BACKGROUND);

            ((TitledBorder) plotPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Gene Ontology Enrichment Analysis - "
                    + goMappingsTable.getValueAt(goMappingsTable.getSelectedRow(), goMappingsTable.getColumn("GO Term").getModelIndex())
                    + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        } else {
            ((TitledBorder) plotPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Gene Ontology Enrichment Analysis"
                    + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        }

        plotPanel.repaint();

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    /**
     * Removes the plot markers.
     */
    private void removePlotMarkers() {
        if (signChartPanel != null && signChartPanel.getChart().getCategoryPlot().getDomainMarkers(Layer.BACKGROUND) != null) {

            Iterator iterator = signChartPanel.getChart().getCategoryPlot().getDomainMarkers(Layer.BACKGROUND).iterator();

            // store the keys in a list first to escape a ConcurrentModificationException
            ArrayList<CategoryMarker> tempMarkers = new ArrayList<CategoryMarker>();

            while (iterator.hasNext()) {
                tempMarkers.add((CategoryMarker) iterator.next());
            }

            for (int i = 0; i < tempMarkers.size(); i++) {
                signChartPanel.getChart().getCategoryPlot().removeDomainMarker(i, tempMarkers.get(i), Layer.BACKGROUND);
            }

            iterator = distributionChartPanel.getChart().getCategoryPlot().getDomainMarkers(Layer.BACKGROUND).iterator();

            // store the keys in a list first to escape a ConcurrentModificationException
            tempMarkers = new ArrayList<CategoryMarker>();

            while (iterator.hasNext()) {
                tempMarkers.add((CategoryMarker) iterator.next());
            }

            for (int i = 0; i < tempMarkers.size(); i++) {
                distributionChartPanel.getChart().getCategoryPlot().removeDomainMarker(i, tempMarkers.get(i), Layer.BACKGROUND);
            }

            goPlotsTabbedPane.repaint();
        }
    }

    /**
     * Sort the p-values and make the same changes to the table indexes.
     *
     * @param pValues
     * @param tableIndexes
     */
    private void sortPValues(ArrayList<Double> pValues, ArrayList<Integer> tableIndexes) {

        // iterate p-value list
        for (int i = 0; i < pValues.size(); i++) {

            double maxValue = -1;
            int maxIndex = -1;

            // find max p-value in sublist
            for (int j = i; j < pValues.size(); j++) {
                if (pValues.get(j) > maxValue) {
                    maxValue = pValues.get(j);
                    maxIndex = j;
                }
            }

            // move in both lists
            double oldValue = pValues.get(i);
            pValues.set(i, pValues.get(maxIndex));
            pValues.set(maxIndex, oldValue);

            int oldIntValue = tableIndexes.get(i);
            tableIndexes.set(i, tableIndexes.get(maxIndex));
            tableIndexes.set(maxIndex, oldIntValue);
        }
    }

    /**
     * Update the GO mappings.
     */
    public void updateMappings() {

        // update the p-value sparkline significance color
        double significanceLevel = 0.05;

        if (onePercentRadioButton.isSelected()) {
            significanceLevel = 0.01;
        }

        goMappingsTable.getColumn("p-value").setCellRenderer(new JSparklinesBarChartTableCellRenderer(
                PlotOrientation.HORIZONTAL, 1.0, peptideShakerGUI.getSparklineColor(), Color.lightGray, significanceLevel));

        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("p-value").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth());

        clearOldResults();

        if (peptideShakerGUI.getIdentification() != null) {

            final Thread displayThread = new Thread("DisplayThread") {
                @Override
                public void run() {
                    displayResults();
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
    }

    /**
     * Clear the old results.
     */
    public void clearOldResults() {

        // clear old results
        DefaultTableModel dm = (DefaultTableModel) goMappingsTable.getModel();
        dm.getDataVector().removeAllElements();
        dm.fireTableDataChanged();

        dm = (DefaultTableModel) proteinTable.getModel();
        dm.getDataVector().removeAllElements();
        dm.fireTableDataChanged();

        goFrequencyPlotPanel.removeAll();
        goFrequencyPlotPanel.revalidate();
        goFrequencyPlotPanel.repaint();

        goSignificancePlotPanel.removeAll();
        goSignificancePlotPanel.revalidate();
        goSignificancePlotPanel.repaint();

        ((TitledBorder) mappingsPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Gene Ontology Mappings"
                + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        mappingsPanel.revalidate();
        mappingsPanel.repaint();

        ((TitledBorder) plotPanel.getBorder()).setTitle(PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Gene Ontology Enrichment Analysis"
                + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING);
        plotPanel.repaint();

        goProteinCountLabel.setText("[GO Proteins: Ensembl: -, Project: -]");
    }

    /**
     * Hides or displays the score column in the protein table.
     */
    public void updateScores() {

        ((DefaultTableModel) proteinTable.getModel()).fireTableStructureChanged();
        setProteinGoTableProperties();

        if (peptideShakerGUI.getDisplayPreferences().showScores()) {
            proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Score");
        } else {
            proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Confidence");
        }

        setProteinGoTableProperties();
    }

    /**
     * Displays or hide sparklines in the tables.
     *
     * @param showSparkLines boolean indicating whether sparklines shall be
     * displayed or hidden
     */
    public void showSparkLines(boolean showSparkLines) {

        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("Frequency All (%)").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("Frequency Dataset (%)").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("Log2 Diff").getCellRenderer()).showNumbers(!showSparkLines);
        ((JSparklinesBarChartTableCellRenderer) goMappingsTable.getColumn("p-value").getCellRenderer()).showNumbers(!showSparkLines);

        goMappingsTable.revalidate();
        goMappingsTable.repaint();

        if (proteinTable.getModel() instanceof ProteinGoTableModel) {
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MS2 Quant.").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("Coverage").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).showNumbers(!showSparkLines);
            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).showNumbers(!showSparkLines);

            try {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumbers(!showSparkLines);
            } catch (IllegalArgumentException e) {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumbers(!showSparkLines);
            }

            proteinTable.revalidate();
            proteinTable.repaint();
        }
    }

    /**
     * Update the protein table.
     */
    private void updateProteinTable() {

        // @TODO: order the proteins in some way?
        if (goMappingsTable.getSelectedRow() != -1) {

            progressDialog = new ProgressDialogX(peptideShakerGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true);
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Loading Protein Data. Please Wait...");

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
                        // clear the old data
                        DefaultTableModel dm = (DefaultTableModel) proteinTable.getModel();
                        dm.getDataVector().removeAllElements();
                        dm.fireTableDataChanged();

                        // get the selected go accession number
                        String selectedGoAccession = (String) goMappingsTable.getValueAt(goMappingsTable.getSelectedRow(), goMappingsTable.getColumn("GO Accession").getModelIndex());

                        // remove the html tags
                        selectedGoAccession = selectedGoAccession.substring(selectedGoAccession.lastIndexOf("GTerm?id=") + "GTerm?id=".length(), selectedGoAccession.lastIndexOf("\"><font"));

                        // get the list of matching proteins
                        GeneMaps geneMaps = peptideShakerGUI.getGeneMaps();
                        HashSet<String> goProteins = geneMaps.getProteinsForGoTerm(selectedGoAccession);
                        HashSet<String> proteinKeys = new HashSet<String>(goProteins.size());
                        Identification identification = peptideShakerGUI.getIdentification();
                        HashMap<String, HashSet<String>> proteinMap = identification.getProteinMap();
                        for (String goProtein : goProteins) {
                            HashSet<String> tempKeys = proteinMap.get(goProtein);
                            if (tempKeys != null) {
                                proteinKeys.addAll(tempKeys);
                            }
                        }

                        ArrayList<String> proteinKeysList = new ArrayList<String>(proteinKeys);
                        identification.loadProteinMatches(proteinKeysList, progressDialog, false);
                        identification.loadProteinMatchParameters(proteinKeysList, new PSParameter(), progressDialog, false);

                        // update the table
                        if (proteinTable.getModel() instanceof ProteinGoTableModel) {
                            ((ProteinGoTableModel) proteinTable.getModel()).updateDataModel(peptideShakerGUI, proteinKeysList);
                        } else {
                            ProteinGoTableModel proteinTableModel = new ProteinGoTableModel(peptideShakerGUI, proteinKeysList);
                            proteinTable.setModel(proteinTableModel);
                        }

                        setProteinGoTableProperties();
                        ((DefaultTableModel) proteinTable.getModel()).fireTableDataChanged();

                        if (proteinTable.getRowCount() > 0) {

                            // get the number of confident and doubtful matches
                            int nConfident = 0;
                            int nDoubtful = 0;
                            PSParameter psParameter = new PSParameter();

                            for (String proteinKey : proteinKeys) {
                                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);
                                MatchValidationLevel level = psParameter.getMatchValidationLevel();

                                if (level == MatchValidationLevel.confident) {
                                    nConfident++;
                                } else if (level == MatchValidationLevel.doubtful) {
                                    nDoubtful++;
                                }
                            }

                            String title = PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Gene Ontology Enrichment Analysis - "
                                    + goMappingsTable.getValueAt(goMappingsTable.getSelectedRow(), goMappingsTable.getColumn("GO Term").getModelIndex())
                                    + " (";
                            try {
                                if (nConfident > 0) {
                                    title += (nConfident + nDoubtful) + "/" + proteinKeys.size() + " - " + nConfident + " confident, " + nDoubtful + " doubtful";
                                } else {
                                    title += proteinKeys.size();
                                }
                            } catch (Exception eNValidated) {
                                peptideShakerGUI.catchException(eNValidated);
                            }
                            title += ")" + PeptideShakerGUI.TITLED_BORDER_HORIZONTAL_PADDING;
                            ((TitledBorder) plotPanel.getBorder()).setTitle(title);
                            plotPanel.repaint();

                            proteinTable.setRowSelectionInterval(0, 0);
                            proteinTable.scrollRectToVisible(proteinTable.getCellRect(0, 0, false));

                            // update the protein selection
                            String selectedProtein = (String) proteinTable.getValueAt(0, proteinTable.getColumn("Accession").getModelIndex());
                            selectedProtein = selectedProtein.substring(selectedProtein.lastIndexOf("\">") + "\">".length(), selectedProtein.lastIndexOf("</font>"));
                            peptideShakerGUI.setSelectedItems(selectedProtein, PeptideShakerGUI.NO_SELECTION, PeptideShakerGUI.NO_SELECTION);
                            proteinTableKeyReleased(null);
                        }

                        progressDialog.setRunFinished();

                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        peptideShakerGUI.catchException(e);
                    }
                }
            }.start();
        }
    }
}
