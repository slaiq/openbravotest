package sa.elm.ob.scm.ad_process.CustodyTransfer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.EutNextRoleLine;

public class CustodyTransferDAO {
  private static final Logger log = LoggerFactory.getLogger(CustodyTransferDAO.class);

  public static boolean isCtDirectApproval(ShipmentInOut ctransfer, String userId) {
    boolean isDirectApp = false;
    try {
      OBContext.setAdminMode();
      int ctLevel = ctransfer.getEscmCtapplevel().intValue();
      if (ctLevel == 1 && (ctransfer.getEscmCtsender().getId().equals(userId)
          || ctransfer.getCreatedBy().getId().equals(userId))) {
        isDirectApp = true;
      } else if (ctLevel == 2 && ctransfer.getEscmCtsendlinemng().getId().equals(userId)) {
        isDirectApp = true;
      } else if (ctLevel == 3 && ctransfer.getEscmCtreceiver().getId().equals(userId)) {
        isDirectApp = true;
      } else if (ctLevel == 4 && ctransfer.getEscmCtreclinemng().getId().equals(userId)) {
        isDirectApp = true;
      }

    } catch (Exception e) {
      log.error("Error in isCtDirectApproval ", e);
      return false;
    } finally {
      // OBContext.restorePreviousMode();
    }
    return isDirectApp;
  }

  public static boolean isCtDelegated(ShipmentInOut ctransfer, String userId, String roleId,
      Date delegationDate, String documentType) {
    boolean delegate = false;
    StringBuffer query = null;
    Query chkdelegate = null;
    EutNextRoleLine nextRoleLnObj = null;
    List<EutNextRoleLine> nextRoleLine = new ArrayList<EutNextRoleLine>();
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      int ctLevel = ctransfer.getEscmCtapplevel().intValue();
      String approver = null;
      if (ctLevel == 1) {
        approver = ctransfer.getEscmCtsender().getId();
      } else if (ctLevel == 2) {
        approver = ctransfer.getEscmCtsendlinemng().getId();
      } else if (ctLevel == 3) {
        approver = ctransfer.getEscmCtreceiver().getId();
      } else {
        approver = ctransfer.getEscmCtreclinemng().getId();
      }
      if (ctransfer.getEutForward() != null) {
        OBQuery<EutNextRoleLine> nextRoleln = OBDal.getInstance().createQuery(EutNextRoleLine.class,
            " as e where e.eUTNextRole.id=:eutNextRoleId and  e.eUTForwardReqmoreinfo.id=:forwardID ");
        nextRoleln.setNamedParameter("eutNextRoleId", ctransfer.getEutNextRole());
        nextRoleln.setNamedParameter("forwardID", ctransfer.getEutForward());
        nextRoleln.setMaxResult(1);
        nextRoleLine = nextRoleln.list();
        if (nextRoleLine.size() > 0) {
          nextRoleLnObj = nextRoleLine.get(0);
          approver = nextRoleLnObj.getUserContact().getId();
        }
      }

      query.append("select dll.role.id from Eut_Docapp_Delegateln dll "
          + "      join dll.eUTDocappDelegate dl "
          + "      where dl.fromDate <=:currentDate and dl.date >=:currentDate and dll.documentType=:docType and dll.role.id=:currentRoleId and dl.processed='Y' "
          + "and dll.userContact.id=:currentUserId and dl.userContact.id=:nextapproverId");
      chkdelegate = OBDal.getInstance().getSession().createQuery(query.toString());
      chkdelegate.setParameter("currentDate", delegationDate);
      chkdelegate.setParameter("currentRoleId", roleId);
      chkdelegate.setParameter("currentUserId", userId);
      chkdelegate.setParameter("nextapproverId", approver);
      chkdelegate.setParameter("docType", documentType);

      if (chkdelegate.list().size() > 0) {
        delegate = true;
      }

    } catch (Exception e) {
      log.error("Error in isCtDelegated ", e);
      return false;
    } finally {
      // OBContext.restorePreviousMode();
    }
    return delegate;
  }

}
