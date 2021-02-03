package sa.elm.ob.finance.charts.BudgetSummary;

import java.sql.Connection;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.client.myob.WidgetInstance;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.marketing.Campaign;

import sa.elm.ob.finance.EFINBudgetLines;

/**
 * @author Gopalakrishnan on 20/09/2016
 */

public class BudgetSummaryDAO {
  Connection conn = null;
  private static Logger log4j = Logger.getLogger(BudgetSummaryDAO.class);

  public BudgetSummaryDAO(Connection con) {
    this.conn = con;
  }

  /**
   * 
   * @param client
   * @param org
   * @param yearId
   * @param bTypeId
   * @param elementId
   * @param AccountId
   * @param projectId
   * @return Budget Details
   */
  public List<EFINBudgetLines> getBudgetDetails(String client, String org, String yearId,
      String bTypeId, String elementId, String AccountId, String projectId) {
    String strWhereclause = "";
    OBQuery<EFINBudgetLines> budglines = null;
    try {
      OBContext.setAdminMode(true);
      strWhereclause = "as e where e.organization.id='" + org + "' and e.efinBudget.year.id='"
          + yearId + "' " + "and e.salesCampaign.id='" + bTypeId + "' and e.accountElement.id='"
          + AccountId + "' and e.project.id='" + projectId + "' and e.efinBudget.alertStatus='APP'";
      budglines = OBDal.getInstance().createQuery(EFINBudgetLines.class, strWhereclause);
    } catch (Exception e) {
      log4j.error("Exception in getBudgetDetails() ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return budglines.list();
  }

  public List<EFINBudgetLines> getBudgetDetailsForAll(String client, String org, String yearId,
      String bTypeId) {
    String strWhereclause = "";
    OBQuery<EFINBudgetLines> budglines = null;
    try {
      OBContext.setAdminMode(true);
      strWhereclause = "as e where e.organization.id='" + org + "' and e.efinBudget.year.id='"
          + yearId + "' and e.salesCampaign.id='" + bTypeId
          + "' and e.efinBudget.alertStatus='APP'";
      budglines = OBDal.getInstance().createQuery(EFINBudgetLines.class, strWhereclause);
    } catch (Exception e) {
      log4j.error("Exception in getBudgetDetails() ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return budglines.list();
  }

  /**
   * 
   * @param ClientId
   * @return OrganizationList
   */
  public List<Organization> getOrganization(String ClientId) {
    OBQuery<Organization> orgList = null;
    try {
      orgList = OBDal.getInstance().createQuery(Organization.class,
          "as e where e.client.id='" + ClientId + "'");
      orgList.setFilterOnReadableOrganization(false);
    } catch (Exception e) {
      log4j.debug("Exception getOrganization :" + e);
    }
    return orgList.list();
  }

  /**
   * 
   * @param clientId
   * @return yearList
   */
  public List<Year> getYear(String clientId) {
    OBQuery<Year> yearList = null;
    try {
      yearList = OBDal.getInstance().createQuery(Year.class,
          "as e where e.client.id='" + clientId + "'");
    } catch (Exception e) {
      log4j.debug("Exception in getYear :" + e);
    }
    return yearList.list();
  }

  /**
   * 
   * @param clientId
   * @return BudgetTypeList
   */
  public List<Campaign> getBudgetType(String clientId) {
    OBQuery<Campaign> bTypeList = null;
    try {
      bTypeList = OBDal.getInstance().createQuery(Campaign.class,
          "as e where e.client.id='" + clientId + "'");
    } catch (Exception e) {
      log4j.debug("Exception in getBudgetType :" + e);
    }
    return bTypeList.list();
  }

  public List<WidgetInstance> getPreferences(String id, String role) {

    String strWhereclause = "";
    OBQuery<WidgetInstance> wInstanceList = null;
    try {
      OBContext.setAdminMode(true);
      strWhereclause = "as e where e.widgetClass.id='777A6C1D55644AF482912F804513460C' and e.visibleAtUser='"
          + id + "' and e.visibleAtRole='" + role + "'";
      wInstanceList = OBDal.getInstance().createQuery(WidgetInstance.class, strWhereclause);
    } catch (Exception e) {
      log4j.error("Exception in getBudgetDetails() ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return wInstanceList.list();
  }

  /**
   * 
   * @param budgetTypeId
   * @return Accounting Elements
   */
  public JSONArray getAccountElement(String budgetTypeId) {
    JSONArray jArray = new JSONArray();
    SQLQuery query = null;
    StringBuilder sqlBuilder = null;
    JSONObject jsonObject = null;
    try {
      OBContext.setAdminMode();

      sqlBuilder = new StringBuilder();
      sqlBuilder
          .append(" select C_Elementvalue_Id, (value || '-' || name ) as name from C_Elementvalue where C_Elementvalue.C_Elementvalue_ID in(select line.C_Elementvalue_ID  from efin_budgettype_acct line join C_Campaign on C_Campaign.C_Campaign_ID= line.C_Campaign_ID  ");
      sqlBuilder
          .append(" left join C_Acctschema_element ase on ase.C_Acctschema_id = C_Campaign.em_efin_c_acctschema_id and ase.elementtype='AC' "
              + " where C_Campaign.C_Campaign_ID='"
              + budgetTypeId
              + "' and ase.isactive='Y') and C_Elementvalue.issummary='Y'");

      log4j.debug("Sql Query :" + sqlBuilder.toString());

      query = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());

      if (query != null && query.list().size() > 0) {
        for (int i = 0; i < query.list().size(); i++) {
          Object[] objects = (Object[]) query.list().get(i);

          jsonObject = new JSONObject();
          jsonObject.put("ElementId", objects[0].toString());
          jsonObject.put("ElementName", objects[1].toString());
          jArray.put(jsonObject);
        }
      }
    } catch (Exception e) {
      log4j.debug("Exception in getAccountElement :" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jArray;
  }

  /**
   * 
   * 
   * @param ElementId
   * @return accounts List
   */
  public JSONArray getAccounts(String ElementId) {
    JSONArray jArray = new JSONArray();
    SQLQuery query = null;
    StringBuilder sqlBuilder = null;
    JSONObject jsonObject = null;
    try {
      OBContext.setAdminMode();

      sqlBuilder = new StringBuilder();
      sqlBuilder
          .append("select C_Elementvalue_Id, (value || '-' || name ) as name from C_Elementvalue where C_Elementvalue.C_Elementvalue_ID in(select node_id from ad_treenode  ");
      sqlBuilder.append("where parent_id='" + ElementId
          + "')   and C_Elementvalue.elementlevel='S' and C_Elementvalue.IsActive='Y' ");

      log4j.debug("Sql Query :" + sqlBuilder.toString());

      query = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());

      if (query != null && query.list().size() > 0) {
        for (int i = 0; i < query.list().size(); i++) {
          Object[] objects = (Object[]) query.list().get(i);

          jsonObject = new JSONObject();
          jsonObject.put("ElementId", objects[0].toString());
          jsonObject.put("ElementName", objects[1].toString());
          jArray.put(jsonObject);
        }
      }
    } catch (Exception e) {
      log4j.debug("Exception in getAccounts :" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jArray;
  }

  /**
   * 
   * 
   * @param accountId
   * @param orgId
   * @param ClientId
   * @return Project List
   */
  public JSONArray getProjects(String accountId, String orgId, String ClientId) {
    JSONArray jArray = new JSONArray();
    SQLQuery query = null;
    StringBuilder sqlBuilder = null;
    JSONObject jsonObject = null;
    try {
      OBContext.setAdminMode();

      sqlBuilder = new StringBuilder();
      sqlBuilder
          .append("select C_Project_Id ,(value||'-'||name) as name from C_Project  where C_Project.C_Project_ID = (select case when em_efin_projacct = 'Y' then (select case when em_efin_project_id is null ");
      sqlBuilder
          .append(" then null else em_efin_project_id end from c_elementvalue join c_project pro on pro.c_project_id=em_efin_project_id where c_elementvalue_id ='"
              + accountId + "' and pro.ad_org_id='" + orgId + "') ");
      sqlBuilder
          .append(" else (select case when em_efin_project_id is not null then null else (select c_project_id from c_project where em_efin_isdefault = 'Y' and ad_client_id ='"
              + ClientId + "') ");
      sqlBuilder
          .append(" end from c_elementvalue  where c_elementvalue_id ='"
              + accountId
              + "') end from c_elementvalue  where c_elementvalue_id = (select parent_id from ad_treenode where node_id = '"
              + accountId + "')) ");
      log4j.debug("Sql Query :" + sqlBuilder.toString());
      query = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());

      if (query != null && query.list().size() > 0) {
        for (int i = 0; i < query.list().size(); i++) {
          Object[] objects = (Object[]) query.list().get(i);

          jsonObject = new JSONObject();
          jsonObject.put("ProjectId", objects[0].toString());
          jsonObject.put("ProjectName", objects[1].toString());
          jArray.put(jsonObject);
        }
      }
    } catch (Exception e) {
      log4j.debug("Exception in getAccounts :" + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jArray;
  }

  public void saveDefaultPreferences(String user, String role, String preferences) {
    SQLQuery query = null;
    StringBuilder sqlBuilder = null;
    try {

      sqlBuilder = new StringBuilder();
      sqlBuilder
          .append("UPDATE obkmo_widget_instance SET em_eut_preferences ='"
              + preferences
              + "' WHERE obkmo_widget_class_id = '777A6C1D55644AF482912F804513460C' AND visibleat_user_id = '"
              + user + "' AND visibleat_role_id = '" + role + "';");
      log4j.debug("Sql Query :" + sqlBuilder.toString());
      query = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());
      query.executeUpdate();

    } catch (Exception e) {
      log4j.debug("Exception in getAccounts :" + e);
    }
  }
}