package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAO;
import sa.elm.ob.scm.actionHandler.dao.ProposalManagementDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Kiruthika on 15/06/2020
 */

// Approval Flow of Proposal Management
public class UnifiedProposalRejectMethods {
  private static final Logger log = LoggerFactory.getLogger(UnifiedProposalRejectMethods.class);

  public static OBError proposalRejectValidation(String proposalId, String clientId, String orgId,
      String userId, String roleId, String tabId) throws Exception {
    // TODO Auto-generated method stub
    try {
      OBContext.setAdminMode();

      // Variable declaration
      EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);
      ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
      String DocStatus = proposalmgmt.getProposalappstatus();

      Boolean allowReject = false;
      String documentType = proposalmgmt.getProposalType().equals("DR") ? "EUT_122" : "EUT_117";

      if (proposalmgmt.getEUTForwardReqmoreinfo() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(
            proposalmgmt.getEUTForwardReqmoreinfo(), userId, roleId, documentType);
      }
      if (proposalmgmt.getEUTReqmoreinfo() != null
          || ((proposalmgmt.getEUTForwardReqmoreinfo() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        return result;
      }

      if (DocStatus.equals("REJ")) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        return result;
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while proposalRejectValidation: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in proposalRejectValidation:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error;

    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  /**
   * Encumbrance validation for reject
   * 
   * @param proposalmgmt
   * @param vars
   * @return
   */
  public static OBError encumbranceValidationReject(EscmProposalMgmt proposalmgmt,
      VariablesSecureApp vars) {

    boolean errorFlag = false;
    try {
      OBContext.setAdminMode();
      // Variable declaration
      ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();
      // Task No.5768
      Boolean fromPR = false;

      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);
      // End Task No.5925

      // pre validation before reject the Proposal
      if (enccontrollist.size() > 0) {
        // check budget controller approved or not , if approved do the prevalidation
        if (proposalmgmt.isEfinIsbudgetcntlapp()) {

          // check lines added from pr ( direct PR- proposal)
          for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
            List<EscmProposalsourceRef> proposalsrcref = proposalDAO
                .checkLinesAddedFromPR(line.getId());
            if (proposalsrcref != null && proposalsrcref.size() > 0) {
              fromPR = true;
              break;
            }
          }
          // if lines not added from PR then do the further validation
          if (!fromPR) {

            // if both auto & manual encumbrance with proposal encumbrance type
            if (proposalmgmt.getEfinEncumbrance() != null
                && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {

              // check encumbrance used or not based on used amount - for both manual & auto
              if (proposalmgmt.getEfinEncumbrance() != null)
                errorFlag = ProposalManagementRejectMethods
                    .chkManualEncumbranceRejValid(proposalmgmt);
              if (errorFlag) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Encum_Used_Cannot_Rej@");
                return result;
              }
            }
          }
        }
      }
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while encumbranceValidationReject: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in encumbranceValidationReject:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  /**
   * Update encumbrance for awarded proposal
   * 
   * @param proposalmgmt
   * @param vars
   * @return
   */
  public static OBError updateProposalEncumbranceReject(EscmProposalMgmt proposalmgmt,
      VariablesSecureApp vars) {

    try {
      OBContext.setAdminMode();
      // Task No.5768
      Boolean fromPR = false;
      EfinBudgetManencum encumbrance = null;
      ProposalManagementDAO proposalDAO = new ProposalManagementDAOImpl();

      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);
      // End Task No.5925

      // Task No.5925
      if (enccontrollist.size() > 0 && proposalmgmt.isEfinIsbudgetcntlapp()) {

        // check lines added from pr ( direct PR- proposal)
        for (EscmProposalmgmtLine line : proposalmgmt.getEscmProposalmgmtLineList()) {
          List<EscmProposalsourceRef> proposalsrcref = proposalDAO
              .checkLinesAddedFromPR(line.getId());
          if (proposalsrcref != null && proposalsrcref.size() > 0) {
            fromPR = true;
            break;
          }
        }

        // if associate proposal line does not have PR
        if (!fromPR) {
          // if proposal is manual encumbrance then reverse applied amount
          if (proposalmgmt.getEfinEncumbrance() != null) {
            if (proposalmgmt.getEscmBidmgmt() != null
                && proposalmgmt.getEscmBidmgmt().getEncumbrance() == null
                && proposalmgmt.getEfinEncumbrance().getEncumType().equals("PAE")) {
              if (proposalmgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {
                ProposalManagementRejectMethods.updateManualEncumAppAmt(proposalmgmt, false);
                proposalmgmt.setEfinIsbudgetcntlapp(false);
                OBDal.getInstance().save(proposalmgmt);

              }
              // if auto the delete the new encumbrance and update the budget inquiry funds
              // available
              else {

                // remove encum
                EfinBudgetManencum encum = proposalmgmt.getEfinEncumbrance();

                ProposalManagementRejectMethods.updateAutoEncumbrancechanges(proposalmgmt, false);

                // remove encum reference in proposal lines.
                List<EscmProposalmgmtLine> proline = proposalmgmt.getEscmProposalmgmtLineList();
                for (EscmProposalmgmtLine proLineList : proline) {
                  proLineList.setEfinBudgmanencumline(null);
                  OBDal.getInstance().save(proLineList);
                }

                // OBDal.getInstance().flush();
                OBDal.getInstance().remove(encum);

                // update the budget controller flag and encumbrance ref
                proposalmgmt.setEfinEncumbrance(null);
                if (proposalmgmt.getEfinEncumbrance() != null) {
                  encumbrance = proposalmgmt.getEfinEncumbrance();
                  encumbrance.setBusinessPartner(null);
                  OBDal.getInstance().save(encumbrance);
                }

                // update encumbrance value in proposal attribute tab
                OBQuery<EscmProposalAttribute> proposalAttrQry = OBDal.getInstance().createQuery(
                    EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id = :proposalId");
                proposalAttrQry.setNamedParameter("proposalId", proposalmgmt.getId());

                if (proposalAttrQry != null) {
                  List<EscmProposalAttribute> proposalAttrList = proposalAttrQry.list();
                  if (proposalAttrList.size() > 0) {
                    EscmProposalAttribute proposalAttr = proposalAttrList.get(0);
                    proposalAttr.setEFINManualEncumbrance(null);
                    proposalAttr.setEFINEncumbranceMethod("A");
                    OBDal.getInstance().save(proposalAttr);
                  }
                }

                proposalmgmt.setEfinIsbudgetcntlapp(false);
                OBDal.getInstance().save(proposalmgmt);
              }
            }
          }
        }

      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while updateProposalEncumbranceReject: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in updateProposalEncumbranceReject:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  /**
   * If proposal is fully awarded, move encumbrance stage
   * 
   * 
   * @param proposalmgmt
   * @param vars
   * @return
   */
  public static OBError changeEncumStageRej(EscmProposalMgmt proposalmgmt,
      VariablesSecureApp vars) {

    try {
      OBContext.setAdminMode();
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposalmgmt);
      // End Task No.5925

      // Task No.5925
      if (enccontrollist.size() > 0 && proposalmgmt.isEfinIsbudgetcntlapp()) {

        EfinBudgetManencum encumbrance = proposalmgmt.getEscmBidmgmt().getEncumbrance();
        encumbrance.setEncumStage("BE");
        proposalmgmt.setEfinIsbudgetcntlapp(false);
        OBDal.getInstance().save(encumbrance);
      }

      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while changeEncumStageRej: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in changeEncumStageRej:" + e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  /**
   * reactivate the split encumbrance from the proposal
   * 
   * @param resultEncum
   * @param proposal
   */
  public static void reactivateSplitBid(EscmProposalMgmt proposal, Boolean isCancel,
      List<String> proposalList) {
    List<EfinBudgetManencumlines> manenculine = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencum encumbrance = null;
    EfinBudgetManencumlines srcEncumLines = null;
    List<EfinBudManencumRev> revList = null;
    try {
      OBContext.setAdminMode();
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposal);
      // End Task No.5925

      // Task No.5925
      if (enccontrollist.size() > 0 && proposal.isEfinIsbudgetcntlapp()) {

        Map<EfinBudgetManencumlines, Double> proposalLineMap = proposal
            .getEscmProposalmgmtLineList().stream().filter(a -> !a.isSummary())
            .collect(Collectors.groupingBy(EscmProposalmgmtLine::getEfinBudgmanencumline,
                Collectors.summingDouble(a -> a.getAwardedamount().doubleValue())));

        for (Map.Entry<EfinBudgetManencumlines, Double> entry : proposalLineMap.entrySet()) {

          BigDecimal revAmtSum = BigDecimal.ZERO;

          EfinBudgetManencumlines newEncumlines = entry.getKey();

          OBQuery<EfinBudManencumRev> revAmtQuery = OBDal.getInstance().createQuery(
              EfinBudManencumRev.class, " as e where e.sRCManencumline.id = :proposalEncumLineID");
          revAmtQuery.setNamedParameter("proposalEncumLineID", newEncumlines.getId());

          if (revAmtQuery != null) {
            List<EfinBudManencumRev> revAmtList = revAmtQuery.list();
            if (revAmtList.size() > 0) {

              Map<EfinBudgetManencumlines, Double> encumLineRevAmtMap = revAmtList.stream()
                  .collect(Collectors.groupingBy(EfinBudManencumRev::getManualEncumbranceLines,
                      Collectors.summingDouble(a -> (a.getRevamount().negate()).doubleValue())));

              for (Map.Entry<EfinBudgetManencumlines, Double> revision : encumLineRevAmtMap
                  .entrySet()) {

                EfinBudgetManencumlines oldEncumLines = revision.getKey();

                revAmtSum = BigDecimal.valueOf(revision.getValue());

                oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(revAmtSum));
                OBDal.getInstance().save(oldEncumLines);

              }
            }
          }
        }

        if (proposal != null)
          encumbrance = proposal.getEfinEncumbrance();

        if (!isCancel) {
          // update encumbrance line is null
          if (proposal != null) {
            for (EscmProposalmgmtLine ln : proposal.getEscmProposalmgmtLineList()) {
              ln.setEfinBudgmanencumline(null);
              OBDal.getInstance().save(ln);
            }
            if (proposal.getEscmBidmgmt() != null
                && proposal.getEscmBidmgmt().getEncumbrance() != null) {
              proposal.setEfinEncumbrance(proposal.getEscmBidmgmt().getEncumbrance());
            } else {
              proposal.setEfinEncumbrance(null);
            }

            encumbrance.setBusinessPartner(null);
            OBDal.getInstance().save(proposal);
          }
        }

        if (encumbrance != null) {
          // fetching revision record based on newly created encumbrance lines
          OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
              .createQuery(EfinBudManencumRev.class, " as e where e.sRCManencumline.id in "
                  + " ( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id=:encumId)");
          revQuery.setNamedParameter("encumId", encumbrance.getId());
          revList = revQuery.list();
          if (revList.size() > 0) {
            for (EfinBudManencumRev rev : revList) {
              srcEncumLines = rev.getSRCManencumline();
              if (!isCancel) {
                rev.setSRCManencumline(null);
                OBDal.getInstance().save(rev);
                OBDal.getInstance().flush();
              }
            }

            // if (proposal.getEfinEncumbrance() != null)
            // encumbrance = proposal.getEfinEncumbrance();
            manenculine = encumbrance.getEfinBudgetManencumlinesList();
            if (!isCancel) {
              encumbrance.setDocumentStatus("DR");
              OBDal.getInstance().save(encumbrance);
            }
            if (manenculine.size() > 0) {
              for (EfinBudgetManencumlines line : manenculine) {
                if (!isCancel)
                  OBDal.getInstance().remove(line);

              }
              if (!isCancel)
                OBDal.getInstance().remove(encumbrance);
            }
            for (EfinBudManencumRev rev : revList) {
              EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
              log.debug("getAccountingCombination:" + lines.getAccountingCombination());
              // lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));

              if (!isCancel) {
                lines.getEfinBudManencumRevList().remove(rev);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in reactivateSplitBid" + e.getMessage());

    }
  }

  /**
   * Get awarded proposal details
   * 
   * @param proposal
   * @return
   */
  public static JSONObject bidtoUnifiedProposalEncumChanges(EscmProposalMgmt proposal) {

    Query query = null;
    BigDecimal proposalAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO, reqLnNetAmt = BigDecimal.ZERO;

    JSONArray encListArray = null;
    JSONObject json = null, result = new JSONObject();
    try {
      OBContext.setAdminMode();
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
      query.setParameter(0, proposal.getEscmBidmgmt().getId());

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
          if (diff.compareTo(BigDecimal.ZERO) < 0) {

            json = new JSONObject();
            json.put("validComId", row[0].toString());
            json.put("proposalAmt", proposalAmt);
            json.put("reqLnNetAmt", reqLnNetAmt);
            json.put("amount", diff);
            json.put("encumId", row[3].toString());

            encListArray.put(json);

          }
        }
        result.put("resultList", encListArray);
      }

    } catch (OBException e) {
      log.error(" Exception while bidtoUnifiedProposalEncumChanges: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception in bidtoUnifiedProposalEncumChanges " + e.getMessage(), e);

    }
    return result;
  }

  /**
   * Revert encumbrance changes
   * 
   * @param proposal
   * @param vars
   * @param appliedamtchk
   * @param proposalList
   * @return
   */
  public static OBError getUnifiedProposaltoBidDetailsRej(EscmProposalMgmt proposal,
      VariablesSecureApp vars, boolean appliedamtchk, List<String> proposalList) {
    Query query = null, query1 = null;
    boolean errorflag = false;
    String message = null;
    BigDecimal proposalAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO, unAppAmt = BigDecimal.ZERO;
    BigDecimal reqLnNetAmt = BigDecimal.ZERO;

    EfinBudgetManencumlines line = null;
    AccountingCombination com = null;

    try {
      OBContext.setAdminMode();
      // Variable declaration
      // Task No.5768

      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposal);
      // End Task No.5925

      // pre validation before reject the Proposal
      if (enccontrollist.size() > 0) {
        if (proposal.isEfinIsbudgetcntlapp()) {

          // before doing validation update all the proposal line failure reason is null
          if (appliedamtchk) {
            String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= null "
                + "  where escm_proposalmgmt_line_id in "
                + " (select escm_proposalmgmt_line_id from escm_proposalmgmt_line prlln"
                + " join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= prlln.escm_bidmgmt_line_id "
                + " join escm_bidmgmt bid on bid.escm_bidmgmt_id=bidln.escm_bidmgmt_id "
                + " where bid.escm_bidmgmt_id = ? and prlln.issummarylevel='N'"
                + " and prlln.awardedamount > 0 and prlln.awardedqty > 0) ";
            query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
            query1.setParameter(0, proposal.getEscmBidmgmt().getId());
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
          query.setParameter(0, proposal.getEscmBidmgmt().getId());
          log.debug("strQuery:" + query.toString());

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

              // if uniquecode present in lines
              if (row[0] != null)
                com = OBDal.getInstance().get(AccountingCombination.class, row[0].toString());

              if (row[1] != null)
                proposalAmt = new BigDecimal(row[1].toString());

              if (row[3] != null) {
                line = ProposalManagementActionMethod.getEncumbranceLine(row[3].toString(),
                    row[0].toString());
                if (line != null)
                  unAppAmt = (line.getRevamount().subtract(line.getAPPAmt())
                      .subtract(line.getUsedAmount()));

              }
              if (row[2] != null)
                reqLnNetAmt = new BigDecimal(row[2].toString());

              if (row[3] != null && line != null)
                line = OBDal.getInstance().get(EfinBudgetManencumlines.class, line.getId());

              // calculate the difference from proposal to bid ( if diff value is negative then
              // decrease
              // or else increase)

              diff = proposalAmt.subtract(reqLnNetAmt);
              // if proposal is manual encumbrance then do the further validaion
              if (proposal.getEscmBidmgmt() != null
                  && proposal.getEscmBidmgmt().getEncumbrance() != null
                  && proposal.getEscmBidmgmt().getEncumbrance().getEncumMethod().equals("M")) {

                // if diff is zero then dont do anything
                if (diff.compareTo(BigDecimal.ZERO) == 0) {

                }
                // increase (if diff is +ve )
                else if (diff.compareTo(BigDecimal.ZERO) > 0) {

                  // in case of manual increase - then reduce
                  // the applied amt
                  if (!appliedamtchk) {
                    // String UpdateQuery = " update efin_budget_manencumlines set app_amt=
                    // app_amt-? "
                    // + " where efin_budget_manencumlines_id = ? ";
                    // query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                    // query1.setParameter(0, diff);
                    // query1.setParameter(1, line.getId());
                    // query1.executeUpdate();
                    // OBDal.getInstance().flush();

                    EfinBudgetManencumlines encumLn = Utility
                        .getObject(EfinBudgetManencumlines.class, line.getId());
                    encumLn.setAPPAmt(encumLn.getAPPAmt().subtract(diff));
                    OBDal.getInstance().save(encumLn);
                    OBDal.getInstance().flush();
                  }
                }
                // decrease ( diff is -ve)
                else {
                  // in case of manual decrease - unapplied amount will be exist then increase the
                  // applied amt
                  if (appliedamtchk) {
                    // check increase applied amount greater than un applied amount or not , if
                    // throw
                    // error
                    if (diff.negate().compareTo(unAppAmt) > 0) {
                      errorflag = true;
                      if (appliedamtchk && errorflag) {
                        message = OBMessageUtils.messageBD("Efin_ReqAmt_More");
                      }
                      String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
                          + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? and (status != 'CL' or status is null) ";
                      query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                      query1.setParameter(0, message);
                      query1.setParameter(1, row[0].toString());
                      query1.setParameter(2, proposal.getId());
                      query1.executeUpdate();
                      OBDal.getInstance().flush();
                    }
                  }
                  // update the applied amount(increase)
                  else {
                    // String UpdateQuery = " update efin_budget_manencumlines set app_amt=
                    // app_amt+? "
                    // + " where efin_budget_manencumlines_id = ? ";
                    // query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                    // query1.setParameter(0, diff.negate());
                    // query1.setParameter(1, line.getId());
                    // query1.executeUpdate();

                    EfinBudgetManencumlines encumLn = Utility
                        .getObject(EfinBudgetManencumlines.class, line.getId());
                    encumLn.setAPPAmt(encumLn.getAPPAmt().add(diff.negate()));
                    OBDal.getInstance().save(encumLn);
                    OBDal.getInstance().flush();
                  }
                }
                // new uniquecode then dont do anything

              }
              // if proposal encumbrance is Auto
              else {
                // if new unique code added in proposal then
                if (row[2] == null) {
                  if (!appliedamtchk) {
                    // Remove ENCUM LINE

                    // delete the encumbrance lines
                    if (proposal.getEscmBidmgmt().getEncumbrance() != null)
                      ProposalManagementRejectMethods.deleteEncumLine(proposal);
                    else if (proposal.getEfinEncumbrance() != null)
                      ProposalManagementRejectMethods
                          .deleteEncumLines(proposal.getEfinEncumbrance(), com, proposal, null);

                    proposal.setEfinEncumbrance(null);

                  } else if (appliedamtchk) {

                    // check funds available in old unique code
                    errorflag = ProposalManagementRejectMethods
                        .chkFundsAvailforReactOldUniqueCode(proposal);

                  }
                }
                // increase
                else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                  if (!appliedamtchk) {

                    // delete encumbrance modification
                    ProposalManagementRejectMethods.deleteModification(line, diff);

                    // String UpdateQuery = " update efin_budget_manencumlines set app_amt= app_amt-
                    // ? "
                    // + " where efin_budget_manencumlines_id = ? ";
                    // query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                    // query1.setParameter(0, diff);
                    // query1.setParameter(1, line.getId());
                    // query1.executeUpdate();

                    EfinBudgetManencumlines encumLn = Utility
                        .getObject(EfinBudgetManencumlines.class, line.getId());
                    encumLn.setAPPAmt(encumLn.getAPPAmt().subtract(diff));
                    OBDal.getInstance().save(encumLn);
                    OBDal.getInstance().flush();
                  }
                }
                // decrease
                else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                  if (appliedamtchk) {
                    errorflag = ProposalManagementActionMethod.chkAutoEncumbranceValidation(
                        proposal, true, com.getId(), false, true, diff.negate());
                  } else {

                    // delete encumbrance modification
                    ProposalManagementRejectMethods.deleteModification(line, diff);

                    EfinBudgetManencumlines encumLn = Utility
                        .getObject(EfinBudgetManencumlines.class, line.getId());
                    encumLn.setAPPAmt(encumLn.getAPPAmt().add(diff.negate()));
                    OBDal.getInstance().save(encumLn);
                    OBDal.getInstance().flush();

                  }
                }
              }
            }
          }

          if (appliedamtchk) {
            OBQuery<EscmProposalmgmtLine> proposalLineQry = OBDal.getInstance().createQuery(
                EscmProposalmgmtLine.class,
                " as e where e.escmBidmgmtLine.escmBidmgmt.id = :bidID and awardedamount > 0 and awardedqty > 0 "
                    + " and (status != 'CL' or status is null) and e.summary = false "
                    + " and e.efinFailureReason is not null ");
            proposalLineQry.setNamedParameter("bidID", proposal.getEscmBidmgmt().getId());
            if (proposalLineQry != null) {
              List<EscmProposalmgmtLine> proposalLineList = proposalLineQry.list();
              if (proposalLineList.size() > 0) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                return result;
              }
            }
          }
          if (!appliedamtchk) {
            for (String proposalMgmtId : proposalList) {
              EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
                  proposalMgmtId);
              proposalMgmt.setEfinIsbudgetcntlapp(false);
              OBDal.getInstance().save(proposalMgmt);
            }
          }
        }
      }
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while getUnifiedProposaltoBidDetailsRej: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in getUnifiedProposaltoBidDetailsRej:" + e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }
  }

  /**
   * update the proposal management header while reject
   * 
   * @param proposalmgmt
   * @return
   */
  // update proposal header status based on Reject
  public static boolean updateUnifiedProposalReject(EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      proposalmgmt.setUpdated(new java.util.Date());
      proposalmgmt.setUpdatedBy(OBContext.getOBContext().getUser());
      proposalmgmt.setProposalappstatus("REJ");
      proposalmgmt.setEscmUnifiedProposalAction("SA");

      proposalmgmt.setProposalstatus("PAWD");

      proposalmgmt.setEUTNextRole(null);

      return true;
    } catch (final Exception e) {
      log.error("Exception in updateUnifiedProposalReject after Reject : ", e);
      return false;
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  public static OBError checkSplitEncumProp(EscmProposalMgmt proposal, boolean appliedAmtChk) {

    Query query = null, query1 = null;
    boolean errorflag = false;
    String message = null;
    BigDecimal proposalAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO, unAppAmt = BigDecimal.ZERO;
    BigDecimal reqLnNetAmt = BigDecimal.ZERO;

    EfinBudgetManencumlines line = null;
    AccountingCombination com = null;

    String proposalMessage = OBMessageUtils.messageBD("Escm_proposalcommon").replace("%",
        proposal.getProposalno());
    try {
      OBContext.setAdminMode();
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = ProposalManagementActionMethod.getEncControleList(proposal);
      // End Task No.5925

      // pre validation before reject the Proposal
      if (enccontrollist.size() > 0) {
        if (proposal.isEfinIsbudgetcntlapp()) {

          // before doing validation update all the proposal line failure reason is null
          if (appliedAmtChk) {
            List<EscmProposalmgmtLine> failureReasonList = proposal.getEscmProposalmgmtLineList()
                .stream().filter(a -> a.getEfinFailureReason() != null)
                .collect(Collectors.toList());
            if (failureReasonList.size() > 0) {
              for (EscmProposalmgmtLine ordln : failureReasonList) {
                ordln.setEfinFailureReason(null);
                OBDal.getInstance().save(ordln);
              }
            }
          }

          String sqlQuery = " select prlln.em_efin_c_validcombination_id as procomid,sum(coalesce((prlln.awardedamount),0)) as proposalamt,"
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
          query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
          query.setParameter(0, proposal.getId());
          log.debug("strQuery:" + query.toString());

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

              // if uniquecode present in lines
              if (row[0] != null)
                com = OBDal.getInstance().get(AccountingCombination.class, row[0].toString());

              if (row[1] != null)
                proposalAmt = new BigDecimal(row[1].toString());

              if (row[3] != null) {
                line = ProposalManagementActionMethod.getEncumbranceLine(row[3].toString(),
                    row[0].toString());
                if (line != null)
                  unAppAmt = (line.getRevamount().subtract(line.getAPPAmt())
                      .subtract(line.getUsedAmount()));

              }
              if (row[2] != null)
                reqLnNetAmt = new BigDecimal(row[2].toString());

              if (row[3] != null && line != null)
                line = OBDal.getInstance().get(EfinBudgetManencumlines.class, line.getId());

              // calculate the difference from proposal to bid ( if diff value is negative then
              // decrease
              // or else increase)

              diff = proposalAmt.subtract(reqLnNetAmt);
              // if proposal is manual encumbrance then do the further validaion
              if (proposal.getEscmBidmgmt() != null
                  && proposal.getEscmBidmgmt().getEncumbrance() != null
                  && proposal.getEscmBidmgmt().getEncumbrance().getEncumMethod().equals("M")) {

                // if diff is zero then dont do anything
                if (diff.compareTo(BigDecimal.ZERO) == 0) {

                }
                // increase (if diff is +ve )
                else if (diff.compareTo(BigDecimal.ZERO) > 0) {

                  // in case of manual increase - then reduce
                  // the applied amt
                  if (!appliedAmtChk) {

                    line.setAPPAmt(line.getAPPAmt().subtract(diff));
                    OBDal.getInstance().save(line);
                    OBDal.getInstance().flush();
                  }
                }
                // decrease ( diff is -ve)
                else {
                  // in case of manual decrease - unapplied amount will be exist then increase the
                  // applied amt
                  if (appliedAmtChk) {
                    // check increase applied amount greater than un applied amount or not , if
                    // throw
                    // error
                    if (diff.negate().compareTo(unAppAmt) > 0) {
                      errorflag = true;
                      if (appliedAmtChk && errorflag) {
                        message = OBMessageUtils.messageBD("Efin_ReqAmt_More");
                      }
                      String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
                          + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? and (status != 'CL' or status is null) ";
                      query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                      query1.setParameter(0, message);
                      query1.setParameter(1, row[0].toString());
                      query1.setParameter(2, proposal.getId());
                      query1.executeUpdate();
                      OBDal.getInstance().flush();
                    }
                  }
                  // update the applied amount(increase)
                  else {

                    line.setAPPAmt(line.getAPPAmt().add(diff.negate()));
                    OBDal.getInstance().save(line);
                    OBDal.getInstance().flush();
                  }
                }
                // new uniquecode then dont do anything

              }
              // if proposal encumbrance is Auto
              else {
                // if new unique code added in proposal then
                if (row[2] == null) {
                  if (!appliedAmtChk) {
                    // Remove ENCUM LINE

                    // delete the encumbrance lines
                    if (proposal.getEscmBidmgmt().getEncumbrance() != null)
                      ProposalManagementRejectMethods.deleteEncumLine(proposal);
                    else if (proposal.getEfinEncumbrance() != null)
                      ProposalManagementRejectMethods
                          .deleteEncumLines(proposal.getEfinEncumbrance(), com, proposal, null);

                    proposal.setEfinEncumbrance(null);

                  } else if (appliedAmtChk) {

                    // check funds available in old unique code
                    errorflag = ProposalManagementRejectMethods
                        .chkFundsAvailforReactOldUniqueCode(proposal);

                  }
                }
                // increase
                else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                  if (!appliedAmtChk) {

                    // delete encumbrance modification
                    ProposalManagementRejectMethods.deleteModification(line, diff);

                    line.setAPPAmt(line.getAPPAmt().subtract(diff));
                    OBDal.getInstance().save(line);
                    OBDal.getInstance().flush();
                  }
                }
                // decrease
                else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                  if (appliedAmtChk) {
                    errorflag = ProposalManagementActionMethod.chkAutoEncumbranceValidation(
                        proposal, true, com.getId(), false, true, diff.negate());
                  } else {

                    // delete encumbrance modification
                    ProposalManagementRejectMethods.deleteModification(line, diff);

                    line.setAPPAmt(line.getAPPAmt().add(diff.negate()));
                    OBDal.getInstance().save(line);
                    OBDal.getInstance().flush();

                  }
                }
              }
            }
          }

          if (appliedAmtChk) {
            List<EscmProposalmgmtLine> proposalLineList = proposal.getEscmProposalmgmtLineList()
                .stream().filter(a -> a.getEfinFailureReason() != null)
                .collect(Collectors.toList());

            if (proposalLineList.size() > 0) {
              OBError result1 = OBErrorBuilder.buildMessage(null, "error", proposalMessage,
                  "@Efin_Chk_Line_Info@");
              return result1;
            }
          }
          if (!appliedAmtChk) {
            proposal.setEfinIsbudgetcntlapp(false);
            OBDal.getInstance().save(proposal);

          }
        }
      }
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
      return result;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while getUnifiedProposaltoBidDetailsRej: " + e);
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return result;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in getUnifiedProposaltoBidDetailsRej:" + e);
      final OBError error1 = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      return error1;
    }

  }

}
