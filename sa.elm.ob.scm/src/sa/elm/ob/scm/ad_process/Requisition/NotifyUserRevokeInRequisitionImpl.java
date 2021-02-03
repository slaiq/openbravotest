package sa.elm.ob.scm.ad_process.Requisition;

import java.util.ArrayList;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.procurement.Requisition;
import org.apache.log4j.Logger;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author DivyaPrakash JS
 * @since 2019-07-22
 *
 */

public class NotifyUserRevokeInRequisitionImpl implements NotifyUserRevokeInRequisitionDAO {
  private static final Logger log4j = Logger.getLogger(NotifyUserRevokeInRequisitionImpl.class);

  @Override
  public Boolean isPurchseReqCreatedForBid(String requisitionID) {
    List<Escmbidsourceref> bidSourceRefList = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidsourceref> bidSourceRefObj = OBDal.getInstance()
          .createQuery(Escmbidsourceref.class, " as e where e.requisition.id= :requisitionID ");
      bidSourceRefObj.setNamedParameter("requisitionID", requisitionID);
      bidSourceRefList = bidSourceRefObj.list();
      if (bidSourceRefList.size() > 0) {
        for (Escmbidsourceref bidsourceref : bidSourceRefList) {
          Escmbidmgmtline bidmgmtline = OBDal.getInstance().get(Escmbidmgmtline.class,
              bidsourceref.getEscmBidmgmtLine().getId());
          if (!bidmgmtline.getEscmBidmgmt().getBidstatus().equals("CL")) {
            return true;
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isPurchseReqCreatedForBid : " + e);
      OBDal.getInstance().rollbackAndClose();
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  @Override
  public void alertProcess(String status, String notifyUser, Requisition objRequisition,
      String alertWindow) {
    List<Alert> alertList = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      // Solving Old alert
      OBQuery<Alert> alertObj = OBDal.getInstance().createQuery(Alert.class,
          " as e where e.alertStatus= :status and e.referenceSearchKey= :requisitionID and e.userContact.id= :notifiedUserID");
      alertObj.setNamedParameter("status", status);
      alertObj.setNamedParameter("requisitionID", objRequisition.getId());
      alertObj.setNamedParameter("notifiedUserID", notifyUser);
      alertList = alertObj.list();
      if (alertList.size() > 0) {
        alertList.forEach(alert -> {
          Alert alertoj = OBDal.getInstance().get(Alert.class, alert.getId());
          alertoj.setAlertStatus("SOLVED");
          OBDal.getInstance().save(alertoj);
        });
      }
    } catch (Exception e) {
      log4j.error("Exception in alertProcess : " + e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void insertActionHistory(Requisition objRequisition, String currentRoleId,
      String currentUserId) {
    try {
      OBContext.setAdminMode();
      // Insert Action history
      JSONObject historyData = new JSONObject();
      historyData.put("ClientId", objRequisition.getClient().getId());
      historyData.put("OrgId", objRequisition.getOrganization().getId());
      historyData.put("RoleId", currentRoleId);
      historyData.put("UserId", currentUserId);
      historyData.put("HeaderId", objRequisition.getId());
      historyData.put("Comments", OBMessageUtils.messageBD("Escm_Pr_NotifyUserRevoke"));
      historyData.put("Status", "NR");
      historyData.put("NextApprover", "");
      historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
      historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
      historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);
      Utility.InsertApprovalHistory(historyData);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error("Exception in insertActionHistory : " + e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
