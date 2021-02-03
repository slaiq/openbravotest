package sa.elm.ob.scm.ad_process.importporeceipt.dao;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.springframework.util.StringUtils;

import sa.elm.ob.scm.ESCMCSVPOReceiptImport;
import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.EscmInitialreceiptView;
import sa.elm.ob.utility.util.Utility;

public class ImportPOReceiptDAO {

  private static final Logger log4j = Logger.getLogger(ImportPOReceiptDAO.class);
  @SuppressWarnings("unused")
  private Connection conn = null;
  DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  Date currentDate = new Date();
  String formattedCurrentDateStr = dateFormat.format(currentDate);

  public ImportPOReceiptDAO(Connection con) {
    this.conn = con;
  }

  public JSONObject processUploadedCsvFile(String inpInOutId, VariablesSecureApp vars) {
    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;
    ShipmentInOut inOut = OBDal.getInstance().get(ShipmentInOut.class, inpInOutId);
    try {
      OBContext.setAdminMode(true);

      isSuccess = addOrUpdatePOReceiptLines(inOut);
      OBDal.getInstance().flush();

      log4j.debug("addOrUpdatePOReceiptLines isSuccess" + isSuccess);

      if (isSuccess == 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("statusMessage", OBMessageUtils.messageBD("Escm_Import_Record_Failed"));
      } else {
        jsonresult = new JSONObject();
        jsonresult.put("status", "1");
        jsonresult.put("statusMessage", OBMessageUtils.messageBD("Escm_Import_Record_Success"));
      }

    } catch (Exception e) {
      jsonresult = new JSONObject();
      try {
        jsonresult.put("status", "1");
        jsonresult.put("statusMessage", e.getMessage());
      } catch (JSONException e1) {
        log4j.error("Exception in processUploadedCsvFile ", e1);
      }
      log4j.error("Exception in processUploadedCsvFile ", e);
    } finally {
      deleteCSVImportEntries();
      OBContext.restorePreviousMode();
    }
    return jsonresult;
  }

  private long getLatestLineNo(ShipmentInOut inOutId) {
    long lineno = 10;
    try {
      List<EscmInitialReceipt> receiptList = new ArrayList<EscmInitialReceipt>();
      OBQuery<EscmInitialReceipt> linesQry = OBDal.getInstance().createQuery(
          EscmInitialReceipt.class,
          "as e where e.goodsShipment.id=:inoutID order by e.lineNo desc");
      linesQry.setNamedParameter("inoutID", inOutId.getId());
      linesQry.setMaxResult(1);
      receiptList = linesQry.list();
      if (receiptList.size() > 0) {
        EscmInitialReceipt objExistLine = receiptList.get(0);
        lineno = objExistLine.getLineNo() + 10;
      }
      return lineno;
    } catch (Exception e) {
      log4j.error("Error in ImportPOReceipt.java : getLatestLineNo() ", e);
      return lineno;
    }
  }

  private EscmInitialReceipt checkLineExistsInInitialReceipt(ShipmentInOut inOutId,
      OrderLine orderLine) {
    EscmInitialReceipt initalReceiptLne = null;
    try {
      List<EscmInitialReceipt> receiptList = new ArrayList<EscmInitialReceipt>();
      OBQuery<EscmInitialReceipt> linesQry = OBDal.getInstance()
          .createQuery(EscmInitialReceipt.class, "as e where e.goodsShipment.id=:inOutId and "
              + " e.salesOrderLine.id=:orderLnId order by e.lineNo desc");
      linesQry.setNamedParameter("inOutId", inOutId.getId());
      linesQry.setNamedParameter("orderLnId", orderLine.getId());
      linesQry.setMaxResult(1);
      receiptList = linesQry.list();
      if (receiptList.size() > 0) {
        initalReceiptLne = receiptList.get(0);
      }
      return initalReceiptLne;
    } catch (Exception e) {
      log4j.error("Error in ImportPOReceipt.java : checkLineExistsInInitialReceipt() ", e);
      return initalReceiptLne;
    }
  }

