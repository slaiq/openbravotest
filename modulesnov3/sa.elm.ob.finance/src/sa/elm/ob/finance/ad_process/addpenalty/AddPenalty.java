package sa.elm.ob.finance.ad_process.addpenalty;

import java.util.List;

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

import sa.elm.ob.utility.EUTDeflookupsTypeLn;

/**
 * @author Kousalya on 05/04/2019
 */

public class AddPenalty extends DalBaseProcess {

  /**
   * This servlet class was responsible for adding penalties
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(AddPenalty.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();

      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = (String) bundle.getContext().getOrganization();
      int success = 1;
      List<EUTDeflookupsTypeLn> penaltyLkpList = AddPenaltyDAO.getPenaltyLookup(clientId);
      if (penaltyLkpList != null) {
        if (penaltyLkpList.size() > 0) {
          for (EUTDeflookupsTypeLn penalty : penaltyLkpList) {
            boolean penaltyExist = AddPenaltyDAO.checkPenaltyExist(penalty.getCode(), clientId);
            if (!penaltyExist) {
              int insertPenalty = AddPenaltyDAO.insertPenaltyTypeMaintenance(clientId, orgId,
                  penalty);
              if (insertPenalty == 1) {
                success = 1;
              } else if (insertPenalty == 0) {
                success = 0;
                break;
              }
            }
          }
        }
      }
      if (success == 1) {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      } else if (success == 0) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_ProcessFailed@");
        bundle.setResult(result);
        return;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in Add Penalty:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}