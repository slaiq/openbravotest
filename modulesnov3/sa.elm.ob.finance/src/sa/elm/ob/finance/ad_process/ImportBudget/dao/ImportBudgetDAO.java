package sa.elm.ob.finance.ad_process.ImportBudget.dao;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.utility.util.Utility;

public class ImportBudgetDAO {
  /**
   * Servlet implementation class Import Budget
   */

  private static final Logger log4j = Logger.getLogger(ImportBudgetDAO.class);
  private Connection conn = null;

  public ImportBudgetDAO(Connection con) {
    this.conn = con;
  }

  /**
   * This method is used to upload CSV file
   * 
   * @param inpBudgetId
   * @param vars
   * @return
   */
  public JSONObject processUploadedCsvFile(String inpBudgetId, VariablesSecureApp vars) {

    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;
    EFINBudget header = OBDal.getInstance().get(EFINBudget.class, inpBudgetId);
    try {
      OBContext.setAdminMode(true);

      isSuccess = UpdateBudgetLines(header);

      // OBDal.getInstance().commitAndClose();
      OBDal.getInstance().flush();

      log4j.debug("isSuccess" + isSuccess);
      if (isSuccess == 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", OBMessageUtils.messageBD("Efin_Import_Record_Failed"));
      } else {
        jsonresult = new JSONObject();
        jsonresult.put("List", "");
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("resultlist", "");
        jsonresult.put("statusMessage", OBMessageUtils.messageBD("Efin_Import_Record_Success"));
      }
    } catch (Exception e) {
      isSuccess = 0;
      jsonresult = new JSONObject();
      try {
        jsonresult.put("List", "");
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("resultlist", "");
        jsonresult.put("statusMessage", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      log4j.error("Exception in Budget Data Import", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonresult;
  }

  /**
   * This method is used to update budget lines
   * 
   * @param header
   * @return
   */
  @SuppressWarnings({ "unused" })
  private int UpdateBudgetLines(EFINBudget header) {
    int status = 1;
    EFINBudgetLines budgetLine = null;
    String strQuery = "";
    PreparedStatement ps = null;
    try {
      Client objClient = OBContext.getOBContext().getCurrentClient();
      strQuery = "update efin_budgetlines set amount = ("
          + "select coalesce(amount,0) from efin_csv_budget_import where efin_csv_budget_import_id = efin_budgetlines.c_validcombination_id "
          + "and uniquecode= efin_budgetlines.uniquecode) ,legacyhisbudgetvalue = (select coalesce(legacyhisbudgetvalue,0) from efin_csv_budget_import "
          + "where efin_csv_budget_import_id = efin_budgetlines.c_validcombination_id and uniquecode= efin_budgetlines.uniquecode) "
          + "where efin_budget_id ='" + header.getId() + "'";
      ps = conn.prepareStatement(strQuery);
      ps.executeUpdate();
    } catch (Exception e) {
      log4j.error("Exception in UpdateBudgetLines : " + e);
      return 0;
    } finally {
      // close connection
      try {
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in Closing connection : " + e);
      }
    }
    return 1;

  }

  /**
   * This method is used to validate CSV file
   * 
   * @param file
   * @param vars
   * @param inpBudgetId
   * @return
   */
  @SuppressWarnings("unused")
  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars, String inpBudgetId) {

    JSONObject resultJSON = new JSONObject();
    JSONArray ListArray = new JSONArray();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;
    Row row = null;
    Cell cell = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    boolean hasInsertPST1 = false;
    PreparedStatement insertPST1 = null;
    boolean value = true;
    int length;

    try {
      EFINBudget header = OBDal.getInstance().get(EFINBudget.class, inpBudgetId);

      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        JSONObject errorList = new JSONObject(), errorJSON = null;
        JSONArray errorListArray = new JSONArray();
        JSONObject uploadDataJSON = null;
        boolean validData = true;
        LinkedHashMap<String, JSONObject> uploadData = new LinkedHashMap<String, JSONObject>();
        Pattern regexTime = Pattern.compile("^\\d{1,2}:\\d{2}$");
        insertPST1 = conn.prepareStatement(
            "insert into efin_csv_budget_import(efin_csv_budget_import_id,ad_org_id,ad_client_id,createdby,updatedby,uniquecode,amount,legacyhisbudgetvalue) values (?, ?,?, ?,?,?,?,?);");

        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          sheet = xssfWorkbook.getSheetAt(s);
          if (sheet == null)
            break;

          int startRow = 1;

          String LineId = "", Uniquecode = "", stramount = "", strlegacy = "";
          BigDecimal amount = BigDecimal.ZERO;
          BigDecimal legacybudvalue = BigDecimal.ZERO;
          JSONObject AmountJSON = null;
          JSONObject LegacybudvalueJSON = null;
          JSONArray AmountArrayJSON = new JSONArray();
          JSONArray LegacybudvalueArrayJSON = new JSONArray();
          JSONObject NegativeJSON = null;
          JSONObject legacyNegativeJSON = null;
          JSONArray NegativeArrayJSON = new JSONArray();
          JSONArray legacyNegativeArrayJSON = new JSONArray();
          JSONObject IDJSON = null;
          JSONArray IDArrayJSON = new JSONArray();
          boolean amountflag = false;
          boolean legacybudvalueflag = false;
          boolean amountvalidationflag = true;
          boolean legacybudvaluevalidationflag = true;
          boolean chknegativeflag = false;
          boolean chklegacynegativeflag = false;
          boolean negativeflag = true;
          boolean legacyNegativeFlag = true;
          boolean LineID = false;
          for (int i = startRow;; i++) {
            row = sheet.getRow(i);

            if (row == null)
              break;

            cell = row.getCell(0);
            if (cell == null) {
              value = chkisnull(getCellValue(cell));
              if (!value) {
                LineId = getCellValue(cell);
                IDJSON = new JSONObject();
                IDJSON.put("LineID", LineId);
                log4j.debug("IDJSON" + IDJSON);
                // IDJSON.put("Uniquecode", row);
                LineID = true;
              }
            }
            if (!LineID) {
              if (cell != null) {
                LineId = Utility.nullToEmpty(getCellValue(cell));
              } else
                LineId = null;
            }
            cell = row.getCell(1);
            log4j.debug("cell" + cell);
            if (cell != null) {
              Uniquecode = Utility.nullToEmpty(getCellValue(cell));
              if (LineID) {
                IDJSON.put("uniquecode", Uniquecode);
              }
            } else
              Uniquecode = null;

            cell = row.getCell(3);
            if (cell != null) {
              amountvalidationflag = isNumeric(getCellValue(cell));
              negativeflag = chkisnegative(getCellValue(cell));
              if (!amountvalidationflag) {
                stramount = getCellValue(cell);
                AmountJSON = new JSONObject();
                AmountJSON.put("amount", stramount);
                AmountJSON.put("uniquecode", Uniquecode);
                log4j.debug("IDJSON" + IDJSON);
                AmountArrayJSON.put(AmountJSON);
                amountflag = true;
              } else if (!negativeflag) {
                stramount = getCellValue(cell);
                NegativeJSON = new JSONObject();
                NegativeJSON.put("amount", stramount);
                NegativeJSON.put("uniquecode", Uniquecode);
                log4j.debug("NegativeJSON" + NegativeJSON);
                NegativeArrayJSON.put(NegativeJSON);
                chknegativeflag = true;
              } else {
                /*
                 * length = getCellValue(cell).toString().length(); if (length > 10) { amount = new
                 * BigDecimal(getCellValue(cell).replace(",", "").substring(0, 9)); } else
                 */
                amount = new BigDecimal(Utility.nullToEmpty(getCellValue(cell)));
              }
            } else {
              amount = null;
            }

            cell = row.getCell(4);
            if (cell != null) {
              legacybudvaluevalidationflag = isNumeric(getCellValue(cell));
              legacyNegativeFlag = chkisnegative(getCellValue(cell));
              if (!legacybudvaluevalidationflag) {
                strlegacy = getCellValue(cell);
                LegacybudvalueJSON = new JSONObject();
                // AmountJSON.put("amount", stramount);
                LegacybudvalueJSON.put("legacyhisbudgetvalue", strlegacy);
                LegacybudvalueJSON.put("uniquecode", Uniquecode);
                log4j.debug("IDJSON" + IDJSON);
                LegacybudvalueArrayJSON.put(LegacybudvalueJSON);
                legacybudvalueflag = true;
              } else if (!legacyNegativeFlag) {
                strlegacy = getCellValue(cell);
                legacyNegativeJSON = new JSONObject();
                legacyNegativeJSON.put("legacyhisbudgetvalue", strlegacy);
                legacyNegativeJSON.put("uniquecode", Uniquecode);
                log4j.debug("NegativeJSON" + legacyNegativeJSON);
                legacyNegativeArrayJSON.put(legacyNegativeJSON);
                chklegacynegativeflag = true;
              } else {
                legacybudvalue = new BigDecimal(Utility.nullToEmpty(getCellValue(cell)));
              }
            } else {
              legacybudvalue = null;
            }

            log4j.debug("LineID" + LineID);
            if (LineID) {
              log4j.debug("IDJSON" + IDJSON);
              IDArrayJSON.put(IDJSON);
            }
            if (LineID) {
              if (IDArrayJSON.length() > 0) {
                errorList.put("List", IDArrayJSON);
                resultJSON.put("status", "0");
                resultJSON.put("recordsFailed", "");
                resultJSON.put("resultlist", errorList);
                resultJSON.put("statusMessage", OBMessageUtils.messageBD("Efin_Import_ID_Null"));
              }
            } else if (!amountflag && !chknegativeflag && !legacybudvalueflag
                && !chklegacynegativeflag) {

              insertPST1.setString(1, LineId);
              insertPST1.setString(2, vars.getOrg());
              insertPST1.setString(3, vars.getClient());
              insertPST1.setString(4, vars.getUser());
              insertPST1.setString(5, vars.getUser());
              insertPST1.setString(6, Uniquecode);
              insertPST1.setBigDecimal(7, amount);
              insertPST1.setBigDecimal(8, legacybudvalue);
              insertPST1.addBatch();
              hasInsertPST1 = true;

              if (hasInsertPST1) {
                log4j.debug("insertPST1" + insertPST1.toString());
                insertPST1.executeBatch();
              }

              ListArray = ChkUniqueCodeinBudgetLine(header);
              if (ListArray.length() > 0) {
                errorList.put("List", ListArray);
                resultJSON.put("status", "0");
                resultJSON.put("recordsFailed", "");
                resultJSON.put("resultlist", errorList);
                resultJSON.put("statusMessage", OBMessageUtils.messageBD("Efin_Import_UniqueCode"));
              } else {
                errorList.put("List", ListArray);
                resultJSON.put("status", "1");
                resultJSON.put("recordsFailed", "");
                resultJSON.put("resultlist", errorList);
                resultJSON.put("statusMessage",
                    OBMessageUtils.messageBD("Efin_Import_Csv_Success"));
              }
            } else {
              if (AmountArrayJSON.length() > 0) {
                errorList.put("List", AmountArrayJSON);
                resultJSON.put("status", "0");
                resultJSON.put("recordsFailed", "");
                resultJSON.put("resultlist", errorList);
                resultJSON.put("statusMessage",
                    OBMessageUtils.messageBD("Efin_Import_Amount_Invalid"));
              } else if (NegativeArrayJSON.length() > 0) {
                errorList.put("List", NegativeArrayJSON);
                resultJSON.put("status", "0");
                resultJSON.put("recordsFailed", "");
                resultJSON.put("resultlist", errorList);
                resultJSON.put("statusMessage",
                    OBMessageUtils.messageBD("Efin_Import_Amount_Negative"));
              } else if (LegacybudvalueArrayJSON.length() > 0) {
                errorList.put("List", LegacybudvalueArrayJSON);
                resultJSON.put("status", "0");
                resultJSON.put("recordsFailed", "");
                resultJSON.put("resultlist", errorList);
                resultJSON.put("statusMessage",
                    OBMessageUtils.messageBD("Efin_Import_Legacy_Invalid"));
              } else if (legacyNegativeArrayJSON.length() > 0) {
                errorList.put("List", legacyNegativeArrayJSON);
                resultJSON.put("status", "0");
                resultJSON.put("recordsFailed", "");
                resultJSON.put("resultlist", errorList);
                resultJSON.put("statusMessage",
                    OBMessageUtils.messageBD("Efin_Import_Amount_Negative"));
              } else {
                errorList.put("List", "");
                resultJSON.put("status", "1");
                resultJSON.put("recordsFailed", "");
                resultJSON.put("resultlist", errorList);
                resultJSON.put("statusMessage",
                    OBMessageUtils.messageBD("Efin_Import_Csv_Success"));
              }
            }

          }
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in uploadTimeSheet() : ", e);
      try {

      } catch (Exception ee) {
        log4j.error("Exception in uploadTimeSheet() : ", ee);
      }
      return resultJSON;
    }
    return resultJSON;

  }

  /**
   * This method is used to get cell value
   * 
   * @param cell
   * @return
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

  /**
   * This method is used to check unique code in budget line
   * 
   * @param header
   * @return
   */
  private JSONArray ChkUniqueCodeinBudgetLine(EFINBudget header) {
    JSONObject errorJSON = new JSONObject();
    JSONArray errorListArray = new JSONArray();
    try {

      Client objClient = OBContext.getOBContext().getCurrentClient();
      PreparedStatement ps = null;
      ResultSet rs = null;
      ps = conn.prepareStatement(
          "select uniquecode from efin_csv_budget_import where efin_csv_budget_import_id not in (\n"
              + " select c_validcombination_id from efin_budgetlines where efin_budget_id = '"
              + header.getId() + "' )\n" + "  or uniquecode not in (\n"
              + " select uniquecode from efin_budgetlines where efin_budget_id = '" + header.getId()
              + "') \n" + "and ad_client_id = '" + objClient.getId() + "' ");

      rs = ps.executeQuery();
      log4j.debug("query" + ps.toString());
      while (rs.next()) {
        errorJSON = new JSONObject();
        errorJSON.put("uniquecode", rs.getString("uniquecode"));
        errorListArray.put(errorJSON);
      }
      // Close conection
      if (ps != null) {
        ps.close();
      }
      if (rs != null) {
        rs.close();
      }
    } catch (Exception e) {
      log4j.error("Exception in ChkUniqueCodeinBudgetLine : " + e);
    }
    return errorListArray;

  }

  /**
   * This method is used to check isNumeric
   * 
   * @param str
   * @return
   */
  public static boolean isNumeric(String str) {
    try {
      if (str.equals("") && str.isEmpty()) {
        str = "0";
      }
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  /**
   * This method is used to check is null
   * 
   * @param cell
   * @return
   */
  public static boolean chkisnull(String cell) {
    try {
      if (cell == null || cell.equals("")) {
        return false;
      } else {
        return true;
      }

    } catch (Exception e) {
      log4j.error("Exception in chkisnull : " + e);
    }
    return true;
  }

  /**
   * This method is used to check is negative
   * 
   * @param cell
   * @return
   */
  public static boolean chkisnegative(String cell) {
    try {
      if (cell.equals("") && cell.isEmpty()) {
        cell = "0";
      }
      if (new BigDecimal(cell).compareTo(BigDecimal.ZERO) < 0) {
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      log4j.error("Exception in chkisnegative : " + e);

    }
    return true;
  }
}