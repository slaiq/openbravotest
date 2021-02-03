package sa.elm.ob.scm.ad_process.TechnicalEvaluationEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAOImpl;
import sa.elm.ob.scm.ad_process.TechnicalEvaluationEvent.dao.TechnicalEvaluationEventProcessDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertUtilityDAO;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Divya on 03/01/2018
 */

public class TechnicalEvaluationEventProcess extends DalBaseProcess {

  /**
   * This servlet class was responsible for Technical Evaluation Event Process.
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(TechnicalEvaluationEventProcess.class);
  private static final String OPENED = "OPE";

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false;
    String Proposallist = null, message = "";
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();

    try {
      OBContext.setAdminMode();
      // declaring variables
      final String techEvaleventId = (String) bundle.getParams().get("Escm_Technicalevl_Event_ID")
          .toString();
      // getting Technical event object by using Technical Evaluation Event Id
      EscmTechnicalevlEvent event = OBDal.getInstance().get(EscmTechnicalevlEvent.class,
          techEvaleventId);

      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = event.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      String description = null;
      String alertWindow = sa.elm.ob.scm.util.AlertWindow.PeeUser;
      String windowId = "006832D5A20E45289F191D08949D252B";
      NextRoleByRuleVO nextApproval = null;
      String Lang = vars.getLanguage();

      Connection conn = OBDal.getInstance().getConnection();
      Boolean chkRoleIsInDocRul = false;
      boolean allowUpdate = false;
      boolean allowDelegation = false, allowApprove = false;
      String appstatus = "";
      Date currentDate = new Date();
      boolean isPeriodOpen = false;
      BigDecimal total_gross_price = BigDecimal.ZERO;
      BigDecimal total_proposal_actual_price = BigDecimal.ZERO;
      // submit process start

      // Approval flow start
      // with final qty 0 TEE should not allow to submit
      for (EscmProposalAttribute att : event.getEscmProposalAttrList()) {
        for (EscmProposalmgmtLine ln : att.getEscmProposalmgmt().getEscmProposalmgmtLineList()) {
          if (ln.getTechLineQty().compareTo(BigDecimal.ZERO) == 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Qty_Zero@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // check proposal total amount is not zero
      for (EscmProposalAttribute att : event.getEscmProposalAttrList()) {
        if (att.getTechNegotiatedPrice().compareTo(BigDecimal.ZERO) == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_TEE_Amt_zero@");
          bundle.setResult(result);
          return;
        }
      }

      if (!event.getCreatedBy().getId().equals(vars.getUser()) && event.getAction().equals("CO")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_Role_NotFirstRole_submit@");
        bundle.setResult(result);
        return;
      }
      // Check transaction period is opened or not
      if (event.getAction().equals("CO")) {
        isPeriodOpen = Utility.checkOpenPeriod(event.getDateH(), orgId, clientId);
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      }

      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // approves the record without refreshing the page
      if (event.getEUTForward() != null) {
        allowApprove = forwardReqMoreInfoDAO.allowApproveReject(event.getEUTForward(), userId,
            roleId, Resource.TECHNICAL_EVALUATION_EVENT);
      }
      if (event.getEUTReqmoreinfo() != null
          || ((event.getEUTForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // Task no:8327 checking tax is defined or not , if not then throw error
      if (event.getStatus().equals("DR") || event.getStatus().equals("ESCM_REJ")) {
        for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
          if (!attr.isTEEIstax()
              || (attr.isTEEIstax() && attr.getTEETotalTaxamt().compareTo(BigDecimal.ZERO) == 0)) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_TEETaxMandatory@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // check role is present in document rule or not
      if (event.getStatus().equals("DR") || event.getStatus().equals("ESCM_REJ")
          || event.getStatus().equals("ESCM_IP")) {
        if (event.getStatus().equals("DR") || event.getStatus().equals("ESCM_REJ")) {
          chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
              clientId, orgId, userId, roleId, Resource.TECHNICAL_EVALUATION_EVENT,
              BigDecimal.ZERO);
          if (!chkRoleIsInDocRul) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_RoleIsNotIncInDocRule@");
            bundle.setResult(result);
            return;
          }

          // check atleast one line having in Proposal tab while submit
          if (event.getEscmProposalAttrList().size() == 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_Proposal_AtleaseLine@");
            bundle.setResult(result);
            return;
          }

          // check rank and Proposal status is filled or not while submit
          if (event.getEscmProposalAttrList().size() > 0) {

            for (EscmProposalAttribute att : event.getEscmProposalAttrList()) {
              if (att.getTechevalDecision() == null) {
                errorFlag = true;
                Proposallist = (Proposallist == null ? att.getEscmProposalmgmt().getProposalno()
                    : Proposallist + "," + att.getEscmProposalmgmt().getProposalno());
              }
            }
            if (errorFlag) {
              message = OBMessageUtils.messageBD("ESCM_ProposalAttr_TechDecision");
              message = message.replace("%", Proposallist);
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", message);
              bundle.setResult(result);
              return;
            }
          }

          /*
           * // check role is present in document rule or not based on amount chkRoleIsInDocRul =
           * UtilityDAO.chkRoleIsInDocRulBasedonAmount( OBDal.getInstance().getConnection(),
           * clientId, orgId, userId, roleId, Resource.TECHNICAL_EVALUATION_EVENT, BigDecimal.ZERO);
           * if (!chkRoleIsInDocRul) { errorFlag = true; OBDal.getInstance().rollbackAndClose();
           * OBError result = OBErrorBuilder.buildMessage(null, "error",
           * "@ESCM_RoleIsNotIncInDocRule@"); bundle.setResult(result); return; }
           */

        }

        // check current role associated with document rule for approval flow
        if (!event.getStatus().equals("DR") && !event.getStatus().equals("ESCM_REJ")) {
          if (event.getEUTNextRole() != null) {
            java.util.List<EutNextRoleLine> li = event.getEUTNextRole().getEutNextRoleLineList();
            for (int i = 0; i < li.size(); i++) {
              String role = li.get(i).getRole().getId();
              if (roleId.equals(role)) {
                allowUpdate = true;
              }
            }
          }
          if (event.getEUTNextRole() != null) {
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
                DocumentTypeE.TEE.getDocumentTypeCode());

            /*
             * sql = ""; Connection con = OBDal.getInstance().getConnection(); PreparedStatement st
             * = null; ResultSet rs1 = null; sql =
             * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
             * + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_123'";
             * st = con.prepareStatement(sql); rs1 = st.executeQuery(); while (rs1.next()) { String
             * roleid = rs1.getString("ad_role_id"); if (roleid.equals(roleId)) { allowDelegation =
             * true; break; } }
             */
          }
          if (!allowUpdate && !allowDelegation) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_AlreadyPreocessed_Approved@");
            bundle.setResult(result);
            return;
          }
        }

        // check already approved or not
        if ((!vars.getUser().equals(event.getCreatedBy().getId()))
            && (event.getStatus().equals("DR"))) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }

        // Throw error if any one line need to Calculate the tax.
        for (EscmProposalAttribute attr : event.getEscmProposalAttrList()) {
          if (attr.isTEEIstax()) {
            Optional<EscmProposalmgmtLine> rslt = attr.getEscmProposalmgmt()
                .getEscmProposalmgmtLineList().stream()
                .filter(x -> x.getTechLineTotal().compareTo(BigDecimal.ZERO) > 0
                    && x.getTEELineTaxamt().compareTo(BigDecimal.ZERO) == 0)
                .findAny();
            if (rslt.isPresent()) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_NeedToCalTax@");
              bundle.setResult(result);
              return;
            }
          }
        }

        if (!errorFlag) {
          // set value for approval history status
          if ((event.getStatus().equals("DR") || event.getStatus().equals("ESCM_REJ"))
              && event.getAction().equals("CO")) {
            appstatus = "SUB";
          } else if (event.getStatus().equals("ESCM_IP") && event.getAction().equals("AP")) {
            appstatus = "AP";
          }

          // update next role
          JSONObject upresult = TechnicalEvaluationEventProcessDAO.updateHeaderStatus(conn,
              clientId, orgId, roleId, userId, event, appstatus, comments, currentDate, vars,
              nextApproval, Lang, bundle);
          if (upresult != null) {
            // if role does not associate with any user then dont allow to process for next approve
            if (upresult.has("count") && upresult.getInt("count") == -2) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  upresult.getString("errormsg"));
              bundle.setResult(result);
              return;
            }
            // approve success message
            else if (upresult.has("count") && upresult.getInt("count") == 2) {
              OBError result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_TEE_Approve@");
              bundle.setResult(result);
              return;
            }
            // submit sucess message
            else if (upresult.has("count") && upresult.getInt("count") == 1) {
              // send an alert to pee user when tee is completed
              description = sa.elm.ob.scm.properties.Resource
                  .getProperty("scm.peeuser.alert", vars.getLanguage())
                  .concat("" + event.getBidNo().getBidno());
              AlertUtility.alertInsertBasedonPreference(event.getId(), event.getEventNo(),
                  "ESCM_PEE_User", event.getClient().getId(), description, "NEW", alertWindow,
                  "scm.peeuser.alert", Constants.GENERIC_TEMPLATE, windowId, null);
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@ESCM_TEE_SubmitSuccess@");
              bundle.setResult(result);
              return;
            }
          }
        }
      }
      // reactive process start
      else if (event.getStatus().equals("CO")) {
        EscmProposalMgmt promgmt = null;

        // chk already reactivated or not
        if (event.getStatus().equals("DR")) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
        // update amount in line
        if (event.getEscmProposalAttrList().size() > 0) {
          for (EscmProposalAttribute objAttribute : event.getEscmProposalAttrList()) {

            // update proposal status
            if (objAttribute.getEscmProposalmgmt() != null) {
              promgmt = objAttribute.getEscmProposalmgmt();
              promgmt.setProposalstatus(OPENED);

            }
            if (objAttribute.getEscmProposalmgmt() != null
                && objAttribute.getEscmProposalmgmt().getEscmProposalmgmtLineList().size() > 0) {
              for (EscmProposalmgmtLine objTechLine : objAttribute.getEscmProposalmgmt()
                  .getEscmProposalmgmtLineList()) {
                if (objTechLine.getProposalDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                  BigDecimal netPrice = (((objTechLine.getGrossUnitPrice()
                      .multiply(objTechLine.getBaselineQuantity()))
                          .subtract(objTechLine.getProposalDiscountAmount()))
                              .divide(objTechLine.getBaselineQuantity())).setScale(2,
                                  RoundingMode.HALF_UP);
                  objTechLine.setNegotUnitPrice(netPrice);
                } else {
                  objTechLine.setNegotUnitPrice(objTechLine.getNetprice());

                }

                objTechLine.setLineTotal(
                    objTechLine.getBaselineQuantity().multiply(objTechLine.getNetprice())
                        .subtract(promgmt.getProposalDiscountAmount()));
                objTechLine.setMovementQuantity(objTechLine.getBaselineQuantity());

                // Added for task 8098
                // Find out whether discount applied on Proposal Management before TEE
                // if the discount applied on proposal level then apply the same

                objTechLine.setDiscount(objTechLine.getProposalDiscount());
                objTechLine.setDiscountmount(objTechLine.getProposalDiscountAmount());

                objTechLine.setPEENegotUnitPrice(objTechLine.getNetprice());
                objTechLine.setPEELineTotal(
                    objTechLine.getBaselineQuantity().multiply(objTechLine.getNetprice()));
                objTechLine.setPEEQty(objTechLine.getBaselineQuantity());
                objTechLine.setUnittax(BigDecimal.ZERO);

                if (!objTechLine.isSummary()) {

                  BigDecimal gross_price = objTechLine.getGrossUnitPrice()
                      .multiply(objTechLine.getBaselineQuantity());
                  total_gross_price = total_gross_price.add(gross_price);
                }
              }
            }

            objAttribute.setProsalDiscountamt(promgmt.getProposalDiscountAmount());

            objAttribute.setProsalDiscount(promgmt.getProposalDiscount());

            if (objAttribute.getEscmProposalmgmt() != null) {
              objAttribute.getEscmProposalmgmt()
                  .setDiscountForTheDeal(objAttribute.getProsalDiscount());
              objAttribute.getEscmProposalmgmt()
                  .setDiscountAmount(objAttribute.getProsalDiscountamt());
              objAttribute.getEscmProposalmgmt().setTotpoafterchngprice(
                  total_gross_price.subtract(objAttribute.getProsalDiscountamt()));

            }

          }
        }

        // chk already reactivated or not
        OBQuery<ESCMProposalEvlEvent> proevlevent = OBDal.getInstance()
            .createQuery(ESCMProposalEvlEvent.class, " as e where e.bidNo.id=:bidId");
        proevlevent.setNamedParameter("bidId", event.getBidNo().getId());
        proevlevent.setMaxResult(1);
        if (proevlevent.list().size() > 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidUsedinPEE@");
          bundle.setResult(result);
          return;
        }

        // Remove TEE Tax and Update Proposal Tax details

        ProposalTaxCalculationDAO dao = new ProposalTaxCalculationDAOImpl();
        DecimalFormat euroRelationFmt = org.openbravo.erpCommon.utility.Utility.getFormat(vars,
            "euroRelation");
        Integer decimalFormat = euroRelationFmt.getMaximumFractionDigits();
        event.getEscmProposalAttrList().forEach(attr -> {
          if (attr.getEscmProposalmgmt() != null && attr.isTEEIstax()
              && attr.getEscmProposalmgmt().isTaxidentify()) {
            EscmProposalMgmt proposalMgmt = attr.getEscmProposalmgmt();
            dao.insertTaxAmount(proposalMgmt, decimalFormat);
          }
          if (attr.getEscmProposalmgmt() != null && attr.isTEEIstax()
              && !attr.getEscmProposalmgmt().isTaxidentify()) {
            EscmProposalMgmt proposalMgmt = attr.getEscmProposalmgmt();
            proposalMgmt.setTaxLine(false);
            proposalMgmt.setEfinTaxMethod(null);
            proposalMgmt.setTotalTaxAmount(BigDecimal.ZERO);
          }
        });

        if (!errorFlag) {
          // update Proposal event status if we reactivate
          event.setUpdated(new java.util.Date());
          event.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          event.setStatus("DR");
          event.setAction("CO");
          OBDal.getInstance().save(event);

          // insert approval history
          if (!StringUtils.isEmpty(event.getId())) {
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", event.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.TECHNICAL_EVL_EVENT_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.TECHNICAL_EVL_EVENT_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.TECHNICAL_EVL_EVENT_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);

          }
          // move the alert to solvesection
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow
                  + "' order by e.creationDate desc");
          queryAlertRule.setMaxResult(1);
          if (queryAlertRule.list().size() > 0) {
            String alertRuleId = queryAlertRule.list().get(0).getId();
            AlertUtilityDAO.deleteAlertPreference(event.getId(), alertRuleId);
          }
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }

      } // reactive process end

    } catch (Exception e) {
      log.debug("Exception in TechnicalEvaluationEventProcess Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      if (OBContext.getOBContext().isInAdministratorMode())
        OBContext.restorePreviousMode();
    }
  }
}