package sa.elm.ob.scm.ad_process.POandContract;

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
import org.openbravo.model.common.order.Order;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;

import sa.elm.ob.finance.ad_process.ExportBudget.util.XSSFExcelStyles;
import sa.elm.ob.utility.util.Constants;

/**
 * 
 * @author poongodi on 23/04/2019
 *
 */

public class POExportProcess extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public static final String tabId = "8F35A05BFBB34C34A80E9DEF769613F7";
  public static final String lineNo = "15FC6CB3917C44179D23364D676994AE";
  public static final String parentLineNo = "7F0A40010F41447491D637B5D37B218C";
  public static final String itemCode = "024EC131D48447788779F92043EA8944";
  public static final String description = "D596E995FB434803A2D074E8F3E124A8";
  public static final String productCtgory = "0FDF8F62AA2B44A49BB433FB6008B2D6";
  public static final String uom = "6C3B92F2297347278CB1AF2CA4262B5F";
  public static final String qtyOrdered = "DB426A728696442C99CF0C2A5F1CF732";
  public static final String negotUnitPrice = "1100438612E24CB880EE85EFD1B8BDE3";
  public static final String grossLineAmt = "9B78097B7BF54F429D4CBB5FCC2E6FC9";
  public static final String summary = "D4A74D2566714528A8FCC031F0D09EB6";
  public static final String needByDate = "0A3E79996173484FB0BC6D957ACE7D19";
  public static final String accountNo = "0EA75399EACA4AACAC0EECDB8537E09B";
  public static final String comments = "7B544E3FB8DB4B69B1FC1EA42126527B";
  public static final String nationalProduct = "B309566328CC425FAF90B9022C7991E8";
  public static final String unqiueCode = "3C2234A759FA4A02B55E49B3C31AC932";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    try {
      // Excel File
      OBContext.setAdminMode();
      @SuppressWarnings("unused")
      File excelFile = null;
      String orderId = request.getParameter("inpRecordId");
      VariablesSecureApp vars = new VariablesSecureApp(request);
      Order order = OBDal.getInstance().get(Order.class, orderId);

      OBContext.setAdminMode();
      Window windowobj = OBDal.getInstance().get(Window.class,
          Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W);
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
      String file = order.getDocumentNo() + windowName;
      file = URLEncoder.encode(file, "UTF-8").replaceAll("\\+", " ");

      String filedir = globalParameters.strFTPDirectory + "/";
      excelFile = createExcel(filedir + file + "-.xlsx", orderId, vars);

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

  public File createExcel(String filePath, String orderId, VariablesSecureApp vars) {
    File file = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      Connection conn = null;
      conn = getConnection();
      int x = 1;
      POExportDAO dao = new POExportDAOImpl();

      st = conn.prepareStatement(
          "select c_orderline.c_orderline_id,c_orderline. Line as line,parent.line as parentlineno,pro.itemcode as itemcode  "
              + ",c_orderline.EM_Escm_Prodescription as description,a.code as procategory,uom.name as uomname,c_orderline.QtyOrdered as qty, "
              + "  c_orderline.PriceActual as negPrice,c_orderline.EM_Escm_Line_Total_Updated as total,c_orderline."
              + "  EM_Escm_Issummarylevel as summary,eut_convert_to_hijri(to_char(c_orderline.EM_Escm_Needbydate,'yyyy-mm-dd')) as needbydate, "
              + "  c_orderline.EM_Escm_Acctno as acctno,c_orderline.EM_Escm_Comments as comments,c_orderline.EM_Escm_Nationalproduct as nationalpro,"
              + "    uniquecode.EM_Efin_Uniquecode as uniquecode  from c_orderline  "
              + "                   left join c_orderline parent on c_orderline.EM_Escm_Parentline_ID = parent.c_orderline_id "
              + "left join (select product.m_product_id,concat(product.value) as  itemcode "
              + "    from m_product product ) pro on "
              + "pro.m_product_id = c_orderline.m_product_id "
              + "                 left join (select prodcat.m_product_category_id,concat(mastprodcat.name,' - ',mastprodcat.value,' - ',prodcat.name,' - ',prodcat.value) as code "
              + "                             from m_product_category prodcat "
              + "                 LEFT JOIN m_product_category mastprodcat ON prodcat.em_escm_product_category = mastprodcat.m_product_category_id "
              + "                               ) a "
              + "                  on a.m_product_category_id = c_orderline.EM_Escm_Product_Category_ID "
              + "                 left join c_uom uom on uom.c_uom_id = c_orderline.C_UOM_ID "
              + "                   left join c_validcombination uniquecode on uniquecode.c_validcombination_id= c_orderline.EM_Efin_C_Validcombination_ID "
              + "                   where c_orderline.c_order_id ='" + orderId
              + "' order by c_orderline. Line asc");

      rs = st.executeQuery();

      file = new File(filePath);
      XSSFWorkbook workbook = new XSSFWorkbook();
      OBContext.setAdminMode();
      Tab tabobj = OBDal.getInstance().get(Tab.class, Constants.PURCHASE_ORDER_LINES_TAB);
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

        for (int i = 0; i <= 15; i++) {
          XSSFCell cell = headerRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString("c_orderline_id"));
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
                fieldMap.containsKey(negotUnitPrice) ? fieldMap.get(negotUnitPrice) : "") + "(*)");
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(grossLineAmt) ? fieldMap.get(grossLineAmt) : ""));
            break;
          case 10:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(summary) ? fieldMap.get(summary) : ""));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(needByDate) ? fieldMap.get(needByDate) : "") + "(*)");
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(accountNo) ? fieldMap.get(accountNo) : ""));
            break;
          case 13:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(comments) ? fieldMap.get(comments) : ""));
            break;
          case 14:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(nationalProduct) ? fieldMap.get(nationalProduct) : ""));
            break;
          case 15:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(unqiueCode) ? fieldMap.get(unqiueCode) : ""));
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
        for (int i = 0; i <= 15; i++) {
          XSSFCell cell = headerRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString("c_orderline_id"));
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
                fieldTrlMap.containsKey(negotUnitPrice) ? fieldTrlMap.get(negotUnitPrice)
                    : fieldMap.get(negotUnitPrice))
                + "(*)");
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(grossLineAmt) ? fieldTrlMap.get(grossLineAmt)
                    : fieldMap.get(grossLineAmt)));
            break;
          case 10:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(summary) ? fieldTrlMap.get(summary)
                    : fieldMap.get(summary)));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(needByDate) ? fieldTrlMap.get(needByDate)
                    : fieldMap.get(needByDate))
                + "(*)");
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(accountNo) ? fieldTrlMap.get(accountNo)
                    : fieldMap.get(accountNo)));
            break;
          case 13:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(comments) ? fieldTrlMap.get(comments)
                    : fieldMap.get(comments)));
            break;
          case 14:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(nationalProduct) ? fieldTrlMap.get(nationalProduct)
                    : fieldMap.get(nationalProduct)));
            break;
          case 15:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(unqiueCode) ? fieldTrlMap.get(unqiueCode)
                    : fieldMap.get(unqiueCode)));
            break;
          }
        }
      }

      while (rs.next()) {
        XSSFRow row = sheet.createRow(x);
        HashMap<Integer, String> cellMap = dao.getPoCellStyle(rs.getString("c_orderline_id"));
        for (int i = 0; i <= 15; i++) {
          XSSFCell cell = row.createCell(i);
          cell.setCellStyle(styles.get(cellMap.get(i)));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString(rs.getString("c_orderline_id")));
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
            cell.setCellValue(new XSSFRichTextString(rs.getString("negPrice")));
            break;
          case 9:
            String formula = "H" + (x + 1) + "*I" + (x + 1);
            cell.setCellType(XSSFCell.CELL_TYPE_NUMERIC);
            cell.setCellFormula(formula);
            break;
          case 10:
            cell.setCellValue(new XSSFRichTextString(rs.getString("summary")));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(rs.getString("needbydate")));
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(rs.getString("acctno")));
            break;
          case 13:
            cell.setCellValue(new XSSFRichTextString(rs.getString("comments")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;
          case 14:
            cell.setCellValue(new XSSFRichTextString(rs.getString("nationalpro")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;
          case 15:
            cell.setCellValue(new XSSFRichTextString(rs.getString("uniquecode")));
            cell.setCellStyle(styles.get("TextUnlockFormat"));
            break;

          }
        }

        sheet.setColumnWidth(0, 50 * 100);
        sheet.setColumnWidth(1, 40 * 100);
        sheet.setColumnWidth(2, 40 * 100);
        sheet.setColumnWidth(3, 50 * 100);
        sheet.setColumnWidth(4, 50 * 100);
        sheet.setColumnWidth(5, 50 * 100);
        sheet.setColumnWidth(6, 50 * 100);
        sheet.setColumnWidth(7, 50 * 100);
        sheet.setColumnWidth(8, 75 * 100);
        sheet.setColumnWidth(9, 50 * 100);
        sheet.setColumnWidth(10, 30 * 100);
        sheet.setColumnWidth(11, 50 * 100);
        sheet.setColumnWidth(12, 50 * 100);
        sheet.setColumnWidth(13, 50 * 100);
        sheet.setColumnWidth(14, 50 * 100);
        sheet.setColumnWidth(15, 100 * 100);

        sheet.setColumnHidden(0, true);

        x++;
      }
      sheet.setColumnHidden(0, true);
      // Order order = OBDal.getInstance().get(Order.class, orderId);
      boolean canInsertNewLine = true;
      int y = 1;
      // if (((order.getEscmAppstatus().equals("ESCM_RA")
      // || order.getEscmAppstatus().equals("ESCM_REJ")) && order.getEscmOrdertype().equals("PUR"))
      // && (order.getEscmOldOrder() != null)) {
      // canInsertNewLine = false;
      // }
      if (canInsertNewLine) {
        XSSFSheet insertSheet = workbook.createSheet(Constants.INSERT);
        if (vars.getLanguage().equals("en_US")) {
          XSSFRow insertHeaderRow = insertSheet.createRow(0);
          for (int i = 0; i <= 15; i++) {
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
                  fieldMap.containsKey(negotUnitPrice) ? fieldMap.get(negotUnitPrice) : "")
                  + "(*)");
              break;
            case 9:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(grossLineAmt) ? fieldMap.get(grossLineAmt) : ""));
              break;
            case 10:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(summary) ? fieldMap.get(summary) : ""));
              break;
            case 11:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(needByDate) ? fieldMap.get(needByDate) : "") + "(*)");
              break;
            case 12:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(accountNo) ? fieldMap.get(accountNo) : ""));
              break;
            case 13:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(comments) ? fieldMap.get(comments) : ""));
              break;
            case 14:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(nationalProduct) ? fieldMap.get(nationalProduct) : ""));
              break;
            case 15:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(unqiueCode) ? fieldMap.get(unqiueCode) : ""));
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
          for (int i = 0; i <= 15; i++) {
            XSSFCell insertCell = insertHeaderRow.createCell(i);
            insertCell.setCellStyle(styles.get("Header"));
            switch (i) {
            case 0:
              insertCell.setCellValue(new XSSFRichTextString("c_orderline_id"));
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
                  fieldTrlMap.containsKey(negotUnitPrice) ? fieldTrlMap.get(negotUnitPrice)
                      : fieldMap.get(negotUnitPrice))
                  + "(*)");
              break;
            case 9:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(grossLineAmt) ? fieldTrlMap.get(grossLineAmt)
                      : fieldMap.get(grossLineAmt)));
              break;
            case 10:
              insertCell.setCellValue(
                  new XSSFRichTextString(fieldTrlMap.containsKey(summary) ? fieldTrlMap.get(summary)
                      : fieldMap.get(summary)));
              break;
            case 11:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(needByDate) ? fieldTrlMap.get(needByDate)
                      : fieldMap.get(needByDate))
                  + "(*)");
              break;
            case 12:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(accountNo) ? fieldTrlMap.get(accountNo)
                      : fieldMap.get(accountNo)));
              break;
            case 13:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(comments) ? fieldTrlMap.get(comments)
                      : fieldMap.get(comments)));
              break;
            case 14:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(nationalProduct) ? fieldTrlMap.get(nationalProduct)
                      : fieldMap.get(nationalProduct)));
              break;
            case 15:
              insertCell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(unqiueCode) ? fieldTrlMap.get(unqiueCode)
                      : fieldMap.get(unqiueCode)));
              break;
            }
          }
        }
        insertSheet.setColumnWidth(0, 50 * 100);
        insertSheet.setColumnWidth(1, 40 * 100);
        insertSheet.setColumnWidth(2, 40 * 100);
        insertSheet.setColumnWidth(3, 50 * 100);
        insertSheet.setColumnWidth(4, 50 * 100);
        insertSheet.setColumnWidth(5, 50 * 100);
        insertSheet.setColumnWidth(6, 50 * 100);
        insertSheet.setColumnWidth(7, 50 * 100);
        insertSheet.setColumnWidth(8, 75 * 100);
        insertSheet.setColumnWidth(9, 50 * 100);
        insertSheet.setColumnWidth(10, 30 * 100);
        insertSheet.setColumnWidth(11, 50 * 100);
        insertSheet.setColumnWidth(12, 50 * 100);
        insertSheet.setColumnWidth(13, 50 * 100);
        insertSheet.setColumnWidth(14, 50 * 100);
        insertSheet.setColumnWidth(15, 100 * 100);

        insertSheet.setColumnHidden(0, true);

        // Create 100 Empty Rows
        for (int i = y; i < 100 + y; i++) {

          XSSFRow row = insertSheet.createRow(i);
          CellStyle editableStyle = workbook.createCellStyle();
          editableStyle.setLocked(false);
          insertSheet.setDefaultColumnStyle(i, editableStyle);
          for (int j = 0; j <= 15; j++) {
            XSSFCell cell = row.createCell(j);
            evaluator.evaluateFormulaCell(cell);
            if (j == 3 || j == 4 || j == 13 || j == 14 || j == 15)
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