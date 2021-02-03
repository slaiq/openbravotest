package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Kiruthika
 */

// Approval Flow of Proposal Management
public class UnifiedProposalActionMethod {
  private static final Logger log = LoggerFactory.getLogger(UnifiedProposalActionMethod.class);
  private static final String REACTIVATE = "RE";
  private static final String SUBMIT_FOR_APPROVAL = "SA";
  public static final String PROPOSAL_WINDOW_ID = "CAF2D3EEF3B241018C8F65E8F877B29F";
  public static final String UNIFIEDTAB_ID = "31D9A3BF1C63488983C286C53A59196C";

  /**
   * Encumbrance validation for awarded proposals
   * 
   * @param proposalmgmt
   * @param vars
   * @return
   */
  public static OBError encumbranceValidation(EscmProposalMgmt proposalmgmt,
      VariablesSecureApp vars) {

    boolean errorFlag = false;
    try {
      OBContext.setAdminMode();
      String budgetcontroller = "";
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      ProposalManagementDAO propDAO = new ProposalManagementDAOImpl();
      String DocStatus = proposalmgmt.getProposalappstatus(),
          ProposalStatus = proposalmgmt.getProposalstatus();
      Boolean fromPR = false;
      String proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%",
          proposalmgmt.getProposalno());

      try {
        budgetcontroller = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", Boolean.TRUE, vars.getClient(),
            proposalmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(),
            PROPOSAL_WINDOW_ID, "N");
        budgetcontroller = (budgetcontroller == null) ? "N" : budgetcontroller;

      } catch (PropertyException e) {
        budgetcontroller = "N";
      }

