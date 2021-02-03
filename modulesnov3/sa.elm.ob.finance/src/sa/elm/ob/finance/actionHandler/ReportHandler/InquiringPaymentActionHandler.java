package sa.elm.ob.finance.actionHandler.ReportHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InquiringPaymentActionHandler extends ProcessReportActionHandler {
  private static final Logger log4j = LoggerFactory.getLogger(InquiringPaymentActionHandler.class);

  private static final String CElementValueId = "C_Elementvalue_id";
  private static final String CbpartnerId = "C_Bpartner_id";
  private static final String str_fromDate = "Efin_FromDate";
  private static final String str_toDate = "Efin_ToDate";
  SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy-MM-dd");

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
    String fromaccount = "";
    String bpartner = "", strFromDate = "", strToDate = "", strView = "";

    try {
      if (jsonContent.has("_params")) {
        JSONObject params = jsonContent.getJSONObject("_params");

        if (params.has(CElementValueId)) {
          fromaccount = params.getString(CElementValueId);
          System.out.println("FromAccount " + fromaccount);

          // C_ElementValue_id
        }
        if (params.has(CbpartnerId)) {
          bpartner = params.getString(CbpartnerId);
          System.out.println("CbpartnerId " + bpartner);
        }
        if (params.has(str_fromDate)) {
          strFromDate = (StringUtils.isNotBlank(params.getString(str_fromDate))
              && StringUtils.isNotEmpty(params.getString(str_fromDate)))
                  ? params.getString(str_fromDate) : "";

          // String conveted = Utility.convertTohijriDate(DateUtils.convertDateToString(, null));
        }
        if (params.has(str_toDate)) {
          System.out.println(strToDate);

          strToDate = (StringUtils.isNotBlank(params.getString(str_toDate))
              && StringUtils.isNotEmpty(params.getString(str_toDate)))
                  ? params.getString(str_toDate) : "";
          System.out.println(strToDate);

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
    // ElementValue objAccount = OBDal.getInstance().get(ElementValue.class, fromaccount);
    // System.out.println(objAccount.getSearchKey());
    parameters.put("paramFromDate", strFromDate);
    parameters.put("paramToDate", strToDate);
    parameters.put("paramUser", objUser.getName());
    parameters.put("paramClient", objClient.getName());
    parameters.put("paramView", strView);
    parameters.put("paramOrg", objOrg.getName());
    parameters.put("inpADUser", vars.getUser());
    parameters.put("AD_Client_ID", vars.getClient());
    parameters.put("inpRole", vars.getRole());
    parameters.put("inporgid", vars.getOrg());
    parameters.put("CYearID", year);
    parameters.put("accoutparam", fromaccount);
    parameters.put("bpartnerparam", bpartner);
    // parameters.put("accountnumber", objAccount.getSearchKey());
    parameters.put("REPORT_LOCALE", currentLocale);
    log4j.info("Base Design :" + RequestContext.getServletContext().getContextPath());
    parameters.put("basedesign", RequestContext.getServletContext().getContextPath());

  }
}