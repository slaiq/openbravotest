package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;

/**
 * @author Gopalakrishnan on 03/07/2017
 */

public class BudgetAddLineDAO {
  /**
   * This Access Layer class is responsible to do database operation in BudgetAddLineProcess process
   * Class
   */
  private Connection conn = null;
  VariablesSecureApp vars = null;

  public BudgetAddLineDAO(Connection conn) {
    this.conn = conn;
  }

  private final Logger log = LoggerFactory.getLogger(BudgetAddLineDAO.class);

  /**
   * 
   * @param strBudgetId
   * @param strCampaignId
   * @param jsonparams
   * @param lineNo
   * @return if record inserted successfully then true else false
   */
  public boolean isNonProjectAccountInsert(String strBudgetId, String strCampaignId,
      JSONObject jsonparams, long lineNo) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    long lineno = lineNo;

    EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, strBudgetId);
    try {
      OBContext.setAdminMode();
      BigDecimal amount = BigDecimal.ZERO;
      ArrayList<String> uniqueCodeList = new ArrayList<String>();
      // frame exitsing line list
      for (EFINBudgetLines objLines : budget.getEFINBudgetLinesList()) {
        uniqueCodeList.add(objLines.getUniquecode());
      }
      final String orgId = jsonparams.getString("AD_Org_ID");
      final String deptId = jsonparams.getString("C_SalesRegion_ID");
      final String budgTypeId = jsonparams.getString("Budget type");
      final String projectId = jsonparams.getString("C_Project_ID");
      final String activityId = (jsonparams.getString("FunClassification") == "null" ? "0"
          : (jsonparams.getString("FunClassification") == null ? null
              : jsonparams.getString("FunClassification")));
      final String future1 = (jsonparams.getString("Future1") == "null" ? "0"
          : (jsonparams.getString("Future1") == null ? null : jsonparams.getString("Future1")));
      final String future2 = (jsonparams.getString("Future2") == "null" ? "0"
          : (jsonparams.getString("Future1") == null ? null : jsonparams.getString("Future2")));
      query = "select el.c_elementvalue_id ,org.ad_org_id,dep.c_salesregion_id, "
          + "pro.c_project_id,fc.c_activity_id,u1.user1_id,u2.user2_id, "
          + " (org.value ||'-'||dep.value ||'-'|| el.value ||'-'||btype.value||'-'||pro.value||'-'||fc.value||'-'||u1.value||'-'||u2.value) "
          + " as uniquecode " + " from c_elementvalue  el "
          + " left join ad_org org on org.ad_org_id=? "
          + " left join c_salesregion dep on dep.c_salesregion_id=? "
          + " left join c_project pro on pro.c_project_id=?"
          + " left join c_activity fc on fc.c_activity_id=? "
          + " left join  user1 u1 on u1.user1_id=? " + " left join  user2 u2 on u2.user2_id=? "
          + " left join c_campaign btype on btype.c_campaign_id=? " + " where el.ad_client_id=?"
          + "and el.c_elementvalue_id in ( "
          + " select replace(unnest(string_to_array(eut_getchildacct(?),',')::character varying []),'''',''))";

      if (query != null) {
        ps = conn.prepareStatement(query);
        ps.setString(1, orgId);
        ps.setString(2, deptId);
        ps.setString(3, projectId);
        ps.setString(4, activityId);
        ps.setString(5, future1);
        ps.setString(6, future2);
        ps.setString(7, budgTypeId);
        ps.setString(8, budget.getClient().getId());
        ps.setString(9, budget.getAccountElement().getId());
        rs = ps.executeQuery();

        while (rs.next()) {
          if (!uniqueCodeList.contains(rs.getString("uniquecode"))) {
            EFINBudgetLines objBudgetLines = OBProvider.getInstance().get(EFINBudgetLines.class);
            objBudgetLines.setUniquecode(rs.getString("uniquecode"));
            objBudgetLines
                .setProject(OBDal.getInstance().get(Project.class, rs.getString("c_project_id")));
            objBudgetLines.setClient(budget.getClient());
            objBudgetLines.setOrganization(OBDal.getInstance().get(Organization.class, orgId));
            objBudgetLines.setAccountElement(
                OBDal.getInstance().get(ElementValue.class, rs.getString("c_elementvalue_id")));
            objBudgetLines.setAmount(amount);
            objBudgetLines.setLineNo(lineno);
            objBudgetLines.setEfinBudget(budget);
            objBudgetLines.setActivity(
                OBDal.getInstance().get(ABCActivity.class, rs.getString("c_activity_id")));
            objBudgetLines.setStDimension(
                OBDal.getInstance().get(UserDimension1.class, rs.getString("user1_id")));
            objBudgetLines.setNdDimension(
                OBDal.getInstance().get(UserDimension2.class, rs.getString("user2_id")));
            objBudgetLines.setSalesRegion(
                OBDal.getInstance().get(SalesRegion.class, rs.getString("c_salesregion_id")));
            objBudgetLines.setSalesCampaign(OBDal.getInstance().get(Campaign.class, budgTypeId));
            OBDal.getInstance().save(objBudgetLines);
            lineno = lineno + 10;
          }

        }
      }
      if (lineno == 10) {
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      log.error("Exception in isNonProjectAccountInsert " + e.getMessage());
      return false;
    } finally {

      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
  }

  /**
   * 
   * @param strBudgetId
   * @param strCampaignId
   * @param jsonparams
   * @param lineNo
   * @param strDepartment
   * @param strProject
   * @param strUser1
   * @param strUser2
   * @param strFunctionalCls
   * @return if record inserted successfully then true else false
   */

  public boolean isProjectAccountInsert(String strBudgetId, String strCampaignId,
      JSONObject jsonparams, long lineNo, String strDepartment, String strProject, String strUser1,
      String strUser2, String strFunctionalCls) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    long lineno = lineNo;

    EFINBudget budget = OBDal.getInstance().get(EFINBudget.class, strBudgetId);
    try {
      OBContext.setAdminMode();
      BigDecimal amount = BigDecimal.ZERO;
      ArrayList<String> uniqueCodeList = new ArrayList<String>();
      final String orgId = jsonparams.getString("AD_Org_ID");
      final String budgTypeId = jsonparams.getString("Budget type");
      // frame exitsing line list
      for (EFINBudgetLines objLines : budget.getEFINBudgetLinesList()) {
        uniqueCodeList.add(objLines.getUniquecode());
      }
      query = " select el.c_elementvalue_id ,org.ad_org_id,dep.c_salesregion_id, "
          + " pro.c_project_id,fc.c_activity_id,u1.user1_id,u2.user2_id,  "
          + " (org.value ||'-'||dep.value ||'-'|| el.value ||'-'||btype.value||'-'||pro.value||'-'||fc.value||'-'||u1.value||'-'||u2.value) "
          + "as uniquecode " + " from c_elementvalue  el "
          + "  left join ad_org org on org.ad_org_id=?  "
          + " left join c_salesregion dep on dep.c_salesregion_id in (" + strDepartment + ")"
          + " join c_project pro on pro.c_project_id in (" + strProject + ") "
          + " left join c_activity fc on fc.c_activity_id in (" + strFunctionalCls + ") "
          + "  left join  user1 u1 on u1.user1_id in (" + strUser1 + ")"
          + " left join  user2 u2 on u2.user2_id in (" + strUser2 + ") "
          + "left join c_campaign btype on btype.c_campaign_id=?" + "  where el.ad_client_id=? "
          + "and el.c_elementvalue_id in ( "
          + " select replace(unnest(string_to_array(eut_getchildacct(?),',')::character varying []),'''','')); ";
      if (query != null) {
        ps = conn.prepareStatement(query);
        ps.setString(1, orgId);
        ps.setString(2, budgTypeId);
        ps.setString(3, budget.getClient().getId());
        ps.setString(4, budget.getAccountElement().getId());
        log.debug("add lines query:" + ps.toString());
        rs = ps.executeQuery();

        while (rs.next()) {
          if (!uniqueCodeList.contains(rs.getString("uniquecode"))) {
            EFINBudgetLines objBudgetLines = OBProvider.getInstance().get(EFINBudgetLines.class);
            objBudgetLines.setUniquecode(rs.getString("uniquecode"));
            objBudgetLines
                .setProject(OBDal.getInstance().get(Project.class, rs.getString("c_project_id")));
            objBudgetLines.setClient(budget.getClient());
            objBudgetLines.setOrganization(OBDal.getInstance().get(Organization.class, orgId));
            objBudgetLines.setAccountElement(
                OBDal.getInstance().get(ElementValue.class, rs.getString("c_elementvalue_id")));
            objBudgetLines.setAmount(amount);
            objBudgetLines.setLineNo(lineno);
            objBudgetLines.setEfinBudget(budget);
            objBudgetLines.setActivity(
                OBDal.getInstance().get(ABCActivity.class, rs.getString("c_activity_id")));
            objBudgetLines.setStDimension(
                OBDal.getInstance().get(UserDimension1.class, rs.getString("user1_id")));
            objBudgetLines.setNdDimension(
                OBDal.getInstance().get(UserDimension2.class, rs.getString("user2_id")));
            objBudgetLines.setSalesRegion(
                OBDal.getInstance().get(SalesRegion.class, rs.getString("c_salesregion_id")));
            objBudgetLines.setSalesCampaign(OBDal.getInstance().get(Campaign.class, budgTypeId));
            OBDal.getInstance().save(objBudgetLines);
            lineno = lineno + 10;
          }

        }
      }
      if (lineno == 10) {
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Exception in isProjectAccountInsert " + e.getMessage());
      return false;
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
  }
}
