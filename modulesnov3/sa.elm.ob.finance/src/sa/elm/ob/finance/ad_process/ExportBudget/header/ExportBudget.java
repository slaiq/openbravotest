package sa.elm.ob.finance.ad_process.ExportBudget.header;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

import sa.elm.ob.finance.ad_process.ExportBudget.util.XSSFExcelStyles;

public class ExportBudget extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    try {
      VariablesSecureApp vars = new VariablesSecureApp(request);
      // Excel File
      Connection conn = null;
      PreparedStatement st = null;
      ResultSet rs = null;
      conn = getConnection();
      String BudgetName = "";
      String BudgetId = vars.getStringParameter("inpefinBudgetId");
      st = conn.prepareStatement(
          "select concat(budgetname,'_',c_campaign.name,'_',c_year.year) as budgetname from efin_budget \n"
              + "left join c_campaign on efin_budget.c_campaign_id = c_campaign.c_campaign_id\n"
              + "left join c_year on efin_budget.c_year_id = c_year.c_year_id\n"
              + " where efin_budget_id = '" + BudgetId + "' ");

      rs = st.executeQuery();
      if (rs.next()) {
        BudgetName = URLEncoder.encode(rs.getString("budgetname"), "UTF-8");
      }
      // create excel file
      String file = BudgetName;
      String filedir = globalParameters.strFTPDirectory + "/";
      createExcel(filedir + file + "-.xlsx", BudgetId);

      response.setContentType("text/html; charset=UTF-8");
      response.setCharacterEncoding("UTF-8");
      response.addHeader("Content-Disposition", "inline; filename=" + file + "-.html");
      printPagePopUpDownload(response.getOutputStream(),
          URLEncoder.encode(file, "UTF-8") + "-.xlsx");

      // close connection
      if (rs != null) {
        rs.close();
      }
      if (st != null) {
        st.close();
      }
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      log4j.error("Exception in Download : ", e);
    } catch (Exception e) {
      log4j.error("Exception in Download : ", e);
    }
  }

  /**
   * This method is used to create Excel
   * 
   * @param filePath
   * @param BudgetId
   * @return
   */
  public File createExcel(String filePath, String BudgetId) {
    File file = null;
    try {
      Connection conn = null;
      PreparedStatement st = null;
      ResultSet rs = null;
      conn = getConnection();
      int x = 0;

      st = conn.prepareStatement(
          "select c_validcombination_id,uniquecodename,uniquecode,amount,legacyhisbudgetvalue from efin_budgetlines where efin_budget_id = '"
              + BudgetId + "'order by line asc");

      rs = st.executeQuery();

      file = new File(filePath);
      XSSFWorkbook workbook = new XSSFWorkbook();
      XSSFSheet sheet = workbook.createSheet("BudgetLines");

      XSSFExcelStyles excelStyles = new XSSFExcelStyles();
      Map<String, XSSFCellStyle> styles = excelStyles.createStyles(workbook);

      x = 1;

      while (rs.next()) {

        // Create Header
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i <= 4; i++) {
          XSSFCell cell = headerRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          switch (i) {

          case 0:
            cell.setCellValue(new XSSFRichTextString("LineId"));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString("UniqueCode"));
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString("UniqueCodeName"));
            break;
          case 3:
            cell.setCellValue(new XSSFRichTextString("Amount"));
            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString("legacyhisbudgetvalue"));
            break;

          }
        }

        XSSFRow row = sheet.createRow(x);
        for (int i = 0; i <= 4; i++) {
          XSSFCell cell = row.createCell(i);
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString(rs.getString("c_validcombination_id")));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(rs.getString("uniquecode")));
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(rs.getString("uniquecodename")));
            break;
          case 3:
            cell.setCellValue(new XSSFRichTextString(rs.getString("amount")));
            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString(rs.getString("legacyhisbudgetvalue")));
            break;

          }
        }

        // Set Column Width
        // sheet.setColumnWidth(0, 50*100);
        // sheet.setColumnWidth(1, 50*100);
        sheet.setColumnWidth(0, 50 * 100);
        sheet.setColumnWidth(1, 75 * 100);
        sheet.setColumnWidth(2, 50 * 100);
        sheet.setColumnWidth(3, 50 * 100);
        sheet.setColumnWidth(4, 50 * 100);
        // Set Column Hidden
        // sheet.setColumnHidden(5, true);
        sheet.setColumnHidden(0, true);

        x++;
      }
      FileOutputStream fileOut = new FileOutputStream(filePath);
      workbook.write(fileOut);
      fileOut.close();
      // close connection
      if (rs != null) {
        rs.close();
      }
      if (st != null) {
        st.close();
      }
    } catch (final Exception e) {
      log4j.error("Exception in createExcel() Method : ", e);
      return null;
    }
    return file;
  }

}