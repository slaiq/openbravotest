package sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.project.Project;

import sa.elm.ob.finance.ad_reports.IntegratedCostBudgetInquiry.vo.IntegratedCostBudgetInquiryVO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Priyanka Ranjan on 22/04/2019
 * 
 */
// Implementation file for Integrated Cost Budget Inquiry Report

public class IntegratedCostBudgetInquiryDAOImpl implements IntegratedCostBudgetInquiryDAO {
  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(IntegratedCostBudgetInquiryDAOImpl.class);

  public IntegratedCostBudgetInquiryDAOImpl(Connection connection) {
    this.conn = connection;
  }

  public Connection getConnection() {
    return conn;
  }

  @SuppressWarnings("resource")
  @Override
  public JSONObject getOrganization(String OrgId, String ClientId, String searchTerm, int pagelimit,
      int page) throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();

      countQuery.append(" select count(O.ad_org_id) as count ");
      selectQuery.append(" select O.ad_org_id as id, (O.value||'-'|| O.Name) as value  ");

      fromQuery
          .append(" FROM ad_org O JOIN AD_ORGTYPE OT ON O.AD_ORGTYPE_ID=OT.AD_ORGTYPE_ID where "
              + " O.AD_Client_ID='" + ClientId
              + "' AND O.isActive = 'Y' AND O.isready = 'Y' AND OT.ISTRANSACTIONSALLOWED='Y'  ");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(" and (O.value||'-'|| O.Name) ilike '%" + searchTerm.toLowerCase() + "%'");

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());

      log4j.debug("query:" + st.toString());

      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);
      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");
        st.setInt(1, pagelimit);
        st.setInt(2, (page - 1) * pagelimit);

        log4j.debug("orglist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("id"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }

      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getOrganization :", e);
      return jsob;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {

      }
    }
    return jsob;
  }

  @Override
  public List<IntegratedCostBudgetInquiryVO> getAcctSchema(String OrgId, String ClientId,
      String schemaId) throws Exception {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<IntegratedCostBudgetInquiryVO> list = new ArrayList<IntegratedCostBudgetInquiryVO>();
    try {

      sqlQuery = "  SELECT C_ACCTSCHEMA_ID as id, ((CASE C_ACCTSCHEMA.isActive WHEN 'N' THEN '**' ELSE '' END) || C_ACCTSCHEMA.Name) as name FROM C_ACCTSCHEMA       WHERE C_ACCTSCHEMA.AD_Org_ID in ("
          + OrgId + ") AND C_ACCTSCHEMA.AD_Client_ID IN(" + ClientId
          + ")   AND (C_ACCTSCHEMA.isActive = 'Y'"
          + " OR C_ACCTSCHEMA.C_ACCTSCHEMA_ID = ? )     ORDER BY name ";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, schemaId);
      rs = st.executeQuery();
      while (rs.next()) {
        IntegratedCostBudgetInquiryVO VO = new IntegratedCostBudgetInquiryVO();
        VO.setAcctschemaId(rs.getString(1));
        VO.setAcctschemaName(rs.getString(2));
        list.add(VO);
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    } finally {
      // close connection
      try {
        if (st != null) {
          st.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection  :" + e);
      }

    }
    return list;
  }

  @Override
  public List<IntegratedCostBudgetInquiryVO> getBudgetType(String OrgId, String ClientId,
      String RoleId) throws Exception {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<IntegratedCostBudgetInquiryVO> list = null;
    try {
      list = new ArrayList<IntegratedCostBudgetInquiryVO>();
      sqlQuery = "  SELECT C_CAMPAIGN_ID as id, ((CASE C_CAMPAIGN.isActive WHEN 'N' THEN '**' ELSE '' END) || (C_CAMPAIGN.Value||'-'||C_CAMPAIGN.Name)) as name FROM C_CAMPAIGN  "
          + "      WHERE C_CAMPAIGN.AD_Org_ID in (" + OrgId + ") AND C_CAMPAIGN.AD_Client_ID IN("
          + ClientId
          + ")    AND (C_CAMPAIGN.isActive = 'Y') AND C_CAMPAIGN.C_CAMPAIGN_ID IN(select bud.C_campaign_ID from efin_security_rules_budtype bud join efin_security_rules ru on ru.efin_security_rules_id=bud.efin_security_rules_id where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y')     ORDER BY name ";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        IntegratedCostBudgetInquiryVO VO = new IntegratedCostBudgetInquiryVO();
        VO.setBudgetTypeId(rs.getString(1));
        VO.setBudgetTypeName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    } finally {
      // close connection
      try {
        if (st != null) {
          st.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection  :" + e);
      }
    }
    return list;
  }

  @Override
  public JSONObject selectLines(VariablesSecureApp vars, String BudgetTypeId, String ClientId,
      String OrgId, String parentAccountId, String deptId, String subAccountId) throws Exception {

    String sqlQuery = "", tempUniqCode = "";

    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject obj = null;
    JSONObject result = new JSONObject();
    JSONArray array = new JSONArray();
    JSONArray arr = null;
    String listofuniquecode = null;
    String budgetTypeValue = null;

    try {
      if (BudgetTypeId != null) {
        Campaign budtypeObj = OBDal.getInstance().get(Campaign.class, BudgetTypeId);
        budgetTypeValue = budtypeObj.getEfinBudgettype();
      }

      sqlQuery = "select inqhd.c_year_id as yearid, yr.year as yearname,inqln.carryforward as previousyearbudget, acctdim.em_efin_uniquecode as uniquecode,inqln.c_validcombination_id as uniquecodeid,inqln.c_campaign_id as budgettypeid,inqln.org_amt as oriamt,inqln.revinc_amt as revincamt,inqln.revdec_amt as recdecamt,"
          + " inqln.disinc_amt as disincamt,inqln.obinc_amt as budget_adj_increase,inqln.obdec_amt as budget_adj_decrease,inqln.disdec_amt as disdecamt,inqln.current_budget as currentbudget,inqln.encumbrance as encum,inqln.funds_available as fundavail, inqln.Spent_Amt as costactual, "
          + " fundsuniq.em_efin_uniquecode as funduniquecode,funds.c_validcombination_id as funduniquecodeid,funds.org_amt as fundoriamt,funds.revinc_amt as fundrevincamt,funds.revdec_amt as fundrevdecamt, "
          + " funds.obinc_amt as funds_adj_increase,funds.obdec_amt as funds_adj_decrease,funds.disinc_amt as funddisincamt,funds.disdec_amt as funddicdecamt,funds.current_budget as fundcurrentbudget,funds.encumbrance as fundencum,funds.funds_available as fundfundsavail,funds.Spent_Amt as fundactual "
          + " from efin_budgetinquiry inqln "
          + " join efin_budgetint inqhd on inqhd.efin_budgetint_id = inqln.efin_budgetint_id "
          + " join c_year yr on inqhd.c_year_id = yr.c_year_id "
          + " join c_validcombination acctdim on acctdim.c_validcombination_id = inqln.c_validcombination_id "
          + " join c_campaign budtype on budtype.c_campaign_id = inqln.c_campaign_id "
          + " left join efin_budgetinquiry funds on  funds.c_validcombination_id = acctdim.EM_Efin_Fundscombination and funds.efin_budgetint_id = inqhd.efin_budgetint_id "
          + " left join c_validcombination fundsuniq on  funds.c_validcombination_id = fundsuniq.c_validcombination_id "
          + " where budtype.em_efin_budgettype = '" + budgetTypeValue + "' ";

      // check Account is parent account
      ElementValue parent_acct_obj = OBDal.getInstance().get(ElementValue.class, parentAccountId);

      if (parentAccountId != null && parent_acct_obj != null && !parentAccountId.equals("0")
          && parent_acct_obj.isSummaryLevel()) {
        sqlQuery += " and inqln.c_elementvalue_id in (select replace(unnest(string_to_array(eut_getchildacct('"
            + parentAccountId + "'),',')::character varying []),'''',''))";

      }
      if (parentAccountId != null && parent_acct_obj != null && !parent_acct_obj.isSummaryLevel()) {
        sqlQuery += " and inqln.c_elementvalue_id ='" + parentAccountId + "'";
      }

      if (deptId != null && !deptId.equals("0")) {
        sqlQuery += " and inqln.c_salesregion_id = '" + deptId + "' ";
      }
      if (subAccountId != null && !subAccountId.equals("0")) {
        sqlQuery += " and inqln.c_project_id = '" + subAccountId + "'  ";
      }
      sqlQuery += "and acctdim.ad_org_id = ? and inqhd.ad_client_id = ?  order by inqln.uniquecode,inqhd.c_year_id ";

      st = conn.prepareStatement(sqlQuery);
      st.setString(1, OrgId);
      st.setString(2, ClientId);
      log4j.debug("IntegratedCostBudgetInquiry:" + st.toString());
      rs = st.executeQuery();

      while (rs.next()) {
        // Group UniqueCode Wise Transaction
        // if same uniquecode then Group the transaction under the uniquecode
        if (tempUniqCode.equals(rs.getString("uniquecode"))) {
          arr = obj.getJSONArray("transaction");
          JSONObject tra = new JSONObject();
          tra.put("year", rs.getString("yearname"));
          tra.put("previousyearbudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("previousyearbudget")));
          tra.put("costOriginalBudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("oriamt")));
          tra.put("costBudgetAdjustIncrease", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("budget_adj_increase")));
          tra.put("costBudgetAdjustDecrease", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("budget_adj_decrease")));
          tra.put("costBudgetRevInc", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("revincamt")));
          tra.put("costBudgetRevDec", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("recdecamt")));
          tra.put("costBudgetDisInc", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("disincamt")));
          tra.put("costBudgetDicDec", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("disdecamt")));
          tra.put("costCurrentBudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("currentbudget")));
          tra.put("costEncum", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("encum")));
          tra.put("costFundsAvail", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundavail")));
          tra.put("costactual", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("costactual")));
          tra.put("fundOriginalBudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundoriamt")));
          tra.put("fundAdjustIncrease", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("funds_adj_increase")));
          tra.put("fundAdjustDecrease", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("funds_adj_Decrease")));
          tra.put("fundBudgetRevInc", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundrevincamt")));
          tra.put("fundBudgetRevDec", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundrevdecamt")));
          tra.put("fundBudgetDisInc", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("funddisincamt")));
          tra.put("fundBudgetDicDec", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("funddicdecamt")));
          tra.put("fundCurrentBudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundcurrentbudget")));
          tra.put("fundEncum", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("fundencum")));
          tra.put("fundactual", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("fundactual")));
          tra.put("fundFundsAvail", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundfundsavail")));
          arr.put(tra);
        }
        // if different uniquecode then form new Uniquecode JsonObject
        else {
          if (listofuniquecode == null)
            listofuniquecode = "'" + rs.getString("uniquecode") + "'";
          else
            listofuniquecode += ",'" + rs.getString("uniquecode") + "'";
          obj = new JSONObject();
          obj.put("uniquecode",
              (rs.getString("uniquecode") == null ? "" : rs.getString("uniquecode")));
          obj.put("uniquecodeid",
              (rs.getString("uniquecodeid") == null ? "" : rs.getString("uniquecodeid")));
          obj.put("funduniquecode",
              (rs.getString("funduniquecode") == null ? "" : rs.getString("funduniquecode")));
          obj.put("funduniquecodeid",
              (rs.getString("funduniquecodeid") == null ? "" : rs.getString("funduniquecodeid")));
          arr = new JSONArray();
          JSONObject tra = new JSONObject();
          tra.put("year", rs.getString("yearname"));

          // tra.put("initialDr",
          // Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
          tra.put("year", rs.getString("yearname"));
          tra.put("previousyearbudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("previousyearbudget")));
          tra.put("costOriginalBudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("oriamt")));
          tra.put("costBudgetAdjustIncrease", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("budget_adj_increase")));
          tra.put("costBudgetAdjustDecrease", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("budget_adj_decrease")));
          tra.put("costBudgetRevInc", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("revincamt")));
          tra.put("costBudgetRevDec", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("recdecamt")));
          tra.put("costBudgetDisInc", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("disincamt")));
          tra.put("costBudgetDicDec", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("disdecamt")));
          tra.put("costCurrentBudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("currentbudget")));
          tra.put("costEncum", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("encum")));
          tra.put("costFundsAvail", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundavail")));
          tra.put("costactual", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("costactual")));
          tra.put("fundOriginalBudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundoriamt")));
          tra.put("fundAdjustIncrease", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("funds_adj_increase")));
          tra.put("fundAdjustDecrease", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("funds_adj_Decrease")));
          tra.put("fundBudgetRevInc", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundrevincamt")));
          tra.put("fundBudgetRevDec", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundrevdecamt")));
          tra.put("fundBudgetDisInc", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("funddisincamt")));
          tra.put("fundBudgetDicDec", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("funddicdecamt")));
          tra.put("fundCurrentBudget", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundcurrentbudget")));
          tra.put("fundEncum", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("fundencum")));
          tra.put("fundactual", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("fundactual")));
          tra.put("fundFundsAvail", Utility.getNumberFormat(vars,
              Utility.numberFormat_PriceRelation, rs.getBigDecimal("fundfundsavail")));

          arr.put(tra);
          obj.put("transaction", arr);
          array.put(obj);
          tempUniqCode = rs.getString("uniquecode");
        }

        result.put("list", array);
      }
      log4j.debug("has lsit:" + result.has("list"));

    } catch (Exception e) {
      log4j.error("Exception selectLines", e);
    }
    return result;

  }

  @Override
  public List<IntegratedCostBudgetInquiryVO> getClientInfo(String ClientId) throws Exception {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<IntegratedCostBudgetInquiryVO> list = null;
    try {
      list = new ArrayList<IntegratedCostBudgetInquiryVO>();

      sqlQuery = " select ad_client_id as id ,(value||'-'||name) as name from ad_client where ad_client_id=? ";

      st = conn.prepareStatement(sqlQuery);
      st.setString(1, ClientId);
      log4j.debug("query:" + st.toString());

      rs = st.executeQuery();
      while (rs.next()) {
        IntegratedCostBudgetInquiryVO VO = new IntegratedCostBudgetInquiryVO();
        VO.setclientId(rs.getString("id"));
        VO.setclientName(rs.getString("name"));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getClientInfo()", e);
    } finally {
      // close connection
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {
        log4j.error("Exception in closing connection  : " + e);
      }
    }
    return list;
  }

  @SuppressWarnings("resource")
  @Override
  public JSONObject getParentAccount(String clientId, String searchTerm, int pagelimit, int page)
      throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();
      countQuery.append(" select count(acct.c_elementvalue_id) as count ");
      selectQuery.append(
          " select acct.c_elementvalue_id as id, (acct.value ||'-'|| acct.name) as value  ");
      fromQuery.append(" from c_elementvalue acct where  "
          // + "acct.issummary='Y' and"
          + " acct.isactive='Y' and acct.ad_client_id=?  ");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(
            " and (acct.value ||'-'|| acct.name) ilike '%" + searchTerm.toLowerCase() + "%'");
      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      st.setString(1, clientId);
      log4j.debug("query:" + st.toString());

      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);
      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");
        st.setString(1, clientId);
        st.setInt(2, pagelimit);
        st.setInt(3, (page - 1) * pagelimit);

        log4j.debug("accountlist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("id"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }

      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getParentAccount :", e);
      return jsob;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {

      }
    }
    return jsob;
  }

  @SuppressWarnings("resource")
  @Override
  public JSONObject getDepartment(String clientId, String searchTerm, int pagelimit, int page)
      throws Exception {
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();
      countQuery.append(" select count(dept.c_salesregion_id) as count ");
      selectQuery
          .append(" select dept.c_salesregion_id as id, (dept.value ||'-'|| dept.name) as value  ");

      fromQuery.append(" from  c_salesregion dept where  ad_client_id=?  ");

      if (searchTerm != null && !searchTerm.equals(""))
        fromQuery.append(
            " and (dept.value ||'-'|| dept.name)  ilike '%" + searchTerm.toLowerCase() + "%'");
      st = conn.prepareStatement(countQuery.append(fromQuery).toString());
      // st = conn.prepareStatement(sqlQuery);
      st.setString(1, clientId);
      log4j.debug("query:" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);
      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");
        st.setString(1, clientId);
        st.setInt(2, pagelimit);
        st.setInt(3, (page - 1) * pagelimit);

        log4j.debug("deptlist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("id"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }

      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getDepartment :", e);
      return jsob;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {

      }
    }
    return jsob;
  }

  @SuppressWarnings("resource")
  @Override
  public JSONObject getSubAccount(String clientId, String searchTerm, int pagelimit, int page,
      String accountId) throws Exception {
    // TODO Auto-generated method stub
    PreparedStatement st = null;
    ResultSet rs = null;
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    int totalRecords = 0;
    try {
      jsob = new JSONObject();
      StringBuilder countQuery = new StringBuilder(), selectQuery = new StringBuilder(),
          fromQuery = new StringBuilder();
      countQuery.append(" select count(proj.c_project_id) as count ");
      selectQuery
          .append(" select proj.c_project_id as id, (proj.value ||'-'|| proj.name) as value  ");

      fromQuery.append(" from c_project proj where ad_client_id=?  ");
      if (searchTerm != null && !searchTerm.equals("")) {
        fromQuery.append(
            " and (proj.value ||'-'|| proj.name)  ilike '%" + searchTerm.toLowerCase() + "%'");
      }

      if (accountId != null && !accountId.equals("")) {
        fromQuery.append(
            " and c_project_id in  (select em_efin_project_id from c_elementvalue where c_elementvalue_id='"
                + accountId + "' )");
      }

      st = conn.prepareStatement(countQuery.append(fromQuery).toString());

      st.setString(1, clientId);
      log4j.debug("query:" + st.toString());
      rs = st.executeQuery();
      if (rs.next())
        totalRecords = rs.getInt("count");
      jsob.put("totalRecords", totalRecords);
      if (totalRecords > 0) {
        st = conn.prepareStatement(
            (selectQuery.append(fromQuery)).toString() + " order by value limit ? offset ? ");
        st.setString(1, clientId);
        st.setInt(2, pagelimit);
        st.setInt(3, (page - 1) * pagelimit);

        log4j.debug("subaccountlist:" + st.toString());
        rs = st.executeQuery();

        while (rs.next()) {
          JSONObject jsonData = new JSONObject();
          jsonData.put("id", rs.getString("id"));
          jsonData.put("recordIdentifier", rs.getString("value"));
          jsonArray.put(jsonData);
        }
      }

      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");

    } catch (final Exception e) {
      log4j.error("Exception in getSubAccount :", e);
      return jsob;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {

      }
    }
    return jsob;
  }

  @Override
  public JSONObject getSubAccountAgainstAccount(String account_id) throws Exception {
    JSONObject jsob = null;
    try {
      jsob = new JSONObject();
      if (account_id != null) {
        ElementValue account_obj = OBDal.getInstance().get(ElementValue.class, account_id);
        Project obj_project = account_obj.getEfinProject();
        if (obj_project != null) {
          jsob.put("sub_account_id", obj_project.getId());
          jsob.put("name", obj_project.getSearchKey().concat("-").concat(obj_project.getName()));
        }
      }

    } catch (OBException e) {
      log4j.error("Exception in getSubAccountAgainstAccount", e);
    }
    return jsob;
  }

}
