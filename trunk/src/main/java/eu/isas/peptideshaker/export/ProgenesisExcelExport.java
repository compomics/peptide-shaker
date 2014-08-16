package eu.isas.peptideshaker.export;

/**
 * This class exports identifications for post-processing with Non-Linear
 * Progenesis as Excel workbooks. Work in progress...
 *
 * @author Harald Barsnes
 */
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.ElementaryIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.waiting.WaitingHandler;
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
     * The row in the Excel file that is currently being written to.
     */
    private int currentRow = 0;
    /**
     * The current enzyme.
     */
    private Enzyme enzyme;

    /**
     * Constructor.
     *
     * @param waitingHandler the waiting handler
     * @param proteinKeys the protein keys to export
     * @param enzyme the enzyme used, needed for the missed cleavages
     * @param identification the identifications
     * @param outputFile the file to export to
     */
    public ProgenesisExcelExport(WaitingHandler waitingHandler, ArrayList<String> proteinKeys, Enzyme enzyme, Identification identification, File outputFile) {
        this.waitingHandler = waitingHandler;
        this.proteinKeys = proteinKeys;
        this.enzyme = enzyme;
        this.identification = identification;
        this.outputFile = outputFile;
    }

    /**
     * Write the data to an Excel file.
     *
     * @throws java.lang.Exception
     */
    public void writeProgenesisExcelExport() throws Exception {

        // set up the waiting handler
        waitingHandler.resetPrimaryProgressCounter();
        waitingHandler.setMaxPrimaryProgressCounter(proteinKeys.size());

        // create the workbook and sheet
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet("Sheet1");
        sheet.setRowSumsBelow(false);

        // set the column widths
        setColumnWidths();

        // create cell styles
        createCellStyles();

        // insert the protein data
        insertProteinData();

        // write the data to an excel file
        if (!waitingHandler.isRunCanceled()) {
            FileOutputStream fileOut = new FileOutputStream(outputFile);
            workbook.write(fileOut);
            fileOut.close();
        }
    }

    /**
     * Insert the protein data including the peptide details.
     */
    private void insertProteinData() throws Exception {

        // create the protein header row
        createProteinHeader();

        for (int i = 1; i < proteinKeys.size(); i++) {

            // get the protein
            String proteinKey = proteinKeys.get(i);
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

            // insert the protein details
            insertProteinDetails(proteinMatch.getMainMatch());

            // create peptide header row
            createPeptideHeader();

            // batch load the peptides
            identification.loadPeptideMatches(proteinMatch.getPeptideMatches(), null);

            int proteinStartRow = currentRow;

            // print the peptide details
            for (int j = 0; j < proteinMatch.getPeptideMatches().size(); j++) {

                String peptideKey = proteinMatch.getPeptideMatches().get(j);

                // insert peptide data
                insertPeptideData(peptideKey);

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
     * @param proteinKey
     * @throws Exception
     */
    private void insertProteinDetails(String proteinKey) throws Exception {

        HSSFRow rowHead = sheet.createRow(++currentRow);
        rowHead.setHeightInPoints(12.75f);

        Cell cell = rowHead.createCell(0);
        cell.setCellValue(proteinKey); // protein accesion
        cell.setCellStyle(proteinRowCellStyle);

        cell = rowHead.createCell(1);
        cell.setCellValue(sequenceFactory.getHeader(proteinKey).getSimpleProteinDescription()); // protein description
        cell.setCellStyle(proteinRowCellStyle);

        cell = rowHead.createCell(2);
        Double proteinMW = sequenceFactory.computeMolecularWeight(proteinKey);
        cell.setCellValue(Util.roundDouble(proteinMW, 2)); // protein molecular weight
        cell.setCellStyle(proteinRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
    }

    /**
     * Inserts the peptide data.
     *
     * @param peptideKey
     */
    private void insertPeptideData(String peptideKey) throws Exception {

        int column = 1;
        HSSFRow rowHead = sheet.createRow(++currentRow);
        rowHead.setHeightInPoints(12.75f);

        Cell cell = rowHead.createCell(column++);
        cell.setCellValue("High"); // High, Medium or Low - refers to the confidence in the peptide // @TODO: figure out how to set this value?
        cell.setCellStyle(a2CellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue(identification.getPeptideMatch(peptideKey).getTheoreticPeptide().getSequenceWithLowerCasePtms()); // peptide sequence, modified residues in lower case
        cell.setCellStyle(peptideRowCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue(identification.getPeptideMatch(peptideKey).getSpectrumCount()); // number of PSMs
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(1); // number of proteins // @TODO: insert real value
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(1); // number of protein groups // @TODO: insert real value
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue("P02768"); // protein group accessions, separated by semi colon // @TODO: insert real value
        cell.setCellStyle(peptideRowCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue(getPeptideModificationsAsString(identification.getPeptideMatch(peptideKey).getTheoreticPeptide())); // the modifications, separated by semi colon _and_ space // @TODO: reformat
        cell.setCellStyle(peptideRowCellStyle);

        cell = rowHead.createCell(column++);
        cell.setCellValue(0.0000); // no idea what this is... // @TODO: insert real value
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(0); // q-value // @TODO: insert real value
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(4.948E-16); // pep value // @TODO: insert real value
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(137); // ion score // @TODO: insert real value
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(7.83842E-14); // e-value // @TODO: insert real value
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(2); // charge // @TODO: how to link the charge to a peptide when the psms can have more than one?
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(identification.getPeptideMatch(peptideKey).getTheoreticPeptide().getMass()
                + ElementaryIon.proton.getTheoreticMass()); // theoretical mass for single charge: MH+ [Da]
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(-1.63); // mass error in ppm // @TODO: how to link the mass error to a peptide when the psms can have different errors?
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(97.32); // retention time in minutes // @TODO: how to link the RT to a peptide when the psms can have different RTs?
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);

        cell = rowHead.createCell(column++);
        cell.setCellValue(identification.getPeptideMatch(peptideKey).getTheoreticPeptide().getNMissedCleavages(enzyme)); // number of missed cleavages
        cell.setCellStyle(peptideRowCellStyle);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
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

        return result.toString();
    }
}
