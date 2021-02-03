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
 *         This one is the class in charge of the report of Expense Summary Details Report
 */

@SuppressWarnings("deprecation")
public class ExpenseSummary {
  private static Logger log4j = Logger.getLogger(ExpenseSummary.class);

  public static File downloadExpenseSummary(File excelFile,
      List<HashMap<String, String>> hashmaplist) {
    File file = excelFile;
    String strFilePath = file.getPath();
    try {
      int totalCol = 10;
      Double totalBalance = 0.00;
      HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
      HSSFSheet expenseSummarySheet = null;
      Row row = null;
      Cell cell = null;
      HSSFCellStyle style = workbook.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      HSSFFont font = workbook.createFont();
      style.setFont(font);
      log4j.debug("expenseSList Size:" + hashmaplist.size());
      expenseSummarySheet = workbook.createSheet("Expense Summary");
      // Row 3
      row = expenseSummarySheet.createRow((short) 2);
      row.setHeight((short) 600);
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 18, HSSFColor.BROWN.index));
      cell.setCellValue("خلاصة المصروفات والمعاملات على الأصول غير المالية");
      expenseSummarySheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 4));
      // row 4
      row = expenseSummarySheet.createRow((short) 3);
      row.setHeight((short) 600);
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("المبلغ");

      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("اسم البند");

      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("التصنيف الاقتصادي");

      // Iterate list
      HSSFCellStyle styleCredit = workbook.createCellStyle();
      HSSFCellStyle creditStyle = ExcelStyle.amountCalibriFont(workbook, styleCredit);
      HSSFCellStyle styleDescAccount = workbook.createCellStyle();
      HSSFCellStyle AccountStyle = ExcelStyle.headerfontColorAccount(workbook, styleDescAccount,
          (short) 10, HSSFColor.BLACK.index);
      int i = 4;
      for (HashMap<String, String> map : hashmaplist) {
        // only for
        // for BreakDown accounts
        if (map.get("level").equals("D")) {
          Double quantity = Double.valueOf(map.get("qty") == null ? "0" : map.get("qty"));
          // qty--YTD month Balance
          row = expenseSummarySheet.createRow(i);
          row.setHeight((short) 400);
          // YTD Balance
          cell = row.createCell(2);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          if (quantity != null) {
            cell.setCellValue(quantity);
            totalBalance = totalBalance + quantity;
          } else {
            cell.setCellValue(Double.valueOf("0"));
          }
          cell.setCellStyle(creditStyle);

          // Account Description
          cell = row.createCell(3);
          cell.setCellType(Cell.CELL_TYPE_STRING);
          cell.setCellValue(map.get("name"));
          cell.setCellStyle(AccountStyle);
          // Project is empty
          cell = row.createCell(4);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          cell.setCellValue(new Double(map.get("account")));
          cell.setCellStyle(AccountStyle);
          i++;
        }
      }
      HSSFCellStyle bottomStyle = ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BLACK.index, HSSFColor.GREY_25_PERCENT.index);
      row = expenseSummarySheet.createRow(i + 1);
      row.setHeight((short) 600);
      // Total Balance
      cell = row.createCell(2);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellValue(totalBalance);
      cell.setCellStyle(bottomStyle);
      //
      cell = row.createCell(3);
      cell.setCellStyle(bottomStyle);
      cell.setCellValue("إجمالي النفقات");
      expenseSummarySheet.addMergedRegion(new CellRangeAddress(i + 1, i + 1, 3, 4));

      // Set Column Width
      for (int j = 0; j < totalCol; j++) {
        if ((j == 2) || (j == 3) || (j == 4)) {
          expenseSummarySheet.setColumnWidth(j, 150 * 100);
        } else {
          expenseSummarySheet.setColumnWidth(j, 60 * 100);
        }
      }
      // sheet row height
      expenseSummarySheet.setDefaultRowHeight((short) 400);

      FileOutputStream fileOut = new FileOutputStream(strFilePath);
      workbook.write(fileOut);
      fileOut.close();
    } catch (final Exception e) {
      log4j.error("Exception in Expense Summary Sheet : ", e);
    }
    return file;
  }
}
