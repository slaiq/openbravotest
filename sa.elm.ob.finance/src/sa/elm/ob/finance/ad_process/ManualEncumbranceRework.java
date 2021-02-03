package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.efinbudgetencum;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
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
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;

public class ManualEncumbranceRework implements Process {

  private static final Logger log = Logger.getLogger(ManualEncumbranceRework.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    log.debug("rework the budget");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    String ManEncumId = (String) bundle.getParams().get("Efin_Budget_Manencum_ID");
    EfinBudgetManencum manEncumbarance = OBDal.getInstance().get(EfinBudgetManencum.class,
        ManEncumId);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = manEncumbarance.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    String headerId = null, alertWindow = AlertWindow.Encumbrance, alertRuleId = null,
        doctype = Resource.MANUAL_ENCUMBRANCE_RULE;
    String appstatus = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());

    log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);

    boolean errorFlag = true;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    String errorMsg = "";
    int count = 0;
    Date currentDate = new Date();
    Boolean allowReject = false;
    EutForwardReqMoreInfo forwardObj = manEncumbarance.getEUTForwardReqmoreinfo();
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

    log.debug("budgetId:" + ManEncumId);

    if (manEncumbarance.getDocumentStatus().equals("CO")) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Efin_AlreadyPreocessed_Approve@");
      bundle.setResult(result);
      return;
    }
    // If the record is Forwarded or given RMI then throw error when any other user tries to
    // reject the record without refreshing the page
    if (manEncumbarance.getEUTForwardReqmoreinfo() != null) {
      allowReject = forwardReqMoreInfoDAO.allowApproveReject(
          manEncumbarance.getEUTForwardReqmoreinfo(), userId, roleId,
          Resource.MANUAL_ENCUMBRANCE_RULE);
    }
    if (manEncumbarance.getEUTReqmoreinfo() != null
        || ((manEncumbarance.getEUTForwardReqmoreinfo() != null) && (!allowReject))) {
      OBDal.getInstance().rollbackAndClose();
      OBError result = OBErrorBuilder.buildMessage(null, "error",
          "@Escm_AlreadyPreocessed_Approved@");
      bundle.setResult(result);
      return;
    }
    try {
      if (errorFlag) {
        try {
          OBContext.setAdminMode(true);

          // get alert rule id
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow
                  + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
              EfinBudgetManencumlines.class, "manualEncumbrance.id ='" + ManEncumId + "'");
          count = lines.list().size();

          if (count > 0) {

            EfinBudgetManencum header = OBDal.getInstance().get(EfinBudgetManencum.class,
                ManEncumId);

            if (header.getNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }

            }
            // check current role is a delegated role or not
            if (header.getNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                  DocumentTypeE.ENCUMBRANCE.getDocumentTypeCode());
              /*
               * String sql = ""; sql =
               * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
               * + ActDate + "' and to_date >='" + ActDate + "'"; st = conn.prepareStatement(sql);
               * rs = st.executeQuery(); if (rs.next()) { String roleid =
               * rs.getString("ad_role_id"); if (roleid.equals(roleId)) { allowDelegation = true; }
               * }
               */
            }

            if (allowUpdate || allowDelegation) {

              // Removing Forward and RMI Id
              if (header.getEUTForwardReqmoreinfo() != null) {
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTForwardReqmoreinfo());
                forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                    Constants.ENCUMBRANCE);
              }
              if (header.getEUTReqmoreinfo() != null) {
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEUTReqmoreinfo());
                forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                    Constants.ENCUMBRANCE);
              }

              // get old nextrole line user and role list
              HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
                  .getNextRoleLineList(header.getNextRole(), Resource.MANUAL_ENCUMBRANCE_RULE);

              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setDocumentStatus("RW");
              header.setAction("CO");
              header.setNextRole(null);
              header.setRevoke(false);
              log.debug("header:" + header.toString());
              OBDal.getInstance().save(header);
              OBDal.getInstance().flush();

              // reject after reserved fund then revert the changes from budget enquiry
              if (manEncumbarance.isReservedfund()) {
                // revert the changes from budget enquiry
                for (EfinBudgetManencumlines encumLine : manEncumbarance
                    .getEfinBudgetManencumlinesList()) {
                  String sql = "select efin_updateBudgetInq(?,?,?) from dual ;";
                  st = conn.prepareStatement(sql);
                  st.setString(1, encumLine.getAccountingCombination().getId());
                  st.setBigDecimal(2, encumLine.getRevamount().negate());
                  st.setString(3, manEncumbarance.getBudgetInitialization().getId());
                  st.executeQuery();

                  // while rejecting if department funds ='N', then revert it back from budget
                  // enquiry for non 990, 999 dept

                  if (!encumLine.getAccountingCombination().isEFINDepartmentFund()) {
                    EfinBudgetInquiry budInq = ManualEncumbaranceSubmitDAO.getBudgetInquiry(
                        encumLine.getAccountingCombination().getId(),
                        encumLine.getManualEncumbrance().getBudgetInitialization().getId());

                    if (budInq != null) {
                      budInq.setEncumbrance(
                          budInq.getEncumbrance().subtract(encumLine.getRevamount()));
                      OBDal.getInstance().save(budInq);
                    }
                  }
                }
                OBDal.getInstance().flush();

                // set reservedfund as 'N'
                manEncumbarance.setReservedfund(false);
              }

              // Alert process Start
              Role objCreatedRole = null;
              if (header.getCreatedBy().getADUserRolesList().size() > 0) {
                if (header.getRole() != null)
                  objCreatedRole = header.getRole();
                else
                  objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
              }
              log.debug("objCreatedRole:" + objCreatedRole);

              // check and insert alert recipient
              OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
                  AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

              if (receipientQuery.list().size() > 0) {
                for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
                  includeRecipient.add(objAlertReceipient.getRole().getId());
                  OBDal.getInstance().remove(objAlertReceipient);
                }
              }
              includeRecipient.add(objCreatedRole.getId());
              // avoid duplicate recipient
              HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
              Iterator<String> iterator = incluedSet.iterator();
              while (iterator.hasNext()) {
                AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
              }
              forwardReqMoreInfoDAO.getAlertForForwardedUser(header.getId(), alertWindow,
                  alertRuleId, objUser, clientId, Constants.REJECT, header.getDocumentNo(),
                  vars.getLanguage(), vars.getRole(), forwardObj, Resource.MANUAL_ENCUMBRANCE_RULE,
                  alertReceiversMap);

              // delete alert for approval alerts
              /*
               * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
               * "as e where e.referenceSearchKey='" + header.getId() +
               * "' and e.alertStatus='NEW'");
               * 
               * if (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
               * objAlert.setAlertStatus("SOLVED"); } }
               */
              String Description = sa.elm.ob.finance.properties.Resource.getProperty(
                  "finance.encumbrance.rejected", vars.getLanguage()) + " " + objUser.getName();
              log.debug("Description:" + Description);
              // delete the unused nextroles in eut_next_role table.
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), doctype);
              AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(), "",
                  header.getCreatedBy().getId(), header.getClient().getId(), Description, "NEW",
                  alertWindow, "finance.encumbrance.rejected", Constants.GENERIC_TEMPLATE);
              // Alert Process end

              DocumentRuleDAO.deleteUnusedNextRoles(conn, Resource.MANUAL_ENCUMBRANCE_RULE);
              headerId = header.getId();
              log.debug("headerId:" + header.getId());
              if (!StringUtils.isEmpty(header.getId())) {
                appstatus = "REW";
                ManualEncumbaranceSubmitDAO.insertManEncumHistory(
                    OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId, headerId,
                    comments, appstatus, null);
                RemoveEncumRecord(OBDal.getInstance().getConnection(), clientId, orgId, roleId,
                    userId, headerId);
              }
              if (count > 0 && !StringUtils.isEmpty(header.getId())) {
                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Efin_ManEncum_Rework@");
                bundle.setResult(result);
                return;
              }
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Efin_AlreadyPreocessed_Approve");
              throw new OBException(errorMsg);
            }
          }
        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());

          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);

          bundle.setResult(error);
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
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static int RemoveEncumRecord(Connection con, String clientId, String orgId, String roleId,
      String userId, String headerId) {
    try {
      OBContext.setAdminMode(true);
      OBQuery<EfinBudgetManencumlines> encumlines = OBDal.getInstance()
          .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id ='" + headerId + "'");
      log.debug("encumlines:" + encumlines.getWhereAndOrderBy());
      if (encumlines.list().size() > 0) {
        for (int i = 0; i < encumlines.list().size(); i++) {
          EfinBudgetManencumlines list = encumlines.list().get(i);

          OBQuery<efinbudgetencum> qry = OBDal.getInstance().createQuery(efinbudgetencum.class,
              " manualEncumbranceLines.id='" + list.getId() + "'");
          log.debug("removewhere:" + qry.getWhereAndOrderBy());
          if (qry.list().size() > 0) {
            for (int j = 0; j < qry.list().size(); j++) {
              efinbudgetencum encumheader = qry.list().get(j);
              OBDal.getInstance().remove(encumheader);
            }
          }
        }
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in insertManEncumHistory: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }
}
