package sa.elm.ob.scm.ad_process.exportporeceipt.header;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.TabTrl;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.ui.WindowTrl;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;
import org.springframework.util.StringUtils;

import sa.elm.ob.finance.ad_process.ExportBudget.util.XSSFExcelStyles;
import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.properties.Resource;
import sa.elm.ob.utility.util.Constants;

public class ExportPOReceipt extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public static final String tabId = "2A8F52E5BF1846B2BFBDAAFEF6F89135";
  public static final String lineNo = "641DF3EE5B7048BFA4AA11392AEF1A4A";

  public static final String uom = "8CC62FFAB41148A0B8D881CBE19FF402";
  public static final String qtyOrdered = "56B98FF11250435F8F70C2D142CE582F";
  public static final String receivedAmount = "BBCEB64404F243BCA0C1211F74EC2584";
  public static final String receivedQty = "9FBC51B5F82F4A139FEE71DB2D00CCFC";

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
      String inOutId = vars.getStringParameter("M_InOut_ID");
      log4j.debug(" inOutId ===>  " + inOutId);

      // Get Inout Details
      ShipmentInOut inOut = OBDal.getInstance().get(ShipmentInOut.class, inOutId);

      if (!StringUtils.isEmpty(inOut.getSalesOrder())) {
        String inOutSpecNo = inOut.getDocumentNo();

        OBContext.setAdminMode();
        Window windowobj = OBDal.getInstance().get(Window.class, Constants.PO_RECEIPT_W);
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
        String file = inOutSpecNo + windowName;
        file = URLEncoder.encode(file, "UTF-8").replaceAll("\\+", " ");

        String filedir = globalParameters.strFTPDirectory + "/";
        @SuppressWarnings("unused")
        File excelFile = createExcel(filedir + file + "-.xlsx", inOut, vars);

        // Set Response Type and Send File in Servlet Output Stream
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Content-Disposition", "inline; filename=" + file + "-.html");
        printPagePopUpDownload(response.getOutputStream(),
            URLEncoder.encode(file, "UTF-8") + "-.xlsx");
      } else {
        advisePopUpRefresh(request, response, "ERROR", "ERROR",
            OBMessageUtils.messageBD("Escm_Export_PORecpt_Err"));
      }
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      log4j.error("Exception in Download : ", e);
    } catch (Exception e) {
      log4j.error("Exception in Download : ", e);
    }
  }

  public File createExcel(String filePath, ShipmentInOut inOut, VariablesSecureApp vars) {
    File file = null;
    try {
      OBContext.setAdminMode();
      String lang = vars.getLanguage();
      if (inOut.getSalesOrder() != null) {
        log4j.debug(inOut.getSalesOrder());
        log4j.debug(inOut.getSalesOrder().getId());

        Order salesOrder = OBDal.getInstance().get(Order.class, inOut.getSalesOrder().getId());

        String salesOrderNo = salesOrder.getDocumentNo();
        Boolean hasLines = inOut.getEscmInitialReceiptList().size() > 0 ? true : false;
        List<EscmInitialReceipt> lineList = inOut.getEscmInitialReceiptList();
        HashMap<String, BigDecimal> qtyReceivedMap = new HashMap<>();
        HashMap<String, BigDecimal> amountReceivedMap = new HashMap<>();

        for (EscmInitialReceipt receipt : lineList) {
          if (receipt.getSalesOrderLine() != null) {
            qtyReceivedMap.put(receipt.getSalesOrderLine().getId(), receipt.getQuantity());
            amountReceivedMap.put(receipt.getSalesOrderLine().getId(), receipt.getReceivedAmount());
          }
        }

        // Create File
        file = new File(filePath);

        // Create Workbook and Sheet
        XSSFWorkbook workbook = new XSSFWorkbook();
        OBContext.setAdminMode();
        Tab tabobj = OBDal.getInstance().get(Tab.class, Constants.PURCHASE_ORDER_LINES_TAB);
        String tabName = "";
        if (tabobj != null) {
          List<TabTrl> transList = tabobj.getADTabTrlList();
          if (transList.size() > 0) {
            tabName = "-" + transList.get(0).getName();
          }
        }
        XSSFSheet sheet = workbook.createSheet(salesOrderNo + tabName);

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

        // Create Styles
        XSSFExcelStyles excelStyles = new XSSFExcelStyles();
        Map<String, XSSFCellStyle> styles = excelStyles.createStyles(workbook);

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
          XSSFRow headerRow = sheet.createRow(0);
          for (int i = 0; i <= noOfColumns; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellStyle(styles.get("Header"));
            switch (i) {
            case 0:
              cell.setCellValue(new XSSFRichTextString("orderLineId"));
              break;
            case 1:
              cell.setCellValue(new XSSFRichTextString("IsSummary"));
              break;
            case 2:
              cell.setCellValue(
                  new XSSFRichTextString(fieldMap.containsKey(lineNo) ? fieldMap.get(lineNo) : ""));
              break;
            case 3:
              cell.setCellValue(new XSSFRichTextString(
                  Resource.getProperty("scm.poreceipt.item.itemdescription", lang)));
              break;
            case 4:
              cell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(qtyOrdered) ? fieldMap.get(qtyOrdered) : ""));
              break;
            case 5:
              cell.setCellValue(
                  new XSSFRichTextString(fieldMap.containsKey(uom) ? fieldMap.get(uom) : ""));
              break;
            case 6:
              cell.setCellValue(
                  new XSSFRichTextString(Resource.getProperty("scm.poreceipt.unitprice", lang)));
              break;
            case 7:
              cell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(receivedQty) ? fieldMap.get(receivedQty) : ""));
              break;
            case 8:
              cell.setCellValue(new XSSFRichTextString(
                  fieldMap.containsKey(receivedAmount) ? fieldMap.get(receivedAmount) : ""));
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
              cell.setCellValue(new XSSFRichTextString("orderLineId"));
              break;
            case 1:
              cell.setCellValue(new XSSFRichTextString("IsSummary"));
              break;
            case 2:
              cell.setCellValue(
                  new XSSFRichTextString(fieldTrlMap.containsKey(lineNo) ? fieldTrlMap.get(lineNo)
                      : fieldMap.get(lineNo)));
              break;
            case 3:
              cell.setCellValue(new XSSFRichTextString(
                  Resource.getProperty("scm.poreceipt.item.itemdescription", lang)));
              break;
            case 4:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(qtyOrdered) ? fieldTrlMap.get(qtyOrdered)
                      : fieldMap.get(qtyOrdered)));
              break;
            case 5:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(uom) ? fieldTrlMap.get(uom) : fieldMap.get(uom)));
              break;
            case 6:
              cell.setCellValue(
                  new XSSFRichTextString(Resource.getProperty("scm.poreceipt.unitprice", lang)));
              break;
            case 7:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(receivedQty) ? fieldTrlMap.get(receivedQty)
                      : fieldMap.get(receivedQty)));
              break;
            case 8:
              cell.setCellValue(new XSSFRichTextString(
                  fieldTrlMap.containsKey(receivedAmount) ? fieldTrlMap.get(receivedAmount)
                      : fieldMap.get(receivedAmount)));
              break;
            }
          }

        }

        int contentStartLine = 1;

        List<OrderLine> OrderLneList = null;

        if (hasLines) {
          // Get Po receipt Line List
          String whereClause = " e where e.salesOrder.id = :orderId  "
              + "and e.id in (select a.salesOrderLine.id from Escm_InitialReceipt a where a.salesOrderLine!=null and a.goodsShipment.id =:shipmentid) order by e.lineNo asc";
          OBQuery<OrderLine> orderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
              whereClause);
          orderLineQry.setNamedParameter("orderId", salesOrder.getId());
          orderLineQry.setNamedParameter("shipmentid", inOut.getId());
          OrderLneList = orderLineQry.list();
        } else {
          // Get Purchase Order Line List
          String whereClause = " e where e.salesOrder.id = :orderId order by e.lineNo asc";
          OBQuery<OrderLine> orderLineQry = OBDal.getInstance().createQuery(OrderLine.class,
              whereClause);
          orderLineQry.setNamedParameter("orderId", salesOrder.getId());
          OrderLneList = orderLineQry.list();
        }

        int lineCount = OrderLneList.size();
        log4j.debug("Line Count " + lineCount);

        if (lineCount > 0) {
          // Create Line Details Entry
          for (OrderLine orderLne : OrderLneList) {
            HashMap<Integer, String> cellMap = getPoCellStyle();
            XSSFRow row = sheet.createRow(contentStartLine);
            for (int i = 0; i <= noOfColumns; i++) {
              XSSFCell cell = row.createCell(i);
              if (!orderLne.isEscmIssummarylevel()) {
                cell.setCellStyle(styles.get(cellMap.get(i)));
              }
              switch (i) {
              case 0:
                cell.setCellValue(new XSSFRichTextString(orderLne.getId()));
                break;
              case 1:
                cell.setCellValue(
                    new XSSFRichTextString(orderLne.isEscmIssummarylevel() ? "Y" : "N"));
                break;
              case 2:
                cell.setCellValue(new XSSFRichTextString(orderLne.getLineNo().toString()));
                break;
              case 3:
                cell.setCellValue(new XSSFRichTextString(
                    orderLne.getProduct() != null ? orderLne.getProduct().getName()
                        : orderLne.getEscmProdescription()));
                break;
              case 4:
                cell.setCellValue(new XSSFRichTextString(orderLne.getOrderedQuantity().toString()));
                break;
              case 5:
                cell.setCellValue(new XSSFRichTextString(orderLne.getUOM().getName()));
                break;
              case 6:
                cell.setCellValue(new XSSFRichTextString(orderLne.getUnitPrice().toString()));
                break;
              case 7:
                if (hasLines) {

                  cell.setCellValue(
                      new XSSFRichTextString(qtyReceivedMap.get(orderLne.getId()).toString()));
                  break;
                } else {
                  cell.setCellValue(new XSSFRichTextString("0"));
                  break;
                }
              case 8:
                if (hasLines) {
                  cell.setCellValue(
                      new XSSFRichTextString(amountReceivedMap.get(orderLne.getId()).toString()));
                  break;
                } else {
                  cell.setCellValue(new XSSFRichTextString("0"));
                  break;
                }
              }
            }

            log4j.debug(" Created Line ===>  " + contentStartLine + " "
                + (orderLne.getProduct() != null ? orderLne.getProduct().getName()
                    : orderLne.getEscmProdescription()));

            contentStartLine++;
          }
        }

        // Set Column Width
        sheet.setColumnWidth(2, 50 * 100);
        sheet.setColumnWidth(3, 75 * 100);
        sheet.setColumnWidth(4, 50 * 100);
        sheet.setColumnWidth(5, 50 * 100);
        sheet.setColumnWidth(6, 50 * 100);
        sheet.setColumnWidth(7, 50 * 100);
        sheet.setColumnWidth(8, 50 * 100);
        sheet.setColumnHidden(0, true);
        sheet.setColumnHidden(1, true);

        // Write workbook in file
        FileOutputStream fileOut = new FileOutputStream(filePath);
        workbook.write(fileOut);
        fileOut.close();
      }
    } catch (final Exception e) {
      log4j.error("Exception in createExcel() Method : ", e);
      return null;
    }
    return file;
  }

  public HashMap<Integer, String> getPoCellStyle() {
    HashMap<Integer, String> txtStyleMap = new HashMap<>();
    try {

      for (int i = 0; i <= 9; i++) {
        if (i == 2 || i == 3) {
          txtStyleMap.put(i, "TextLock");
        }
        if (i == 4 || i == 6 || i == 8 || i == 7) {
          txtStyleMap.put(i, "NumericUnlock");
        }
      }

    } catch (final Exception e) {
      log4j.error("Exception in getPoCellStyle() Method : ", e);
    }
    return txtStyleMap;
  }

}