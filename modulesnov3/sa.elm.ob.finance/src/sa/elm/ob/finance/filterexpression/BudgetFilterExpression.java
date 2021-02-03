package sa.elm.ob.finance.filterexpression;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EfinNonexpenseLines;

public class BudgetFilterExpression implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(BudgetFilterExpression.class);

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    String clientid = "";
    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      strCurrentParam = requestMap.get("currentParam");
      clientid = context.get("inpadClientId").toString();
      log4j.debug("strCurrentParam:" + strCurrentParam);
      log4j.debug("strCurrentParam:" + requestMap);
      log4j.debug("strCurrentParam:" + context.get("inpadOrgId"));
      // JSONObject orgId = context.get("inpadOrgId");
      PreparedStatement ps = null;
      Connection conn = OBDal.getInstance().getConnection();
      ResultSet rs = null;
      String orgId = null;
      String DateQuery = "select eut_convert_to_hijri(to_char(now(),'YYYY-MM-DD')) as DefaultValue";

      // Get the Organization
      if (strCurrentParam.equals("AD_Org_ID")) {
        OBQuery<Organization> org = OBDal.getInstance().createQuery(Organization.class,
            " id <>'0' and client ='" + clientid + "' order by searchKey asc ");
        if (org.list().size() > 0) {
          Organization org1 = org.list().get(0);
          orgId = org1.getId();
          return orgId;
        }
      }
      /*
       * if (strCurrentParam.equals("AD_Org_ID")) return organization;
       */

      if (strCurrentParam.equals("C_SalesRegion_ID")) {
        log4j.debug("C_SalesRegion_ID" + strCurrentParam);
        // OBQuery<SalesRegion> dept = OBDal.getInstance().createQuery(SalesRegion.class,
        // " organization.id ='" + orgId + "' order by searchKey asc ");
        OBQuery<SalesRegion> dept = OBDal.getInstance().createQuery(SalesRegion.class,
            " default ='Y' order by searchKey asc ");
        List<SalesRegion> deptList = dept.list();
        if (deptList.size() > 0) {
          SalesRegion d1 = deptList.get(0);
          return d1.getId();

        }
        /*
         * else if(dept.list().size() == 0) { try { ps =
         * conn.prepareStatement("select eut_parent_org ('" + orgId + "','" + vars.getClient() +
         * "')"); log4j.debug("select:" + ps.toString()); rs = ps.executeQuery(); if(rs.next()) {
         * log4j.debug("eut_parent_org:" + rs.getString("eut_parent_org")); parentOrg =
         * rs.getString("eut_parent_org"); OBQuery<SalesRegion> deptParent =
         * OBDal.getInstance().createQuery(SalesRegion.class, " organization.id in (" + parentOrg +
         * ") order by searchKey asc "); if(deptParent.list().size() > 0) { SalesRegion d1 =
         * deptParent.list().get(0); log4j.debug("d1:" + d1.getId()); return d1.getId();
         * 
         * } }
         * 
         * } catch (SQLException e) { log4j.error("error in BudgetFilterExpression" + e); }
         * 
         * }
         */
      }
      if (strCurrentParam.equals("C_Project_ID")) {
        OBQuery<Project> prj = OBDal.getInstance().createQuery(Project.class,
            " eFINDefault='Y' order by searchKey asc ");
        List<Project> prjList = prj.list();
        if (prjList.size() > 0) {
          Project project = prjList.get(0);
          return project.getId();

        }
      }
      if (strCurrentParam.equals("Efin_Act_Date") || strCurrentParam.equals("Efin_Trx_Date")) {
        try {
          ps = conn.prepareStatement(DateQuery);
          rs = ps.executeQuery();
          if (rs.next()) {
            return rs.getString("DefaultValue");
          }
        } catch (Exception e) {
          log4j.debug("error while getting Date default value:" + e.getMessage());
        }
      }
      if (strCurrentParam.equals("Efin_Act_Date")) {
        try {
          ps = conn.prepareStatement(DateQuery);
          rs = ps.executeQuery();
          if (rs.next()) {
            return rs.getString("DefaultValue");
          }
        } catch (Exception e) {
          log4j.debug("error while getting Date default value:" + e.getMessage());
        }
      }
      if (strCurrentParam.equals("Budget type")) {
        final EFINBudget budgetId = OBDal.getInstance().get(EFINBudget.class,
            context.getString("inpefinBudgetId").toString());
        return budgetId.getSalesCampaign().getId();

      }

      if (strCurrentParam.equals("FunClassification")) {
        OBQuery<ABCActivity> activity = OBDal.getInstance().createQuery(ABCActivity.class,
            " efinIsdefault='Y' order by searchKey asc ");
        List<ABCActivity> activityList = activity.list();
        if (activityList.size() > 0) {
          ABCActivity act = activityList.get(0);
          return act.getId();

        }
      }
      if (strCurrentParam.equals("C_Campaign")) {
        final EfinNonexpenseLines expenseline = OBDal.getInstance().get(EfinNonexpenseLines.class,
            context.getString("inpefinNonexpenseLinesId").toString());
        return expenseline.getSalesCampaign().getId();
      }
      if (strCurrentParam.equals("C_Elementvalue_ID")) {
        OBQuery<ElementValue> element = OBDal.getInstance().createQuery(ElementValue.class,
            "elementLevel='S' order by searchKey asc ");
        List<ElementValue> elementList = element.list();
        if (elementList.size() > 0) {
          ElementValue elevalue = elementList.get(0);
          return elevalue.getId();

        }
      }
      if (strCurrentParam.equals("Future1")) {
        OBQuery<UserDimension1> user1 = OBDal.getInstance().createQuery(UserDimension1.class,
            " efinIsdefault='Y' order by searchKey asc ");
        List<UserDimension1> user1LIst = user1.list();
        if (user1LIst.size() > 0) {
          UserDimension1 usr1 = user1LIst.get(0);
          return usr1.getId();

        }
      }
      if (strCurrentParam.equals("Future2")) {
        OBQuery<UserDimension2> user2 = OBDal.getInstance().createQuery(UserDimension2.class,
            " efinIsdefault='Y' order by searchKey asc ");
        List<UserDimension2> user2List = user2.list();
        if (user2List.size() > 0) {
          UserDimension2 usr2 = user2List.get(0);
          return usr2.getId();

        }
      }
      if (strCurrentParam.equals("BudgType_Read_Only_Logic")) {
        if (context.getString("inpefinBudgetId") != null)
          return "Y";
      }
      if (strCurrentParam.equalsIgnoreCase("dateAcct")) {
        try {
          ps = conn.prepareStatement(DateQuery);
          rs = ps.executeQuery();
          if (rs.next()) {
            return rs.getString("DefaultValue");
          }
        }

        catch (Exception e) {
          log4j.debug("error while getting Date default value:" + e.getMessage());
        }
      }
      if (strCurrentParam.equals("statementDate")) {
        try {
          ps = conn.prepareStatement(DateQuery);
          rs = ps.executeQuery();
          if (rs.next()) {
            return rs.getString("DefaultValue");
          }
        }

        catch (Exception e) {
          log4j.debug("error while getting Date default value:" + e.getMessage());
        }
      }
      if (strCurrentParam.equals("statement_date")) {
        try {
          ps = conn.prepareStatement(DateQuery);
          rs = ps.executeQuery();
          if (rs.next()) {
            return rs.getString("DefaultValue");
          }
        }

        catch (Exception e) {
          log4j.debug("error while getting Date default value:" + e.getMessage());
        }
      }
    } catch (JSONException e) {
      log4j.debug("Error getting the default value of Organization" + strCurrentParam + " "
          + e.getMessage());
      return null;
    }
    return null;
  }
}
