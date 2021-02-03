package sa.elm.ob.finance.ad_process.ExportBudget.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XSSFExcelStyles {
  public Map<String, XSSFCellStyle> createStyles(XSSFWorkbook wb) {
    Map<String, XSSFCellStyle> styles = new HashMap<String, XSSFCellStyle>();
    XSSFCellStyle style = wb.createCellStyle();
    XSSFDataFormat fmt = wb.createDataFormat();
    XSSFFont headerFont = wb.createFont();
    headerFont.setBoldweight((short) 50000);

    style = wb.createCellStyle();
    style.setFont(headerFont);
    style.setLocked(true);
    style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
    styles.put("Header", style);

    style = wb.createCellStyle();
    style.setFont(headerFont);
    style.setLocked(true);
    styles.put("BoldTextLock", style);

    style = wb.createCellStyle();
    style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
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
    style.setAlignment(XSSFCellStyle.ALIGN_LEFT);
    styles.put("CommonFields", style);

    style = wb.createCellStyle();
    XSSFDataFormat xssfDataFormat = wb.createDataFormat();
    style.setDataFormat(xssfDataFormat.getFormat("#,##0.000"));
    styles.put("DecimalFormat", style);

    style = wb.createCellStyle();
    style.setFont(headerFont);
    style.setLocked(true);
    style.setAlignment(XSSFCellStyle.ALIGN_RIGHT);
    styles.put("NumericLock", style);

    style = wb.createCellStyle();
    style.setFont(headerFont);
    style.setLocked(false);
    style.setAlignment(XSSFCellStyle.ALIGN_RIGHT);
    styles.put("NumericUnlock", style);

    style = wb.createCellStyle();
    style.setLocked(false);
    style.setDataFormat(fmt.getFormat("@"));
    styles.put("TextUnlockFormat", style);

    return styles;
  }
}