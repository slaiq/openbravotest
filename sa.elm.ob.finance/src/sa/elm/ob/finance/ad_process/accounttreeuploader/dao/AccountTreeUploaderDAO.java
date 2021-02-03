package sa.elm.ob.finance.ad_process.accounttreeuploader.dao;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.accounting.coa.Element;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

import sa.elm.ob.finance.EFIN_CSV_Import_ElementVal;
import sa.elm.ob.finance.ad_process.paymentoutmof.UploadMOFVO;
import sa.elm.ob.utility.util.Utility;

public class AccountTreeUploaderDAO {
  Connection con = null;
  private static Logger log4j = Logger.getLogger(AccountTreeUploaderDAO.class);
  UploadMOFVO VO = null;
  final static String quoted = "\"(:?[^\"]|\"\")+\"";
  final static String projectTreeId = "A02E9371401C4BDF918AEAED62F91A08";

  public AccountTreeUploaderDAO(Connection con) {
    this.con = con;
  }

  /**
   * 
   * @param file
   * @param vars
   * @return success once file processed
   */
  public JSONObject processUploadFile(File file, VariablesSecureApp vars, String inpElementId) {
    JSONObject jsonresult = new JSONObject();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;
    Row row = null;

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
              jsonresult = importDataInDatabase(fields, inpElementId);
              result.clear();
            }
          }
        }
      }

      if (jsonresult.has("status") && "1".equals(jsonresult.getString("status"))) {
        // Insert into actual table
        jsonresult = insertIntoElementValue(inpElementId);
        if (jsonresult.has("status") && "1".equals(jsonresult.getString("status"))) {
          // Insert into tree
          jsonresult = updateTree(inpElementId);
          if (jsonresult.has("status") && "1".equals(jsonresult.getString("status"))) {
            // once tree framed remove data from temp table
            OBQuery<EFIN_CSV_Import_ElementVal> obQuery = OBDal.getInstance().createQuery(
                EFIN_CSV_Import_ElementVal.class,
                "as e where e.accountTree.id='" + inpElementId + "' ");
            if (obQuery.list().size() > 0) {
              for (EFIN_CSV_Import_ElementVal delcsvObj : obQuery.list()) {
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
   * @param elementId
   * @return success valid xlsx file
   */
  @SuppressWarnings("unused")
  public JSONObject processValidateFile(File file, VariablesSecureApp vars, String inpElementId) {
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
    StringBuffer searchKey = new StringBuffer();
    String br = "";
    ArrayList<String> searchKeyList = new ArrayList<String>();
    boolean isDuplicate = false;

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
          int startRow = 1;
          List<String> allowedAccountType = Arrays.asList("A", "L", "O", "E", "R", "M");
          List<String> allowedAccountSign = Arrays.asList("N", "D", "C", "E");
          List<String> allowedElementLevel = Arrays.asList("C", "D", "E", "S");
          List<String> booleanData = Arrays.asList("Y", "N");

          for (int i = startRow;; i++) {
            row = sheet.getRow(i);
            if (row == null)
              break;
            int noOfColumns = row.getLastCellNum();
            description.append(br);
            if (noOfColumns < 8) {
              failed += 1;
              description.append("Partial record at Row No.<b>" + (i + 1)
                  + "</b> in the data file, expected(8).<br>");
              br = "<br>";
            } else if (noOfColumns > 8) {
              failed += 1;
              description.append("Row No.<b>" + (i + 1)
                  + "</b> in the data file has more fields than expected(8).<br>");
              br = "<br>";
            } else {
              // checking duplicate search key exists in uploading file
              if (searchKeyList.contains(row.getCell(0).toString())) {
                failed += 1;
                isDuplicate = true;
                // appending duplicate search key
                searchKey.append(" ," + getCellValue(row.getCell(0)));
              } else {
                searchKeyList.add(row.getCell(0).toString());
                // Search Key
                if (row.getCell(0) != null && StringUtils.isNotEmpty((row.getCell(0)).toString())) {
                  OBCriteria<ElementValue> elementValCr = OBDal.getInstance()
                      .createCriteria(ElementValue.class);
                  elementValCr.add(Restrictions.eq(ElementValue.PROPERTY_ACCOUNTINGELEMENT + ".id",
                      inpElementId));
                  elementValCr.add(Restrictions.ilike(ElementValue.PROPERTY_SEARCHKEY,
                      getCellValue(row.getCell(0))));
                  elementValCr.setMaxResults(1);
                  List<ElementValue> elementValList = elementValCr.list();
                  if (elementValList.size() > 0) {// Element Value exists
                    failed += 1;
                    description
                        .append("Element Value with search key <b>" + getCellValue(row.getCell(0))
                            + "</b> at line No.<b>" + (i + 1) + "</b> already exists.<br>");
                    br = "<br>";
                  }
                } else {
                  failed += 1;
                  description
                      .append("Search key is missing in at line No.<b>" + (i + 1) + "</b>.<br>");
                  br = "<br>";
                }

                // Name
                if (StringUtils.isEmpty((row.getCell(1)).toString())) {
                  failed += 1;
                  description.append("Name is missing in at line No.<b>" + (i + 1) + "</b>.<br>");
                  br = "<br>";
                }

                // Account Type
                if (row.getCell(4) != null && StringUtils.isNotEmpty((row.getCell(4)).toString())) {

                  if (!allowedAccountType.contains((row.getCell(4)).toString())) {
                    failed += 1;
                    description.append("Invalid Account Type <b>" + getCellValue(row.getCell(4))
                        + "</b> at line No.<b>" + (i + 1)
                        + "</b>. Must any one of (\"A\", \"L\", \"O\", \"E\", \"R\", \"M\") <br>");
                    br = "<br>";
                  }

                } else {
                  failed += 1;
                  description
                      .append("Account Type is missing in at line No.<b>" + (i + 1) + "</b>.<br>");
                  br = "<br>";
                }

                // Account Sign
                if (row.getCell(5) != null && StringUtils.isNotEmpty((row.getCell(5)).toString())) {

                  if (!allowedAccountSign.contains((row.getCell(5)).toString())) {
                    failed += 1;
                    description.append("Invalid Account Sign <b>" + getCellValue(row.getCell(5))
                        + "</b> at line No.<b>" + (i + 1)
                        + "</b>. Must any one of (\"N\", \"D\", \"C\", \"E\") <br>");
                    br = "<br>";
                  }

                } else {
                  failed += 1;
                  description
                      .append("Account Sign is missing in at line No.<b>" + (i + 1) + "</b>.<br>");
                  br = "<br>";
                }

                // Element Level
                if (row.getCell(6) != null && StringUtils.isNotEmpty((row.getCell(6)).toString())) {

                  if (!allowedElementLevel.contains((row.getCell(6)).toString())) {
                    failed += 1;
                    description.append("Invalid Element Level <b>" + getCellValue(row.getCell(6))
                        + "</b> at line No.<b>" + (i + 1)
                        + "</b>. Must any one of (\"C\", \"D\", \"E\", \"S\") <br>");
                    br = "<br>";
                  }

                } else {
                  failed += 1;
                  description
                      .append("Element Level is missing in at line No.<b>" + (i + 1) + "</b>.<br>");
                  br = "<br>";
                }

                // Is Department Fund
                if (row.getCell(7) != null && StringUtils.isNotEmpty((row.getCell(7)).toString())) {

                  if (!booleanData.contains((row.getCell(7)).toString())) {
                    failed += 1;
                    description.append("Invalid IsDepartmentFund <b>" + getCellValue(row.getCell(7))
                        + "</b> at line No.<b>" + (i + 1)
                        + "</b>. Must any one of (\"Y\", \"N\") <br>");
                    br = "<br>";
                  }

                } else {
                  failed += 1;
                  description.append(
                      "IsDepartmentFund is missing in at line No.<b>" + (i + 1) + "</b>.<br>");
                  br = "<br>";
                }

              }
            }
          }
        }
        if (failed > 0) {
          if (isDuplicate) {
            description.append("Duplicate search key exists in uploading file -  ");
            description.append(searchKey.toString().replaceFirst(" ,", ""));
          }
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
    } catch (final Exception e) {
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
   */

  private List<String> parseExcel(Row row) {
    final List<String> strings = new ArrayList<String>();
    for (int cn = 0; cn < 8; cn++) {
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
   * 
   * @param fields
   * @return success when data inserted in active table
   */
  private JSONObject importDataInDatabase(String[] fields, String inpElementId) {
    JSONObject jsonresult = new JSONObject();

    try {
      EFIN_CSV_Import_ElementVal csvImport = null;
      csvImport = OBProvider.getInstance().get(EFIN_CSV_Import_ElementVal.class);
      csvImport.setSearchKey(fields[0].toString());
      csvImport.setCommercialName(fields[1].toString());
      if (!StringUtils.isEmpty(fields[2].toString())) {
        csvImport.setDescription(fields[2].toString());
      }
      if (!StringUtils.isEmpty(fields[3].toString())) {
        csvImport.setParentValue(fields[3].toString());
      }
      csvImport.setAccountType(fields[4].toString());
      csvImport.setAccountSign(fields[5].toString());
      csvImport.setElementLevel(fields[6].toString());
      csvImport.setDepartmentfund(fields[7].toString().equals("Y") ? true : false);
      Element elem = OBDal.getInstance().get(Element.class, inpElementId);
      csvImport.setAccountTree(elem);
      OBDal.getInstance().save(csvImport);
      OBDal.getInstance().flush();

      jsonresult.put("status", "1");
      jsonresult.put("statusMessage", "Records Inserted Succesfully");
    } catch (Exception e) {
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
   * @return success once requisition line inserted
   */
  private JSONObject insertIntoElementValue(String inpElementId) {
    JSONObject jsonresult = new JSONObject();
    int j = 0;
    try {
      OBCriteria<EFIN_CSV_Import_ElementVal> csvCr = OBDal.getInstance()
          .createCriteria(EFIN_CSV_Import_ElementVal.class);
      csvCr.add(
          Restrictions.eq(EFIN_CSV_Import_ElementVal.PROPERTY_ACCOUNTTREE + ".id", inpElementId));
      List<EFIN_CSV_Import_ElementVal> ls = csvCr.list();
      if (ls.size() > 0) {
        for (int i = 0; i < ls.size(); i++) {
          EFIN_CSV_Import_ElementVal lne = ls.get(i);
          ElementValue elemVal = OBProvider.getInstance().get(ElementValue.class);
          // set header org in line org
          Element accounttree = OBDal.getInstance().get(Element.class, inpElementId);
          elemVal.setOrganization(accounttree.getOrganization());

          elemVal.setSearchKey(lne.getSearchKey());
          elemVal.setName(lne.getCommercialName());
          if (!StringUtils.isEmpty(lne.getDescription())) {
            elemVal.setDescription(lne.getDescription());
          }
          elemVal.setAccountType(lne.getAccountType());
          elemVal.setAccountSign(lne.getAccountSign());
          elemVal.setElementLevel(lne.getElementLevel());
          elemVal.setEfinIsdeptfund(lne.isDepartmentfund());
          Element elem = OBDal.getInstance().get(Element.class, inpElementId);
          elemVal.setAccountingElement(elem);
          elemVal.setEfinImpParent(lne.getParentValue());
          OBDal.getInstance().save(elemVal);
          if ((j % 100) == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          }
          j++;
        }
        OBDal.getInstance().flush();
      }
      jsonresult.put("status", "1");
      jsonresult.put("statusMessage", "Records Inserted Succesfully");
    } catch (Exception e) {
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
  private JSONObject updateTree(String inpElementId) throws OBException {
    JSONObject jsonresult = new JSONObject();
    try {
      OBCriteria<EFIN_CSV_Import_ElementVal> csvCr = OBDal.getInstance()
          .createCriteria(EFIN_CSV_Import_ElementVal.class);
      csvCr.add(
          Restrictions.eq(EFIN_CSV_Import_ElementVal.PROPERTY_ACCOUNTTREE + ".id", inpElementId));
      List<EFIN_CSV_Import_ElementVal> ls = csvCr.list();
      if (ls.size() > 0) {
        for (int i = 0; i < ls.size(); i++) {
          EFIN_CSV_Import_ElementVal lne = ls.get(i);

          // Has Parent
          if (!StringUtils.isEmpty(lne.getParentValue())) {
            // Fetch Element Value
            OBCriteria<ElementValue> elemCr = OBDal.getInstance()
                .createCriteria(ElementValue.class);
            elemCr.add(
                Restrictions.eq(ElementValue.PROPERTY_ACCOUNTINGELEMENT + ".id", inpElementId));
            elemCr.add(Restrictions.ilike(ElementValue.PROPERTY_SEARCHKEY, lne.getSearchKey()));
            elemCr.setMaxResults(1);
            List<ElementValue> childList = elemCr.list();
            if (childList.size() > 0) {
              // Fetch Element Value's TreeNode
              OBCriteria<TreeNode> treeNode = OBDal.getInstance().createCriteria(TreeNode.class);
              treeNode.add(Restrictions.eq(TreeNode.PROPERTY_NODE, childList.get(0).getId()));
              List<TreeNode> nodeList = treeNode.list();
              if (nodeList.size() > 0) {
                String nodeId = nodeList.get(0).getId();

                // Fetch Parent's Element Value
                OBCriteria<ElementValue> elemCr1 = OBDal.getInstance()
                    .createCriteria(ElementValue.class);
                elemCr1.add(
                    Restrictions.eq(ElementValue.PROPERTY_ACCOUNTINGELEMENT + ".id", inpElementId));
                elemCr1
                    .add(Restrictions.ilike(ElementValue.PROPERTY_SEARCHKEY, lne.getParentValue()));
                elemCr1.setMaxResults(1);
                List<ElementValue> parentList = elemCr1.list();
                if (parentList.size() > 0) {
                  // Update parent in tree node
                  TreeNode treeNod = OBDal.getInstance().get(TreeNode.class, nodeId);
                  treeNod.setReportSet(parentList.get(0).getId());
                  OBDal.getInstance().save(treeNod);

                  // Update issummary
                  ElementValue ElemtSummary = OBDal.getInstance().get(ElementValue.class,
                      parentList.get(0).getId());
                  ElemtSummary.setSummaryLevel(true);
                  OBDal.getInstance().save(ElemtSummary);
                  OBDal.getInstance().flush();
                } else {
                  jsonresult.put("status", "0");
                  jsonresult.put("statusMessage", "ElementValue with search key "
                      + lne.getParentValue() + " does not exists!!");
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
      try {
        jsonresult.put("status", "0");
        jsonresult.put("statusMessage", "Record Insert Failed");
      } catch (JSONException e1) {
      }
    }
    return jsonresult;
  }

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

  public Currency getCurrency(String clientId) {
    Currency curr = null;
    OBCriteria<Client> clientCr = OBDal.getInstance().createCriteria(Client.class);
    clientCr.add(Restrictions.eq(Currency.PROPERTY_ID, clientId));
    clientCr.setMaxResults(1);
    List<Client> clientList = clientCr.list();
    if (clientList.size() > 0) {// Element Value exists
      curr = clientList.get(0).getCurrency();
    }
    return curr;
  }

}