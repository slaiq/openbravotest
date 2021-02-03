package sa.elm.ob.scm.ad_process.BidManagement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.TabTrl;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.ui.WindowTrl;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;

import sa.elm.ob.finance.ad_process.ExportBudget.util.XSSFExcelStyles;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.utility.util.Constants;

/**
 * 
 * @author Gokul on 21/05/2020
 *
 */

public class BidExportProcess extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public static final String tabId = "D54F30C8AD574A2A84999F327EF0E3A4";
  public static final String lineNo = "46B7987D0F6F41CF8DD62531114898E9";
  public static final String parentLineNo = "DD009162A49D4B93A99E8966FEAF0E2E";
  public static final String itemCode = "F20EF356F5FD47BFBFAB52EA0932C088";
  public static final String description = "A79DEE330C604598B9F3EBD3D8ECFD3E";
  public static final String productCtgory = "5DEBDDC073724C76AD0C4032EDBB5BA2";
  public static final String uom = "3F72A109BFA84BA09FA848A585E707ED";
  public static final String qtyOrdered = "2E481840B38240719095900E254C57D1";
  // public static final String negotUnitPrice = "1100438612E24CB880EE85EFD1B8BDE3";
  // public static final String grossLineAmt = "9B78097B7BF54F429D4CBB5FCC2E6FC9";
  public static final String summary = "62E6565D97514D5F8A213021808904C1";
  // public static final String needByDate = "0A3E79996173484FB0BC6D957ACE7D19";
  // public static final String accountNo = "0EA75399EACA4AACAC0EECDB8537E09B";
  // public static final String comments = "7B544E3FB8DB4B69B1FC1EA42126527B";
  // public static final String nationalProduct = "B309566328CC425FAF90B9022C7991E8";
  public static final String unqiueCode = "75397AF1F82249D99C91F40DCFEF0B5F";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    try {
      // Excel File
      OBContext.setAdminMode();
      @SuppressWarnings("unused")
      File excelFile = null;
      String bidId = request.getParameter("inpRecordId");
      VariablesSecureApp vars = new VariablesSecureApp(request);
      EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class, bidId);

      OBContext.setAdminMode();
      Window windowobj = OBDal.getInstance().get(Window.class, Constants.BID_MANAGEMENT_W);
      String windowName = "";
      if (windowobj != null) {
        List<WindowTrl> transList = windowobj.getADWindowTrlList();
        if (transList.size() > 0) {
          windowName = "_" + transList.get(0).getName();
        }
      }
      // create excel file
      // Task No: 8230 structured in reverse direction to get the required
      // format(ArabiceWindowName_Docno)
      String file = bid.getBidno() + windowName;
      file = URLEncoder.encode(file, "UTF-8").replaceAll("\\+", " ");

      String filedir = globalParameters.strFTPDirectory + "/";
      excelFile = createExcel(filedir + file + "-.xlsx", bidId, vars);

      response.setContentType("text/html; charset=UTF-8");
      response.setCharacterEncoding("UTF-8");
      response.addHeader("Content-Disposition", "inline; filename=" + file + "-.html");
      printPagePopUpDownload(response.getOutputStream(),
          URLEncoder.encode(file, "UTF-8") + "-.xlsx");

    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      log4j.error("Exception in Download : ", e);
    } catch (Exception e) {
      log4j.error("Exception in Download : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public File createExcel(String filePath, String bidId, VariablesSecureApp vars) {
    File file = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      Connection conn = null;
      conn = getConnection();
      int x = 1;
      int y = 1;
      BidExportDAO dao = new BidExportDAOImpl();

      st = conn.prepareStatement(
          " select escm_bidmgmt_line.escm_bidmgmt_line_id,escm_bidmgmt_line.line as line,parent.line as parentlineno, "
              + " pro.itemcode as itemcode ,escm_bidmgmt_line.Description as description,a.code as procategory, "
              + " uom.name as uomname,escm_bidmgmt_line.Movementqty as qty, "
              + " escm_bidmgmt_line.Issummarylevel as summary,uniquecode.EM_Efin_Uniquecode as uniquecode "
              + " from escm_bidmgmt_line  "
              + " left join escm_bidmgmt_line parent on escm_bidmgmt_line.Parentline_ID = parent.escm_bidmgmt_line_id "
              + " left join (select product.m_product_id,concat(product.value) as  itemcode "
              + " from m_product product ) pro on pro.m_product_id = escm_bidmgmt_line.m_product_id "
              + " left join (select prodcat.m_product_category_id, "
              + " concat(mastprodcat.name,' - ',mastprodcat.value,' - ',prodcat.name,' - ',prodcat.value) as code "
              + " from m_product_category prodcat "
              + " LEFT JOIN m_product_category mastprodcat ON prodcat.em_escm_product_category = mastprodcat.m_product_category_id "
              + " ) a on a.m_product_category_id = escm_bidmgmt_line.M_Product_Category_ID "
              + " left join c_uom uom on uom.c_uom_id = escm_bidmgmt_line.C_UOM_ID "
              + " left join c_validcombination uniquecode on uniquecode.c_validcombination_id= escm_bidmgmt_line.C_Validcombination_ID "
              + " where escm_bidmgmt_line.escm_bidmgmt_id = '" + bidId
              + "' order by escm_bidmgmt_line. line asc");

      rs = st.executeQuery();

      file = new File(filePath);
      XSSFWorkbook workbook = new XSSFWorkbook();
      OBContext.setAdminMode();
      Tab tabobj = OBDal.getInstance().get(Tab.class, Constants.BID_MANAGEMENT_LINES_TAB);
      String tabName = "";
      if (tabobj != null) {
        List<TabTrl> transList = tabobj.getADTabTrlList();
        if (transList.size() > 0) {
          tabName = transList.get(0).getName();
        }
      }
      XSSFSheet sheet = workbook.createSheet(tabName);
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      XSSFSheet insertSheet = workbook.createSheet(Constants.INSERT);
      XSSFExcelStyles excelStyles = new XSSFExcelStyles();
      Map<String, XSSFCellStyle> styles = excelStyles.createStyles(workbook);

      sheet.enableLocking();
      if (!vars.getLanguage().equals("en_US")) {
        sheet.setRightToLeft(true);
      }
      CTSheetProtection sheetProtection = sheet.getCTWorksheet().getSheetProtection();

      sheetProtection.setSelectLockedCells(true);
      sheetProtection.setSelectUnlockedCells(false);
      sheetProtection.setFormatCells(false);
      sheetProtection.setFormatColumns(false);
      sheetProtection.setFormatRows(true);
      sheetProtection.setInsertColumns(false);
      sheetProtection.setInsertRows(false);
      sheetProtection.setInsertHyperlinks(false);
      sheetProtection.setDeleteColumns(false);
      sheetProtection.setDeleteRows(false);
      sheetProtection.setSort(false);
      sheetProtection.setAutoFilter(false);
      sheetProtection.setPivotTables(false);
      sheetProtection.setObjects(false);
      sheetProtection.setScenarios(false);

      List<Field> fieldList = null;
      HashMap<String, String> fieldMap = new HashMap<>();

      OBQuery<Field> fieldObj = OBDal.getInstance().createQuery(Field.class,
          "as e where e.tab.id=:tabId");
      fieldObj.setNamedParameter("tabId", tabId);
      fieldList = fieldObj.list();
      for (Field itr : fieldList) {
        fieldMap.put(itr.getId(), itr.getName());
      }
      if (vars.getLanguage().equals("en_US")) {
        // Create Header
        XSSFRow headerRow = sheet.createRow(0);
        XSSFRow insertHeaderRow = insertSheet.createRow(0);
        for (int i = 0; i <= 9; i++) {
          XSSFCell cell = headerRow.createCell(i);
          XSSFCell insertCell = insertHeaderRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          insertCell.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString("escm_bidmgmt_line_id"));
            insertCell.setCellValue(new XSSFRichTextString("InsertLines"));
            break;
          case 1:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(lineNo) ? fieldMap.get(lineNo) : "")
                    + "(*)");
            insertCell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(lineNo) ? fieldMap.get(lineNo) : "")
                    + "(*)");
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(parentLineNo) ? fieldMap.get(parentLineNo) : ""));
            insertCell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(parentLineNo) ? fieldMap.get(parentLineNo) : ""));
            break;
          case 3:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(itemCode) ? fieldMap.get(itemCode) : ""));
            insertCell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(itemCode) ? fieldMap.get(itemCode) : ""));
            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(description) ? fieldMap.get(description) : "") + "(*)");
            insertCell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(description) ? fieldMap.get(description) : "") + "(*)");
            break;
          case 5:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(productCtgory) ? fieldMap.get(productCtgory) : ""));
            insertCell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(productCtgory) ? fieldMap.get(productCtgory) : ""));
            break;
          case 6:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(uom) ? fieldMap.get(uom) : "") + "(*)");
            insertCell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(uom) ? fieldMap.get(uom) : "") + "(*)");
            break;
          case 7:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(qtyOrdered) ? fieldMap.get(qtyOrdered) : "") + "(*)");
            insertCell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(qtyOrdered) ? fieldMap.get(qtyOrdered) : "") + "(*)");
            break;
          case 8:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(summary) ? fieldMap.get(summary) : ""));
            insertCell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(summary) ? fieldMap.get(summary) : ""));
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(unqiueCode) ? fieldMap.get(unqiueCode) : ""));
            insertCell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(unqiueCode) ? fieldMap.get(unqiueCode) : ""));
            break;
          }
        }

      }

      else {
        List<FieldTrl> fieldTrlList = null;
        HashMap<String, String> fieldTrlMap = new HashMap<>();
        OBQuery<FieldTrl> fieldTrlObj = OBDal.getInstance().createQuery(FieldTrl.class,
            "as e join e.field field join field.tab tab where field.tab.id=:tabId");
        fieldTrlObj.setNamedParameter("tabId", tabId);
        fieldTrlList = fieldTrlObj.list();
        for (FieldTrl itr : fieldTrlList) {
          fieldTrlMap.put(itr.getField().getId(),
              StringUtils.isEmpty(itr.getName()) ? itr.getField().getName() : itr.getName());
        }
        // Create Header
        XSSFRow headerRow = sheet.createRow(0);
        XSSFRow insertHeaderRow = insertSheet.createRow(0);
        for (int i = 0; i <= 9; i++) {
          XSSFCell cell = headerRow.createCell(i);
          XSSFCell insertCell = insertHeaderRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          insertCell.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString("escm_bidmgmt_line_id"));
            insertCell.setCellValue(new XSSFRichTextString("escm_bidmgmt_line_id"));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(lineNo) ? fieldTrlMap.get(lineNo) : fieldMap.get(lineNo))
                + "(*)");
            insertCell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(lineNo) ? fieldTrlMap.get(lineNo) : fieldMap.get(lineNo))
                + "(*)");
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(parentLineNo) ? fieldTrlMap.get(parentLineNo)
                    : fieldMap.get(parentLineNo)));
            insertCell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(parentLineNo) ? fieldTrlMap.get(parentLineNo)
                    : fieldMap.get(parentLineNo)));
            break;
          case 3:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(itemCode) ? fieldTrlMap.get(itemCode)
                    : fieldMap.get(itemCode)));
            insertCell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(itemCode) ? fieldTrlMap.get(itemCode)
                    : fieldMap.get(itemCode)));
            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(description) ? fieldTrlMap.get(description)
                    : fieldMap.get(description))
                + "(*)");
            insertCell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(description) ? fieldTrlMap.get(description)
                    : fieldMap.get(description))
                + "(*)");
            break;
          case 5:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(productCtgory) ? fieldTrlMap.get(productCtgory)
                    : fieldMap.get(productCtgory)));
            insertCell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(productCtgory) ? fieldTrlMap.get(productCtgory)
                    : fieldMap.get(productCtgory)));
            break;
          case 6:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(uom) ? fieldTrlMap.get(uom) : fieldMap.get(uom)) + "(*)");
            insertCell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(uom) ? fieldTrlMap.get(uom) : fieldMap.get(uom)) + "(*)");
            break;
          case 7:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(qtyOrdered) ? fieldTrlMap.get(qtyOrdered)
                    : fieldMap.get(qtyOrdered))
                + "(*)");
            insertCell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(qtyOrdered) ? fieldTrlMap.get(qtyOrdered)
                    : fieldMap.get(qtyOrdered))
                + "(*)");
            break;
          case 8:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(summary) ? fieldTrlMap.get(summary)
                    : fieldMap.get(summary)));
            insertCell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(summary) ? fieldTrlMap.get(summary)
                    : fieldMap.get(summary)));
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(unqiueCode) ? fieldTrlMap.get(unqiueCode)
                    : fieldMap.get(unqiueCode)));
            insertCell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(unqiueCode) ? fieldTrlMap.get(unqiueCode)
                    : fieldMap.get(unqiueCode)));
            break;

          }
        }

      }

      while (rs.next()) {
        XSSFRow row = sheet.createRow(x);
        HashMap<Integer, String> cellMap = dao
            .getBidCellStyle(rs.getString("escm_bidmgmt_line_id"));
        for (int i = 0; i <= 9; i++) {
          XSSFCell cell = row.createCell(i);
          cell.setCellStyle(styles.get(cellMap.get(i)));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString(rs.getString("escm_bidmgmt_line_id")));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(rs.getString("line")));
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(rs.getString("parentlineno")));
            break;
          case 3:
            cell.setCellValue(new XSSFRichTextString(rs.getString("itemcode")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString(rs.getString("description")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;
          case 5:
            cell.setCellValue(new XSSFRichTextString(rs.getString("procategory")));
            break;
          case 6:
            cell.setCellValue(new XSSFRichTextString(rs.getString("uomname")));
            break;
          case 7:
            cell.setCellValue(new XSSFRichTextString(rs.getString("qty")));
            break;
          case 8:
            cell.setCellValue(new XSSFRichTextString(rs.getString("summary")));
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(rs.getString("uniquecode")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;

          }
        }

        sheet.setColumnWidth(0, 50 * 100);
        sheet.setColumnWidth(1, 80 * 100);
        sheet.setColumnWidth(2, 80 * 100);
        sheet.setColumnWidth(3, 80 * 100);
        sheet.setColumnWidth(4, 80 * 100);
        sheet.setColumnWidth(5, 80 * 100);
        sheet.setColumnWidth(6, 80 * 100);
        sheet.setColumnWidth(7, 80 * 100);
        sheet.setColumnWidth(8, 85 * 100);
        sheet.setColumnWidth(9, 90 * 100);
        sheet.setColumnHidden(0, true);

        insertSheet.setColumnWidth(0, 50 * 100);
        insertSheet.setColumnWidth(1, 80 * 100);
        insertSheet.setColumnWidth(2, 80 * 100);
        insertSheet.setColumnWidth(3, 80 * 100);
        insertSheet.setColumnWidth(4, 80 * 100);
        insertSheet.setColumnWidth(5, 80 * 100);
        insertSheet.setColumnWidth(6, 80 * 100);
        insertSheet.setColumnWidth(7, 80 * 100);
        insertSheet.setColumnWidth(8, 85 * 100);
        insertSheet.setColumnWidth(9, 90 * 100);
        insertSheet.setColumnHidden(0, true);
        x++;
      }

      insertSheet.setColumnHidden(0, true);
      sheet.setColumnHidden(0, true);

      // Create 100 Empty Rows
      for (int i = y; i < 100 + y; i++) {

        XSSFRow row = insertSheet.createRow(i);
        CellStyle editableStyle = workbook.createCellStyle();
        editableStyle.setLocked(false);
        insertSheet.setDefaultColumnStyle(i, editableStyle);
        for (int j = 0; j <= 9; j++) {
          XSSFCell cell = row.createCell(j);
          evaluator.evaluateFormulaCell(cell);
          if (j == 3 || j == 4 || j == 9)
            cell.setCellStyle(styles.get("TextUnlockFormat"));
        }
      }

      FileOutputStream fileOut = new FileOutputStream(filePath);
      workbook.write(fileOut);
      fileOut.close();
    } catch (final Exception e) {
      log4j.error("Exception in createExcel() Method : ", e);
      return null;
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in createExcel() Method ", e);
      }
      OBContext.restorePreviousMode();
    }
    return file;
  }

}