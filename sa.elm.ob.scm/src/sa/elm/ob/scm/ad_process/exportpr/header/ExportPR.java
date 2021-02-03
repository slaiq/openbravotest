package sa.elm.ob.scm.ad_process.exportpr.header;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
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
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.finance.ad_process.ExportBudget.util.XSSFExcelStyles;
import sa.elm.ob.scm.properties.Resource;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.UtilityDAO;

public class ExportPR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public static final String tabId = "800251";
  public static final String lineNo = "1004400099";
  public static final String parentLineNo = "63FC6590F6B24EF8912EDF513454073A";
  public static final String itemCode = "803838";
  public static final String productCtgory = "A3600660D3A54316AE1A229BB61A2C3C";
  public static final String uom = "1004400002";
  public static final String qty = "803839";
  public static final String unitPrice = "1004400080";
  public static final String needByDate = "1004400102";
  public static final String notes = "1004400025";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    try {
      VariablesSecureApp vars = new VariablesSecureApp(request);

      Enumeration<String> enums = vars.getParameterNames();
      while (enums.hasMoreElements()) {
        String param = enums.nextElement();
        log4j.debug(param);
      }

      // Input Param
      String reqId = vars.getStringParameter("M_Requisition_ID");
      log4j.debug(" reqId ===>  " + reqId);

      // Get Requisition Details
      Requisition req = OBDal.getInstance().get(Requisition.class, reqId);

      String reqSpecNo = req.getDocumentNo();

      OBContext.setAdminMode();
      Window windowobj = OBDal.getInstance().get(Window.class, Constants.PURCHASE_REQUISITION_W);
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
      String file = reqSpecNo + windowName;
      file = URLEncoder.encode(file, "UTF-8").replaceAll("\\+", " ");

      String filedir = globalParameters.strFTPDirectory + "/";
      // if (req.getDocumentStatus().equals("DR")) {
      @SuppressWarnings("unused")
      File excelFile = createExcel(filedir + file + "-.xlsx", req, vars);
      // }

      // Set Response Type and Send File in Servlet Output Stream
      response.setContentType("text/html; charset=UTF-8");
      response.setCharacterEncoding("UTF-8");
      response.addHeader("Content-Disposition", "inline; filename=" + file + "-.html");
      printPagePopUpDownload(response.getOutputStream(),
          URLEncoder.encode(file, "UTF-8") + "-.xlsx");

    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      log4j.error("Exception in Export PR Download : ", e);
    } catch (Exception e) {
      log4j.error("Exception in Export PR Download : ", e);
    }
  }

  public File createExcel(String filePath, Requisition req, VariablesSecureApp vars) {
    File file = null;
    try {
      OBContext.setAdminMode();
      String lang = vars.getLanguage();
      if (req != null) {
        String prNo = req.getDocumentNo();

        // Create File
        file = new File(filePath);

        // Create Workbook and Sheet
        XSSFWorkbook workbook = new XSSFWorkbook();

        OBContext.setAdminMode();
        Tab tabobj = OBDal.getInstance().get(Tab.class, Constants.PURCHASE_REQUISITION_LINES_TAB);
        String tabName = "";
        if (tabobj != null) {
          List<TabTrl> transList = tabobj.getADTabTrlList();
          if (transList.size() > 0) {
            tabName = "-" + transList.get(0).getName();
          }
        }
        XSSFSheet sheet = workbook.createSheet(prNo + tabName);

        if (!vars.getLanguage().equals("en_US")) {
          sheet.setRightToLeft(true);
        }

        // Create Styles
        XSSFExcelStyles excelStyles = new XSSFExcelStyles();
        Map<String, XSSFCellStyle> styles = excelStyles.createStyles(workbook);
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        List<Field> fieldList = null;
        HashMap<String, String> fieldMap = new HashMap<>();

        OBQuery<Field> fieldObj = OBDal.getInstance().createQuery(Field.class,
            "as e where e.tab.id=:tabId");
        fieldObj.setNamedParameter("tabId", tabId);
        fieldList = fieldObj.list();
        for (Field itr : fieldList) {
          fieldMap.put(itr.getId(), itr.getName());
        }

        // Create Header
        int noOfColumns = 9;
        if (vars.getLanguage().equals("en_US")) {
          // Create Header
          XSSFRow headerRow = sheet.createRow(0);
          for (int i = 0; i <= noOfColumns; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellStyle(styles.get("Header"));
            switch (i) {
            case 0:
              cell.setCellValue(
                  new XSSFRichTextString(fieldMap.containsKey(lineNo) ? fieldMap.get(lineNo) : "")
                      + "(*)");
              break;
            case 1:
              cell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(parentLineNo) ? fieldMap.get(parentLineNo) : ""));
              break;
            case 2:
              cell.setCellValue(
                  new XSSFRichTextString(Resource.getProperty("scm.item.code", lang)));// Category
              break;
            case 3:
              cell.setCellValue(
                  new XSSFRichTextString(Resource.getProperty("scm.item.name", lang)) + "(*)");// Item
                                                                                               // Code
              break;
            case 4:
              cell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(productCtgory) ? fieldMap.get(productCtgory) : ""));// Item
                                                                                           // Name
              break;
            case 5:
              cell.setCellValue(
                  new XSSFRichTextString(fieldMap.containsKey(uom) ? fieldMap.get(uom) : ""));
              break;
            case 6:
              cell.setCellValue(
                  new XSSFRichTextString(fieldMap.containsKey(qty) ? fieldMap.get(qty) : "")
                      + "(*)");
              break;
            case 7:
              cell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(unitPrice) ? fieldMap.get(unitPrice) : ""));
              break;
            case 8:
              cell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(needByDate) ? fieldMap.get(needByDate) : ""));
              break;
            case 9:
              cell.setCellValue(
                  new XSSFRichTextString(fieldMap.containsKey(notes) ? fieldMap.get(notes) : ""));
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

          XSSFRow headerRow = sheet.createRow(0);
          for (int i = 0; i <= noOfColumns; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellStyle(styles.get("Header"));
            switch (i) {
            case 0:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(lineNo) ? fieldTrlMap.get(lineNo) : fieldMap.get(lineNo))
                  + "(*)");
              break;
            case 1:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(parentLineNo) ? fieldTrlMap.get(parentLineNo)
                      : fieldMap.get(parentLineNo)));
              break;
            case 2:
              cell.setCellValue(
                  new XSSFRichTextString(Resource.getProperty("scm.item.code", lang)));// Category
              break;
            case 3:
              cell.setCellValue(
                  new XSSFRichTextString(Resource.getProperty("scm.item.name", lang)) + "(*)");// Item
                                                                                               // Code
              break;
            case 4:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(productCtgory) ? fieldTrlMap.get(productCtgory)
                      : fieldMap.get(productCtgory)));// Item
              // Name
              break;
            case 5:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(uom) ? fieldTrlMap.get(uom) : fieldMap.get(uom)));
              break;
            case 6:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(qty) ? fieldTrlMap.get(qty) : fieldMap.get(qty)) + "(*)");
              break;
            case 7:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(unitPrice) ? fieldTrlMap.get(unitPrice)
                      : fieldMap.get(unitPrice)));
              break;
            case 8:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(needByDate) ? fieldTrlMap.get(needByDate)
                      : fieldMap.get(notes)));
              break;
            case 9:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(notes) ? fieldTrlMap.get(notes) : fieldMap.get(notes)));
              break;
            }
          }

        }

        int contentStartLine = 1;

        // Get Purchase Requisition Line List
        String whereClause = " e where e.requisition.id = :reqId  order by e.lineNo asc";
        OBQuery<RequisitionLine> reqLineQry = OBDal.getInstance().createQuery(RequisitionLine.class,
            whereClause);
        reqLineQry.setNamedParameter("reqId", req.getId());
        List<RequisitionLine> ReqLneList = reqLineQry.list();
        int lineCount = ReqLneList.size();
        log4j.debug("Line Count " + lineCount);
        String lineno, mainItem, category, itemCode, itemName, uom, needbyDate, notes;
        BigDecimal qty = BigDecimal.ZERO, unitPrice;
        if (lineCount > 0) {
          // Create Line Details Entry
          OBContext.setAdminMode();
          for (RequisitionLine reqLne : ReqLneList) {
            lineno = reqLne.getLineNo().toString();
            mainItem = reqLne.getEscmParentlineno() != null
                ? reqLne.getEscmParentlineno().getLineNo().toString()
                : "";
            category = reqLne.getEscmProdcate() != null ? reqLne.getEscmProdcate().getSearchKey()
                : "";
            itemCode = reqLne.getProduct() != null ? reqLne.getProduct().getSearchKey() : "";
            itemName = reqLne.getDescription();
            uom = reqLne.getUOM() != null ? reqLne.getUOM().getName() : "";
            qty = reqLne.getQuantity();

            unitPrice = new BigDecimal("0");
            if (reqLne.getUnitPrice() != null)
              unitPrice = reqLne.getUnitPrice();

            needbyDate = "";
            if (reqLne.getNeedByDate() != null) {
              DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss");
              String gregorianDate = dateFormat.format(reqLne.getNeedByDate());
              needbyDate = UtilityDAO.convertTohijriDate(gregorianDate);
            }

            notes = reqLne.getNotesForSupplier();
            XSSFRow row = sheet.createRow(contentStartLine);
            for (int i = 0; i <= noOfColumns; i++) {
              XSSFCell cell = row.createCell(i);
              switch (i) {
              case 0:
                cell.setCellValue(new XSSFRichTextString(lineno));
                break;
              case 1:
                cell.setCellValue(new XSSFRichTextString(mainItem));
                break;
              case 2:
                cell.setCellValue(new XSSFRichTextString(itemCode));
                cell.setCellStyle(styles.get("TextUnlockFormat"));
                break;
              case 3:
                cell.setCellValue(new XSSFRichTextString(itemName));
                cell.setCellStyle(styles.get("TextUnlockFormat"));
                break;
              case 4:
                cell.setCellValue(new XSSFRichTextString(category));
                break;
              case 5:
                cell.setCellValue(new XSSFRichTextString(uom));
                break;
              case 6:
                cell.setCellValue(qty.doubleValue());
                break;
              case 7:
                cell.setCellValue(unitPrice.doubleValue());
                break;
              case 8:
                cell.setCellValue(new XSSFRichTextString(needbyDate));
                break;
              case 9:
                cell.setCellValue(new XSSFRichTextString(notes));
                cell.setCellStyle(styles.get("TextUnlockFormat"));
                break;
              }
            }

            log4j.debug(" Created Line ===>  " + contentStartLine + " "
                + (reqLne.getProduct() != null ? reqLne.getProduct().getName()
                    : reqLne.getDescription()));

            contentStartLine++;
          }
        }

        // Set Column Width
        for (int i = 0; i <= noOfColumns; i++) {
          if (i != 4) {
            sheet.setColumnWidth(i, 50 * 100);
          } else {
            sheet.setColumnWidth(i, 75 * 100);
          }
        }
        // Create 100 Empty Rows
        for (int i = contentStartLine; i < 100 + contentStartLine; i++) {

          XSSFRow row = sheet.createRow(i);
          CellStyle editableStyle = workbook.createCellStyle();
          editableStyle.setLocked(false);
          sheet.setDefaultColumnStyle(i, editableStyle);
          for (int j = 0; j < noOfColumns; j++) {
            XSSFCell cell = row.createCell(j);
            evaluator.evaluateFormulaCell(cell);
            if (j == 2 || j == 3 || j == 9)
              cell.setCellStyle(styles.get("TextUnlockFormat"));
          }
        }

        // Write workbook in file
        FileOutputStream fileOut = new FileOutputStream(filePath);
        workbook.write(fileOut);
        fileOut.close();
      }
    } catch (final Exception e) {
      log4j.error("Exception in PR createExcel() Method : ", e);
      return null;
    }
    return file;
  }

}