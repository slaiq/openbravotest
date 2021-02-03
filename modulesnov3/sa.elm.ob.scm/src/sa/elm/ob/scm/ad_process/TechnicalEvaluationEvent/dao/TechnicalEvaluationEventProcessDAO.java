package sa.elm.ob.scm.ad_process.TechnicalEvaluationEvent.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author DIVYA-05-01-2018
 *
 */
public class TechnicalEvaluationEventProcessDAO {

  private final static Logger log = LoggerFactory
      .getLogger(TechnicalEvaluationEventProcessDAO.class);
  private static final String TECHNICALREVIEW = "TER";

  /**
   * Update the next role
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param techevlevent
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param paramnextApproval
   * @param Lang
   * @param bundle
   * @return
   */
  public static JSONObject updateHeaderStatus(Connection con, String clientId, String orgId,
      String roleId, String userId, EscmTechnicalevlEvent techevlevent, String appstatus,
      String comments, Date currentDate, VariablesSecureApp vars,
      NextRoleByRuleVO paramnextApproval, String Lang, ProcessBundle bundle) {
    String strTechEventId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.TechnicalEvalEvent;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = techevlevent.getCreatedBy();
    NextRoleByRuleVO nextApproval = paramnextApproval;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    boolean isDummyRole = false;
    String errorMsgs = null;
    JSONObject result = new JSONObject();
    BigDecimal discount_Amount = BigDecimal.ZERO;
    BigDecimal final_NegPrice = BigDecimal.ZERO;
    BigDecimal discAmt = BigDecimal.ZERO;
    BigDecimal calculated_Discount_Amount = BigDecimal.ZERO;

    try {
      OBContext.setAdminMode();

      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      JSONObject fromUserandRoleJson = new JSONObject();
      String fromUser = userId;
      String fromRole = roleId;
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

      BigDecimal grossPrice = BigDecimal.ZERO;
      BigDecimal netPrice = BigDecimal.ZERO;

      isDirectApproval = isDirectApproval(techevlevent.getId(), roleId);

      // setting json object value initially
      result.put("count", 0);
      result.put("errormsg", "null");

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if ((techevlevent.getEUTNextRole() != null)) {

        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            techevlevent.getEUTNextRole(), userId, roleId, clientId, orgId,
            Resource.TECHNICAL_EVALUATION_EVENT, isDummyRole, isDirectApproval);
        if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
          if (fromUserandRoleJson.has("fromUser"))
            fromUser = fromUserandRoleJson.getString("fromUser");
          if (fromUserandRoleJson.has("fromRole"))
            fromRole = fromUserandRoleJson.getString("fromRole");
          if (fromUserandRoleJson.has("isDirectApproval"))
            isDirectApproval = fromUserandRoleJson.getBoolean("isDirectApproval");
        }
      } else {
        fromUser = userId;
        fromRole = roleId;
      }
      // first time submit the record
      if ((techevlevent.getEUTNextRole() == null)) {
        // getting next level approver
        nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
            orgId, fromRole, fromUser, Resource.TECHNICAL_EVALUATION_EVENT, BigDecimal.ZERO);

      }
      // after submit- Next level approval
      else {
        // checking direct approver or delegated approver
        // if isDirectApproval flag is "true" then approver is direct approver
        if (isDirectApproval) {
          // getting next level approver
          nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
              orgId, fromRole, fromUser, Resource.TECHNICAL_EVALUATION_EVENT, BigDecimal.ZERO);

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id=:roleId");
                userRole.setNamedParameter("roleId", objNextRoleLine.getRole().getId());
                // checking next level approver is delegated with backward role in Document Rule.
                // Ex- ((document rule is a,b,c,d) b is approving the record. c is next level
                // approver, but c is delegated with b then if b is approving it will wait for d
                // approve only. it will skip the c)
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.list().get(0).getUserContact().getId(),
                    Resource.TECHNICAL_EVALUATION_EVENT, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                // check backward delegation
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, Resource.TECHNICAL_EVALUATION_EVENT,
                    BigDecimal.ZERO);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, Resource.TECHNICAL_EVALUATION_EVENT,
                BigDecimal.ZERO);
          }
        }
        // if approver is delegated user
        else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, Resource.TECHNICAL_EVALUATION_EVENT, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, delegatedToRole, fromUser,
                Resource.TECHNICAL_EVALUATION_EVENT, BigDecimal.ZERO);
        }
      }

      // if Role doesnt has any user associated then this condition will execute and return error
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        count = -2;

        result.put("count", count);
        result.put("errormsg", errorMsgs);

      }
      // if no error and having next level approver then update the status as inprogress
      else if (nextApproval != null && nextApproval.hasApproval()) {

        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao.getNextRoleLineList(
            techevlevent.getEUTNextRole(), Resource.TECHNICAL_EVALUATION_EVENT);

        // update the technical evaluation event update status
        techevlevent.setUpdated(new java.util.Date());
        techevlevent.setUpdatedBy(OBContext.getOBContext().getUser());
        techevlevent.setAction("AP");
        techevlevent.setStatus("ESCM_IP");
        techevlevent.setEUTNextRole(nextRole);

        // get alert recipients - Task No:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {

          // solving approval alerts - Task No:7618
          AlertUtility.solveAlerts(techevlevent.getId());

          forwardDao.getAlertForForwardedUser(techevlevent.getId(), alertWindow, alertRuleId,
              objUser, clientId, Constants.APPROVE, techevlevent.getEventNo(), Lang, vars.getRole(),
              techevlevent.getEUTForward(), Resource.TECHNICAL_EVALUATION_EVENT, alertReceiversMap);
          // define waiting for approval description
          String Description = sa.elm.ob.scm.properties.Resource
              .getProperty("scm.techevaluation.event.wfa", Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(techevlevent.getId(), techevlevent.getEventNo(),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                techevlevent.getClient().getId(), Description, "NEW", alertWindow,
                "scm.techevaluation.event.wfa", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.

            // OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
            // EutDocappDelegateln.class,
            // " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleID "
            // + " and hd.fromDate <=:currentdate and hd.date >=:currentdate and
            // e.documentType='EUT_123'");
            // delegationln.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
            // delegationln.setNamedParameter("currentdate", currentDate);

            /* Task #7742 */

            List<EutDocappDelegateln> delegationlnList = UtilityDAO
                .getDelegation(objNextRoleLine.getRole().getId(), currentDate, "EUT_123");

            // log.debug("delegationln:" + delegationln.getWhereAndOrderBy());
            if (delegationlnList != null && delegationlnList.size() > 0) {
              for (EutDocappDelegateln obDocAppDelegation : delegationlnList) {
                AlertUtility.alertInsertionRole(techevlevent.getId(), techevlevent.getEventNo(),
                    obDocAppDelegation.getRole().getId(),
                    obDocAppDelegation.getUserContact().getId(), techevlevent.getClient().getId(),
                    Description, "NEW", alertWindow, "scm.techevaluation.event.wfa",
                    Constants.GENERIC_TEMPLATE);

                includeRecipient.add(obDocAppDelegation.getRole().getId());
              }
              if (nextRole.getEutNextRoleLineList().size() == 1 && delegationlnList.size() == 1
                  && Utility.getAssignedUserForRoles(
                      nextRole.getEutNextRoleLineList().get(0).getRole().getId()).size() == 1) {
                if (pendingapproval != null)
                  pendingapproval += objNextRoleLine.getRole().getName() + " ("
                      + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + "/"
                      + delegationlnList.get(0).getRole().getName() + " - "
                      + delegationlnList.get(0).getUserContact().getName();
                else
                  pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                      objNextRoleLine.getRole().getName() + " ("
                          + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                          + delegationlnList.get(0).getRole().getName() + "-"
                          + delegationlnList.get(0).getUserContact().getName());
              }

            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 2;
        result.put("count", count);
        result.put("errormsg", "null");
      }
      // final approver
      else {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao.getNextRoleLineList(
            techevlevent.getEUTNextRole(), Resource.TECHNICAL_EVALUATION_EVENT);

        // update amount in line
        if (techevlevent.getEscmProposalAttrList().size() > 0) {
          for (EscmProposalAttribute objAttribute : techevlevent.getEscmProposalAttrList()) {
            grossPrice = BigDecimal.ZERO;
            netPrice = BigDecimal.ZERO;

            // update proposal status
            if (objAttribute.getEscmProposalmgmt() != null) {
              EscmProposalMgmt promgmt = objAttribute.getEscmProposalmgmt();
              promgmt.setProposalstatus(TECHNICALREVIEW);
            }
            if (objAttribute.getTEEEfinTaxMethod() != null && objAttribute.isTEEIstax()) {
              EscmProposalMgmt promgmt = objAttribute.getEscmProposalmgmt();
              if (!promgmt.isTaxLine() && promgmt.getEfinTaxMethod() == null) {
                promgmt.setEfinTaxMethod(objAttribute.getTEEEfinTaxMethod());
                promgmt.setTaxLine(true);
              }

            }
            if (objAttribute.getEscmProposalmgmt() != null
                && objAttribute.getEscmProposalmgmt().getEscmProposalmgmtLineList().size() > 0) {
              for (EscmProposalmgmtLine objTechLine : objAttribute.getEscmProposalmgmt()
                  .getEscmProposalmgmtLineList()) {

                objTechLine.setNegotUnitPrice(objTechLine.getTechUnitPrice());
                objTechLine.setLineTotal(objTechLine.getTechLineTotal());
                objTechLine.setMovementQuantity(objTechLine.getTechLineQty());
                objTechLine.setPEENegotUnitPrice(objTechLine.getTechUnitPrice());
                objTechLine.setPEELineTotal(objTechLine.getTechLineTotal());
                objTechLine.setPEEQty(objTechLine.getTechLineQty());

                // Added for task 8098
                if (objTechLine.getLineTotal().compareTo(BigDecimal.ZERO) != 0
                    && !objTechLine.isSummary()) {
                  if (objTechLine.getEscmProposalmgmt().getProposalType().equals("DR"))
                    objTechLine.setDiscountmount(objTechLine.getTechDiscountamt());
                  else {
                    BigDecimal proposal_GrossPrice = objTechLine.getGrossUnitPrice();
                    if (objAttribute.getTEEEfinTaxMethod() != null
                        && objAttribute.getTEEEfinTaxMethod().isPriceIncludesTax())
                      final_NegPrice = objTechLine.getTechUnitPrice()
                          .add(objTechLine.getTEELineTaxamt().divide(objTechLine.getTechLineQty(),
                              2, RoundingMode.HALF_UP));
                    else
                      final_NegPrice = objTechLine.getTechUnitPrice();

                    if (proposal_GrossPrice.compareTo(final_NegPrice) > 0) {
                      // discount_Amount = proposal_GrossPrice.subtract(final_NegPrice)
                      // .multiply(objTechLine.getTechLineQty());
                      discount_Amount = objTechLine.getProposalDiscountAmount()
                          .add(objTechLine.getTechDiscountamt());

                    }
                    // only update the proposal discount when there is change in TEE discount amount
                    // else keep proposal discount amount
                    if (objTechLine.getTechDiscountamt().compareTo(BigDecimal.ZERO) > 0) {
                      objTechLine.setDiscountmount(discount_Amount);
                    } else {
                      objTechLine.setDiscountmount(objTechLine.getProposalDiscountAmount());
                    }

                  }
                  if (objTechLine.getDiscountmount().compareTo(BigDecimal.ZERO) > 0) {
                    if (objTechLine.getEscmProposalmgmt().getProposalType().equals("DR"))
                      objTechLine.setDiscount(objTechLine.getTechDiscount());
                    else {
                      if (objAttribute.getTEEEfinTaxMethod() != null
                          && objAttribute.getTEEEfinTaxMethod().isPriceIncludesTax())
                        discAmt = objTechLine.getGrossUnitPrice().subtract(
                            objTechLine.getTechUnitPrice().add(objTechLine.getTEELineTaxamt()
                                .divide(objTechLine.getTechLineQty(), 2, RoundingMode.HALF_UP)));
                      else
                        discAmt = objTechLine.getGrossUnitPrice()
                            .subtract(objTechLine.getTechUnitPrice());

                      BigDecimal disc = (discAmt.divide(objTechLine.getGrossUnitPrice(), 15,
                          RoundingMode.HALF_UP)).multiply(new BigDecimal(100));
                      objTechLine.setDiscount(disc);
                    }
                  } else {
                    objTechLine.setDiscount(BigDecimal.ZERO);
                  }
                } else {
                  objTechLine.setDiscountmount(BigDecimal.ZERO);
                  objTechLine.setDiscount(BigDecimal.ZERO);
                }

                // Removed from task 8098
                // objTechLine.setDiscount(objTechLine.getTechDiscount());
                //
                // if (objTechLine.getTechDiscountamt().compareTo(new BigDecimal(0)) > 0) {
                // objTechLine.setDiscountmount(objTechLine.getTechDiscountamt());
                // } else {
                // objTechLine.setDiscountmount(BigDecimal.ZERO);
                // }

                // end task 8098
                objTechLine.setTaxAmount(objTechLine.getTEELineTaxamt());
                objTechLine.setPEELineTaxamt(objTechLine.getPEELineTaxamt());
                // while changing unitprice then also it should consider as discount
                if (!objTechLine.isSummary()) {
                  if (objTechLine.getGrossUnitPrice()
                      .compareTo(objTechLine.getTechUnitPrice()) == 0) {
                    grossPrice = grossPrice.add(
                        (objTechLine.getTechLineQty().multiply(objTechLine.getTechUnitPrice())));
                    netPrice = netPrice.add(objTechLine.getLineTotal());
                  } else {
                    grossPrice = grossPrice
                        .add(objTechLine.getNetprice().multiply(objTechLine.getMovementQuantity()));
                    if (objAttribute.getTEEEfinTaxMethod() != null
                        && !objAttribute.getTEEEfinTaxMethod().isPriceIncludesTax()) {
                      netPrice = netPrice.add(
                          objTechLine.getTechUnitPrice().multiply(objTechLine.getTechLineQty()));
                    } else {
                      netPrice = netPrice.add(objTechLine.getLineTotal());
                    }

                  }

                  calculated_Discount_Amount = calculated_Discount_Amount.add(objTechLine
                      .getProposalDiscountAmount().add(objTechLine.getTechDiscountamt()));

                }
              }
              // grossPrice = objAttribute.getNegotiatedPrice();

              // changed 6607
              // grossPrice = objAttribute.getEscmProposalmgmt().getNetPrice();

              //
              if (grossPrice.compareTo(netPrice) > 0) {
                // objAttribute.setProsalDiscountamt(grossPrice.subtract(netPrice));
                objAttribute.setProsalDiscountamt(calculated_Discount_Amount);

              } else {
                objAttribute.setProsalDiscountamt(BigDecimal.ZERO);
              }
              if (objAttribute.getProsalDiscountamt().compareTo(BigDecimal.ZERO) > 0) {
                objAttribute.setProsalDiscount(
                    (objAttribute.getProsalDiscountamt().multiply(new BigDecimal("100")))
                        .divide(grossPrice, 2, RoundingMode.HALF_UP));
              } else {
                objAttribute.setProsalDiscount(BigDecimal.ZERO);
              }
              log.debug("discount : " + objAttribute.getProsalDiscount());
              log.debug("discount : " + objAttribute.getProsalDiscountamt());

              objAttribute.getEscmProposalmgmt()
                  .setDiscountForTheDeal(objAttribute.getProsalDiscount());
              objAttribute.getEscmProposalmgmt()
                  .setDiscountAmount(objAttribute.getProsalDiscountamt());
            }
          }
        }

        // update the header status
        techevlevent.setUpdated(new java.util.Date());
        techevlevent.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;
        if (techevlevent.getRole() != null) {
          objCreatedRole = techevlevent.getRole();
        }

        // solving approval alerts - Task No:7618
        AlertUtility.solveAlerts(techevlevent.getId());

        // get alert recipients - Task No:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

        // check and insert recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }

        forwardDao.getAlertForForwardedUser(techevlevent.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, techevlevent.getEventNo(), Lang, vars.getRole(),
            techevlevent.getEUTForward(), Resource.TECHNICAL_EVALUATION_EVENT, alertReceiversMap);
        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.scm.properties.Resource
            .getProperty("scm.techevaluation.event.approved", Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(techevlevent.getId(), techevlevent.getEventNo(), "",
            techevlevent.getCreatedBy().getId(), techevlevent.getClient().getId(), Description,
            "NEW", alertWindow, "scm.techevaluation.event.approved", Constants.GENERIC_TEMPLATE);
        techevlevent.setAction("RE");
        techevlevent.setStatus("CO");
        techevlevent.setEUTNextRole(null);
        count = 1;
        result.put("count", count);
        result.put("errormsg", "null");

      }
      OBDal.getInstance().save(techevlevent);
      strTechEventId = techevlevent.getId();

      // insert approval history
      if (!StringUtils.isEmpty(strTechEventId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", strTechEventId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.TECHNICAL_EVL_EVENT_HISTORY);
        historyData.put("HeaderColumn", ApprovalTables.TECHNICAL_EVL_EVENT_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.TECHNICAL_EVL_EVENT_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBContext.setAdminMode();
      // Removing forwardRMI id
      if (techevlevent.getEUTForward() != null) {
        // Removing the Role Access given to the forwarded user
        // Update statuses draft the forward Record
        forwardReqMoreInfoDAO.setForwardStatusAsDraft(techevlevent.getEUTForward());
        // Removing Forward_Rmi id from transaction screens
        forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(techevlevent.getId(),
            Constants.TECHNICAL_EVALUATION_EVENT);

      }
      if (techevlevent.getEUTReqmoreinfo() != null) {
        // Update statuses draft the RMI Record
        forwardReqMoreInfoDAO.setForwardStatusAsDraft(techevlevent.getEUTReqmoreinfo());
        // access remove
        // Remove Forward_Rmi id from transaction screens
        forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(techevlevent.getId(),
            Constants.TECHNICAL_EVALUATION_EVENT);

      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.TECHNICAL_EVALUATION_EVENT);

    } /*
       * catch (Exception e) { log.error("Exception in updateHeaderStatus in Purchase Order: ", e);
       * OBDal.getInstance().rollbackAndClose(); }
       */
    catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log.debug("Exception in updateHeaderStatus in Purchase Order:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } /*
       * finally { OBContext.restorePreviousMode(); }
       */
    return result;
  }

  /**
   * check approver is direct approver or not
   * 
   * @param techeventId
   * @param roleId
   * @return
   */
  public static boolean isDirectApproval(String techeventId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(tecevnt.escm_technicalevl_event_id) from escm_technicalevl_event tecevnt join eut_next_role rl on "
          + "tecevnt.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and tecevnt.escm_technicalevl_event_id = ? and li.ad_role_id =?";

      ps = con.prepareStatement(query);
      ps.setString(1, techeventId);
      ps.setString(2, roleId);

      rs = ps.executeQuery();

      if (rs.next()) {
        if (rs.getInt("count") > 0)
          return true;
        else
          return false;
      } else
        return false;

    } catch (Exception e) {
      log.error("Exception in Technical Evaluation Event -isDirectApproval " + e.getMessage());
      return false;
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
  }
}
