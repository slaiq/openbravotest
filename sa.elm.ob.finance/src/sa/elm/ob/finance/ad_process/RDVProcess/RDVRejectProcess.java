package sa.elm.ob.finance.ad_process.RDVProcess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.dms.service.DMSRDVService;
import sa.elm.ob.finance.dms.serviceimplementation.DMSRDVServiceImpl;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Poongodi on 18/01/2018
 *
 */
public class RDVRejectProcess implements Process {
  /**
   * This Servlet Class responsible to reject records in rdv
   */
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(RDVRejectProcess.class);

  @SuppressWarnings("unused")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // TODO Auto-generated method stub
    // Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String Lang = vars.getLanguage();

    final String rdvTxnId = (String) bundle.getParams().get("Efin_Rdvtxn_ID").toString();

    EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class, rdvTxnId);
    boolean checkEncumbranceAmountZero = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = transaction.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EfinRDVTransaction headerId = null;
    Boolean IsAdvance = false;
    String appstatus = "", alertWindow = AlertWindow.RDVTransaction;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    Boolean allowReject = false;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    EutForwardReqMoreInfo forwardObj = transaction.getEUTForwardReqmoreinfo();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    String constant = null;
    int count = 0;
    try {
      OBContext.setAdminMode(true);
      // get alert window based on advance
      if (transaction.getEfinRDVTxnlineList().size() == 1) {
        EfinRDVTxnline txnLine = transaction.getEfinRDVTxnlineList().get(0);
        IsAdvance = txnLine.isAdvance();
        if (IsAdvance) {
          alertWindow = AlertWindow.RDVAdvance;
          constant = Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE;
        } else {
          alertWindow = AlertWindow.RDVTransaction;
          constant = Constants.RECEIPT_DELIVERY_VERIFICATION;
        }
      }

      // get old nextrole line user and role list
      HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
          .getNextRoleLineList(transaction.getNextRole(), Resource.RDV_Transaction);

      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // reject the record without refreshing the page
      if (transaction.getEUTForwardReqmoreinfo() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(
            transaction.getEUTForwardReqmoreinfo(), userId, roleId, Resource.RDV_Transaction);
      }
      if (transaction.getEUTReqmoreinfo() != null
          || ((transaction.getEUTForwardReqmoreinfo() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }

      if (errorFlag) {

        if (transaction.getNextRole() != null) {
          java.util.List<EutNextRoleLine> li = transaction.getNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (transaction.getNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId,
              DocumentTypeE.RDV.getDocumentTypeCode());
          /*
           * String sql = ""; sql =
           * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
           * + CurrentDate + "' and to_date >='" + CurrentDate + "' and document_type='EUT_124'"; st
           * = conn.prepareStatement(sql); rs = st.executeQuery(); if (rs.next()) { String roleid =
           * rs.getString("ad_role_id"); if (roleid.equals(roleId)) { allowDelegation = true; } }
           */
        }
        if (allowUpdate || allowDelegation) {
          // Removing Forward and RMI Id
          if (transaction.getEUTForwardReqmoreinfo() != null) {
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(transaction.getEUTForwardReqmoreinfo());
            forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(transaction.getId(), constant);
          }
          if (transaction.getEUTReqmoreinfo() != null) {
            forwardReqMoreInfoDAO.setForwardStatusAsDraft(transaction.getEUTReqmoreinfo());
            forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(transaction.getId(), constant);
          }

          transaction.setUpdated(new java.util.Date());
          transaction.setUpdatedBy(OBContext.getOBContext().getUser());
          transaction.setAction("CO");
          transaction.setAppstatus("REJ");
          transaction.setNextRole(null);
          transaction.setRefbutton(false);
          transaction.setSubmitroleid(null);
          if (transaction.isContractcategoryRolePassed()) {
            transaction.setContractcategoryRolePassed(false);
          }
          OBDal.getInstance().save(transaction);
          OBDal.getInstance().flush();

          if (transaction.isAdvancetransaction()
              && transaction.getEfinRDVTxnlineList().size() > 0) {
            EfinRDVTxnline txnLineObj = transaction.getEfinRDVTxnlineList().get(0);
            txnLineObj.setAction("CO");
            txnLineObj.setApprovalStatus("DR");
            OBDal.getInstance().save(txnLineObj);
          }

          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              Resource.RDV_Transaction);
          headerId = transaction;
          if (!StringUtils.isEmpty(headerId.getId())) {
            appstatus = "REJ";

            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", headerId.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", appstatus);
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.RDV_Txn_History);
            historyData.put("HeaderColumn", ApprovalTables.RDV_Txn_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.RDV_Txn_DOCACTION_COLUMN);
            count = Utility.InsertApprovalHistory(historyData);
          }
          if (count > 0 && !StringUtils.isEmpty(headerId.getId())) {
            String Description = sa.elm.ob.finance.properties.Resource
                .getProperty("efin.rdv.rejected", Lang) + " " + objUser.getName();

            Role objCreatedRole = null;
            if (transaction.getCreatedBy().getADUserRolesList().size() > 0) {
              if (transaction.getRole() != null)
                objCreatedRole = transaction.getRole();
              else
                objCreatedRole = transaction.getCreatedBy().getADUserRolesList().get(0).getRole();
            }
            // check and insert alert recipient
            OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
                AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

            if (receipientQuery.list().size() > 0) {
              for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
                includeRecipient.add(objAlertReceipient.getRole().getId());
                OBDal.getInstance().remove(objAlertReceipient);
              }
            }
            if (includeRecipient != null)
              includeRecipient.add(objCreatedRole.getId());
            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
            }

            // // delete alert for approval alerts
            // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            // "as e where e.referenceSearchKey='" + transaction.getId()
            // + "' and e.alertStatus='NEW'");
            //
            // if (alertQuery.list().size() > 0) {
            // for (Alert objAlert : alertQuery.list()) {
            // objAlert.setAlertStatus("SOLVED");
            // }
            // }

            forwardReqMoreInfoDAO.getAlertForForwardedUser(transaction.getId(), alertWindow,
                alertRuleId, objUser, clientId, Constants.REJECT,
                transaction.getTXNVersion()
                    + (StringUtils.isEmpty(transaction.getCertificateNo()) ? ""
                        : "-" + transaction.getCertificateNo()),
                Lang, vars.getRole(), forwardObj, Resource.RDV_Transaction, alertReceiversMap);

            // Check Encumbrance Amount is Zero Or Negative
            if (transaction.getEfinRdv() != null) {
              if (transaction.getEfinRdv().getManualEncumbrance() != null) {
                encumLinelist = transaction.getEfinRdv().getManualEncumbrance()
                    .getEfinBudgetManencumlinesList();
              }
            }
            if (encumLinelist.size() > 0)
              checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

            if (checkEncumbranceAmountZero) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
              bundle.setResult(result);
              return;
            }

            // delete the unused nextroles in eut_next_role table.
            DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                Resource.RDV_Transaction);
            boolean ex = AlertUtility.alertInsertionRole(transaction.getId(),
                transaction.getTXNVersion()
                    + (StringUtils.isEmpty(transaction.getCertificateNo()) ? ""
                        : "-" + transaction.getCertificateNo()),
                "", transaction.getCreatedBy().getId(), transaction.getClient().getId(),
                Description, "NEW", alertWindow, "efin.rdv.rejected", Constants.GENERIC_TEMPLATE);

            try {
              // DMS integration
              DMSRDVService dmsService = new DMSRDVServiceImpl();
              dmsService.rejectAndReactivateOperations(transaction);
            } catch (Exception e) {
              log.error("Error while deleting the record in dms reject" + e.getMessage());
            }

            OBError result = OBErrorBuilder.buildMessage(null, "success", "@Efin_RDV_Reject@");
            bundle.setResult(result);
            return;
          }
          OBDal.getInstance().flush();
        } else {
          errorFlag = false;
          errorMsg = OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved");
          throw new OBException(errorMsg);
        }
      }
    } catch (Exception e) {
      log.error("exception :", e);
      obError.setType("Error");
      obError.setTitle("Error");
      obError.setMessage(errorMsg);
      bundle.setResult(obError);
      OBDal.getInstance().rollbackAndClose();
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }
}
