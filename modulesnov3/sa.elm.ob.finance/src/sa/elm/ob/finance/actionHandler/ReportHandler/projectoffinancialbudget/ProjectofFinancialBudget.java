package sa.elm.ob.finance.actionHandler.ReportHandler.projectoffinancialbudget;

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
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.actionHandler.ReportHandler.ProcessReportActionHandler;
import sa.elm.ob.utility.util.Utility;

public class ProjectofFinancialBudget extends ProcessReportActionHandler {
  private static final Logger log4j = LoggerFactory.getLogger(ProjectofFinancialBudget.class);

  private static final String budgetYearId = "Efin_BudgetInt_id";
  private static final String chapterNo = "Chapter_No";
  private static final String elementValueIdFrom = "C_ElementValue_Id_From";
  private static final String elementValueIdTo = "C_ElementValue_ID_To";
  SimpleDateFormat dateFormat = new SimpleDateFormat(OBPropertiesProvider.getInstance()
      .getOpenbravoProperties().getProperty("dateTimeFormat.java"));
  VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

  @Override
  protected String getPDFFileName(ReportDefinition report, Map<String, Object> parameters,
      ExportType expType, JSONObject params) {
    return report.getProcessDefintion().getName() + "-" + dateFormat.format(new Date());
  }

  /**
   * 
   * To Pass Extra Parameters to Report
   */
  protected void addAdditionalParameters(ReportDefinition process, JSONObject jsonContent,
      Map<String, Object> parameters) {
    String inpBudgetYearId = "";
    String inpChapterNo = "";
    String inpFromAccountId = "", inpToAccountId = "";
    EfinBudgetIntialization budInit = null;
    ElementValue elemValue = null;
    try {
      if (jsonContent.has("_params")) {
        JSONObject params = jsonContent.getJSONObject("_params");

        if (params.has(budgetYearId)) {
          inpBudgetYearId = params.getString(budgetYearId);
          budInit = Utility.getObject(EfinBudgetIntialization.class, inpBudgetYearId);
        }
        if (params.has(chapterNo)) {
          inpChapterNo = params.getString(chapterNo);
          elemValue = Utility.getObject(ElementValue.class, inpChapterNo);
        }
        if (params.has(elementValueIdFrom)) {
          inpFromAccountId = params.getString(elementValueIdFrom);
        }
        if (params.has(elementValueIdTo)) {
          inpToAccountId = params.getString(elementValueIdTo);
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

    parameters.put("inpClientId", vars.getClient());
    parameters.put("inpOrgId", vars.getOrg());
    parameters.put("inpCYearId", inpBudgetYearId);
    parameters.put("inpCYearName", budInit != null ? budInit.getCommercialName() : "");
    parameters.put("inpChapterNo", inpChapterNo == null ? "" : inpChapterNo);
    parameters.put("inpChapterValue", elemValue == null ? "" : elemValue.getSearchKey());
    parameters.put("inpCElementValueIdFrom", inpFromAccountId == null ? "" : inpFromAccountId);
    parameters.put("inpCElementValueIdTo", inpToAccountId == null ? "" : inpToAccountId);
    parameters.put("REPORT_LOCALE", currentLocale);
    log4j.debug("Base Design :" + RequestContext.getServletContext().getContextPath());
    parameters.put("basedesign", RequestContext.getServletContext().getContextPath());
  }
}