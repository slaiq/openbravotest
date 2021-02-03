package sa.elm.ob.scm.ad_process.Requisition;

import java.sql.Connection;
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
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.ESCMPurchaseReqAppHist;
import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Gokul 22/06/20
 *
 */
public class RequisitionReactivate implements Process {
  /*
   * This servlet class is responsible to reactivate PR Records
   */
  private final static OBError obError = new OBError();
  private static Logger log = Logger.getLogger(RequisitionReactivate.class);
  private static String documentType = "";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // TODO Auto-generated method stub
    @SuppressWarnings("unused")
    Connection connection = null;
    OBContext.setAdminMode(true);
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    boolean checkEncumbranceAmountZero = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      // throw new RuntimeException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    }

    String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
    Requisition objRequisition = Utility.getObject(Requisition.class, strRequisitionId);
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = objRequisition.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String Description = "";
    // String comments = (String) bundle.getParams().get("notes").toString();
    String headerId = null;
    String appstatus = "", alertWindow = AlertWindow.PurchaseRequisition;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    String Lang = vars.getLanguage();
    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    boolean errorFlag = true;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    EutForwardReqMoreInfo forwardObj = null;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    User user = null;
    Role returnRole = null;
    // Task No.5925
    List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
    log.debug("Requisition Id:" + strRequisitionId);

    try {
      OBContext.setAdminMode();
      forwardObj = objRequisition.getEutForward();
      ESCMPurchaseReqAppHist acthist = null;
      OBQuery<ESCMPurchaseReqAppHist> acthistqry = OBDal.getInstance().createQuery(
          ESCMPurchaseReqAppHist.class, " as e where e.requisition.id = '" + strRequisitionId
              + "' and e.purchasereqaction='RETURN' order by e.creationDate desc");
      acthistqry.setMaxResult(1);
      if (acthistqry.list().get(0).getId() != null) {
        acthist = OBDal.getInstance().get(ESCMPurchaseReqAppHist.class,
            acthistqry.list().get(0).getId());
      }
      user = OBDal.getInstance().get(User.class, acthist.getCreatedBy().getId());
      returnRole = OBDal.getInstance().get(Role.class, acthist.getRole().getId());
      if (objRequisition.getEscmProcesstype() != null
          && objRequisition.getEscmProcesstype().equals("DP")) {
        documentType = Resource.PURCHASE_REQUISITION;
      } else {
        documentType = Resource.PURCHASE_REQUISITION_LIMITED;
      }
      if (objRequisition != null) {
        OBQuery<Escmbidsourceref> bidSrcRef = OBDal.getInstance()
            .createQuery(Escmbidsourceref.class, " as e where e.requisition.id= :reqId ");
        bidSrcRef.setNamedParameter("reqId", strRequisitionId);
        if (bidSrcRef.list() != null && bidSrcRef.list().size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_PR_Bidref@");
          bundle.setResult(result);
          return;
        }

        OBQuery<EscmProposalsourceRef> propSrcRef = OBDal.getInstance()
            .createQuery(EscmProposalsourceRef.class, " as e where e.requisition.id= :reqId ");
        propSrcRef.setNamedParameter("reqId", strRequisitionId);
        if (propSrcRef.list() != null && propSrcRef.list().size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PR_Prop_Ref@");
          bundle.setResult(result);
          return;
        }

        OBQuery<EscmOrderSourceRef> ordSrcRef = OBDal.getInstance()
            .createQuery(EscmOrderSourceRef.class, " as e where e.requisition.id= :reqId ");
        ordSrcRef.setNamedParameter("reqId", strRequisitionId);
        if (ordSrcRef.list() != null && ordSrcRef.list().size() > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_PR_PO_Ref@");
          bundle.setResult(result);
          return;
        }

      }

      // check pr encumbrance type is enable or not
      OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
          " as e where e.encumbranceType='PRE' and e.client.id=:clientID and e.active='Y' ");
      encumcontrol.setNamedParameter("clientID", clientId);
      encumcontrol.setFilterOnActive(true);
      encumcontrol.setMaxResult(1);
      enccontrollist = encumcontrol.list();