  private int addOrUpdatePOReceiptLines(ShipmentInOut inOut) {
    try {
      String receiveType = inOut.getEscmReceivetype();

      // GetLatestLineNo
      long lineno = getLatestLineNo(inOut);

      OBQuery<ESCMCSVPOReceiptImport> csvImportQry = OBDal.getInstance()
          .createQuery(ESCMCSVPOReceiptImport.class, "order by creationDate");
      List<ESCMCSVPOReceiptImport> csvImportList = csvImportQry.list();

      for (ESCMCSVPOReceiptImport csvImportLne : csvImportList) {
        EscmInitialReceipt ParentInitalReceiptLne = null; // For immediate parent
        EscmInitialReceipt ParentListInitalReceiptLne = null; // For super parents
        boolean hasNewParent = false;
        BigDecimal percentageAchieved = BigDecimal.ZERO;
        long delayDays = 0;

        // Inputs
        boolean isSummaryLevel = csvImportLne.getSalesOrderLine().isEscmIssummarylevel();
        BigDecimal receivedQty = csvImportLne.getReceivedQuantity();
        BigDecimal receivedAmt = csvImportLne.getReceivedAmount();
        OrderLine orderLine = csvImportLne.getSalesOrderLine();

        if (!isSummaryLevel
            && ((receiveType.equalsIgnoreCase("QTY"))
                && (receivedQty.compareTo(BigDecimal.ZERO) > 0))
            || ((receiveType.equalsIgnoreCase("AMT"))
                && (receivedAmt.compareTo(BigDecimal.ZERO) > 0))) {

          if (receiveType.equalsIgnoreCase("AMT")) {
            percentageAchieved = calculatePercentageAchieved(receivedAmt,
                orderLine.getLineNetAmount());
          }

          // check line already exists in initial receipt if exists update the quantity else insert
          EscmInitialReceipt existingInitalReceiptLne = checkLineExistsInInitialReceipt(inOut,
              orderLine);

          if (existingInitalReceiptLne != null) {
            if (receiveType.equalsIgnoreCase("AMT")) {
              existingInitalReceiptLne.setQuantity(new BigDecimal(1));
              existingInitalReceiptLne.setReceivedAmount(receivedAmt);
              existingInitalReceiptLne.setTOTLineAmt(receivedAmt);
              existingInitalReceiptLne.setPercentageAchieved(percentageAchieved);
            } else {
              existingInitalReceiptLne.setQuantity(receivedQty);
            }
            OBDal.getInstance().save(existingInitalReceiptLne);
            OBDal.getInstance().flush();
          } else {

            // Check order line has parent
            if (orderLine.getEscmParentline() != null) {
              ParentInitalReceiptLne = null;

              // List to hold all parents and insert in Initial Receipt table in an order
              List<OrderLine> parentlist = new ArrayList<OrderLine>();
              parentlist.clear();

              // Check the parent already exists in initial receipt
              OrderLine orderLineParent = OBDal.getInstance().get(OrderLine.class,
                  orderLine.getEscmParentline().getId());
              ParentInitalReceiptLne = checkLineExistsInInitialReceipt(inOut, orderLineParent);

              // If parent initial receipt does not exists, add to parent list to insert new
              if (ParentInitalReceiptLne == null) {
                hasNewParent = true;
                parentlist.add(orderLineParent);
                // Loop through all super parents till the parent has initial receipt entry or no
                // more super parents and add to parent list to insert new
                while (hasNewParent) {
                  if (orderLineParent.getEscmParentline() != null) {
                    orderLineParent = OBDal.getInstance().get(OrderLine.class,
                        orderLineParent.getEscmParentline().getId());
                    ParentInitalReceiptLne = checkLineExistsInInitialReceipt(inOut,
                        orderLineParent);

                    // If parent initial receipt does not exists, add to parent list to insert new
                    if (ParentInitalReceiptLne == null) {
                      hasNewParent = true;
                      parentlist.add(orderLineParent);
                    } else {
                      hasNewParent = false;
                    }
                  } else {
                    hasNewParent = false;
                  }
                }

                // Insert all parent into Initial Receipt in an order(Iterate in reverse)
                ListIterator<OrderLine> li = parentlist.listIterator(parentlist.size());
                while (li.hasPrevious()) {
                  ParentListInitalReceiptLne = null;
                  OrderLine line = li.previous();

                  // Get InitalReceipt Parent Reference if parent exists
                  if (line.getEscmParentline() != null) {
                    ParentListInitalReceiptLne = checkLineExistsInInitialReceipt(inOut, line);
                  }

                  EscmInitialReceipt newParent = OBProvider.getInstance()
                      .get(EscmInitialReceipt.class);
                  newParent.setGoodsShipment(inOut);
                  if (receiveType.equalsIgnoreCase("AMT")) {
                    newParent.setQuantity(new BigDecimal(1));
                    newParent.setReceivedAmount(receivedAmt);
                    newParent.setTOTLineAmt(receivedAmt);
                    // Parent do not have percentage achieved, Execution Dates and Days.
                  } else {
                    newParent.setQuantity(receivedQty);
                  }
                  newParent.setOrganization(inOut.getOrganization());
                  newParent.setClient(inOut.getClient());
                  newParent.setAlertStatus("A");
                  newParent.setUnitprice(line.getUnitPrice());
                  newParent.setManual(false);
                  if (line.getProduct() != null) {
                    newParent.setProduct(line.getProduct());
                    newParent.setImage(line.getProduct().getImage());
                  } else {
                    newParent.setProduct(null);
                    newParent.setImage(null);
                  }
                  newParent.setSummaryLevel(true);
                  if (ParentListInitalReceiptLne != null) {
                    newParent.setParentLine(OBDal.getInstance().get(EscmInitialreceiptView.class,
                        ParentListInitalReceiptLne.getId()));
                  }
                  newParent.setSourceRef(null);
                  newParent.setDescription(line.getEscmProdescription());
                  newParent.setLineNo(lineno);
                  newParent.setNotes("");
                  newParent.setUOM(line.getUOM());
                  newParent.setSalesOrderLine(line);
                  newParent
                      .setDeliverydate(dateFormat.parse(dateFormat.format(new java.util.Date())));
                  OBDal.getInstance().save(newParent);
                  OBDal.getInstance().flush();

                  // Keep inserted initial receipt parent object for parent reference in main line
                  ParentInitalReceiptLne = newParent;

                  lineno = lineno + 10;
                }
              }
            }

            // Insert main line
            EscmInitialReceipt newObject = OBProvider.getInstance().get(EscmInitialReceipt.class);
            newObject.setGoodsShipment(inOut);
            if (receiveType.equalsIgnoreCase("AMT")) {
              newObject.setQuantity(new BigDecimal(1));
              newObject.setReceivedAmount(receivedAmt);
              newObject.setTOTLineAmt(receivedAmt);
              newObject.setPercentageAchieved(percentageAchieved);
              newObject.setEXEStartDateH(currentDate);
              newObject.setEXEEndDateH(currentDate);
              newObject.setEXEStartDateG(formattedCurrentDateStr);
              newObject.setEXEEndDateG(formattedCurrentDateStr);
              newObject.setContractExeDays(1L);
              if (!StringUtils.isEmpty(orderLine.getEscmNeedbydate())) {
                delayDays = calculateDelayDays(currentDate, orderLine.getEscmNeedbydate());
              }
              newObject.setContractDelayDays(delayDays);
            } else {
              newObject.setQuantity(receivedQty);
            }
            newObject.setOrganization(inOut.getOrganization());
            newObject.setClient(inOut.getClient());
            newObject.setAlertStatus("A");
            newObject.setUnitprice(orderLine.getUnitPrice());
            newObject.setManual(false);
            if (orderLine.getProduct() != null) {
              newObject.setProduct(orderLine.getProduct());
              newObject.setImage(orderLine.getProduct().getImage());
            } else {
              newObject.setProduct(null);
              newObject.setImage(null);
            }
            newObject.setSummaryLevel(false);
            if (ParentInitalReceiptLne != null) {
              newObject.setParentLine(OBDal.getInstance().get(EscmInitialreceiptView.class,
                  ParentInitalReceiptLne.getId()));
            }
            newObject.setSourceRef(null);
            newObject.setDescription(orderLine.getEscmProdescription());
            newObject.setLineNo(lineno);
            newObject.setNotes("");
            newObject.setUOM(orderLine.getUOM());
            newObject.setSalesOrderLine(orderLine);
            newObject.setDeliverydate(dateFormat.parse(dateFormat.format(new java.util.Date())));
            OBDal.getInstance().save(newObject);
            OBDal.getInstance().flush();

            lineno = lineno + 10;
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Error in ImportPOReceipt.java : addOrUpdatePOReceiptLines() ", e);
      return 0;
    }
    return 1;
  }

  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars, String inpInOutId,
      boolean isUpload) {
    JSONObject resultJSON = new JSONObject();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;

    try {
      OBContext.setAdminMode(true);

      boolean validFile = true;

      StringBuffer resultMessage = new StringBuffer();
      StringBuffer numericErrorDetails = new StringBuffer();
      StringBuffer negativeErrorDetails = new StringBuffer();
      StringBuffer orderLineNotExistsDetails = new StringBuffer();
      StringBuffer dueErrorDetails = new StringBuffer();

      boolean hasErrorMessage = false;
      boolean hasValidLine = false;

      ShipmentInOut inOut = OBDal.getInstance().get(ShipmentInOut.class, inpInOutId);
      String receiveType = inOut.getEscmReceivetype();

      // Get Work Book
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          if (validFile) {
            // Get Each Sheet
            sheet = xssfWorkbook.getSheetAt(s);
            if (sheet == null)
              break;

            // Initialize Data to be inserted in temporary table
            BigDecimal receivedQty = BigDecimal.ZERO;
            BigDecimal receivedAmt = BigDecimal.ZERO;

            int startRow = 1;
            int orderLine = 0;
            int summaryLevel = 1;
            int lineNoColumn = 2;
            int reveivedQtyColumn = 7;
            int reveivedAmtColumn = 8;
            Row row = null;
            Cell cell = null;
            String orderLineId = null;
            String lineNo = "";
            String cellVal = "";

            // Get Each Row
            for (int i = startRow;; i++) {
              boolean hasError = false;

              row = sheet.getRow(i);

              if (row == null)
                break;
              // summaryLevel
              // if summary level is "Y" then skip all the validations
              cell = row.getCell(summaryLevel);

              if (Utility.nullToEmpty(getCellValue(cell)).equals("N")) {

                // OrderLine
                cell = row.getCell(orderLine);
                if (cell != null) {
                  orderLineId = Utility.nullToEmpty(getCellValue(cell));
                } else {
                  validFile = false;
                  break;
                }

                // Line No
                cell = row.getCell(lineNoColumn);
                if (cell != null) {
                  lineNo = Utility.nullToEmpty(getCellValue(cell));
                } else {
                  validFile = false;
                  break;
                }

                // Received Qty
                cell = row.getCell(reveivedQtyColumn);
                if (cell != null) {
                  cellVal = getCellValue(cell);
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails.append(
                        OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo + " - "
                            + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                            + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(getCellValue(cell))) {
                    negativeErrorDetails.append(
                        OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo + " - "
                            + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                            + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    receivedQty = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                  }
                }

                // Received Amount
                cell = row.getCell(reveivedAmtColumn);
                if (cell != null) {
                  cellVal = getCellValue(cell);
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails.append(
                        OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo + " - "
                            + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedAmt")
                            + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(getCellValue(cell))) {
                    negativeErrorDetails.append(
                        OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo + " - "
                            + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedAmt")
                            + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    receivedAmt = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                  }
                }

                // Due value validation
                OrderLine orderLne = OBDal.getInstance().get(OrderLine.class, orderLineId);

                if (orderLne != null) {
                  if (receiveType.equalsIgnoreCase("AMT")) {
                    BigDecimal totalReceivedAmt = orderLne.getEscmAmtporec() != null
                        ? orderLne.getEscmAmtporec()
                        : BigDecimal.ZERO;
                    BigDecimal totalReturnedAmt = orderLne.getEscmAmtreturned() != null
                        ? orderLne.getEscmAmtreturned()
                        : BigDecimal.ZERO;
                    BigDecimal actualReceived = totalReceivedAmt.subtract(totalReturnedAmt);
                    BigDecimal dueAmt = orderLne.getLineNetAmount().subtract(actualReceived);

                    if (receivedAmt.compareTo(dueAmt) > 0) {
                      dueErrorDetails.append(
                          String.format(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_DueAmt"),
                              receivedAmt, dueAmt, lineNo));
                      dueErrorDetails.append("<br>");
                    }
                  } else {
                    BigDecimal totalReceivedQty = orderLne.getEscmQtyporec() != null
                        ? orderLne.getEscmQtyporec()
                        : BigDecimal.ZERO;
                    BigDecimal totalRejectedQty = orderLne.getEscmQtyrejected() != null
                        ? orderLne.getEscmQtyrejected()
                        : BigDecimal.ZERO;
                    BigDecimal totalInitiReturnedQty = orderLne.getEscmQtyirr() != null
                        ? orderLne.getEscmQtyirr()
                        : BigDecimal.ZERO;
                    BigDecimal totalReturnedQty = orderLne.getEscmQtyreturned() != null
                        ? orderLne.getEscmQtyreturned()
                        : BigDecimal.ZERO;
                    BigDecimal totalCancelledQty = orderLne.getEscmQtycanceled() != null
                        ? orderLne.getEscmQtycanceled()
                        : BigDecimal.ZERO;
                    BigDecimal actualReceivedQty = totalReceivedQty.subtract(totalRejectedQty)
                        .subtract(totalInitiReturnedQty).subtract(totalReturnedQty);
                    BigDecimal dueQty = orderLne.getOrderedQuantity().subtract(actualReceivedQty)
                        .subtract(totalCancelledQty);

                    if (receivedQty.compareTo(dueQty) > 0) {
                      dueErrorDetails.append(
                          String.format(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_DueQty"),
                              receivedQty, dueQty, lineNo));
                      dueErrorDetails.append("<br>");
                    }
                  }

                  // If no error, insert into temporary table
                  if (!hasError) {
                    if (!orderLne.isEscmIssummarylevel()
                        && ((receiveType.equalsIgnoreCase("QTY"))
                            && (receivedQty.compareTo(BigDecimal.ZERO) > 0))
                        || ((receiveType.equalsIgnoreCase("AMT"))
                            && (receivedAmt.compareTo(BigDecimal.ZERO) > 0))) {
                      ESCMCSVPOReceiptImport csvImport = OBProvider.getInstance()
                          .get(ESCMCSVPOReceiptImport.class);
                      csvImport.setSalesOrderLine(orderLne);
                      if (receiveType.equalsIgnoreCase("AMT")) {
                        csvImport.setReceivedAmount(receivedAmt);
                      } else {
                        csvImport.setReceivedQuantity(receivedQty);
                      }
                      OBDal.getInstance().save(csvImport);
                      OBDal.getInstance().flush();
                      hasValidLine = true;
                    }
                  }
                } else {
                  validFile = false;
                  break;
                }
              }
            }
          }

          if (validFile) {
            // Numeric Errors
            if (numericErrorDetails.toString().length() > 0) {
              resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_NonNumeric"));
              resultMessage.append("<br>");
              resultMessage.append(numericErrorDetails.toString());
              hasErrorMessage = true;
            }

            // Negative Errors
            if (negativeErrorDetails.toString().length() > 0) {
              resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_Negative"));
              resultMessage.append("<br>");
              resultMessage.append(negativeErrorDetails.toString());
              hasErrorMessage = true;
            }

            // Order Line Errors
            if (orderLineNotExistsDetails.toString().length() > 0) {
              resultMessage
                  .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_InvalidOrderLine"));
              resultMessage.append("<br>");
              resultMessage.append(orderLineNotExistsDetails.toString());
              hasErrorMessage = true;
            }

            // Due Qty Erros
            if (dueErrorDetails.toString().length() > 0) {
              resultMessage.append(dueErrorDetails.toString());
              hasErrorMessage = true;
            }

            if (!hasErrorMessage) {
              if (!checkValidOrderLines(inOut)) {
                resultMessage
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_DiffOrderLine"));
                hasErrorMessage = true;
              } else if (!hasValidLine) {
                resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_NoValue"));
                resultMessage.append("<br>");
                resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_ErrNote"));
                hasErrorMessage = true;
              }
            }
          } else {
            // Invalid Files
            resultMessage.append(OBMessageUtils.messageBD("Escm_newline_notallowed"));
            hasErrorMessage = true;
          }

          if (hasErrorMessage) {
            resultJSON.put("status", "0");
            resultJSON.put("statusMessage", resultMessage.toString());
            log4j.debug("Validation Failed");
          } else {
            resultJSON.put("status", "1");
            resultJSON.put("statusMessage", OBMessageUtils.messageBD("Escm_CSV_ValidatedSuccess"));
            log4j.debug("Validation Success");
          }
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in ImportPOReceipt() : ", e);
      return null;
    } finally {
      if (!isUpload) {
        deleteCSVImportEntries();
        log4j.debug("Deleted CSV Import Entry");
      }
      OBContext.restorePreviousMode();
    }
    return resultJSON;
  }

  public void deleteCSVImportEntries() {
    try {
      OBQuery<ESCMCSVPOReceiptImport> csvImportQry = OBDal.getInstance()
          .createQuery(ESCMCSVPOReceiptImport.class, "order by creationDate");
      List<ESCMCSVPOReceiptImport> csvImportList = csvImportQry.list();
      for (ESCMCSVPOReceiptImport csvimprt : csvImportList) {
        OBDal.getInstance().remove(csvimprt);
      }
    } catch (final Exception e) {
      log4j.error("Exception in deleteCSVImportEntries() : ", e);
    }
  }

  private BigDecimal calculatePercentageAchieved(BigDecimal actual, BigDecimal total) {
    BigDecimal percentageAchieved = BigDecimal.ZERO;
    try {
      percentageAchieved = (actual.divide(total)).multiply(new BigDecimal("100"));
      return percentageAchieved;
    } catch (final Exception e) {
      log4j.error("Exception in calculatePercentageAchieved() : ", e);
      return percentageAchieved;
    }
  }

  private long calculateDelayDays(Date expectedDate, Date actualDate) {
    long delayDays = 0;
    try {
      if (actualDate.after(expectedDate)) {
        delayDays = ((actualDate.getTime() - expectedDate.getTime()) / (1000 * 60 * 60 * 24));
      }
      return delayDays;
    } catch (final Exception e) {
      log4j.error("Exception in calculateDelayDate() : ", e);
      return delayDays;
    }
  }

  private boolean checkValidOrderLines(ShipmentInOut inOut) {
    boolean result = false;
    try {
      String whereClause = " e where e.salesOrderLine.id not in (select so.id from OrderLine so where so.salesOrder.id= :poOrderId)";
      OBQuery<ESCMCSVPOReceiptImport> csvImportedQry = OBDal.getInstance()
          .createQuery(ESCMCSVPOReceiptImport.class, whereClause);
      csvImportedQry.setNamedParameter("poOrderId", inOut.getSalesOrder().getId());
      List<ESCMCSVPOReceiptImport> csvImportedList = csvImportedQry.list();
      if (csvImportedList.size() > 0) {
        result = false;
      } else {
        result = true;
      }
      return result;
    } catch (final Exception e) {
      log4j.error("Exception in checkValidOrderLines() : ", e);
      return result;
    }
  }

  private String getCellValue(Cell cell) {
    try {
      if (cell == null)
        return "";
      if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
        if (DateUtil.isCellDateFormatted(cell)) {
          java.util.Date date = cell.getDateCellValue();
          return Utility.dateTimeFormat.format(date);
        } else {
          return cell.getNumericCellValue() == 0 ? "0"
              : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
                  .toString().trim();
        }
      } else
        return Utility.nullToEmpty(cell.getRichStringCellValue().getString());
    } catch (final Exception e) {
      log4j.error("Exception in getCellValue", e);
      return "";
    }
  }

  public static boolean isNumeric(String strVal) {
    try {
      String str = strVal;
      if (str.equals("") && str.isEmpty()) {
        str = "0";
      }
      @SuppressWarnings("unused")
      double d = Double.parseDouble(str);
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  public static boolean chkisnull(String cell) {
    try {
      if (cell == null || cell.equals("")) {
        return false;
      } else {
        return true;
      }

    } catch (Exception e) {
      log4j.error("Exception in chkisnull()", e);
    }
    return true;
  }

  public static boolean chkisnegative(String cellVal) {
    try {
      String cell = cellVal;
      if (cell.equals("") && cell.isEmpty()) {
        cell = "0";
      }
      if (new BigDecimal(cell).compareTo(BigDecimal.ZERO) < 0) {
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      log4j.error("Exception in chkisnegative()", e);
    }
    return true;
  }

}