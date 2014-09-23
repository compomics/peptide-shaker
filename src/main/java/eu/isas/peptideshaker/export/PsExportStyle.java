package eu.isas.peptideshaker.export;

import com.compomics.util.io.export.WorkbookStyle;
import com.compomics.util.io.export.writers.ExcelWriter;
import java.util.HashMap;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;

/**
 * This class contains the style for a PeptideShaker excel export.
 *
 * @author Marc
 */
public class PsExportStyle implements WorkbookStyle {

    /**
     * Workbook
     */
    private HSSFWorkbook workbook;
    /**
     * The implemented cell styles
     */
    private CellStyle mainTitle, standardHeader, standard;
    /**
     * Map of the cell styles according to the hierarchical depth
     */
    private HashMap<Integer, CellStyle> hierarchicalStyles = new HashMap<Integer, CellStyle>();
    /**
     * Map of the header styles according to the hierarchical depth
     */
    private HashMap<Integer, CellStyle> hierarchicalHeaders = new HashMap<Integer, CellStyle>();
    /**
     * Constructor
     * 
     * @param excelWriter the excel writer for this style
     */
    public PsExportStyle(ExcelWriter excelWriter) { //@TODO: possible to make a generic style workbook independent?
        this.workbook = excelWriter.getWorkbook();
    }
    
    private void setCellStyles() {
        
// Main title
        Font f = workbook.createFont();
        f.setFontHeightInPoints((short) 20);
        mainTitle = workbook.createCellStyle();
        mainTitle.setFont(f);
        
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
        standardHeader.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
        standardHeader.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        
        // Standard Cell
        f = workbook.createFont();
        f.setFontHeightInPoints((short) 8);
        standard = workbook.createCellStyle();
        standard.setFont(f);
        
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
