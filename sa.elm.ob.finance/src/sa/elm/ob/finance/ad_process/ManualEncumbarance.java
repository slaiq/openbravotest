package sa.elm.ob.finance.ad_process;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
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
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 26/05/2016
 */

public class ManualEncumbarance extends DalBaseProcess {

  /**
   * Manual Encumbarance Transaction submit Tracking on manual Encumbarance History Window
   */
  private static final Logger log = LoggerFactory.getLogger(ManualEncumbarance.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    // Create carry Forward
    log.debug("entering into ManualEncumbarnce");
    try {
      OBContext.setAdminMode();
      String ManEncumId = (String) bundle.getParams().get("Efin_Budget_Manencum_ID");
      EfinBudgetManencum manEncumbarance = OBDal.getInstance().get(EfinBudgetManencum.class,
          ManEncumId);
      log.debug("DocStatus:" + manEncumbarance.getDocumentStatus());
      log.debug("DocAction:" + manEncumbarance.getAction());
      String DocStatus = manEncumbarance.getDocumentStatus();
      String DocAction = manEncumbarance.getAction();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = manEncumbarance.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      String header = null;
      Boolean allowUpdate = false;
      Boolean allowDelegation = false;
      Boolean indocumentrule = false;
      Boolean isValid = true;
      Date currentDate = new Date();
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      Boolean allowApprove = false;
      BigDecimal encumamt = manEncumbarance.getAmount();
      boolean isPeriodOpen = true;
      Boolean isHavingCreatedRole = false;

      User user = OBContext.getOBContext().getUser();
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;
      // Task #8198
      // check submitted role have the branch details or not
      if (DocStatus.equals("DR")) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() == null) {

          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RoleBranchNotDefine@");
          bundle.setResult(result);
          return;
        }
      }
      // find the submitted role org/branch details

