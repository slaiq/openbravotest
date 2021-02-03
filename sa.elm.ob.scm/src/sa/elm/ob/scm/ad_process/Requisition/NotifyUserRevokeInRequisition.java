package sa.elm.ob.scm.ad_process.Requisition;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.springframework.util.StringUtils;

import sa.elm.ob.scm.util.AlertWindow;

/**
 * 
 * @author Divyaprakash JS
 * @since 2019-07-22
 *
 */
public class NotifyUserRevokeInRequisition implements Process {
  private static final Logger log4j = Logger.getLogger(NotifyUserRevokeInRequisition.class);

  /**
   * This class is used to perform revoke notify user process in purchase requisition
   */
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();

      String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
      Requisition objRequisition = OBDal.getInstance().get(Requisition.class, strRequisitionId);

      String currentRoleId = OBContext.getOBContext().getRole().getId();
      String currentUserId = OBContext.getOBContext().getUser().getId();
      String alertWindow = AlertWindow.PurchaseRequisition;
      String notifyUser = !StringUtils.isEmpty(objRequisition.getEscmNotifyuser())
          ? objRequisition.getEscmNotifyuser()
          : "";
      String status = "NEW";

      NotifyUserRevokeInRequisitionDAO dao = new NotifyUserRevokeInRequisitionImpl();

      // check bid management is created for requisition
      Boolean isPurchseReqCreatedForBid = dao.isPurchseReqCreatedForBid(strRequisitionId);
      if (isPurchseReqCreatedForBid) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_PurReq_BidMgmtExits@");
        bundle.setResult(result);
        return;
      }

      // Notified user Revoke process changes
      if (!objRequisition.isEscmSendnotification()) {
        objRequisition.setEscmSendnotification(true);
        objRequisition.setEscmNotifyuser(null);
        OBDal.getInstance().save(objRequisition);
      }
      OBDal.getInstance().flush();
      // Alert process
      dao.alertProcess(status, notifyUser, objRequisition, alertWindow);

      // Insert Action History
      dao.insertActionHistory(objRequisition, currentRoleId, currentUserId);

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Pr_NotifyUserRevoke@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      log4j.error("Exception in NotifyUserRevokeInRequisition : " + e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
