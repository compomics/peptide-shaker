package eu.isas.peptideshaker.followup;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.ElementaryIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.preferences.DigestionPreferences;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.io.*;
import java.util.ArrayList;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;

/**
 * This class exports identifications for post-processing with Non-Linear
 * Progenesis as Excel workbooks. Work in progress...
 *
 * @author Harald Barsnes
 */
public class ProgenesisExcelExport {

    /**
     * The waiting handler.
     */
    private WaitingHandler waitingHandler;
    /**
     * The protein keys.
     */
    private ArrayList<String> proteinKeys;
    /**
     * The corresponding identification.
     */
    private Identification identification;
    /**
     * The output file.
     */
    private File outputFile;
    /**
     * The workbook.
     */
    private HSSFWorkbook workbook;
    /**
     * The sheet to write to.
     */
    private HSSFSheet sheet;
    /**
     * The cell styles.
     */
    private CellStyle borderedCellStyle, proteinRowCellStyle, peptideRowCellStyle, a2CellStyle;
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The row in the Excel file that is currently being written to.
     */
    private int currentRow = 0;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;

    /**
     * Constructor.
     *
     * @param waitingHandler the waiting handler
     * @param proteinKeys the protein keys to export
     * @param identification the identifications
     * @param outputFile the file to export to
     * @param identificationParameters the identification parameters
     */
    public ProgenesisExcelExport(WaitingHandler waitingHandler, ArrayList<String> proteinKeys, Identification identification, File outputFile, IdentificationParameters identificationParameters) {
        this.waitingHandler = waitingHandler;
        this.proteinKeys = proteinKeys;
        this.identificationParameters = identificationParameters;
        this.identification = identification;
        this.outputFile = outputFile;
    }

    /**
     * Write the data to an Excel file.
     *
     * @throws Exception thrown if an error occurs when exporting to Excel
     */
    public void writeProgenesisExcelExport() throws Exception {

        // create the workbook and sheet
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet("Sheet1");
        sheet.setRowSumsBelow(false);

        // set the column widths
        setColumnWidths();

        // create cell styles
        createCellStyles();

        waitingHandler.setWaitingText("Loading Data. Please Wait..."); // @TODO: better use of the waiting dialog
        waitingHandler.resetPrimaryProgressCounter();
        waitingHandler.setMaxPrimaryProgressCounter(6);
        waitingHandler.increasePrimaryProgressCounter();

        // set up the waiting handler
        waitingHandler.setWaitingText("Exporting Data. Please Wait...");
        waitingHandler.resetPrimaryProgressCounter();
        waitingHandler.setMaxPrimaryProgressCounter(proteinKeys.size());

        // insert the protein data
        insertProteinData();

        // write the data to an excel file
        if (!waitingHandler.isRunCanceled()) {
            FileOutputStream fileOut = new FileOutputStream(outputFile);
            try {
                workbook.write(fileOut);
            } finally {
                fileOut.close();
            }
        }
    }

    /**
     * Insert the protein data including the peptide details.
     */
    private void insertProteinData() throws Exception {

        // create the protein header row
        createProteinHeader();

        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(new PSParameter());
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);
        ProteinMatch proteinMatch;

        while ((proteinMatch = proteinMatchesIterator.next()) != null) {

            // insert the protein details
            insertProteinDetails(proteinMatch.getMainMatch());

            // create peptide header row
            createPeptideHeader();

            int proteinStartRow = currentRow;

            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, true, parameters, waitingHandler);
            PeptideMatch peptideMatch;

            // print the peptide details
            while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                // insert peptide data
                insertPeptideData(peptideMatch);

