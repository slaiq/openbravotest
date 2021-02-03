package sa.elm.ob.finance.reports.MonthlyClosingReport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

import sa.elm.ob.finance.reports.Style.ExcelStyle;

/**
 * @author Gopalakrishnan created on 18/01/2017
 * 
 *         This one is the class in charge of the report of Revnue Account
 */

@SuppressWarnings("deprecation")
public class RevenueSheet {
  private static Logger log4j = Logger.getLogger(RevenueSheet.class);

  public static File downloadRevenueSheet(File excelFile, List<HashMap<String, String>> hashmaplist) {
    File file = excelFile;
    String strFilePath = file.getPath();
    try {
      int totalCol = 12;
      Double totalBalance = 0.00;
      Double totalActualBalance = 0.00;
      Double totalPrevBalance = 0.00;
      HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
      HSSFSheet revenueSheet = null;
      Row row = null;
      Cell cell = null;
      HSSFCellStyle style = workbook.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      HSSFFont font = workbook.createFont();
      style.setFont(font);
      log4j.debug("expenseSheetList:" + hashmaplist.toString());

      revenueSheet = workbook.createSheet("Revenue");
      // Row 3
      HSSFCellStyle headerStyle = ExcelStyle.headerfontColor(workbook, (short) 14,
          HSSFColor.BROWN.index);
      row = revenueSheet.createRow((short) 2);
      row.setHeight((short) 400);
      cell = row.createCell((short) 7);
      cell.setCellStyle(headerStyle);
      cell.setCellValue("الإيرادات");
      // row 4
      row = revenueSheet.createRow((short) 3);
      row.setHeight((short) 400);
      // row 5
      row = revenueSheet.createRow((short) 4);
      row.setHeight((short) 600);
      cell = row.createCell((short) 7);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("التصنيف الإقتصادي");

      cell = row.createCell((short) 6);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("اسم البند");

      cell = row.createCell((short) 5);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("Forecast (manual)");

      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("إيرادات الشهر الحالي");

      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("إيرادات الأشهر الماضية");
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("الجملة");

      // Iterate list
      HSSFCellStyle styleCredit = workbook.createCellStyle();
      HSSFCellStyle creditStyle = ExcelStyle.amountCalibriFont(workbook, styleCredit);
      int i = 5;
      for (HashMap<String, String> map : hashmaplist) {
        // only for
        // BreakDown accounts
        if (map.get("level").equals("S")) {
          // for calculating final sum
          Double actualBalance = Double.valueOf(map.get("qty") == null ? "0" : map.get("qty"));
          Double prevBalance = Double.valueOf(map.get("qtyRef") == null ? "0" : map.get("qtyRef"));
          totalBalance = totalBalance + actualBalance + prevBalance;
          totalPrevBalance = totalPrevBalance + prevBalance;
          totalActualBalance = totalActualBalance + actualBalance;
        }
        if (map.get("level").equals("D")) {
          Double actualBalance = Double.valueOf(map.get("qty") == null ? "0" : map.get("qty"));
          Double prevBalance = Double.valueOf(map.get("qtyRef") == null ? "0" : map.get("qtyRef"));
          // qty--Actual month Balance
          // qtyref - previous Month Balance
          row = revenueSheet.createRow(i);
          row.setHeight((short) 400);
          // Total Balance
          cell = row.createCell(2);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          if (actualBalance != null && prevBalance != null) {
            cell.setCellValue(actualBalance + prevBalance);
          } else {
            cell.setCellValue(Double.valueOf("0"));
          }
          cell.setCellStyle(creditStyle);
          // opening Balance
          cell = row.createCell(3);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          if (prevBalance != null) {
            cell.setCellValue(prevBalance);
          } else {
            cell.setCellValue(Double.valueOf("0"));
          }
          cell.setCellStyle(creditStyle);
          // Actual Transaction
          cell = row.createCell(4);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          if (actualBalance != null) {
            cell.setCellValue(actualBalance);
          } else {
            cell.setCellValue(Double.valueOf("0"));
          }
          cell.setCellStyle(creditStyle);
          // Name
          cell = row.createCell(5);
          cell.setCellType(Cell.CELL_TYPE_STRING);
          // Name
          cell = row.createCell(6);
          cell.setCellType(Cell.CELL_TYPE_STRING);
          cell.setCellValue(map.get("name"));
          cell.setCellStyle(creditStyle);
          // Project column value as "Act Search key"
          cell = row.createCell(7);
          cell.setCellType(Cell.CELL_TYPE_STRING);
          cell.setCellValue(map.get("account"));
          cell.setCellStyle(creditStyle);
          i++;
        }
      }
      HSSFCellStyle bottomStyle = ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BLACK.index, HSSFColor.GREY_25_PERCENT.index);
      row = revenueSheet.createRow(i + 1);
      row.setHeight((short) 600);
      // Total Balance
      cell = row.createCell(2);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellValue(totalBalance);
      cell.setCellStyle(bottomStyle);
      // Total opening Balance
      cell = row.createCell(3);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellValue(totalPrevBalance);
      cell.setCellStyle(bottomStyle);
      // Total Actual Transaction Balance
      cell = row.createCell(4);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellValue(totalActualBalance);
      cell.setCellStyle(bottomStyle);

      cell = row.createCell(5);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(bottomStyle);

      cell = row.createCell(6);
      cell.setCellStyle(bottomStyle);
      cell.setCellValue("جملة الإيرادات");
      revenueSheet.addMergedRegion(new CellRangeAddress(i + 1, i + 1, 6, 7));

      // summary
      row = revenueSheet.createRow(i + 2);
      row.setHeight((short) 600);
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 11, HSSFColor.BROWN.index));
      cell.setCellValue("خلاصة المصروفات والمعاملات على الأصول غير المالية");
      revenueSheet.addMergedRegion(new CellRangeAddress(i + 2, i + 2, 2, 7));

      //
      cell = row.createCell(7);
      cell.setCellStyle(bottomStyle);
      // Set Column Width
      for (int j = 0; j < totalCol; j++) {
        if (j == 0) {
          revenueSheet.setColumnWidth(j, 40 * 100);
        } else if (j == 1) {
          revenueSheet.setColumnWidth(j, 40 * 100);
        } else if (j == 6) {
          revenueSheet.setColumnWidth(j, 120 * 100);
        } else {
          revenueSheet.setColumnWidth(j, 100 * 100);
        }

      }

      // sheet row height
      revenueSheet.setDefaultRowHeight((short) 400);

      FileOutputStream fileOut = new FileOutputStream(strFilePath);
      workbook.write(fileOut);
      fileOut.close();
    } catch (final Exception e) {
      log4j.error("Exception in Revenue Sheet : ", e);
    }
    return file;
  }
}
