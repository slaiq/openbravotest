package sa.elm.ob.finance.actionHandler.ReportHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncumbranceFundsActionHandler extends ProcessReportActionHandler {
  private static final Logger log4j = LoggerFactory.getLogger(EncumbranceFundsActionHandler.class);

  SimpleDateFormat dateFormat = new SimpleDateFormat(OBPropertiesProvider.getInstance()
      .getOpenbravoProperties().getProperty("dateTimeFormat.java"));
  VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

  @Override
  protected String getPDFFileName(ReportDefinition report, Map<String, Object> parameters,
      ExportType expType, JSONObject params) {
    String cBpartnerID = "";

    /*
     * if (params.has(CBpartnerID)) { try { cBpartnerID = params.getString(CBpartnerID); } catch
     * (JSONException e) { log4j.debug("Param not found in Report Parameters to Print Name" +
     * e.getMessage()); } BusinessPartner bpObj = OBDal.getInstance().get(BusinessPartner.class,
     * cBpartnerID); if (bpObj != null) cBpartnerID = bpObj.getIdentifier() + "-"; }
     */
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
    String BudgetManencum = "";
    String toaccount = "", strFromDate = "", strToDate = "", strView = "";

    try {
      if (jsonContent.has("_params")) {
        JSONObject params = jsonContent.getJSONObject("_params");
        System.out.println(params);
        if (params.has("Efin_Budget_Manencum_ID")) {
          BudgetManencum = params.getString("Efin_Budget_Manencum_ID");
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
    System.out.println(strFromDate);
    System.out.println(strToDate);

    parameters.put("paramUser", objUser.getName());
    parameters.put("paramClient", objClient.getName());
    parameters.put("paramFromDate", strFromDate);
    parameters.put("paramToDate", strToDate);
    parameters.put("paramView", strView);
    parameters.put("paramOrg", objOrg.getName());
    parameters.put("inpADUser", vars.getUser());
    parameters.put("AD_Client_ID", vars.getClient());
    parameters.put("inpRole", vars.getRole());
    parameters.put("inporgid", vars.getOrg());
    parameters.put("CYearID", year);
    parameters.put("EfinBudgetManencum", BudgetManencum);
    parameters.put("CElementValueIDTo", toaccount);
    parameters.put("REPORT_LOCALE", currentLocale);
    log4j.info("Base Design :" + RequestContext.getServletContext().getContextPath());
    parameters.put("basedesign", RequestContext.getServletContext().getContextPath());

  }
}
