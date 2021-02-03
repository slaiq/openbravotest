package sa.elm.ob.finance.ad_process.ExportBudget.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class ExcelStyles {
  public Map<String, HSSFCellStyle> createStyles(HSSFWorkbook wb) {
    Map<String, HSSFCellStyle> styles = new HashMap<String, HSSFCellStyle>();
    HSSFCellStyle style = wb.createCellStyle();
    HSSFFont headerFont = wb.createFont();
    headerFont.setBoldweight((short) 50000);

    style = wb.createCellStyle();
    style.setFont(headerFont);
    style.setLocked(true);
    style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
    styles.put("Header", style);

    style = wb.createCellStyle();
    style.setFont(headerFont);
    style.setLocked(true);
    styles.put("BoldTextLock", style);

    style = wb.createCellStyle();
    style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
    style.setLocked(true);
    styles.put("RowNumber", style);

    style = wb.createCellStyle();
    style.setLocked(true);
    styles.put("CellLock", style);

    style = wb.createCellStyle();
    style.setLocked(false);
    styles.put("CellUnlock", style);

    style = wb.createCellStyle();
    style.setWrapText(true);
    style.setLocked(true);
    styles.put("TextLock", style);

    style = wb.createCellStyle();
    style.setLocked(false);
    styles.put("TextUnlock", style);

    style = wb.createCellStyle();
    style.setFont(headerFont);
    style.setLocked(true);
    style.setAlignment(HSSFCellStyle.ALIGN_LEFT);
    styles.put("CommonFields", style);

    style = wb.createCellStyle();
    HSSFDataFormat hssfDataFormat = wb.createDataFormat();
    style.setDataFormat(hssfDataFormat.getFormat("#,##0.000"));
    styles.put("DecimalFormat", style);

    return styles;
  }
}