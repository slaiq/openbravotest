package sa.elm.ob.scm.ad_process.ImportExportPropMgmt;

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
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.util.Constants;

/**
 * 
 * @author Kiruthika on 28/05/2020
 *
 */

public class ProposalExportProcess extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public static final String tabId = "88E026FD2D0446048C80E9D4749AB608";
  public static final String lineNo = "8889A70DC78646B19D1D922F99364982";
  public static final String parentLineNo = "78852B7015E44F9CB377775C7EBA759A";
  public static final String itemCode = "E85CAF06613C4E4A8E78ED90FAB6DC91";
  public static final String description = "CB6373394B4D408399A863A0B2EB5CFC";
  public static final String productCtgory = "F0ADC1C5A84B41C6912E5F541A972F63";
  public static final String uom = "FF630D55D7DD4AA281208380752ADD54";
  public static final String qtyOrdered = "6749B589F9B3448E9003BC4F5A51F420";
  public static final String initialUnitPrice = "BFF54D53B7A246C2ABB6BC1833F63156";
  public static final String negotUnitPrice = "4F84688163A04287AE10790A87AFE5F7";
  public static final String netUnitPrice = "A5DD82C7321C4547829FB0D37A4F6291";
  public static final String discountPer = "94C38F7198444F3EB5CF649A0ED5693A";
  public static final String discountAmt = "F5BF2542F92A425692C92A449F617427";
  public static final String lineTotal = "4D475996D07A4325888714DFC94D7482";
  public static final String notProvided = "E5A2F03EA91C48E4A7081868292F7155";
  public static final String nationalPdt = "3BE78D1F47BC4DF69A717624A80EBE0C";
  public static final String summary = "BC7BE5B12EE54937B86C197DACE7AED0";
  public static final String uniqueCode = "166007760B754F53BAB04F712AD245B3";
  public static final String comments = "4EEFD82829304E9EB0061C67D1090B91";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    try {
      // Excel File
      OBContext.setAdminMode();
      @SuppressWarnings("unused")
      File excelFile = null;
      String proposalId = request.getParameter("inpRecordId");
      VariablesSecureApp vars = new VariablesSecureApp(request);
      EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);

      OBContext.setAdminMode();
      Window windowobj = OBDal.getInstance().get(Window.class, Constants.PROPOSAL_MANAGEMENT_W);
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
      String file = proposal.getProposalno() + windowName;
      file = URLEncoder.encode(file, "UTF-8").replaceAll("\\+", " ");

      String filedir = globalParameters.strFTPDirectory + "/";
      excelFile = createExcel(filedir + file + "-.xlsx", proposalId, vars);

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

  public File createExcel(String filePath, String proposalId, VariablesSecureApp vars) {
    File file = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      Connection conn = null;
      conn = getConnection();
      int x = 1;
      ProposalExportDAO dao = new ProposalExportDAOImpl();

      st = conn.prepareStatement(
          "select ln.escm_proposalmgmt_line_id, ln.Line as lineno, prntln.Line as parentlineno, "
              + " pro.itemcode as itemcode, ln.Description as description, a.code as productcategory,  "
              + " uom.name as uomname, ln.Movementqty, ln.Gross_Unit_Price, ln.Negot_Unit_Price, "
              + " ln.Netprice, ln.Discount, ln.Discountmount, ln.Line_Total, ln.Notprovided, "
              + " ln.Nationalproduct, ln.Issummarylevel, uniquecode.EM_Efin_Uniquecode as uniquecode, "
              + " ln.comments from escm_proposalmgmt_line ln "
              + " left join escm_proposalmgmt_line prntln on ln.Parentline_ID = prntln.escm_proposalmgmt_line_id "
              + " left join (select product.m_product_id,concat(product.value) as  itemcode "
              + " from m_product product) pro on pro.m_product_id = ln.M_Product_ID "
              + " left join (select prodcat.m_product_category_id,concat(mastprodcat.name,' - ',mastprodcat.value,' - ', "
              + " prodcat.name,' - ',prodcat.value) as code  " + " from m_product_category prodcat "
              + " left join m_product_category mastprodcat ON prodcat.em_escm_product_category = mastprodcat.m_product_category_id) a "
              + " on a.m_product_category_id = ln.M_Product_Category_ID  "
              + " left join c_uom uom on uom.c_uom_id = ln.C_Uom_ID "
              + " left join c_validcombination uniquecode on uniquecode.c_validcombination_id= ln.EM_Efin_C_Validcombination_ID "
              + " where ln.escm_proposalmgmt_id = '" + proposalId + "' order by ln.Line asc");

      rs = st.executeQuery();

      file = new File(filePath);
      XSSFWorkbook workbook = new XSSFWorkbook();
      OBContext.setAdminMode();
      Tab tabobj = OBDal.getInstance().get(Tab.class, Constants.PROPOSAL_MANAGEMENT_LINES_TAB);
      String tabName = "";
      if (tabobj != null) {
        List<TabTrl> transList = tabobj.getADTabTrlList();
        if (transList.size() > 0) {
          tabName = transList.get(0).getName();
        }
      }
      XSSFSheet sheet = workbook.createSheet(tabName);
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
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
      int y = 1;

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
        for (int i = 0; i <= 18; i++) {
          XSSFCell cell = headerRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString("escm_proposalmgmt_line_id"));
            break;
          case 1:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(lineNo) ? fieldMap.get(lineNo) : "")
                    + "(*)");
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(parentLineNo) ? fieldMap.get(parentLineNo) : ""));
            break;
          case 3:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(itemCode) ? fieldMap.get(itemCode) : ""));
            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(description) ? fieldMap.get(description) : "") + "(*)");
            break;
          case 5:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(productCtgory) ? fieldMap.get(productCtgory) : ""));
            break;
          case 6:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(uom) ? fieldMap.get(uom) : "") + "(*)");
            break;
          case 7:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(qtyOrdered) ? fieldMap.get(qtyOrdered) : "") + "(*)");
            break;
          case 8:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(initialUnitPrice) ? fieldMap.get(initialUnitPrice) : "")
                + "(*)");
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(negotUnitPrice) ? fieldMap.get(negotUnitPrice) : ""));
            break;
          case 10:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(netUnitPrice) ? fieldMap.get(netUnitPrice) : ""));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(discountPer) ? fieldMap.get(discountPer) : ""));
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(discountAmt) ? fieldMap.get(discountAmt) : ""));
            break;
          case 13:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(lineTotal) ? fieldMap.get(lineTotal) : ""));
            break;
          case 14:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(notProvided) ? fieldMap.get(notProvided) : ""));
            break;
          case 15:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(nationalPdt) ? fieldMap.get(nationalPdt) : ""));
            break;
          case 16:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(summary) ? fieldMap.get(summary) : ""));
            break;
          case 17:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(uniqueCode) ? fieldMap.get(uniqueCode) : ""));
            break;
          case 18:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(comments) ? fieldMap.get(comments) : ""));
            break;
          }
        }
      } else {
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
        for (int i = 0; i <= 18; i++) {
          XSSFCell cell = headerRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString("escm_proposalmgmt_line_id"));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(lineNo) ? fieldTrlMap.get(lineNo) : fieldMap.get(lineNo))
                + "(*)");
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(parentLineNo) ? fieldTrlMap.get(parentLineNo)
                    : fieldMap.get(parentLineNo)));
            break;
          case 3:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(itemCode) ? fieldTrlMap.get(itemCode)
                    : fieldMap.get(itemCode)));
            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(description) ? fieldTrlMap.get(description)
                    : fieldMap.get(description))
                + "(*)");
            break;
          case 5:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(productCtgory) ? fieldTrlMap.get(productCtgory)
                    : fieldMap.get(productCtgory)));
            break;
          case 6:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(uom) ? fieldTrlMap.get(uom) : fieldMap.get(uom)) + "(*)");
            break;
          case 7:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(qtyOrdered) ? fieldTrlMap.get(qtyOrdered)
                    : fieldMap.get(qtyOrdered))
                + "(*)");
            break;
          case 8:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(initialUnitPrice) ? fieldTrlMap.get(initialUnitPrice)
                    : fieldMap.get(initialUnitPrice))
                + "(*)");
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(negotUnitPrice) ? fieldTrlMap.get(negotUnitPrice)
                    : fieldMap.get(negotUnitPrice)));
            break;
          case 10:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(netUnitPrice) ? fieldTrlMap.get(netUnitPrice)
                    : fieldMap.get(netUnitPrice)));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(discountPer) ? fieldTrlMap.get(discountPer)
                    : fieldMap.get(discountPer)));
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(discountAmt) ? fieldTrlMap.get(discountAmt)
                    : fieldMap.get(discountAmt)));
            break;
          case 13:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(lineTotal) ? fieldTrlMap.get(lineTotal)
                    : fieldMap.get(lineTotal)));
            break;
          case 14:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(notProvided) ? fieldTrlMap.get(notProvided)
                    : fieldMap.get(notProvided)));
            break;
          case 15:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(nationalPdt) ? fieldTrlMap.get(nationalPdt)
                    : fieldMap.get(nationalPdt)));
            break;
          case 16:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(summary) ? fieldTrlMap.get(summary)
                    : fieldMap.get(summary)));
            break;
          case 17:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(uniqueCode) ? fieldTrlMap.get(uniqueCode)
                    : fieldMap.get(uniqueCode)));
            break;
          case 18:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(comments) ? fieldTrlMap.get(comments)
                    : fieldMap.get(comments)));
            break;
          }
        }
      }

      while (rs.next()) {
        XSSFRow row = sheet.createRow(x);
        HashMap<Integer, String> cellMap = dao
            .getProposalCellStyle(rs.getString("escm_proposalmgmt_line_id"));
        for (int i = 0; i <= 18; i++) {
          XSSFCell cell = row.createCell(i);
          cell.setCellStyle(styles.get(cellMap.get(i)));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString(rs.getString("escm_proposalmgmt_line_id")));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(rs.getString("lineno")));
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
            cell.setCellValue(new XSSFRichTextString(rs.getString("productcategory")));
            break;
          case 6:
            cell.setCellValue(new XSSFRichTextString(rs.getString("uomname")));
            break;
          case 7:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Movementqty")));
            break;
          case 8:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Gross_Unit_Price")));
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Negot_Unit_Price")));
            break;
          case 10:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Netprice")));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Discount")));
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Discountmount")));
            break;
          case 13:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Line_Total")));
            break;
          case 14:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Notprovided")));
            break;
          case 15:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Nationalproduct")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;
          case 16:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Issummarylevel")));
            break;
          case 17:
            cell.setCellValue(new XSSFRichTextString(rs.getString("uniquecode")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;
          case 18:
            cell.setCellValue(new XSSFRichTextString(rs.getString("Comments")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;
          }
        }

        sheet.setColumnWidth(0, 40 * 100);
        sheet.setColumnWidth(1, 30 * 100);
        sheet.setColumnWidth(2, 30 * 100);
        sheet.setColumnWidth(3, 50 * 100);
        sheet.setColumnWidth(4, 50 * 100);
        sheet.setColumnWidth(5, 60 * 100);
        sheet.setColumnWidth(6, 40 * 100);
        sheet.setColumnWidth(7, 50 * 100);
        sheet.setColumnWidth(8, 50 * 100);
        sheet.setColumnWidth(9, 50 * 100);
        sheet.setColumnWidth(10, 50 * 100);
        sheet.setColumnWidth(11, 60 * 100);
        sheet.setColumnWidth(12, 50 * 100);
        sheet.setColumnWidth(13, 50 * 100);
        sheet.setColumnWidth(14, 30 * 100);
        sheet.setColumnWidth(15, 50 * 100);
        sheet.setColumnWidth(16, 30 * 100);
        sheet.setColumnWidth(17, 100 * 100);
        sheet.setColumnWidth(18, 100 * 100);

        sheet.setColumnHidden(0, true);

        x++;
      }
      sheet.setColumnHidden(0, true);

      EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      if (proposal.getEscmBidmgmt() == null && proposal.getProposalstatus().equals("DR")) {

        XSSFSheet insertSheet = workbook.createSheet(Constants.INSERT);

        if (vars.getLanguage().equals("en_US")) {
          // Create Header
          XSSFRow insertHeaderRow = insertSheet.createRow(0);
          for (int i = 0; i <= 18; i++) {
            XSSFCell insertCell = insertHeaderRow.createCell(i);
            insertCell.setCellStyle(styles.get("Header"));
            switch (i) {
            case 0:
              insertCell.setCellValue(new XSSFRichTextString("InsertLines"));
              break;
            case 1:
              insertCell.setCellValue(
                  new XSSFRichTextString(fieldMap.containsKey(lineNo) ? fieldMap.get(lineNo) : "")
                      + "(*)");
              break;
            case 2:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(parentLineNo) ? fieldMap.get(parentLineNo) : ""));
              break;
            case 3:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(itemCode) ? fieldMap.get(itemCode) : ""));
              break;
            case 4:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(description) ? fieldMap.get(description) : "") + "(*)");
              break;
            case 5:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(productCtgory) ? fieldMap.get(productCtgory) : ""));
              break;
            case 6:
              insertCell.setCellValue(
                  new XSSFRichTextString(fieldMap.containsKey(uom) ? fieldMap.get(uom) : "")
                      + "(*)");
              break;
            case 7:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(qtyOrdered) ? fieldMap.get(qtyOrdered) : "") + "(*)");
              break;
            case 8:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(initialUnitPrice) ? fieldMap.get(initialUnitPrice) : "")
                  + "(*)");
              break;
            case 9:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(negotUnitPrice) ? fieldMap.get(negotUnitPrice) : ""));
              break;
            case 10:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(netUnitPrice) ? fieldMap.get(netUnitPrice) : ""));
              break;
            case 11:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(discountPer) ? fieldMap.get(discountPer) : ""));
              break;
            case 12:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(discountAmt) ? fieldMap.get(discountAmt) : ""));
              break;
            case 13:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(lineTotal) ? fieldMap.get(lineTotal) : ""));
              break;
            case 14:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(notProvided) ? fieldMap.get(notProvided) : ""));
              break;
            case 15:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(nationalPdt) ? fieldMap.get(nationalPdt) : ""));
              break;
            case 16:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(summary) ? fieldMap.get(summary) : ""));
              break;
            case 17:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(uniqueCode) ? fieldMap.get(uniqueCode) : ""));
              break;
            case 18:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(comments) ? fieldMap.get(comments) : ""));
              break;
            }
          }
        } else {
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
          XSSFRow insertHeaderRow = insertSheet.createRow(0);
          for (int i = 0; i <= 18; i++) {
            XSSFCell insertCell = insertHeaderRow.createCell(i);
            insertCell.setCellStyle(styles.get("Header"));
            switch (i) {
            case 0:
              insertCell.setCellValue(new XSSFRichTextString("escm_proposalmgmt_line_id"));
              break;
            case 1:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(lineNo) ? fieldTrlMap.get(lineNo) : fieldMap.get(lineNo))
                  + "(*)");
              break;
            case 2:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(parentLineNo) ? fieldTrlMap.get(parentLineNo)
                      : fieldMap.get(parentLineNo)));
              break;
            case 3:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(itemCode) ? fieldTrlMap.get(itemCode)
                      : fieldMap.get(itemCode)));
              break;
            case 4:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(description) ? fieldTrlMap.get(description)
                      : fieldMap.get(description))
                  + "(*)");
              break;
            case 5:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(productCtgory) ? fieldTrlMap.get(productCtgory)
                      : fieldMap.get(productCtgory)));
              break;
            case 6:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(uom) ? fieldTrlMap.get(uom) : fieldMap.get(uom)) + "(*)");
              break;
            case 7:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(qtyOrdered) ? fieldTrlMap.get(qtyOrdered)
                      : fieldMap.get(qtyOrdered))
                  + "(*)");
              break;
            case 8:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(initialUnitPrice) ? fieldTrlMap.get(initialUnitPrice)
                      : fieldMap.get(initialUnitPrice))
                  + "(*)");
              break;
            case 9:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(negotUnitPrice) ? fieldTrlMap.get(negotUnitPrice)
                      : fieldMap.get(negotUnitPrice)));
              break;
            case 10:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(netUnitPrice) ? fieldTrlMap.get(netUnitPrice)
                      : fieldMap.get(netUnitPrice)));
              break;
            case 11:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(discountPer) ? fieldTrlMap.get(discountPer)
                      : fieldMap.get(discountPer)));
              break;
            case 12:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(discountAmt) ? fieldTrlMap.get(discountAmt)
                      : fieldMap.get(discountAmt)));
              break;
            case 13:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(lineTotal) ? fieldTrlMap.get(lineTotal)
                      : fieldMap.get(lineTotal)));
              break;
            case 14:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(notProvided) ? fieldTrlMap.get(notProvided)
                      : fieldMap.get(notProvided)));
              break;
            case 15:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(nationalPdt) ? fieldTrlMap.get(nationalPdt)
                      : fieldMap.get(nationalPdt)));
              break;
            case 16:
              insertCell.setCellValue(
                  new XSSFRichTextString(fieldTrlMap.containsKey(summary) ? fieldTrlMap.get(summary)
                      : fieldMap.get(summary)));
              break;
            case 17:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(uniqueCode) ? fieldTrlMap.get(uniqueCode)
                      : fieldMap.get(uniqueCode)));
              break;
            case 18:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(comments) ? fieldTrlMap.get(comments)
                      : fieldMap.get(comments)));
              break;
            }
          }
        }

        insertSheet.setColumnWidth(0, 40 * 100);
        insertSheet.setColumnWidth(1, 30 * 100);
        insertSheet.setColumnWidth(2, 30 * 100);
        insertSheet.setColumnWidth(3, 50 * 100);
        insertSheet.setColumnWidth(4, 50 * 100);
        insertSheet.setColumnWidth(5, 50 * 100);
        insertSheet.setColumnWidth(6, 40 * 100);
        insertSheet.setColumnWidth(7, 50 * 100);
        insertSheet.setColumnWidth(8, 50 * 100);
        insertSheet.setColumnWidth(9, 50 * 100);
        insertSheet.setColumnWidth(10, 50 * 100);
        insertSheet.setColumnWidth(11, 60 * 100);
        insertSheet.setColumnWidth(12, 50 * 100);
        insertSheet.setColumnWidth(13, 50 * 100);
        insertSheet.setColumnWidth(14, 30 * 100);
        insertSheet.setColumnWidth(15, 50 * 100);
        insertSheet.setColumnWidth(16, 30 * 100);
        insertSheet.setColumnWidth(17, 100 * 100);
        insertSheet.setColumnWidth(18, 100 * 100);

        insertSheet.setColumnHidden(0, true);

        // Create 100 Empty Rows
        for (int i = y; i < 100 + y; i++) {

          XSSFRow row = insertSheet.createRow(i);
          CellStyle editableStyle = workbook.createCellStyle();
          editableStyle.setLocked(false);
          insertSheet.setDefaultColumnStyle(i, editableStyle);
          for (int j = 0; j <= 18; j++) {
            XSSFCell cell = row.createCell(j);
            evaluator.evaluateFormulaCell(cell);
            if (j == 3 || j == 4 || j == 15 || j == 17 || j == 18)
              cell.setCellStyle(styles.get("TextUnlockFormat"));
          }
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