package sa.elm.ob.utility.ad_process.Forward;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.actionHandler.BidManagementRevoke;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.PrimaryKeyDocumentTypeE;

public class ForwardRevoke implements Process {
  private static final Logger log = Logger.getLogger(BidManagementRevoke.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {
    String windowReference = null, alertWindowType = null, inpRecordId = null;
    String docType = null, type = null;
    EutForwardReqMoreInfo forReqMoreInfo = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();
      final String clientId = (String) bundle.getContext().getClient();
      String comments = (String) bundle.getParams().get("comments").toString();
      // final String orgId = bid.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
      User usr = OBDal.getInstance().get(User.class, vars.getUser());
      String Lang = vars.getLanguage();
      Requisition req = null;
      EscmProposalMgmt proMgmt = null;
      MaterialIssueRequest mir = null;
      Invoice purchaseinv = null;
      Boolean isRevoke = false;
      String inpwindowId = vars.getStringParameter("inpwindowId");
      Window windowObj = OBDal.getInstance().get(Window.class, inpwindowId);
      String windowName = windowObj.getName();
      PrimaryKeyDocumentTypeE e = PrimaryKeyDocumentTypeE.getWindowType(inpwindowId);
      String primaryKeyColumn = e.getPrimaryKeyColumnName();
      inpRecordId = (String) bundle.getParams().get(primaryKeyColumn).toString();
      EFINFundsReq fundsReqMgmt = null;
      // logic for the windows which have more than one document type
      if (inpwindowId.equals(Constants.PURCHASE_REQUISITION_W)) {
        req = OBDal.getInstance().get(Requisition.class, inpRecordId);
        type = req.getEscmProcesstype();
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      } else if (inpwindowId.equals(Constants.PROPOSAL_MANAGEMENT_W)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        type = proMgmt.getProposalType();
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      } else if (inpwindowId.equals(Constants.MATERIAL_ISSUE_REQUEST_W)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        type = mir.getEscmDocumenttype();
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      } else if (inpwindowId.equals(Constants.FUNDS_REQ_MGMT_W)) {
        fundsReqMgmt = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
        type = fundsReqMgmt.getTransactionType();
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      } else if (inpwindowId.equals(Constants.PURCHASE_INVOICE_W)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
        if (purchaseinv.getTransactionDocument().isEfinIsprepayinv())
          type = "PPI";
        else if (purchaseinv.getTransactionDocument().isEfinIsprepayinvapp())
          type = "PPA";
        else
          type = "API";
        e = PrimaryKeyDocumentTypeE.getWindowTypeSpecialCase(type);
      }
      docType = e.getStrDocumentType();
      windowReference = e.getwindowReference();
      alertWindowType = e.getAlertWindowType();

      EutForwardReqMoreInfo forwardObj = forwardReqMoreInfoDAO.getForwardObj(inpRecordId,
          windowReference);
      if (forwardObj == null) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved"));
        bundle.setResult(obError);
        return;
      }
      // check current role and user can forward revoke or not
      isRevoke = forwardReqMoreInfoDAO.isCurrentRoleandUserCanRevoke(forwardObj.getId(), userId,
          roleId, docType);
      if (!isRevoke) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved"));
        bundle.setResult(obError);
        return;
      }

      EutForwardReqMoreInfo rmi = forwardReqMoreInfoDAO.getReqMoreInfo(inpRecordId,
          windowReference);
      if (rmi != null) {
        // Update record status as 'DR'
        forReqMoreInfo = forwardReqMoreInfoDAO.revokeForwardUpdateRecordRMI(windowReference, vars,
            inpRecordId, null, false, docType, false, Constants.FORWARD_REVOKE);

        forwardReqMoreInfoDAO.revokeForwardDeleteEutNextRoleLineRMI(request, forReqMoreInfo, vars,
            inpRecordId, windowReference);

        forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(inpRecordId, windowReference);
      }
      // Update record status as 'DR'
      forReqMoreInfo = forwardReqMoreInfoDAO.revokeForwardUpdateRecord(windowReference, vars,
          inpRecordId, comments, false);
      // Delete forwarded EutNextRoleLine
      forwardReqMoreInfoDAO.revokeForwardDeleteEutNextRoleLine(request, forReqMoreInfo, vars,
          inpRecordId, windowReference);
      // Insert Record in Action History for Revoke
      forwardReqMoreInfoDAO.insertActionHistory(request, vars, inpRecordId, windowReference,
          Constants.FORWARD_REVOKE, forReqMoreInfo, false);

      // Remove Role Access from Receiver
      // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forReqMoreInfo.getId(), conn);
      // Remove Forward_Rmi id from transaction screens
      forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(inpRecordId, windowReference);

      // alert process
      String UserName = usr.getName();
      forwardReqMoreInfoDAO.alertprocess(clientId, alertWindowType, forReqMoreInfo,
          Constants.FORWARD_REVOKE, Lang, UserName, windowName, docType, false, request,
          windowReference);

      // set previous forward_id in transaction screens // for revoke chain changes
      EutForwardReqMoreInfo previousForwardObj = forwardReqMoreInfoDAO
          .getPreviousForwardObj(forReqMoreInfo.getRequest(), userId, roleId, docType);
      if (previousForwardObj != null
          && !previousForwardObj.getRequest().equals(forReqMoreInfo.getId())) {
        EutNextRoleLine line = forwardReqMoreInfoDAO.insertEutNextRoleLine(request, vars,
            inpRecordId, previousForwardObj, windowReference);

      }

      // inserting the forward alert for user who revoke the forward request now
      if (previousForwardObj != null) {
        ForwardRequestMoreInfoDAOImpl.insertForward_Rmialert(clientId, alertWindowType,
            previousForwardObj, Lang, windowName, true);
      }
      // Find Original alert coming from approval cycle(exclude Frwd and RMI)

      List<Alert> originalList = forwardReqMoreInfoDAO.findOriginalAlert(inpRecordId,
          forwardReqMoreInfoDAO.getNextRole(inpRecordId, windowReference), windowReference, true);

      for (Alert alert : originalList) {
        alert.setAlertStatus("NEW");
        OBDal.getInstance().save(alert);
      }

      OBDal.getInstance().flush();

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_ForwardRevoke_Sucess@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception While ForwardRevoke :", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
