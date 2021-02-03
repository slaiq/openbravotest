package sa.elm.ob.scm.ad_process.ImportPurchaseOrder;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.scm.ESCMProductCategoryV;
import sa.elm.ob.scm.EscmOrderlineV;
import sa.elm.ob.scm.POLinesImport;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Kiruthika on 23/04/2019
 *
 */

public class ImportPOLinesDAO {

  private static final Logger log4j = Logger.getLogger(ImportPOLinesDAO.class);
  @SuppressWarnings("unused")
  private Connection conn = null;
  DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  Date currentDate = new Date();
  String formattedCurrentDateStr = dateFormat.format(currentDate);

  public ImportPOLinesDAO(Connection con) {
    this.conn = con;
  }

  public JSONObject processUploadedCsvFile(String inpOrderId, VariablesSecureApp vars) {
    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;
    Order order = OBDal.getInstance().get(Order.class, inpOrderId);
    try {
      OBContext.setAdminMode(true);

      isSuccess = UpdatePOLines(order);
      OBDal.getInstance().flush();

      log4j.debug("addOrUpdatePOLines isSuccess" + isSuccess);

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

  private int UpdatePOLines(Order order) {
    try {

      OBQuery<POLinesImport> csvImportQry = OBDal.getInstance().createQuery(POLinesImport.class,
          " as e where e.documentNo = :orderId order by e.creationDate ");
      csvImportQry.setNamedParameter("orderId", order);
      List<POLinesImport> csvImportList = csvImportQry.list();
      HashMap<Long, Long> parentLineMap = new HashMap<>();

      for (POLinesImport csvImportLne : csvImportList) {

        OrderLine parentId = null;
        String productId = null;
        String productCategoryId = null;
        ESCMProductCategoryV category = null;
        String uomId = null;
        String uniqueCodeId = null;
        Product product = null;

        if (csvImportLne.getSalesOrderLine() != null) {
          OBQuery<OrderLine> existionOrderLine = OBDal.getInstance().createQuery(OrderLine.class,
              " as e where e.id = :orderLineId and e.salesOrder.id = :Orderid ");
          existionOrderLine.setNamedParameter("orderLineId",
              csvImportLne.getSalesOrderLine().getId());
          existionOrderLine.setNamedParameter("Orderid", order);
          List<OrderLine> linesList = existionOrderLine.list();
          if (linesList.size() > 0) {
            OrderLine poline = linesList.get(0);
            poline.setLineNo(csvImportLne.getLineNo());
            if (csvImportLne.getParentlineNo() != null) {
              parentId = getParent(csvImportLne.getParentlineNo(), order.getId());
              if (parentId != null) {
                EscmOrderlineV parentLineV = OBDal.getInstance().get(EscmOrderlineV.class,
                    parentId.getId());
                poline.setEscmParentline(parentLineV);
              } else {
                poline.setEscmParentline(null);
                parentLineMap.put(csvImportLne.getLineNo(), csvImportLne.getParentlineNo());
              }
            } else {
              poline.setEscmParentline(null);
            }
            if (csvImportLne.getItem() != null && csvImportLne.getItem() != "") {
              productId = getProductId(csvImportLne.getItem());
              if (productId != null) {
                product = OBDal.getInstance().get(Product.class, productId);
                poline.setProduct(product);
              }
            } else {
              poline.setProduct(null);
            }
            if (csvImportLne.getItem() != null && csvImportLne.getItem() != "" && product != null) {
              poline.setEscmProdescription(product.getName());
            } else {
              if (csvImportLne.getDescription() != null) {
                poline.setEscmProdescription(csvImportLne.getDescription());
              }
            }

            if (csvImportLne.getProductCategory() != null
                && csvImportLne.getProductCategory() != "") {
              if (product != null && product.getProductCategory() != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                    product.getProductCategory().getId());
                poline.setEscmProductCategory(category);

              } else {
                productCategoryId = getProductCategory(csvImportLne.getProductCategory());
                if (productCategoryId != null) {
                  category = OBDal.getInstance().get(ESCMProductCategoryV.class, productCategoryId);
                  poline.setEscmProductCategory(category);
                }
              }
            } else {
              if (product != null && product.getProductCategory() != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                    product.getProductCategory().getId());
                poline.setEscmProductCategory(category);
              }
            }
            if (csvImportLne.getUom() != null) {
              if (product != null && product.getUOM() != null) {
                poline.setUOM(product.getUOM());
              } else {
                uomId = getUOM(csvImportLne.getUom());
                if (uomId != null)
                  poline.setUOM(OBDal.getInstance().get(UOM.class, uomId));
              }
            }
            poline.setOrderedQuantity(csvImportLne.getQtyOrdered());
            poline.setUnitPrice(csvImportLne.getUnitPrice());
            poline.setEscmLineTotalUpdated(csvImportLne.getGrossLineamt());
            if (csvImportLne.isSummary() != null) {
              poline.setEscmIssummarylevel(csvImportLne.isSummary());
            }
            if (csvImportLne.getNeedByDate() != null) {
              poline.setEscmNeedbydate(csvImportLne.getNeedByDate());
            }
            if (csvImportLne.getAcctNo() != null) {
              poline.setEscmAcctno(csvImportLne.getAcctNo());
            }
            if (csvImportLne.getComments() != null) {
              poline.setEscmComments(csvImportLne.getComments());
            }
            if (csvImportLne.getNationalProduct() != null) {
              poline.setEscmNationalproduct(csvImportLne.getNationalProduct());
            }
            if (csvImportLne.getUniquecode() != null) {
              uniqueCodeId = getUniqueCodeId(csvImportLne.getUniquecode());

              // updating unique code name and funds available along with unique code
              if (uniqueCodeId != null) {
                AccountingCombination dimension = OBDal.getInstance()
                    .get(AccountingCombination.class, uniqueCodeId);
                BigDecimal fundsAvailable = RequisitionDao.getAutoEncumFundsAvailable(uniqueCodeId,
                    poline.getSalesOrder().getEfinBudgetint().getId());
                poline.setEFINUniqueCode(
                    OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
                poline.setEFINUniqueCodeName(dimension.getEfinUniquecodename());
                poline.setEFINFundsAvailable(fundsAvailable);
              } else {
                poline.setEFINUniqueCode(null);
                poline.setEFINUniqueCodeName(null);
                poline.setEFINFundsAvailable(BigDecimal.ZERO);
              }

            }
            OBDal.getInstance().save(poline);
          }
        } else {
          // insert new orderline
          OrderLine poline = OBProvider.getInstance().get(OrderLine.class);

          Order objPo = csvImportLne.getDocumentNo();
          poline.setClient(objPo.getClient());
          poline.setOrganization(objPo.getOrganization());
          poline.setCreationDate(new java.util.Date());
          poline.setCreatedBy(objPo.getCreatedBy());
          poline.setUpdated(new java.util.Date());
          poline.setUpdatedBy(objPo.getUpdatedBy());
          poline.setActive(true);
          poline.setSalesOrder(objPo);
          poline.setLineNo(csvImportLne.getLineNo());
          poline.setOrderDate(objPo.getOrderDate());
          poline.setWarehouse(objPo.getWarehouse());

          Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
          poline.setCurrency(
              objPo.getCurrency() == null
                  ? (objPo.getOrganization().getCurrency() == null ? objCurrency
                      : objPo.getOrganization().getCurrency())
                  : objPo.getCurrency());

          TaxRate tax = null;

          OBQuery<TaxRate> objTaxQry = OBDal.getInstance().createQuery(TaxRate.class,
              "as e order by e.creationDate desc");
          objTaxQry.setMaxResult(1);
          List<TaxRate> objTaxList = objTaxQry.list();
          if (objTaxList.size() > 0) {
            tax = objTaxList.get(0);
          }

          if (tax != null) {
            poline.setTax(tax);
          }

          if (csvImportLne.getItem() != null && csvImportLne.getItem() != "") {
            productId = getProductId(csvImportLne.getItem());
            if (productId != null) {
              product = OBDal.getInstance().get(Product.class, productId);
              poline.setProduct(product);
            }
          } else {
            poline.setProduct(null);
          }

          if (csvImportLne.getItem() != null && csvImportLne.getItem() != "" && product != null) {
            poline.setEscmProdescription(product.getName());
          } else {
            if (csvImportLne.getDescription() != null) {
              poline.setEscmProdescription(csvImportLne.getDescription());
            }
          }

          if (csvImportLne.getUom() != null) {
            if (product != null && product.getUOM() != null) {
              poline.setUOM(product.getUOM());
            } else {
              uomId = getUOM(csvImportLne.getUom());
              if (uomId != null)
                poline.setUOM(OBDal.getInstance().get(UOM.class, uomId));
            }
          }

          poline.setOrderedQuantity(csvImportLne.getQtyOrdered());

          poline.setUnitPrice(csvImportLne.getUnitPrice());
          poline.setEscmLineTotalUpdated(csvImportLne.getGrossLineamt());
          poline.setLineNetAmount(csvImportLne.getGrossLineamt());
          poline.setEscmIsmanual(true);

          if (csvImportLne.isSummary() != null) {
            poline.setEscmIssummarylevel(csvImportLne.isSummary());
          }

          if (csvImportLne.getProductCategory() != null
              && csvImportLne.getProductCategory() != "") {
            if (product != null && product.getProductCategory() != null) {
              category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                  product.getProductCategory().getId());
              poline.setEscmProductCategory(category);

            } else {
              productCategoryId = getProductCategory(csvImportLne.getProductCategory());
              if (productCategoryId != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class, productCategoryId);
                poline.setEscmProductCategory(category);
              }
            }
          } else {
            if (product != null && product.getProductCategory() != null) {
              category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                  product.getProductCategory().getId());
              poline.setEscmProductCategory(category);
            }
          }

          if (csvImportLne.getNeedByDate() != null) {
            poline.setEscmNeedbydate(csvImportLne.getNeedByDate());
          }

          if (csvImportLne.getParentlineNo() != null) {
            parentId = getParent(csvImportLne.getParentlineNo(), order.getId());
            if (parentId != null) {
              EscmOrderlineV parentLineV = OBDal.getInstance().get(EscmOrderlineV.class,
                  parentId.getId());
              poline.setEscmParentline(parentLineV);
            } else {
              poline.setEscmParentline(null);
              parentLineMap.put(csvImportLne.getLineNo(), csvImportLne.getParentlineNo());
            }
          } else {
            poline.setEscmParentline(null);
          }

          if (csvImportLne.getUniquecode() != null) {
            uniqueCodeId = getUniqueCodeId(csvImportLne.getUniquecode());

            // updating unique code name and funds available along with unique code
            if (uniqueCodeId != null) {
              AccountingCombination dimension = OBDal.getInstance().get(AccountingCombination.class,
                  uniqueCodeId);
              BigDecimal fundsAvailable = RequisitionDao.getAutoEncumFundsAvailable(uniqueCodeId,
                  poline.getSalesOrder().getEfinBudgetint().getId());
              poline.setEFINUniqueCode(
                  OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
              poline.setEFINUniqueCodeName(dimension.getEfinUniquecodename());
              poline.setEFINFundsAvailable(fundsAvailable);
            } else {
              poline.setEFINUniqueCode(null);
              poline.setEFINUniqueCodeName(null);
              poline.setEFINFundsAvailable(BigDecimal.ZERO);
            }

          }
          OBDal.getInstance().save(poline);
        }
      }
      OBDal.getInstance().flush();

      // Update parent line id
      for (Long lineNo : parentLineMap.keySet()) {
        OBQuery<OrderLine> lineQry = OBDal.getInstance().createQuery(OrderLine.class,
            " as e where e.salesOrder.id = :orderId and e.lineNo = :lineNo");
        lineQry.setNamedParameter("orderId", order.getId());
        lineQry.setNamedParameter("lineNo", lineNo);
        List<OrderLine> lineList = lineQry.list();
        if (lineList.size() > 0) {
          OrderLine line = lineList.get(0);

          OrderLine parentLine = getParent(parentLineMap.get(lineNo), order.getId());
          if (parentLine != null) {
            EscmOrderlineV parentLineV = OBDal.getInstance().get(EscmOrderlineV.class,
                parentLine.getId());
            line.setEscmParentline(parentLineV);
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Error in ImportPOLines.java : UpdatePOLines() ", e);
      return 0;
    }
    return 1;
  }

  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars, String inpOrderId,
      boolean isUpload) {
    JSONObject resultJSON = new JSONObject();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;

    try {
      OBContext.setAdminMode(true);

      boolean validFile = true;

      ArrayList<String> lineNoList = new ArrayList<String>();

      StringBuffer resultMessage = new StringBuffer();
      StringBuffer numericErrorDetails = new StringBuffer();
      StringBuffer negativeErrorDetails = new StringBuffer();
      StringBuffer orderLineNotExistsDetails = new StringBuffer();
      StringBuffer uniqueCodeDetails = new StringBuffer();
      StringBuffer itemCodeDetails = new StringBuffer();
      StringBuffer pdtCategoryDetails = new StringBuffer();
      StringBuffer needByDateDetails = new StringBuffer();
      StringBuffer uomDetails = new StringBuffer();
      StringBuffer parentLineDetails = new StringBuffer();
      StringBuffer qtyDetails = new StringBuffer();
      StringBuffer priceDetails = new StringBuffer();
      StringBuffer nationalPdtDetails = new StringBuffer();
      StringBuffer changetypeDetails = new StringBuffer();
      StringBuffer lineExistsDetails = new StringBuffer();
      StringBuffer descriptionMandatory = new StringBuffer();
      StringBuffer uomMandatory = new StringBuffer();
      StringBuffer qtyMandatory = new StringBuffer();
      StringBuffer NegUnitPriceMandatory = new StringBuffer();
      StringBuffer needByDateMandatory = new StringBuffer();

      boolean noMandatoryFields = false;
      boolean hasErrorMessage = false;
      // boolean hasValidLine = false;
      boolean isParentInvalid = false;
      boolean lineNumberExists = false;
      boolean isLineNoBlank = false;
      Order order = OBDal.getInstance().get(Order.class, inpOrderId);

      // Get Work Book
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);

      // Get all line numbers and store it in lineNoList
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          if (validFile) {
            sheet = xssfWorkbook.getSheetAt(s);
            if (sheet == null)
              break;
            int startRow = 1;

            Row row = null;
            Cell cell = null;

            for (int i = startRow;; i++) {

              row = sheet.getRow(i);

              if (row == null)
                break;
              if (row.getCell(1) != null && StringUtils.isEmpty(row.getCell(1).toString()))
                break;
              // Line No
              cell = row.getCell(1);
              if (cell != null) {
                if (lineNoList.contains(getCellValue(cell))) {
                  lineNumberExists = true;
                  validFile = false;
                  break;
                } else {
                  lineNoList.add(getCellValue(cell));
                }
              } else {
                validFile = false;
                noMandatoryFields = true;
                break;
              }
            }
          }
        }
      }
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          if (validFile) {
            // Get Each Sheet
            sheet = xssfWorkbook.getSheetAt(s);
            if (sheet == null)
              break;

            boolean isUpdateSheet = false;
            if (sheet.getRow(s) != null) {
              Cell updateCell = sheet.getRow(s).getCell(0);
              if (updateCell != null) {
                String update = getCellValue(updateCell);
                if (update != null && update.equals("c_orderline_id")) {
                  isUpdateSheet = true;
                }
              }
            }

            // Initialize Data to be inserted in temporary table
            BigDecimal orderQty = BigDecimal.ZERO;
            BigDecimal unitPrice = BigDecimal.ZERO;
            BigDecimal lineAmount = BigDecimal.ZERO;
            OrderLine orderLne = null;

            String description = "";

            String productCategory = "";
            int startRow = 1;

            // Get Each Row
            for (int i = startRow;; i++) {
              boolean hasError = false;
              Row row = null;
              Cell cell = null;
              String orderLineId = null;
              String lineNo = "";
              String parentLineNo = "";
              String cellVal = "";
              String itemCode = "";
              String uom = "";
              String summary = "";
              String acctNo = "";
              String comments = "";
              String nationalPro = "";
              String needDate = "";
              boolean isSummary = false;
              String strOrderQty = null;
              String strUnitPrice = null;
              boolean changeTypeValue = false;
              String uniqueCode = "";

              row = sheet.getRow(i);

              if (row == null)
                break;

              // OrderLine
              cell = row.getCell(0);
              if (cell != null) {

                orderLineId = Utility.nullToEmpty(getCellValue(cell));
                orderLne = OBDal.getInstance().get(OrderLine.class, orderLineId);

                if (orderLne != null) {
                  if (!checkValidOrderLines(orderLineId, inpOrderId)) {
                    resultMessage.append(OBMessageUtils.messageBD("Escm_DifferentPOLines"));
                    hasErrorMessage = true;
                    break;
                  }
                } else {
                  if (StringUtils.isNotEmpty(cell.toString())) {
                    String lineNumber = getCellValue(row.getCell(1));
                    String message = OBMessageUtils.messageBD("Escm_OrderlineNotExists");
                    message = message.replace("%", lineNumber);
                    resultMessage.append(message);
                    hasErrorMessage = true;
                    break;
                  }
                }
              } else {
                // validFile = false;
                // break;
                orderLne = null;
              }
              if (StringUtils.isEmpty(row.getCell(1).toString())
                  && StringUtils.isEmpty(row.getCell(2).toString())
                  && StringUtils.isEmpty(row.getCell(3).toString())
                  && StringUtils.isEmpty(row.getCell(4).toString())
                  && StringUtils.isEmpty(row.getCell(5).toString())
                  && StringUtils.isEmpty(row.getCell(6).toString())
                  && StringUtils.isEmpty(row.getCell(7).toString())
                  && StringUtils.isEmpty(row.getCell(8).toString())
                  && StringUtils.isEmpty(row.getCell(9).toString())
                  && StringUtils.isEmpty(row.getCell(10).toString())
                  && StringUtils.isEmpty(row.getCell(11).toString())
                  && StringUtils.isEmpty(row.getCell(12).toString())
                  && StringUtils.isEmpty(row.getCell(13).toString())
                  && StringUtils.isEmpty(row.getCell(14).toString())
                  && StringUtils.isEmpty(row.getCell(15).toString()))
                break;
              // Line No
              cell = row.getCell(1);
              if (cell != null && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (cellVal != "" && cellVal != null) {
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + cellVal
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(cellVal)) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + cellVal
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    if (!isUpdateSheet) {
                      boolean isLineExists = isValidParentLine(cellVal, order.getId());
                      if (isLineExists) {
                        lineExistsDetails.append(
                            OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + cellVal);
                        lineExistsDetails.append("<br>");
                        hasError = true;
                      }
                    }
                    lineNo = getCellValue(cell);
                  }
                } else {
                  validFile = false;
                  break;
                }
              } else {
                isLineNoBlank = true;
                hasError = true;
              }

