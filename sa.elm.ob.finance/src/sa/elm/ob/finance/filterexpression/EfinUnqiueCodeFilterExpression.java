package sa.elm.ob.finance.filterexpression;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EfinBudgetControlParam;

/**
 * @author Prakash
 * 
 */
public class EfinUnqiueCodeFilterExpression implements FilterExpression {
  private final static Logger log4j = Logger.getLogger(EfinUnqiueCodeFilterExpression.class);

  @Override
  public String getExpression(Map<String, String> requestMap) {
    String strCurrentParam = "";
    String clientid = "";

    try {
      JSONObject context = new JSONObject(requestMap.get("context"));
      strCurrentParam = requestMap.get("currentParam");
      clientid = context.get("inpadClientId").toString();
      String fundsReqId = context.optString("Efin_Fundsreq_ID", " ");

      // Set default org
      if (strCurrentParam.equals("AD_Org_ID")) {
        EfinBudgetControlParam objFC = null;
        OBQuery<EfinBudgetControlParam> budgetcontrolparam = OBDal.getInstance()
            .createQuery(EfinBudgetControlParam.class, " as e where  e.client.id=:clientId");
        budgetcontrolparam.setNamedParameter("clientId", clientid);

        List<EfinBudgetControlParam> budgetcontrolparamList = budgetcontrolparam.list();

        if (budgetcontrolparamList.size() > 0) {
          objFC = budgetcontrolparamList.get(0);
        }
        if (objFC != null) {
          return objFC.getAgencyHqOrg().getId();
        } else {
          return "";
        }
      }

      // Set default dept
      if (strCurrentParam.equals("C_SalesRegion_ID")) {
        return "";
      }

      // Set default Account
      if (strCurrentParam.equals("C_Elementvalue_ID")) {
        OBQuery<AcctSchemaElement> dimension = OBDal.getInstance()
            .createQuery(AcctSchemaElement.class, " ad_client_id = '" + clientid
                + "'  and elementtype='AC'and c_elementvalue_id is not null");

        if (dimension.list().size() > 0) {
          return dimension.list().get(0).getAccountElement().getId();
        }
      }

      // Set default Project
      if (strCurrentParam.equals("C_Project_ID")) {
        OBQuery<AcctSchemaElement> dimension = OBDal.getInstance()
            .createQuery(AcctSchemaElement.class, " ad_client_id = '" + clientid
                + "'  and elementtype='PJ' and c_project_id is not null");

        if (dimension.list().size() > 0) {
          return dimension.list().get(0).getProject().getId();
        }
      }

      // Set default Budget Type
      if (strCurrentParam.equals("C_Campaign_ID")) {
        EFINFundsReq fundsReq = OBDal.getInstance().get(EFINFundsReq.class, fundsReqId);
        if (fundsReq != null) {
          return fundsReq.getSalesCampaign() != null ? fundsReq.getSalesCampaign().getId() : "";
        }
      }

      // Set default Entity
      if (strCurrentParam.equals("C_Bpartner_ID")) {
        OBQuery<AcctSchemaElement> dimension = OBDal.getInstance()
            .createQuery(AcctSchemaElement.class, " ad_client_id = '" + clientid
                + "'  and elementtype='BP' and c_bpartner_id is not null");

        if (dimension.list().size() > 0) {
          return dimension.list().get(0).getBusinessPartner().getId();
        }
      }

      // Set default Functional Classification
      if (strCurrentParam.equals("C_Activity_ID")) {
        OBQuery<AcctSchemaElement> dimension = OBDal.getInstance()
            .createQuery(AcctSchemaElement.class, " ad_client_id = '" + clientid
                + "'  and elementtype='AY' and c_activity_id is not null");

        if (dimension.list().size() > 0) {
          return dimension.list().get(0).getActivity().getId();
        }
      }

      // Set default Future 1
      if (strCurrentParam.equals("User1_ID")) {
        OBQuery<AcctSchemaElement> dimension = OBDal.getInstance()
            .createQuery(AcctSchemaElement.class, " ad_client_id = '" + clientid
                + "'  and elementtype='U1' and em_efin_user1 is not null");

        if (dimension.list().size() > 0) {
          return dimension.list().get(0).getEfinUser1().getId();
        }
      }

      // Set default Future 2
      if (strCurrentParam.equals("User2_ID")) {
        OBQuery<AcctSchemaElement> dimension = OBDal.getInstance()
            .createQuery(AcctSchemaElement.class, " ad_client_id = '" + clientid
                + "'  and elementtype='U2' and em_efin_user2 is not null");

        if (dimension.list().size() > 0) {
          return dimension.list().get(0).getEfinUser2().getId();
        }
      }

    } catch (JSONException e) {
      log4j.debug("Error getting the default value in Filter Unique Code " + strCurrentParam + " "
          + e.getMessage());
      return null;
    }
    return null;
  }
}
