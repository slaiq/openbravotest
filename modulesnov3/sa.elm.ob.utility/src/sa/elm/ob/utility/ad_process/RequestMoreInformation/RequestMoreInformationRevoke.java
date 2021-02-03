package sa.elm.ob.utility.ad_process.RequestMoreInformation;

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
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.PrimaryKeyDocumentTypeE;
import sa.elm.ob.utility.util.Utility;

public class RequestMoreInformationRevoke implements Process {
  private static final Logger log = Logger.getLogger(BidManagementRevoke.class);
  private final OBError obError = new OBError();

  public void execute(ProcessBundle bundle) throws Exception {
    final String clientId = (String) bundle.getContext().getClient();
    String comments = (String) bundle.getParams().get("comments").toString();
    // final String orgId = bid.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();

    EutForwardReqMoreInfo forReqMoreInfo = null;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    // String alertRuleId = "";
    // ArrayList<String> includeRecipient = new ArrayList<String>();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User usr = Utility.getObject(User.class, vars.getUser());
    String Lang = vars.getLanguage();
    Requisition req = null;
    EscmProposalMgmt proMgmt = null;
    MaterialIssueRequest mir = null;
    Invoice purchaseinv = null;
    String alertWindowType = null, type = null, doctype = null;
    String inpwindowId = vars.getStringParameter("inpwindowId");
    Window windowObj = Utility.getObject(Window.class, inpwindowId);
    String windowName = windowObj.getName();

    String windowReference = null, inpRecordId = null;
    // String Lang = vars.getLangge();
    // String Description = "", lastWaitingRoleId = "";
    try {
      OBContext.setAdminMode();

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
      doctype = e.getStrDocumentType();
      windowReference = e.getwindowReference();
      alertWindowType = e.getAlertWindowType();
      alertWindowType = e.getAlertWindowType();

      forReqMoreInfo = forwardReqMoreInfoDAO.getRmiIdReqRes(inpRecordId, windowReference);
      EutForwardReqMoreInfo rmi = forwardReqMoreInfoDAO.getReqMoreInfo(inpRecordId,
          windowReference);
      boolean isRMIRevokeUser = forwardReqMoreInfoDAO.checkRMIRevokeUser(forReqMoreInfo, userId,
          roleId, doctype);

      if ((forReqMoreInfo != null && forReqMoreInfo.getREQResponse().equals("RES"))
          || !isRMIRevokeUser || rmi == null) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved"));
        bundle.setResult(obError);
        return;
      }
      // Update record status as 'DR'
      forReqMoreInfo = forwardReqMoreInfoDAO.revokeForwardUpdateRecordRMI(windowReference, vars,
          inpRecordId, comments, true, doctype, false, Constants.REQUEST_MORE_INFORMATION_REVOKE);
      // Delete forwarded EutNextRoleLine
      forwardReqMoreInfoDAO.revokeForwardDeleteEutNextRoleLineRMI(request, forReqMoreInfo, vars,
          inpRecordId, windowReference);
      // Insert Record in Action History for Revoke
      forwardReqMoreInfoDAO.insertActionHistoryRMIRevoke(request, vars, inpRecordId,
          windowReference, Constants.REQUEST_MORE_INFORMATION_REVOKE, forReqMoreInfo);

      // Remove Role Access to Receiver
      // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId, forReqMoreInfo.getId(), conn);

      // Remove Forward_Rmi id from transaction screens
      forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(inpRecordId, windowReference);

      // alert process
      String UserName = usr.getName();
      forwardReqMoreInfoDAO.alertprocess(clientId, alertWindowType, forReqMoreInfo,
          Constants.REQUEST_MORE_INFORMATION_REVOKE, Lang, UserName, windowName, doctype, false,
          request, windowReference);

      EutForwardReqMoreInfo previousRmiReqObj = forwardReqMoreInfoDAO
          .getPreviousRMIRecordBasedOnRequestId(vars.getUser(), vars.getRole(), forReqMoreInfo,
              doctype, true);
      if (previousRmiReqObj != null
          && !previousRmiReqObj.getRequest().equals(forReqMoreInfo.getId())) {
        forwardReqMoreInfoDAO.insertEutNextRoleLineRMI(request, vars, inpRecordId,
            previousRmiReqObj, windowReference);

      }
      // inserting the rmi alert for user who revoke the forward request now
      if (previousRmiReqObj != null) {
        ForwardRequestMoreInfoDAOImpl.insertForward_Rmialert(clientId, alertWindowType,
            previousRmiReqObj, Lang, windowName, false);
      }
      // Find Original alert coming from approval cycle(exclude Frwd and RMI)

      List<Alert> originalList = forwardReqMoreInfoDAO.findOriginalAlert(inpRecordId,
          forwardReqMoreInfoDAO.getNextRole(inpRecordId, windowReference), windowReference, false);

      for (Alert alert : originalList) {
        alert.setAlertStatus("NEW");
        OBDal.getInstance().save(alert);
      }

      OBDal.getInstance().flush();
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_RMIRevoke_Sucess@");
      bundle.setResult(result);
      return;

    } catch (Exception e1) {
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in Request More Information Revoke :", e1);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
