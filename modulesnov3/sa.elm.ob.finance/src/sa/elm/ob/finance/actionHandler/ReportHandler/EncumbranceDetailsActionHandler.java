package sa.elm.ob.finance.actionHandler.ReportHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncumbranceDetailsActionHandler extends ProcessReportActionHandler {
  private static final Logger log4j = LoggerFactory
      .getLogger(EncumbranceDetailsActionHandler.class);
  private static final String CBpartnerID = "C_Bpartner_ID";

  private static final String CYearID = "Efin_BudgetInt_id";
  private static final String CCampaignID = "C_Campaign_ID";
  private static final String CElementValueIdFrom = "C_ElementValue_Id_From";
  private static final String CElementValueIDTo = "C_ElementValue_ID_To";
  private static final String str_stage = "Stage";
  private static final String str_method = "Method";
  private static final String str_type = "EncumbranceType";
  SimpleDateFormat dateFormat = new SimpleDateFormat(OBPropertiesProvider.getInstance()
      .getOpenbravoProperties().getProperty("dateTimeFormat.java"));
  VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

  @Override
  protected String getPDFFileName(ReportDefinition report, Map<String, Object> parameters,
      ExportType expType, JSONObject params) {
    String cBpartnerID = "";

    if (params.has(CBpartnerID)) {
      try {
        cBpartnerID = params.getString(CBpartnerID);
      } catch (JSONException e) {
        log4j.debug("Param not found in Report Parameters to Print Name" + e.getMessage());
      }
      BusinessPartner bpObj = OBDal.getInstance().get(BusinessPartner.class, cBpartnerID);
      if (bpObj != null)
        cBpartnerID = bpObj.getIdentifier() + "-";
    }
    return cBpartnerID + report.getProcessDefintion().getName() + "-"
        + dateFormat.format(new Date());
  }

  /**
   * 
   * To Pass Extra Parameters to Report
   */
  protected void addAdditionalParameters(ReportDefinition process, JSONObject jsonContent,
      Map<String, Object> parameters) {
    String year = "";
    String campaign = "";
    String fromaccount = "";
    String toaccount = "", strstage = "", strtype = "", strmethod = "";

    try {
      if (jsonContent.has("_params")) {
        JSONObject params = jsonContent.getJSONObject("_params");
        if (params.has(CYearID)) {
          year = params.getString(CYearID);
        }
        if (params.has(CCampaignID)) {
          campaign = params.getString(CCampaignID);
        }
        if (params.has(CElementValueIdFrom)) {
          fromaccount = params.getString(CElementValueIdFrom);
        }
        if (params.has(CElementValueIDTo)) {
          toaccount = params.getString(CElementValueIDTo);
        }

        if (params.has(str_stage)) {
          strstage = (StringUtils.isNotBlank(params.getString(str_stage))
              && StringUtils.isNotEmpty(params.getString(str_stage))) ? params.getString(str_stage)
                  : "";
        }
        if (params.has(str_method)) {
          strmethod = (StringUtils.isNotBlank(params.getString(str_method))
              && StringUtils.isNotEmpty(params.getString(str_method)))
                  ? params.getString(str_method)
                  : "";
        }
        if (params.has(str_type)) {
          strtype = (StringUtils.isNotBlank(params.getString(str_type))
              && StringUtils.isNotEmpty(params.getString(str_type))) ? params.getString(str_type)
                  : "";
        }

      }
    } catch (JSONException e) {
      log4j.debug("AdditionalParam not found in Report Parameters" + e.getMessage());
      e.printStackTrace();
    }
    String strLanguage = vars.getLanguage();
    log4j.debug("strLanguage" + strLanguage);
    Locale currentLocale = new Locale(strLanguage.split("_")[0].toString(),
        strLanguage.split("_")[1].toString());
    log4j.debug("currentLocale" + currentLocale);
    org.openbravo.model.ad.access.User objUser = OBDal.getInstance()
        .get(org.openbravo.model.ad.access.User.class, vars.getUser());
    Client objClient = OBDal.getInstance().get(Client.class, vars.getClient());
    org.openbravo.model.common.enterprise.Organization objOrg = OBDal.getInstance()
        .get(org.openbravo.model.common.enterprise.Organization.class, vars.getOrg());
    parameters.put("paramUser", objUser.getName());
    parameters.put("paramClient", objClient.getName());
    parameters.put("paramType", strtype);
    parameters.put("paramMethod", strmethod);
    parameters.put("paramStage", strstage);
    parameters.put("paramOrg", objOrg.getName());
    parameters.put("inpADUser", vars.getUser());
    parameters.put("AD_Client_ID", vars.getClient());
    parameters.put("inpRole", vars.getRole());
    parameters.put("inporgid", vars.getOrg());
    parameters.put("CYearID", year);
    parameters.put("CCampaignID", campaign);
    parameters.put("CElementValueIdFrom", fromaccount);
    parameters.put("CElementValueIDTo", toaccount);
    parameters.put("REPORT_LOCALE", currentLocale);
    log4j.info("Base Design :" + RequestContext.getServletContext().getContextPath());
    parameters.put("basedesign", RequestContext.getServletContext().getContextPath());
  }
}