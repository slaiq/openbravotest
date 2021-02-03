package sa.elm.ob.scm.ad_process.mms;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.scm.webservice.approvedpo.CreatePoReceiptService;
import sa.elm.ob.scm.webservice.approvedpo.CreatePoReceiptServiceImpl;
import sa.elm.ob.scm.webservice.constants.WebserviceConstants;

public class MmsDataProcess extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(MmsDataProcess.class);
  private ProcessLogger logger;

  @SuppressWarnings("unused")
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = null;
    logger = bundle.getLogger();

    try {
      OBContext.setAdminMode();
      logger.logln("MMS Data Process Schedule Started.");

      String alertWindow = AlertWindow.MMSScript;
      List<Preference> alertRoles = null;
      String clientId = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty(WebserviceConstants.CLIENT_KEY);

      CreatePoReceiptService service = new CreatePoReceiptServiceImpl();
      // Initialize variables
      vars = service.intializeVars(clientId);

      JSONObject jsonResult = MmsDataProcessDAO.processMMSData(vars);
      if (jsonResult.getBoolean("status")) {
        logger.logln("log success:" + jsonResult.get("message").toString());
        OBError result = OBErrorBuilder.buildMessage(null, "success",
            jsonResult.get("message").toString());
        bundle.setResult(result);
        return;
      } else {
        logger.logln("log error:" + jsonResult.get("message").toString());
        MmsDataProcessDAO.insertAlertsforMMS(jsonResult, alertWindow, clientId);
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            jsonResult.get("message").toString());
        bundle.setResult(result);
        return;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in MMS Data process:", e);
      logger.logln("Exeception in MMS Data process:" + e);

      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}