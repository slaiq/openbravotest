package sa.elm.ob.finance.ad_process.ImportBudgetAdjustment.dao;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.actionHandler.dao.AdjustmentAddLineHandlerDao;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.properties.Resource;
import sa.elm.ob.utility.util.Utility;

public class ImportBudgetAdjustmentDAO {
  /**
   * Data Access layer to Import Budget adjustment Lines In Budget adjustment Window
   */

  private static final Logger log4j = Logger.getLogger(ImportBudgetAdjustmentDAO.class);
  private Connection conn = null;

  public ImportBudgetAdjustmentDAO(Connection con) {
    this.conn = con;
  }

  /**
   * 
   * @param file
   * @param vars
   * @param inpBudgetRevId
   * @return success once file processed
   */
  public JSONObject processUploadedCsvFile(File file, VariablesSecureApp vars,
      String inpBudgetAdjID) {

    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;
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
              /*
               * List<String> result = row; if (result == null) continue; String fields[] =
               * (String[]) result.toArray(new String[0]);
               */
              BudgetAdjustment budgetadj = OBDal.getInstance().get(BudgetAdjustment.class,
                  inpBudgetAdjID);
              isSuccess = insertIntoBudgetAdjustment(budgetadj, row);

            }
          }
        }

      }

      if (isSuccess == 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", "Record Insert Failed");
      } else {
        jsonresult = new JSONObject();
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", "Records Inserted Succesfully");
      }
    } catch (Exception e) {
      isSuccess = 0;
      log4j.error("Exception in processUploadedCsvFile : " + e);
      jsonresult = new JSONObject();
      try {
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", e.getMessage());
      } catch (JSONException e1) {
        log4j.error("Exception in JSONException : " + e);
      }
      log4j.error("Exception in Import Budget adjustment line Data Import", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonresult;

  }

  /**
   * 
   * @param file
   * @param vars
   * @param inpBudgetRevId
   * @return success valid xlsx file
   */
  @SuppressWarnings({ "unused", "null" })
  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars,
      String inpBudgetAdjID) {
    JSONObject resultJSON = new JSONObject();
    JSONArray ListArray = new JSONArray();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;
    Row row = null;
    Cell cell = null;
    Cell cell1 = null;
    Cell cell2 = null;
    Cell cell3 = null;
    Cell cell4 = null;
    Cell cell5 = null;
    boolean value = true;
    int failed = 0;
    int lineNo = 1, duplicateline = 1;
    StringBuffer description = new StringBuffer();
    String br = "";
    String budgetDefId = "", campaign = "", uniqueCode = "", validcombinationId = "", sql = "",
        campaignName = "", budgetDefName = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    JSONObject object = null;
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    Map<String, List<String>> map1 = new HashMap<String, List<String>>();
    Map<String, List<String>> map2 = new HashMap<String, List<String>>();
    Map<String, List<String>> map3 = new HashMap<String, List<String>>();
    Map<String, List<String>> map4 = new HashMap<String, List<String>>();
    Map<String, List<String>> map5 = new HashMap<String, List<String>>();
    Map<String, List<String>> map6 = new HashMap<String, List<String>>();
    Map<String, List<String>> map7 = new HashMap<String, List<String>>();
    Map<String, List<String>> isActivemap8 = new HashMap<String, List<String>>();
    try {

      BudgetAdjustment budgetadj = OBDal.getInstance().get(BudgetAdjustment.class, inpBudgetAdjID);
      budgetDefId = budgetadj.getEfinBudgetint().getId();
      campaign = budgetadj.getBudgetType().getId();
      campaignName = budgetadj.getBudgetType().getName();
      budgetDefName = budgetadj.getEfinBudgetint().getCommercialName();
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
      HashSet<String> lines = new HashSet<String>();
      HashSet<String> lines1 = new HashSet<String>();
      HashSet<String> increaseAmt = new HashSet<String>();
      HashSet<String> decreaseAmt = new HashSet<String>();
      HashSet<String> legacybudvalue = new HashSet<String>();
      HashSet<Integer> rowNo2 = new HashSet<Integer>();
      HashSet<Integer> rowNo4 = new HashSet<Integer>();
      HashSet<Integer> rowNo5 = new HashSet<Integer>();
      HashSet<Integer> rowNo6 = new HashSet<Integer>();
      boolean successFlag = true;
      String lineno = "";
      String uniquecodeName = null;
      List<Integer> values = new ArrayList<Integer>();
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        JSONObject errorList = new JSONObject(), errorJSON = null;
        JSONArray errorListArray = new JSONArray();
        JSONObject uploadDataJSON = null;
        boolean validData = true;
        String lang = vars.getLanguage();
        LinkedHashMap<String, JSONObject> uploadData = new LinkedHashMap<String, JSONObject>();
        Pattern regexTime = Pattern.compile("^\\d{1,2}:\\d{2}$");
        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          sheet = xssfWorkbook.getSheetAt(s);
          if (sheet == null)
            break;
          int startRow = 1;
          String LineId = "", Uniquecode = "", stramount = "";
          boolean Negativeflag = true;

          for (int i = startRow;; i++) {
            lineNo += 1;
            row = sheet.getRow(i);
            if (row == null)
              break;

            // int noOfColumns = row.getLastCellNum();

            description.append(br);
            if (row.getCell(0) == null || row.getCell(0).toString().equals("")) {
              failed += 1;
              description.append("UniqueCode at line No.<b>" + lineNo + "</b> is mandatory.<br>");
              successFlag = false;
            }
            if (row.getCell(0) != null && StringUtils.isNotEmpty((row.getCell(0)).toString())) {
              cell = row.getCell(0);
              uniqueCode = getCellValue(cell);

              if (lines.add(uniqueCode)) {
                List<String> list = new ArrayList<String>();
                list.add(String.valueOf(lineNo));
                map.put(uniqueCode, list);

              } else {
                List<String> list = map.get(uniqueCode);
                if (list == null) {
                  list = new ArrayList<String>();
                  list.add(String.valueOf(lineNo));
                  map.put(uniqueCode, list);
                }
                list.add(String.valueOf(lineNo));
                successFlag = false;
              }
              OBQuery<AccountingCombination> validcombination = OBDal.getInstance()
                  .createQuery(AccountingCombination.class, "efinUniqueCode='" + uniqueCode + "'");
              validcombination.setFilterOnActive(false);

              if (validcombination != null && validcombination.list().size() > 0) {
                validcombinationId = validcombination.list().get(0).getId();
                uniquecodeName = validcombination.list().get(0).getEfinUniquecodename();
              } else {
                validcombinationId = null;
              }
              log4j.debug("validcombinationId" + validcombinationId);
              if (validcombinationId != null) {

                if (!validcombination.list().get(0).isActive()) {
                  if (isActivemap8.get(uniqueCode) != null) {
                    List<String> list = isActivemap8.get(uniqueCode);
                    list.add(String.valueOf(lineNo));
                  } else {
                    List<String> list = new ArrayList<String>();
                    list.add(String.valueOf(lineNo));
                    isActivemap8.put(uniqueCode, list);
                  }
                } else {
                  ps = conn.prepareStatement(
                      "select count(efin_budgetinquiry_id) as count from efin_budgetinquiry where c_campaign_id = '"
                          + campaign + "' \n" + "and  efin_budgetint_id = '" + budgetDefId
                          + "' and c_validcombination_id ='" + validcombinationId + "'\n"
                          + "and isbudget = 'Y'");
                  rs = ps.executeQuery();
                  if (rs.next()) {
                    if (rs.getInt("count") == 0) {
                      if (map2.get(uniqueCode) != null) {
                        List<String> list = map2.get(uniqueCode);
                        list.add(String.valueOf(lineNo));
                      } else {
                        List<String> list = new ArrayList<String>();
                        list.add(String.valueOf(lineNo));
                        map2.put(uniqueCode, list);
                      }
                      successFlag = false;
                    }

                  }
                  OBQuery<BudgetAdjustmentLine> budgrevchk = OBDal.getInstance().createQuery(
                      BudgetAdjustmentLine.class,
                      " efinBudgetadj.id ='" + inpBudgetAdjID + "' and client.id = '"
                          + budgetadj.getClient().getId() + "' and accountingCombination.id='"
                          + validcombinationId + "'");
                  if (budgrevchk.list().size() > 0) {
                    failed += 1;
                    description.append("UniqueCode  <b>" + row.getCell(0) + "</b> at line No.<b>"
                        + lineNo + "</b> already exists in Budget adjustment Lines.<br>");
                    successFlag = false;
                  }
                }
              }

              else {
                if (map1.get(uniqueCode) != null) {
                  List<String> list = map1.get(uniqueCode);
                  list.add(String.valueOf(lineNo));
                } else {
                  List<String> list = new ArrayList<String>();
                  list.add(String.valueOf(lineNo));
                  map1.put(uniqueCode, list);
                }
                successFlag = false;
              }

            }
            // if (successFlag) {
            // if (row.getCell(1) != null) {
            // cell4 = row.getCell(1);
            // String dimensionName = getCellValue(cell4);
            // if (dimensionName.equals(uniquecodeName)) {
            //
            // } else {
            // if (map6.get(uniqueCode) != null) {
            // List<String> list = map6.get(uniqueCode);
            // list.add(String.valueOf(lineNo));
            // } else {
            // List<String> list = new ArrayList<String>();
            // list.add(String.valueOf(lineNo));
            // map6.put(uniqueCode, list);
            // }
            // }
            // }
            // }
            if (row.getCell(2) == null) {
              increaseAmt.add("IncreaseAmount");
              rowNo4.add(lineNo);
            }
            if (row.getCell(2) != null) {
              cell1 = row.getCell(2);
              Negativeflag = chkisnegative(getCellValue(cell1));
              if (!Negativeflag) {
                if (map3.get(uniqueCode) != null) {
                  List<String> list = map3.get(uniqueCode);
                  list.add(String.valueOf(lineNo));
                } else {
                  List<String> list = new ArrayList<String>();
                  list.add(String.valueOf(lineNo));
                  map3.put(uniqueCode, list);
                }

              }
            }
            if (row.getCell(3) == null) {
              decreaseAmt.add("decreaseAmount");
              rowNo5.add(lineNo);

            }
            if (row.getCell(3) != null) {
              cell2 = row.getCell(3);
              Negativeflag = chkisnegative(getCellValue(cell2));
              if (!Negativeflag) {
                if (map4.get(uniqueCode) != null) {
                  List<String> list = map4.get(uniqueCode);
                  list.add(String.valueOf(lineNo));
                } else {
                  List<String> list = new ArrayList<String>();
                  list.add(String.valueOf(lineNo));
                  map4.put(uniqueCode, list);
                }

              }

            }
            if (row.getCell(4) == null) {
              legacybudvalue.add("legacyhisbudgetvalue");
              rowNo6.add(lineNo);

            }
            if (row.getCell(4) != null) {
              cell5 = row.getCell(4);
              Negativeflag = chkisnegative(getCellValue(cell5));
              if (!Negativeflag) {
                if (map7.get(uniqueCode) != null) {
                  List<String> list = map7.get(uniqueCode);
                  list.add(String.valueOf(lineNo));
                } else {
                  List<String> list = new ArrayList<String>();
                  list.add(String.valueOf(lineNo));
                  map7.put(uniqueCode, list);
                }

              }

            }

            if (row.getCell(2) != null && row.getCell(3) != null) {
              if (new BigDecimal(getCellValue(cell1)).compareTo(new BigDecimal(0)) > 0
                  && new BigDecimal(getCellValue(cell2)).compareTo(new BigDecimal(0)) > 0) {
                if (map5.get(uniqueCode) != null) {
                  List<String> list = map5.get(uniqueCode);
                  list.add(String.valueOf(lineNo));
                } else {
                  List<String> list = new ArrayList<String>();
                  list.add(String.valueOf(lineNo));
                  map5.put(uniqueCode, list);
                }

              }
            }

          }

          for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            lineno = null;
            String key = entry.getKey();
            List<String> value1 = entry.getValue();
            if (value1.size() > 1) {
              for (String s1 : value1) {
                if (lineno != null)
                  lineno = lineno + "," + s1;
                else
                  lineno = s1;
              }
              description.append(br);
              failed += 1;
              description.append("UniqueCode <b>" + key + " is duplicate in lineNo </b>" + lineno
                  + " in the data file.<br>");
            }
          }
          for (Map.Entry<String, List<String>> entry : map1.entrySet()) {
            description.append(br);
            failed += 1;
            description.append("UniqueCode  <b>" + entry.getKey() + "</b> at line No.<b>"
                + entry.getValue() + "</b> not present in Account Dimension.<br>");

          }

          for (Map.Entry<String, List<String>> entry : isActivemap8.entrySet()) {
            description.append(br);
            failed += 1;
            description.append(Resource.getProperty("finance.uniquecode", lang) + " <b> "
                + entry.getKey() + " </b> " + Resource.getProperty("finance.at.lineno", lang)
                + " <b> " + entry.getValue() + "</b> "
                + Resource.getProperty("finance.not.active", lang) + " <br>");

          }

          for (Map.Entry<String, List<String>> entry : map2.entrySet()) {

            description.append(br);
            failed += 1;
            description
                .append("UniqueCode <b>" + entry.getKey() + "</b> at line No.<b>" + entry.getValue()
                    + "<b> not belongs to related Budget Type/Budget Definition.</br> ");
          }
          // for (Map.Entry<String, List<String>> entry : map6.entrySet()) {
          //
          // description.append(br);
          // failed += 1;
          // description.append("UniqueCodeName at line No.<b>" + entry.getValue()
          // + "</b> Should not less be related to this uniquecode </b> " + entry.getKey()
          // + "<br>");
          // }

          for (Map.Entry<String, List<String>> entry : map3.entrySet()) {

            description.append(br);
            failed += 1;
            description.append("Increase Amount <b>" + entry.getKey() + "</b> at line No.<b>"
                + entry.getValue() + "</b> Should not less than zero<br>");
          }
          for (String s1 : increaseAmt) {
            description.append(br);
            failed += 1;
            description
                .append("Increase amount at line No.<b>" + rowNo4 + "</b> is mandatory.<br>");

          }

          for (String s1 : decreaseAmt) {
            failed += 1;
            description
                .append("Decrease amount at line No.<b>" + rowNo5 + "</b> is mandatory.<br>");
          }
          for (Map.Entry<String, List<String>> entry : map4.entrySet()) {

            description.append(br);
            failed += 1;
            description.append("Decrease Amount <b>" + entry.getKey() + "</b> at line No.<b>"
                + entry.getValue() + "</b> should not less than zero<br>");
          }
          for (Map.Entry<String, List<String>> entry : map5.entrySet()) {

            description.append(br);
            failed += 1;
            description.append("Either increase or decrese value is allowed for this uniquecode <b>"
                + entry.getKey() + "</b> at line No.<b>" + entry.getValue() + "</br>");
          }
          for (Map.Entry<String, List<String>> entry : map7.entrySet()) {

            description.append(br);
            failed += 1;
            description.append("Legacy Historical Budget Value <b>" + entry.getKey()
                + "</b> at line No.<b>" + entry.getValue() + "</b> should not less than zero<br>");
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
    } catch (final Exception e) {
      log4j.error("Exception in while validating adjustment lines sheet() : ", e);
      try {

      } catch (Exception ee) {
        log4j.error("Exception in while validating adjustment lines sheet()  : ", ee);
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
   * Parsing .xlsx file using regular Expression
   */

  private List<String> parseExcel(Row row) {
    final List<String> strings = new ArrayList<String>();
    int i = 0;
    for (Cell mycell : row) {
      strings.add(getCellValue(mycell));
      i++;
      if (i == 13)
        break;
    }

    return strings;
  }

  /**
   * This method is used to insert into budget adjustment
   * 
   * @param budgetadj
   * @param row
   * @return
   */
  private int insertIntoBudgetAdjustment(BudgetAdjustment budgetadj, Row row) {

    int length;
    BigDecimal amount = BigDecimal.ZERO;
    String validcombinationId = "", budgetInt = "", uniqueCodeName = "";
    BigDecimal OriginalAmount = BigDecimal.ZERO;
    BigDecimal CurrentBudget = BigDecimal.ZERO;

    try {
      OBQuery<AccountingCombination> validcombination = OBDal.getInstance()
          .createQuery(AccountingCombination.class, "efinUniqueCode='" + row.getCell(0) + "'");

      if (validcombination != null && validcombination.list().size() > 0) {
        validcombinationId = validcombination.list().get(0).getId();

      }
      budgetInt = budgetadj.getEfinBudgetint().getId();

      AccountingCombination objActCombination = OBDal.getInstance().get(AccountingCombination.class,
          validcombinationId);

      OBQuery<EfinBudgetInquiry> budgetInq = OBDal.getInstance().createQuery(
          EfinBudgetInquiry.class,
          "accountingCombination.id= :accountingCombinationID and efinBudgetint.id = :efinBudgetintID");
      budgetInq.setNamedParameter("accountingCombinationID", validcombinationId);
      budgetInq.setNamedParameter("efinBudgetintID", budgetInt);
      List<EfinBudgetInquiry> budgetInqList = budgetInq.list();
      if (budgetInq != null && budgetInqList.size() > 0) {
        OriginalAmount = budgetInqList.get(0).getREVAmount();
        CurrentBudget = budgetInqList.get(0).getCurrentBudget();
        uniqueCodeName = budgetInqList.get(0).getUniqueCodeName();
      }
      log4j.debug("OriginalAmount" + OriginalAmount);
      log4j.debug("CurrentBudget" + CurrentBudget);

      BudgetAdjustmentLine poline = OBProvider.getInstance().get(BudgetAdjustmentLine.class);
      poline.setClient(budgetadj.getClient());
      poline.setOrganization(budgetadj.getOrganization());
      poline.setCreationDate(new java.util.Date());
      poline.setCreatedBy(budgetadj.getCreatedBy());
      poline.setUpdated(new java.util.Date());
      poline.setUpdatedBy(budgetadj.getUpdatedBy());
      poline.setActive(true);
      poline.setEfinBudgetadj(budgetadj);
      poline.setAccountingCombination(objActCombination);
      poline.setAccount(objActCombination.getAccount());
      poline.setBusinessPartner(objActCombination.getBusinessPartner());
      poline.setSalesCampaign(objActCombination.getSalesCampaign());
      poline.setSubAccount(objActCombination.getProject());
      poline.setFunctionalClassfication(objActCombination.getActivity());
      poline.setFuture1(objActCombination.getStDimension());
      poline.setNdDimension(objActCombination.getNdDimension());
      poline.setDepartment(objActCombination.getSalesRegion());
      poline.setOrgid(objActCombination.getOrganization());
      poline.setOriginalBudget(OriginalAmount);
      poline.setBudgetInquiryLine(PurchaseInvoiceSubmitUtils.getBudgetInquiry(objActCombination,
          budgetadj.getEfinBudgetint()));
      poline.setCostcurbudget(
          AdjustmentAddLineHandlerDao.getCostCurrentBudget(objActCombination, budgetInt));

      if (row.getCell(1) != null) {
        poline.setUniqueCodeName(row.getCell(1).toString());
      } else {
        poline.setUniqueCodeName(uniqueCodeName);
      }
      if (new BigDecimal(getCellValue(row.getCell(2))).compareTo(new BigDecimal(0)) > 0) {
        length = getCellValue(row.getCell(2)).toString().length();
        if (length > 10) {
          amount = new BigDecimal(getCellValue(row.getCell(2)).replace(",", ""));// .substring(0,
                                                                                 // 10)
          poline.setIncrease(amount);
        } else {
          poline.setIncrease(new BigDecimal(getCellValue(row.getCell(2))));
        }
        poline.setCurrentBudget(CurrentBudget.add(new BigDecimal(getCellValue(row.getCell(2)))));
        poline
            .setORGBudgetRevised(OriginalAmount.add(new BigDecimal(getCellValue(row.getCell(2)))));
      }

      if (new BigDecimal(getCellValue(row.getCell(3))).compareTo(new BigDecimal(0)) > 0) {
        length = getCellValue(row.getCell(3)).toString().length();
        if (length > 10) {
          amount = new BigDecimal(getCellValue(row.getCell(3)).replace(",", ""));// .substring(0,
                                                                                 // 10)
          poline.setDecrease(amount);
        } else {
          poline.setDecrease(new BigDecimal(getCellValue(row.getCell(3))));
        }
        poline
            .setCurrentBudget(CurrentBudget.subtract(new BigDecimal(getCellValue(row.getCell(3)))));
        poline.setORGBudgetRevised(
            OriginalAmount.subtract(new BigDecimal(getCellValue(row.getCell(3)))));
      }
      if (row.getCell(4) != null) {
        poline.setLegacyHistoricalBudgetValue(new BigDecimal(getCellValue(row.getCell(4))));

      }

      /*
       * if (poline.getIncrease().compareTo(new BigDecimal(0)) > 0) { String distOrgId =
       * budgetadj.getDistributionLinkOrg() == null ? "" :
       * budgetadj.getDistributionLinkOrg().getId(); // fetch distribution org String query =
       * "select ad_org_id from ad_org where ad_org_id in (select ad_org_id from c_validcombination "
       * + "where c_salesregion_id = '" + strCostCentre + "' and account_id ='" + accountId + "') "
       * + " and ad_org_id ='" + distOrgId + "'"; SQLQuery distOrgQuery =
       * OBDal.getInstance().getSession().createSQLQuery(query); List distOrgList =
       * distOrgQuery.list(); if (distOrgQuery != null && distOrgList.size() > 0) { Object
       * objDistOrg = distOrgList.get(0); if (objDistOrg != null) { strDistOrg =
       * objDistOrg.toString(); } } if (strDistOrg != null) {
       * 
       * poline.setDistribute(true);
       * poline.setDislinkorg(OBDal.getInstance().get(Organization.class, strDistOrg));
       * 
       * } }
       */
      OBDal.getInstance().save(poline);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      log4j.error("Exception in insertIntoBudgetAdjustment : " + e);
      return 0;
    }
    return 1;

  }

  /**
   * This method is used to check negative
   * 
   * @param cell
   * @return
   */
  public static boolean chkisnegative(String cell) {
    try {
      if (new BigDecimal(cell).compareTo(new BigDecimal(0)) < 0) {
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