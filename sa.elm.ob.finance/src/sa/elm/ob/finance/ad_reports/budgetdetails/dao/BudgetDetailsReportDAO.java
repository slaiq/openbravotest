package sa.elm.ob.finance.ad_reports.budgetdetails.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;

import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.finance.ad_reports.budgetdetails.vo.BudgetDetailsReportVO;
import sa.elm.ob.utility.util.Utility;

public class BudgetDetailsReportDAO {

  @SuppressWarnings("unused")
  private Connection conn = null;
  VariablesSecureApp vars = null;
  private static Logger log4j = Logger.getLogger(BudgetDetailsReportDAO.class);
  BudgetDetailsReportDAO DAO = null;

  public BudgetDetailsReportDAO(Connection con, VariablesSecureApp vars) {
    this.conn = con;
    this.vars = vars;
  }

  /**
   * 
   * @param vars
   * @return
   */
  // List<BudgetDetailsReportVO>
  @SuppressWarnings("rawtypes")
  public List<BudgetDetailsReportVO> getOrgList(VariablesSecureApp vars) {
    // JSONArray jsonArray = new JSONArray();
    SQLQuery query = null;
    List<BudgetDetailsReportVO> orgList = new ArrayList<BudgetDetailsReportVO>();
    try {
      OBContext.setAdminMode();
      Client client = OBDal.getInstance().get(Client.class, vars.getClient());

      StringBuilder orgQuery = new StringBuilder();
      BudgetDetailsReportVO vo = null;
      orgQuery.append("SELECT o.ad_org_id,concat(o.value,' - ',o.name) AS name"
          + "  FROM ad_org o JOIN ad_orgtype ot ON o.ad_orgtype_id=ot.ad_orgtype_id"
          + "  WHERE isready  ='Y' AND istransactionsallowed='Y' AND o.ad_client_id in('0', :clentID) OR o.ad_org_id='0'");
      query = OBDal.getInstance().getSession().createSQLQuery(orgQuery.toString());
      query.setParameter("clentID", client.getId());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] rows = (Object[]) iterator.next();
          vo = new BudgetDetailsReportVO();
          vo.setOrgId(Utility.nullToEmpty(rows[0]));
          vo.setOrgName(Utility.nullToEmpty(rows[1]));
          orgList.add(vo);
        }

      }

      return orgList;

    } catch (final Exception e) {
      log4j.error("Exception in getting organization list :", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return orgList;

  }

  /**
   * 
   * @param vars
   * @return
   */
  @SuppressWarnings("rawtypes")
  public List<BudgetDetailsReportVO> gettingYearList(VariablesSecureApp vars) {
    // JSONArray jsonArray = new JSONArray();
    SQLQuery query = null;
    List<BudgetDetailsReportVO> yearList = new ArrayList<BudgetDetailsReportVO>();
    try {
      OBContext.setAdminMode();
      Client client = OBDal.getInstance().get(Client.class, vars.getClient());

      StringBuilder yearQuery = new StringBuilder();
      BudgetDetailsReportVO vo = null;
      yearQuery.append(
          "select  bugdef.efin_budgetint_id,(bugdef.name||'-'||yr.year) as name from efin_budgetint bugdef "
              + "left join c_year yr on yr.c_year_id= bugdef.c_year_id "
              + "where bugdef.ad_client_id= :clientID and bugdef.ad_org_id = '0' order by year");
      log4j.debug("yearQuery" + yearQuery);

      query = OBDal.getInstance().getSession().createSQLQuery(yearQuery.toString());
      query.setParameter("clientID", client.getId());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] rows = (Object[]) iterator.next();
          vo = new BudgetDetailsReportVO();
          vo.setYearId(Utility.nullToEmpty(rows[0]));

          vo.setYearName(Utility.nullToEmpty(rows[1]));

          yearList.add(vo);
        }

      }

      return yearList;

    } catch (final Exception e) {
      log4j.error("Exception in getting year list :", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return yearList;

  }

  /**
   * 
   * @param vars
   * @return
   */
  @SuppressWarnings("rawtypes")
  public List<BudgetDetailsReportVO> gettingTypeList(VariablesSecureApp vars) {
    // JSONArray jsonArray = new JSONArray();
    SQLQuery query = null;
    List<BudgetDetailsReportVO> typeList = new ArrayList<BudgetDetailsReportVO>();
    try {
      OBContext.setAdminMode();
      StringBuilder typeQuery = new StringBuilder();
      Client client = OBDal.getInstance().get(Client.class, vars.getClient());
      BudgetDetailsReportVO votyp = null;
      typeQuery.append(
          "select c_campaign_id,concat(value,' - ',name) as name from c_campaign where ad_client_id= :clientID "
              + " AND C_CAMPAIGN_ID IN(select bud.C_campaign_ID from efin_security_rules_budtype bud "
              + " join efin_security_rules ru on ru.efin_security_rules_id=bud.efin_security_rules_id "
              + " where ru.efin_security_rules_id=(select em_efin_security_rules_id "
              + " from ad_role where ad_role_id= :roleID )and efin_processbutton='Y') "
              + " order by name ");

      query = OBDal.getInstance().getSession().createSQLQuery(typeQuery.toString());
      query.setParameter("clientID", client.getId());
      query.setParameter("roleID", vars.getRole());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] rows = (Object[]) iterator.next();
          votyp = new BudgetDetailsReportVO();
          votyp.setBudgetTypeId(Utility.nullToEmpty(rows[0]));
          votyp.setBudgetTypeName(Utility.nullToEmpty(rows[1]));
          typeList.add(votyp);
        }

      }
      return typeList;

    } catch (final Exception e) {
      log4j.error("Exception in getting Type List :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return typeList;
  }

  /**
   * 
   * @param vars
   * @param inpBudgetTypeId
   * @return
   */
  @SuppressWarnings("rawtypes")
  public List<BudgetDetailsReportVO> gettingGroupList(VariablesSecureApp vars,
      String inpBudgetTypeId) {
    // JSONArray jsonArray = new JSONArray();
    SQLQuery query = null;
    List<BudgetDetailsReportVO> groupList = new ArrayList<BudgetDetailsReportVO>();

    OBContext.setAdminMode();
    StringBuilder grpQuery = new StringBuilder();

    try {
      BudgetDetailsReportVO vogrp = null;

      String TypId = inpBudgetTypeId;
      log4j.info(TypId + inpBudgetTypeId);
      grpQuery.append(
          "select acct.c_elementvalue_id,concat(typ.value,' - ',typ.name,' - ',list.name) as groupName from efin_budgettype_acct  acct  join c_elementvalue typ on acct.c_elementvalue_id = typ.c_elementvalue_id "
              + " join ad_ref_list list on list.value=typ.accounttype and list.ad_reference_id ='117' where acct.c_campaign_id='"
              + TypId + "' order by groupName");

      query = OBDal.getInstance().getSession().createSQLQuery(grpQuery.toString());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] rows = (Object[]) iterator.next();
          vogrp = new BudgetDetailsReportVO();
          vogrp.setBudgetGroupId(Utility.nullToEmpty(rows[0]));
          vogrp.setBudgetGroupName(Utility.nullToEmpty(rows[1]));
          groupList.add(vogrp);
        }

      }

      return groupList;
    } catch (Exception e) {
      log4j.error("Exception in  Getting Group List : ", e);
    } finally {
      OBContext.restorePreviousMode();

    }
    return groupList;

  }

  /**
   * 
   * @param vars
   * @return
   */
  @SuppressWarnings("rawtypes")
  public String getCurrentYear(VariablesSecureApp vars) {
    // JSONArray jsonArray = new JSONArray();
    SQLQuery query = null;
    String yearId = null;
    OBContext.setAdminMode();
    StringBuilder curYrQry = new StringBuilder();

    try {

      Client client = OBDal.getInstance().get(Client.class, vars.getClient());

      curYrQry.append(
          "select c_year.c_year_id,year from c_period join c_year on c_year.c_year_id = c_period.c_year_id where to_date(to_char(now(),'dd-MM-yyyy'),'dd-MM-yyyy')"
              + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy')"
              + " and c_year.ad_client_id='" + client.getId() + "' and c_year.ad_org_id='0'");

      query = OBDal.getInstance().getSession().createSQLQuery(curYrQry.toString());

      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] rows = (Object[]) iterator.next();
          yearId = Utility.nullToEmpty(rows[0]);
        }
      }

      return yearId;
    } catch (Exception e) {
      log4j.error("Exception in  setting current year List : ", e);
    } finally {
      OBContext.restorePreviousMode();

    }
    return yearId;

  }

  /**
   * 
   * @param orgId
   * @param vars
   * @return
   */
  // To get parent organization of selected org.
  @SuppressWarnings("rawtypes")
  public String getParentOrg(String orgId, VariablesSecureApp vars) {
    SQLQuery query = null;
    String OrgparentId = null;
    OBContext.setAdminMode();
    StringBuilder orgParentQry = new StringBuilder();
    try {
      Client client = OBDal.getInstance().get(Client.class, vars.getClient());
      orgParentQry
          .append("select eut_parent_org('" + orgId + "','" + client.getId() + "') as parents ");
      query = OBDal.getInstance().getSession().createSQLQuery(orgParentQry.toString());
      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          OrgparentId = (String) iterator.next();
        }
      }
      return OrgparentId;
    } catch (Exception e) {
      log4j.error("Exception in  getting parent organization : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return OrgparentId;
  }

  /**
   * 
   * @param vars
   * @param inpOrgId
   * @return
   */
  @SuppressWarnings("rawtypes")
  public List<BudgetDetailsReportVO> getCostCenterDeptList(VariablesSecureApp vars,
      String inpOrgId) {
    // JSONArray jsonArray = new JSONArray();
    SQLQuery query = null;
    List<BudgetDetailsReportVO> deptList = new ArrayList<BudgetDetailsReportVO>();
    try {
      OBContext.setAdminMode();
      Client client = OBDal.getInstance().get(Client.class, vars.getClient());

      StringBuilder deptQuery = new StringBuilder();
      BudgetDetailsReportVO vo = null;
      String OrgId = inpOrgId;
      EfinBudgetControlParam bcp = FundsReqMangementDAO.getControlParam(client.getId());
      String hgOrg = bcp.getAgencyHqOrg().getId();
      String bcudept = bcp.getBudgetcontrolunit().getId();
      String bccostcenter = bcp.getBudgetcontrolCostcenter().getId();
      if (OrgId.equals(hgOrg)) {
        deptQuery.append(
            "select c_salesregion_id,concat(value,' - ',name) as deptname from c_salesregion where ad_client_id='"
                + client.getId() + "' and ad_org_id ='" + OrgId + "' or c_salesregion_id = '"
                + bcudept + "' or c_salesregion_id = '" + bccostcenter + "' order by value");
        log4j.debug("deptQuery" + deptQuery);

      } else if (OrgId.equals("0")) {
        deptQuery.append(
            "select c_salesregion_id,concat(value,' - ',name) as deptname from c_salesregion where ad_client_id='"
                + client.getId() + "' order by value");
        log4j.debug("deptQuery" + deptQuery);
      } else {
        deptQuery.append(
            "select c_salesregion_id,concat(value,' - ',name) as deptname from c_salesregion  where ad_client_id='"
                + client.getId() + "' and ad_org_id ='" + OrgId + "' or c_salesregion_id = '"
                + bccostcenter + "' order by value");
        log4j.debug("deptQuery" + deptQuery);
      }
      query = OBDal.getInstance().getSession().createSQLQuery(deptQuery.toString());

      if (query != null && query.list().size() > 0) {
        for (Iterator iterator = query.list().iterator(); iterator.hasNext();) {
          Object[] rows = (Object[]) iterator.next();
          vo = new BudgetDetailsReportVO();
          vo.setDeptId(Utility.nullToEmpty(rows[0]));

          vo.setDeptName(Utility.nullToEmpty(rows[1]));

          deptList.add(vo);
        }

      }

      return deptList;

    } catch (final Exception e) {
      log4j.error("Exception in getting cost center dept list :", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return deptList;

  }

}