package sa.elm.ob.finance.ad_process.projectuploader.dao;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.project.Project;

import sa.elm.ob.finance.Efin_Csv_Import_Project;
import sa.elm.ob.finance.ad_process.paymentoutmof.UploadMOFVO;
import sa.elm.ob.utility.util.Utility;

public class ProjectUploaderDAO {
  Connection con = null;
  private static Logger log4j = Logger.getLogger(ProjectUploaderDAO.class);
  UploadMOFVO VO = null;
  final static String quoted = "\"(:?[^\"]|\"\")+\"";
  final static String projectTreeId = "A02E9371401C4BDF918AEAED62F91A08";

  public ProjectUploaderDAO(Connection con) {
    this.con = con;
  }

  /**
   * This method is used to processUploadFile
   * 
   * @param file
   * @param vars
   * @return success once file processed
   */
  public JSONObject processUploadFile(File file, VariablesSecureApp vars) {
    JSONObject jsonresult = new JSONObject();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;
    Row row = null;
    String uploadId = SequenceIdData.getUUID();

    try {
      OBContext.setAdminMode(true);
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
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
            else {
              List<String> result = parseExcel(row);
              if (result == null)
                continue;

              String fields[] = (String[]) result.toArray(new String[0]);
              for (int j = 0; j < fields.length; j++) {
                fields[j] = fields[j].replace("\"", "");
              }
              // Insert into temp table
              Currency curr = getCurrency(vars.getClient());
              jsonresult = importDataInDatabase(fields, uploadId, curr);
              result.clear();
            }
          }
        }
      }

      if (jsonresult.has("status") && "1".equals(jsonresult.getString("status"))) {
        // Insert into actual table
        jsonresult = insertIntoProject(uploadId);
        if (jsonresult.has("status") && "1".equals(jsonresult.getString("status"))) {
          // Insert into tree
          jsonresult = updateTree(uploadId);
          if (jsonresult.has("status") && "1".equals(jsonresult.getString("status"))) {
            // once tree framed remove data from temp table
            OBQuery<Efin_Csv_Import_Project> obQuery = OBDal.getInstance().createQuery(
                Efin_Csv_Import_Project.class, "as e where e.importId='" + uploadId + "' ");
            if (obQuery.list().size() > 0) {
              for (Efin_Csv_Import_Project delcsvObj : obQuery.list()) {
                OBDal.getInstance().remove(delcsvObj);
              }
            }
          }
        }
      }

      OBDal.getInstance().flush();
      if (jsonresult.has("status") && "1".equals(jsonresult.getString("status"))) {
        OBDal.getInstance().commitAndClose();
      } else {
        OBDal.getInstance().rollbackAndClose();
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      jsonresult = new JSONObject();
      try {
        jsonresult.put("status", "0");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", e.getMessage());
      } catch (JSONException e1) {
        log4j.error("Exception in JSON :" + e);
      }
      log4j.error("Exception in Requisition Line Data Import", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonresult;
  }

  /**
   * 
   * @param file
   * @param vars
   * @return success valid xlsx file
   */
  @SuppressWarnings("unused")
  public JSONObject processValidateFile(File file, VariablesSecureApp vars) {
    JSONObject resultJSON = new JSONObject();
    JSONArray ListArray = new JSONArray();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;
    Row row = null;
    Cell cell = null;

    boolean value = true;
    int length;
    int failed = 0;
    int lineNo = 1, duplicateline = 1;
    StringBuffer description = new StringBuffer();
    String br = "";

    try {
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        JSONObject errorList = new JSONObject(), errorJSON = null;
        JSONArray errorListArray = new JSONArray();
        JSONObject uploadDataJSON = null;
        boolean validData = true;
        LinkedHashMap<String, JSONObject> uploadData = new LinkedHashMap<String, JSONObject>();
        Pattern regexTime = Pattern.compile("^\\d{1,2}:\\d{2}$");
        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          sheet = xssfWorkbook.getSheetAt(s);
          if (sheet == null)
            break;

          int startRow = 1;

          String LineId = "", Uniquecode = "", stramount = "";
          BigDecimal amount = BigDecimal.ZERO;
          JSONObject AmountJSON = null;
          JSONArray AmountArrayJSON = new JSONArray();
          JSONObject NegativeJSON = null;
          JSONArray NegativeArrayJSON = new JSONArray();
          JSONObject IDJSON = null;
          JSONArray IDArrayJSON = new JSONArray();
          boolean amountflag = false;
          boolean amountvalidationflag = true;
          boolean chknegativeflag = false;
          boolean Negativeflag = true;
          boolean LineID = false;
          for (int i = startRow;; i++) {
            row = sheet.getRow(i);
            if (row == null)
              break;
            int noOfColumns = row.getLastCellNum();
            description.append(br);
            if (noOfColumns < 2) {
              failed += 1;
              description.append("Partial record at Row No.<b>" + i + "</b> in the data file.<br>");
              br = "<br>";
            } else if (noOfColumns > 4) {
              failed += 1;
              description.append("Row No.<b>" + i
                  + "</b> in the data file has more fields than expected(12).<br>");
              br = "<br>";
            } else {
              if (row.getCell(0) != null && StringUtils.isNotEmpty((row.getCell(0)).toString())) {
                OBCriteria<Project> projectListCr = OBDal.getInstance()
                    .createCriteria(Project.class);
                projectListCr.add(
                    Restrictions.ilike(Product.PROPERTY_SEARCHKEY, getCellValue(row.getCell(0))));
                projectListCr.setMaxResults(1);
                List<Project> projectList = projectListCr.list();
                if (projectList.size() > 0) {// Project exists
                  failed += 1;
                  description.append("Project with search key <b>" + row.getCell(0)
                      + "</b> at line No.<b>" + i + "</b> already exists.<br>");
                  br = "<br>";
                }
              }
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
          resultJSON.put("statusMessage", "CSV Validated Succesfully");
        }
      }
    } catch (

    final Exception e) {
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
   * Parsing .xlsx file using regular Expression
   * 
   * @param row
   * @return
   */
  private List<String> parseExcel(Row row) {
    final List<String> strings = new ArrayList<String>();
    for (int cn = 0; cn < 4; cn++) {
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
   * @param cell
   * @return value of cell based on type
   */
  private String getCellValue(Cell cell) {
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
        return cell.getStringCellValue();
      } else if (Cell.CELL_TYPE_FORMULA == cell.getCellType()) {
        cell.setCellType(Cell.CELL_TYPE_STRING);
        return cell.getStringCellValue();
      } else
        return Utility.nullToEmpty(cell.getRichStringCellValue().getString());
    } catch (final Exception e) {
      log4j.error("Exception in getCellValue", e);
      return "";
    }
  }

  /**
   * This method is used to import Data In Database
   * 
   * @param fields
   * @return success when data inserted in active table
   */
  private JSONObject importDataInDatabase(String[] fields, String uploadId, Currency curr) {
    JSONObject jsonresult = new JSONObject();

    try {
      Efin_Csv_Import_Project csvImport = null;
      csvImport = OBProvider.getInstance().get(Efin_Csv_Import_Project.class);
      csvImport.setValue(fields[0].toString());
      csvImport.setName(fields[1].toString());
      if (!StringUtils.isEmpty(fields[2].toString())) {
        csvImport.setDescription(fields[2].toString());
      }
      if (fields.length > 3 && !StringUtils.isEmpty(fields[3].toString())) {
        csvImport.setParentvalue(fields[3].toString());
      }
      csvImport.setCurrency(curr);
      csvImport.setImportId(uploadId);
      OBDal.getInstance().save(csvImport);
      OBDal.getInstance().flush();

      jsonresult.put("status", "1");
      jsonresult.put("statusMessage", "Records Inserted Succesfully");
    } catch (Exception e) {
      try {
        jsonresult.put("status", "0");
        jsonresult.put("statusMessage", "Record Insert Failed");
      } catch (JSONException e1) {
        log4j.error("Exception in JSON : " + e);
      }
      log4j.error("Exception in importDataInDatabase : " + e);
    }
    return jsonresult;
  }

  /**
   * 
   * @param inpRequistionId
   * @return success once requisition line inserted
   */
  private JSONObject insertIntoProject(String importId) {
    JSONObject jsonresult = new JSONObject();
    try {
      OBCriteria<Efin_Csv_Import_Project> csvCr = OBDal.getInstance()
          .createCriteria(Efin_Csv_Import_Project.class);
      csvCr.add(Restrictions.eq(Efin_Csv_Import_Project.PROPERTY_IMPORTID, importId));
      List<Efin_Csv_Import_Project> ls = csvCr.list();
      if (ls.size() > 0) {
        for (int i = 0; i < ls.size(); i++) {
          Efin_Csv_Import_Project lne = ls.get(i);
          Project proj = OBProvider.getInstance().get(Project.class);

          proj.setSearchKey(lne.getValue());
          proj.setName(lne.getName());
          if (!StringUtils.isEmpty(lne.getDescription())) {
            proj.setDescription(lne.getDescription());
          }
          proj.setCurrency(lne.getCurrency());
          OBDal.getInstance().save(proj);
          OBDal.getInstance().flush();
        }
      }
      jsonresult.put("status", "1");
      jsonresult.put("statusMessage", "Records Inserted Succesfully");
    } catch (Exception e) {
      log4j.error("Exception in insertIntoProject : " + e);
      try {
        jsonresult.put("status", "0");
        jsonresult.put("statusMessage", "Record Insert Failed");
      } catch (JSONException e1) {
      }
    }
    return jsonresult;
  }

  /**
   * 
   * @param inpRequistionId
   * @return return success once updated lines to frame tree
   */
  @SuppressWarnings("unchecked")
  private JSONObject updateTree(String importId) throws OBException {
    JSONObject jsonresult = new JSONObject();
    try {
      OBCriteria<Efin_Csv_Import_Project> csvCr = OBDal.getInstance()
          .createCriteria(Efin_Csv_Import_Project.class);
      csvCr.add(Restrictions.eq(Efin_Csv_Import_Project.PROPERTY_IMPORTID, importId));
      List<Efin_Csv_Import_Project> ls = csvCr.list();
      if (ls.size() > 0) {
        for (int i = 0; i < ls.size(); i++) {
          Efin_Csv_Import_Project lne = ls.get(i);

          // Has Parent
          if (!StringUtils.isEmpty(lne.getParentvalue())) {
            // Fetch Project
            OBCriteria<Project> projCr = OBDal.getInstance().createCriteria(Project.class);
            projCr.add(Restrictions.ilike(Project.PROPERTY_SEARCHKEY, lne.getValue()));
            projCr.setMaxResults(1);
            List<Project> childList = projCr.list();
            if (childList.size() > 0) {
              // Fetch Project's TreeNode
              OBCriteria<TreeNode> treeNode = OBDal.getInstance().createCriteria(TreeNode.class);
              treeNode.add(Restrictions.eq(TreeNode.PROPERTY_NODE, childList.get(0).getId()));
              List<TreeNode> nodeList = treeNode.list();
              if (nodeList.size() > 0) {
                String nodeId = nodeList.get(0).getId();

                // Fetch Parent's Project
                OBCriteria<Project> projCr1 = OBDal.getInstance().createCriteria(Project.class);
                projCr1.add(Restrictions.ilike(Project.PROPERTY_SEARCHKEY, lne.getParentvalue()));
                projCr1.setMaxResults(1);
                List<Project> parentList = projCr1.list();
                if (parentList.size() > 0) {
                  // Update parent in tree node
                  TreeNode treeNod = OBDal.getInstance().get(TreeNode.class, nodeId);
                  treeNod.setReportSet(parentList.get(0).getId());
                  OBDal.getInstance().save(treeNod);

                  // Update issummary
                  Project projSum = OBDal.getInstance().get(Project.class,
                      parentList.get(0).getId());
                  projSum.setSummaryLevel(true);
                  OBDal.getInstance().save(projSum);
                  OBDal.getInstance().flush();
                } else {
                  jsonresult.put("status", "0");
                  jsonresult.put("statusMessage",
                      "Project with search key " + lne.getParentvalue() + " does not exists!!");
                  break;
                }
              }
            }
          }
          jsonresult.put("status", "1");
          jsonresult.put("statusMessage", "Records Inserted Succesfully");
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in updateTree : " + e);
      try {
        jsonresult.put("status", "0");
        jsonresult.put("statusMessage", "Record Insert Failed");
      } catch (JSONException e1) {
        log4j.error("JSONException  : " + e);
      }
    }
    return jsonresult;
  }

  /**
   * This method is used to getTable ID
   * 
   * @param tabId
   * @return
   */
  public String getTableId(String tabId) {
    ResultSet rs = null;
    PreparedStatement st = null;
    String tableID = "";

    try {
      st = con.prepareStatement("select ad_table_id from ad_tab  where ad_tab_id  =? ");
      st.setString(1, tabId);
      log4j.debug(st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        tableID = rs.getString("ad_table_id");
      }
    } catch (Exception e) {
      log4j.error("Exception in getTableId :", e);
      return tableID;
    }
    return tableID;
  }

  /**
   * This method is used to getCurrency
   * 
   * @param clientId
   * @return
   */
  public Currency getCurrency(String clientId) {
    Currency curr = null;
    OBCriteria<Client> clientCr = OBDal.getInstance().createCriteria(Client.class);
    clientCr.add(Restrictions.eq(Currency.PROPERTY_ID, clientId));
    clientCr.setMaxResults(1);
    List<Client> clientList = clientCr.list();
    if (clientList.size() > 0) {// Project exists
      curr = clientList.get(0).getCurrency();
    }
    return curr;
  }

}