package sa.elm.ob.scm.ad_process.RequisitionLineUploader.dao;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
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
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.ESCMProductCategoryV;
import sa.elm.ob.scm.EscmCsvImportPrline;
import sa.elm.ob.scm.EscmRequisitionlineV;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Gopalakrishnan on 06/11/2017
 *
 */
public class RequisitionLineUploaderDAOImpl implements RequisitionLineUploaderDAO {
  /**
   * Data Access layer to Import Requisition Lines In Requisition Window
   */

  private static final Logger log4j = Logger.getLogger(RequisitionLineUploaderDAOImpl.class);
  @SuppressWarnings("unused")
  private Connection conn = null;

  public RequisitionLineUploaderDAOImpl(Connection con) {
    this.conn = con;
  }

  /**
   * 
   * @param file
   * @param vars
   * @param inpRequistionId
   * @return success once file processed
   */
  @Override
  public JSONObject processUploadedCsvFile(File file, VariablesSecureApp vars,
      String inpRequistionId) {

    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;
    Row row = null;
    StringBuffer strDelQuery = null;

    try {
      OBContext.setAdminMode(true);
      int totalCount = 0;
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
      strDelQuery = new StringBuffer();
      strDelQuery.append(" delete from Escm_Csv_Import_Prline ");
      // OBDal.getInstance().getSession().createSQLQuery(strDelQuery).executeUpdate();
      OBDal.getInstance().getSession().createQuery(strDelQuery.toString()).executeUpdate();

      if (xssfWorkbook.getNumberOfSheets() > 0) {
        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          sheet = xssfWorkbook.getSheetAt(s);
          if (sheet == null)
            break;

          int startRow = 1;

          for (int i = startRow;; i++) {
            row = sheet.getRow(i);
            if (row == null)
              break;
            else if (isRowEmpty(row)) {
              break;
            } else {
              List<String> result = parseExcel(row);
              if (result == null)
                continue;

              String fields[] = result.toArray(new String[0]);
              for (int j = 0; j < fields.length; j++) {
                fields[j] = fields[j].replace("\"", "");
              }
              isSuccess = importDataInDatabase(fields, inpRequistionId);
              result.clear();
            }
            totalCount = i;
          }
        }
      }
      log4j.debug("isSuccess" + isSuccess);
      if (isSuccess == 1) {
        // All columns in xls are inserted in temp table EscmCsvImportPrline), now actual records
        // are
        // created
        // Inserting PRLInes
        isSuccess = insertIntoRequisitionLine(inpRequistionId);
        if (isSuccess == 1) {
          // all records whether inserted successfully then arrange them into tree
          isSuccess = updateRequisitionLine(inpRequistionId);
          // once tree framed remove data from temp table
          if (isSuccess == 1) {
            OBQuery<EscmCsvImportPrline> obQuery = OBDal.getInstance()
                .createQuery(EscmCsvImportPrline.class, "as e where e.requisition.id=:reqID");
            obQuery.setNamedParameter("reqID", inpRequistionId);
            if (obQuery.list().size() > 0) {
              for (EscmCsvImportPrline delcsvObj : obQuery.list()) {
                OBDal.getInstance().remove(delcsvObj);
              }
            }
          }
        } else if (isSuccess == 2) {
          jsonresult = new JSONObject();
          jsonresult.put("status", "0");
          jsonresult.put("recordsFailed", "");
          jsonresult.put("statusMessage", OBMessageUtils.messageBD("ESCM_PRDupLineCantImport"));
          return jsonresult;
        }
      }

      OBDal.getInstance().flush();

      if (isSuccess == 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", OBMessageUtils.messageBD("Escm_Import_Record_Failed"));
      } else {
        jsonresult = new JSONObject();
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage",
            OBMessageUtils.messageBD("ESCM_PRRecord_Insert_Success") + totalCount + "");
      }
    } catch (Exception e) {
      isSuccess = 0;
      jsonresult = new JSONObject();
      try {
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", e.getMessage());
      } catch (JSONException e1) {
        log4j.error("Exception in Requisition Line Data Import", e);
      }
      log4j.error("Exception in Reuisition Line Data Import", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonresult;

  }

  /**
   * @param row
   * @return boolean
   */
  public boolean isEmptyRow(Row row) {
    int emptyCell = 0, noOfCell = 0;
    try {
      for (Cell cell : row) {
        noOfCell += 1;
        if (StringUtils.isEmpty(getCellValue(cell))) {
          emptyCell += 1;
        } else {
          break;
        }
      }
      if (noOfCell == emptyCell) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      log4j.error("Exception in isEmptyRow : " + e);
    }
    return false;
  }

  /**
   * 
   * @param file
   * @param vars
   * @param inpBudgetId
   * @return success valid xlsx file
   */
  @Override
  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars,
      String inpRequistionId) {

    JSONObject resultJSON = new JSONObject();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;
    Row row = null;
    String isNumericWithorWithoutDecimal = "\\d*\\.?\\d+";
    String dateFormat = "([0-9]{2})-([0-9]{2})-([0-9]{4})";
    List<String> linenoList = new ArrayList<>();

    int failed = 0;
    StringBuffer description = new StringBuffer();
    String br = "";

    try {

      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
      if (xssfWorkbook.getNumberOfSheets() > 0) {

        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          sheet = xssfWorkbook.getSheetAt(s);
          if (sheet == null)
            break;

          int startRow, i;

          row = sheet.getRow(0);
          int noOfColumns = row.getLastCellNum();
          description.append(br);
          if (noOfColumns < 10) {
            failed += 1;
            description.append(OBMessageUtils.messageBD("ESCM_ExportPR_ParLine").replace("%", "10"))
                .append("<br>");
            br = "<br>";
          } else if (noOfColumns > 10) {
            failed += 1;
            description
                .append(OBMessageUtils.messageBD("ESCM_ExportPR_MorFields").replace("%", "10"))
                .append("<br>");
            br = "<br>";
          }
          for (startRow = 1;; startRow++) {
            row = sheet.getRow(startRow);
            i = startRow + 1;

            if (row == null || isEmptyRow(row))
              break;

            if (row.getCell(8) != null && StringUtils.isNotEmpty((row.getCell(8)).toString())) {

              if (StringUtils.isNumericSpace(getCellValue(row.getCell(8)))
                  || !row.getCell(8).toString().matches(dateFormat)) {
                failed += 1;
                description.append(OBMessageUtils.messageBD("ESCM_ExportPR_InCrctDate").replace("%",
                    String.valueOf(i))).append("<br>");
                br = "<br>";
              } else {
                // check date is past Date
                Date needDate = new SimpleDateFormat("yyyy-MM-dd")
                    .parse(UtilityDAO.convertToGregorian(getCellValue(row.getCell(8))));
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                if (needDate.before(cal.getTime())) {
                  failed += 1;
                  description.append(OBMessageUtils.messageBD("ESCM_ExportPR_PastDate").replace("%",
                      String.valueOf(i))).append("<br>");
                  br = "<br>";
                }
              }

            }
            if (row.getCell(2) != null && StringUtils.isNotEmpty((row.getCell(2)).toString())) {
              OBCriteria<Product> productListCr = OBDal.getInstance().createCriteria(Product.class);
              productListCr.add(
                  Restrictions.ilike(Product.PROPERTY_SEARCHKEY, getCellValue(row.getCell(2))));
              productListCr.setMaxResults(1);
              List<Product> productList = productListCr.list();
              if (!(productList.size() > 0)) {// PriceList not found
                failed += 1;
                description.append(OBMessageUtils.messageBD("ESCM_ExportPR_ProdNotFound")
                    .replaceFirst("%", getCellValue(row.getCell(2)))
                    .replace("%", String.valueOf(i))).append("<br>");
                br = "<br>";
              }
            }
            if (!StringUtils.isNotEmpty(getCellValue(row.getCell(0)))
                || !StringUtils.isNumericSpace(getCellValue(row.getCell(0)))) {
              failed += 1;
              description.append(
                  OBMessageUtils.messageBD("ESCM_ExportPR_LineNo").replace("%", String.valueOf(i)))
                  .append("<br>");
              br = "<br>";
            } else {
              if (linenoList.contains(getCellValue(row.getCell(0)))) {
                failed += 1;
                description.append(OBMessageUtils.messageBD("ESCM_ExportPR_LineNoDup").replace("%",
                    String.valueOf(i))).append("<br>");
                br = "<br>";
              } else
                linenoList.add(getCellValue(row.getCell(0)));
            }
            if (!StringUtils.isNotEmpty(getCellValue(row.getCell(3)))
                && !StringUtils.isNotEmpty(getCellValue(row.getCell(2)))) {
              failed += 1;
              description.append(
                  OBMessageUtils.messageBD("ESCM_ExportPR_DesMan").replace("%", String.valueOf(i)))
                  .append("<br>");
              br = "<br>";
            }

            if (!StringUtils.isNotEmpty(getCellValue(row.getCell(6)))
                || !getCellValue(row.getCell(6)).toString()
                    .matches(isNumericWithorWithoutDecimal)) {
              failed += 1;
              description.append(
                  OBMessageUtils.messageBD("ESCM_ExportPR_QtyMan").replace("%", String.valueOf(i)))
                  .append("<br>");
              br = "<br>";
            }
            if (row.getCell(6) != null && getCellValue(row.getCell(6)).equals("0")) {
              failed += 1;
              description
                  .append(
                      OBMessageUtils.messageBD("ESCM_ExportPR_Qty").replace("%", String.valueOf(i)))
                  .append("<br>");
              br = "<br>";
            }
            if (row.getCell(5) != null && StringUtils.isNotEmpty(row.getCell(5).toString())) {
              OBQuery<UOM> uomQuery = OBDal.getInstance().createQuery(UOM.class,
                  " where name = :uomName ");
              uomQuery.setNamedParameter("uomName", getCellValue(row.getCell(5)));

              if (!(uomQuery != null && uomQuery.list().size() > 0)) {
                failed += 1;

                description.append(OBMessageUtils.messageBD("ESCM_ExportPR_UOMNotFound")
                    .replaceFirst("%", getCellValue(row.getCell(5)))
                    .replace("%", String.valueOf(i))).append("<br>");
                br = "<br>";
              }
            }
            if (row.getCell(7) != null
                && StringUtils.isNotEmpty(getCellValue(row.getCell(7)).toString())
                && !getCellValue(row.getCell(7)).toString()
                    .matches(isNumericWithorWithoutDecimal)) {
              failed += 1;
              description.append(OBMessageUtils.messageBD("ESCM_ExportPR_UnitPrice").replace("%",
                  String.valueOf(i))).append("<br>");
              br = "<br>";
            }
          }
        }
        if (failed > 0) {
          resultJSON = new JSONObject();
          resultJSON.put("status", "0");
          resultJSON.put("recordsFailed", Integer.toString(failed));
          resultJSON.put("statusMessage", description);
        } else {
          resultJSON.put("status", "1");
          resultJSON.put("recordsFailed", Integer.toString(failed));
          resultJSON.put("statusMessage", OBMessageUtils.messageBD("Efin_Import_Csv_Success"));
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in while validating requisition lines sheet() : ", e);
      try {

      } catch (Exception ee) {
        log4j.error("Exception in while validating requisition lines sheet()  : ", ee);
      }
      return resultJSON;
    }
    return resultJSON;

  }

  /**
   * 
   * @param cell
   * @return value of cell based on type
   */
  @Override
  public String getCellValue(Cell cell) {
    try {
      if (cell == null)
        return "";
      if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
        if (DateUtil.isCellDateFormatted(cell)) {
          java.util.Date date = cell.getDateCellValue();
          return Utility.dateTimeFormat.format(date);
        } else {
          return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
              .toString().trim();
        }
      } else if (Cell.CELL_TYPE_STRING == cell.getCellType()) {
        return cell.getStringCellValue().trim();
      } else if (Cell.CELL_TYPE_FORMULA == cell.getCellType()) {
        cell.setCellType(Cell.CELL_TYPE_STRING);
        return cell.getStringCellValue().trim();
      } else
        return Utility.nullToEmpty(cell.getRichStringCellValue().getString().trim());
    } catch (final Exception e) {
      log4j.error("Exception in getCellValue", e);
      return "";
    }
  }

  /**
   * Parsing .xlsx file using regular Expression
   */

  @Override
  public List<String> parseExcel(Row row) {
    final List<String> strings = new ArrayList<String>();
    for (int cn = 0; cn < 12; cn++) {
      Cell cell = row.getCell(cn, Row.RETURN_BLANK_AS_NULL);
      if (cell == null) {
        strings.add("");
      } else {
        strings.add(getCellValue(cell));
      }
    }
    return strings;
  }

  /**
   * 
   * @param fields
   * @param inpRequistionId
   * @return success when data inserted in active table
   */
  @Override
  public int importDataInDatabase(String[] fields, String inpRequistionId) {
    int isSuccess = 1;

    try {

      EscmCsvImportPrline csvImport = null;
      String strUnitPrice = StringUtils.isEmpty(fields[7].toString()) ? "0" : fields[7].toString();
      String str_field6 = StringUtils.isEmpty(fields[6].toString()) ? "1" : fields[6].toString();
      BigDecimal qty = new BigDecimal(str_field6.replaceAll(",", ""));
      BigDecimal unitPrice = new BigDecimal(strUnitPrice.replaceAll(",", ""));

      csvImport = OBProvider.getInstance().get(EscmCsvImportPrline.class);
      csvImport.setRequisition(OBDal.getInstance().get(Requisition.class, inpRequistionId));
      csvImport.setLineNo(fields[0].toString());
      csvImport.setParentline(fields[1].toString());
      csvImport.setProductcategory(fields[4].toString());
      csvImport.setItem(fields[2].toString());
      if (fields[3] != null && fields[3].toString().length() > 2000)
        csvImport.setDescription(fields[3].toString().substring(0, 1999));
      else
        csvImport.setDescription(fields[3].toString());
      csvImport.setUom(fields[5].toString());
      csvImport.setReservedQuantity(qty);
      csvImport.setNeedByDate(fields[8].toString());
      // csvImport.setFunds(new BigDecimal(str_field8.replaceAll(",", "")));
      csvImport.setUnitPrice(unitPrice);
      csvImport.setLinetotal(unitPrice.multiply(qty));
      csvImport.setComments(fields[9].toString());
      /*
       * if (fields[11].equals("1")) { csvImport.setSummary(true); } else {
       * csvImport.setSummary(false); }
       */
      OBDal.getInstance().save(csvImport);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      isSuccess = 0;
      log4j.error("Exception in importDataInDatabase() : ", e);
    }
    return isSuccess;

  }

  /**
   * 
   * @param inpRequistionId
   * @return success once requisition line inserted
   */
  @Override
  @SuppressWarnings("unchecked")
  public int insertIntoRequisitionLine(String inpRequistionId) {
    int status = 1;
    try {
      /*
       * select prd.id as m_product_id, cat.id as m_product_category_id, csv.description,
       * csv.parentline, csv.needByDate, csv.reservedQuantity, csv.funds, csv.linetotal,
       * csv.comments, csv.lineNo, csv.requisition.id, csv.unitPrice from Escm_Csv_Import_Prline
       * csv, Product prd, ProductCategory cat where upper(prd.searchKey) like upper(csv.item) and
       * prd.client.id='' and upper(cat.searchKey) like upper(csv.productcategory) and
       * cat.client.id='' and csv.requisition.id=''
       */
      // START of Requisition Line
      log4j.debug("RequisitionId" + inpRequistionId);
      String selectPRLine = " select prd.m_product_id,cat.m_product_category_id,csv.description,csv.parentline,csv.needbydate, "
          + " csv.quantity,csv.funds,csv.linetotal,csv.note,csv.summary,csv.line_no,csv.m_requisition_id,unitprice, (select c_uom_id from c_uom where name = csv.uom and ad_client_id in('0',csv.ad_client_id) limit 1) as c_uom_id from escm_csv_import_prline csv "
          + " left join m_product prd on prd.value ilike csv.item and prd.ad_client_id =? "
          + " left join m_product_category cat on cat.value ilike csv.productcategory and cat.ad_client_id=? "
          + " where csv.m_requisition_id=? ";
      SQLQuery prLineQuery = OBDal.getInstance().getSession().createSQLQuery(selectPRLine);
      prLineQuery.setParameter(0, OBContext.getOBContext().getCurrentClient().getId());
      prLineQuery.setParameter(1, OBContext.getOBContext().getCurrentClient().getId());
      prLineQuery.setParameter(2, inpRequistionId);

      List<Object[]> objectslist = prLineQuery.list();
      if (prLineQuery != null && objectslist.size() > 0) {
        for (int i = 0; i < objectslist.size(); i++) {
          Object[] objects = objectslist.get(i);
          RequisitionLine objPRline = null;

          long lineno = new Long((String) objects[10]);
          Requisition objPR = OBDal.getInstance().get(Requisition.class, inpRequistionId);
          if (objPR != null) {
            if (objPR.getProcurementRequisitionLineList().size() > 0) {
              for (RequisitionLine PRLine : objPR.getProcurementRequisitionLineList()) {
                if (PRLine.getLineNo().equals(lineno)) {
                  objPRline = PRLine;
                  break;
                }
              }
            }
          }
          if (objPRline != null) {// PR Line update case
            status = updateInsertPRLine(objPRline, objects, status);
          } else {// PR Line insert case
            objPRline = OBProvider.getInstance().get(RequisitionLine.class);
            objPRline.setLineNo(objects[10] == null ? new BigDecimal("10").longValue()
                : new BigDecimal(objects[10].toString()).longValue());
            status = updateInsertPRLine(objPRline, objects, status);
          }
        }
      }
    } catch (ConstraintViolationException e) {
      log4j.error("Exception in insertIntoRequisitionLine() : ", e);
      status = 2;
    } catch (Exception e) {
      log4j.error("Exception in insertIntoRequisitionLine() : ", e);
      status = 0;
      // TODO: handle exception
    }
    return status;
  }

  public int updateInsertPRLine(RequisitionLine objPRline, Object[] objects, int status) {

    UOM uom = null;
    int statusTemp = status;

    try {
      objPRline.setDescription(objects[2].toString());
      if (objects[0] != null && StringUtils.isNotEmpty(objects[0].toString())) {
        Product objProduct = OBDal.getInstance().get(Product.class, objects[0].toString());
        objPRline.setProduct(objProduct);
        objPRline.setUOM(objProduct.getUOM());
        if (objProduct != null) {// StringUtils.isEmpty(objects[2].toString()) && Task No.
          objPRline.setDescription(objProduct.getName());
        }
        if (objProduct.getProductCategory() != null) {
          ESCMProductCategoryV objProductCategoryView = OBDal.getInstance()
              .get(ESCMProductCategoryV.class, objProduct.getProductCategory().getId());
          objPRline.setEscmProdcate(objProductCategoryView);
        }
      } else if (objects[13] != null && StringUtils.isNotEmpty(objects[13].toString())) {
        uom = Utility.getObject(UOM.class, objects[13].toString());
        objPRline.setUOM(uom);
      } else {
        OBQuery<UOM> uomQuery = OBDal.getInstance().createQuery(UOM.class,
            " where  lower(eDICode) = lower('EA')");
        if (uomQuery != null && uomQuery.list().size() > 0) {
          uom = uomQuery.list().get(0);
          objPRline.setUOM(uom);
        }
      }
      if (objects[1] != null && StringUtils.isNotEmpty(objects[1].toString())) {
        ESCMProductCategoryV objProductCatV = OBDal.getInstance().get(ESCMProductCategoryV.class,
            objects[1].toString());
        objPRline.setEscmProdcate(objProductCatV);
      }
      if (objects[4] != null && StringUtils.isNotEmpty(objects[4].toString())) {
        Date needDate = new SimpleDateFormat("yyyy-MM-dd")
            .parse(UtilityDAO.convertToGregorian(objects[4].toString()));
        objPRline.setNeedByDate(needDate);
      }
      objPRline.setQuantity(objects[5] == null ? BigDecimal.ZERO : (BigDecimal) objects[5]);
      /*
       * objPRline.setEFINFundsAvailable( objects[6] == null ? BigDecimal.ZERO : (BigDecimal)
       * objects[6]);
       */
      objPRline.setLineNetAmount(objects[7] == null ? BigDecimal.ZERO : (BigDecimal) objects[7]);
      objPRline.setNotesForSupplier(objects[8] == null ? "" : objects[8].toString());
      if (objects[11] != null && StringUtils.isNotEmpty(objects[11].toString())) {
        Requisition objPR = OBDal.getInstance().get(Requisition.class, objects[11].toString());
        objPRline.setRequisition(objPR);
        objPRline.setOrganization(objPR.getOrganization());
      }
      objPRline.setUnitPrice(objects[12] == null ? BigDecimal.ZERO : (BigDecimal) objects[12]);

      OBDal.getInstance().save(objPRline);
      OBDal.getInstance().flush();
      // End of Product
    } catch (ConstraintViolationException e) {
      log4j.error("Exception in insertIntoRequisitionLine() : ", e);
      statusTemp = 2;
    } catch (Exception e) {
      log4j.error("Exception in insertIntoRequisitionLine() : ", e);
      statusTemp = 0;
      // TODO: handle exception
    }
    return statusTemp;
  }

  /**
   * 
   * @param inpRequistionId
   * @return return success once updated lines to frame tree
   */
  @Override
  @SuppressWarnings("rawtypes")
  public int updateRequisitionLine(String inpRequistionId) {
    int status = 1;
    StringBuffer query = null;
    Query reqQuery = null;
    try {
      query = new StringBuffer();
      // START of Product Master
      query.append(
          "select pline.id as parentLine, chline.id as childline from Escm_Csv_Import_Prline csv left ");
      query.append(
          " join csv.requisition req left join req.procurementRequisitionLineList chline join ");
      query.append(
          " req.procurementRequisitionLineList pline where (chline.lineNo)=(csv.lineNo) and ");
      query.append(" (pline.lineNo) =(csv.parentline) and csv.requisition.id=:inpRequistionId ");
      reqQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      reqQuery.setParameter("inpRequistionId", inpRequistionId);
      log4j.debug(" Query : " + query.toString());

      if (reqQuery != null) {
        if (reqQuery.list().size() > 0) {
          for (Iterator iterator = reqQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            // select requisition line
            if (objects[1] != null && StringUtils.isNotEmpty(objects[1].toString())) {
              RequisitionLine objPRline = OBDal.getInstance().get(RequisitionLine.class,
                  objects[1].toString());
              if (objects[0] != null && StringUtils.isNotEmpty(objects[0].toString())) {
                EscmRequisitionlineV objParentLine = OBDal.getInstance()
                    .get(EscmRequisitionlineV.class, objects[0].toString());
                if (objParentLine != null)
                  objPRline.setEscmParentlineno(objParentLine);
              }
              OBDal.getInstance().save(objPRline);
            }
            OBDal.getInstance().flush();
            // End of Product
          }
        }
      }

      /*
       * String selectPRLine =
       * "select  pline.m_requisitionline_id as parentLine,chline.m_requisitionline_id  as childline "
       * + "  from escm_csv_import_prline csv " +
       * " left join m_requisitionline chline on chline.m_requisition_id=csv.m_requisition_id  " +
       * "  and to_number(chline.line)=to_number(csv.line_no) " +
       * "  join m_requisitionline pline on pline.m_requisition_id=csv.m_requisition_id " +
       * "   and to_number(csv.parentline)=to_number(pline.line) " +
       * "  where csv.m_requisition_id=? "; SQLQuery prLineQuery =
       * OBDal.getInstance().getSession().createSQLQuery(selectPRLine); prLineQuery.setParameter(0,
       * inpRequistionId);
       */
    } catch (Exception e) {
      log4j.error("Exception in updateRequisitionLine() : ", e);
      status = 0;
    }
    return status;
  }

  @Override
  public boolean isRowEmpty(Row row) {
    for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
      Cell cell = row.getCell(c);
      if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
          && StringUtils.isNotBlank(cell.toString()))
        return false;
    }
    return true;
  }
}