              // parent Line no
              cell = row.getCell(2);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (!isNumeric(cellVal)) {
                  numericErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  numericErrorDetails.append("<br>");
                  hasError = true;
                } else if (!chkisnegative(getCellValue(cell))) {
                  negativeErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  negativeErrorDetails.append("<br>");
                  hasError = true;
                } else if (cellVal != "" && !lineNoList.contains(cellVal)) {
                  parentLineDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  parentLineDetails.append("<br>");
                  hasError = true;
                } else {
                  parentLineNo = getCellValue(cell);
                  // if (isUpdateSheet) {
                  // if (parentLineNo != null && parentLineNo != ""
                  // && !isValidParentLine(parentLineNo, inpOrderId)) {
                  // isParentInvalid = true;
                  // parentLineDetails
                  // .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                  // + lineNo + " -> " + cellVal);
                  // parentLineDetails.append("<br>");
                  // hasError = true;
                  // }
                  // }
                }
              }

              // item Code
              cell = row.getCell(3);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "" && !isValidItemCode(cellVal)) {
                  itemCodeDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                      + lineNo + " ->  " + cellVal);
                  itemCodeDetails.append("<br>");
                  hasError = true;
                } else {
                  itemCode = getCellValue(cell);
                }
              }

              // Description
              cell = row.getCell(4);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  description = getCellValue(cell);
                } else {
                  if (itemCode == null || itemCode.equals("")) {
                    descriptionMandatory
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    descriptionMandatory.append("<br>");
                    hasError = true;
                  }
                }
              } else {
                if (itemCode == null || itemCode.equals("")) {
                  descriptionMandatory
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  descriptionMandatory.append("<br>");
                  hasError = true;
                }
              }

              // product category
              cell = row.getCell(5);
              if (cell != null) {
                cellVal = cell.toString();// getCellValue(cell);
                if (cellVal != null && cellVal != "" && !isValidPdtCategory(cellVal)) {
                  pdtCategoryDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  pdtCategoryDetails.append("<br>");
                  hasError = true;
                } else {
                  productCategory = cellVal;
                }
              }

              // UOM
              cell = row.getCell(6);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal == null || cellVal == "") {
                  uomMandatory.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                      + lineNo + " ->  " + cellVal);
                  uomMandatory.append("<br>");
                  hasError = true;
                }
                if (cellVal != null && cellVal != "" && !isValidUOM(cellVal)) {
                  uomDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                      + lineNo + " ->  " + cellVal);
                  uomDetails.append("<br>");
                  hasError = true;
                  break;
                } else {
                  uom = getCellValue(cell);
                }
              } else {
                uomDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                    + lineNo + " ->  " + cellVal);
                uomDetails.append("<br>");
                hasError = true;
              }

              // summary
              cell = row.getCell(10);
              if (cell != null) {
                cellVal = getCellValue(cell);
                summary = cell.toString();
                isSummary = (summary.equals("Y")) ? true : false;
              }

              // Qty Ordered
              cell = row.getCell(7);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal == null || cellVal == "") {
                  qtyDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                      + lineNo + " ->  " + cellVal);
                  qtyDetails.append("<br>");
                  hasError = true;
                }
                if (cellVal != null && cellVal != "") {
                  strOrderQty = cellVal;
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(getCellValue(cell))) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    if (cellVal.equals("0") && !isSummary) {
                      qtyDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                          + lineNo + " ->  " + cellVal);
                      qtyDetails.append("<br>");
                      hasError = true;
                    } else {
                      orderQty = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                  }
                }
              } else {
                qtyDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                    + lineNo + " ->  " + cellVal);
                qtyDetails.append("<br>");
                hasError = true;
                break;
              }

              // Unit Price
              cell = row.getCell(8);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  strUnitPrice = cellVal;
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(getCellValue(cell))) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    // if (cellVal.equals("0") && !isSummary) {
                    // priceDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                    // + lineNo + " -> " + cellVal);
                    // priceDetails.append("<br>");
                    // hasError = true;
                    // } else {
                    unitPrice = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                    // }
                  }
                } else {
                  NegUnitPriceMandatory
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  NegUnitPriceMandatory.append("<br>");
                  hasError = true;
                }
              } else {
                NegUnitPriceMandatory
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                        + " ->  " + cellVal);
                NegUnitPriceMandatory.append("<br>");
                hasError = true;
              }

              // GrossLineAmount
              cell = row.getCell(9);
              if (cell != null) {
                if (strOrderQty != null && strUnitPrice != null && isNumeric(strOrderQty)
                    && isNumeric(strUnitPrice)) {
                  BigDecimal Amount = new BigDecimal(strOrderQty)
                      .multiply(new BigDecimal(strUnitPrice));
                  lineAmount = Amount.setScale(2, BigDecimal.ROUND_HALF_UP);
                }
              }

              // need by date
              cell = row.getCell(11);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal == null || cellVal == "") {
                  needByDateMandatory
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  needByDateMandatory.append("<br>");
                  hasError = true;
                }
                if (cellVal != null && cellVal != "" && !isValidNeedbyDate(cellVal)) {
                  needByDateDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " -> " + cellVal);
                  needByDateDetails.append("<br>");
                  hasError = true;
                } else {
                  needDate = UtilityDAO.convertToGregorian_tochar(cellVal);
                }
              } else {
                needByDateMandatory
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                        + " ->  " + cellVal);
                needByDateMandatory.append("<br>");
                hasError = true;
              }

              // Account no
              cell = row.getCell(12);
              if (cell != null) {
                cellVal = getCellValue(cell);
                acctNo = cellVal;

              }
              // Comments
              cell = row.getCell(13);
              if (cell != null) {
                cellVal = getCellValue(cell);
                comments = cellVal;

              }
              // nationalProduct
              cell = row.getCell(14);
              if (cell != null) {
                cellVal = getCellValue(cell);
                nationalPro = cellVal;
                if (!isNumeric(cellVal)) {
                  numericErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  numericErrorDetails.append("<br>");
                  hasError = true;
                } else if (!chkisnegative(getCellValue(cell))) {
                  negativeErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  negativeErrorDetails.append("<br>");
                  hasError = true;
                } else {
                  if (cellVal != null && cellVal != ""
                      && (Long.parseLong(cellVal) > 100 || Long.parseLong(cellVal) == 0)) {
                    nationalPdtDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    nationalPdtDetails.append("<br>");
                    hasError = true;
                  } else {
                    nationalPro = cellVal;
                  }
                }

              }

              // Unique code
              cell = row.getCell(15);
              if (!isSummary) {
                cellVal = getCellValue(cell);

                if (cellVal != "" && !isValidUniqueCode(cellVal)) {
                  uniqueCodeDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  uniqueCodeDetails.append("<br>");
                  hasError = true;
                } else {
                  uniqueCode = cellVal;
                }
              }
              // po change type greater than gross line amount then throw error
              if (lineAmount.compareTo(new BigDecimal(0)) > 0) {
                changeTypeValue = changeType(orderLne, lineAmount);
                if (changeTypeValue) {
                  changetypeDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo);
                  hasError = true;
                }

              }

              if (orderLne != null) {

                // If no error, insert into temporary table
                if (!hasError) {
                  POLinesImport csvImport = OBProvider.getInstance().get(POLinesImport.class);
                  csvImport.setSalesOrderLine(orderLne);
                  csvImport.setLineNo(Long.parseLong(lineNo));
                  if (parentLineNo != "")
                    csvImport.setParentlineNo(Long.parseLong(parentLineNo));
                  csvImport.setQtyOrdered(orderQty);
                  csvImport.setUnitPrice(unitPrice);
                  csvImport.setUniquecode(uniqueCode);
                  csvImport.setItem(itemCode);
                  csvImport.setDescription(description);
                  csvImport.setProductCategory(productCategory);
                  csvImport.setUom(uom);
                  csvImport.setGrossLineamt(lineAmount);
                  if (summary.equals("Y")) {
                    csvImport.setSummary(true);
                  } else {
                    csvImport.setSummary(false);
                  }
                  if (needDate != null)
                    csvImport.setNeedByDate(new SimpleDateFormat("dd-MM-yyyy").parse(needDate));
                  csvImport.setAcctNo(acctNo);
                  csvImport.setComments(comments);
                  if (nationalPro != "")
                    csvImport.setNationalProduct(Long.parseLong(nationalPro));
                  csvImport.setDocumentNo(order);
                  OBDal.getInstance().save(csvImport);
                  OBDal.getInstance().flush();
                  // hasValidLine = true;

                }
              } else {
                if (!hasError) {
                  POLinesImport csvImport = OBProvider.getInstance().get(POLinesImport.class);
                  csvImport.setSalesOrderLine(null);
                  csvImport.setLineNo(Long.parseLong(lineNo));
                  if (parentLineNo != "")
                    csvImport.setParentlineNo(Long.parseLong(parentLineNo));
                  csvImport.setQtyOrdered(orderQty);
                  csvImport.setUnitPrice(unitPrice);
                  csvImport.setUniquecode(uniqueCode);
                  csvImport.setItem(itemCode);
                  csvImport.setDescription(description);
                  csvImport.setProductCategory(productCategory);
                  csvImport.setUom(uom);
                  csvImport.setGrossLineamt(lineAmount);
                  if (summary.equals("Y")) {
                    csvImport.setSummary(true);
                  } else {
                    csvImport.setSummary(false);
                  }
                  if (needDate != null)
                    csvImport.setNeedByDate(new SimpleDateFormat("dd-MM-yyyy").parse(needDate));
                  csvImport.setAcctNo(acctNo);
                  csvImport.setComments(comments);
                  if (nationalPro != "")
                    csvImport.setNationalProduct(Long.parseLong(nationalPro));
                  csvImport.setDocumentNo(order);
                  OBDal.getInstance().save(csvImport);
                  OBDal.getInstance().flush();
                }
              }
            }
          }
        }
        if (validFile) {
          // If Line No is Blank
          if (isLineNoBlank) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_LineNo_Mandatory"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }
          // Description Null Error
          if (descriptionMandatory.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_Desc_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(descriptionMandatory.toString());
            hasErrorMessage = true;
          }

          // UOM Null Error
          if (uomMandatory.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_UOM_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(uomMandatory.toString());
            hasErrorMessage = true;
          }

          // Quantity Null Error
          if (qtyMandatory.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_Qty_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(qtyMandatory.toString());
            hasErrorMessage = true;
          }

          // Negotiated Unit Price Null Error
          if (NegUnitPriceMandatory.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_NegUnitPrice_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(NegUnitPriceMandatory.toString());
            hasErrorMessage = true;
          }

          // Need By Date Null Error
          if (needByDateMandatory.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_NeedbyDate"));
            resultMessage.append("<br>");
            resultMessage.append(needByDateMandatory.toString());
            hasErrorMessage = true;
          }

          // Throws error when mandatory fields are not filled.
          if (noMandatoryFields) {
            resultMessage.append(OBMessageUtils.messageBD("OBUIAPP_FillMandatoryFields"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }

          // Quantity can not be zero
          if (qtyDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
            resultMessage.append("<br>");
            resultMessage.append(qtyDetails.toString());
            hasErrorMessage = true;
          }

          // Unit Price can not be zero
          if (priceDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("EFIN_PenaltyRelAmtGrtZero"));
            resultMessage.append("<br>");
            resultMessage.append(priceDetails.toString());
            hasErrorMessage = true;
          }

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

          if (uniqueCodeDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidUniqueCode"));
            resultMessage.append("<br>");
            resultMessage.append(uniqueCodeDetails.toString());
            hasErrorMessage = true;
          }

          // Item code Errors
          if (itemCodeDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidItemCode"));
            resultMessage.append("<br>");
            resultMessage.append(itemCodeDetails.toString());
            hasErrorMessage = true;
          }

          // Product Category Errors
          if (pdtCategoryDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidProductCategory"));
            resultMessage.append("<br>");
            resultMessage.append(pdtCategoryDetails.toString());
            hasErrorMessage = true;
          }

          // Need by date Errors
          if (needByDateDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidNeedbyDate"));
            resultMessage.append("<br>");
            resultMessage.append(needByDateDetails.toString());
            hasErrorMessage = true;
          }

          // UOM Errors
          if (uomDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidUOM"));
            resultMessage.append("<br>");
            resultMessage.append(uomDetails.toString());
            hasErrorMessage = true;
          }

          // National Product Errors
          if (nationalPdtDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_National_Product_value"));
            resultMessage.append("<br>");
            resultMessage.append(nationalPdtDetails.toString());
            hasErrorMessage = true;
          }
          // parentLine Error
          if (parentLineDetails.toString().length() > 0) {
            if (isParentInvalid) {
              resultMessage.append(OBMessageUtils.messageBD("Escm_PO_NoParentLineNumber"));
              resultMessage.append("<br>");
              resultMessage.append(parentLineDetails.toString());
              hasErrorMessage = true;
            } else {
              resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidParentLine"));
              resultMessage.append("<br>");
              resultMessage.append(parentLineDetails.toString());
              hasErrorMessage = true;
            }
          }
          if (changetypeDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_POChangeValCantBeApplied"));
            resultMessage.append("<br>");
            resultMessage.append(changetypeDetails.toString());
            hasErrorMessage = true;
          }
          // Line already exists
          if (lineExistsDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Efin_lineexist"));
            resultMessage.append("<br>");
            resultMessage.append(lineExistsDetails.toString());
            hasErrorMessage = true;
          }

        } else {
          if (noMandatoryFields) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_LineNo_Mandatory"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          } else if (lineNumberExists) {
            // Line number already exists
            resultMessage.append(OBMessageUtils.messageBD("Efin_lineexist"));
            resultMessage.append("<br>");
            hasErrorMessage = true;

          } else {
            // Invalid Files
            resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_InvalidfFile"));
            hasErrorMessage = true;
          }
        }
      }
      if (hasErrorMessage) {
        resultJSON.put("status", "0");
        resultJSON.put("statusMessage", resultMessage.toString());
        OBDal.getInstance().rollbackAndClose();
        log4j.debug("Validation Failed");
      } else {
        resultJSON.put("status", "1");
        resultJSON.put("statusMessage", OBMessageUtils.messageBD("Escm_CSV_ValidatedSuccess"));
        log4j.debug("Validation Success");
      }
    } catch (final Exception e) {
      log4j.error("Exception in ImportPOLines() : ", e);
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
      OBQuery<POLinesImport> csvImportQry = OBDal.getInstance().createQuery(POLinesImport.class,
          "order by creationDate");
      List<POLinesImport> csvImportList = csvImportQry.list();
      for (POLinesImport csvimprt : csvImportList) {
        OBDal.getInstance().remove(csvimprt);
      }
    } catch (final Exception e) {
      log4j.error("Exception in deleteCSVImportEntries() : ", e);
    }
  }

  private boolean checkValidOrderLines(String orderLineId, String orderId) {
    boolean result = false;
    try {

      OrderLine line = OBDal.getInstance().get(OrderLine.class, orderLineId);

      if (orderId.equals(line.getSalesOrder().getId())) {
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
      log4j.error("Exception in chkisnull method in ImportPOLinesDAO.java", e);
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
      log4j.error("Exception in chkisnegative method in ImportPOLinesDAO.java", e);

    }
    return true;
  }

  public static boolean isValidUniqueCode(String uniqueCode) {
    boolean isValidUniquecode = false;
    try {
      if (!uniqueCode.equals("") && !uniqueCode.isEmpty()) {
        OBQuery<AccountingCombination> uniqueCodeQry = OBDal.getInstance()
            .createQuery(AccountingCombination.class, "efinUniqueCode = :uniqueCode");
        uniqueCodeQry.setNamedParameter("uniqueCode", uniqueCode);
        List<AccountingCombination> uniqueCodeList = uniqueCodeQry.list();
        if (uniqueCodeList.size() > 0) {
          isValidUniquecode = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidUniqueCode method in ImportPOLinesDAO.java", e);
      return isValidUniquecode;
    }
    return isValidUniquecode;
  }

  public static OrderLine getParent(Long lineno, String orderId) {
    OrderLine poline = null;
    try {
      OBQuery<OrderLine> ParentLine = OBDal.getInstance().createQuery(OrderLine.class,
          " as e where e.lineNo = :ParentlineNo and e.salesOrder.id = :Orderid ");
      ParentLine.setNamedParameter("ParentlineNo", lineno);
      ParentLine.setNamedParameter("Orderid", orderId);

      List<OrderLine> parentLinesList = ParentLine.list();
      if (parentLinesList != null && parentLinesList.size() > 0) {
        poline = parentLinesList.get(0);
      }
    } catch (Exception e) {
      log4j.error("Exception in getParent", e);
    }
    return poline;
  }

  public static String getProductId(String itemCode) {
    String productId = null;
    try {

      if (!itemCode.equals("") && itemCode != null) {
        // String[] item = itemCode.split("-", 2);

        OBQuery<org.openbravo.model.common.plm.Product> itemCodeQry = OBDal.getInstance()
            .createQuery(org.openbravo.model.common.plm.Product.class, "trim(searchKey) = :code ");// and
                                                                                                   // trim(name)
                                                                                                   // =
                                                                                                   // :name
        itemCodeQry.setNamedParameter("code", itemCode);// item[0].trim()
        // itemCodeQry.setNamedParameter("name", item[1].trim());
        List<org.openbravo.model.common.plm.Product> itemCodeList = itemCodeQry.list();
        if (itemCodeList.size() > 0) {
          productId = itemCodeList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getProductId method in ImportPOLinesDAO.java", e);
      return productId;
    }
    return productId;
  }

  public static String getProductCategory(String pdtCategory) {
    String ProductCategory = null;
    try {
      if (!pdtCategory.equals("") && pdtCategory != null) {
        OBQuery<ESCMProductCategoryV> pdtCategoryQry = OBDal.getInstance()
            .createQuery(ESCMProductCategoryV.class, "validationCode = :code");
        pdtCategoryQry.setNamedParameter("code", pdtCategory);
        List<ESCMProductCategoryV> pdtCategoryList = pdtCategoryQry.list();
        if (pdtCategoryList.size() > 0) {
          ProductCategory = pdtCategoryList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getProductCategory", e);
    }
    return ProductCategory;
  }

  public static String getUOM(String uomName) {

    String uomId = null;
    try {

      if (!uomName.equals("") && uomName != null) {
        OBQuery<UOM> uomQuery = OBDal.getInstance().createQuery(UOM.class, " name = :name");
        uomQuery.setNamedParameter("name", uomName);
        List<UOM> uomCodeList = uomQuery.list();
        if (uomCodeList.size() > 0) {
          uomId = uomCodeList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getUOM method in ImportPOLinesDAO.java", e);
      return uomId;
    }
    return uomId;
  }

  public static String getUniqueCodeId(String uniqueCode) {
    String uniquecodeId = null;
    try {
      if (!uniqueCode.equals("") && !uniqueCode.isEmpty()) {
        OBQuery<AccountingCombination> uniqueCodeQry = OBDal.getInstance()
            .createQuery(AccountingCombination.class, "efinUniqueCode = '" + uniqueCode + "'");
        List<AccountingCombination> uniqueCodeList = uniqueCodeQry.list();
        if (uniqueCodeList.size() > 0) {
          uniquecodeId = uniqueCodeList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getUniqueCodeId method in ImportPOLinesDAO.java", e);
      return uniquecodeId;
    }
    return uniquecodeId;
  }

  public static boolean isValidItemCode(String itemCode) {
    boolean isValidItemcode = false;
    try {
      if (itemCode.equals("-")) {
        isValidItemcode = true;
        return isValidItemcode;
      }
      if (!itemCode.equals("") && itemCode != null) {
        // String[] item = itemCode.split("-", 2);
        // if (item.length == 2) {
        OBQuery<Product> itemCodeQry = OBDal.getInstance().createQuery(Product.class,
            "trim(searchKey) = :code");
        itemCodeQry.setNamedParameter("code", itemCode);// item[0].trim()
        // itemCodeQry.setNamedParameter("name", item[1].trim());

        List<Product> itemCodeList = itemCodeQry.list();
        if (itemCodeList.size() > 0) {
          isValidItemcode = true;
        }
        // }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidItemCode method in ImportPOLinesDAO.java", e);
      return isValidItemcode;
    }
    return isValidItemcode;
  }

  public static boolean isValidPdtCategory(String pdtCategory) {
    boolean isValidPdtCategory = false;
    try {
      if (!pdtCategory.equals("") && pdtCategory != null) {
        OBQuery<ESCMProductCategoryV> pdtCategoryQry = OBDal.getInstance()
            .createQuery(ESCMProductCategoryV.class, "validationCode = :code");
        pdtCategoryQry.setNamedParameter("code", pdtCategory);
        List<ESCMProductCategoryV> pdtCategoryList = pdtCategoryQry.list();
        if (pdtCategoryList.size() > 0) {
          isValidPdtCategory = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidPdtCategory method in ImportPOLinesDAO.java", e);
      return isValidPdtCategory;
    }
    return isValidPdtCategory;
  }

  public static boolean isValidNeedbyDate(String date) {
    SimpleDateFormat df = new SimpleDateFormat("dd-mm-yyyy");
    boolean isValidHijiriDate = false;
    try {
      df.parse(date);

      PreparedStatement st = null;
      ResultSet rs = null;
      String gregDate = null;
      try {
        st = new DalConnectionProvider(false).getConnection()
            .prepareStatement("select eut_convertto_gregorian('" + date + "')");
        rs = st.executeQuery();
        if (rs.next()) {
          gregDate = rs.getString("eut_convertto_gregorian");
        }
      } catch (final Exception e) {
        log4j.error("Exception in isValidNeedbyDate method in ImportPOLinesDAO.java", e);
        return isValidHijiriDate;
      }
      if (gregDate != null) {
        isValidHijiriDate = true;
      }
      return isValidHijiriDate;
    } catch (ParseException e) {
      log4j.error("Exception in isValidNeedbyDate method in ImportPOLinesDAO.java", e);
      return isValidHijiriDate;
    }
  }

  public static boolean isValidUOM(String uom) {
    boolean isValidUOM = false;
    try {
      if (!uom.equals("") && uom != null) {
        OBQuery<UOM> uomQry = OBDal.getInstance().createQuery(UOM.class, "name = :name");
        uomQry.setNamedParameter("name", uom);
        List<UOM> uomList = uomQry.list();
        if (uomList.size() > 0) {
          isValidUOM = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidUOM method in ImportPOLinesDAO.java", e);
      return isValidUOM;
    }
    return isValidUOM;
  }

  public static boolean isValidParentLine(String parentLineNo, String orderId) {
    boolean isValidParentLine = false;
    try {
      if (!parentLineNo.equals("") && parentLineNo != null) {
        OBQuery<OrderLine> parentLineQry = OBDal.getInstance().createQuery(OrderLine.class,
            "salesOrder.id = :orderId and lineNo = :lineNo");
        parentLineQry.setNamedParameter("orderId", orderId);
        parentLineQry.setNamedParameter("lineNo", Long.parseLong(parentLineNo));
        List<OrderLine> parentLineList = parentLineQry.list();
        if (parentLineList.size() > 0) {
          isValidParentLine = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidParentLine method in ImportPOLinesDAO.java", e);
      return isValidParentLine;
    }
    return isValidParentLine;
  }

  public boolean changeType(OrderLine orderLne, BigDecimal lineNetAmt) {
    boolean isGreaterThanValue = false;
    try {
      if (orderLne != null) {
        if (orderLne.getEscmPoChangeFactor() != null && orderLne.getEscmPoChangeValue() != null) {
          if (orderLne.getEscmPoChangeValue().compareTo(lineNetAmt) > 0) {
            isGreaterThanValue = true;
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in changeType method in ImportPOLinesDAO.java", e);
      return isGreaterThanValue;
    }
    return isGreaterThanValue;
  }
}