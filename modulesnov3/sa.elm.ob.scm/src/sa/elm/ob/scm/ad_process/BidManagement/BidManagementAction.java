package sa.elm.ob.scm.ad_process.BidManagement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.ESCMProductContCatg;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbiddates;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.Escmbidsourceref;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementProcessDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementProcessDAOImpl;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class BidManagementAction extends DalBaseProcess {
  /**
   * This class is responsible for Bid Management DocActions
   */
  private static final Logger log = LoggerFactory.getLogger(BidManagementAction.class);
  private static String errorMsgs = null;
  private static NextRoleByRuleVO nextApproval = null;
  private static String bidMgmtWindowId = "E509200618424FD099BAB1D4B34F96B8";
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    Connection conn = OBDal.getInstance().getConnection();
    String appstatus = "";
    boolean errorFlag = false;
    boolean allowUpdate = false;
    boolean allowDelegation = false;
    boolean bidstatus = false; // , initialBGPresent = false;
    try {
      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("Escm_Bidmgmt_ID").toString();
      EscmBidMgmt bidmgmt = Utility.getObject(EscmBidMgmt.class, receiptId);
      String DocStatus = bidmgmt.getBidappstatus();
      String DocAction = bidmgmt.getEscmDocaction();
      boolean checkEncumbranceAmountZero = false;
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = bidmgmt.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      Date currentDate = new Date();
      int count = 0;
      String errorMsg = "";
      String preferenceValue = "";
      // encumbrance
      JSONObject resultEncum = null;
      Boolean chkRoleIsInDocRul, chkSubRolIsInFstRolofDR = false;
      Boolean fromPR = false, mixedencumbrance = false;
      @SuppressWarnings("unused")
      Boolean isSkipPR = false;
      String comments = (String) bundle.getParams().get("notes").toString();
      Boolean isPeriodOpen = false;

      // Checking contract category is empty or not
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("ESCM_RA")) {
        if (bidmgmt.getContractType() == null) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_ContractCatgCantBeEmpty@");
          bundle.setResult(result);
          return;
        }
      }

      // Budget Definition is Pre closed validation
      if (bidmgmt.getEfinBudgetinitial().isPreclose()) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;
      }

      // Budget Definition Pre Close Year validation
      if (bidmgmt.getEfinBudgetinitial().getStatus().equals("CL")) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;
      }

      // Bid class is mandatory
      if (bidmgmt.getBidclass() == null) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_bidclass_mand@");
        bundle.setResult(result);
        return;
      }

      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      // check pr encumbrance type is enable or not .. Task No.5925
      enccontrollist = BidManagementDAO.getPREncumTypeList(clientId);
      log.debug("enccontlist:" + enccontrollist.size());
      // End Task No.5925

      // Check transaction period is opened or not
      if (DocAction.equals("CO")) {
        isPeriodOpen = Utility.checkOpenPeriod(bidmgmt.getBidcreationdate(), orgId, clientId);
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      }

      Boolean allowApprove = false;

      if (bidmgmt.getEUTForwardReqmoreinfo() != null) {
        allowApprove = forwardDao.allowApproveReject(bidmgmt.getEUTForwardReqmoreinfo(), userId,
            roleId, Resource.Bid_Management);
      }
      if (bidmgmt.getEUTReqmoreinfo() != null
          || ((bidmgmt.getEUTForwardReqmoreinfo() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // checking if bid category is empty or not and throw error if it is empty
      if (bidmgmt.getESCMBidCategoriesList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidCategoryIsEmpty@");
        bundle.setResult(result);
        return;
      }

      // checking if bid dates is empty or not and throw error if it is empty
      if (bidmgmt.getEscmBiddatesList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@ESCM_BidDatesIsEmpty_Bidmng@");
        bundle.setResult(result);
        return;
      }

      if (bidmgmt.getBidtype().equals("LD") || bidmgmt.getBidtype().equals("TR")) {
        if (bidmgmt.getBidpurpose() == null) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidPurposeIsEmpty@");
          bundle.setResult(result);
          return;
        }
      }

      // check role is present in document rule or not
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) {
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, orgId, userId, roleId, Resource.Bid_Management, BigDecimal.ZERO);
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
        // chk qty is zero
        for (Escmbidmgmtline line : bidmgmt.getEscmBidmgmtLineList()) {
          if (line.getMovementQuantity().compareTo(BigDecimal.ZERO) == 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_IR_Quantity@");
            bundle.setResult(result);
            return;
          }
        }
        if (bidmgmt.getBidtype().equals("TR")) {
          if (bidmgmt.getRfpprice() == null) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_RfpPrice@");
            bundle.setResult(result);
            return;
          }
          // else if (bidmgmt.getRfpprice().compareTo(new BigDecimal(0)) == 0) {
          // OBDal.getInstance().rollbackAndClose();
          // OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_RfpPrice_Zero@");
          // bundle.setResult(result);
          // return;
          // }
        }
      }

      // Check whether the product belongs to that contract category or not
      if (bidmgmt.getContractType() != null) {
        String contCatgId = bidmgmt.getContractType().getId();
        for (Escmbidmgmtline line : bidmgmt.getEscmBidmgmtLineList()) {
          if (line.getProduct() != null) {
            if (line.getProduct().getESCMPRODCONTCATGList() != null
                && line.getProduct().getESCMPRODCONTCATGList().size() != 0) {
              boolean contCatgMatch = true;
              for (ESCMProductContCatg pContCatg : line.getProduct().getESCMPRODCONTCATGList()) {
                if (pContCatg.getContractCategory().getId().equals(contCatgId)) {
                  contCatgMatch = true;
                  break;

                } else {
                  if (contCatgId.equals(Utility.getConCatTypeOther())) {
                    contCatgMatch = true;
                    break;
                  } else {
                    contCatgMatch = false;
                  }
                }
              }
              if (!contCatgMatch) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_ItemMismatchWithContCatg@");
                bundle.setResult(result);
                return;
              }
            }
          }
        }
      }

      // chk submitting role is in first role in document rule
      if (DocStatus.equals("DR")) {
        chkSubRolIsInFstRolofDR = UtilityDAO.chkSubRolIsInFstRolofDR(
            OBDal.getInstance().getConnection(), clientId, orgId, userId, roleId,
            Resource.Bid_Management, BigDecimal.ZERO);
        if (!chkSubRolIsInFstRolofDR) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Role_NotFundsReserve_submit@");
          bundle.setResult(result);
          return;
        }
      }

      // Copy UniqueCode ... task no. 6007
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("ESCM_RA")) {
        List<?> reqlinelist = BidManagementDAO.getUniqueCode(bidmgmt.getId());
        // if all line uniquecode is same
        if (reqlinelist != null && reqlinelist.size() == 1) {
          Object[] reqline = (Object[]) reqlinelist.get(0);
          if (reqline != null && reqline[1] != null) {
            String uniqueCode = reqline[1].toString();
            // if all line uniquecode is same but not same as header unique code then update header
            // uniquecode with line uniquecode value
            if (bidmgmt.getEFINUniqueCode() != null) {
              if (!uniqueCode.equals(bidmgmt.getEFINUniqueCode().getId())) {
                AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class,
                    uniqueCode);
                bidmgmt.setEFINUniqueCode(acct);

              }
            } else {
              AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class,
                  uniqueCode);
              bidmgmt.setEFINUniqueCode(acct);

            }
          }
        }
        // if all line uniquecode is not same then make header uniquecode as null
        else if (reqlinelist != null && reqlinelist.size() != 1) {
          bidmgmt.setEFINUniqueCode(null);

        }
      }

      // check pr is same department or not
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) {
        /*
         * if (!BidManagementDAO.checkMultiDepPR(receiptId, conn)) { errorFlag = true;
         * OBDal.getInstance().rollbackAndClose(); OBError result =
         * OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidMultiDep@");
         * bundle.setResult(result); return; }
         */

        /*
         * // check whether any requesting Department is null if
         * (BidManagementDAO.checkRequesterIsEmpty(receiptId, conn)) { errorFlag = true;
         * OBDal.getInstance().rollbackAndClose(); OBError result =
         * OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidEmptyRequester");
         * bundle.setResult(result); return; }
         */
        /*
         * // Check bid has different agency in Bid lines if
         * (!BidManagementDAO.checkMultiAgencyPR(receiptId, conn)) { errorFlag = true;
         * OBDal.getInstance().rollbackAndClose(); OBError result =
         * OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidMultiAgency@");
         * bundle.setResult(result); return; }
         */
      }

      if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_RA") || DocStatus.equals("ESCM_REJ")
          || DocStatus.equals("ESCM_IP")) && !bidmgmt.isEfinIsbudgetcntlapp()) {
        // Task No.5925
        if (enccontrollist.size() > 0) {

          mixedencumbrance = BidManagementDAO.checkmixedPREncumbrance(bidmgmt);
          if (mixedencumbrance) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_BidPRMixedEncumbrance@");
            bundle.setResult(result);
            return;
          }
          List<?> deptList = BidManagementDAO.getDepartment(bidmgmt.getId());
          if (deptList != null && deptList.size() > 1) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Req_SameDept@");
            bundle.setResult(result);
            return;
          }

          // chk same budget type
          List<?> budTypList = BidManagementDAO.getBudgetType(bidmgmt.getId());
          if (budTypList != null && budTypList.size() > 1) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Req_SameBType@");
            bundle.setResult(result);
            return;
          }
          // check mixed encumbrance(one pr having encumbrance another pr not having encumbrance)
          // added in bid lines then throw error

        }
      }
      // End Task No.5925

      // chk the budget control preference
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("ESCM_IP")) {
        // try {
        // preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
        // vars.getClient(), bidmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(),
        // bidMgmtWindowId);
        // } catch (PropertyException e) {
        // preferenceValue = "N";
        // }
        // // Check preference value is not null
        // if (preferenceValue == null) {
        // OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
        // bundle.setResult(result);
        // return;
        // }

        try {
          preferenceValue = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
              "ESCM_BudgetControl", Boolean.TRUE, vars.getClient(),
              bidmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(), bidMgmtWindowId,
              "N");
          preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;

        } catch (PropertyException e) {
          preferenceValue = "N";
          // log.error("Exception in getting budget controller :", e);
        }

        if (!preferenceValue.equals("Y") && bidmgmt.getEUTForwardReqmoreinfo() != null) {// check
                                                                                         // for
          // temporary
          // preference
          String requester_user_id = bidmgmt.getEUTForwardReqmoreinfo().getUserContact().getId();
          String requester_role_id = bidmgmt.getEUTForwardReqmoreinfo().getRole().getId();
          preferenceValue = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
              vars.getRole(), vars.getUser(), vars.getClient(), bidmgmt.getOrganization().getId(),
              bidMgmtWindowId, requester_user_id, requester_role_id);
        }
        // // Check preference value is not null

        if (preferenceValue == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
          bundle.setResult(result);
          return;
        }

        // check preference is given by forward then restrict to give access while submit
        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ"))
            && preferenceValue.equals("Y")) {
          List<Preference> prefs = forwardDao.getPreferences("ESCM_BudgetControl", true,
              vars.getClient(), bidmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(),
              bidMgmtWindowId, false, true, true);
          for (Preference preference : prefs) {
            if (preference.getEutForwardReqmoreinfo() != null) {
              preferenceValue = "N";
            }
          }
        }

        OBContext.setAdminMode();
        if (preferenceValue != null && preferenceValue.equals("Y")
            && !bidmgmt.getBidtype().equals("DR")) {
          if (bidmgmt.getApprovedbudget().compareTo(new BigDecimal(0)) == 0) {
            // errorFlag = true;
            // OBDal.getInstance().rollbackAndClose();
            // OBError result = OBErrorBuilder.buildMessage(null, "error",
            // "@Escm_Bid_Appbug_zero@");
            // bundle.setResult(result);
            // return;
          } else if (bidmgmt.getApprovedbudget() != null) {
            if (BidManagementDAO.getBidConfigCount(bidmgmt.getOrganization().getId(),
                bidmgmt.getBidtype()) > 0) {
              if (BidManagementDAO.getBidConfigAppBudCount(bidmgmt.getOrganization().getId(),
                  bidmgmt.getBidtype(), bidmgmt.getApprovedbudget()) > 0) {
                bidstatus = true;
              }
              if (!bidstatus) {
                errorMsg = OBMessageUtils.messageBD("Escm_appbud");
                throw new OBException(errorMsg);
              }
            }
          }
        }
        if (preferenceValue != null && preferenceValue.equals(
            "Y")) {/*
                    * // Task No.5925 if (enccontrollist.size() > 0 &&
                    * !bidmgmt.isEfinIsbudgetcntlapp()) { for (Escmbidmgmtline line :
                    * bidmgmt.getEscmBidmgmtLineList()) { OBQuery<Escmbidsourceref> bidsrcref =
                    * OBDal.getInstance() .createQuery(Escmbidsourceref.class,
                    * " as e where  e.escmBidmgmtLine.id='" + line.getId() +
                    * "' and e.requisitionLine.id is not null"); if (bidsrcref.list() != null &&
                    * bidsrcref.list().size() > 0) { fromPR = true; break; } } // unique code
                    * mandatory for budget controller if (fromPR) {
                    * 
                    * // check the Purchase requistion line added is having Skip encumbrance flag or
                    * not for (Escmbidmgmtline lines : bidmgmt.getEscmBidmgmtLineList()) { if
                    * (!lines.isSummarylevel()) { for (Escmbidsourceref srcref :
                    * lines.getEscmBidsourcerefList()) { if (srcref.getRequisition() != null) {
                    * isSkipPR = srcref.getRequisition().isEfinSkipencumbrance(); break; } } } }
                    * 
                    * OBQuery<Escmbidmgmtline> lines = OBDal.getInstance().createQuery(
                    * Escmbidmgmtline.class, "escmBidmgmt.id = '" + bidmgmt.getId() +
                    * "' and issummarylevel = 'N'"); if (lines.list() != null && lines.list().size()
                    * > 0) { for (Escmbidmgmtline bidline : lines.list()) { if
                    * (bidline.getAccountingCombination() == null && !isSkipPR) { errorFlag = true;
                    * OBDal.getInstance().rollbackAndClose(); OBError result =
                    * OBErrorBuilder.buildMessage(null, "error",
                    * "@Efin_Req_Uniquecode_Mandatory@");// ESCM_RoleIsNotIncInDocRule
                    * bundle.setResult(result); return; } } } } // encumbrance validation
                    * resultEncum = BidManagementDAO.checkFullPRQtyUitlizeorNot(bidmgmt);
                    * log.debug("resultEncum1:" + resultEncum); if (resultEncum != null &&
                    * resultEncum.has("isAssociatePREncumbrance") &&
                    * resultEncum.getBoolean("isAssociatePREncumbrance") &&
                    * resultEncum.has("isFullQtyUsed") && !resultEncum.getBoolean("isFullQtyUsed"))
                    * { EfinBudgetManencum encumbrance =
                    * OBDal.getInstance().get(EfinBudgetManencum.class,
                    * resultEncum.getString("encumbrance")); errorFlag =
                    * BidManagementDAO.chkFundsAvailforNewEncumbrance(bidmgmt, encumbrance, null,
                    * null, false); if (errorFlag) { count = -4; errorFlag = true; OBError result1 =
                    * OBErrorBuilder.buildMessage(null, "error", "@ESCM_ProcessFailed(Reason)@");
                    * bundle.setResult(result1); return; } } else { if (resultEncum != null &&
                    * resultEncum.has("encumbrance")) { EfinBudgetManencum encumbrance =
                    * OBDal.getInstance().get(EfinBudgetManencum.class,
                    * resultEncum.getString("encumbrance")); errorFlag =
                    * BidManagementDAO.chkFundsAvailforNewEncumbrance(bidmgmt, encumbrance, null,
                    * null, true); if (errorFlag) { count = -4; errorFlag = true; OBError result1 =
                    * OBErrorBuilder.buildMessage(null, "error", "@ESCM_ProcessFailed(Reason)@");
                    * bundle.setResult(result1); return; } } } }
                    */
          // End Task No.5925
        }
      }
      // check supplier and biddates size for draft and rejected records
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) {
        if (bidmgmt.getBidtype().equals("TR")) {
          if (!(bidmgmt.getEscmBiddatesList().size() > 0)
              || !(bidmgmt.getEscmBidtermcdnList().size() > 0)) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Biddate_Empty@");
            bundle.setResult(result);
            return;
          }
        }
        /*
         * else if (bidmgmt.getBidtype().equals("DR")) { if (
         * (!(bidmgmt.getEscmBiddatesList().size() > 0)) ||
         * !(bidmgmt.getEscmBidsuppliersList().size() > 0)) { errorFlag = true;
         * OBDal.getInstance().rollbackAndClose(); OBError result =
         * OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidSup_Empty@");
         * bundle.setResult(result); return; } }
         */else if (!bidmgmt.getBidtype().equals("DR")) {
          if ((!(bidmgmt.getEscmBiddatesList().size() > 0))
              /* || !(bidmgmt.getEscmBidsuppliersList().size() > 0) */
              || !(bidmgmt.getEscmBidtermcdnList().size() > 0)) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Biddate_Empty@");
            bundle.setResult(result);
            return;
          }
        }
      }

      /*
       * if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) { if (bidmgmt.getBidtype() !=
       * null && (bidmgmt.getBidtype().equals("LD") || bidmgmt.getBidtype().equals("TR"))) { if
       * (bidmgmt.getEscmBidtermcdnList().size() > 0) { for (EscmBidTermCondition bid :
       * bidmgmt.getEscmBidtermcdnList()) { if (bid.getAttributename() != null &&
       * bid.getAttributename().getSearchKey() != null &&
       * bid.getAttributename().getSearchKey().equals("ING")) { initialBGPresent = true; break; } }
       * if (!initialBGPresent) { OBDal.getInstance().rollbackAndClose(); OBError result =
       * OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidContainInitalBG@");
       * bundle.setResult(result); return; } } } }
       */

      // chk qty match
      if (bidmgmt.getEscmBidmgmtLineList().size() > 0) {
        String lineno = null;
        for (Escmbidmgmtline line : bidmgmt.getEscmBidmgmtLineList()) {
          BigDecimal configAppBudSize = BidManagementDAO.getBidSourRefQty(line.getId());

          log.debug("getMovementQuantity" + line.getMovementQuantity());
          if (configAppBudSize != null
              && configAppBudSize.compareTo(line.getMovementQuantity()) != 0) {
            errorFlag = true;
            if (lineno != null)
              lineno = lineno + "," + line.getLineNo().toString();
            else
              lineno = line.getLineNo().toString();
          }
        }
        if (errorFlag) {
          OBDal.getInstance().rollbackAndClose();
          String message = OBMessageUtils.messageBD("ESCM_BidMgmLine_GrtSrcQty");
          message = message.replace("%", lineno);
          OBError result = OBErrorBuilder.buildMessage(null, "error", message);
          bundle.setResult(result);
          return;
        }
      }

      /*
       * // chk submitting role is in first role in document rule if (DocStatus.equals("DR")) {
       * chkSubRolIsInFstRolofDR = UtilityDAO.chkSubRolIsInFstRolofDR(
       * OBDal.getInstance().getConnection(), clientId, orgId, userId, roleId,
       * Resource.Bid_Management, BigDecimal.ZERO); if (!chkSubRolIsInFstRolofDR) { errorFlag =
       * true; OBDal.getInstance().rollbackAndClose(); OBError result =
       * OBErrorBuilder.buildMessage(null, "error", "@Efin_Role_NotFundsReserve_submit@");
       * bundle.setResult(result); return; } }
       */

      // check current role associated with document rule for approval flow
      if (!DocStatus.equals("DR") && !DocStatus.equals("ESCM_REJ")
          && !DocStatus.equals("ESCM_RA")) {
        if (bidmgmt.getEUTNextRole() != null) {
          java.util.List<EutNextRoleLine> li = bidmgmt.getEUTNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        // check current role is a delegated role or not
        if (bidmgmt.getEUTNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
              DocumentTypeE.BID.getDocumentTypeCode());
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
      if ((!vars.getUser().equals(bidmgmt.getCreatedBy().getId()))
          && (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      if (!errorFlag) {
        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("ESCM_RA"))
            && DocAction.equals("CO")) {
          appstatus = "SUB";
        }
        /*
         * else if (DocStatus.equals("ESCM_RA") && bidmgmt.getBidstatus().equals("EXT")) { appstatus
         * = "EXT"; } else if (DocStatus.equals("ESCM_RA") && bidmgmt.getBidstatus().equals("WD")) {
         * appstatus = "WD"; } else if (DocStatus.equals("ESCM_RA") &&
         * bidmgmt.getBidstatus().equals("PP")) { appstatus = "PP"; } else if
         * (DocStatus.equals("ESCM_RA") && bidmgmt.getBidstatus().equals("RES")) { appstatus =
         * "RES"; }
         */else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
          appstatus = "AP";
        }

        int setWarn = 0;
        if (!bidmgmt.getBidtype().equals("DR")) {
          if (bidmgmt.getApprovedbudget().compareTo(new BigDecimal("1000000")) > 0) {
            if (bidmgmt.getBidtype().equals("DR")) {
              setWarn = 1;
            }
          }
          // TaskNo.- 6610
          /*
           * else if (bidmgmt.getApprovedbudget().compareTo(new BigDecimal("0")) == 0) { setWarn =
           * 2; }
           */
          else if (bidmgmt.getApprovedbudget() != null) {
            if (BidManagementDAO.getBidConfigCount(bidmgmt.getOrganization().getId(),
                bidmgmt.getBidtype()) > 0) {
              if (BidManagementDAO.getBidConfigAppBudCount(bidmgmt.getOrganization().getId(),
                  bidmgmt.getBidtype(), bidmgmt.getApprovedbudget()) > 0) {
                bidstatus = true;
              }
              if (!bidstatus) {
                setWarn = 3;
              }
            }
          }
        }

        BidManagementDAO.updateDaysLeft(bidmgmt.getId(), vars, bundle);
        count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, bidmgmt, appstatus,
            comments, currentDate, vars, Lang, bundle, preferenceValue, enccontrollist);

        // Check Encumbrance Amount is Zero Or Negative
        if (bidmgmt.getEncumbrance() != null)
          encumLinelist = bidmgmt.getEncumbrance().getEfinBudgetManencumlinesList();
        if (encumLinelist.size() > 0)
          checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

        if (checkEncumbranceAmountZero) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
          bundle.setResult(result);
          return;
        }

        if (count == 3) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
          bundle.setResult(result);
          return;
        } else if (count == -2) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsgs);
          bundle.setResult(result);
          return;
        } else if (count == 2) {
          OBError result = null;
          if (setWarn == 1) {
            result = OBErrorBuilder.buildMessage(null, "warning", "@Escm_Bid_Complete_BudgWarn@");
          } else {
            result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
          }
          bundle.setResult(result);

        } /*
           * else if (enccontrollist.size() > 0 && count == 5) {
           * OBDal.getInstance().rollbackAndClose(); OBError result =
           * OBErrorBuilder.buildMessage(null, "error", "@Efin_No_BudgetControl@");
           * bundle.setResult(result); return; }
           */

        else if (count == -4) {
          errorFlag = true;
          OBError result1 = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_ProcessFailed(Reason)@");
          bundle.setResult(result1);
          return;
        }

        else if (count == 1) {
          OBError result = null;
          if (setWarn == 1) {
            result = OBErrorBuilder.buildMessage(null, "warning", "@Escm_Bid_Complete_BudgWarn@");
          }
          // TaskNo.- 6610
          /*
           * else if (setWarn == 2) { errorFlag = true; OBDal.getInstance().rollbackAndClose();
           * OBError result1 = OBErrorBuilder.buildMessage(null, "error",
           * "@Escm_Bid_Appbug_zero_Final@"); bundle.setResult(result1); return; }
           */
          else if (setWarn == 3) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result1 = OBErrorBuilder.buildMessage(null, "error", "@Escm_appbud_final@");
            bundle.setResult(result1);
            return;
          } else {
            result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
            bundle.setResult(result);
          }
        }
        if ((count == 1 || count == 2) && !errorFlag) {
          // split and merge concept //Task No.5925

          if (!nextApproval.hasApproval()) {
            // if (preferenceValue != null && preferenceValue.equals("Y")) {
            // Task No.5925
            if (enccontrollist.size() > 0 && !bidmgmt.isEfinIsbudgetcntlapp()) {
              for (Escmbidmgmtline line : bidmgmt.getEscmBidmgmtLineList()) {
                List<Escmbidsourceref> bidsrcref = BidManagementDAO.getBidSourceRef(line.getId());
                if (bidsrcref != null && bidsrcref.size() > 0) {
                  fromPR = true;
                  break;
                }
              }
              // unique code mandatory for budget controller
              if (fromPR) {
                // check the Purchase requistion line added is having Skip encumbrance flag or not
                for (Escmbidmgmtline lines : bidmgmt.getEscmBidmgmtLineList()) {
                  if (!lines.isSummarylevel()) {
                    for (Escmbidsourceref srcref : lines.getEscmBidsourcerefList()) {
                      if (srcref.getRequisition() != null) {
                        isSkipPR = srcref.getRequisition().isEfinSkipencumbrance();
                        break;
                      }
                    }
                  }
                }
                // This block is commented, because bid management should not check unicode
                // validation (Reference Task No. 7169)
                /*
                 * OBQuery<Escmbidmgmtline> lines = OBDal.getInstance().createQuery(
                 * Escmbidmgmtline.class, "escmBidmgmt.id = '" + bidmgmt.getId() +
                 * "' and issummarylevel = 'N'"); if (lines.list() != null && lines.list().size() >
                 * 0) { for (Escmbidmgmtline bidline : lines.list()) { if
                 * (bidline.getAccountingCombination() == null && !isSkipPR) { errorFlag = true;
                 * OBDal.getInstance().rollbackAndClose(); OBError result =
                 * OBErrorBuilder.buildMessage(null, "error", "@Efin_Req_Uniquecode_Mandatory@");//
                 * ESCM_RoleIsNotIncInDocRule bundle.setResult(result); return; } } }
                 */
              }
              // encumbrance validation
              resultEncum = BidManagementDAO.checkFullPRQtyUitlizeorNot(bidmgmt);
              log.debug("resultEncum1:" + resultEncum);
              if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                  && resultEncum.getBoolean("isAssociatePREncumbrance")
                  && resultEncum.has("isFullQtyUsed") && !resultEncum.getBoolean("isFullQtyUsed")) {
                EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                    resultEncum.getString("encumbrance"));
                errorFlag = BidManagementDAO.chkFundsAvailforNewEncumbrance(bidmgmt, encumbrance,
                    null, null, false);
                if (errorFlag) {
                  count = -4;
                  errorFlag = true;
                  OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_ProcessFailed(Reason)@");
                  bundle.setResult(result1);
                  return;
                }
              } else {
                if (resultEncum != null && resultEncum.has("encumbrance")) {
                  EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                      resultEncum.getString("encumbrance"));
                  errorFlag = BidManagementDAO.chkFundsAvailforNewEncumbrance(bidmgmt, encumbrance,
                      null, null, true);
                  if (errorFlag) {
                    count = -4;
                    errorFlag = true;
                    OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_ProcessFailed(Reason)@");
                    bundle.setResult(result1);
                    return;
                  }
                }
              }
            }
            // End Task No.5925
            // }
            if (enccontrollist.size() > 0 && !bidmgmt.isEfinIsbudgetcntlapp()) {
              OBInterceptor.setPreventUpdateInfoChange(true);
              // encumbrance split & merge concept
              if (!errorFlag && resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                  && resultEncum.getBoolean("isAssociatePREncumbrance")) {
                if (resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {
                  EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                      resultEncum.getString("encumbrance"));
                  bidmgmt.setEncumbrance(encumbrance);
                  bidmgmt.getEncumbrance().setEncumStage("BE");
                  if (encumbrance.getEncumMethod().equals("A"))
                    bidmgmt.getEncumbrance().setDescription(bidmgmt.getBidname());
                  bidmgmt.setEfinIsbudgetcntlapp(true);
                  OBDal.getInstance().save(bidmgmt);

                  BidManagementDAO.StageMoveUniquecodeChanges(bidmgmt, encumbrance);

                  for (Escmbidmgmtline line : bidmgmt.getEscmBidmgmtLineList()) {
                    if (line.getAccountingCombination() != null
                        && bidmgmt.getEncumbrance() != null) {
                      List<EfinBudgetManencumlines> manline = BidManagementDAO.getEncumLine(
                          bidmgmt.getEncumbrance().getId(),
                          line.getAccountingCombination().getId());
                      if (manline.size() > 0) {
                        line.setEfinBudgmanencumline(manline.get(0));
                      }
                    }
                  }
                } else {

                  if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                    BidManagementDAO.splitPR(resultEncum, bidmgmt, null, null, null);
                  }
                  if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                    BidManagementDAO.splitPR(resultEncum, bidmgmt, null, null, null);
                  }
                }
              }
            }
            bidmgmt.setEfinIsbudgetcntlapp(true);
            OBDal.getInstance().save(bidmgmt);
            OBDal.getInstance().flush();
            OBInterceptor.setPreventUpdateInfoChange(false);
          }
          // End Task No.5925
        }

        return;
      }
    } catch (OBException e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertAutoEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in BidManagementAction:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, EscmBidMgmt objRequest, String appstatus, String comments, Date currentDate,
      VariablesSecureApp vars, String Lang, ProcessBundle bundle, String preferenceValue,
      List<EfinEncControl> enccontrollist) {
    String requistionId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.BidManagement;
    User objUser = Utility.getObject(User.class, vars.getUser());
    User objCreater = objRequest.getCreatedBy();
    boolean isDummyRole = false;
    boolean errorFlag = false;
    ProposalManagementProcessDAO delDAO = new ProposalManagementProcessDAOImpl();
    try {
      OBContext.setAdminMode();

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      JSONObject fromUserandRoleJson = new JSONObject();
      // ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      String fromUser = userId;
      String fromRole = roleId;
      isDirectApproval = BidManagementDAO.isDirectApproval(objRequest.getId(), roleId);
      // get alert rule id
      alertRuleId = BidManagementDAO.getAlertRule(clientId, alertWindow);

      if ((objRequest.getEUTNextRole() != null)) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            objRequest.getEUTNextRole(), userId, roleId, clientId, orgId, Resource.Bid_Management,
            isDummyRole, isDirectApproval);
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

      if ((objRequest.getEUTNextRole() == null)) {
        /*
         * nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
         * orgId, roleId, userId, Resource.Bid_Management, 0.00);
         */
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser,
            Resource.Bid_Management, objRequest.getRole().getId(), fromUser, true,
            objRequest.getBidappstatus());
      } else {
        if (isDirectApproval) {

          nextApproval = NextRoleByRule.getRequesterNextRole(OBDal.getInstance().getConnection(),
              clientId, orgId, fromRole, fromUser, Resource.Bid_Management,
              objRequest.getRole().getId());

          /*
           * nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
           * OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId,
           * Resource.Bid_Management, 0.00, objRequest.getCreatedBy().getId(), false);
           */

          // if Role doesnt has any user associated then this condition will execute and return
          // error
          if (nextApproval != null && nextApproval.getErrorMsg() != null
              && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
            errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
            errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
            count = -2;
          }

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = Utility.getObject(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                String user = BidManagementDAO.getUserRole(objNextRoleLine.getRole().getId());
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId, user,
                    Resource.Bid_Management, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, Resource.Bid_Management, 0.00);
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, Resource.Bid_Management, 0.00);
          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, Resource.Bid_Management, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getRequesterDelegatedNextRole(
                OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                delegatedToRole, fromUser, Resource.Bid_Management, objRequest.getRole().getId());
        }
      }

      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
        log.debug("error msg>>" + nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole"));
        count = 3;
      }
      // if Role doesnt has any user associated then this condition will execute and return error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        count = -2;
      } else if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        nextRole = Utility.getObject(EutNextRole.class, nextApproval.getNextRoleId());

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequest.getEUTNextRole(), Resource.Bid_Management);

        // nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        objRequest.setEscmDocaction("AP");

        if (!objRequest.getBidstatus().equals("EXT") && !objRequest.getBidstatus().equals("WD")
            && !objRequest.getBidstatus().equals("PP")
            && !objRequest.getBidstatus().equals("RES")) {
          objRequest.setBidstatus("IA");
        }
        objRequest.setBidappstatus("ESCM_IP");
        objRequest.setEUTNextRole(nextRole);
        // get alert recipient
        /*
         * OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
         * AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
         */
        List<AlertRecipient> alrtRecLs = BidManagementDAO.getAlertReceipient(alertRuleId);
        log.debug("alrtRecLs size>" + alrtRecLs.size());

        forwardDao.getAlertForForwardedUser(objRequest.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, objRequest.getBidno() + "-" + objRequest.getBidname(),
            Lang, vars.getRole(), objRequest.getEUTForwardReqmoreinfo(), Resource.Bid_Management,
            alertReceiversMap);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // delete alert for approval alerts
          // BidManagementDAO.getAlert(objRequest.getId());

          /*
           * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery( Alert.class,
           * "as e where e.referenceSearchKey='" + objRequest.getId() +
           * "' and e.alertStatus='NEW'"); if (alertQuery.list().size() > 0) { for (Alert objAlert :
           * alertQuery.list()) { objAlert.setAlertStatus("SOLVED"); } }
           */
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.BidMgmt.wfa",
              Lang) + " " + objCreater.getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(objRequest.getId(),
                objRequest.getBidno() + "-" + objRequest.getBidname(),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                objRequest.getClient().getId(), Description, "NEW", alertWindow, "scm.BidMgmt.wfa",
                Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            // String delUser = BidManagementDAO.getDelegationUser(currentDate,
            // objNextRoleLine.getRole().getId());

            List<EutDocappDelegateln> delegationln = delDAO
                .getDelegation(objNextRoleLine.getRole().getId(), currentDate, "EUT_116");
            if (delegationln != null && delegationln.size() > 0) {
              /* Task #7742 */

              for (EutDocappDelegateln obDocAppDelegation : delegationln) {
                AlertUtility.alertInsertionRole(objRequest.getId(),
                    objRequest.getBidno() + "-" + objRequest.getBidname(),
                    obDocAppDelegation.getRole().getId(),
                    obDocAppDelegation.getUserContact().getId(), objRequest.getClient().getId(),
                    Description, "NEW", alertWindow, "scm.BidMgmt.wfa", Constants.GENERIC_TEMPLATE);
                includeRecipient.add(obDocAppDelegation.getRole().getId());
              }
              if (nextRole.getEutNextRoleLineList().size() == 1 && delegationln.size() == 1
                  && Utility.getAssignedUserForRoles(
                      nextRole.getEutNextRoleLineList().get(0).getRole().getId()).size() == 1) {
                if (pendingapproval != null)
                  pendingapproval += objNextRoleLine.getRole().getName() + " ("
                      + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                      + delegationln.get(0).getRole().getName() + "-"
                      + delegationln.get(0).getUserContact().getName();
                else
                  pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                      objNextRoleLine.getRole().getName() + " ("
                          + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                          + delegationln.get(0).getRole().getName() + "-"
                          + delegationln.get(0).getUserContact().getName());
              }
            }
            /*
             * if (!delUser.equals("")) { if (pendingapproval != null) pendingapproval += "/" +
             * delUser; else pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
             * delUser); }
             */
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (alrtRecLs.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecLs) {
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
        objRequest.setEscmDocaction("AP");
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 2;
      } else {
        // in final approval should check budget controller already processed. else error.

        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objRequest.getEUTNextRole(), Resource.Bid_Management);

        /*
         * if (!objRequest.getBidtype().equals("DR")) { if (enccontrollist.size() > 0 &&
         * (preferenceValue == null || !preferenceValue.equals("Y"))) {
         * 
         * if (!objRequest.isEfinIsbudgetcntlapp()) { count = 5; return count; } } }
         */
        JSONObject resultEncum = BidManagementDAO.checkFullPRQtyUitlizeorNot(objRequest);
        log.debug("resultEncum:" + resultEncum);
        if (resultEncum != null && resultEncum.has("encumbrance")) {
          EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
              resultEncum.getString("encumbrance"));
          errorFlag = BidManagementDAO.chkFundsAvailforNewEncumbrance(objRequest, encumbrance, null,
              null, false);
          if (errorFlag) {
            count = -4;
            return count;
          }
        }

        ArrayList<String> includeRecipient = new ArrayList<String>();
        objRequest.setUpdated(new java.util.Date());
        objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;
        if (objRequest.getCreatedBy().getADUserRolesList().size() > 0
            && objRequest.getRole() != null) {
          objCreatedRole = objRequest.getRole();
        }
        forwardDao.getAlertForForwardedUser(objRequest.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, objRequest.getBidno() + "-" + objRequest.getBidname(),
            Lang, vars.getRole(), objRequest.getEUTForwardReqmoreinfo(), Resource.Bid_Management,
            alertReceiversMap);
        // delete alert for approval alerts
        // BidManagementDAO.getAlert(objRequest.getId());

        /*
         * OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
         * "as e where e.referenceSearchKey='" + objRequest.getId() + "' and e.alertStatus='NEW'");
         * if (alertQuery.list().size() > 0) { for (Alert objAlert : alertQuery.list()) {
         * objAlert.setAlertStatus("SOLVED"); } }
         */
        // get alert recipient
        List<AlertRecipient> alrtRecLs = BidManagementDAO.getAlertReceipient(alertRuleId);
        log.debug("alrtRecLs size>" + alrtRecLs.size());
        /*
         * OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance().createQuery(
         * AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
         */
        // check and insert recipient
        if (alrtRecLs.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecLs) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.BidMgmt.approved",
            Lang) + " " + objUser.getName();
        AlertUtility.alertInsertionRole(objRequest.getId(),
            objRequest.getBidno() + "-" + objRequest.getBidname(), objRequest.getRole().getId(),
            objRequest.getCreatedBy().getId(), objRequest.getClient().getId(), Description, "NEW",
            alertWindow, "scm.BidMgmt.approved", Constants.GENERIC_TEMPLATE);
        objRequest.setEscmDocaction("PD");
        objRequest.setBidstatus("ACT");
        objRequest.setBidappstatus("ESCM_AP");
        objRequest.setEUTNextRole(null);
        // set all bid dates flag as approved
        for (Escmbiddates objDates : objRequest.getEscmBiddatesList()) {
          objDates.setApproved(true);
        }
        count = 1;

      }
      // after approved by forwarded user
      if (objRequest.getEUTForwardReqmoreinfo() != null) {
        // REMOVE Role Access to Receiver
        // forwardDao.removeRoleAccess(clientId, objRequest.getEUTForwardReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequest.getEUTForwardReqmoreinfo());
        // set forward_rmi id as null in record
        objRequest.setEUTForwardReqmoreinfo(null);
      }

      // removing rmi
      if (objRequest.getEUTReqmoreinfo() != null) {
        // REMOVE Role Access to Receiver
        // forwardDao.removeReqMoreInfoRoleAccess(clientId, objRequest.getEUTReqmoreinfo().getId(),
        // con);
        // set status as "Draft" for forward record
        forwardDao.setForwardStatusAsDraft(objRequest.getEUTReqmoreinfo());
        // set forward_rmi id as null in record
        objRequest.setEUTReqmoreinfo(null);
        objRequest.setRequestMoreInformation("N");
      }

      OBDal.getInstance().save(objRequest);
      requistionId = objRequest.getId();
      if (!StringUtils.isEmpty(requistionId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", requistionId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.Bid_Management_History);
        historyData.put("HeaderColumn", ApprovalTables.Bid_Management_History_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Bid_Management_History_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
          Resource.Bid_Management);

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in BM: ", e);
      OBDal.getInstance().rollbackAndClose();
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return count;
  }
}