      if (!budgetcontroller.equals("Y") && proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        // check for
        // temporary
        // preference
        String requester_user_id = proposalmgmt.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = proposalmgmt.getEUTForwardReqmoreinfo().getRole().getId();
        budgetcontroller = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            vars.getRole(), vars.getUser(), vars.getClient(),
            proposalmgmt.getOrganization().getId(), PROPOSAL_WINDOW_ID, requester_user_id,
            requester_role_id);
      }

      // check preference is given by forward then restrict to give access while submit
      if (((ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD"))
          && (DocStatus.equals("REJ") || DocStatus.equals("INC")))
          && budgetcontroller.equals("Y")) {
        List<Preference> prefs = forwardDao.getPreferences("ESCM_BudgetControl", true,
            vars.getClient(), proposalmgmt.getOrganization().getId(), vars.getUser(),
            vars.getRole(), Constants.PROPOSAL_MANAGEMENT_W, false, true, true);
        for (Preference preference : prefs) {
          if (preference.getEutForwardReqmoreinfo() != null) {
            budgetcontroller = "N";
          }
        }
      }
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);

      if (enccontrollist.size() > 0 && (budgetcontroller == null || budgetcontroller.equals("Y"))
          && !proposalmgmt.isEfinIsbudgetcntlapp()) {

        for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
          List<EscmProposalsourceRef> proposalsrcref = propDAO.checkLinesAddedFromPR(line.getId());
          if (proposalsrcref != null && proposalsrcref.size() > 0) {
            fromPR = true;
            break;
          }
        }
        if (!fromPR) {
          if (proposalmgmt.getEfinEncumbrance() != null
              && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {

            // check all line uniquecode belongs to same encumbrance if manual encumbrance.
            errorFlag = ProposalManagementActionMethod.checkAllUniquecodesameEncum(proposalmgmt);
            if (errorFlag) {

              OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
                  "@Efin_Unicode_Same_Encum@");
              return result;
            }

            // If proposals are having same manual encumbrance, add all proposal amount and check if
            // there is remaining amount
            errorFlag = checkManualEncumRemAmt(proposalmgmt, true);
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              return result;
            }

            // manual encum validation and update.
            // errorFlag =
            // ProposalManagementActionMethod.chkManualEncumbranceValidation(proposalmgmt,
            // true);
            // if (errorFlag) {
            // OBError result = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
            // "@Efin_Chk_Line_Info@");
            // return result;
            // }
          }
          // auto validation
          else if ((proposalmgmt.getEfinEncumbrance() == null
              && proposalmgmt.getEscmBidmgmt() == null)
              || (proposalmgmt.getEfinEncumbrance() == null && proposalmgmt.getEscmBidmgmt() != null
                  && proposalmgmt.getEscmBidmgmt().getEncumbrance() == null)) {
            JSONObject errorresult = ProposalManagementActionMethod
                .checkAutoEncumValidationForPR(proposalmgmt);
            if (errorresult.has("errorflag")) {
              if (errorresult.getString("errorflag").equals("0")) {
                errorFlag = true;
                OBError result1 = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
                    errorresult.getString("errormsg"));
                return result1;
              }
            }
          }
        }
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while encumbranceValidation: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in encumbranceValidation:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  public static boolean checkBudgetController(EscmProposalMgmt proposalmgmt,
      VariablesSecureApp vars) {

    boolean isBudgetController = false;
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    String DocStatus = proposalmgmt.getProposalappstatus(),
        ProposalStatus = proposalmgmt.getProposalstatus();
    try {
      OBContext.setAdminMode();
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);
      String budgetcontroller = "";
      try {
        budgetcontroller = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", Boolean.TRUE, vars.getClient(),
            proposalmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(),
            PROPOSAL_WINDOW_ID, "N");
        budgetcontroller = (budgetcontroller == null) ? "N" : budgetcontroller;

      } catch (PropertyException e) {
        budgetcontroller = "N";
      }
      if (!budgetcontroller.equals("Y") && proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        // check for
        // temporary
        // preference
        String requester_user_id = proposalmgmt.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = proposalmgmt.getEUTForwardReqmoreinfo().getRole().getId();
        budgetcontroller = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            vars.getRole(), vars.getUser(), vars.getClient(),
            proposalmgmt.getOrganization().getId(), PROPOSAL_WINDOW_ID, requester_user_id,
            requester_role_id);
      }
      // check preference is given by forward then restrict to give access while submit
      if (((ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD"))
          && (DocStatus.equals("REJ") || DocStatus.equals("INC")))
          && budgetcontroller.equals("Y")) {
        List<Preference> prefs = forwardDao.getPreferences("ESCM_BudgetControl", true,
            vars.getClient(), proposalmgmt.getOrganization().getId(), vars.getUser(),
            vars.getRole(), Constants.PROPOSAL_MANAGEMENT_W, false, true, true);
        for (Preference preference : prefs) {
          if (preference.getEutForwardReqmoreinfo() != null) {
            budgetcontroller = "N";
          }
        }
      }
      // Task No.5925
      if (enccontrollist.size() > 0 && (budgetcontroller == null || budgetcontroller.equals("Y"))
          && !proposalmgmt.isEfinIsbudgetcntlapp()) {
        if (budgetcontroller.equals("Y")) {
          isBudgetController = true;
        }
      }

      return isBudgetController;
    } catch (Exception e) {
      log.error("Exception while checkBudgetController", e);
      OBDal.getInstance().rollbackAndClose();
      return false;
    }
  }

  /**
   * Update encumbrance for awarded proposals
   * 
   * @param proposalmgmt
   * @param vars
   * @return
   */
  public static OBError updateProposalEncumbrance(EscmProposalMgmt proposalmgmt,
      VariablesSecureApp vars) {

    try {
      OBContext.setAdminMode();
      String budgetcontroller = "";
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      ProposalManagementDAO propDAO = new ProposalManagementDAOImpl();
      String DocStatus = proposalmgmt.getProposalappstatus(),
          ProposalStatus = proposalmgmt.getProposalstatus();

      Boolean fromPR = false;
      EfinBudgetManencum encumbrance = null;

      try {
        budgetcontroller = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", Boolean.TRUE, vars.getClient(),
            proposalmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(),
            PROPOSAL_WINDOW_ID, "N");
        budgetcontroller = (budgetcontroller == null) ? "N" : budgetcontroller;

      } catch (PropertyException e) {
        budgetcontroller = "N";
      }

      if (!budgetcontroller.equals("Y") && proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        // check for
        // temporary
        // preference
        String requester_user_id = proposalmgmt.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = proposalmgmt.getEUTForwardReqmoreinfo().getRole().getId();
        budgetcontroller = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            vars.getRole(), vars.getUser(), vars.getClient(),
            proposalmgmt.getOrganization().getId(), PROPOSAL_WINDOW_ID, requester_user_id,
            requester_role_id);
      }

      // check preference is given by forward then restrict to give access while submit
      if (((ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD"))
          && (DocStatus.equals("REJ") || DocStatus.equals("INC")))
          && budgetcontroller.equals("Y")) {
        List<Preference> prefs = forwardDao.getPreferences("ESCM_BudgetControl", true,
            vars.getClient(), proposalmgmt.getOrganization().getId(), vars.getUser(),
            vars.getRole(), Constants.PROPOSAL_MANAGEMENT_W, false, true, true);
        for (Preference preference : prefs) {
          if (preference.getEutForwardReqmoreinfo() != null) {
            budgetcontroller = "N";
          }
        }
      }
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);
      // Task No.5925
      if (enccontrollist.size() > 0 && !proposalmgmt.isEfinIsbudgetcntlapp()
          && budgetcontroller != null && budgetcontroller.equals("Y")) {

        // check lines added from pr
        for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
          List<EscmProposalsourceRef> proposalsrcref = propDAO.checkLinesAddedFromPR(line.getId());
          if (proposalsrcref != null && proposalsrcref.size() > 0) {
            fromPR = true;
            break;
          }
        }
        if (!fromPR) {

          if (proposalmgmt.getEscmBidmgmt() != null
              && proposalmgmt.getEscmBidmgmt().getEncumbrance() != null) {
            // split bid encumbrance
          } else if (proposalmgmt.getEfinEncumbrance() != null
              && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {
            // amount update
            ProposalManagementActionMethod.chkManualEncumbranceValidation(proposalmgmt, false);
            proposalmgmt.setEfinIsbudgetcntlapp(true);
            OBDal.getInstance().save(proposalmgmt);
          } else {
            // without encumbrance, insert auto encumbrance.
            ProposalManagementActionMethod.insertAutoEncumbrance(proposalmgmt);
            proposalmgmt.setEfinIsbudgetcntlapp(true);
            OBDal.getInstance().save(proposalmgmt);
          }
        }

        if (proposalmgmt.getEfinEncumbrance() != null) {
          encumbrance = proposalmgmt.getEfinEncumbrance();
          encumbrance.setBusinessPartner(proposalmgmt.getSupplier());
          OBDal.getInstance().save(encumbrance);
        }
      }
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while updateProposalEncumbrance: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in updateProposalEncumbrance:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  /**
   * Check if proposal is fully awarded
   * 
   * @param proposalList
   * @return
   */
  public static boolean isProposalFullyAwarded(List<String> proposalList) {

    boolean isFullyAwarded = false;

    for (String proposalId : proposalList) {

      EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);

      for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
        if (!line.isSummary()) {
          BigDecimal movementQty = line.getMovementQuantity() != null ? line.getMovementQuantity()
              : BigDecimal.ZERO;
          BigDecimal lineTotal = line.getLineTotal() != null ? line.getLineTotal()
              : BigDecimal.ZERO;
          BigDecimal awardedQty = line.getAwardedqty() != null ? line.getAwardedqty()
              : BigDecimal.ZERO;
          BigDecimal awaredAmt = line.getAwardedamount() != null ? line.getAwardedamount()
              : BigDecimal.ZERO;
          if (movementQty.compareTo(BigDecimal.ZERO) > 0 && lineTotal.compareTo(BigDecimal.ZERO) > 0
              && awardedQty.compareTo(BigDecimal.ZERO) > 0
              && awaredAmt.compareTo(BigDecimal.ZERO) > 0) {
            if (movementQty.compareTo(awardedQty) == 0 && lineTotal.compareTo(awaredAmt) == 0) {
              isFullyAwarded = true;
            } else {
              isFullyAwarded = false;
              break;
            }
          }
        }
      }
    }
    return isFullyAwarded;
  }

  /**
   * If proposal is fully awarded, move encumbrance stage
   * 
   * @param proposalmgmt
   * @param vars
   * @return
   */
  public static OBError changeEncumStage(EscmProposalMgmt proposalmgmt, VariablesSecureApp vars) {

    try {
      OBContext.setAdminMode();
      String budgetcontroller = "";
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      String DocStatus = proposalmgmt.getProposalappstatus(),
          ProposalStatus = proposalmgmt.getProposalstatus();
      String proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%",
          proposalmgmt.getProposalno());

      try {
        budgetcontroller = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", Boolean.TRUE, vars.getClient(),
            proposalmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(),
            PROPOSAL_WINDOW_ID, "N");
        budgetcontroller = (budgetcontroller == null) ? "N" : budgetcontroller;

      } catch (PropertyException e) {
        budgetcontroller = "N";
      }

      if (!budgetcontroller.equals("Y") && proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        // check for
        // temporary
        // preference
        String requester_user_id = proposalmgmt.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = proposalmgmt.getEUTForwardReqmoreinfo().getRole().getId();
        budgetcontroller = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            vars.getRole(), vars.getUser(), vars.getClient(),
            proposalmgmt.getOrganization().getId(), PROPOSAL_WINDOW_ID, requester_user_id,
            requester_role_id);
      }

      // check preference is given by forward then restrict to give access while submit
      if (((ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD"))
          && (DocStatus.equals("REJ") || DocStatus.equals("INC")))
          && budgetcontroller.equals("Y")) {
        List<Preference> prefs = forwardDao.getPreferences("ESCM_BudgetControl", true,
            vars.getClient(), proposalmgmt.getOrganization().getId(), vars.getUser(),
            vars.getRole(), Constants.PROPOSAL_MANAGEMENT_W, false, true, true);
        for (Preference preference : prefs) {
          if (preference.getEutForwardReqmoreinfo() != null) {
            budgetcontroller = "N";
          }
        }
      }
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);

      if (enccontrollist.size() > 0 && (budgetcontroller == null || budgetcontroller.equals("Y"))
          && !proposalmgmt.isEfinIsbudgetcntlapp()) {

        EfinBudgetManencum encumbrance = proposalmgmt.getEscmBidmgmt().getEncumbrance();
        encumbrance.setEncumStage("PAE");
        proposalmgmt.setEfinIsbudgetcntlapp(true);
        OBDal.getInstance().save(encumbrance);

      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while changeEncumStage: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in changeEncumStage:" + e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  /**
   * Do increase or decrease before splitting bid encumbrance
   * 
   * @param proposalmgmt
   * @param vars
   * @param appliedAmtCheck
   * @param proposalList
   * @return
   */
  public static OBError getBidtoUnifiedProposalDetails(EscmProposalMgmt proposalmgmt,
      VariablesSecureApp vars, Boolean appliedAmtCheck, List<String> proposalList) {

    try {
      OBContext.setAdminMode();
      String budgetcontroller = "";
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      String DocStatus = proposalmgmt.getProposalappstatus(),
          ProposalStatus = proposalmgmt.getProposalstatus();
      boolean uniqueCodeChanged = false;

      try {
        budgetcontroller = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", Boolean.TRUE, vars.getClient(),
            proposalmgmt.getOrganization().getId(), vars.getUser(), vars.getRole(),
            PROPOSAL_WINDOW_ID, "N");
        budgetcontroller = (budgetcontroller == null) ? "N" : budgetcontroller;

      } catch (PropertyException e) {
        budgetcontroller = "N";
      }

      if (!budgetcontroller.equals("Y") && proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        // check for
        // temporary
        // preference
        String requester_user_id = proposalmgmt.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = proposalmgmt.getEUTForwardReqmoreinfo().getRole().getId();
        budgetcontroller = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            vars.getRole(), vars.getUser(), vars.getClient(),
            proposalmgmt.getOrganization().getId(), PROPOSAL_WINDOW_ID, requester_user_id,
            requester_role_id);
      }

      // check preference is given by forward then restrict to give access while submit
      if (((ProposalStatus.equals("AWD") || ProposalStatus.equals("PAWD"))
          && (DocStatus.equals("REJ") || DocStatus.equals("INC")))
          && budgetcontroller.equals("Y")) {
        List<Preference> prefs = forwardDao.getPreferences("ESCM_BudgetControl", true,
            vars.getClient(), proposalmgmt.getOrganization().getId(), vars.getUser(),
            vars.getRole(), Constants.PROPOSAL_MANAGEMENT_W, false, true, true);
        for (Preference preference : prefs) {
          if (preference.getEutForwardReqmoreinfo() != null) {
            budgetcontroller = "N";
          }
        }
      }
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);

      if (enccontrollist.size() > 0 && (budgetcontroller == null || budgetcontroller.equals("Y"))
          && !proposalmgmt.isEfinIsbudgetcntlapp()) {

        // Bid encumbrance validation
        Query query = null, query1 = null;
        String message = null;
        BigDecimal proposalAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO,
            unAppAmt = BigDecimal.ZERO, reqLnNetAmt = BigDecimal.ZERO;
        EfinBudgetManencumlines line = null, newencumLine = null;
        AccountingCombination com = null;

        if (appliedAmtCheck) {
          String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= null "
              + "  where escm_proposalmgmt_line_id in "
              + " (select escm_proposalmgmt_line_id from escm_proposalmgmt_line prlln"
              + " join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= prlln.escm_bidmgmt_line_id "
              + " join escm_bidmgmt bid on bid.escm_bidmgmt_id=bidln.escm_bidmgmt_id "
              + " where bid.escm_bidmgmt_id = ? and prlln.issummarylevel='N'"
              + " and prlln.awardedamount > 0 and prlln.awardedqty > 0) ";
          query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
          query1.setParameter(0, proposalmgmt.getEscmBidmgmt().getId());
          query1.executeUpdate();
        }

        String sqlQuery = " select  prlln.em_efin_c_validcombination_id as procomid,sum(coalesce((prlln.awardedamount),0)) as proposalamt,"
            + " (select coalesce(sum(reqln.priceactual* srcref.quantity),0) as srcamt   "
            + " from escm_bidsourceref srcref  join escm_bidmgmt_line ln on ln.escm_bidmgmt_line_id= srcref.escm_bidmgmt_line_id "
            + " join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id and reqln.em_escm_issummary='N' "
            + "  where ln.escm_bidmgmt_id=bidln.escm_bidmgmt_id and ln.c_validcombination_id=prlln.em_efin_c_validcombination_id "
            + "  group by ln.c_validcombination_id), bid.efin_budget_manencum_id   "
            + "  from escm_proposalmgmt_line prlln "
            + "  join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= prlln.escm_bidmgmt_line_id "
            + "   join escm_bidmgmt bid on bid.escm_bidmgmt_id=bidln.escm_bidmgmt_id "
            + " left join efin_budget_manencumlines manenc on manenc.efin_budget_manencumlines_id= bidln.em_efin_budgmanencumline_id  "
            + " left join escm_bidsourceref bidsrcref on bidsrcref.escm_bidmgmt_line_id=bidln.escm_bidmgmt_line_id  "
            + " left join m_requisitionline reqln on reqln.m_requisitionline_id=bidsrcref.m_requisitionline_id "
            + "  and bidln.issummarylevel='N'  "
            + "  where bid.escm_bidmgmt_id = ? and prlln.issummarylevel='N' "
            + "  group by bidln.escm_bidmgmt_id,prlln.em_efin_c_validcombination_id , prlln.em_efin_c_validcombination_id "
            + " ,bid.efin_budget_manencum_id ";

        query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
        query.setParameter(0, proposalmgmt.getEscmBidmgmt().getId());
        log.debug("strQuery:" + query.toString());
        List queryList = query.list();
        if (queryList != null && queryList.size() > 0) {
          for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
            Object[] row = (Object[]) iterator.next();
            line = null;
            message = null;
            proposalAmt = BigDecimal.ZERO;
            reqLnNetAmt = BigDecimal.ZERO;
            diff = BigDecimal.ZERO;

            if (row[0] != null)
              com = OBDal.getInstance().get(AccountingCombination.class, row[0].toString());

            if (row[1] != null)
              proposalAmt = new BigDecimal(row[1].toString());

            // if uniquecode present in lines
            if (row[3] != null) {
              line = ProposalManagementActionMethod.getEncumbranceLine(row[3].toString(),
                  row[0].toString());
              if (line != null)
                unAppAmt = (line.getRevamount().subtract(line.getAPPAmt())
                    .subtract(line.getUsedAmount()));

            }
            if (row[2] != null)
              reqLnNetAmt = new BigDecimal(row[2].toString());

            diff = proposalAmt.subtract(reqLnNetAmt);
            if (proposalmgmt.getEscmBidmgmt().getEncumbrance() != null
                && proposalmgmt.getEscmBidmgmt().getEncumbrance().getEncumMethod().equals("M")) {

              // increase
              if (diff.compareTo(BigDecimal.ZERO) > 0) {
                if (diff.compareTo(unAppAmt) > 0) {
                  if (appliedAmtCheck) {
                    message = OBMessageUtils.messageBD("Escm_AwardedAmtLess");
                  }
                  OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance().createQuery(
                      EscmProposalmgmtLine.class,
                      " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                          + " and e.escmBidmgmtLine.escmBidmgmt.id = :bidID and awardedamount > 0 and awardedqty > 0 "
                          + " and (status != 'CL' or status is null) and e.summary = false ");
                  proposalLineQry.setNamedParameter("uniqueCodeID", row[0].toString());
                  proposalLineQry.setNamedParameter("bidID", proposalmgmt.getEscmBidmgmt().getId());
                  if (proposalLineQry != null) {
                    List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
                    if (proposalLineList.size() > 0) {
                      for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                        proposalLine.setEfinFailureReason(message);
                        OBDal.getInstance().save(proposalLine);
                      }
                      OBDal.getInstance().flush();
                    }
                  }

                } else {
                  // If there are some remaining amount, move it to applied amount
                  if (!appliedAmtCheck) {
                    EfinBudgetManencumlines encumLn = Utility
                        .getObject(EfinBudgetManencumlines.class, line.getId());
                    encumLn.setAPPAmt(encumLn.getAPPAmt().add(diff));
                    OBDal.getInstance().save(encumLn);
                    OBDal.getInstance().flush();
                  }
                }
              }
              // decrease
              else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // in case of manual increase - unapplied amount will be exist then update applied
                // amt
                if (!appliedAmtCheck) {

                  EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                      line.getId());
                  encumLn.setAPPAmt(encumLn.getAPPAmt().subtract(diff.negate()));
                  OBDal.getInstance().save(encumLn);
                  OBDal.getInstance().flush();
                }
              }
              // new uniquecode
              if (line == null) {

                if (appliedAmtCheck) {
                  message = OBMessageUtils.messageBD("EFIN_PropNewUniqNotAllow");
                }
                OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance().createQuery(
                    EscmProposalmgmtLine.class,
                    " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                        + " and e.escmBidmgmtLine.escmBidmgmt.id = :bidID and awardedamount > 0 and awardedqty > 0 "
                        + " and (status != 'CL' or status is null) and e.summary = false ");
                proposalLineQry.setNamedParameter("uniqueCodeID", row[0].toString());
                proposalLineQry.setNamedParameter("bidID", proposalmgmt.getEscmBidmgmt().getId());
                if (proposalLineQry != null) {
                  List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
                  if (proposalLineList.size() > 0) {
                    for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                      proposalLine.setEfinFailureReason(message);
                      OBDal.getInstance().save(proposalLine);
                    }
                    OBDal.getInstance().flush();
                  }
                }
              }
            } else {
              // new unique code or difference is more (increase)
              if (line == null) {

                if (!appliedAmtCheck) {
                  // INSERT ENCUM LINE
                  // if (proposalmgmt.getEscmBidmgmt() != null) {
                  // uniqueCodeChanged = false;
                  // EfinBudgetManencum encum = proposalmgmt.getEscmBidmgmt().getEncumbrance();
                  // newencumLine = insertEncumbranceLines(encum, proposalAmt, com, proposalList);
                  //
                  // for (String proposalId : proposalList) {
                  // EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
                  // proposalId);
                  // // update encumbranceline in proposal management
                  // ProposalManagementActionMethod.updateEncumbranceLineInProposal(proposal,
                  // newencumLine, com);
                  // }
                  //
                  // }
                } else {

                  uniqueCodeChanged = true;

                  JSONObject commonvalresult = CommonValidationsDAO.CommonFundsChecking(
                      proposalmgmt.getEscmBidmgmt().getEncumbrance().getBudgetInitialization(), com,
                      diff);

                  if (commonvalresult.has("errorFlag")
                      && commonvalresult.getString("errorFlag").equals("0")
                      && commonvalresult.has("message")) {

                    OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance().createQuery(
                        EscmProposalmgmtLine.class,
                        " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                            + " and e.escmBidmgmtLine.escmBidmgmt.id = :bidID and awardedamount > 0 and awardedqty > 0 "
                            + " and (status != 'CL' or status is null) and e.summary = false ");
                    proposalLineQry.setNamedParameter("uniqueCodeID", row[0].toString());
                    proposalLineQry.setNamedParameter("bidID",
                        proposalmgmt.getEscmBidmgmt().getId());
                    if (proposalLineQry != null) {
                      List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
                      if (proposalLineList.size() > 0) {
                        for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                          proposalLine.setEfinFailureReason(commonvalresult.getString("message"));
                          OBDal.getInstance().save(proposalLine);
                        }
                        OBDal.getInstance().flush();
                      }
                    }
                  }
                }
              } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                // increase
                if (!appliedAmtCheck) {
                  BidManagementDAO.insertEncumbranceModification(line, diff, null, "PRO", null,
                      null);
                  EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                      line.getId());
                  encumLn.setAPPAmt(encumLn.getAPPAmt().add(diff));
                  OBDal.getInstance().save(encumLn);
                  OBDal.getInstance().flush();
                } else {
                  JSONObject commonvalresult = CommonValidationsDAO.CommonFundsChecking(
                      proposalmgmt.getEscmBidmgmt().getEncumbrance().getBudgetInitialization(), com,
                      diff);

                  if (commonvalresult.has("errorFlag")
                      && commonvalresult.getString("errorFlag").equals("0")
                      && commonvalresult.has("message")) {

                    OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance().createQuery(
                        EscmProposalmgmtLine.class,
                        " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                            + " and e.escmBidmgmtLine.escmBidmgmt.id = :bidID and awardedamount > 0 and awardedqty > 0 "
                            + " and (status != 'CL' or status is null) and e.summary = false ");
                    proposalLineQry.setNamedParameter("uniqueCodeID", row[0].toString());
                    proposalLineQry.setNamedParameter("bidID",
                        proposalmgmt.getEscmBidmgmt().getId());
                    if (proposalLineQry != null) {
                      List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
                      if (proposalLineList.size() > 0) {
                        for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                          proposalLine.setEfinFailureReason(commonvalresult.getString("message"));
                          OBDal.getInstance().save(proposalLine);
                        }
                        OBDal.getInstance().flush();
                      }
                    }
                  }
                }
              }
              // decrease
              else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                if (!appliedAmtCheck) { // Trigger changes enc_decrease=enc_decrease+?,
                  // ,revamount=revamount-? ,remaining_amount=remaining_amount -?

                  EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                      line.getId());
                  encumLn.setAPPAmt(encumLn.getAPPAmt().add(diff));
                  OBDal.getInstance().save(encumLn);
                  OBDal.getInstance().flush();

                  // insert encumbrance modification
                  BidManagementDAO.insertEncumbranceModification(line, diff, null, "PRO", null,
                      null);

                  OBDal.getInstance().flush();

                }
              }

            }
          }
        }

        if (!appliedAmtCheck) {
          // split bid encumbrance
          for (String proposalId : proposalList) {
            EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
            JSONObject result = bidtoProposalEncumChanges(proposal, true);
            splitBidEncumbrance(proposal, result);
          }
        }

        if (appliedAmtCheck) {
          OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance().createQuery(
              EscmProposalmgmtLine.class,
              " as e where e.escmBidmgmtLine.escmBidmgmt.id = :bidID and awardedamount > 0 and awardedqty > 0 "
                  + " and (status != 'CL' or status is null) and e.summary = false "
                  + " and e.efinFailureReason is not null ");
          proposalLineQry.setNamedParameter("bidID", proposalmgmt.getEscmBidmgmt().getId());
          if (proposalLineQry != null) {
            List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
            if (proposalLineList.size() > 0) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              return result;
            }
          }
        }
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while getBidtoUnifiedProposalDetails: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in getBidtoUnifiedProposalDetails:" + e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  /**
   * Insert encumbrance line
   * 
   * @param encum
   * @param Amount
   * @param com
   * @param proposalList
   * @return
   */

  public static EfinBudgetManencumlines insertEncumbranceLines(EfinBudgetManencum encum,
      BigDecimal Amount, AccountingCombination com, List<String> proposalList) {
    Long lineNo = 0L;
    EfinBudgetManencumlines encumLines = null;
    try {
      OBContext.setAdminMode();
      List<EfinBudgetManencumlines> encumLnList = new ArrayList<EfinBudgetManencumlines>();
      // List<EscmProposalmgmtLine> proposalLnList = new ArrayList<EscmProposalmgmtLine>();

      // get the next line no based on bid management id
      OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumID   order by e.creationDate desc ");
      lines.setNamedParameter("encumID", encum.getId());
      lines.setMaxResult(1);
      encumLnList = lines.list();
      if (encumLnList.size() > 0) {
        lineNo = encumLnList.get(0).getLineNo();
        lineNo += 10;
      }
      OBQuery<EfinBudgetManencumlines> ln = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctId"
              + " order by e.creationDate desc ");
      ln.setNamedParameter("encumID", encum.getId());
      ln.setNamedParameter("acctId", com.getId());
      ln.setMaxResult(1);
      encumLnList = ln.list();
      if (encumLnList.size() == 0) {
        encumLines = OBProvider.getInstance().get(EfinBudgetManencumlines.class);
        encumLines.setManualEncumbrance(encum);
        encumLines.setLineNo(lineNo);
        encumLines.setAmount(Amount);
        encumLines.setUsedAmount(BigDecimal.ZERO);
        encumLines.setRemainingAmount(BigDecimal.ZERO);
        encumLines.setAPPAmt(Amount);
        encumLines.setRevamount(Amount);
        encumLines.setOrganization(encum.getOrganization());
        encumLines.setSalesRegion(com.getSalesRegion());
        encumLines.setAccountElement(com.getAccount());
        encumLines.setSalesCampaign(com.getSalesCampaign());
        encumLines.setProject(com.getProject());
        encumLines.setActivity(com.getActivity());
        encumLines.setStDimension(com.getStDimension());
        encumLines.setNdDimension(com.getNdDimension());
        encumLines.setBusinessPartner(com.getBusinessPartner());
        encumLines.setAccountingCombination(com);
        encumLines.setUniqueCodeName(com.getEfinUniquecodename());
        encumLines.setAuto(true);
        OBDal.getInstance().save(encumLines);
      } else {
        encumLines = encumLnList.get(0);
        encumLines.setAmount(encumLines.getAmount().add(Amount));
        encumLines.setAPPAmt(encumLines.getAPPAmt().add(Amount));
        encumLines.setRevamount(encumLines.getRevamount().add(Amount));
        OBDal.getInstance().save(encumLines);
      }
      OBDal.getInstance().flush();
      return encumLines;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in insertEncumbranceLines " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
    return encumLines;
  }

  /**
   * Get encumbrance details for awarded proposals
   * 
   * @param proposal
   * @return
   */
  public static JSONObject bidtoProposalEncumChanges(EscmProposalMgmt proposal,
      boolean isFromUnifiedProposal) {

    Query query = null;
    BigDecimal proposalAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO, reqLnNetAmt = BigDecimal.ZERO;

    JSONArray encListArray = null;
    JSONObject json = null, result = new JSONObject();
    try {
      OBContext.setAdminMode();
      String sqlQuery = null;
      if (isFromUnifiedProposal) {
        sqlQuery = " select prlln.em_efin_c_validcombination_id as procomid,sum(coalesce((prlln.awardedamount),0)) as proposalamt,"
            + " (select coalesce(sum(reqln.priceactual* srcref.quantity),0) as srcamt   "
            + " from escm_bidsourceref srcref  join escm_bidmgmt_line ln on ln.escm_bidmgmt_line_id= srcref.escm_bidmgmt_line_id "
            + " join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id and reqln.em_escm_issummary='N' "
            + "  where ln.escm_bidmgmt_id=bidln.escm_bidmgmt_id and ln.c_validcombination_id=prlln.em_efin_c_validcombination_id "
            + "  group by ln.c_validcombination_id), bid.efin_budget_manencum_id   "
            + "  from escm_proposalmgmt_line prlln "
            + "  join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= prlln.escm_bidmgmt_line_id "
            + "   join escm_bidmgmt bid on bid.escm_bidmgmt_id=bidln.escm_bidmgmt_id "
            + " left join efin_budget_manencumlines manenc on manenc.efin_budget_manencumlines_id= bidln.em_efin_budgmanencumline_id  "
            + " left join escm_bidsourceref bidsrcref on bidsrcref.escm_bidmgmt_line_id=bidln.escm_bidmgmt_line_id  "
            + " left join m_requisitionline reqln on reqln.m_requisitionline_id=bidsrcref.m_requisitionline_id "
            + "  and bidln.issummarylevel='N'  "
            + "  where prlln.escm_proposalmgmt_id = ? and prlln.issummarylevel='N' "
            + "  group by bidln.escm_bidmgmt_id,prlln.em_efin_c_validcombination_id "
            + " ,bid.efin_budget_manencum_id ";
      } else {
        sqlQuery = " select prlln.em_efin_c_validcombination_id as procomid,sum(coalesce((prlln.awardedamount),0)) as proposalamt,"
            + " (select coalesce(sum(reqln.priceactual* propln.awardedqty),0) as srcamt   "
            + " from escm_proposalmgmt_line propln join escm_bidmgmt_line ln on ln.escm_bidmgmt_line_id= propln.escm_bidmgmt_line_id  "
            + " join escm_bidsourceref srcref on ln.escm_bidmgmt_line_id= srcref.escm_bidmgmt_line_id   "
            + " join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id and reqln.em_escm_issummary='N' "
            + " where ln.escm_bidmgmt_id=bidln.escm_bidmgmt_id and ln.c_validcombination_id=prlln.em_efin_c_validcombination_id "
            + " and propln.escm_proposalmgmt_id = prlln.escm_proposalmgmt_id"
            + " group by ln.c_validcombination_id), bid.efin_budget_manencum_id   "
            + " from escm_proposalmgmt_line prlln "
            + " join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= prlln.escm_bidmgmt_line_id "
            + " join escm_bidmgmt bid on bid.escm_bidmgmt_id=bidln.escm_bidmgmt_id "
            + " left join efin_budget_manencumlines manenc on manenc.efin_budget_manencumlines_id= bidln.em_efin_budgmanencumline_id  "
            + " left join escm_bidsourceref bidsrcref on bidsrcref.escm_bidmgmt_line_id=bidln.escm_bidmgmt_line_id  "
            + " left join m_requisitionline reqln on reqln.m_requisitionline_id=bidsrcref.m_requisitionline_id "
            + " and bidln.issummarylevel='N'  "
            + " where prlln.escm_proposalmgmt_id = ? and prlln.issummarylevel='N' "
            + " group by bidln.escm_bidmgmt_id,prlln.em_efin_c_validcombination_id,  prlln.escm_proposalmgmt_id, "
            + " bid.efin_budget_manencum_id ";

      }
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setParameter(0, proposal.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();

      encListArray = new JSONArray();

      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();

          proposalAmt = BigDecimal.ZERO;
          reqLnNetAmt = BigDecimal.ZERO;
          diff = BigDecimal.ZERO;

          if (row[1] != null)
            proposalAmt = new BigDecimal(row[1].toString());

          if (row[3] != null) {

          }
          if (row[2] != null)
            reqLnNetAmt = new BigDecimal(row[2].toString());

          diff = proposalAmt.subtract(reqLnNetAmt);
          // if (diff.compareTo(BigDecimal.ZERO) < 0) {

          json = new JSONObject();
          json.put("validComId", row[0].toString());
          json.put("proposalAmt", proposalAmt);
          json.put("reqLnNetAmt", reqLnNetAmt);
          json.put("amount", diff);
          json.put("encumId", row[3].toString());

          encListArray.put(json);

          // }
        }
        result.put("resultList", encListArray);
      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while bidtoProposalEncumChanges: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in bidtoProposalEncumChanges " + e.getMessage(), e);
    }
    return result;
  }

  /**
   * Split bid encumbrance
   * 
   * @param proposal
   * @param result
   */
  public static void splitBidEncumbrance(EscmProposalMgmt proposal, JSONObject result) {

    EfinBudgetManencum newEncumbrance = null, oldEncumbrance = null;
    EfinBudgetManencumlines oldEncumLine = null;
    AccountingCombination com = null;
    JSONObject jsonencum = null;
    try {
      OBContext.setAdminMode();
      if (proposal.getEscmBidmgmt().getEncumbrance() != null) {
        oldEncumbrance = proposal.getEscmBidmgmt().getEncumbrance();

        // create the Encumbrance
        newEncumbrance = ProposalManagementActionMethod.insertEncumbranceproposal(proposal,
            oldEncumbrance);

        if (result != null && result.getJSONArray("resultList") != null) {
          // result1 = result.getJSONObject("resultList");
          JSONArray array = result.getJSONArray("resultList");
          for (int i = 0; i < array.length(); i++) {
            jsonencum = array.getJSONObject(i);
            // JSONArray encumarray = json.getJSONArray("encList");

            // for (int j = 0; j < encumarray.length(); j++) {
            // jsonencum = encumarray.getJSONObject(j);
            if (jsonencum.has("validComId") && jsonencum.getString("validComId") != null
                && jsonencum.has("encumId") && jsonencum.getString("encumId") != null
                && jsonencum.has("proposalAmt") && jsonencum.getString("proposalAmt") != null) {

              oldEncumLine = ProposalManagementActionMethod.getEncumbranceLine(
                  jsonencum.getString("encumId"), jsonencum.getString("validComId"));

              com = OBDal.getInstance().get(AccountingCombination.class,
                  jsonencum.getString("validComId"));

              BigDecimal proposalAmt = new BigDecimal(jsonencum.getString("proposalAmt"));

              // insert the Encumbrance lines and modification
              encumbranceLinesChanges(proposal, newEncumbrance, oldEncumbrance, proposalAmt, com,
                  oldEncumLine);

            }

          }
        }

        newEncumbrance.setDocumentStatus("CO");
        newEncumbrance.setAction("PD");
        OBDal.getInstance().save(newEncumbrance);

        proposal.setEfinEncumbrance(newEncumbrance);
        proposal.setEfinIsbudgetcntlapp(true);

        // update encumbrance value in proposal attribute tab
        OBQuery<EscmProposalAttribute> proposalAttrQry = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id = :proposalId");
        proposalAttrQry.setNamedParameter("proposalId", proposal.getId());

        if (proposalAttrQry != null) {
          List<EscmProposalAttribute> proposalAttrList = proposalAttrQry.list();
          if (proposalAttrList.size() > 0) {
            EscmProposalAttribute proposalAttr = proposalAttrList.get(0);
            proposalAttr.setEFINManualEncumbrance(newEncumbrance);
            proposalAttr.setEFINEncumbranceMethod(newEncumbrance.getEncumMethod());
            OBDal.getInstance().save(proposalAttr);
          }
        }
        OBDal.getInstance().save(proposal);
        OBDal.getInstance().flush();

      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while splitBidEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in splitBidEncumbrance " + e.getMessage(), e);
    }
  }

  /**
   * Insert modification and insert encum lines
   * 
   * @param proposal
   * @param encumbrancenewObj
   * @param oldencumbranceObj
   * @param amount
   * @param com
   * @param oldEncumLine
   */
  public static void encumbranceLinesChanges(EscmProposalMgmt proposal,
      EfinBudgetManencum encumbrancenewObj, EfinBudgetManencum oldencumbranceObj, BigDecimal amount,
      AccountingCombination com, EfinBudgetManencumlines oldEncumLine) {

    EfinBudgetManencumlines manualline = null;
    EfinBudManencumRev manEncumRev = null;
    try {
      OBContext.setAdminMode();
      if (amount.compareTo(BigDecimal.ZERO) > 0) {
        manEncumRev = BidManagementDAO.insertEncumbranceModification(oldEncumLine, amount.negate(),
            null, "PRO", null, null);
        manualline = insertEncumbranceLinesProposal(proposal, encumbrancenewObj, oldencumbranceObj,
            amount, com);

        if (manualline != null && manEncumRev != null) {
          manEncumRev.setSRCManencumline(manualline);
          OBDal.getInstance().save(manEncumRev);
        }
        oldEncumLine.setAPPAmt(oldEncumLine.getAPPAmt().subtract(amount));
        OBDal.getInstance().save(oldEncumLine);

      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in encumbranceLinesChanges " + e.getMessage());
    }
  }

  /**
   * Insert encumbrance line and update in proposal lines
   * 
   * @param proposal
   * @param encumbrancenewObj
   * @param oldencumbranceObj
   * @param totalAmount
   * @param com
   * @return
   */
  public static EfinBudgetManencumlines insertEncumbranceLinesProposal(EscmProposalMgmt proposal,
      EfinBudgetManencum encumbrancenewObj, EfinBudgetManencum oldencumbranceObj,
      BigDecimal totalAmount, AccountingCombination com) {
    Long lineno = 10L;
    EfinBudgetManencumlines manualline = null;
    try {
      OBContext.setAdminMode();
      List<EfinBudgetManencumlines> newEncumLnList = new ArrayList<EfinBudgetManencumlines>();

      // check already unqiuecode exists or not
      OBQuery<EfinBudgetManencumlines> lnexistQry = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:acctID");
      lnexistQry.setNamedParameter("encumID", encumbrancenewObj.getId());
      lnexistQry.setNamedParameter("acctID", com);
      lnexistQry.setMaxResult(1);
      newEncumLnList = lnexistQry.list();
      // if exists update the amount, revision amount ,applied amount
      if (newEncumLnList.size() > 0) {
        manualline = newEncumLnList.get(0);
        manualline.setAmount(manualline.getAmount().add(totalAmount));
        manualline.setRevamount(manualline.getRevamount().add(totalAmount));
        manualline.setRemainingAmount(BigDecimal.ZERO);
        manualline.setOriginalamount(manualline.getOriginalamount().add(totalAmount));
        manualline.setAPPAmt(manualline.getAPPAmt().add(totalAmount));
        OBDal.getInstance().save(manualline);
      }
      // if not exists then insert the Encumbrance lines
      else {
        manualline = OBProvider.getInstance().get(EfinBudgetManencumlines.class);
        manualline.setClient(encumbrancenewObj.getClient());
        manualline.setOrganization(encumbrancenewObj.getOrganization());
        manualline.setUpdatedBy(encumbrancenewObj.getCreatedBy());
        manualline.setCreationDate(new java.util.Date());
        manualline.setCreatedBy(encumbrancenewObj.getCreatedBy());
        manualline.setUpdated(new java.util.Date());
        manualline.setLineNo(lineno);
        if (com != null) {
          manualline.setUniquecode(com.getEfinUniqueCode());
          manualline.setSalesRegion(com.getSalesRegion());
          manualline.setAccountElement(com.getAccount());
          manualline.setSalesCampaign(com.getSalesCampaign());
          manualline.setProject(com.getProject());
          manualline.setActivity(com.getActivity());
          manualline.setStDimension(com.getStDimension());
          manualline.setNdDimension(com.getNdDimension());
          manualline.setAccountingCombination(com);
        }

        manualline.setBudgetLines(null);
        manualline.setManualEncumbrance(encumbrancenewObj);
        manualline.setAmount(totalAmount);
        manualline.setRevamount(totalAmount);
        manualline.setRemainingAmount(BigDecimal.ZERO);
        manualline.setOriginalamount(totalAmount);
        manualline.setAPPAmt(totalAmount);
        manualline.setUsedAmount(BigDecimal.ZERO);
        lineno += 10;
        OBDal.getInstance().save(manualline);
        OBDal.getInstance().flush();

        // update encumbranceline in proposal management
        ProposalManagementActionMethod.updateEncumbranceLineInProposal(proposal, manualline, com);
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in insertEncumbranceLinesProposal " + e.getMessage());
    }
    return manualline;
  }

  // update proposal header status based on next approver
  public static boolean updateUnifiedProposalHeader(NextRoleByRuleVO nextApproval,
      EscmProposalMgmt proposalmgmt) {
    EutNextRole nextRole = null;
    try {
      OBContext.setAdminMode();

      if (nextApproval.getNextRoleId() == null) {
        proposalmgmt.setUpdated(new java.util.Date());
        proposalmgmt.setUpdatedBy(OBContext.getOBContext().getUser());
        proposalmgmt.setProposalappstatus("APP");
        proposalmgmt.setEscmUnifiedProposalAction("PD");
        proposalmgmt.setEUTNextRole(null);

      } else {
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        proposalmgmt.setUpdated(new java.util.Date());
        proposalmgmt.setUpdatedBy(OBContext.getOBContext().getUser());
        proposalmgmt.setProposalappstatus("INP");
        proposalmgmt.setEscmUnifiedProposalAction("AP");
        proposalmgmt.setEUTNextRole(nextRole);
      }

      return true;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in updateUnifiedProposalHeader : ", e);
      return false;
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  /**
   * Common method to insert approval history
   * 
   * @param data
   *          {@link JSONObject} containing the data like the approval action and the next performer
   *          etc.
   * @return The count of the inserted lines.
   */

  public static int InsertApprovalHistory(JSONObject data) {
    int count = 0;

    try {
      OBContext.setAdminMode();
      StringBuilder queryBuilder = new StringBuilder();
      String historyId = SequenceIdData.getUUID();
      String strTableName = data.getString("HistoryTable");

      queryBuilder.append(" INSERT INTO  ").append(strTableName);
      queryBuilder.append(" ( ").append(strTableName.concat("_id"))
          .append(", ad_client_id, ad_org_id,");
      queryBuilder.append(" createdby, updatedby,   ").append(data.getString("HeaderColumn"))
          .append(" , approveddate, ");
      queryBuilder.append(" comments, ").append(data.getString("ActionColumn"))
          .append(" , pendingapproval, seqno,ad_role_id,is_unifiedProposal)");
      queryBuilder.append(" VALUES (?, ?, ?, ");
      queryBuilder.append(" ?, ?,?, ?,");
      queryBuilder.append(" ?, ?, ?, ?,?,?);");
      PreparedStatement query = OBDal.getInstance().getConnection()
          .prepareStatement(queryBuilder.toString());
      query.setString(1, historyId);
      query.setString(2, data.getString("ClientId"));
      query.setString(3, data.getString("OrgId"));
      query.setString(4, data.getString("UserId"));
      query.setString(5, data.getString("UserId"));
      query.setString(6, data.getString("HeaderId"));
      query.setTimestamp(7, new java.sql.Timestamp(System.currentTimeMillis()));
      query.setString(8, data.getString("Comments"));
      query.setString(9, data.getString("Status"));
      query.setString(10, data.optString("NextApprover"));
      query.setInt(11, UtilityDAO.getHistorySequence(strTableName, data.getString("HeaderColumn"),
          data.getString("HeaderId")));
      query.setString(12, data.optString("RoleId"));
      query.setString(13, "Y");
      log.debug("History Query: " + query.toString());
      count = query.executeUpdate();
    } catch (Exception e) {
      count = 0;
      log.error("Exception while InsertApprovalHistory(): ", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Split bid encumbrance
   * 
   * @param proposal
   * @param result
   */
  public static OBError createSplitEncumbrance(EscmProposalMgmt proposal, JSONObject result,
      boolean appliedAmtCheck) {

    EfinBudgetManencum newEncumbrance = null, oldEncumbrance = null;
    EfinBudgetManencumlines oldEncumLine = null;
    AccountingCombination com = null;
    JSONObject jsonencum = null;

    String proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%",
        proposal.getProposalno());

    try {
      OBContext.setAdminMode();
      if (proposal.getEscmBidmgmt().getEncumbrance() != null) {
        oldEncumbrance = proposal.getEscmBidmgmt().getEncumbrance();

        if (!appliedAmtCheck) {
          // create the Encumbrance
          newEncumbrance = ProposalManagementActionMethod.insertEncumbranceproposal(proposal,
              oldEncumbrance);
        }

        if (result != null && result.getJSONArray("resultList") != null) {
          // result1 = result.getJSONObject("resultList");
          JSONArray array = result.getJSONArray("resultList");
          for (int i = 0; i < array.length(); i++) {
            jsonencum = array.getJSONObject(i);

            if (jsonencum.has("validComId") && jsonencum.getString("validComId") != null
                && jsonencum.has("encumId") && jsonencum.getString("encumId") != null
                && jsonencum.has("proposalAmt") && jsonencum.getString("proposalAmt") != null
                && jsonencum.has("reqLnNetAmt") && jsonencum.getString("reqLnNetAmt") != null) {

              String message = null;

              oldEncumLine = ProposalManagementActionMethod.getEncumbranceLine(
                  jsonencum.getString("encumId"), jsonencum.getString("validComId"));

              com = OBDal.getInstance().get(AccountingCombination.class,
                  jsonencum.getString("validComId"));

              BigDecimal proposalAmt = new BigDecimal(jsonencum.getString("proposalAmt"));
              BigDecimal reqLineAmt = new BigDecimal(jsonencum.getString("reqLnNetAmt"));

              BigDecimal diff = proposalAmt.subtract(reqLineAmt);

              if (proposal.getEscmBidmgmt().getEncumbrance().getEncumMethod().equals("M")) {
                // new uniquecode
                if (oldEncumLine == null) {

                  if (appliedAmtCheck) {
                    message = OBMessageUtils.messageBD("EFIN_PropNewUniqNotAllow");
                  }
                  OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance().createQuery(
                      EscmProposalmgmtLine.class, " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                          + " and e.escmProposalmgmt.id = :proposalID and (status != 'CL' or status is null) ");
                  proposalLineQry.setNamedParameter("uniqueCodeID", com.getId());
                  proposalLineQry.setNamedParameter("proposalID", proposal.getId());
                  if (proposalLineQry != null) {
                    List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
                    if (proposalLineList.size() > 0) {
                      for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                        proposalLine.setEfinFailureReason(message);
                        OBDal.getInstance().save(proposalLine);
                      }
                    }
                  }
                } else {

                  // decrease
                  if (diff.compareTo(BigDecimal.ZERO) <= 0) {

                    if (!appliedAmtCheck) {
                      // insert the Encumbrance lines and modification
                      encumbranceLinesChanges(proposal, newEncumbrance, oldEncumbrance, proposalAmt,
                          com, oldEncumLine);

                    }
                  } else if (diff.compareTo(BigDecimal.ZERO) > 0) {

                    // increase
                    // Check in remaining amt
                    BigDecimal unAppAmt = (oldEncumLine.getRevamount()
                        .subtract(oldEncumLine.getAPPAmt()).subtract(oldEncumLine.getUsedAmount()));

                    if (diff.compareTo(unAppAmt) > 0) {

                      // Award amt is greater than remaining amt, update failure reason
                      if (appliedAmtCheck) {
                        message = OBMessageUtils.messageBD("Escm_AwardedAmtLess");
                      }
                      OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance()
                          .createQuery(EscmProposalmgmtLine.class,
                              " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                                  + " and e.escmProposalmgmt.id = :proposalID and (status != 'CL' or status is null) ");
                      proposalLineQry.setNamedParameter("uniqueCodeID", com.getId());
                      proposalLineQry.setNamedParameter("proposalID", proposal.getId());
                      if (proposalLineQry != null) {
                        List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
                        if (proposalLineList.size() > 0) {
                          for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                            proposalLine.setEfinFailureReason(message);
                            OBDal.getInstance().save(proposalLine);
                          }
                        }
                      }

                    } else {
                      // If there are some remaining amount, move it to applied amount
                      if (!appliedAmtCheck) {

                        oldEncumLine.setAPPAmt(oldEncumLine.getAPPAmt().add(diff));
                        OBDal.getInstance().save(oldEncumLine);
                        OBDal.getInstance().flush();

                        // insert the Encumbrance lines and modification
                        encumbranceLinesChanges(proposal, newEncumbrance, oldEncumbrance,
                            proposalAmt, com, oldEncumLine);

                      }
                    }
                  }
                }
              } else {
                // Auto encumbrance
                if (oldEncumLine == null) {

                  if (appliedAmtCheck) {

                    JSONObject commonvalresult = CommonValidationsDAO.CommonFundsChecking(
                        proposal.getEscmBidmgmt().getEncumbrance().getBudgetInitialization(), com,
                        proposalAmt);

                    if (commonvalresult.has("errorFlag")
                        && commonvalresult.getString("errorFlag").equals("0")
                        && commonvalresult.has("message")) {

                      OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance()
                          .createQuery(EscmProposalmgmtLine.class,
                              " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                                  + " and e.escmProposalmgmt.id = :proposalID and (status != 'CL' or status is null) ");
                      proposalLineQry.setNamedParameter("uniqueCodeID", com.getId());
                      proposalLineQry.setNamedParameter("proposalID", proposal.getId());
                      if (proposalLineQry != null) {
                        List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
                        if (proposalLineList.size() > 0) {
                          for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                            proposalLine.setEfinFailureReason(commonvalresult.getString("message"));
                            OBDal.getInstance().save(proposalLine);
                          }
                          OBDal.getInstance().flush();
                        }
                      }
                    }
                  }
                } else {

                  if (!appliedAmtCheck) {
                    // decrease
                    if (diff.compareTo(BigDecimal.ZERO) < 0) {

                      // do decrease first, then do modification
                      BidManagementDAO.insertEncumbranceModification(oldEncumLine, diff, null,
                          "PRO", null, null);

                      oldEncumLine.setAPPAmt(oldEncumLine.getAPPAmt().add(diff));
                      OBDal.getInstance().save(oldEncumLine);

                      // insert the Encumbrance lines and modification
                      encumbranceLinesChanges(proposal, newEncumbrance, oldEncumbrance, proposalAmt,
                          com, oldEncumLine);

                    } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                      // do increase first, then do modification
                      BidManagementDAO.insertEncumbranceModification(oldEncumLine, diff, null,
                          "PRO", null, null);

                      oldEncumLine.setAPPAmt(oldEncumLine.getAPPAmt().add(diff));
                      OBDal.getInstance().save(oldEncumLine);

                      // insert the Encumbrance lines and modification
                      encumbranceLinesChanges(proposal, newEncumbrance, oldEncumbrance, proposalAmt,
                          com, oldEncumLine);
                    } else {
                      // insert the Encumbrance lines and modification
                      encumbranceLinesChanges(proposal, newEncumbrance, oldEncumbrance, proposalAmt,
                          com, oldEncumLine);
                    }
                  }
                }
              }
            }
          }

          if (!appliedAmtCheck) {
            newEncumbrance.setDocumentStatus("CO");
            newEncumbrance.setAction("PD");
            OBDal.getInstance().save(newEncumbrance);

            proposal.setEfinEncumbrance(newEncumbrance);
            proposal.setEfinIsbudgetcntlapp(true);

            OBDal.getInstance().save(proposal);
            OBDal.getInstance().flush();
          }

        }

        if (appliedAmtCheck) {

          List<EscmProposalmgmtLine> proposalLineList = proposal.getEscmProposalmgmtLineList()
              .stream().filter(a -> a.getEfinFailureReason() != null).collect(Collectors.toList());

          if (proposalLineList.size() > 0) {
            OBError result1 = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
                "@Efin_Chk_Line_Info@");
            return result1;
          }

        }

      }

      OBError error = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return error;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while getBidtoUnifiedProposalDetails: " + e);
      OBError error = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in getBidtoUnifiedProposalDetails:" + e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  /**
   * If proposals are having same manual encumbrance, add all proposal amount and check if there is
   * remaining amount
   * 
   * @param proposal
   * @param appliedamtchk
   * @return
   */
  public static boolean checkManualEncumRemAmt(EscmProposalMgmt proposal, boolean appliedamtchk) {

    Query query = null;
    boolean errorflag = false;
    String message = null;
    try {
      OBContext.setAdminMode();
      for (EscmProposalmgmtLine ln : proposal.getEscmProposalmgmtLineList()) {
        ln.setEfinFailureReason(null);
        OBDal.getInstance().save(ln);
      }
      OBDal.getInstance().flush();

      String sqlQuery = "  select manln.c_validcombination_id  ,coalesce( sum (awardedamount),0) ,  "
          + "   coalesce(manln.revamount,0)- coalesce(app_amt,0)- coalesce(used_amount,0) as unapamt ,  "
          + "   case when coalesce( sum (awardedamount),0) > coalesce(manln.revamount,0)- coalesce(app_amt,0)- coalesce(used_amount,0)  "
          + "   then 'f' else 't' end ,manln.efin_budget_manencumlines_id, manln.efin_budget_manencum_id "
          + "   from escm_proposalmgmt_line  ln   "
          + "   join escm_proposalmgmt pro on pro.escm_proposalmgmt_id= ln.escm_proposalmgmt_id  "
          + "   join efin_budget_manencumlines manln on manln.efin_budget_manencum_id= pro.em_efin_encumbrance_id  "
          + "   and manln.c_validcombination_id= ln.em_efin_c_validcombination_id and ln.issummarylevel='N' "
          + "   and (ln.status != 'CL' or ln.status is null)  where pro.escm_bidmgmt_id  =  ?   "
          + "   group by manln.c_validcombination_id  ,manln.revamount ,used_amount,app_amt,manln.efin_budget_manencumlines_id ";

      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      query.setParameter(0, proposal.getEscmBidmgmt().getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          if (appliedamtchk) {
            if (row[3] != null && row[0] != null && row[3].equals("f")) {
              message = OBMessageUtils.messageBD("Efin_ReqAmt_More");
              errorflag = true;

              String lineQuery = " as e where e.eFINUniqueCode.id = :uniqueCodeID "
                  + " and e.escmBidmgmtLine.escmBidmgmt.id = :bidID and awardedamount > 0 and awardedqty > 0 "
                  + " and (status != 'CL' or status is null) and e.summary = false "
                  + " and e.escmProposalmgmt.efinEncumbrance.id = :encumbranceID ";

              OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance()
                  .createQuery(EscmProposalmgmtLine.class, lineQuery);
              proposalLineQry.setNamedParameter("uniqueCodeID", row[0].toString());
              proposalLineQry.setNamedParameter("bidID", proposal.getEscmBidmgmt().getId());
              proposalLineQry.setNamedParameter("encumbranceID", row[5].toString());

              List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
              if (proposalLineList.size() > 0) {
                for (EscmProposalmgmtLine proposalLine : proposalLineList) {
                  proposalLine.setEfinFailureReason(message);
                  OBDal.getInstance().save(proposalLine);
                }
                OBDal.getInstance().flush();
              }
            }
          }
        }
      }
      return errorflag;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in checkManualEncumRemAmt " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return true;
    }
  }

  /**
   * Revert awarded quantity in Proposal
   * 
   * @param proposal
   * @return
   */
  public static OBError revertAwardedQtyProposal(List<EscmProposalMgmt> proposalList) {
    try {
      OBContext.setAdminMode();
      for (EscmProposalMgmt proposal : proposalList) {
        if (proposal != null && proposal.getProposalstatus().equals("PAWD")) {
          for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
            if (line.getAwardedqty().compareTo(BigDecimal.ZERO) > 0) {
              line.setAwardedamount(BigDecimal.ZERO);
              line.setAwardedqty(BigDecimal.ZERO);
              OBDal.getInstance().save(line);
            }
          }
          proposal.setProposalstatus("ANY");
          proposal.setAwardamount(BigDecimal.ZERO);
          OBDal.getInstance().save(proposal);
          OBDal.getInstance().flush();
        }
      }
      OBError error = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return error;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while revertAwardedQtyProposal: " + e);
      OBError error = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in revertAwardedQtyProposal:" + e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

}
