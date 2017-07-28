package eu.isas.peptideshaker.export;

import com.compomics.util.io.export.WorkbookStyle;
import com.compomics.util.io.export.writers.ExcelWriter;
import java.util.HashMap;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

/**
 * This class contains the style for a PeptideShaker excel export.
 *
 * @author Marc Vaudel
 */
public class PsExportStyle implements WorkbookStyle {

    /**
     * Workbook.
     */
    private HSSFWorkbook workbook;
    /**
     * The implemented cell styles.
     */
    private CellStyle mainTitle, standardHeader, standard;
    /**
     * Map of the cell styles according to the hierarchical depth.
     */
    private HashMap<Integer, CellStyle> hierarchicalStyles = new HashMap<>();
    /**
     * Map of the header styles according to the hierarchical depth.
     */
    private HashMap<Integer, CellStyle> hierarchicalHeaders = new HashMap<>();
    /**
     * Map of the different styles available.
     */
    private final static HashMap<HSSFWorkbook, PsExportStyle> styles = new HashMap<HSSFWorkbook, PsExportStyle>();

    /**
     * Returns the style attached to that writer or create a new one if none
     * found.
     *
     * @param excelWriter the writer of interest
     *
     * @return the style attached to that writer
     */
    public static PsExportStyle getReportStyle(ExcelWriter excelWriter) {
        HSSFWorkbook workbook = excelWriter.getWorkbook();
        PsExportStyle result = styles.get(workbook);
        if (result == null) {
            result = new PsExportStyle(excelWriter);
            styles.put(workbook, result);
        }
        return result;
    }

    /**
     * Constructor.
     *
     * @param excelWriter the excel writer for this style
     */
    private PsExportStyle(ExcelWriter excelWriter) { //@TODO: possible to make a generic style workbook independent?
        this.workbook = excelWriter.getWorkbook();
        setCellStyles();
    }

    /**
     * Sets the cell styles.
     */
    private void setCellStyles() {

        // Main title
        Font f = workbook.createFont();
        f.setFontHeightInPoints((short) 20);
        mainTitle = workbook.createCellStyle();
        mainTitle.setFont(f);

        // Standard Cell
        f = workbook.createFont();
        f.setFontHeightInPoints((short) 8);
        standard = workbook.createCellStyle();
        standard.setFont(f);

        // Standard Header
        f = workbook.createFont();
        f.setFontHeightInPoints((short) 8);
        standardHeader = workbook.createCellStyle();
        standardHeader.setFont(f);
        standardHeader.setBorderBottom(CellStyle.BORDER_THIN);
        standardHeader.setBorderTop(CellStyle.BORDER_THIN);
        standardHeader.setBorderLeft(CellStyle.BORDER_THIN);
        standardHeader.setBorderRight(CellStyle.BORDER_THIN);
        standardHeader.setAlignment(CellStyle.ALIGN_CENTER);
        standardHeader.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        standardHeader.setFillForegroundColor(HSSFColor.PALE_BLUE.index);
        standardHeader.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

        // Change PALE_BLUE to actually pale blue
        HSSFPalette palette = workbook.getCustomPalette();
        palette.setColorAtIndex(HSSFColor.PALE_BLUE.index,
                (byte) 200,
                (byte) 200,
                (byte) 250
        );

        // Hierarchical headers
        hierarchicalHeaders.put(0, standardHeader);

        CellStyle subHeader = workbook.createCellStyle();
        subHeader.setFont(f);
        subHeader.setBorderBottom(CellStyle.BORDER_THIN);
        subHeader.setBorderTop(CellStyle.BORDER_THIN);
        subHeader.setBorderLeft(CellStyle.BORDER_THIN);
        subHeader.setBorderRight(CellStyle.BORDER_THIN);
        subHeader.setAlignment(CellStyle.ALIGN_CENTER);
        subHeader.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        subHeader.setFillForegroundColor(HSSFColor.GREY_50_PERCENT.index);
        palette.setColorAtIndex(HSSFColor.GREY_50_PERCENT.index,
                (byte) 220,
                (byte) 220,
                (byte) 250
        );
        subHeader.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        hierarchicalHeaders.put(1, subHeader);

        subHeader = workbook.createCellStyle();
        subHeader.setFont(f);
        subHeader.setBorderBottom(CellStyle.BORDER_THIN);
        subHeader.setBorderTop(CellStyle.BORDER_THIN);
        subHeader.setBorderLeft(CellStyle.BORDER_THIN);
        subHeader.setBorderRight(CellStyle.BORDER_THIN);
        subHeader.setAlignment(CellStyle.ALIGN_CENTER);
        subHeader.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        subHeader.setFillForegroundColor(HSSFColor.GREY_40_PERCENT.index);
        palette.setColorAtIndex(HSSFColor.GREY_40_PERCENT.index,
                (byte) 230,
                (byte) 230,
                (byte) 250
        );
        subHeader.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        hierarchicalHeaders.put(2, subHeader);

        subHeader = workbook.createCellStyle();
        subHeader.setFont(f);
        subHeader.setBorderBottom(CellStyle.BORDER_THIN);
        subHeader.setBorderTop(CellStyle.BORDER_THIN);
        subHeader.setBorderLeft(CellStyle.BORDER_THIN);
        subHeader.setBorderRight(CellStyle.BORDER_THIN);
        subHeader.setAlignment(CellStyle.ALIGN_CENTER);
        subHeader.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        subHeader.setFillForegroundColor(HSSFColor.GREY_25_PERCENT.index);
        palette.setColorAtIndex(HSSFColor.GREY_25_PERCENT.index,
                (byte) 240,
                (byte) 240,
                (byte) 250
        );
        subHeader.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        for (int i = 3; i < 100; i++) {
            hierarchicalHeaders.put(i, subHeader);
        }
    }

    @Override
    public CellStyle getMainTitleStyle() {
        return mainTitle;
    }

    @Override
    public float getMainTitleRowHeight() {
        return 26f;
    }

    @Override
    public CellStyle getStandardStyle() {
        return standard;
    }

    @Override
    public CellStyle getStandardStyle(int hierarchicalDepth) {
        CellStyle cellStyle = hierarchicalStyles.get(hierarchicalDepth);
        if (cellStyle == null) {
            cellStyle = standard;
        }
        return cellStyle;
    }

    @Override
    public float getStandardHeight() {
        return 12.75f;
    }

    @Override
    public CellStyle getHeaderStyle() {
        return standardHeader;
    }

    @Override
    public CellStyle getHeaderStyle(int hierarchicalDepth) {
        CellStyle cellStyle = hierarchicalHeaders.get(hierarchicalDepth);
        if (cellStyle == null) {
            cellStyle = standardHeader;
        }
        return cellStyle;
    }

    @Override
    public float getHeaderHeight() {
        return 12.75f;
    }
}
