package sa.elm.ob.scm.actionHandler;

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
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
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
 * This class is used for Bid Management Reject Process
 * 
 * @author qualian
 *
 */
public class BidManagementReject implements Process {
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(BidManagementReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // TODO Auto-generated method stub
    // Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = Utility.getObject(User.class, vars.getUser());
    String Lang = vars.getLanguage();

    /*
     * try {
     * 
     * ConnectionProvider provider = bundle.getConnection(); connection = provider.getConnection();
     * } catch (NoConnectionAvailableException e) {
     * log.error("No Database Connection Available.Exception:" + e); throw new RuntimeException(e);
     * }
     */
    final String receiptId = (String) bundle.getParams().get("Escm_Bidmgmt_ID").toString();

    EscmBidMgmt bidmgmt = Utility.getObject(EscmBidMgmt.class, receiptId);

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = bidmgmt.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    EscmBidMgmt headerId = null;
    String appstatus = "", alertWindow = AlertWindow.BidManagement;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    boolean checkEncumbranceAmountZero = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();

    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;
    EutForwardReqMoreInfo forwardObj = bidmgmt.getEUTForwardReqmoreinfo();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

    EfinBudgetManencum encumbrance = null;
    JSONObject resultEncum = null;
    try {
      OBContext.setAdminMode(true);

      Boolean allowReject = false;

      if (bidmgmt.getEUTForwardReqmoreinfo() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(bidmgmt.getEUTForwardReqmoreinfo(),
            userId, roleId, Resource.Bid_Management);
      }
      if (bidmgmt.getEUTReqmoreinfo() != null
          || ((bidmgmt.getEUTForwardReqmoreinfo() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = BidManagementDAO.getPREncumTypeList(clientId);

      // get alert rule id
      alertRuleId = BidManagementDAO.getAlertRule(clientId, alertWindow);
      if (errorFlag) {
        try {
          count = BidManagementDAO.getBidMgmtLineCount(receiptId);
          if (count > 0) {
            EscmBidMgmt header = Utility.getObject(EscmBidMgmt.class, receiptId);
            // Task No.5925
            if (enccontrollist.size() > 0 && header.getEncumbrance() != null) {
              encumbrance = header.getEncumbrance();
              // reject the merge and splitencumbrance
              resultEncum = BidManagementDAO.checkFullPRQtyUitlizeorNot(header);
              // if full qty only used then remove the encumbrance reference and change the
              // encumencumbrance stage as PR Stage
              if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                  && resultEncum.getBoolean("isAssociatePREncumbrance")
                  && resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {
                errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(header, null,
                    null);
                log.debug("errorFlag12:" + errorFlag);
                if (errorFlag) {
                  OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_ProcessFailed(Reason)@");
                  bundle.setResult(result1);
                  return;
                } else {
                  // Check Encumbrance Amount is Zero Or Negative
                  if (header.getEncumbrance() != null)
                    encumLinelist = header.getEncumbrance().getEfinBudgetManencumlinesList();
                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZero = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (checkEncumbranceAmountZero) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_Encumamt_Neg@");
                    bundle.setResult(result);
                    return;
                  }
                  BidManagementDAO.reactivateStageUniqueCodeChg(resultEncum, bidmgmt, null);
                  encumbrance.setEncumStage("PRE");
                  header.setEfinIsbudgetcntlapp(false);
                  header.setEncumbrance(null);
                }

              } else {

                errorFlag = BidManagementDAO.chkFundsAvailforReactOldEncumbrance(header, null,
                    null);
                if (errorFlag) {
                  OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_ProcessFailed(Reason)@");
                  bundle.setResult(result1);
                  return;
                } else {
                  if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                    BidManagementDAO.reactivateSplitPR(resultEncum, header, null);
                  }
                  if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                    BidManagementDAO.reactivateSplitPR(resultEncum, header, null);
                  }
                }
              }
            }
            // End Task No.5925

            if (header.getEUTNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getEUTNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }

            // check current role is a delegated role or not
            if (header.getEUTNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId,
                  DocumentTypeE.BID.getDocumentTypeCode());
            }
            if (allowUpdate || allowDelegation) {

              // remove eut_forward_rmi id from record
              if (bidmgmt.getEUTNextRole() != null) {
                // Give Role Access to Receiver
                if (bidmgmt.getEUTForwardReqmoreinfo() != null) {
                  // update status as "DR"
                  forwardReqMoreInfoDAO.setForwardStatusAsDraft(bidmgmt.getEUTForwardReqmoreinfo());
                  // access remove
                  // forwardReqMoreInfoDAO.removeRoleAccess(clientId, forwardId, conn);

                  // Remove Forward_Rmi id from transaction screens
                  forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(bidmgmt.getId(), "BID");

                }
                if (bidmgmt.getEUTReqmoreinfo() != null) {

                  // update status as "DR"
                  forwardReqMoreInfoDAO.setForwardStatusAsDraft(bidmgmt.getEUTReqmoreinfo());

                  // access remove
                  // forwardReqMoreInfoDAO.removeReqMoreInfoRoleAccess(clientId,
                  // requestMoreInfoId,conn);

                  // Remove Forward_Rmi id from transaction screens
                  forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(bidmgmt.getId(), "BID");
                }
              }

              HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
                  .getNextRoleLineList(header.getEUTNextRole(), Resource.Bid_Management);
              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setEscmDocaction("CO");
              header.setBidappstatus("ESCM_REJ");
              header.setBidstatus("IA");
              header.setEUTNextRole(null);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  Resource.Bid_Management);

              headerId = header;
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
                historyData.put("HistoryTable", ApprovalTables.Bid_Management_History);
                historyData.put("HeaderColumn",
                    ApprovalTables.Bid_Management_History_HEADER_COLUMN);
                historyData.put("ActionColumn",
                    ApprovalTables.Bid_Management_History_DOCACTION_COLUMN);
                count = Utility.InsertApprovalHistory(historyData);
              }
              if (count > 0 && !StringUtils.isEmpty(header.getId())) {

                Role objCreatedRole = null;
                if (header.getCreatedBy().getADUserRolesList().size() > 0) {
                  if (header.getRole() != null)
                    objCreatedRole = header.getRole();
                  else
                    objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
                }
                // check and insert alert recipient
                List<AlertRecipient> alrtRecLs = BidManagementDAO.getAlertReceipient(alertRuleId);
                if (alrtRecLs.size() > 0) {
                  for (AlertRecipient objAlertReceipient : alrtRecLs) {
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

                // Check Encumbrance Amount is Zero Or Negative
                if (bidmgmt.getEncumbrance() != null)
                  encumLinelist = bidmgmt.getEncumbrance().getEfinBudgetManencumlinesList();
                if (encumLinelist.size() > 0)
                  checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

                if (checkEncumbranceAmountZero) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_Encumamt_Neg@");
                  bundle.setResult(result);
                  return;
                }

                // delete alert for approval alerts
                BidManagementDAO.getAlert(header.getId());
                forwardReqMoreInfoDAO.getAlertForForwardedUser(bidmgmt.getId(), alertWindow,
                    alertRuleId, objUser, clientId, Constants.REJECT,
                    bidmgmt.getBidno() + "-" + bidmgmt.getBidname(), Lang, vars.getRole(),
                    forwardObj, Resource.Bid_Management, alertReceiversMap);
                String Description = sa.elm.ob.scm.properties.Resource
                    .getProperty("scm.BidMgmt.rejected", Lang) + " " + objUser.getName();
                // delete the unused nextroles in eut_next_role table.
                DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                    Resource.Bid_Management);
                AlertUtility.alertInsertionRole(header.getId(),
                    header.getBidno() + "-" + bidmgmt.getBidname(), objCreatedRole.getId(),
                    header.getCreatedBy().getId(), header.getClient().getId(), Description, "NEW",
                    alertWindow, "scm.BidMgmt.rejected", Constants.GENERIC_TEMPLATE);

                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Escm_BidrejectSuccess@");
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
      } else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
