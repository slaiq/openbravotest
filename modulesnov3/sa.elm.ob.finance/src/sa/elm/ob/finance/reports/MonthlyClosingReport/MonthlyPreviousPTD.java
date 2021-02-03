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
 * @author Gopalakrishnan created on 11/01/2017
 * 
 *         This one is the class in charge of the report of Previous month YTD Summary
 */

@SuppressWarnings("deprecation")
public class MonthlyPreviousPTD {
  private static Logger log4j = Logger.getLogger(MonthlyPreviousPTD.class);

  public static File downloadPTDSummary(File excelFile, List<HashMap<String, String>> hashmaplist) {
    File file = excelFile;
    String strFilePath = file.getPath();
    try {
      int totalCol = 10;
      HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
      HSSFSheet prevYTDSheet = null;
      Row row = null;
      Cell cell = null;
      HSSFCellStyle style = workbook.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      HSSFFont font = workbook.createFont();
      style.setFont(font);
      log4j.debug("prevYTDSheetList:" + hashmaplist.toString());
      prevYTDSheet = workbook.createSheet("Previous Month YTD");
      // Row 1
      row = prevYTDSheet.createRow((short) 0);
      row.setHeight((short) 400);
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 9, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("متأخر لنهاية الشهر الماضي");
      prevYTDSheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 3));
      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 9, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("البيان");
      prevYTDSheet.addMergedRegion(new CellRangeAddress(0, 1, 4, 4));
      cell = row.createCell((short) 5);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 9, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("التصنيف الإقتصادي");
      prevYTDSheet.addMergedRegion(new CellRangeAddress(0, 1, 5, 5));

      // row 2
      row = prevYTDSheet.createRow((short) 1);
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 9, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("دائــــــــــــــــنCR");
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 9, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("مديــــــــــــــنDR");

      // Iterate list
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
        Double creditRef = Double.valueOf(map.get("qtycreditRef") == null ? "0" : map
            .get("qtycreditRef"));
        Double debitRef = Double.valueOf(map.get("qtyRef") == null ? "0" : map.get("qtyRef"));
        // qty--current month dr
        // qtyref - current month opening dr
        // qtyCredit--current month cr
        // qtycreditRef - current month opening cr
        row = prevYTDSheet.createRow(i);
        row.setHeight((short) 400);
        // cell credit
        cell = row.createCell(2);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        if (map.get("qtycreditRef") != null) {
          cell.setCellValue(creditRef);
        } else {
          cell.setCellValue(Double.valueOf("0"));
        }
        cell.setCellStyle(creditStyle);

        cell = row.createCell(3);
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        if (map.get("qtyRef") != null) {
          cell.setCellValue(debitRef);
        } else {
          cell.setCellValue(Double.valueOf("0"));
        }
        cell.setCellStyle(creditStyle);
        // cell Description
        cell = row.createCell(4);
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
        cell = row.createCell(5);
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
        i++;
      }
      // Set Column Width
      for (int j = 0; j < totalCol; j++) {
        if (j == 4) {
          prevYTDSheet.setColumnWidth(j, 150 * 100);
        } else if (j == 5) {
          prevYTDSheet.setColumnWidth(j, 100 * 100);
        } else
          prevYTDSheet.setColumnWidth(j, 60 * 100);
      }

      // sheet row height
      prevYTDSheet.setDefaultRowHeight((short) 400);

      FileOutputStream fileOut = new FileOutputStream(strFilePath);
      workbook.write(fileOut);
      fileOut.close();
    } catch (final Exception e) {
      log4j.error("Exception in Monthly PreviousPtD() : ", e);
    }
    return file;
  }
}
