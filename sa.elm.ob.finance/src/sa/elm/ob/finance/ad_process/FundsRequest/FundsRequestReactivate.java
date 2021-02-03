package sa.elm.ob.finance.ad_process.FundsRequest;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.util.CommonValidations;

/**
 * 
 * @author Divya.J on 03/10/2017
 * 
 */

public class FundsRequestReactivate extends DalBaseProcess {

  /**
   * Funds Request reactivate process
   */
  private static final Logger log = LoggerFactory.getLogger(FundsRequestReactivate.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("entering into funds request reactivate");

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Boolean errorFlag = false;
    try {
      OBContext.setAdminMode();
      String fundsreqId = (String) bundle.getParams().get("Efin_Fundsreq_ID");

      final EFINFundsReq req = OBDal.getInstance().get(EFINFundsReq.class, fundsreqId);

      CommonValidations.checkValidations(fundsreqId, "BudgetDistribution",
          OBContext.getOBContext().getCurrentClient().getId(), "RE", false);

      if (!errorFlag) {
        // reactivate process
        // count = FundsRequestActionDAO.reactivateBudgetInqchanges(conn, fundsreqId);
        req.setDocumentStatus("DR");
        req.setAction("CO");
        OBDal.getInstance().save(request);
        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@EFIN_Process_Success@");
        bundle.setResult(result);
        return;

      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_budget_adj_Linesreact_Failed"));
        bundle.setResult(result);
        return;
      }
    } catch (Exception e) {
      log.error("Exception in FundsRequestReactivate : " + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
      OBDal.getInstance().commitAndClose();
    }
  }

}
