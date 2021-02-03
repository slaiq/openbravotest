package sa.elm.ob.finance.ad_process;

import java.sql.Connection;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceRevokeDAO;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopinagh. R
 * 
 */

public class EncumbranceCancelProcess implements Process {

  private static final Logger log4j = Logger.getLogger(EncumbranceCancelProcess.class);
  private OBError obError = new OBError();
  public static final String CANCEL = "CA";

  @Override
  public synchronized void execute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      Connection connection = null;
      Boolean isEncumbranceUsed = Boolean.FALSE;

      try {
        ConnectionProvider provider = bundle.getConnection();
        connection = provider.getConnection();
      } catch (NoConnectionAvailableException e) {
        log4j.error("No Database Connection Available.Exception:" + e);
        throw new RuntimeException(e);
      }

      final String strEncumbranceId = (String) bundle.getParams().get("Efin_Budget_Manencum_ID");
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = (String) bundle.getContext().getOrganization();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();

      EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
          strEncumbranceId);

      if (encumbrance.getDocumentStatus().equals(CANCEL)) {
        obError = OBErrorBuilder.buildMessage(null, "error", "@Efin_Revoke_Error@");
        bundle.setResult(obError);
        return;
      }

      EncumbranceCancellationImpl cancellationImpl = new EncumbranceCancellationImpl(connection);
      isEncumbranceUsed = cancellationImpl.isTransactedEncumbrance(encumbrance);

      if (isEncumbranceUsed) {
        obError = OBErrorBuilder.buildMessage(null, "error", "@efin_document_trx@");
        bundle.setResult(obError);
        return;
      }

      ManualEncumbaranceRevokeDAO.cancelEncumbrance(encumbrance);
      ManualEncumbaranceSubmitDAO.insertManEncumHistory(OBDal.getInstance().getConnection(),
          clientId, orgId, roleId, userId, strEncumbranceId, "", CANCEL, null);

      obError = OBErrorBuilder.buildMessage(null, "success", "@Efin_ManEncum_Cancel@");
      bundle.setResult(obError);

      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {

      log4j.error("Exception in EncumbranceCancelProcess: " + e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
