package sa.elm.ob.finance.reports.MonthlyClosingReport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import org.hibernate.SQLQuery;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.finance.reports.Style.ExcelStyle;

/**
 * @author Gopalakrishnan created on 17/01/2017
 * 
 *         This one is the class in charge of the report of Expense Account
 */

@SuppressWarnings("deprecation")
public class ExpenseSheet {
  private static Logger log4j = Logger.getLogger(ExpenseSheet.class);

  @SuppressWarnings("unchecked")
  public static File downloadExpenseSheet(File excelFile,
      List<HashMap<String, String>> hashmaplist, String sheetNo, String ClientId) {
    File file = excelFile;
    String strFilePath = file.getPath();
    String elementvalue = "", elementDesc = "", parentElement = "", parentDesc = "";
    try {
      int totalCol = 10;
      Double totalBalance = 0.00;
      Double totalCurrentBudget = 0.00;
      Double totalActualBalance = 0.00;
      Double totalPrevBalance = 0.00;
      HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
      HSSFSheet expenseSheet = null;
      Row row = null;
      Cell cell = null;
      HSSFCellStyle style = workbook.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      HSSFFont font = workbook.createFont();
      style.setFont(font);
      log4j.debug("expenseSheetList:" + hashmaplist.toString());
      // get Search Key Value from Setup Window
      String selectElement = "select ele.value as elvalue,ele.name as elname ,pnode.value as pvalue,pnode.name as pname from efin_monthly_rpt_node nde "
          + " join efin_monthlyreport_group grp on grp.efin_monthlyreport_group_id=nde.efin_monthlyreport_group_id "
          + " join c_elementvalue ele on ele.c_elementvalue_id=nde.c_elementvalue_id "
          + " join   (select node_id,el.value,el.name from ad_treenode tn "
          + " join c_elementvalue el on tn.parent_id=el.c_elementvalue_id ) pnode on pnode.node_id=ele.c_elementvalue_id "
          + " where grp.sheet='" + sheetNo + "' and nde.ad_client_id=?";

      SQLQuery elementQuery = OBDal.getInstance().getSession().createSQLQuery(selectElement);
      elementQuery.setParameter(0, ClientId);
      List<Object[]> elementList = (ArrayList<Object[]>) elementQuery.list();
      if (elementList != null && elementList.size() > 0) {
        Object[] objects = elementList.get(0);
        elementvalue = (String) objects[0];
        elementDesc = (String) objects[1];
        parentElement = (String) objects[2];
        parentDesc = (String) objects[3];

      }
      expenseSheet = workbook.createSheet(elementvalue + " " + "Expense");
      // Row 3
      HSSFCellStyle headerStyle = ExcelStyle.headerfontColor(workbook, (short) 14,
          HSSFColor.VIOLET.index);
      row = expenseSheet.createRow((short) 2);
      row.setHeight((short) 400);
      cell = row.createCell((short) 7);
      cell.setCellStyle(headerStyle);
      cell.setCellValue(parentElement);
      cell = row.createCell((short) 6);
      cell.setCellStyle(headerStyle);
      cell.setCellValue(parentDesc);
      // row4
      HSSFCellStyle elmentStyle = ExcelStyle.headerfontColor(workbook, (short) 14,
          HSSFColor.BROWN.index);
      row = expenseSheet.createRow((short) 3);
      row.setHeight((short) 400);
      cell = row.createCell((short) 7);
      cell.setCellStyle(elmentStyle);
      cell.setCellValue(elementvalue);
      cell = row.createCell((short) 6);
      cell.setCellStyle(elmentStyle);
      cell.setCellValue(elementDesc);
      // row 5
      row = expenseSheet.createRow((short) 4);
      row.setHeight((short) 400);
      // row 6
      row = expenseSheet.createRow((short) 5);
      row.setHeight((short) 600);
      cell = row.createCell((short) 8);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("رقم البرنامج / المشروع");

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
      cell.setCellValue("الإعتماد الحالي");
      cell = row.createCell((short) 4);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue(" مصروفات الشهر الجاري");
      cell = row.createCell((short) 3);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("مصروفات الأشهر الماضية");
      cell = row.createCell((short) 2);
      cell.setCellStyle(ExcelStyle.fillColorBackGround(workbook, (short) 11, HSSFColor.BROWN.index,
          HSSFColor.GREY_25_PERCENT.index));
      cell.setCellValue("الجملة");

      // Iterate list
      HSSFCellStyle styleCredit = workbook.createCellStyle();
      HSSFCellStyle creditStyle = ExcelStyle.amountCalibriFont(workbook, styleCredit);
      int i = 6;
      for (HashMap<String, String> map : hashmaplist) {
        // only for
        // sub accounts
        if (map.get("level").equals("S")) {
          Double actualBalance = Double.valueOf(map.get("qty") == null ? "0" : map.get("qty"));
          Double prevBalance = Double.valueOf(map.get("qtyRef") == null ? "0" : map.get("qtyRef"));
          Double currentBalance = Double.valueOf(map.get("qtycredit") == null ? "0" : map
              .get("qtycredit"));
          // qty--Actual month Balance
          // qtyref - previous Month Balance
          // qtycredit--current month Balance
          row = expenseSheet.createRow(i);
          row.setHeight((short) 400);
          // Total Balance
          cell = row.createCell(2);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          if (actualBalance != null && prevBalance != null) {
            cell.setCellValue(actualBalance + prevBalance);
            totalBalance = totalBalance + actualBalance + prevBalance;
          } else {
            cell.setCellValue(Double.valueOf("0"));
          }
          cell.setCellStyle(creditStyle);
          // opening Balance
          cell = row.createCell(3);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          if (prevBalance != null) {
            cell.setCellValue(prevBalance);
            totalPrevBalance = totalPrevBalance + prevBalance;
          } else {
            cell.setCellValue(Double.valueOf("0"));
          }
          cell.setCellStyle(creditStyle);
          // Actual Transaction
          cell = row.createCell(4);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          if (actualBalance != null) {
            cell.setCellValue(actualBalance);
            totalActualBalance = totalActualBalance + actualBalance;
          } else {
            cell.setCellValue(Double.valueOf("0"));
          }
          cell.setCellStyle(creditStyle);
          // Current Budget Balance
          cell = row.createCell(5);
          cell.setCellType(Cell.CELL_TYPE_NUMERIC);
          if (currentBalance != null) {
            cell.setCellValue(currentBalance);
            totalCurrentBudget = totalCurrentBudget + currentBalance;
          } else {
            cell.setCellValue(Double.valueOf("0"));
          }
          cell.setCellStyle(creditStyle);
          // Name
          cell = row.createCell(6);
          cell.setCellType(Cell.CELL_TYPE_STRING);
          cell.setCellValue(map.get("name"));
          cell.setCellStyle(creditStyle);
          // Project is empty
          // Project column value as "Act Search key"
          if (!StringUtils.isEmpty(map.get("project"))) {
            // cell 6 --project
            cell = row.createCell(7);
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(map.get("project"));
            cell.setCellStyle(creditStyle);
            // cell 7 --act value
            cell = row.createCell(8);
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(map.get("account"));
            cell.setCellStyle(creditStyle);

          } else {
            cell = row.createCell(7);
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(map.get("account"));
            cell.setCellStyle(creditStyle);
          }
          i++;
        }
      }
      HSSFCellStyle bottomStyle = ExcelStyle.fillBotttomColorBackGround(workbook, (short) 10,
          HSSFColor.BLACK.index, HSSFColor.GREY_25_PERCENT.index);
      row = expenseSheet.createRow(i + 1);
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
      // Total Current Budget Balance
      cell = row.createCell(5);
      cell.setCellType(Cell.CELL_TYPE_NUMERIC);
      cell.setCellValue(totalCurrentBudget);
      cell.setCellStyle(bottomStyle);
      //
      cell = row.createCell(6);
      cell.setCellStyle(bottomStyle);
      cell.setCellValue("جملة ( المعاملات على الأصول غير المالية )");
      expenseSheet.addMergedRegion(new CellRangeAddress(i + 1, i + 1, 6, 8));
      //
      cell = row.createCell(7);
      cell.setCellStyle(bottomStyle);
      // Set Column Width
      for (int j = 0; j < totalCol; j++) {
        if (j == 0) {
          expenseSheet.setColumnWidth(j, 40 * 100);
        } else if (j == 1) {
          expenseSheet.setColumnWidth(j, 40 * 100);
        } else if (j == 7) {
          expenseSheet.setColumnWidth(j, 120 * 100);
        } else {
          expenseSheet.setColumnWidth(j, 100 * 100);
        }

      }

      // sheet row height
      expenseSheet.setDefaultRowHeight((short) 400);

      FileOutputStream fileOut = new FileOutputStream(strFilePath);
      workbook.write(fileOut);
      fileOut.close();
    } catch (final Exception e) {
      log4j.error("Exception in ExpenseSheet : ", e);
    }
    return file;
  }
}
