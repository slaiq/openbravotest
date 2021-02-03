package sa.elm.ob.finance.reports.MonthlyClosingReport;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
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
import org.codehaus.jettison.json.JSONObject;

import sa.elm.ob.finance.reports.Style.ExcelStyle;

/**
 * @author Gopalakrishnan created on 28/12/2016
 * 
 */

@SuppressWarnings("deprecation")
public class MonthlyClosingReportSheet {
  private static Logger log4j = Logger.getLogger(MonthlyClosingReportSheet.class);

  public static File downloadMonthlyClosingYTD(String filePath, String clientId, String orgId,
      String userId, String lang, JSONObject jsonInput, List<HashMap<String, String>> hashmaplist,
      BigDecimal openingBalance) {
    File file = null;
    try {
      int totalCol = 5;
      Double totalCredit = 0.00;
      Double totalDebit = 0.00;
      Double openingCredit = 0.00;
      Double openingDebit = 0.00;

      file = new File(filePath);
      file.getParentFile().mkdirs();
      if (file.exists()) {
        file.delete();
        file.createNewFile();
      }
      HSSFWorkbook workbook = new HSSFWorkbook();
      HSSFSheet sheet = null;
      Row row = null;
      Cell cell = null;
      HSSFCellStyle style = workbook.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      HSSFFont font = workbook.createFont();
      style.setFont(font);

      log4j.debug("hashmaplist:" + hashmaplist.toString());
      sheet = workbook.createSheet("YTD Trial Balance");
      // Row 1
      row = sheet.createRow((short) 0);
      row.setHeight((short) 600);
      cell = row.createCell((short) 1);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 18, HSSFColor.BROWN.index));
      cell.setCellValue(" YTD NET Balanceكشف التوازن ");
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 4));
      // row2
      row = sheet.createRow((short) 1);
      row.setHeight((short) 600);
      cell = row.createCell((short) 1);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue(" CRدائـــــــــــــــــــــن");
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("البيان");
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("التصنيف الإقتصادي");
      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("مديــــــــــــنDR");

      // iterate hashMap List
      HSSFCellStyle styleCredit = workbook.createCellStyle();
      HSSFCellStyle creditStyle = ExcelStyle.amountCalibriFont(workbook, styleCredit);
      HSSFCellStyle styleDescHeader = workbook.createCellStyle();
      HSSFCellStyle headerStyle = ExcelStyle.headerfontColorHeader(workbook, styleDescHeader,
          (short) 12, HSSFColor.RED.index);
      HSSFCellStyle styleDescBreak = workbook.createCellStyle();
      HSSFCellStyle breakStyle = ExcelStyle.headerfontColorBreakDown(workbook, styleDescBreak,
          (short) 12, HSSFColor.TURQUOISE.index);
      HSSFCellStyle styleDescAccount = workbook.createCellStyle();
      HSSFCellStyle AccountStyle = ExcelStyle.headerfontColorAccount(workbook, styleDescAccount,
          (short) 10, HSSFColor.LIGHT_BLUE.index);
      HSSFCellStyle styleDescSubAct = workbook.createCellStyle();
      HSSFCellStyle subAcctStyle = ExcelStyle.headerfontColorSubAccount(workbook, styleDescSubAct,
          (short) 8, HSSFColor.BLACK.index);
      int i = 2;
      for (HashMap<String, String> map : hashmaplist) {
        row = sheet.createRow(i);
        row.setHeight((short) 400);
        // cell credit
        cell = row.createCell(1);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        if (map.get("qty") != null) {
          if (Double.valueOf(map.get("qty")) < 0) {
            cell.setCellValue(Math.abs(Double.valueOf(map.get("qty"))));
            if (map.get("level").equals("S")) {
              totalCredit = totalCredit + Double.valueOf(map.get("qty"));
            }

          } else {
            cell.setCellValue(Integer.valueOf("0"));
          }
        } else {
          cell.setCellValue(Integer.valueOf("0"));
        }

        cell.setCellStyle(creditStyle);
        // cell Description
        cell = row.createCell(2);
        cell.setCellType(Cell.CELL_TYPE_STRING);
        cell.setCellValue(map.get("name") + "");
        if (map.get("level").equals("E")) {
          cell.setCellStyle(headerStyle);
        } else if (map.get("level").equals("C")) {
          cell.setCellStyle(breakStyle);
        } else if (map.get("level").equals("D")) {
          cell.setCellStyle(AccountStyle);
        } else {
          cell.setCellStyle(subAcctStyle);
        }

        // cell account
        cell = row.createCell(3);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        cell.setCellValue(new Double(map.get("account")));
        if (map.get("level").equals("E")) {
          cell.setCellStyle(headerStyle);
        } else if (map.get("level").equals("C")) {
          cell.setCellStyle(breakStyle);
        } else if (map.get("level").equals("D")) {
          cell.setCellStyle(AccountStyle);
        } else {
          cell.setCellStyle(subAcctStyle);
        }

        // cell debit
        cell = row.createCell(4);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        if (map.get("qty") != null) {
          if (Double.valueOf(map.get("qty")) > 0) {
            cell.setCellValue(Math.abs(Double.valueOf(map.get("qty"))));
            if (map.get("level").equals("S")) {
              totalDebit = totalDebit + Double.valueOf(map.get("qty"));
            }
          } else {
            cell.setCellValue(Integer.valueOf("0"));
          }
        } else {
          cell.setCellValue(Integer.valueOf("0"));
        }
        cell.setCellStyle(creditStyle);
        i++;
      }
      // Total row
      row = sheet.createRow(i + 1);
      row.setHeight((short) 400);
      // total credit
      cell = row.createCell((short) 1);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BROWN.index, HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue(Math.abs(totalCredit));
      //
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 11, HSSFColor.BLACK.index));
      cell.setCellValue("المجموع");
      //
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      // total debit
      cell = row.createCell((short) 4);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BROWN.index, HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue(Math.abs(totalDebit));

      cell = row.createCell((short) 5);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BROWN.index));

      // opening Balance Row
      row = sheet.createRow(i + 2);
      row.setHeight((short) 400);
      // credit opening
      cell = row.createCell((short) 1);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      if (openingBalance.compareTo(BigDecimal.ZERO) < 0) {
        openingCredit = openingBalance.doubleValue();
        cell.setCellValue(Math.abs(openingBalance.doubleValue()));
      } else {
        cell.setCellValue(Integer.valueOf("0"));
      }
      //
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("ناقصاً الأرصدة المدورة من السنة المالية الماضية");
      //
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("Year Opening Balance");
      // debit opening
      cell = row.createCell((short) 4);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      if (openingBalance.compareTo(BigDecimal.ZERO) > 0) {
        cell.setCellValue(Math.abs(openingBalance.doubleValue()));
        openingDebit = openingBalance.doubleValue();
      } else {
        cell.setCellValue(Integer.valueOf("0"));
      }
      //
      cell = row.createCell((short) 5);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BROWN.index));

      // final Total row
      row = sheet.createRow(i + 3);
      row.setHeight((short) 400);
      // final total credit
      cell = row.createCell((short) 1);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BROWN.index, HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue(Math.abs(totalCredit - openingCredit));
      //
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 10, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("الجمـــــــــــــــــــــــــــــــــــــــــــــــلة");
      //
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 10, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      // final total debit
      cell = row.createCell((short) 4);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BROWN.index, HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue(Math.abs(totalDebit - openingDebit));
      //
      cell = row.createCell((short) 5);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BROWN.index));
      row = sheet.createRow(i + 5);
      row.setHeight((short) 600);
      cell = row.createCell((short) 1);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 12, HSSFColor.RED.index));
      cell.setCellValue("في هذا الكشف تقوم الجهة بإدراج الحسابات التي تخصها فقط وفقاً للدليل .");
      sheet.addMergedRegion(new CellRangeAddress(i + 5, i + 5, 1, 4));
      // summary row
      row = sheet.createRow(i + 7);
      row.setHeight((short) 600);
      cell = row.createCell((short) 1);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("صاحب الصلاحية");
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("مدير الإدارة المالية                                 المراقب المالي");
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("رئيس المحاسبة ");
      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("الموظف المختص");
      // final row
      row = sheet.createRow(i + 8);
      row.setHeight((short) 600);
      cell = row.createCell((short) 1);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("الإسم :");
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("الإسم :                                               الإسم :");
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("الإسم :");
      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 10, HSSFColor.BLACK.index));
      cell.setCellValue("الإسم :");
      // Set Column Width
      for (int j = 0; j < totalCol; j++) {
        if (j == 0)
          sheet.setColumnWidth(j, 25 * 100);
        if (j == 2)
          sheet.setColumnWidth(j, 150 * 100);
        else
          sheet.setColumnWidth(j, 60 * 100);
      }

      // sheet row height
      sheet.setDefaultRowHeight((short) 400);

      // Sheet Protection
      sheet.setDisplayFormulas(false);

      FileOutputStream fileOut = new FileOutputStream(filePath);
      workbook.write(fileOut);
      fileOut.close();
    } catch (final Exception e) {
      log4j.error("Exception in MonthlyClosingReportSheet() : ", e);
    }
    return file;
  }
}