      if (manEncumbarance.getNextRole() != null) {
        if (manEncumbarance.getEfinSubmittedRole() != null
            && manEncumbarance.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = manEncumbarance.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (manEncumbarance.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }

      if (manEncumbarance.getBudgetInitialization().getStatus().equals("CL")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;
      }
      // Pre - Close Year Validation
      if (manEncumbarance.getBudgetInitialization().isPreclose()) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;
      }

      // check current role is present in document rule or not
      if (!manEncumbarance.getDocumentStatus().equals("DR")
          && !manEncumbarance.getDocumentStatus().equals("RW")) {
        if (manEncumbarance.getNextRole() != null) {
          java.util.List<EutNextRoleLine> li = manEncumbarance.getNextRole()
              .getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (manEncumbarance.getNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
              DocumentTypeE.ENCUMBRANCE.getDocumentTypeCode());
          /*
           * String sql = ""; Connection conn = OBDal.getInstance().getConnection();
           * PreparedStatement st = null; ResultSet rs = null; sql =
           * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
           * + ActDate + "' and to_date >='" + ActDate + "'"; st = conn.prepareStatement(sql); rs =
           * st.executeQuery();
           * 
           * if (rs.next()) {
           * 
           * String roleid = rs.getString("ad_role_id"); if (roleid.equals(roleId)) {
           * allowDelegation = true; } }
           */
        }
        // If the record is Forwarded or given RMI then throw error when any other user tries to
        // approve the record without refreshing the page
        if (manEncumbarance.getEUTForwardReqmoreinfo() != null) {
          allowApprove = forwardDao.allowApproveReject(manEncumbarance.getEUTForwardReqmoreinfo(),
              userId, roleId, Resource.MANUAL_ENCUMBRANCE_RULE);
        }
        if (manEncumbarance.getEUTReqmoreinfo() != null
            || ((manEncumbarance.getEUTForwardReqmoreinfo() != null) && (!allowApprove))) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }

        if (!allowUpdate && !allowDelegation) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }
      }

      // throw the error message while 2nd user try to approve while 1st user already reworked that
      // record with same role
      // Removed for migrated Record
      // if ((!vars.getUser().equals(manEncumbarance.getCreatedBy().getId()))
      // && manEncumbarance.getDocumentStatus().equals("RW")) {
      // OBDal.getInstance().rollbackAndClose();
      // OBError result = OBErrorBuilder.buildMessage(null, "error",
      // "@Efin_AlreadyPreocessed_Approve@");
      // bundle.setResult(result);
      // return;
      // }

      if (manEncumbarance.getEfinCommitsDataload() == null
          && manEncumbarance.getEfinReservationDataload() == null) {
        // After Revoked by submiter if approver is try to Approve the same record then throw error

        isHavingCreatedRole = user.getADUserRolesList().stream().anyMatch(a -> a.getRole().getId()
            .equals(manEncumbarance.getRole() != null ? manEncumbarance.getRole().getId() : null));

        if (!isHavingCreatedRole && manEncumbarance.getDocumentStatus().equals("DR")) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_AlreadyPreocessed_Approve@");
          bundle.setResult(result);
          return;
        }
      }
      // Check if ManualEncumbrance is active or not
      if (manEncumbarance.isActive().equals(false)) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_ManEncum_Isactive@");
        bundle.setResult(result);
        return;
      }
      // Check if lines contains Zero values in amount
      OBQuery<EfinBudgetManencumlines> amountQuery = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          "as e where e.manualEncumbrance.id='" + ManEncumId + "' and e.amount <=0 ");
      if (amountQuery.list().size() > 0) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@EFin_Encumbarance_AmountZero@");
        bundle.setResult(result);
        return;
      }

      // Check transaction period is opened or not before submitting record
      if ("CO".equals(manEncumbarance.getAction())) {
        isPeriodOpen = Utility.checkOpenPeriod(manEncumbarance.getTransactionDate(),
            orgId.equals("0") ? vars.getOrg() : orgId, manEncumbarance.getClient().getId());
        if (!isPeriodOpen) {
          if (manEncumbarance.getDocumentStatus().equals("DR")) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
            bundle.setResult(result);
            return;
          }

          else {
            // check the status of record ,status is other than draft check current date and
            // accounting date year is
            // same then check period is open for the current date then allow by updating
            // the accounting date as current date,then do not
            // allow to submit

            if (UtilityDAO.getYearId(currentDate, clientId)
                .equals(UtilityDAO.getYearId(manEncumbarance.getTransactionDate(), clientId))) {
              isPeriodOpen = Utility.checkOpenPeriod(currentDate,
                  orgId.equals("0") ? vars.getOrg() : orgId, manEncumbarance.getClient().getId());

              if (!isPeriodOpen) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
                bundle.setResult(result);
                return;
              } else {
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                Date now = new Date();
                Date todaydate = dateFormat.parse(dateFormat.format(now));
                manEncumbarance.setAccountingDate(todaydate);
                manEncumbarance.setTransactionDate(todaydate);
              }

            } else {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
              bundle.setResult(result);
              return;
            }
          }

        }

      }
      // Check Statue of Each Encumbrance Lnes
      // If ErrorFlag Status Exists Stop the process
      /*
       * OBQuery<EfinBudgetManencumlines> LinesQuery = OBDal.getInstance().createQuery(
       * EfinBudgetManencumlines.class, "as e where e.manualEncumbrance.id='" + ManEncumId +
       * "' and e.errorflag='1'"); if (LinesQuery.list().size() > 0) {
       * OBDal.getInstance().rollbackAndClose(); String validation =
       * ManualEncumbaranceSubmitDAO.preValidation(ManEncumId, ActDate, DocStatus, hasApproval,
       * DocAction); if (!validation.equals("1")) { OBError result =
       * OBErrorBuilder.buildMessage(null, "error", "@Efin_ErroFlag_Encumbarance_lines@");
       * bundle.setResult(result); return; } }
       */

      // check common validation before approving or submitting the record before Reservedfund
      if (!manEncumbarance.isReservedfund()) {
        isValid = CommonValidations.checkValidations(ManEncumId, "Encumbrance",
            OBContext.getOBContext().getCurrentClient().getId(), "CO", false);
        log.debug("isValid:" + isValid);

        // if common validation return false throwing error
        if (!isValid) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Encum_Failure@");
          bundle.setResult(result);
          return;
        }
      }
      /*
       * Trigger changes for (EfinBudgetManencumlines encumLine :
       * manEncumbarance.getEfinBudgetManencumlinesList()) { OBQuery<EfinBudgetInquiry> budInq =
       * OBDal.getInstance().createQuery(EfinBudgetInquiry.class, "efinBudgetint.id='" +
       * manEncumbarance.getBudgetInitialization().getId() + "'"); if
       * (encumLine.getAccountingCombination().isEFINDepartmentFund()) { if (budInq.list() != null
       * && budInq.list().size() > 0) { budInqList = budInq.list(); for (EfinBudgetInquiry Enquiry :
       * budInqList) { if (encumLine.getAccountingCombination() ==
       * Enquiry.getAccountingCombination()) {
       * Enquiry.setEncumbrance(Enquiry.getEncumbrance().add(encumLine.getAmount()));
       * OBDal.getInstance().save(Enquiry); if (Enquiry.getParent() != null) {
       * Enquiry.getParent().setEncumbrance(
       * Enquiry.getParent().getEncumbrance().add(encumLine.getAmount())); allDept =
       * Enquiry.getParent(); OBDal.getInstance().save(Enquiry); } if (allDept.getParent() != null)
       * { allDept.getParent().setEncumbrance(
       * allDept.getParent().getEncumbrance().add(encumLine.getAmount()));
       * OBDal.getInstance().save(allDept); } } } } } else { OBQuery<EfinBudgetControlParam> bcp =
       * OBDal.getInstance() .createQuery(EfinBudgetControlParam.class, ""); if (bcp.list() != null
       * && bcp.list().size() > 0) { department =
       * bcp.list().get(0).getBudgetcontrolCostcenter().getId();
       * 
       * OBQuery<AccountingCombination> accountCombination = OBDal.getInstance().createQuery(
       * AccountingCombination.class, "account.id= '" + encumLine.getAccountElement().getId() + "'"
       * + " and businessPartner.id='" +
       * encumLine.getAccountingCombination().getBusinessPartner().getId() + "' " +
       * "and salesRegion.id='" + department + "' and project.id = '" +
       * encumLine.getAccountingCombination().getProject().getId() + "' " + "and salesCampaign.id='"
       * + encumLine.getAccountingCombination().getSalesCampaign().getId() + "' " +
       * "and activity.id='" + encumLine.getAccountingCombination().getActivity().getId() +
       * "' and stDimension.id='" + encumLine.getAccountingCombination().getStDimension().getId() +
       * "' " + "and ndDimension.id = '" +
       * encumLine.getAccountingCombination().getNdDimension().getId() + "' " +
       * "and organization.id = '" + encumLine.getAccountingCombination().getOrganization().getId()
       * + "'");
       * 
       * if (accountCombination.list() != null && accountCombination.list().size() > 0) {
       * AccountingCombination combination = accountCombination.list().get(0);
       * 
       * if (budInq.list() != null && budInq.list().size() > 0) { budInqList = budInq.list(); for
       * (EfinBudgetInquiry Enquiry : budInqList) { if (combination ==
       * Enquiry.getAccountingCombination()) {
       * Enquiry.setEncumbrance(Enquiry.getEncumbrance().add(encumLine.getAmount()));
       * OBDal.getInstance().save(Enquiry); if (Enquiry.getParent() != null) {
       * Enquiry.getParent().setEncumbrance(
       * Enquiry.getParent().getEncumbrance().add(encumLine.getAmount()));
       * OBDal.getInstance().save(Enquiry); } } } } } } } }
       */

      /*
       * if (manEncumbarance != null) { OBError result1 = OBErrorBuilder.buildMessage(null,
       * "success", "@Efin_ManEncum_Submit@"); bundle.setResult(result1); return; }
       */

      // Submit Process starts
      if (DocStatus.equals("DR") || DocStatus.equals("RW")) {
        indocumentrule = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(), clientId,
            submittedRoleOrgId, userId, roleId, Resource.MANUAL_ENCUMBRANCE_RULE, encumamt);
        if (manEncumbarance.getNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
              DocumentTypeE.ENCUMBRANCE.getDocumentTypeCode());
        }
        if (!indocumentrule && !allowDelegation) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_PurInv_CantComplete@");
          bundle.setResult(result);
          return;
        }
        manEncumbarance.setRole(OBContext.getOBContext().getRole());
        OBDal.getInstance().save(manEncumbarance);
      }
      if (indocumentrule || allowDelegation || allowUpdate) {
        // String validation = ManualEncumbaranceSubmitDAO.preValidation(ManEncumId, ActDate,
        // DocStatus, hasApproval, DocAction);

        // if (validation.equals("1")) {
        if ((DocStatus.equals("DR") || DocStatus.equals("RW")) && DocAction.equals("CO")) {
          // if (success.equals("1")) {
          if (manEncumbarance.getAction().equals("CO")
              && (manEncumbarance.getDocumentStatus().equals("DR")
                  || manEncumbarance.getDocumentStatus().equals("RW"))) {

            header = ManualEncumbaranceSubmitDAO.checkApprover(OBDal.getInstance().getConnection(),
                clientId, orgId, roleId, userId, ManEncumId, comments, vars);
            if (header.equals("NoManagerAssociatedWithRole")) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
              bundle.setResult(result);
              return;
            } else if (!StringUtils.isEmpty(header)) {
              OBError result = OBErrorBuilder.buildMessage(null, "success",
                  "@Efin_ManEncum_Submit@");
              bundle.setResult(result);
              return;
            }
          }
          // }
          /*
           * else { OBDal.getInstance().rollbackAndClose(); OBError result =
           * OBErrorBuilder.buildMessage(null, "error", "@Failure@"); bundle.setResult(result);
           * return; }
           */

        } else if (manEncumbarance.getAction().equals("AP")
            && manEncumbarance.getDocumentStatus().equals("WFA")) {

          header = ManualEncumbaranceSubmitDAO.checkApprover(OBDal.getInstance().getConnection(),
              clientId, orgId, roleId, userId, ManEncumId, comments, vars);
          if (header.equals("NoManagerAssociatedWithRole")) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
            bundle.setResult(result);
            return;
          } else if (!StringUtils.isEmpty(header)) {
            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@Efin_ManEncum_Approve@");
            bundle.setResult(result);
            return;
          }

        }
        // }
        /*
         * else if (validation.equals("0")) { PreparedStatement psm = null, psm2 = null; ResultSet
         * rst = null, rst2 = null; try { OBError result = OBErrorBuilder.buildMessage(null,
         * "error", "@Efin_Fundsavailabe_Negative@"); bundle.setResult(result); return;
         */
        /*
         * psm = con.prepareStatement(
         * "select efin_budget_manencumlines_id from efin_budget_manencumlines where efin_budget_manencum_id='"
         * + ManEncumId + "' and errorflag='1' and amount <= funds_available"); rst =
         * psm.executeQuery(); while (rst.next()) { EfinBudgetManencumlines lines =
         * OBDal.getInstance().get(EfinBudgetManencumlines.class,
         * rst.getString("efin_budget_manencumlines_id")); lines.setCheckingStatus("");
         * lines.setErrorflag("0"); OBDal.getInstance().save(lines); } OBDal.getInstance().flush();
         */
        /*
         * } catch (Exception e) { e.printStackTrace();
         * log.error("Exception in updating manencum line error flag", e.getMessage()); }
         */
        /*
         * psm2 = con.prepareStatement(
         * "select efin_budget_manencumlines_id from efin_budget_manencumlines where efin_budget_manencum_id='"
         * + ManEncumId + "' and errorflag='1' "); rst2 = psm2.executeQuery(); if (rst2.next()) {
         * OBDal.getInstance().rollbackAndClose(); OBError result =
         * OBErrorBuilder.buildMessage(null, "error", "@Efin_Fundsavailabe_Negative@");
         * bundle.setResult(result); return; } else { OBError result =
         * OBErrorBuilder.buildMessage(null, "success", "@Efin_ManEncum_Submit@");
         * bundle.setResult(result); return; }
         */
        // }
      } else {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Failure@");
        bundle.setResult(result);
        return;
      }

    } catch (Exception e) {
      e.printStackTrace();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /*
   * public static int changeEncumStatus(Connection con, String clientId, String orgId, String
   * roleId, String userId, String headerId, String manencumstatus) { AdvPaymentMngtDao dao = new
   * AdvPaymentMngtDao(); try { OBContext.setAdminMode(true);
   * 
   * OBQuery<EfinBudgetManencumlines> encumlines = OBDal.getInstance()
   * .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id ='" + headerId + "'"); if
   * (encumlines.list().size() > 0) { for (int i = 0; i < encumlines.list().size(); i++) {
   * EfinBudgetManencumlines list = encumlines.list().get(i);
   * 
   * final OBQuery<efinbudgetencum> qry = OBDal.getInstance().createQuery( efinbudgetencum.class,
   * " manualEncumbranceLines.id='" + list.getId() + "'"); if (qry.list().size() > 0) { for (int j =
   * 0; j < qry.list().size(); j++) { efinbudgetencum encumheader = qry.list().get(j);
   * encumheader.setUpdated(new java.util.Date());
   * encumheader.setUpdatedBy(dao.getObject(User.class, userId));
   * encumheader.setAppstatus(manencumstatus); OBDal.getInstance().save(encumheader); } } } }
   * OBDal.getInstance().flush(); OBDal.getInstance().commitAndClose();
   * 
   * } catch (Exception e) { log.error("Exception in insertManEncumHistory: ", e);
   * OBDal.getInstance().rollbackAndClose(); return 0; } finally { OBContext.restorePreviousMode();
   * } return 1; }
   */

}