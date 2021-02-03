package sa.elm.ob.scm.ad_process.OpenEnvlopCommitee;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertUtilityDAO;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 */

public class OpenEnvlopCommiteeAction extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(OpenEnvlopCommiteeAction.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean count = false;
    boolean errorFlag = false;
    BigDecimal netPrice = BigDecimal.ZERO;
    boolean isPeriodOpen = false;
    int propCount = 0, peeCount = 0;
    try {
      OBContext.setAdminMode();
      final String openenvid = (String) bundle.getParams().get("Escm_Openenvcommitee_ID")
          .toString();
      Escmopenenvcommitee openenvcommitee = OBDal.getInstance().get(Escmopenenvcommitee.class,
          openenvid);
      List<EscmProposalMgmt> proposalList = new ArrayList<EscmProposalMgmt>();
      String Status = openenvcommitee.getAlertStatus();
      String action = openenvcommitee.getEscmDocaction();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = openenvcommitee.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String description = null;
      String alertWindow = sa.elm.ob.scm.util.AlertWindow.TeeUser;
      String windowId = "62E42B7D4CF74BF08532F18D5AF084FD";
      if (Status.equals("DR")) {
        count = Chkuser(openenvid, userId, roleId, openenvid);
        if (!count) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_OpenEnvCommitee@");
          bundle.setResult(result);
          return;
        }
      }
      // Restricted to submit the record of Open Envelope Event with Net Price zero
      if (Status.equals("DR")) {
        for (EscmProposalAttribute line : openenvcommitee.getEscmProposalAttrList()) {
          netPrice = netPrice.add(line.getNetPrice());
        }
        if (netPrice != null && (netPrice.compareTo(BigDecimal.ZERO) <= 0)) {
          OBError result = OBErrorBuilder.buildMessage(null, "Error",
              "@ESCM_OpenEnvEvent_NetPrice@");
          bundle.setResult(result);
          return;
        }
      }

      if (Status.equals("DR")) {
        // Check contract catg cannot be empty
        if (openenvcommitee.getContractType() == null) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_ContractCatgCantBeEmpty@");
          bundle.setResult(result);
          return;
        }
        if (openenvcommitee.getBidNo() != null) {
          String contCategId = null;
          boolean diffContCat = false;
          OBQuery<EscmProposalMgmt> proposalQry = OBDal.getInstance()
              .createQuery(EscmProposalMgmt.class, " as e where e.escmBidmgmt.id=:bidId and"
                  + " e.contractType is not null  order by e.contractType asc ");
          proposalQry.setNamedParameter("bidId", openenvcommitee.getBidNo().getId());
          proposalList = proposalQry.list();
          if (proposalList.size() > 0) {
            for (EscmProposalMgmt proposal : proposalList) {
              if (contCategId != null && !contCategId.equals(proposal.getContractType().getId())) {
                diffContCat = true;
                break;
              } else
                contCategId = proposal.getContractType().getId();
            }
            if (diffContCat) {
              errorFlag = true;
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Escm_PropDiffContCategory@");
              bundle.setResult(result);
              return;
            } else {
              if (contCategId != null && openenvcommitee.getContractType() != null
                  && !openenvcommitee.getContractType().getId().equals(contCategId)) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_HeadLineContCatDiff@");
                bundle.setResult(result);
                return;
              }
            }
          }
        }
      }

      // check net price is zero
      for (EscmProposalAttribute att : openenvcommitee.getEscmProposalAttrList()) {
        if (att.getNetPrice().compareTo(BigDecimal.ZERO) == 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "Error", "@ESCM_OEE_NetAmt@");
          bundle.setResult(result);
          return;
        }
      }

      // Check transaction period is opened or not
      if (Status.equals("DR")) {
        isPeriodOpen = Utility.checkOpenPeriod(openenvcommitee.getTodaydate(), orgId, clientId);
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
        for (EscmProposalAttribute attr : openenvcommitee.getEscmProposalAttrList()) {
          EscmProposalMgmt proposal = attr.getEscmProposalmgmt();
          if (!proposal.getProposalstatus().equals("SUB")) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_ProposalStatusCheck@");
            bundle.setResult(result);
            return;
          }
        }
        if (openenvcommitee.getBidNo() != null) {
          peeCount = openenvcommitee.getEscmProposalAttrList().size();
          OBQuery<EscmProposalMgmt> prop = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
              " as e where e.escmBidmgmt.id= :BidId and e.proposalstatus in ('SUB')");
          prop.setNamedParameter("BidId", openenvcommitee.getBidNo());
          propCount = prop.list().size();
          if (peeCount != propCount) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_ProposalStatusCheck@");
            bundle.setResult(result);
            return;
          }
        }
      }
      if (Status.equals("DR")) {
        openenvcommitee.setAlertStatus("CO");
        openenvcommitee.setEscmDocaction("RE");
        for (EscmProposalAttribute line : openenvcommitee.getEscmProposalAttrList()) {
          OBQuery<EscmProposalMgmt> proposalmgmt = OBDal.getInstance()
              .createQuery(EscmProposalMgmt.class, " as e where e.id=:proposalId");
          proposalmgmt.setNamedParameter("proposalId", line.getEscmProposalmgmt().getId());
          log.debug("transaction" + proposalmgmt.getWhereAndOrderBy());
          proposalList = proposalmgmt.list();
          if (proposalList.size() > 0) {
            EscmProposalMgmt promgmt = proposalList.get(0);
            promgmt.setProposalstatus("OPE");
            promgmt.setNetPrice(line.getNetPrice());
            // eve promgmt.setDiscountAmount(line.getDiscountAmount());
            // promgmt.setDiscountForTheDeal(line.getDiscount());
            // for (EscmProposalmgmtLine ln : promgmt.getEscmProposalmgmtLineList()) {
            // ln.setDiscount(line.getDiscount());
            //
            // }
          }

        }

        // While submitting the open envelope event, check bg status is not completed, if yes then
        // check bgamount and set status as completed
        for (EscmProposalAttribute line : openenvcommitee.getEscmProposalAttrList()) {
          for (Escmbankguaranteedetail detail : line.getEscmBankguaranteeDetailList()) {
            String status = "";
            ESCMBGWorkbench bgworkbenchObj = detail.getEscmBgworkbench();
            if (bgworkbenchObj.getBidNo() != null
                && !bgworkbenchObj.getBidNo().getBidtype().equals("DR")) {
              errorFlag = BGWorkbenchDAO.chkTotBGAmtCalAmtinOEE(bgworkbenchObj.getId());
              if (!errorFlag) {
                bgworkbenchObj.setBghdstatus("CO");
                bgworkbenchObj.setBgaction("RE");
                bgworkbenchObj.setTransactionDate(detail.getUpdated());
                if (bgworkbenchObj.getEscmBankguaranteeDetailList().size() > 0) {
                  for (Escmbankguaranteedetail bgdet : bgworkbenchObj
                      .getEscmBankguaranteeDetailList()) {
                    bgdet.setBgstatus("ACT");
                    OBDal.getInstance().save(bgdet);
                  }
                }
                OBDal.getInstance().save(bgworkbenchObj);
                OBDal.getInstance().flush();

              } else {
                if (bgworkbenchObj.getType().equals("IBG"))
                  status = "@ESCM_BGTotBGAmtComCalAmtIB@";
                else
                  status = "@ESCM_BGTotBGAmtComCalAmtFB@";

                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "Error", status);
                bundle.setResult(result);
                return;
              }

            }
          }
        }
        // if open envlop is completed then need to send an alert to TEE User
        if (openenvcommitee.getAlertStatus().equals("CO")) {

          description = sa.elm.ob.scm.properties.Resource
              .getProperty("scm.teeuser.alert", vars.getLanguage())
              .concat("" + openenvcommitee.getBidNo().getBidno());
          AlertUtility.alertInsertBasedonPreference(openenvcommitee.getId(),
              openenvcommitee.getEventno(), "ESCM_TEE_User", openenvcommitee.getClient().getId(),
              description, "NEW", alertWindow, "scm.teeuser.alert", Constants.GENERIC_TEMPLATE,
              windowId, null);
        }
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      }
      if (Status.equals("CO") && action.equals("RE")) {
        /*
         * check if proposal is used in proposal evaluation. if used then should not allow to
         * reactivate.
         */
        for (EscmProposalAttribute line : openenvcommitee.getEscmProposalAttrList()) {
          if (!line.getEscmProposalmgmt().getProposalstatus().equals("OPE")) {
            errorFlag = true;
            OBError result = OBErrorBuilder.buildMessage(null, "Error",
                "@Escm_openenv_reactivate@");
            bundle.setResult(result);
            return;
          }
        }
        /*
         * check if proposal is used in TEE . if used then should not allow to reactivate.
         */
        OBQuery<EscmTechnicalevlEvent> techEvlevent = OBDal.getInstance()
            .createQuery(EscmTechnicalevlEvent.class, " as e where e.bidNo.id=:bidId");
        techEvlevent.setNamedParameter("bidId", openenvcommitee.getBidNo().getId());
        techEvlevent.setMaxResult(1);
        if (techEvlevent.list().size() > 0) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "Error", "@ESCM_OpenEnvCantRecTEE@");
          bundle.setResult(result);
          return;
        }

        for (EscmProposalAttribute line : openenvcommitee.getEscmProposalAttrList()) {
          OBQuery<ESCMBGWorkbench> bgworkbench = OBDal.getInstance().createQuery(
              ESCMBGWorkbench.class,
              " as e where e.documentNo.id=:proposalId and e.bghdstatus ='CO'");
          bgworkbench.setNamedParameter("proposalId", line.getEscmProposalmgmt().getId());

          if (bgworkbench.list().size() > 0) {
            errorFlag = true;
            OBError result = OBErrorBuilder.buildMessage(null, "Error", "@ESCM_OpenEnvCantRecBG@");
            bundle.setResult(result);
            return;
          }
        }
        if (errorFlag == false) {
          openenvcommitee.setAlertStatus("DR");
          openenvcommitee.setEscmDocaction("CO");
          for (EscmProposalAttribute line : openenvcommitee.getEscmProposalAttrList()) {
            OBQuery<EscmProposalMgmt> proposalmgmt = OBDal.getInstance().createQuery(
                EscmProposalMgmt.class, " as e where e.id=:proposalId and proposalstatus='OPE'");
            proposalmgmt.setNamedParameter("proposalId", line.getEscmProposalmgmt().getId());
            log.debug("transaction" + proposalmgmt.getWhereAndOrderBy());
            if (proposalmgmt.list().size() > 0) {
              EscmProposalMgmt promgmt = proposalmgmt.list().get(0);
              promgmt.setProposalstatus("SUB");
              promgmt.setNetPrice(BigDecimal.ZERO);
              // eve promgmt.setDiscountAmount(BigDecimal.ZERO);
              // promgmt.setDiscountForTheDeal(BigDecimal.ZERO);
              // for (EscmProposalmgmtLine ln : promgmt.getEscmProposalmgmtLineList()) {
              // ln.setDiscount(BigDecimal.ZERO);
              // ln.setDiscountmount(BigDecimal.ZERO);
              // ln.setNegotUnitPrice(BigDecimal.ZERO);
              // ln.setNetprice(BigDecimal.ZERO);
              // ln.setLineTotal(BigDecimal.ZERO);
              // ln.setGrossUnitPrice(BigDecimal.ZERO);
              // }

            }
          }
          // make alert as solved
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow
                  + "' order by e.creationDate desc");
          queryAlertRule.setMaxResult(1);
          if (queryAlertRule.list().size() > 0) {
            String alertRuleId = queryAlertRule.list().get(0).getId();
            AlertUtilityDAO.deleteAlertPreference(openenvid, alertRuleId);
          }
          OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Po_React_succ@");
          bundle.setResult(result);
          return;
        }
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in OpenEnvlopCommiteeAction:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private boolean Chkuser(String RequestId, String userid, String roleid, String openenvid) {
    log.debug("RequestId" + RequestId);
    log.debug("userid" + userid);
    log.debug("roleid" + roleid);

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    Boolean value = false;
    try {
      query = "select 'Y' from escm_openenvcommitee where exists (select 1 from escm_openenvcommitee "
          + "where escm_openenvcommitee_id = ? and createdby = ? and ad_role_id=? ) "
          + "and escm_openenvcommitee_id = ?";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, RequestId);
        ps.setString(2, userid);
        ps.setString(3, roleid);
        ps.setString(4, openenvid);
        rs = ps.executeQuery();
        log.debug("query" + query.toString());
        if (rs.next()) {
          value = true;
        } else
          return false;
      }
    } catch (Exception e) {
      log.error("Exception in Chkuser " + e.getMessage());
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
    return value;
  }
}
