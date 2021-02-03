package sa.elm.ob.finance.actionHandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;

public class UniqueCodeFilterProcess extends BaseProcessActionHandler {

  final private static Logger log = Logger.getLogger(UniqueCodeFilterProcess.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      JSONObject jsonRequest = new JSONObject(content);

      // Get the Params value
      log.debug("entering into Unique Code Filter process");
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String orgId = jsonparams.getString("AD_Org_ID");
      final String deptId = jsonparams.getString("C_SalesRegion_ID");
      final String accountId = jsonparams.getString("C_Elementvalue_ID");
      final String projectId = jsonparams.getString("C_Project_ID");
      final String budgTypeId = jsonparams.getString("C_Campaign_ID");
      final String entityId = jsonparams.getString("C_Bpartner_ID");
      final String activityId = jsonparams.getString("C_Activity_ID");
      // final String future1Id = jsonparams.getString("User1_ID");
      // final String future2Id = jsonparams.getString("User2_ID");
      final String isFilterToAccount = jsonparams.optString("IsToAccount", null);
      final String orgDefaultKey = "999";
      final String deptDefaultKey = "01";
      final String acctDefaultKey = "000";
      final String projDefaultKey = "000";
      final String budTypDefaultKey = "000";
      final String entityDefaultKey = "000";
      final String activityDefaultKey = "000";
      // final String future1DefaultKey = "000";
      // final String future2DefaultKey = "000";

      Organization org = null;
      SalesRegion dept = null;
      ElementValue acct = null;
      Project proj = null;
      Campaign budTyp = null;
      BusinessPartner entity = null;
      ABCActivity activity = null;
      // UserDimension1 future1 = null;
      // UserDimension2 future2 = null;

      StringBuffer uniqueCode = new StringBuffer();

      if (!StringUtils.isEmpty(orgId)) {
        org = OBDal.getInstance().get(Organization.class, orgId);
      }

      if (!StringUtils.isEmpty(deptId)) {
        dept = OBDal.getInstance().get(SalesRegion.class, deptId);
      }

      if (!StringUtils.isEmpty(accountId)) {
        acct = OBDal.getInstance().get(ElementValue.class, accountId);
      }

      if (!StringUtils.isEmpty(projectId)) {
        proj = OBDal.getInstance().get(Project.class, projectId);
      }

      if (!StringUtils.isEmpty(budgTypeId)) {
        budTyp = OBDal.getInstance().get(Campaign.class, budgTypeId);
      }

      if (!StringUtils.isEmpty(entityId)) {
        entity = OBDal.getInstance().get(BusinessPartner.class, entityId);
      }

      if (!StringUtils.isEmpty(activityId)) {
        activity = OBDal.getInstance().get(ABCActivity.class, activityId);
      }

      // if (!StringUtils.isEmpty(future1Id)) {
      // future1 = OBDal.getInstance().get(UserDimension1.class, future1Id);
      // }
      //
      // if (!StringUtils.isEmpty(future2Id)) {
      // future2 = OBDal.getInstance().get(UserDimension2.class, future2Id);
      // }

      uniqueCode.append(org != null ? org.getSearchKey() : orgDefaultKey);
      uniqueCode.append("-");
      uniqueCode.append(dept != null ? dept.getSearchKey() : deptDefaultKey);
      uniqueCode.append("-");
      uniqueCode.append(acct != null ? acct.getSearchKey() : acctDefaultKey);
      uniqueCode.append("-");
      uniqueCode.append(proj != null ? proj.getSearchKey() : projDefaultKey);
      uniqueCode.append("-");
      uniqueCode.append(budTyp != null ? budTyp.getSearchKey() : budTypDefaultKey);
      uniqueCode.append("-");
      uniqueCode.append(entity != null ? entity.getSearchKey() : entityDefaultKey);
      uniqueCode.append("-");
      uniqueCode.append(activity != null ? activity.getSearchKey() : activityDefaultKey);
      // uniqueCode.append("-");
      // uniqueCode.append(future1 != null ? future1.getSearchKey() : future1DefaultKey);
      // uniqueCode.append("-");
      // uniqueCode.append(future2 != null ? future2.getSearchKey() : future2DefaultKey);

      JSONArray responseActions = new JSONArray();

      JSONObject filterGridByUniqueCodeAction = new JSONObject();

      JSONObject filterGridByUniqueCodeParam = new JSONObject();
      filterGridByUniqueCodeParam.put("uniqueCode", uniqueCode);

      if (!StringUtils.isEmpty(isFilterToAccount)) {
        filterGridByUniqueCodeParam.put("isFilterToAccount", Boolean.valueOf(isFilterToAccount));
      }

      // Parameters for Independent Filter
      if (org != null) {
        filterGridByUniqueCodeParam.put("org", org.getIdentifier());
      }
      if (dept != null) {
        filterGridByUniqueCodeParam.put("dept", dept.getIdentifier());
      }
      if (acct != null) {
        filterGridByUniqueCodeParam.put("acct", acct.getIdentifier());
      }
      if (proj != null) {
        filterGridByUniqueCodeParam.put("proj", proj.getIdentifier());
      }
      if (budTyp != null) {
        filterGridByUniqueCodeParam.put("budTyp", budTyp.getIdentifier());
      }
      if (entity != null) {
        filterGridByUniqueCodeParam.put("entity", entity.getIdentifier());
      }
      if (activity != null) {
        filterGridByUniqueCodeParam.put("activity", activity.getIdentifier());
      }
      // if (future1 != null) {
      // filterGridByUniqueCodeParam.put("future1", future1.getIdentifier());
      // }
      // if (future2 != null) {
      // filterGridByUniqueCodeParam.put("future2", future2.getIdentifier());
      // }

      filterGridByUniqueCodeAction.put("filterGridByUniqueCode", filterGridByUniqueCodeParam);

      responseActions.put(filterGridByUniqueCodeAction);

      jsonResponse.put("responseActions", responseActions);

    } catch (final Exception e) {
      e.printStackTrace();
      JSONObject errormsg = new JSONObject();
      try {
        errormsg.put("severity", "error");
        errormsg.put("text", errormsg.put("text", "Error while filtering unique code"));
        jsonResponse.put("message", errormsg);
      } catch (JSONException e1) {
        log.error("exception :", e1);
      }
    }
    return jsonResponse;
  }
}