      if (enccontrollist.size() > 0) {
        if (objRequisition.isEfinEncumbered()) {
          // get encum line list
          List<EfinBudgetManencumlines> encumLinesList = null;
          OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance()
              .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id=:encumID");
          encumLines.setNamedParameter("encumID", objRequisition.getEfinBudgetManencum().getId());
          if (encumLines.list() != null && encumLines.list().size() > 0) {
            encumLinesList = encumLines.list();
          }
          // validation
          errorFlag = RequisitionDao.checkFundsForReject(objRequisition, encumLinesList);
          if (errorFlag) {
            // manual encum
            if (objRequisition.getEfinBudgetManencum().getEncumMethod().equals("M")) {
              // update amount
              OBInterceptor.setPreventUpdateInfoChange(true);
              RequisitionDao.updateManualEncumAmountRej(objRequisition, encumLinesList, false, "");
              objRequisition.setEfinEncumbered(false);
              OBDal.getInstance().save(objRequisition);
              OBDal.getInstance().flush();
              OBInterceptor.setPreventUpdateInfoChange(false);
            }
            // auto encumbrance
            else {
              RequisitionDao.updateAmtInEnquiryRej(objRequisition.getId(), encumLinesList, false,
                  "");

              // Check Encumbrance Amount is Zero Or Negative
              if (objRequisition.getEfinBudgetManencum() != null)
                encumLinelist = objRequisition.getEfinBudgetManencum()
                    .getEfinBudgetManencumlinesList();
              if (encumLinelist.size() > 0)
                checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

              if (checkEncumbranceAmountZero) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
                bundle.setResult(result);
                return;
              }

              // remove encum
              EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  objRequisition.getEfinBudgetManencum().getId());
              encum.setDocumentStatus("DR");
              objRequisition.setEfinBudgetManencum(null);
              objRequisition.setEscmManualEncumNo("");
              objRequisition.setEfinEncumbered(false);
              OBDal.getInstance().save(objRequisition);
              // remove encum reference in lines.
              List<RequisitionLine> reqLine = objRequisition.getProcurementRequisitionLineList();
              for (RequisitionLine reqLineList : reqLine) {
                reqLineList.setEfinBudEncumlines(null);
                reqLineList.setEfinCValidcombination(null);
                reqLineList.setEfinUniquecodename(null);
                OBDal.getInstance().save(reqLineList);
              }
              OBDal.getInstance().flush();
              // if (objRequisition.getEfinBudgetManencum() != null) {
              // objRequisition.getEfinBudgetManencum()
              // .setProcurementRequisitionEMEfinBudgetManencumIDList(null);
              // }
              OBDal.getInstance().remove(encum);

            }
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_Encum_Used_Cannot_Rej@");
            bundle.setResult(result);
            return;
          }
        }
      }
      // get alert rule id
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if (errorFlag) {
        try {

          // get old nextrole line user and role list
          HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
              .getNextRoleLineList(objRequisition.getEutNextRole(), documentType);

          if (objRequisition.getEscmMaterialRequest() == null) {
            if (objRequisition.getEutNextRole() != null) {
              java.util.List<EutNextRoleLine> li = objRequisition.getEutNextRole()
                  .getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (objRequisition.getEutNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId, documentType);
            }
            objRequisition.setUpdated(new java.util.Date());
            objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
            objRequisition.setEscmSendnotification(true);
            objRequisition.setEscmPrReturn(false);
            objRequisition.setEscmDocStatus("DR");
            objRequisition.setDocumentStatus("DR");
            objRequisition.setDocumentAction("CO");
            objRequisition.setEscmDocaction("CO");
            objRequisition.setEutNextRole(null);
            OBDal.getInstance().save(objRequisition);
            // OBDal.getInstance().flush();

            headerId = objRequisition.getId();
            if (!StringUtils.isEmpty(objRequisition.getId())) {
              appstatus = "REA";

              JSONObject historyData = new JSONObject();

              historyData.put("ClientId", clientId);
              historyData.put("OrgId", orgId);
              historyData.put("RoleId", roleId);
              historyData.put("UserId", userId);
              historyData.put("HeaderId", headerId);
              historyData.put("Comments", "");
              historyData.put("Status", appstatus);
              historyData.put("NextApprover", "");
              historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
              historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
              historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

              count = Utility.InsertApprovalHistory(historyData);
            }
            if (count > 0 && !StringUtils.isEmpty(objRequisition.getId())) {
              Role objCreatedRole = null;
              if (objRequisition.getEscmAdRole() != null)
                objCreatedRole = objRequisition.getEscmAdRole();
              else if (objRequisition.getCreatedBy().getADUserRolesList().size() > 0) {

                objCreatedRole = objRequisition.getCreatedBy().getADUserRolesList().get(0)
                    .getRole();
              }
              // check and insert alert recipient
              AlertUtility.insertAlertRecipient(returnRole.getId(), user.getId(), clientId,
                  alertWindow);
              List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);
              // OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              // .createQuery(AlertRecipient.class, "as e where e.userContact.id='" + user + "'");
              // List<AlertRecipient> alertRecList1 = receipientQuery.list();
              // alrtRecList.add(alertRecList1.get(0));

              if (alrtRecList.size() > 0) {
                for (AlertRecipient objAlertReceipient : alrtRecList) {
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

              // solve alerts
              AlertUtility.solveAlerts(objRequisition.getId());

              // delete alert for approval alerts
              // AlertUtility.deleteAlerts(objRequisition.getId());

              // forwardReqMoreInfoDAO.getAlertForForwardedUser(objRequisition.getId(), alertWindow,
              // alertRuleId, objUser, clientId, Constants.REJECT, objRequisition.getDocumentNo(),
              // Lang, vars.getRole(), forwardObj, documentType, alertReceiversMap);

              // // set alert for requester
              // Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.pr.reactivate",
              // vars.getLanguage()) + " " + objUser.getName();
              // AlertUtility.alertInsertionRole(objRequisition.getId(),
              // objRequisition.getDocumentNo(), objCreatedRole.getId(),
              // objRequisition.getCreatedBy().getId(), objRequisition.getClient().getId(),
              // Description, "NEW", alertWindow, "scm.pr.reactivate", Constants.GENERIC_TEMPLATE);

              // set alert for return user
              Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.pr.reactivate",
                  vars.getLanguage()) + " by " + objUser.getName();
              AlertUtility.alertInsertionRole(objRequisition.getId(),
                  objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
                  returnRole.getId(), user.getId(), objRequisition.getClient().getId(), Description,
                  "NEW", alertWindow, "scm.pr.reactivate", Constants.GENERIC_TEMPLATE);

              OBError result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_PR_Reactivate@");
              bundle.setResult(result);
              return;
            }
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          } else {

            // get alert rule
            alertRuleId = AlertUtility.getAlertRule(clientId, "PRFMIR");

            // upddate the header status as rejected
            objRequisition.setUpdated(new java.util.Date());
            objRequisition.setUpdatedBy(OBContext.getOBContext().getUser());
            objRequisition.setEscmDocStatus("DR");
            objRequisition.setEscmDocaction("PD");
            objRequisition.setEutNextRole(null);
            objRequisition.setEscmPrReturn(false);
            OBDal.getInstance().save(objRequisition);
            OBDal.getInstance().flush();

            Role objCreatedRole = null;

            if (objRequisition.getEscmMaterialRequest() != null) {
              if (objRequisition.getEscmMaterialRequest().getRole() != null)
                objCreatedRole = objRequisition.getEscmMaterialRequest().getRole();
              else if (objRequisition.getEscmMaterialRequest().getCreatedBy().getADUserRolesList()
                  .size() > 0) {

                objCreatedRole = objRequisition.getEscmMaterialRequest().getCreatedBy()
                    .getADUserRolesList().get(0).getRole();
              }
            } else {
              if (objRequisition.getEscmAdRole() != null)
                objCreatedRole = objRequisition.getEscmAdRole();
              else if (objRequisition.getCreatedBy().getADUserRolesList().size() > 0) {

                objCreatedRole = objRequisition.getCreatedBy().getADUserRolesList().get(0)
                    .getRole();
              }
            }

            if (includeRecipient != null)
              includeRecipient.add(objCreatedRole.getId());

            // delete alert for approval alerts - Task:7618
            AlertUtility.deleteAlerts(objRequisition.getId());

            forwardReqMoreInfoDAO.getAlertForForwardedUser(objRequisition.getId(), alertWindow,
                alertRuleId, objUser, clientId, Constants.REJECT,
                objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(), Lang,
                vars.getRole(), forwardObj, documentType, alertReceiversMap);
            // avoid duplicate recipient
            HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
            Iterator<String> iterator = incluedSet.iterator();
            while (iterator.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator.next(), null, clientId,
                  AlertWindow.PurchaseReqForMIR);
            }
            Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.createPR.for.MIR.rej",
                Lang) + " " + OBContext.getOBContext().getUser().getName();

            AlertUtility.alertInsertionRole(objRequisition.getId(),
                objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
                objCreatedRole.getId(), "", objRequisition.getClient().getId(), Description, "NEW",
                AlertWindow.PurchaseReqForMIR, "scm.createPR.for.MIR.rej",
                Constants.GENERIC_TEMPLATE);

            HashSet<String> incluedSet1 = new HashSet<String>(includeRecipient);
            Iterator<String> iterator1 = incluedSet1.iterator();
            while (iterator1.hasNext()) {
              AlertUtility.insertAlertRecipient(iterator1.next(), null, clientId,
                  AlertWindow.PurchaseReqForMIR);
            }

            headerId = objRequisition.getId();
            if (!StringUtils.isEmpty(objRequisition.getId())) {
              appstatus = "REA";

              JSONObject historyData = new JSONObject();

              historyData.put("ClientId", clientId);
              historyData.put("OrgId", orgId);
              historyData.put("RoleId", roleId);
              historyData.put("UserId", userId);
              historyData.put("HeaderId", headerId);
              historyData.put("Comments", "");
              historyData.put("Status", appstatus);
              historyData.put("NextApprover", "");
              historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
              historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
              historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);

              count = Utility.InsertApprovalHistory(historyData);
            }

            // Check Encumbrance Amount is Zero Or Negative
            if (objRequisition.getEfinBudgetManencum() != null)
              encumLinelist = objRequisition.getEfinBudgetManencum()
                  .getEfinBudgetManencumlinesList();
            if (encumLinelist.size() > 0)
              checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

            if (checkEncumbranceAmountZero) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
              bundle.setResult(result);
              return;
            }

            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@ESCM_Requisition_Rework@");
            bundle.setResult(result);
            return;

          }
        } catch (Exception e) {
          // Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          e.printStackTrace();
          bundle.setResult(error);
          /*
           * obError.setType("Error"); obError.setTitle("Error"); obError.setMessage(errorMsg);
           * bundle.setResult(obError);
           */
          OBDal.getInstance().rollbackAndClose();

        }
      }

      else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }

      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      e.printStackTrace();
      bundle.setResult(obError);
      log.error("exception in Requisition rework:", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
