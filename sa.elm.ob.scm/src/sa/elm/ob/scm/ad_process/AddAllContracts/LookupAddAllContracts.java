package sa.elm.ob.scm.ad_process.AddAllContracts;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

/**
 * @author Kiruthika on 22/07/2020
 */

public class LookupAddAllContracts implements Process {

  /**
   * This class is used to add contracts in Role - Lookup Access which are not added already
   */
  private static Logger log = Logger.getLogger(LookupAddAllContracts.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();

      // Variable declaration
      final String roleId = bundle.getParams().get("AD_Role_ID").toString();
      final String userId = bundle.getContext().getUser();
      final String orgId = bundle.getContext().getOrganization();
      final String clientId = bundle.getContext().getClient();

      AddAllContracts dao = new AddAllContractsImpl();

      boolean isSuccess = dao.addAllContracts(roleId, userId, orgId, clientId);

      if (isSuccess) {

        OBError result = OBErrorBuilder.buildMessage(null, "success",
            "@Escm_AddAllContractsSuccess@");
        bundle.setResult(result);
        return;

      } else {
        OBDal.getInstance().rollbackAndClose();
        OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
            vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        bundle.setResult(error);
        return;
      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while LookupAddAllContracts: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in LookupAddAllContracts:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}