                if (waitingHandler.isRunCanceled()) {
                    break;
                }
            }

            if (waitingHandler.isRunCanceled()) {
                break;
            }

            // group the peptide rows
            sheet.groupRow(proteinStartRow, currentRow);
            sheet.setRowGroupCollapsed(proteinStartRow, true);

            waitingHandler.increasePrimaryProgressCounter();
        }
    }

    /**
     * Insert the protein details.
     *
     * @param proteinAccession the protein key
     *
     * @throws Exception thrown if an error occurs when getting the sequence
     * details
     */
    private void insertProteinDetails(String proteinAccession) throws Exception {

        HSSFRow rowHead = sheet.createRow(++currentRow);
        rowHead.setHeightInPoints(12.75f);

        Cell cell = rowHead.createCell(0);
        cell.setCellValue(proteinAccession); // protein accesion
        cell.setCellStyle(proteinRowCellStyle);

        cell = rowHead.createCell(1);
        cell.setCellValue(sequenceFactory.getHeader(proteinAccession).getSimpleProteinDescription()); // protein description
        cell.setCellStyle(proteinRowCellStyle);

        cell = rowHead.createCell(2);
        Double proteinMW = sequenceFactory.computeMolecularWeight(proteinAccession);
        cell.setCellValue(Util.roundDouble(proteinMW, 2)); // protein molecular weight
        cell.setCellStyle(proteinRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
    }

    /**
     * Inserts the peptide data.
     *
     * @param peptideMatch the peptide match
     */
    private void insertPeptideData(PeptideMatch peptideMatch) throws Exception {

        Peptide peptide = peptideMatch.getTheoreticPeptide();
        ArrayList<String> proteinAccessions = peptide.getParentProteins(identificationParameters.getSequenceMatchingPreferences());
        StringBuilder proteinAccessionsAsString = new StringBuilder();
        for (String proteinAccession : proteinAccessions) {
            if (proteinAccessionsAsString.length() > 0) {
                proteinAccessionsAsString.append(';');
            }
            proteinAccessionsAsString.append(proteinAccession);
        }
        ArrayList<String> proteinGroups = new ArrayList<String>(identification.getProteinMatches(peptide));

        ArrayList<String> spectrumKeys = peptideMatch.getSpectrumMatchesKeys();
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PsmIterator psmIterator = identification.getPsmIterator(spectrumKeys, parameters, false, waitingHandler);
        SpectrumMatch spectrumMatch;

        while ((spectrumMatch = psmIterator.next()) != null) {

            if (waitingHandler.isRunCanceled()) {
                break;
            }

            String spectrumKey = spectrumMatch.getKey();
            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

            if (spectrumMatch.getBestPeptideAssumption() != null) { // Should always be the case

                PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                peptide = peptideAssumption.getPeptide();

                int column = 1;
                HSSFRow rowHead = sheet.createRow(++currentRow);
                rowHead.setHeightInPoints(12.75f);

                Cell cell = rowHead.createCell(column++);
                MatchValidationLevel matchValidationLevel = psParameter.getMatchValidationLevel();
                // High, Medium or Low - refers to the confidence in the peptide
                if (matchValidationLevel == MatchValidationLevel.confident) {
                    cell.setCellValue("High");
                } else if (matchValidationLevel == MatchValidationLevel.doubtful) {
                    cell.setCellValue("Medium");
                } else {
                    cell.setCellValue("Low");
                }
                cell.setCellStyle(a2CellStyle);

                cell = rowHead.createCell(column++);
                cell.setCellValue(peptide.getSequenceWithLowerCasePtms()); // peptide sequence, modified residues in lower case
                cell.setCellStyle(peptideRowCellStyle);

                cell = rowHead.createCell(column++);
                cell.setCellValue(1); // number of PSMs
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                cell.setCellValue(proteinAccessions.size()); // number of proteins
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                cell.setCellValue(proteinGroups.size()); // number of protein groups
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                cell.setCellValue(proteinAccessionsAsString.toString()); // protein accessions, separated by semi colon
                cell.setCellStyle(peptideRowCellStyle);

                cell = rowHead.createCell(column++);
                cell.setCellValue(getPeptideModificationsAsString(peptide)); // the modifications, separated by semi colon _and_ space // @TODO: reformat
                cell.setCellStyle(peptideRowCellStyle);

                cell = rowHead.createCell(column++);
                Double delta = psParameter.getDeltaPEP(); // PeptideShaker closest equivalent to a delta Cn
                if (delta == null) {
                    cell.setCellValue(Double.NaN);
                    // @TODO: set another type?
                } else {
                    cell.setCellValue(delta);
                    cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                }
                cell.setCellStyle(peptideRowCellStyle);

                cell = rowHead.createCell(column++);
                cell.setCellValue(0); // PeptideShaker q-value // @TODO: insert real value
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                cell.setCellValue(psParameter.getPsmProbability()); // pep value
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                double score = psParameter.getPsmScore(); // PeptideShaker closest equivalent to an ion score
                cell.setCellValue(score);
                cell.setCellStyle(peptideRowCellStyle);
                if (score != Double.POSITIVE_INFINITY) {
                    cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                }

                cell = rowHead.createCell(column++);
                cell.setCellValue(psParameter.getPsmProbabilityScore()); // PeptideShaker closest equivalent to an e-value
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                cell.setCellValue(peptideAssumption.getIdentificationCharge().value); // charge
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                cell.setCellValue(peptideMatch.getTheoreticPeptide().getMass()
                        + ElementaryIon.proton.getTheoreticMass()); // theoretical mass for single charge: MH+ [Da]
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                Precursor precursor = spectrumFactory.getPrecursor(spectrumKey);

                cell = rowHead.createCell(column++);
                cell.setCellValue(peptideAssumption.getDeltaMass(precursor.getMz(), true, identificationParameters.getSearchParameters().getMinIsotopicCorrection(), identificationParameters.getSearchParameters().getMaxIsotopicCorrection())); // mass error in ppm
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                Double rt = precursor.getRt();
                if (rt > 0) {
                    rt /= 60;
                } else {
                    rt = Double.NaN;
                }
                cell.setCellValue(rt); // retention time in minutes
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);

                cell = rowHead.createCell(column++);
                DigestionPreferences digestionPreferences = identificationParameters.getSearchParameters().getDigestionPreferences();
                Integer nMissedCleavages = peptide.getNMissedCleavages(digestionPreferences);
                if (nMissedCleavages == null) {
                    nMissedCleavages = 0;
                }
                cell.setCellValue(nMissedCleavages); // number of missed cleavages
                cell.setCellStyle(peptideRowCellStyle);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            }
        }
    }

    /**
     * Create a peptide header.
     */
    private void createPeptideHeader() {

        HSSFRow rowHead = sheet.createRow(++currentRow);
        rowHead.setHeightInPoints(15.75f);

        int column = 1;
        Cell cell = rowHead.createCell(column++);
        cell.setCellValue("A2");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("Sequence");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("# PSMs");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("# Proteins");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("# Protein Groups");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("Protein Group Accessions");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("Modifications");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("ΔCn");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("q-Value");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("PEP");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("IonScore");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("Exp Value");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("Charge");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("MH+ [Da]");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("ΔM [ppm]");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue("RT [min]");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(column);
        cell.setCellValue("# Missed Cleavages");
        cell.setCellStyle(borderedCellStyle);
    }

    /**
     * Create the protein header.
     */
    private void createProteinHeader() {
        HSSFRow rowHead = sheet.createRow(currentRow);
        rowHead.setHeightInPoints(15.75f);

        Cell cell = rowHead.createCell(0);
        cell.setCellValue("Accession");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(1);
        cell.setCellValue("Description");
        cell.setCellStyle(borderedCellStyle);

        cell = rowHead.createCell(2);
        cell.setCellValue("MW [kDa]");
        cell.setCellStyle(borderedCellStyle);
    }

    /**
     * Set the widths of the columns.
     */
    private void setColumnWidths() {
        int column = 0;
        sheet.setColumnWidth(column++, 4500); // units of 1/256th of a character...
        sheet.setColumnWidth(column++, 10000);
        sheet.setColumnWidth(column++, 5500);
        sheet.setColumnWidth(column++, 2300);
        sheet.setColumnWidth(column++, 3000);
        sheet.setColumnWidth(column++, 3700);
        sheet.setColumnWidth(column++, 5300);
        sheet.setColumnWidth(column++, 5000);
        sheet.setColumnWidth(column++, 2000);
        sheet.setColumnWidth(column++, 2000);
        sheet.setColumnWidth(column++, 2300);
        sheet.setColumnWidth(column++, 2300);
        sheet.setColumnWidth(column++, 2300);
        sheet.setColumnWidth(column++, 2000);
        sheet.setColumnWidth(column++, 2300);
        sheet.setColumnWidth(column++, 2300);
        sheet.setColumnWidth(column++, 2300);
        sheet.setColumnWidth(column++, 4200);
    }

    /**
     * Create the cell styles.
     */
    private void createCellStyles() {

        // the font size
        Font f = workbook.createFont();
        f.setFontHeightInPoints((short) 8);

        // bordered cell style
        borderedCellStyle = workbook.createCellStyle();
        borderedCellStyle.setFont(f);
        borderedCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        borderedCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        borderedCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        borderedCellStyle.setBorderRight(CellStyle.BORDER_THIN);
        borderedCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
        borderedCellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        borderedCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        borderedCellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        // protein row cell style
        proteinRowCellStyle = workbook.createCellStyle();
        proteinRowCellStyle.setFont(f);
        proteinRowCellStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        proteinRowCellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        // peptide row cell style
        peptideRowCellStyle = workbook.createCellStyle();
        peptideRowCellStyle.setFont(f);
        peptideRowCellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        peptideRowCellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        // a2 column cell style
        a2CellStyle = workbook.createCellStyle();
        a2CellStyle.setFont(f);
        a2CellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        a2CellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        a2CellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    }

    /**
     * Returns the peptide modifications as a string. Example: M1(Oxidation);
     * C3(Carbamidomethyl).
     *
     * @param peptide the peptide
     * @return the peptide modifications as a string
     */
    public static String getPeptideModificationsAsString(Peptide peptide) {

        StringBuilder result = new StringBuilder();

        if (peptide.isModified()) {
            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                int site = modificationMatch.getModificationSite();
                String ptmName = modificationMatch.getTheoreticPtm();

                if (result.length() > 0) {
                    result.append("; ");
                }

                result.append(peptide.getSequence().charAt(site - 1));
                result.append(site);
                result.append("(");
                result.append(ptmName);
                result.append(")");
            }
        }

        return result.toString();
    }
}
