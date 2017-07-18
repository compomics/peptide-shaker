package eu.isas.peptideshaker.gui.tablemodels;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.preferences.DisplayPreferences;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import java.awt.Color;
import java.awt.Component;
import java.sql.SQLNonTransientConnectionException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import no.uib.jsparklines.data.ArrrayListDataPoints;
import no.uib.jsparklines.data.Chromosome;
import no.uib.jsparklines.extra.ChromosomeTableCellRenderer;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Model for the protein table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinTableModel extends SelfUpdatingTableModel {

    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * The identification features generator provides identification information
     * on the matches.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The display features generator provides display features.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The exception handler catches exceptions.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The list of the keys of the protein matches being displayed.
     */
    private ArrayList<String> proteinKeys = null;
    /**
     * If true the scores will be shown.
     */
    private boolean showScores = false;
    /**
     * The batch size.
     */
    private int batchSize = 20;
    /**
     * The gene maps.
     */
    private GeneMaps geneMaps;

    /**
     * Constructor which sets a new empty table.
     */
    public ProteinTableModel() {
    }

    /**
     * Constructor.
     *
     * @param identification the identification containing the protein
     * information
     * @param identificationFeaturesGenerator the identification features
     * generator generating the features of the identification
     * @param geneMaps the gene maps
     * @param displayFeaturesGenerator the display features generator generating
     * the display elements
     * @param exceptionHandler an exception handler catching exceptions
     * @param proteinKeys the keys of the protein matches to display
     */
    public ProteinTableModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps, 
            DisplayFeaturesGenerator displayFeaturesGenerator, ExceptionHandler exceptionHandler, ArrayList<String> proteinKeys) {
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.geneMaps = geneMaps;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.exceptionHandler = exceptionHandler;
        this.proteinKeys = proteinKeys;
    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param identification the identification containing the protein
     * information
     * @param identificationFeaturesGenerator the identification features
     * generator generating the features of the identification
     * @param geneMaps the gene maps
     * @param displayFeaturesGenerator the display features generator generating
     * the display elements
     * @param exceptionHandler an exception handler catching exceptions
     * @param proteinKeys the keys of the protein matches to display
     */
    public void updateDataModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, GeneMaps geneMaps, 
            DisplayFeaturesGenerator displayFeaturesGenerator, ExceptionHandler exceptionHandler, ArrayList<String> proteinKeys) {
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.geneMaps = geneMaps;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.exceptionHandler = exceptionHandler;
        this.proteinKeys = proteinKeys;
    }

    /**
     * Sets whether the scores should be displayed.
     *
     * @param showScores a boolean indicating whether the scores should be
     * displayed
     */
    public void showScores(boolean showScores) {
        this.showScores = showScores;
    }

    /**
     * Reset the protein keys.
     */
    public void reset() {
        proteinKeys = null;
    }

    @Override
    public int getRowCount() {
        if (proteinKeys != null) {
            return proteinKeys.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        return 13;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return " ";
            case 1:
                return "  ";
            case 2:
                return "PI";
            case 3:
                return "Accession";
            case 4:
                return "Description";
            case 5:
                return "Chr";
            case 6:
                return "Coverage";
            case 7:
                return "#Peptides";
            case 8:
                return "#Spectra";
            case 9:
                return "MS2 Quant.";
            case 10:
                return "MW";
            case 11:
                if (showScores) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 12:
                return "";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        if (proteinKeys != null) {

            int viewIndex = getViewIndex(row);

            try {
                boolean useDB = !isSelfUpdating();
                String proteinKey = proteinKeys.get(viewIndex);

                switch (column) {
                    case 0:
                        return viewIndex + 1;
                    case 1:
                        PSParameter psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB && !isScrolling);
                        if (psParameter == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        return psParameter.isStarred();
                    case 2:
                        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB && !isScrolling);
                        if (psParameter == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        return psParameter.getProteinInferenceClass();
                    case 3:
                        ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey, useDB && !isScrolling);
                        if (proteinMatch == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        if (!isScrolling) {
                            return displayFeaturesGenerator.addDatabaseLink(proteinMatch.getMainMatch());
                        } else {
                            return proteinMatch.getMainMatch();
                        }
                    case 4:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB && !isScrolling);
                        if (proteinMatch == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        String description = null;
                        try {
                            description = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getSimpleProteinDescription();

                            // if description is not set, return the accession instead - fix for home made fasta headers
                            if (description == null || description.trim().isEmpty()) {
                                description = proteinMatch.getMainMatch();
                            }
                        } catch (Exception e) {
                            exceptionHandler.catchException(e);
                        }
                        return description;
                    case 5:
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB && !isScrolling);
                        if (proteinMatch == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        String geneName = sequenceFactory.getHeader(proteinMatch.getMainMatch()).getGeneName();
                        String chromosomeName = geneMaps.getChromosome(geneName);
                        if (chromosomeName == null || chromosomeName.length() == 0) {
                            return new Chromosome(null);
                        } else {
                            return new Chromosome(chromosomeName);
                        }
                    case 6:
                        if (isScrolling) {
                            return null;
                        }
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && (!identificationFeaturesGenerator.sequenceCoverageInCache(proteinKey)
                                || !identificationFeaturesGenerator.observableCoverageInCache(proteinKey))
                                && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        HashMap<Integer, Double> sequenceCoverage;
                        try {
                            sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                        } catch (Exception e) {
                            exceptionHandler.catchException(e);
                            return Double.NaN;
                        }
                        Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                        Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                        Double sequenceCoverageNotValidated = 100 * sequenceCoverage.get(MatchValidationLevel.not_validated.getIndex());
                        double possibleCoverage = 100;
                        try {
                            possibleCoverage = 100 * identificationFeaturesGenerator.getObservableCoverage(proteinKey);
                        } catch (Exception e) {
                            exceptionHandler.catchException(e);
                        }
                        ArrayList<Double> doubleValues = new ArrayList<Double>();
                        doubleValues.add(sequenceCoverageConfident);
                        doubleValues.add(sequenceCoverageDoubtful);
                        doubleValues.add(sequenceCoverageNotValidated);
                        doubleValues.add(possibleCoverage - sequenceCoverageConfident - sequenceCoverageDoubtful - sequenceCoverageNotValidated);
                        ArrrayListDataPoints arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumExceptLastNumber);
                        return arrrayListDataPoints;
                    case 7:
                        if (isScrolling) {
                            return null;
                        }
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && (proteinMatch == null
                                || !identificationFeaturesGenerator.nValidatedPeptidesInCache(proteinKey)
                                && !identification.proteinDetailsInCache(proteinKey))) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        double nConfidentPeptides = identificationFeaturesGenerator.getNConfidentPeptides(proteinKey);
                        double nDoubtfulPeptides = identificationFeaturesGenerator.getNValidatedPeptides(proteinKey) - nConfidentPeptides;

                        doubleValues = new ArrayList<Double>();
                        doubleValues.add(nConfidentPeptides);
                        doubleValues.add(nDoubtfulPeptides);
                        doubleValues.add(proteinMatch.getPeptideCount() - nConfidentPeptides - nDoubtfulPeptides);
                        arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
                        return arrrayListDataPoints;
                    case 8:
                        if (isScrolling) {
                            return null;
                        }
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB
                                && (!identificationFeaturesGenerator.nValidatedSpectraInCache(proteinKey)
                                || !identificationFeaturesGenerator.nSpectraInCache(proteinKey))
                                && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        double nConfidentSpectra = identificationFeaturesGenerator.getNConfidentSpectra(proteinKey);
                        double nDoubtfulSpectra = identificationFeaturesGenerator.getNValidatedSpectra(proteinKey) - nConfidentSpectra;
                        int nSpectra = identificationFeaturesGenerator.getNSpectra(proteinKey);

                        doubleValues = new ArrayList<Double>();
                        doubleValues.add(nConfidentSpectra);
                        doubleValues.add(nDoubtfulSpectra);
                        doubleValues.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                        arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
                        return arrrayListDataPoints;
                    case 9:
                        if (isScrolling) {
                            return null;
                        }
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && !identificationFeaturesGenerator.spectrumCountingInCache(proteinKey)
                                && (proteinMatch == null || !identification.proteinDetailsInCache(proteinKey))) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        return identificationFeaturesGenerator.getNormalizedSpectrumCounting(proteinKey);
                    case 10:
                        if (isScrolling) {
                            return null;
                        }
                        proteinMatch = identification.getProteinMatch(proteinKey, useDB);
                        if (!useDB && proteinMatch == null) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                        String mainMatch = proteinMatch.getMainMatch();
                        Protein currentProtein = sequenceFactory.getProtein(mainMatch);
                        if (currentProtein != null) {
                            return sequenceFactory.computeMolecularWeight(mainMatch);
                        } else {
                            return null;
                        }
                    case 11:
                        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB && !isScrolling);
                        if (psParameter == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        if (psParameter != null) {
                            if (showScores) {
                                return psParameter.getProteinScore();
                            } else {
                                return psParameter.getProteinConfidence();
                            }
                        } else {
                            return null;
                        }
                    case 12:
                        psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, new PSParameter(), useDB && !isScrolling);
                        if (psParameter == null) {
                            if (isScrolling) {
                                return null;
                            } else if (!useDB) {
                                dataMissingAtRow(row);
                                return DisplayPreferences.LOADING_MESSAGE;
                            }
                        }
                        if (psParameter != null) {
                            return psParameter.getMatchValidationLevel().getIndex();
                        } else {
                            return null;
                        }
                    default:
                        return null;
                }
            } catch (SQLNonTransientConnectionException e) {
                // this one can be ignored i think?
                return null;
            } catch (Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.catchException(e);
                } else {
                    throw new IllegalArgumentException("Table not instantiated.");
                }
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Indicates whether the table content was instantiated.
     *
     * @return a boolean indicating whether the table content was instantiated.
     */
    public boolean isInstantiated() {
        return identification != null;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, columnIndex) != null) {
                return getValueAt(i, columnIndex).getClass();
            }
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    protected void catchException(Exception e) {
        setSelfUpdating(false);
        exceptionHandler.catchException(e);
    }

    @Override
    protected int loadDataForRows(ArrayList<Integer> rows, WaitingHandler waitingHandler) {

        ArrayList<String> tempKeys = new ArrayList<String>();
        for (int i : rows) {
            String proteinKey = proteinKeys.get(i);
            tempKeys.add(proteinKey);
        }

        try {
            ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
            parameters.add(new PSParameter());

            ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(tempKeys, parameters, true, parameters, true, parameters, waitingHandler);
            proteinMatchesIterator.setBatchSize(batchSize);

            int i = 0;
            while (proteinMatchesIterator.hasNext()) {
                ProteinMatch proteinMatch = proteinMatchesIterator.next();
                String proteinKey = proteinMatch.getKey();
                if (waitingHandler.isRunCanceled()) {
                    return rows.get(i);
                }
                identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                if (waitingHandler.isRunCanceled()) {
                    return rows.get(i);
                }
                identificationFeaturesGenerator.getObservableCoverage(proteinKey);
                if (waitingHandler.isRunCanceled()) {
                    return rows.get(i);
                }
                identificationFeaturesGenerator.getNValidatedPeptides(proteinKey);
                if (waitingHandler.isRunCanceled()) {
                    return rows.get(i);
                }
                identificationFeaturesGenerator.getNValidatedSpectra(proteinKey);
                if (waitingHandler.isRunCanceled()) {
                    return rows.get(i);
                }
                identificationFeaturesGenerator.getNSpectra(proteinKey);
                if (waitingHandler.isRunCanceled()) {
                    return rows.get(i);
                }
                identificationFeaturesGenerator.getSpectrumCounting(proteinKey);
                if (waitingHandler.isRunCanceled()) {
                    return rows.get(i);
                }
                i++;
            }
        } catch (SQLNonTransientConnectionException e) {
            // connection has been closed
            return rows.get(rows.size() - 1);
        } catch (Exception e) {
            catchException(e);
            return rows.get(0);
        }

        return rows.get(rows.size() - 1);
    }

    @Override
    protected void loadDataForColumn(int column, WaitingHandler waitingHandler) {
        try {
            if (column == 1
                    || column == 2
                    || column == 11
                    || column == 12) {
                identification.loadProteinMatchParameters(proteinKeys, new PSParameter(), waitingHandler, false);
            } else if (column == 3
                    || column == 4
                    || column == 5
                    || column == 6
                    || column == 7
                    || column == 8
                    || column == 9
                    || column == 10) {
                identification.loadProteinMatches(proteinKeys, waitingHandler, false);
            }
        } catch (Exception e) {
            catchException(e);
        }
    }

    /**
     * Set up the properties of the protein table.
     *
     * @param proteinTable the protein table
     * @param sparklineColor the sparkline color to use
     * @param sparklineColorNotValidated the sparkline color for not validated
     * stuffs
     * @param parentClass the parent class used to get icons
     * @param sparklineColorNotFound the sparkline color for not found stuffs
     * @param sparklineColorDoubtful the sparkline color for doubtful
     * @param scoreAndConfidenceDecimalFormat the decimal format for score and
     * confidence
     * @param maxProteinKeyLength the longest protein key to display
     */
    public static void setProteinTableProperties(JTable proteinTable, Color sparklineColor, Color sparklineColorNotValidated,
            Color sparklineColorNotFound, Color sparklineColorDoubtful, DecimalFormat scoreAndConfidenceDecimalFormat, Class parentClass, Integer maxProteinKeyLength) {

        // @TODO: find a better location for this method?
        // the index column
        proteinTable.getColumn(" ").setMaxWidth(50);
        proteinTable.getColumn(" ").setMinWidth(50);

        proteinTable.getColumn("Chr").setMaxWidth(50);
        proteinTable.getColumn("Chr").setMinWidth(50);

        try {
            proteinTable.getColumn("Confidence").setMaxWidth(90);
            proteinTable.getColumn("Confidence").setMinWidth(90);
        } catch (IllegalArgumentException w) {
            proteinTable.getColumn("Score").setMaxWidth(90);
            proteinTable.getColumn("Score").setMinWidth(90);
        }

        // the validated column
        proteinTable.getColumn("").setMaxWidth(30);
        proteinTable.getColumn("").setMinWidth(30);

        // the selected columns
        proteinTable.getColumn("  ").setMaxWidth(30);
        proteinTable.getColumn("  ").setMinWidth(30);

        // the protein inference column
        proteinTable.getColumn("PI").setMaxWidth(37);
        proteinTable.getColumn("PI").setMinWidth(37);

        // set up the protein inference color map
        HashMap<Integer, Color> proteinInferenceColorMap = new HashMap<Integer, Color>();
        proteinInferenceColorMap.put(PSParameter.NOT_GROUP, sparklineColor);
        proteinInferenceColorMap.put(PSParameter.RELATED, Color.YELLOW);
        proteinInferenceColorMap.put(PSParameter.RELATED_AND_UNRELATED, Color.ORANGE);
        proteinInferenceColorMap.put(PSParameter.UNRELATED, Color.RED);

        // set up the protein inference tooltip map
        HashMap<Integer, String> proteinInferenceTooltipMap = new HashMap<Integer, String>();
        proteinInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Single Protein");
        proteinInferenceTooltipMap.put(PSParameter.RELATED, "Related Proteins");
        proteinInferenceTooltipMap.put(PSParameter.RELATED_AND_UNRELATED, "Related and Unrelated Proteins");
        proteinInferenceTooltipMap.put(PSParameter.UNRELATED, "Unrelated Proteins");

        proteinTable.getColumn("Accession").setCellRenderer(new HtmlLinksRenderer(TableProperties.getSelectedRowHtmlTagFontColor(), TableProperties.getNotSelectedRowHtmlTagFontColor()));
        proteinTable.getColumn("PI").setCellRenderer(new JSparklinesIntegerColorTableCellRenderer(sparklineColor, proteinInferenceColorMap, proteinInferenceTooltipMap));

        // use a gray color for no decoy searches
        Color nonValidatedColor = sparklineColorNotValidated;
        if (!SequenceFactory.getInstance().isClosed() && !SequenceFactory.getInstance().concatenatedTargetDecoy()) {
            nonValidatedColor = sparklineColorNotFound;
        }
        ArrayList<Color> sparklineColors = new ArrayList<Color>();
        sparklineColors.add(sparklineColor);
        sparklineColors.add(sparklineColorDoubtful);
        sparklineColors.add(nonValidatedColor);
        sparklineColors.add(sparklineColorNotFound);

        JSparklinesArrayListBarChartTableCellRenderer coverageCellRendered = new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColors, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumExceptLastNumber);
        coverageCellRendered.showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0.00"));
        proteinTable.getColumn("Coverage").setCellRenderer(coverageCellRendered);

        JSparklinesArrayListBarChartTableCellRenderer peptidesCellRenderer = new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColors, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
        peptidesCellRenderer.showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0"));
        proteinTable.getColumn("#Peptides").setCellRenderer(peptidesCellRenderer);

        JSparklinesArrayListBarChartTableCellRenderer spectraCellRenderer = new JSparklinesArrayListBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColors, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
        spectraCellRenderer.showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0"));
        proteinTable.getColumn("#Spectra").setCellRenderer(spectraCellRenderer);

        JSparklinesBarChartTableCellRenderer spectrumCountingCellRenderer = new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor);
        spectrumCountingCellRenderer.showNumberAndChart(true, TableProperties.getLabelWidth() + 20, new DecimalFormat("0.00E00"));
        proteinTable.getColumn("MS2 Quant.").setCellRenderer(spectrumCountingCellRenderer);

        JSparklinesBarChartTableCellRenderer mwCellRenderer = new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 10.0, sparklineColor);
        mwCellRenderer.showNumberAndChart(true, TableProperties.getLabelWidth());
        proteinTable.getColumn("MW").setCellRenderer(mwCellRenderer);

        proteinTable.getColumn("Chr").setCellRenderer(new ChromosomeTableCellRenderer());

        try {
            proteinTable.getColumn("Confidence").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, scoreAndConfidenceDecimalFormat);
        } catch (IllegalArgumentException e) {
            proteinTable.getColumn("Score").setCellRenderer(new JSparklinesBarChartTableCellRenderer(PlotOrientation.HORIZONTAL, 100.0, sparklineColor));
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true, TableProperties.getLabelWidth() - 20, scoreAndConfidenceDecimalFormat);
        }

        proteinTable.getColumn("").setCellRenderer(new JSparklinesIntegerIconTableCellRenderer(MatchValidationLevel.getIconMap(parentClass), MatchValidationLevel.getTooltipMap()));
        proteinTable.getColumn("  ").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(parentClass.getResource("/icons/star_yellow.png")),
                new ImageIcon(parentClass.getResource("/icons/star_grey.png")),
                new ImageIcon(parentClass.getResource("/icons/star_grey.png")),
                "Starred", null, null));

        // set the preferred size of the accession column
        if (maxProteinKeyLength != null) {
            Integer width = getPreferredAccessionColumnWidth(proteinTable, proteinTable.getColumn("Accession").getModelIndex(), 6, maxProteinKeyLength);

            if (width != null) {
                proteinTable.getColumn("Accession").setMinWidth(width);
                proteinTable.getColumn("Accession").setMaxWidth(width);
            } else {
                proteinTable.getColumn("Accession").setMinWidth(15);
                proteinTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
            }
        }
    }

    /**
     * Gets the preferred width of the column specified by colIndex. The column
     * will be just wide enough to show the column head and the widest cell in
     * the column. Margin pixels are added to the left and right (resulting in
     * an additional width of 2*margin pixels. Returns null if the max width
     * cannot be set.
     *
     * @param table the table
     * @param colIndex the colum index
     * @param margin the margin to add
     * @param maxProteinKeyLength the maximal protein key length
     *
     * @return the preferred width of the column
     */
    public static Integer getPreferredAccessionColumnWidth(JTable table, int colIndex, int margin, Integer maxProteinKeyLength) {

        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(colIndex);

        // get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        int width = comp.getPreferredSize().width;

        // get maximum width of column data
        if (maxProteinKeyLength == null || (maxProteinKeyLength + 5) > (table.getColumnName(colIndex).length() + margin)) {
            return null;
        }

        // add margin
        width += 2 * margin;

        return width;
    }
}
