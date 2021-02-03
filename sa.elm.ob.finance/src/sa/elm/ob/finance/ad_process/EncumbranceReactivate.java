package sa.elm.ob.finance.ad_process;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
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
public class EncumbranceReactivate implements Process {

  private static final Logger log4j = Logger.getLogger(EncumbranceReactivate.class);
  private OBError obError = new OBError();
  public static final String REACTIVATE = "REACT";
  public static final String APPROVED = "CO";
  public static final String DRAFT = "DR";

  @Override
  public synchronized void execute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      Connection connection = null;
      Boolean isEncumbranceUsed = Boolean.FALSE;
      Boolean isModified = Boolean.FALSE, isPeriodOpen = Boolean.TRUE;
      HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

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

      // Check transaction period is opened or not
      isPeriodOpen = Utility.checkOpenPeriod(encumbrance.getAccountingDate(),
          orgId.equals("0") ? vars.getOrg() : orgId, encumbrance.getClient().getId());
      if (!isPeriodOpen) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
        bundle.setResult(result);
        return;
      }
      // check preclose or closed year validation
      // cost account transaction not allowed for closed or pre closed year
      // Funds account transaction not allowed for closed year
      if (encumbrance.getSalesCampaign().getEfinBudgettype().equals("C")) {
        if (encumbrance.getBudgetInitialization().isPreclose()
            || encumbrance.getBudgetInitialization().getStatus().equals("CL")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Preclose_Reactivate@");
          bundle.setResult(result);
          return;
        }
      } else {
        if (encumbrance.getBudgetInitialization().getStatus().equals("CL")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Preclose_Reactivate@");
          bundle.setResult(result);
          return;
        }
      }

      if (!encumbrance.getDocumentStatus().equals(APPROVED)) {
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

      // Restrict Reactivate after Encumbrance Modification
      isModified = ManualEncumbaranceRevokeDAO.isEncumbranceModifiedCheckAmt(encumbrance);
      if (isModified) {
        obError = OBErrorBuilder.buildMessage(null, "error", "@Efin_Encum_Modified@");
        bundle.setResult(obError);
        return;
      }

      // Restrict Reactivate after Encumbrance Modification
      // isModified = ManualEncumbaranceRevokeDAO.isEncumbranceModified(encumbrance);
      // if (isModified) {
      // obError = OBErrorBuilder.buildMessage(null, "error", "@Efin_Encum_Modified@");
      // bundle.setResult(obError);
      // return;
      // }

      // Delete modification records
      ManualEncumbaranceRevokeDAO.deleteModificationRecords(encumbrance);

      ManualEncumbaranceRevokeDAO.reactivateEncumbrance(encumbrance);
      ManualEncumbaranceSubmitDAO.insertManEncumHistory(OBDal.getInstance().getConnection(),
          clientId, orgId, roleId, userId, strEncumbranceId, "", REACTIVATE, null);

      obError = OBErrorBuilder.buildMessage(null, "success", "@Efin_manencum_reactivate@");
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
