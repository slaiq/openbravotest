package sa.elm.ob.finance.ad_reports.TrialBalance.dao;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.calendar.Period;

import sa.elm.ob.finance.ad_forms.journalapproval.vo.GLJournalApprovalVO;
import sa.elm.ob.utility.util.Utility;

public class ReportTrialBalancePTDDAO {
  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(ReportTrialBalancePTDDAO.class);

  public ReportTrialBalancePTDDAO(Connection connection) {
    this.conn = connection;
  }

  public Connection getConnection() {
    return conn;
  }

  /**
   * THis method is used to get period
   * 
   * @param OrgId
   * @param ClientId
   * @param PeriodId
   * @return
   */
  public List<GLJournalApprovalVO> getPeriod(String OrgId, String ClientId, String PeriodId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = " SELECT C_PERIOD_ID as id, ((CASE C_PERIOD.isActive WHEN 'N' THEN '**' ELSE '' END) || C_PERIOD.Name) as name FROM C_PERIOD "
          + "    left join c_year yr on yr.c_year_id = C_PERIOD.c_year_id "
          + "	      WHERE C_PERIOD.AD_Org_ID IN (" + OrgId + ") AND C_PERIOD.AD_Client_ID IN("
          + ClientId + ")  AND (C_PERIOD.isActive = 'Y' OR C_PERIOD.C_PERIOD_ID = ? )"
          + "     ORDER BY  yr.year , periodno ";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, PeriodId);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setPeriod(rs.getString(1));
        VO.setPeriodName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
      return list;
    }
    return list;
  }

  /**
   * This method is used to get current period
   * 
   * @param vars
   * @param OrgId
   * @param ClientId
   * @return
   */
  public String getCurrentPeriod(VariablesSecureApp vars, String OrgId, String ClientId) {
    String CurrentPeriodId = "";
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      conn = OBDal.getInstance().getConnection();
      ps = conn.prepareStatement(
          "select c_period_id from c_period where to_date(to_char(now(),'dd-MM-yyyy'),'dd-MM-yyyy') between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') and ad_org_id in("
              + OrgId + ") and ad_client_id in(" + ClientId + ")");
      rs = ps.executeQuery();
      if (rs.next()) {
        CurrentPeriodId = rs.getString("c_period_id");
      }
    } catch (Exception e) {
    }
    return CurrentPeriodId;
  }

  /**
   * This method is used to get account schema
   * 
   * @param OrgId
   * @param ClientId
   * @param schemaId
   * @return
   */
  public List<GLJournalApprovalVO> getAcctSchema(String OrgId, String ClientId, String schemaId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "  SELECT C_ACCTSCHEMA_ID as id, ((CASE C_ACCTSCHEMA.isActive WHEN 'N' THEN '**' ELSE '' END) || C_ACCTSCHEMA.Name) as name FROM C_ACCTSCHEMA       WHERE C_ACCTSCHEMA.AD_Org_ID in ("
          + OrgId + ") AND C_ACCTSCHEMA.AD_Client_ID IN(" + ClientId
          + ")   AND (C_ACCTSCHEMA.isActive = 'Y'"
          + " OR C_ACCTSCHEMA.C_ACCTSCHEMA_ID = ? )     ORDER BY name ";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, schemaId);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setAcctschemaId(rs.getString(1));
        VO.setAcctschemaName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get organization
   * 
   * @param OrgId
   * @param ClientId
   * @return
   */
  public List<GLJournalApprovalVO> getOrganization(String OrgId, String ClientId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      // sqlQuery =
      // " SELECT ad_org_id as id, ((CASE ad_org.isActive WHEN 'N' THEN '**' ELSE '' END) ||
      // (ad_org.value||'-'|| ad_org.Name)) as name FROM ad_org WHERE ad_org.AD_Client_ID IN("
      // + ClientId + ") AND (ad_org.isActive = 'Y'" +
      // " OR ad_org.ad_org_id = ? ) ORDER BY name ";
      // query for listing org based on org type (legal with accounting)
      sqlQuery = "  SELECT O.ad_org_id as id,((CASE O.isActive WHEN 'N' THEN '**' ELSE '' END) || (O.value||'-'|| O.Name)) as name FROM ad_org O"
          + " JOIN AD_ORGTYPE OT ON O.AD_ORGTYPE_ID=OT.AD_ORGTYPE_ID "
          + "WHERE  (O.AD_Client_ID IN(" + ClientId
          + ") AND O.isActive = 'Y' AND O.isready = 'Y' AND OT.ISTRANSACTIONSALLOWED='Y') "
          + "OR O.ad_org_id = '0'  OR O.ad_org_id = ? ORDER BY O.name";

      st = conn.prepareStatement(sqlQuery);
      // st.setString(1, ClientId);
      st.setString(1, Utility.nullToEmpty(OrgId));
      log4j.debug("query:" + st.toString());

      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setOrgId(rs.getString("id"));
        VO.setOrgName(rs.getString("name"));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get account value
   * 
   * @param elementId
   * @return
   */
  public String getAccountValue(String elementId) {
    String sqlQuery = "", elementValue = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      sqlQuery = "   SELECT C_ELEMENTVALUE.VALUE   as value       FROM C_ELEMENTVALUE         WHERE C_ELEMENTVALUE_ID = ?         AND C_ELEMENTVALUE.ISACTIVE = 'Y'";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, elementId);
      rs = st.executeQuery();
      if (rs.next()) {
        elementValue = rs.getString("value");

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return elementValue;
  }

  /**
   * This method is used to get account description
   * 
   * @param elementId
   * @return
   */
  public String getAccountDescription(String elementId) {
    String sqlQuery = "", name = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      sqlQuery = "   SELECT C_ELEMENTVALUE.name   as name       FROM C_ELEMENTVALUE         WHERE C_ELEMENTVALUE_ID = ?         AND C_ELEMENTVALUE.ISACTIVE = 'Y'";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, elementId);
      rs = st.executeQuery();
      if (rs.next()) {
        name = rs.getString("name");

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return name;
  }

  /**
   * This method is used to get department
   * 
   * @param OrgId
   * @param ClientId
   * @param salesregionId
   * @return
   */
  public Integer getdepartment(String OrgId, String ClientId, String salesregionId) {
    String sqlQuery = "";
    Integer count = 0;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      sqlQuery = " SELECT count(*) as count ,C_SALESREGION.C_SALESREGION_ID  as defregion FROM C_SALESREGION WHERE C_SALESREGION.AD_Org_ID IN("
          + OrgId + ") AND C_SALESREGION.AD_Client_ID IN(" + ClientId
          + ")  AND C_SALESREGION.isdefault='Y'  AND C_SALESREGION.C_SALESREGION_ID IN("
          + salesregionId + ")  group by C_SALESREGION_ID";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      if (rs.next()) {
        count = rs.getInt("count");
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return count;
  }

  /**
   * This method is used to get period start end date
   * 
   * @param OrgId
   * @param ClientId
   * @param fromPeriod
   * @param toPeriod
   * @return
   */
  public String getPedStrEndDate(String OrgId, String ClientId, String fromPeriod,
      String toPeriod) {
    String sqlQuery = "", startdate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      sqlQuery = "    select startdate,enddate from c_period where  C_PERIOD.AD_Org_ID IN(" + OrgId
          + ") AND C_PERIOD.AD_Client_ID IN(" + ClientId + ")  AND c_period_id= ?  ";
      st = conn.prepareStatement(sqlQuery);
      if (fromPeriod != null)
        st.setString(1, fromPeriod);
      else
        st.setString(1, toPeriod);
      rs = st.executeQuery();
      if (rs.next()) {
        if (fromPeriod != null)
          startdate = rs.getString("startdate");
        else
          startdate = rs.getString("enddate");

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return startdate;
  }

  /**
   * This method is used to get pedstr date
   * 
   * @param OrgId
   * @param ClientId
   * @param fromPeriod
   * @return
   */
  public String getPedStrDate(String OrgId, String ClientId, String fromPeriod) {
    String sqlQuery = "", startdate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      sqlQuery = "   select startdate from c_period where  C_PERIOD.AD_Org_ID IN(" + OrgId
          + ") AND C_PERIOD.AD_Client_ID IN(" + ClientId + ") and c_year_id =("
          + " select c_year_id from c_period where c_period_id= ?)    and periodno ='1' ";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, fromPeriod);
      rs = st.executeQuery();
      if (rs.next()) {
        startdate = rs.getString("startdate");

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return startdate;
  }

  /**
   * This method is sued to get fc start date
   * 
   * @param OrgId
   * @param ClientId
   * @return
   */
  public String getFCStartDate(String OrgId, String ClientId) {
    String sqlQuery = "", startdate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      sqlQuery = "   select min(startdate) as startdate from c_period    where  C_PERIOD.AD_Org_ID IN("
          + OrgId + ") AND C_PERIOD.AD_Client_ID IN(" + ClientId + ")  ";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      if (rs.next()) {
        startdate = rs.getString("startdate");

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return startdate;
  }

  /**
   * This method is used to get funds
   * 
   * @param OrgId
   * @param ClientId
   * @param FundId
   * @param RoleId
   * @return
   */
  public List<GLJournalApprovalVO> getFunds(String OrgId, String ClientId, String FundId,
      String RoleId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "  SELECT C_CAMPAIGN_ID as id, ((CASE C_CAMPAIGN.isActive WHEN 'N' THEN '**' ELSE '' END) || (C_CAMPAIGN.Value||'-'||C_CAMPAIGN.Name)) as name FROM C_CAMPAIGN  "
          + "      WHERE C_CAMPAIGN.AD_Org_ID in (" + OrgId + ") AND C_CAMPAIGN.AD_Client_ID IN("
          + ClientId
          + ")  AND C_CAMPAIGN.em_efin_iscarryforward ='N'  AND (C_CAMPAIGN.isActive = 'Y' OR C_CAMPAIGN.C_CAMPAIGN_ID = ? ) AND C_CAMPAIGN.C_CAMPAIGN_ID IN(select bud.C_campaign_ID from efin_security_rules_budtype bud join efin_security_rules ru on ru.efin_security_rules_id=bud.efin_security_rules_id where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y')     ORDER BY name ";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, FundId);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setFundId(rs.getString(1));
        VO.setFundName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get entity
   * 
   * @param OrgId
   * @param ClientId
   * @param bpId
   * @return
   */
  public List<GLJournalApprovalVO> getEntity(String OrgId, String ClientId, String bpId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "   SELECT C_BPartner.C_BPartner_ID AS ID,C_BPartner.NAME      FROM C_BPartner    WHERE AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND AD_CLIENT_ID IN (" + ClientId + ")    AND C_BPartner_ID IN ("
          + bpId + ") ";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getEntity()", e);
    }
    return list;
  }

  /**
   * This method is used to get entity from unique code
   * 
   * @param OrgId
   * @param ClientId
   * @param bpId
   * @return
   */
  public List<GLJournalApprovalVO> getEntityfromuniquecode(String OrgId, String ClientId,
      String bpId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "select bp.c_bpartner_id as id, bp.name from c_bpartner bp "
          + "         join c_validcombination acc on acc.c_bpartner_id = bp.c_bpartner_id "
          + "         where acc.em_efin_uniquecode ='" + bpId + "' and acc.ad_client_id = '"
          + OBContext.getOBContext().getCurrentClient().getId() + "'";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);
      }
    } catch (Exception e) {
      log4j.error("Exception in getEntityfromuniquecode()", e);
    }
    return list;
  }

  public List<GLJournalApprovalVO> getDepartment(String OrgId, String ClientId, String deptId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "   SELECT C_SALESREGION.C_SALESREGION_ID AS ID,C_SALESREGION.NAME      FROM C_SALESREGION    WHERE AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND AD_CLIENT_ID IN (" + ClientId + ")    AND C_SALESREGION_ID IN ("
          + deptId + ") ";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get department from unique code
   * 
   * @param OrgId
   * @param ClientId
   * @param deptId
   * @return
   */
  public List<GLJournalApprovalVO> getDepartmentfromuniquecode(String OrgId, String ClientId,
      String deptId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = " select f.c_salesregion_id,sal.name from fact_acct f join c_salesregion sal on sal.c_salesregion_id = f.c_salesregion_id  WHERE f.AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND f.AD_CLIENT_ID IN (" + ClientId
          + ")    AND f.em_efin_uniquecode IN ('" + deptId
          + "') and f.c_bpartner_id is not null and f.c_salesregion_id is not null and f.c_project_id is not null and f.c_campaign_id is not null and f.c_activity_id is not null and f.user1_id is not null and f.user2_id is not null limit 1";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get activity
   * 
   * @param OrgId
   * @param ClientId
   * @param actId
   * @return
   */
  public List<GLJournalApprovalVO> getActivity(String OrgId, String ClientId, String actId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "   SELECT C_Activity.C_Activity_ID AS ID,C_Activity.NAME      FROM C_Activity    WHERE AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND AD_CLIENT_ID IN (" + ClientId + ")    AND C_Activity_ID IN ("
          + actId + ") ";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get activity from unique code
   * 
   * @param OrgId
   * @param ClientId
   * @param actId
   * @return
   */
  public List<GLJournalApprovalVO> getActivityfromuniquecode(String OrgId, String ClientId,
      String actId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = " select f.c_activity_id,act.name from fact_acct f join c_activity act on act.c_activity_id = f.c_activity_id  WHERE f.AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND f.AD_CLIENT_ID IN (" + ClientId
          + ")    AND f.em_efin_uniquecode IN ('" + actId
          + "') and f.c_bpartner_id is not null and f.c_salesregion_id is not null and f.c_project_id is not null and f.c_campaign_id is not null and f.c_activity_id is not null and f.user1_id is not null and f.user2_id is not null limit 1";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get user1
   * 
   * @param OrgId
   * @param ClientId
   * @param user1Id
   * @return
   */
  public List<GLJournalApprovalVO> getUser1(String OrgId, String ClientId, String user1Id) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "   SELECT USER1.USER1_ID AS ID,USER1.NAME      FROM USER1    WHERE AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND AD_CLIENT_ID IN (" + ClientId + ")    AND USER1_ID IN (" + user1Id
          + ") ";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get user1 from unique code
   * 
   * @param OrgId
   * @param ClientId
   * @param userId
   * @return
   */
  public List<GLJournalApprovalVO> getUser1fromuniquecode(String OrgId, String ClientId,
      String userId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = " select f.user1_id,us1.name from fact_acct f join user1 us1 on us1.user1_id = f.user1_id  WHERE f.AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND f.AD_CLIENT_ID IN (" + ClientId
          + ")    AND f.em_efin_uniquecode IN ('" + userId
          + "') and f.c_bpartner_id is not null and f.c_salesregion_id is not null and f.c_project_id is not null and f.c_campaign_id is not null and f.c_activity_id is not null and f.user1_id is not null and f.user2_id is not null limit 1";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get user2
   * 
   * @param OrgId
   * @param ClientId
   * @param user2Id
   * @return
   */
  public List<GLJournalApprovalVO> getUser2(String OrgId, String ClientId, String user2Id) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "   SELECT USER2.USER2_ID AS ID,USER2.NAME      FROM USER2    WHERE AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND AD_CLIENT_ID IN (" + ClientId + ")    AND USER2_ID IN (" + user2Id
          + ") ";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get user2 from unique code
   * 
   * @param OrgId
   * @param ClientId
   * @param userId
   * @return
   */
  public List<GLJournalApprovalVO> getUser2fromuniquecode(String OrgId, String ClientId,
      String userId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = " select f.user2_id,us2.name from fact_acct f join user2 us2 on us2.user2_id = f.user2_id  WHERE f.AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND f.AD_CLIENT_ID IN (" + ClientId
          + ")    AND f.em_efin_uniquecode IN ('" + userId
          + "') and f.c_bpartner_id is not null and f.c_salesregion_id is not null and f.c_project_id is not null and f.c_campaign_id is not null and f.c_activity_id is not null and f.user1_id is not null and f.user2_id is not null limit 1";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get project
   * 
   * @param OrgId
   * @param ClientId
   * @param projectId
   * @return
   */
  public List<GLJournalApprovalVO> getProject(String OrgId, String ClientId, String projectId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "   SELECT C_PROJECT.C_PROJECT_ID AS ID,C_PROJECT.NAME      FROM C_PROJECT    WHERE AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND AD_CLIENT_ID IN (" + ClientId + ")    AND C_PROJECT_ID IN ("
          + projectId + ") ";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get project from unique code
   * 
   * @param OrgId
   * @param ClientId
   * @param proId
   * @return
   */
  public List<GLJournalApprovalVO> getProjectfromuniquecode(String OrgId, String ClientId,
      String proId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = " select f.c_project_id,pro.name from fact_acct f join c_project pro on pro.c_project_id = f.c_project_id  WHERE f.AD_ORG_ID IN  ("
          + OrgId + ") " + "  AND f.AD_CLIENT_ID IN (" + ClientId
          + ")    AND f.em_efin_uniquecode IN ('" + proId
          + "') and f.c_bpartner_id is not null and f.c_salesregion_id is not null and f.c_project_id is not null and f.c_campaign_id is not null and f.c_activity_id is not null and f.user1_id is not null and f.user2_id is not null limit 1";
      st = conn.prepareStatement(sqlQuery);
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setName(rs.getString(2));
        list.add(VO);
      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * This method is used to get all period
   * 
   * @param ClientId
   * @param FromDate
   * @param ToDate
   * @return
   */
  public List<GLJournalApprovalVO> getAllPeriod(String ClientId, String FromDate, String ToDate) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      sqlQuery = "   SELECT c_period.c_period_id AS ID,to_char(c_period.startdate,'dd-MM-YYYY'),c_period.name,c_period.periodno    FROM c_period    WHERE "
          + " AD_CLIENT_ID IN (" + ClientId
          + ")   and  c_period.startdate>=TO_DATE(?) and c_period.enddate<=TO_DATE(?)  order by c_period.startdate ";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, FromDate);
      st.setString(2, ToDate);
      log4j.debug("period:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setId(rs.getString(1));
        VO.setStartdate(rs.getString(2));
        VO.setName(rs.getString(3));
        VO.setPeriod(rs.getString(4));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * THis method is used to select lines
   * 
   * @param vars
   * @param fromDate
   * @param InitialBalance
   * @param YearInitialBal
   * @param strClientId
   * @param accountId
   * @param BpartnerId
   * @param productId
   * @param projectId
   * @param DeptId
   * @param BudgetTypeId
   * @param FunclassId
   * @param User1Id
   * @param User2Id
   * @param AcctschemaId
   * @param strOrgFamily
   * @param ClientId
   * @param OrgId
   * @param DateTo
   * @param strAccountFromValue
   * @param strAccountToValue
   * @param strStrDateFC
   * @param FrmPerStDate
   * @param uniqueCode
   * @param inpcElementValueIdFrom
   * @return
   */
  @SuppressWarnings({ "unused", "resource" })
  public JSONObject selectLines(VariablesSecureApp vars, String fromDate, String InitialBalance,
      String YearInitialBal, String strClientId, String accountId, String BpartnerId,
      String productId, String projectId, String DeptId, String BudgetTypeId, String FunclassId,
      String User1Id, String User2Id, String AcctschemaId, String strOrgFamily, String ClientId,
      String OrgId, String DateTo, String strAccountFromValue, String strAccountToValue,
      String strStrDateFC, String FrmPerStDate, String uniqueCode, String inpcElementValueIdFrom) {
    File file = null;
    String sqlQuery = "", sqlQuery1 = "", tempUniqCode = "", date = "", groupClause = "",
        tempStartDate = "", periodId = "";
    BigDecimal initialDr = new BigDecimal(0);
    BigDecimal initialCr = new BigDecimal(0);
    BigDecimal initialNet = new BigDecimal(0);
    PreparedStatement st = null;
    ResultSet rs = null;
    String RoleId = vars.getRole();
    List<GLJournalApprovalVO> list = null;
    JSONObject obj = null;
    int count = 0;
    JSONObject result = new JSONObject();
    JSONArray array = new JSONArray();
    JSONArray arr = null;
    String listofuniquecode = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();

      sqlQuery = "select a.id,a.c_period_id as periodid,a.periodno,to_char(a.startdate,'dd-MM-yyyy') as startdate ,a.name ,ev.accounttype as type,COALESCE(A.EM_EFIN_UNIQUECODE,ev.value) as account_id, ev.name, a.EM_EFIN_UNIQUECODE as uniquecode,a.defaultdep, a.depart,ev.value as account ,"
          + "          replace(a.EM_EFIN_UNIQUECODE,'-'||a.depart||'-','-'||a.defaultdep||'-') as replaceacct, "
          + "  sum(initalamtdr)as initalamtdr,  sum(intialamtcr)as intialamtcr  ,  sum(a.initialnet )as SALDO_INICIAL,sum( a.amtacctcr) as amtacctcr, sum(a.amtacctdr) as amtacctdr, sum(A.AMTACCTDR)-sum(A.AMTACCTCR) AS SALDO_FINAL  ,"
          + "  sum(initalamtdr)+sum( a.amtacctdr) as finaldr, sum(a.intialamtcr)+sum( a.amtacctcr)   as finalcr , sum(a.initialnet)+sum(A.AMTACCTDR)-sum(A.AMTACCTCR) as finalnet  "
          + "         from(             SELECT  per.startdate,per.name ,per.periodno ,(case when (DATEACCT < TO_DATE(?) or (DATEACCT = TO_DATE(?) and F.FACTACCTTYPE = ?)) then F.AMTACCTDR  else 0 end) as initalamtdr, "
          + "(case when (DATEACCT < TO_DATE(?) or (DATEACCT = TO_DATE(?) and F.FACTACCTTYPE = ?)) then  F.AMTACCTCR else 0 end) as intialamtcr, "
          + " (case when (DATEACCT < TO_DATE(?) or (DATEACCT = TO_DATE(?) and F.FACTACCTTYPE = ?)) then F.AMTACCTDR - F.AMTACCTCR else 0 end) as initialnet, "
          + "             (case when (DATEACCT >= TO_DATE(?) AND F.FACTACCTTYPE not in('O', 'R', 'C')) or (DATEACCT = TO_DATE(?) and F.FACTACCTTYPE = ?) then F.AMTACCTDR else 0 end) as AMTACCTDR, "
          + "             (case when (DATEACCT >= TO_DATE(?) AND F.FACTACCTTYPE not in('O', 'R', 'C')) or (DATEACCT = TO_DATE(?) and F.FACTACCTTYPE = ?) then F.AMTACCTCR else 0 end) as AMTACCTCR, "
          + "             F.ACCOUNT_ID AS ID,F.EM_EFIN_UNIQUECODE,reg.value as depart,f.c_period_id , "
          + " (select REG.value from c_salesregion REG  where REG.isdefault='Y'  ";

      if (strClientId != null)
        sqlQuery += "AND REG.AD_CLIENT_ID IN " + strClientId;
      sqlQuery += ") as defaultdep " + "   FROM FACT_ACCT F "
          + " left join (select name,periodno,startdate,c_period_id from c_period ) per on per.c_period_id=f.c_period_id   left  join c_salesregion reg on reg.c_salesregion_id= f.c_salesregion_id  where 1=1 ";
      if (strOrgFamily != null)
        sqlQuery += "AND F.AD_ORG_ID IN (" + strOrgFamily + ")";
      if (ClientId != null)
        sqlQuery += "AND F.AD_CLIENT_ID IN (" + ClientId + ")";
      if (ClientId != null)
        sqlQuery += "AND F.AD_ORG_ID IN (" + OrgId + ")";
      // sqlQuery += " AND DATEACCT > TO_DATE(?) AND DATEACCT < TO_DATE(?) AND 1=1 ";
      sqlQuery += "  AND DATEACCT >= TO_DATE(?)  AND DATEACCT < TO_DATE(?) AND 1=1  ";
      if (accountId != null)
        sqlQuery += "AND F.account_ID='" + accountId + "'";
      if (BpartnerId != null)
        sqlQuery += "AND  F.C_BPARTNER_ID IN (" + BpartnerId.replaceFirst(",", "") + ")";
      if (productId != null)
        sqlQuery += "AND F.M_PRODUCT_ID IN " + productId;
      if (projectId != null)
        sqlQuery += "AND F.C_PROJECT_ID IN (" + projectId.replaceFirst(",", "") + ")";
      if (DeptId != null)
        sqlQuery += "AND F.C_SALESREGION_ID IN (" + DeptId.replaceFirst(",", "") + ")";
      if (BudgetTypeId != null)
        sqlQuery += "AND coalesce(F.C_CAMPAIGN_ID, (select c.C_CAMPAIGN_ID from C_CAMPAIGN c where em_efin_iscarryforward = 'N' and ad_client_id = F.ad_client_id limit 1))='"
            + BudgetTypeId + "'";
      if (FunclassId != null)
        sqlQuery += "AND F.C_ACTIVITY_ID IN (" + FunclassId.replaceFirst(",", "") + ")";
      if (User1Id != null)
        sqlQuery += "AND F.USER1_ID IN (" + User1Id.replaceFirst(",", "") + ")";
      if (User2Id != null)
        sqlQuery += "AND F.USER2_ID IN (" + User2Id.replaceFirst(",", "") + ")";
      if (AcctschemaId != null)
        sqlQuery += "AND F.C_ACCTSCHEMA_ID ='" + AcctschemaId + "'";
      if (uniqueCode != null)
        sqlQuery += "		AND (F.EM_EFIN_UNIQUECODE = '" + uniqueCode + "' or F.acctvalue='"
            + uniqueCode + "')";

      sqlQuery += " AND F.ACCOUNT_ID IN (select act.c_elementvalue_id from efin_security_rules_act act "
          + "join efin_security_rules ru on ru.efin_security_rules_id=act.efin_security_rules_id "
          + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y') "
          + "AND F.C_Salesregion_ID in (select dep.C_Salesregion_ID from Efin_Security_Rules_Dept dep "
          + "join efin_security_rules ru on ru.efin_security_rules_id=dep.efin_security_rules_id "
          + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y') "
          + "AND F.C_Project_ID in (select proj.c_project_id from efin_security_rules_proj proj "
          + "join efin_security_rules ru on ru.efin_security_rules_id=proj.efin_security_rules_id "
          + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y') "
          + "AND F.C_CAMPAIGN_ID IN(select bud.C_campaign_ID from efin_security_rules_budtype bud "
          + "join efin_security_rules ru on ru.efin_security_rules_id=bud.efin_security_rules_id "
          + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y') "
          + "AND F.C_Activity_ID in (select act.C_Activity_ID from efin_security_rules_activ act "
          + " join efin_security_rules ru on ru.efin_security_rules_id=act.efin_security_rules_id "
          + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y') "
          + "AND F.User1_ID in (select fut1.User1_ID from efin_security_rules_fut1 fut1 "
          + " join efin_security_rules ru on ru.efin_security_rules_id=fut1.efin_security_rules_id "
          + " where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y') "
          + "AND F.User2_ID in (select fut2.User2_ID from efin_security_rules_fut2 fut2 "
          + " join efin_security_rules ru on ru.efin_security_rules_id=fut2.efin_security_rules_id "
          + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + RoleId + "' )and efin_processbutton='Y') ";

      sqlQuery += " AND F.ISACTIVE='Y' " + "         ) a, c_elementvalue ev "
          + "          where a.id = ev.c_elementvalue_id           and ev.elementlevel = 'S'       ";
      if (strAccountFromValue != null)
        sqlQuery += "		AND EV.VALUE >= '" + strAccountFromValue + "'";
      if (strAccountToValue != null)
        sqlQuery += "		AND EV.VALUE <= '" + strAccountToValue + "'";

      sqlQuery += " ";

      sqlQuery += " and (a.initialnet <>0 or a.amtacctcr <>0 or a.amtacctdr<>0)  group by  a.c_period_id,a.name,ev.accounttype, a.startdate , a.id ,a.periodno, A.EM_EFIN_UNIQUECODE,ev.value,ev.name,a.defaultdep,a.depart "
          + "          order by uniquecode,a.startdate, account_id ,ev.value, ev.name, id ";

      st = conn.prepareStatement(sqlQuery);
      st.setString(1, fromDate);
      st.setString(2, fromDate);
      st.setString(3, InitialBalance);
      st.setString(4, fromDate);
      st.setString(5, fromDate);
      st.setString(6, InitialBalance);
      st.setString(7, fromDate);
      st.setString(8, fromDate);
      st.setString(9, InitialBalance);
      st.setString(10, fromDate);
      st.setString(11, fromDate);
      st.setString(12, YearInitialBal);
      st.setString(13, fromDate);
      st.setString(14, fromDate);
      st.setString(15, YearInitialBal);
      st.setString(16, fromDate);
      st.setString(17, DateTo);
      // st.setString(16, DateTo);
      log4j.debug("ReportTrialBalancePTD:" + st.toString());
      rs = st.executeQuery();
      // Particular Date Range if we get record then need to form the JSONObject
      while (rs.next()) {
        // Group UniqueCode Wise Transaction
        // if same uniquecode then Group the transaction under the uniquecode
        if (tempUniqCode.equals(rs.getString("uniquecode"))) {
          arr = obj.getJSONArray("transaction");
          JSONObject tra = new JSONObject();
          if (rs.getInt("periodno") == 1
              && (rs.getString("type").equals("E") || rs.getString("type").equals("R"))) {
            initialDr = new BigDecimal(0);
            initialCr = new BigDecimal(0);
            initialNet = new BigDecimal(0);
            FrmPerStDate = getPedStrEndDate(OrgId, ClientId, rs.getString("periodid"), null);
            FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
          } else {
            if (rs.getInt("periodno") > 1) {
              FrmPerStDate = getPedStrDate(OrgId, ClientId, rs.getString("periodid"));
              FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                  .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
            }
            List<GLJournalApprovalVO> initial = selectInitialBal(rs.getString("account_id"),
                rs.getString("startdate"), FrmPerStDate, strStrDateFC, rs.getString("type"), 2,
                RoleId);
            if (initial.size() > 0) {
              GLJournalApprovalVO vo = initial.get(0);
              initialDr = vo.getInitDr();
              initialCr = vo.getInitCr();
              initialNet = vo.getInitNet();
            }
          }
          tra.put("startdate", rs.getString("name"));
          tra.put("date", new SimpleDateFormat("MM-dd-yyyy")
              .format(new SimpleDateFormat("dd-MM-yyyy").parse(rs.getString("startdate"))));
          tra.put("perDr", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("amtacctdr")));
          tra.put("perCr", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("amtacctcr")));
          tra.put("finalpernet", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("SALDO_FINAL")));
          tra.put("initialDr",
              Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
          tra.put("initialCr",
              Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
          tra.put("initialNet",
              Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
          tra.put("finaldr", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              (rs.getBigDecimal("amtacctdr").add(initialDr))));
          tra.put("finalcr", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              (rs.getBigDecimal("amtacctcr").add(initialCr))));
          tra.put("finalnet", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              (rs.getBigDecimal("SALDO_FINAL").add(initialNet))));
          tra.put("id", rs.getString("id"));
          tra.put("periodid", rs.getString("periodid"));
          tra.put("type", "1");
          arr.put(tra);

        }
        // if different uniquecode then form new Uniquecode JsonObject
        else {
          if (listofuniquecode == null)
            listofuniquecode = "'" + rs.getString("uniquecode") + "'";
          else
            listofuniquecode += ",'" + rs.getString("uniquecode") + "'";
          obj = new JSONObject();
          obj.put("uniquecode", (rs.getString("uniquecode") == null ? rs.getString("account")
              : rs.getString("uniquecode")));
          obj.put("accountId", (rs.getString("id") == null ? "" : rs.getString("id")));
          arr = new JSONArray();
          JSONObject tra = new JSONObject();
          if (rs.getInt("periodno") == 1
              && (rs.getString("type").equals("E") || rs.getString("type").equals("R"))) {
            initialDr = new BigDecimal(0);
            initialCr = new BigDecimal(0);
            initialNet = new BigDecimal(0);
            FrmPerStDate = getPedStrEndDate(OrgId, ClientId, rs.getString("periodid"), null);
            FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
          } else {
            if (rs.getInt("periodno") > 1) {
              FrmPerStDate = getPedStrDate(OrgId, ClientId, rs.getString("periodid"));
              FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                  .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
            }
            List<GLJournalApprovalVO> initial = selectInitialBal(rs.getString("account_id"),
                rs.getString("startdate"), FrmPerStDate, strStrDateFC, rs.getString("type"), 1,
                RoleId);
            if (initial.size() > 0) {
              GLJournalApprovalVO vo = initial.get(0);
              initialDr = vo.getInitDr();
              initialCr = vo.getInitCr();
              initialNet = vo.getInitNet();
            }
          }
          tra.put("startdate", rs.getString("name"));
          tra.put("date", new SimpleDateFormat("MM-dd-yyyy")
              .format(new SimpleDateFormat("dd-MM-yyyy").parse(rs.getString("startdate"))));
          tra.put("initialDr",
              Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
          tra.put("initialCr",
              Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
          tra.put("initialNet",
              Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
          tra.put("perDr", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("amtacctdr")));
          tra.put("perCr", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("amtacctcr")));
          tra.put("finalpernet", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              rs.getBigDecimal("SALDO_FINAL")));
          tra.put("finaldr", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              (rs.getBigDecimal("amtacctdr").add(initialDr))));
          tra.put("finalcr", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              (rs.getBigDecimal("amtacctcr").add(initialCr))));
          tra.put("finalnet", Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
              (rs.getBigDecimal("SALDO_FINAL").add(initialNet))));
          tra.put("id", rs.getString("id"));
          tra.put("periodid", rs.getString("periodid"));
          /*
           * if((initialDr.compareTo(new BigDecimal(0)) > 0) || (initialCr.compareTo(new
           * BigDecimal(0)) > 0) || (initialNet.compareTo(new BigDecimal(0)) > 0)) { tra.put("type",
           * "1"); } else
           */
          tra.put("type", "1");
          arr.put(tra);
          obj.put("transaction", arr);
          array.put(obj);
          tempUniqCode = rs.getString("uniquecode");
        }

        result.put("list", array);
      }
      log4j.debug("has lsit:" + result.has("list"));
      if (result.has("list")) {
        JSONArray finalres = result.getJSONArray("list");
        JSONObject json = null, json1 = null, json2 = null;
        log4j.debug("json.length:" + finalres.length());
        for (int i = 0; i < finalres.length(); i++) {
          json = finalres.getJSONObject(i);
          log4j.debug("json.getString:" + json.getString("uniquecode"));
          if (json.getString("uniquecode") != null) {
            String UniqueCode = json.getString("uniquecode");
            String acctId = json.getString("accountId");
            ElementValue type = OBDal.getInstance().get(ElementValue.class, acctId);
            JSONArray transaction = json.getJSONArray("transaction");
            List<GLJournalApprovalVO> period = getAllPeriod(ClientId, fromDate, DateTo);
            List<String> periodIds = new ArrayList<String>(), tempPeriods = new ArrayList<String>();

            for (GLJournalApprovalVO vo : period) {
              periodIds.add(vo.getId());
            }

            tempPeriods = periodIds;

            for (int j = 0; j < transaction.length(); j++) {
              json1 = transaction.getJSONObject(j);
              periodId = json1.getString("periodid");
              tempPeriods.remove(periodId);
            }
            log4j.debug("size:" + tempPeriods.size());
            if (tempPeriods.size() > 0) {
              log4j.debug("jtempPeriods:" + tempPeriods);
              count = 0;
              for (String missingPeriods : tempPeriods) {
                json2 = new JSONObject();
                json2.put("startdate", Utility.getObject(Period.class, missingPeriods).getName());
                Date startdate = Utility.getObject(Period.class, missingPeriods).getStartingDate();
                json2.put("date", new SimpleDateFormat("MM-dd-yyyy").format(startdate));
                json2.put("perDr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                json2.put("perCr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                json2.put("finalpernet",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                json2.put("id", acctId);
                json2.put("periodid", missingPeriods);
                Long periodNo = Utility.getObject(Period.class, missingPeriods).getPeriodNo();
                if (new BigDecimal(periodNo).compareTo(new BigDecimal(1)) == 0
                    && (type.getAccountType().equals("E") || type.getAccountType().equals("R"))) {
                  json2.put("initialDr",
                      Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                  json2.put("initialCr",
                      Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                  json2.put("initialNet",
                      Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                  json2.put("finaldr",
                      Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                  json2.put("finalcr",
                      Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                  json2.put("finalnet",
                      Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                  json2.put("type", "1");

                  /*
                   * count++; if(count > 0) { FrmPerStDate = getPedStrEndDate(OrgId, ClientId,
                   * missingPeriods, null); FrmPerStDate = new
                   * SimpleDateFormat("dd-MM-yyyy").format(new
                   * SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate)); }
                   */

                } else {
                  // Get the Last Opening and closing Balance Entry Date
                  // If closing and opening is not happened then assign financial year start date
                  Boolean isThereOpeningBeforeThePeriod = false;
                  String tempStartDateFC = strStrDateFC;
                  strStrDateFC = selectLastOpeningBalanceDate(OrgId, ClientId, missingPeriods);
                  if (StringUtils.isNotEmpty(strStrDateFC)) {
                    isThereOpeningBeforeThePeriod = true;
                    strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                        .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
                  } else {
                    strStrDateFC = tempStartDateFC;
                  }

                  FrmPerStDate = getPedStrEndDate(OrgId, ClientId, missingPeriods, null);
                  FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                      .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
                  List<GLJournalApprovalVO> initial = selectInitialBal(UniqueCode,
                      new SimpleDateFormat("dd-MM-yyyy").format(startdate), FrmPerStDate,
                      strStrDateFC, type.getAccountType(), 2, RoleId);
                  if (initial.size() > 0) {
                    GLJournalApprovalVO vo = initial.get(0);
                    json2.put("initialDr", Utility.getNumberFormat(vars,
                        Utility.numberFormat_PriceRelation, (vo.getInitDr())));
                    json2.put("initialCr", Utility.getNumberFormat(vars,
                        Utility.numberFormat_PriceRelation, (vo.getInitCr())));
                    json2.put("initialNet", Utility.getNumberFormat(vars,
                        Utility.numberFormat_PriceRelation, (vo.getInitNet())));
                    if (((vo.getInitDr().compareTo(new BigDecimal(0)) > 0)
                        || (vo.getInitCr().compareTo(new BigDecimal(0)) > 0)
                        || (vo.getInitNet().compareTo(new BigDecimal(0)) > 0))
                        || isThereOpeningBeforeThePeriod) {
                      json2.put("type", "1");
                    } else
                      json2.put("type", "0");

                    json2.put("finaldr", Utility.getNumberFormat(vars,
                        Utility.numberFormat_PriceRelation, new BigDecimal(0).add(vo.getInitDr())));
                    json2.put("finalcr", Utility.getNumberFormat(vars,
                        Utility.numberFormat_PriceRelation, new BigDecimal(0).add(vo.getInitCr())));
                    json2.put("finalnet",
                        Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation,
                            new BigDecimal(0).add(vo.getInitNet())));

                  }
                }

                transaction.put(json2);
              }
            }

            json.put("transaction", transaction);
          }

        }
        log4j.debug("LIST:" + list);
        log4j.debug("Acct PTD listofuniquecode:" + listofuniquecode);

        List<GLJournalApprovalVO> period = getAllPeriod(ClientId, fromDate, DateTo);
        String tempYear = null;
        sqlQuery1 = " select a.uniquecode,a.id from(  select distinct coalesce(em_efin_uniquecode,acctvalue) as uniquecode ,f.account_id as id from fact_acct  f where 1=1  ";
        if (strOrgFamily != null)
          sqlQuery1 += "AND F.AD_ORG_ID IN (" + strOrgFamily + ")";
        if (ClientId != null)
          sqlQuery1 += "AND F.AD_CLIENT_ID IN (" + ClientId + ")";
        if (ClientId != null)
          sqlQuery1 += "AND F.AD_ORG_ID IN (" + OrgId + ")";
        sqlQuery1 += "  AND DATEACCT < TO_DATE(?) AND 1=1  ";
        if (accountId != null)
          sqlQuery1 += "AND F.account_ID='" + accountId + "'";
        if (BpartnerId != null)
          sqlQuery1 += "AND  F.C_BPARTNER_ID IN (" + BpartnerId.replaceFirst(",", "") + ")";
        if (productId != null)
          sqlQuery1 += "AND F.M_PRODUCT_ID IN (" + productId.replaceFirst(",", "") + ")";
        if (projectId != null)
          sqlQuery1 += "AND F.C_PROJECT_ID IN (" + projectId.replaceFirst(",", "") + ")";
        if (DeptId != null)
          sqlQuery1 += "AND F.C_SALESREGION_ID IN (" + DeptId.replaceFirst(",", "") + ")";
        if (BudgetTypeId != null)
          sqlQuery1 += "AND coalesce(F.C_CAMPAIGN_ID, (select c.C_CAMPAIGN_ID from C_CAMPAIGN c where em_efin_iscarryforward = 'N' and ad_client_id = F.ad_client_id limit 1))='"
              + BudgetTypeId + "'";
        if (FunclassId != null)
          sqlQuery1 += "AND F.C_ACTIVITY_ID IN (" + FunclassId.replaceFirst(",", "") + ")";
        if (User1Id != null)
          sqlQuery1 += "AND F.USER1_ID IN (" + User1Id.replaceFirst(",", "") + ")";
        if (User2Id != null)
          sqlQuery1 += "AND F.USER2_ID IN (" + User2Id.replaceFirst(",", "") + ")";
        if (AcctschemaId != null)
          sqlQuery1 += "AND F.C_ACCTSCHEMA_ID ='" + AcctschemaId + "'";

        if (uniqueCode != null)
          sqlQuery1 += "              AND (F.EM_EFIN_UNIQUECODE = '" + uniqueCode
              + "' or F.acctvalue='" + uniqueCode + "')";
        if (listofuniquecode != null)
          sqlQuery1 += "              AND F.EM_EFIN_UNIQUECODE  not in ( " + listofuniquecode + ")";
        sqlQuery1 += " AND F.ISACTIVE='Y' " + "         ) a, c_elementvalue ev "
            + "          where a.id = ev.c_elementvalue_id           and ev.elementlevel = 'S'       ";
        if (strAccountFromValue != null)
          sqlQuery1 += "              AND EV.VALUE >= '" + strAccountFromValue + "'";
        if (strAccountToValue != null)
          sqlQuery1 += "              AND EV.VALUE <= '" + strAccountToValue + "'";
        st = conn.prepareStatement(sqlQuery1);
        st.setString(1, DateTo);
        log4j.debug("Acct PTD afterlist:" + st.toString());
        rs = st.executeQuery();
        while (rs.next()) {
          ElementValue type = OBDal.getInstance().get(ElementValue.class, rs.getString("id"));
          for (GLJournalApprovalVO vo : period) {

            if (tempYear != null) {
              if (!tempYear.equals(vo.getStartdate().split("-")[2])) {
                FrmPerStDate = getPedStrDate(OrgId, ClientId, vo.getId());
                FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                    .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
              }
            } else {
              tempYear = vo.getStartdate().split("-")[2];
              FrmPerStDate = getPedStrDate(OrgId, ClientId, vo.getId());
              FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                  .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
            }

            String tempStartDateFC = strStrDateFC;
            strStrDateFC = selectLastOpeningBalanceDate(OrgId, ClientId, vo.getId());
            if (StringUtils.isNotEmpty(strStrDateFC)) {
              strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                  .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
            } else {
              strStrDateFC = tempStartDateFC;
            }
            List<GLJournalApprovalVO> initial = selectInitialBal(rs.getString("uniquecode"),
                vo.getStartdate(), FrmPerStDate, strStrDateFC, type.getAccountType(), 1, RoleId);
            if (initial.size() > 0) {
              GLJournalApprovalVO VO = initial.get(0);
              initialDr = VO.getInitDr();
              initialCr = VO.getInitCr();
              initialNet = VO.getInitNet();

            }
            if (tempUniqCode.equals(rs.getString("uniquecode"))) {
              arr = obj.getJSONArray("transaction");
              JSONObject tra = new JSONObject();
              tra.put("startdate", vo.getName());
              tra.put("date", new SimpleDateFormat("MM-dd-yyyy")
                  .format(new SimpleDateFormat("dd-MM-yyyy").parse(FrmPerStDate)));
              tra.put("perDr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("perCr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("finalpernet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("initialDr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
              tra.put("initialCr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
              tra.put("initialNet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
              tra.put("finaldr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
              tra.put("finalcr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
              tra.put("finalnet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
              tra.put("id", accountId);
              tra.put("periodid", vo.getId());
              tra.put("type", "1");
              arr.put(tra);
            } else {
              obj = new JSONObject();
              obj.put("uniquecode", rs.getString("uniquecode"));
              obj.put("accountId", rs.getString("id"));
              arr = new JSONArray();
              JSONObject tra = new JSONObject();
              tra.put("startdate", vo.getName());
              tra.put("date", new SimpleDateFormat("MM-dd-yyyy")
                  .format(new SimpleDateFormat("dd-MM-yyyy").parse(FrmPerStDate)));
              tra.put("perDr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("perCr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("finalpernet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("initialDr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
              tra.put("initialCr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
              tra.put("initialNet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
              tra.put("finaldr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
              tra.put("finalcr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
              tra.put("finalnet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
              tra.put("id", rs.getString("id"));
              tra.put("periodid", vo.getId());
              tra.put("type", "1");
              arr.put(tra);
              obj.put("transaction", arr);
              array.put(obj);
              tempUniqCode = rs.getString("uniquecode");
            }
          }

        }
      }
      // Transaction not exists for the period
      else if (!result.has("list")) {

        List<GLJournalApprovalVO> period = getAllPeriod(ClientId, fromDate, DateTo);
        String tempYear = null;

        if (uniqueCode != null) {
          ElementValue type = OBDal.getInstance().get(ElementValue.class, inpcElementValueIdFrom);
          for (GLJournalApprovalVO vo : period) {

            if (tempYear != null) {
              if (!tempYear.equals(vo.getStartdate().split("-")[2])) {
                FrmPerStDate = getPedStrDate(OrgId, ClientId, vo.getId());
                FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                    .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
              }
            } else {
              tempYear = vo.getStartdate().split("-")[2];
              FrmPerStDate = getPedStrDate(OrgId, ClientId, vo.getId());
              FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                  .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
            }
            String tempStartDateFC = strStrDateFC;
            strStrDateFC = selectLastOpeningBalanceDate(OrgId, ClientId, vo.getId());
            if (StringUtils.isNotEmpty(strStrDateFC)) {
              strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                  .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
            } else {
              strStrDateFC = tempStartDateFC;
            }

            List<GLJournalApprovalVO> initial = selectInitialBal(uniqueCode, vo.getStartdate(),
                FrmPerStDate, strStrDateFC, type.getAccountType(), 1, RoleId);
            if (initial.size() > 0) {
              GLJournalApprovalVO VO = initial.get(0);
              initialDr = VO.getInitDr();
              initialCr = VO.getInitCr();
              initialNet = VO.getInitNet();

            }
            if (tempUniqCode.equals(uniqueCode)) {
              arr = obj.getJSONArray("transaction");
              JSONObject tra = new JSONObject();
              tra.put("startdate", vo.getName());
              tra.put("date", new SimpleDateFormat("MM-dd-yyyy")
                  .format(new SimpleDateFormat("dd-MM-yyyy").parse(FrmPerStDate)));
              tra.put("perDr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("perCr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("finalpernet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("initialDr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
              tra.put("initialCr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
              tra.put("initialNet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
              tra.put("finaldr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
              tra.put("finalcr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
              tra.put("finalnet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
              tra.put("id", accountId);
              tra.put("periodid", vo.getId());
              tra.put("type", "1");
              arr.put(tra);
            } else {
              obj = new JSONObject();
              obj.put("uniquecode", uniqueCode);
              obj.put("accountId", accountId);
              arr = new JSONArray();
              JSONObject tra = new JSONObject();
              tra.put("startdate", vo.getName());
              tra.put("date", new SimpleDateFormat("MM-dd-yyyy")
                  .format(new SimpleDateFormat("dd-MM-yyyy").parse(FrmPerStDate)));
              tra.put("perDr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("perCr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("finalpernet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
              tra.put("initialDr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
              tra.put("initialCr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
              tra.put("initialNet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
              tra.put("finaldr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
              tra.put("finalcr",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
              tra.put("finalnet",
                  Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
              tra.put("id", accountId);
              tra.put("periodid", vo.getId());
              tra.put("type", "1");
              arr.put(tra);
              obj.put("transaction", arr);
              array.put(obj);
              tempUniqCode = uniqueCode;
            }
          }
          result.put("list", array);
        } else {

          sqlQuery1 = " select a.uniquecode,a.id from(  select distinct coalesce(em_efin_uniquecode,acctvalue) as uniquecode ,f.account_id as id from fact_acct  f where 1=1  ";
          if (strOrgFamily != null)
            sqlQuery1 += "AND F.AD_ORG_ID IN (" + strOrgFamily + ")";
          if (ClientId != null)
            sqlQuery1 += "AND F.AD_CLIENT_ID IN (" + ClientId + ")";
          if (ClientId != null)
            sqlQuery1 += "AND F.AD_ORG_ID IN (" + OrgId + ")";
          sqlQuery1 += "  AND DATEACCT < TO_DATE(?) AND 1=1  ";
          if (accountId != null)
            sqlQuery1 += "AND F.account_ID='" + accountId + "'";
          if (BpartnerId != null)
            sqlQuery1 += "AND  F.C_BPARTNER_ID IN (" + BpartnerId.replaceFirst(",", "") + ")";
          if (productId != null)
            sqlQuery1 += "AND F.M_PRODUCT_ID IN " + productId;
          if (projectId != null)
            sqlQuery1 += "AND F.C_PROJECT_ID IN (" + projectId.replaceFirst(",", "") + ")";
          if (DeptId != null)
            sqlQuery1 += "AND F.C_SALESREGION_ID IN (" + DeptId.replaceFirst(",", "") + ")";
          if (BudgetTypeId != null)
            sqlQuery1 += "AND coalesce(F.C_CAMPAIGN_ID, (select c.C_CAMPAIGN_ID from C_CAMPAIGN c where em_efin_iscarryforward = 'N' and ad_client_id = F.ad_client_id limit 1))='"
                + BudgetTypeId + "'";
          if (FunclassId != null)
            sqlQuery1 += "AND F.C_ACTIVITY_ID IN (" + FunclassId.replaceFirst(",", "") + ")";
          if (User1Id != null)
            sqlQuery1 += "AND F.USER1_ID IN (" + User1Id.replaceFirst(",", "") + ")";
          if (User2Id != null)
            sqlQuery1 += "AND F.USER2_ID IN (" + User2Id.replaceFirst(",", "") + ")";
          if (AcctschemaId != null)
            sqlQuery1 += "AND F.C_ACCTSCHEMA_ID ='" + AcctschemaId + "'";
          if (uniqueCode != null)
            sqlQuery1 += "		AND (F.EM_EFIN_UNIQUECODE = '" + uniqueCode
                + "' or F.acctvalue='" + uniqueCode + "')";
          sqlQuery1 += " AND F.ISACTIVE='Y' " + "         ) a, c_elementvalue ev "
              + "          where a.id = ev.c_elementvalue_id           and ev.elementlevel = 'S'       ";
          if (strAccountFromValue != null)
            sqlQuery1 += "		AND EV.VALUE >= '" + strAccountFromValue + "'";
          if (strAccountToValue != null)
            sqlQuery1 += "		AND EV.VALUE <= '" + strAccountToValue + "'";
          st = conn.prepareStatement(sqlQuery1);
          st.setString(1, DateTo);
          log4j.debug("Acct PTD:" + st.toString());
          rs = st.executeQuery();
          while (rs.next()) {
            ElementValue type = OBDal.getInstance().get(ElementValue.class, rs.getString("id"));
            for (GLJournalApprovalVO vo : period) {

              if (tempYear != null) {
                if (!tempYear.equals(vo.getStartdate().split("-")[2])) {
                  FrmPerStDate = getPedStrDate(OrgId, ClientId, vo.getId());
                  FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                      .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
                }
              } else {
                tempYear = vo.getStartdate().split("-")[2];
                FrmPerStDate = getPedStrDate(OrgId, ClientId, vo.getId());
                FrmPerStDate = new SimpleDateFormat("dd-MM-yyyy")
                    .format(new SimpleDateFormat("yyyy-MM-dd").parse(FrmPerStDate));
              }
              String tempStartDateFC = strStrDateFC;
              strStrDateFC = selectLastOpeningBalanceDate(OrgId, ClientId, vo.getId());
              if (StringUtils.isNotEmpty(strStrDateFC)) {
                strStrDateFC = new SimpleDateFormat("dd-MM-yyyy")
                    .format(new SimpleDateFormat("yyyy-MM-dd").parse(strStrDateFC));
              } else {
                strStrDateFC = tempStartDateFC;
              }
              List<GLJournalApprovalVO> initial = selectInitialBal(rs.getString("uniquecode"),
                  vo.getStartdate(), FrmPerStDate, strStrDateFC, type.getAccountType(), 1, RoleId);
              if (initial.size() > 0) {
                GLJournalApprovalVO VO = initial.get(0);
                initialDr = VO.getInitDr();
                initialCr = VO.getInitCr();
                initialNet = VO.getInitNet();

              }
              if (tempUniqCode.equals(rs.getString("uniquecode"))) {
                arr = obj.getJSONArray("transaction");
                JSONObject tra = new JSONObject();
                tra.put("startdate", vo.getName());
                tra.put("date", new SimpleDateFormat("MM-dd-yyyy")
                    .format(new SimpleDateFormat("dd-MM-yyyy").parse(FrmPerStDate)));
                tra.put("perDr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                tra.put("perCr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                tra.put("finalpernet",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                tra.put("initialDr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
                tra.put("initialCr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
                tra.put("initialNet",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
                tra.put("finaldr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
                tra.put("finalcr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
                tra.put("finalnet",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
                tra.put("id", accountId);
                tra.put("periodid", vo.getId());
                tra.put("type", "1");
                arr.put(tra);
              } else {
                obj = new JSONObject();
                obj.put("uniquecode", rs.getString("uniquecode"));
                obj.put("accountId", rs.getString("id"));
                arr = new JSONArray();
                JSONObject tra = new JSONObject();
                tra.put("startdate", vo.getName());
                tra.put("date", new SimpleDateFormat("MM-dd-yyyy")
                    .format(new SimpleDateFormat("dd-MM-yyyy").parse(FrmPerStDate)));
                tra.put("perDr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                tra.put("perCr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                tra.put("finalpernet",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, 0));
                tra.put("initialDr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
                tra.put("initialCr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
                tra.put("initialNet",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
                tra.put("finaldr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialDr));
                tra.put("finalcr",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialCr));
                tra.put("finalnet",
                    Utility.getNumberFormat(vars, Utility.numberFormat_PriceRelation, initialNet));
                tra.put("id", rs.getString("id"));
                tra.put("periodid", vo.getId());
                tra.put("type", "1");
                arr.put(tra);
                obj.put("transaction", arr);
                array.put(obj);
                tempUniqCode = rs.getString("uniquecode");
              }
            }
            result.put("list", array);
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception Creating Excel Sheet", e);
    }
    return result;

  }

  /**
   * This method is used to select initial balance
   * 
   * @param acctId
   * @param resultFromDt
   * @param FrmPerDate
   * @param FrmFCDate
   * @param acctType
   * @param resultpos
   * @param RoleId
   * @return
   */
  public List<GLJournalApprovalVO> selectInitialBal(String acctId, String resultFromDt,
      String FrmPerDate, String FrmFCDate, String acctType, int resultpos, String RoleId) {
    String sqlQuery = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    List<GLJournalApprovalVO> list = null;
    try {
      list = new ArrayList<GLJournalApprovalVO>();
      if ((acctType.equals("E") || acctType.equals("R"))) {
        sqlQuery = "    SELECT sum(case when (DATEACCT > TO_DATE(?) and DATEACCT < TO_DATE(?) )"
            + " then F.AMTACCTDR - F.AMTACCTCR else 0 end )as initialamt,"
            + "	sum(case when(DATEACCT > TO_DATE(?) and DATEACCT < TO_DATE(?) )  then F.AMTACCTDR else 0 end ) as initialdr ,"
            + "sum(case when (DATEACCT > TO_DATE(?) and DATEACCT < TO_DATE(?) )  then  F.AMTACCTCR else 0 end ) as initialcr"
            + "    FROM FACT_ACCT f       WHERE 1=1       AND  ( f.EM_EFIN_UNIQUECODE = ?  or f.acctvalue=? ) "
            + " AND F.ACCOUNT_ID IN (select act.c_elementvalue_id from efin_security_rules_act act "
            + "join efin_security_rules ru on ru.efin_security_rules_id=act.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.C_Salesregion_ID in (select dep.C_Salesregion_ID from Efin_Security_Rules_Dept dep "
            + "join efin_security_rules ru on ru.efin_security_rules_id=dep.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.C_Project_ID in (select proj.c_project_id from efin_security_rules_proj proj "
            + "join efin_security_rules ru on ru.efin_security_rules_id=proj.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.C_CAMPAIGN_ID IN(select bud.C_campaign_ID from efin_security_rules_budtype bud "
            + "join efin_security_rules ru on ru.efin_security_rules_id=bud.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.C_Activity_ID in (select act.C_Activity_ID from efin_security_rules_activ act "
            + " join efin_security_rules ru on ru.efin_security_rules_id=act.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.User1_ID in (select fut1.User1_ID from efin_security_rules_fut1 fut1 "
            + " join efin_security_rules ru on ru.efin_security_rules_id=fut1.efin_security_rules_id "
            + " where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.User2_ID in (select fut2.User2_ID from efin_security_rules_fut2 fut2 "
            + " join efin_security_rules ru on ru.efin_security_rules_id=fut2.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') " + "     ";
        st = conn.prepareStatement(sqlQuery);
        st.setString(1, FrmPerDate);
        st.setString(2, resultFromDt);
        st.setString(3, FrmPerDate);
        st.setString(4, resultFromDt);
        st.setString(5, FrmPerDate);
        st.setString(6, resultFromDt);
        st.setString(7, acctId);
        st.setString(8, acctId);
        log4j.debug("st:" + st.toString());

        rs = st.executeQuery();

      } else if (acctType.equals("A") || acctType.equals("L") || acctType.equals("O")
          || acctType.equals("M")) {
        sqlQuery = "    SELECT sum("
            + "case when ((DATEACCT > TO_DATE(?) and DATEACCT < TO_DATE(?)  "
            + "AND f.FACTACCTTYPE not in('O', 'R', 'C') "
            + ") or (dateacct = To_date(?) AND f.factaccttype = 'O' )) "
            + "then F.AMTACCTDR - F.AMTACCTCR else 0 end " + ")as initialamt,"
            + "	sum(case when( (DATEACCT > TO_DATE(?) and DATEACCT < TO_DATE(?) "
            + " AND f.FACTACCTTYPE not in('O', 'R', 'C')) or (dateacct = To_date(?) AND f.factaccttype = 'O' ) )  then F.AMTACCTDR else 0 end )"
            + " as initialdr , " + "sum(case when ((DATEACCT > TO_DATE(?) and DATEACCT < TO_DATE(?)"
            + "AND f.FACTACCTTYPE not in('O', 'R', 'C') ) or (dateacct = To_date(?) AND f.factaccttype = 'O' ) )  then  F.AMTACCTCR else 0 end "
            + ") as initialcr"
            + "    FROM FACT_ACCT f      WHERE 1=1       AND (f.EM_EFIN_UNIQUECODE = ?   or f.acctvalue=? ) "
            + " AND F.ACCOUNT_ID IN (select act.c_elementvalue_id from efin_security_rules_act act "
            + "join efin_security_rules ru on ru.efin_security_rules_id=act.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.C_Salesregion_ID in (select dep.C_Salesregion_ID from Efin_Security_Rules_Dept dep "
            + "join efin_security_rules ru on ru.efin_security_rules_id=dep.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.C_Project_ID in (select proj.c_project_id from efin_security_rules_proj proj "
            + "join efin_security_rules ru on ru.efin_security_rules_id=proj.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.C_CAMPAIGN_ID IN(select bud.C_campaign_ID from efin_security_rules_budtype bud "
            + "join efin_security_rules ru on ru.efin_security_rules_id=bud.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.C_Activity_ID in (select act.C_Activity_ID from efin_security_rules_activ act "
            + " join efin_security_rules ru on ru.efin_security_rules_id=act.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.User1_ID in (select fut1.User1_ID from efin_security_rules_fut1 fut1 "
            + " join efin_security_rules ru on ru.efin_security_rules_id=fut1.efin_security_rules_id "
            + " where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') "
            + "AND F.User2_ID in (select fut2.User2_ID from efin_security_rules_fut2 fut2 "
            + " join efin_security_rules ru on ru.efin_security_rules_id=fut2.efin_security_rules_id "
            + "where ru.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
            + RoleId + "' )and efin_processbutton='Y') ";
        st = conn.prepareStatement(sqlQuery);
        st.setString(1, FrmFCDate);
        st.setString(2, resultFromDt);
        st.setString(3, FrmFCDate);
        st.setString(4, FrmFCDate);
        st.setString(5, resultFromDt);
        st.setString(6, FrmFCDate);
        st.setString(7, FrmFCDate);
        st.setString(8, resultFromDt);
        st.setString(9, FrmFCDate);
        st.setString(10, acctId);
        st.setString(11, acctId);
        log4j.debug("assst:" + st.toString());
        rs = st.executeQuery();
      }

      while (rs.next()) {
        GLJournalApprovalVO VO = new GLJournalApprovalVO();
        VO.setInitNet(rs.getBigDecimal(1));
        VO.setInitDr(rs.getBigDecimal(2));
        VO.setInitCr(rs.getBigDecimal(3));
        list.add(VO);

      }
    } catch (Exception e) {
      log4j.error("Exception in getOrgs()", e);
    }
    return list;
  }

  /**
   * 
   * @param OrgId
   * @param ClientId
   * @param inpcFromPeriodId
   * @return last opening balance date before the start date of from-period
   */

  public String selectLastOpeningBalanceDate(String OrgId, String ClientId,
      String inpcFromPeriodId) {
    String sqlQuery = "", startdate = "";
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      sqlQuery = "                  select max(a.dateacct) as startdate from fact_acct a where a.AD_Org_ID IN("
          + OrgId + ") " + " AND a.AD_Client_ID IN(" + ClientId + ") "
          + "               and  dateacct <= (select startdate from c_period "
          + "               where c_period_id=?) and factaccttype='O' ";
      st = conn.prepareStatement(sqlQuery);
      st.setString(1, inpcFromPeriodId);
      rs = st.executeQuery();
      if (rs.next()) {
        startdate = rs.getString("startdate");
      }
    } catch (Exception e) {
      log4j.error("Exception in selectLastOpeningBalanceDate()", e);
    }
    return startdate;
  }

}
