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
 * @author Gopalakrishnan created on 17/01/2017
 * 
 *         This one is the class in charge of the report of Current Month YTD Summary
 */

@SuppressWarnings("deprecation")
public class MonthlyCurrentMonthYTD {
  private static Logger log4j = Logger.getLogger(MonthlyCurrentMonthYTD.class);

  public static File downloadCurrentMonthYTD(File excelFile,
      List<HashMap<String, String>> hashmaplist) {
    File file = excelFile;
    String strFilePath = file.getPath();
    try {
      int totalCol = 5;
      Double totalCredit = 0.00;
      Double totalDebit = 0.00;

      HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
      HSSFSheet sheet = null;
      Row row = null;
      Cell cell = null;
      HSSFCellStyle style = workbook.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      HSSFFont font = workbook.createFont();
      style.setFont(font);

      log4j.debug("hashmaplist:" + hashmaplist.toString());
      sheet = workbook.createSheet("DR CR Details of Current Month YTD");
      // Row 1
      row = sheet.createRow((short) 0);
      row.setHeight((short) 600);
      cell = row.createCell((short) 1);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 18, HSSFColor.BROWN.index));
      cell.setCellValue(" (Current Month - YTD)المصروفات والإيرادات وحسابات التسوية");
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 6));
      // row2
      row = sheet.createRow((short) 1);
      row.setHeight((short) 400);
      cell = row.createCell((short) 1);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("CRدائــــــــــــــــــــــــــــن");
      sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 2));
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("/Descالبيان");
      sheet.addMergedRegion(new CellRangeAddress(1, 2, 3, 3));
      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("التصنيف الإقتصادي/MoF Account");
      sheet.addMergedRegion(new CellRangeAddress(1, 2, 4, 4));
      cell = row.createCell((short) 5);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("مديـــــــــــــــــــــــــــنDR");
      sheet.addMergedRegion(new CellRangeAddress(1, 1, 5, 6));

      // row 3
      row = sheet.createRow((short) 2);
      row.setHeight((short) 400);
      cell = row.createCell((short) 1);
      cell.setCellValue("الجمـــــــلة");
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell = row.createCell((short) 2);
      cell.setCellValue("المفردات");
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell = row.createCell((short) 5);
      cell.setCellValue("الجمـــــــلة");
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell = row.createCell((short) 6);
      cell.setCellValue("المفردات");
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));

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
      int i = 3;
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
        cell = row.createCell(3);
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
        cell = row.createCell(4);
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
        cell = row.createCell(6);
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

      // final Total row
      row = sheet.createRow(i);
      row.setHeight((short) 400);
      // final total credit
      cell = row.createCell((short) 1);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BROWN.index, HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue(Math.abs(totalCredit));
      //
      cell = row.createCell((short) 2);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BROWN.index, HSSFColor.GREY_25_PERCENT.index));
      //
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 10, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("الجمـــــــــــــــــــــــــــــــــــــــــــــــلة");
      //
      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 10, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      //
      cell = row.createCell((short) 5);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BROWN.index, HSSFColor.GREY_25_PERCENT.index));
      // final total debit
      cell = row.createCell((short) 6);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellStyle(ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BROWN.index, HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue(totalDebit);

      row = sheet.createRow(i + 1);
      row.setHeight((short) 600);
      cell = row.createCell((short) 1);
      cell.setCellStyle(ExcelStyle.headerfontColor(workbook, (short) 12, HSSFColor.RED.index));
      cell.setCellValue("في هذا الكشف تقوم الجهة  بإدراج الحسابات التي تخصها فقط وفقاً للدليل .");
      sheet.addMergedRegion(new CellRangeAddress(i + 1, i + 1, 1, 4));

      // Set Column Width
      for (int j = 0; j < totalCol; j++) {
        if (j == 3) {
          sheet.setColumnWidth(j, 150 * 100);
        } else {
          sheet.setColumnWidth(j, 60 * 100);
        }
      }

      // sheet row height
      sheet.setDefaultRowHeight((short) 400);

      // Sheet Protection
      sheet.setDisplayFormulas(false);

      FileOutputStream fileOut = new FileOutputStream(strFilePath);
      workbook.write(fileOut);
      fileOut.close();
    } catch (final Exception e) {
      log4j.error("Exception in MonthlyCurrentMonthYTD() : ", e);
    }
    return file;
  }
}
