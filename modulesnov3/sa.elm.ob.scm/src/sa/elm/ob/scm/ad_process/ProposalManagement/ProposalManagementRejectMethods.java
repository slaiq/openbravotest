package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Priyanka Ranjan on 22/06/2017
 */

// Reject Process methods of Proposal Management.
public class ProposalManagementRejectMethods {
  private static final Logger log = LoggerFactory.getLogger(ProposalManagementActionMethod.class);

  /**
   * update the proposal management header while reject
   * 
   * @param proposalmgmt
   * @return
   */
  // update proposal header status based on Reject
  public static boolean updateproposalmanagementheaderforReject(EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      proposalmgmt.setUpdated(new java.util.Date());
      proposalmgmt.setUpdatedBy(OBContext.getOBContext().getUser());

      proposalmgmt.setProposalappstatus("REJ");
      proposalmgmt.setEscmDocaction("SA");
      if (!proposalmgmt.getProposalstatus().equals("AWD")
          && !proposalmgmt.getProposalstatus().equals("PAWD")) {
        proposalmgmt.setProposalstatus("SHO");
      }
      if (!proposalmgmt.isNeedEvaluation()) {
        proposalmgmt.setProposalstatus("DR");
      }

      proposalmgmt.setEUTNextRole(null);

      return true;
    } catch (final Exception e) {
      log.error("Exception in updateproposalmanagementheader after Reject : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * if proposal is associate with manual encumbrance or auto encumbrance( with out bid) then check
   * used amount is greater or zero
   * 
   * @param proposalmgmt
   * @return if greater the used amount then return false
   */
  public static boolean chkManualEncumbranceRejValid(EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      EscmProposalMgmt baseProposal = proposalmgmt.getEscmBaseproposal();
      if (baseProposal == null) {
        OBQuery<EfinBudgetManencumlines> encline = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class,
            " as e  where e.manualEncumbrance.id=:encumId and e.id in "
                + " ( select b.efinBudgmanencumline.id from Escm_Proposalmgmt_Line b "
                + " where b.escmProposalmgmt.id=:proposalId)  and e.usedAmount > 0 ");
        encline.setNamedParameter("encumId", proposalmgmt.getEfinEncumbrance().getId());
        encline.setNamedParameter("proposalId", proposalmgmt.getId());

        if (encline.list().size() > 0) {
          return true;
        } else
          return false;
      } else if (baseProposal != null) {
        for (EscmProposalmgmtLine lines : proposalmgmt.getEscmProposalmgmtLineList()) {
          if (!lines.isSummary() && lines.getStatus() == null) {
            EfinBudgetManencumlines encline = lines.getEscmOldProposalline()
                .getEfinBudgmanencumline();
            // if reserved then consider new proposal line total
            BigDecimal amountDiffernce = lines.getEscmOldProposalline().getLineTotal()
                .subtract(lines.getLineTotal());
            if (amountDiffernce.compareTo(BigDecimal.ZERO) > 0
                && encline.getRemainingAmount().compareTo(amountDiffernce) < 0
                && encline.getManualEncumbrance().getEncumMethod().equals("M")) {
              return true;
            } else {
              return false;
            }
          }
        }
      }

    } catch (final Exception e) {
      log.error("Exception in chkManualEncumbranceValidation after Reject : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  public static boolean chkAutoEncumbranceValid(EscmProposalMgmt proposalmgmt) {
    boolean isAutoValid = false;
    try {
      OBContext.setAdminMode();
      EscmProposalMgmt baseProposal = proposalmgmt.getEscmBaseproposal();
      if (baseProposal != null) {
        for (EscmProposalmgmtLine lines : proposalmgmt.getEscmProposalmgmtLineList()) {
          if (!lines.isSummary()) {
            if (lines.getStatus() == null) {
              // if reserved then consider new proposal line total
              BigDecimal amountDiffernce = lines.getEscmOldProposalline().getLineTotal()
                  .subtract(lines.getLineTotal());
              if (amountDiffernce.compareTo(BigDecimal.ZERO) > 0) {
                JSONObject fundsCheckingObject = CommonValidationsDAO.CommonFundsChecking(
                    proposalmgmt.getEfinBudgetinitial(), lines.getEFINUniqueCode(),
                    amountDiffernce);
                if (fundsCheckingObject.has("errorFlag")) {
                  if ("0".equals(fundsCheckingObject.get("errorFlag"))) {
                    isAutoValid = true;
                    break;
                  }
                }
              }
            } else if (lines.getStatus() != null && lines.getEscmOldProposalline() != null
                && lines.getEscmOldProposalline().getStatus() == null) {
              // if reserved then consider new proposal line total
              BigDecimal amountDiffernce = lines.getEscmOldProposalline().getLineTotal();
              if (amountDiffernce.compareTo(BigDecimal.ZERO) > 0) {
                JSONObject fundsCheckingObject = CommonValidationsDAO.CommonFundsChecking(
                    proposalmgmt.getEfinBudgetinitial(), lines.getEFINUniqueCode(),
                    amountDiffernce);
                if (fundsCheckingObject.has("errorFlag")) {
                  if ("0".equals(fundsCheckingObject.get("errorFlag"))) {
                    isAutoValid = true;
                    break;
                  }
                }
              }

            }
          }
        }
      }

    } catch (

    final Exception e) {
      log.error("Exception in chkManualEncumbranceValidation after Reject : ", e);
      return isAutoValid;
    } finally {
      OBContext.restorePreviousMode();
    }
    return isAutoValid;
  }

  /**
   * update manual encumbrance applied amount if proposal is manual encumbrance(proposal without
   * bid)
   * 
   * @param proposalmgmt
   */
  public static void updateManualEncumAppAmt(EscmProposalMgmt proposalmgmt, Boolean iscancel) {

    try {
      EscmProposalMgmt baseProposalObj = proposalmgmt.getEscmBaseproposal();
      OBContext.setAdminMode();
      List<EscmProposalmgmtLine> prolineList = proposalmgmt.getEscmProposalmgmtLineList();
      // checking with propsal line
      if (baseProposalObj == null) {
        for (EscmProposalmgmtLine proposalline : prolineList) {
          if (!proposalline.isSummary()) {
            EfinBudgetManencumlines encline = proposalline.getEfinBudgmanencumline();
            if (encline != null) {
              if ("PAWD".equals(proposalmgmt.getProposalstatus())) {
                encline.setAPPAmt(encline.getAPPAmt().subtract(proposalline.getAwardedamount()));
              } else {
                encline.setAPPAmt(encline.getAPPAmt().subtract(proposalline.getLineTotal()));
              }
              OBDal.getInstance().save(encline);
            }
            if (iscancel) {
              // BidManagementDAO.insertEncumbranceModification(encline,
              // proposalline.getLineTotal().negate(), null, "PRO", null, null);
            } else {
              proposalline.setEfinBudgmanencumline(null);
              OBDal.getInstance().save(proposalline);
            }
          }
        }
      } else if (baseProposalObj != null) {
        for (EscmProposalmgmtLine lines : prolineList) {
          if (!lines.isSummary()) {
            if (lines.getStatus() == null) {
              EfinBudgetManencumlines encline = lines.getEscmOldProposalline()
                  .getEfinBudgmanencumline();
              // if reserved then consider new proposal line total
              BigDecimal amountDiffernce = lines.getEscmOldProposalline().getLineTotal()
                  .subtract(lines.getLineTotal());

              // update in remaining amount
              EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                  encline.getId());
              encumLn.setAPPAmt(encumLn.getAPPAmt().add(amountDiffernce));
              encumLn.setRemainingAmount(encumLn.getRemainingAmount().subtract(amountDiffernce));
              OBDal.getInstance().save(encumLn);
            } else if (lines.getStatus() != null && lines.getEscmOldProposalline() != null
                && lines.getEscmOldProposalline().getStatus() == null) {
              EfinBudgetManencumlines encline = lines.getEscmOldProposalline()
                  .getEfinBudgmanencumline();
              // if reserved then consider new proposal line total
              BigDecimal amountDiffernce = lines.getEscmOldProposalline().getLineTotal();

              // update in remaining amount
              EfinBudgetManencumlines encumLn = Utility.getObject(EfinBudgetManencumlines.class,
                  encline.getId());
              encumLn.setAPPAmt(encumLn.getAPPAmt().add(amountDiffernce));
              encumLn.setRemainingAmount(encumLn.getRemainingAmount().subtract(amountDiffernce));
              OBDal.getInstance().save(encumLn);
            }
          }
        }
      }

    } catch (final Exception e) {
      log.error("Exception in updateManualEncumAppAmt after Reject : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * update auto encumbrance value in budget enquiry ( with out bid in proposal)
   * 
   * @param proposalmgmt
   */
  public static void updateAutoEncumbrancechanges(EscmProposalMgmt proposalmgmt, boolean isCancel) {
    try {
      OBContext.setAdminMode();
      BigDecimal amt = BigDecimal.ZERO, amtTemp = BigDecimal.ZERO;
      List<EscmProposalmgmtLine> prolineList = proposalmgmt.getEscmProposalmgmtLineList();
      EscmProposalMgmt baseProposalObj = proposalmgmt.getEscmBaseproposal();
      List<EscmProposalmgmtLine> baseProlineList;
      BigDecimal diff = BigDecimal.ZERO;

      // checking with propsal line
      for (EscmProposalmgmtLine proposalline : prolineList) {
        if (!proposalline.isSummary()
            && (proposalline.getStatus() == null || !proposalline.getStatus().equals("CL"))) {
          EfinBudgetManencumlines encline = proposalline.getEfinBudgmanencumline();
          if (isCancel) {

            if (encline.getManualEncumbrance().getAppliedAmount().compareTo(BigDecimal.ZERO) == 1) {

              if (proposalmgmt.getEscmBaseproposal() == null) {

                amt = encline.getAPPAmt().subtract(proposalline.getLineTotal());
                encline.setAPPAmt(amt);
                amtTemp = amtTemp.add(proposalline.getLineTotal());
                if (encline.getManualEncumbrance().getAppliedAmount().subtract(amtTemp)
                    .compareTo(BigDecimal.ZERO) == 0 && baseProposalObj == null)
                  encline.getManualEncumbrance().setDocumentStatus("CA");
              }
              if (proposalmgmt.getEscmBaseproposal() == null)
                BidManagementDAO.insertEncumbranceModification(encline,
                    proposalline.getLineTotal().negate(), null, "PRO", null, null);
            } else {
              // This else is for the particular case (proposal->po, po cancel and proposal
              // cancel)
              encline.getManualEncumbrance().setDocumentStatus("CA");
              break;
            }

          } else {

            if (baseProposalObj == null) {
              if (encline != null) {
                encline.getManualEncumbrance().setDocumentStatus("DR");
                // remove associated proposal line refernce
                if (encline.getEscmProposalmgmtLineEMEfinBudgmanencumlineIDList().size() > 0) {
                  for (EscmProposalmgmtLine proLineList : encline
                      .getEscmProposalmgmtLineEMEfinBudgmanencumlineIDList()) {
                    proLineList.setEfinBudgmanencumline(null);
                    OBDal.getInstance().save(proLineList);
                  }
                }

                OBDal.getInstance().remove(encline);
              }
            }
          }
          /*
           * Trigger changes EfinEncumbarnceRevision.updateBudgetInquiry(encline,
           * encline.getManualEncumbrance(), proposalline.getLineTotal().negate());
           */
        }
      }

      // for cancel case if there exist a previous version, then update the encumbrance based on
      // the previous version values (Adding new records in the modification tab based on the new
      // and old proposal)
      if (isCancel) {
        if (baseProposalObj != null) {
          for (EscmProposalmgmtLine newproposalline : prolineList) {
            if (!newproposalline.isSummary()) {
              if (newproposalline.getStatus() == null) {
                EfinBudgetManencumlines encline = newproposalline.getEfinBudgmanencumline();
                diff = newproposalline.getLineTotal()
                    .subtract(newproposalline.getEscmOldProposalline() != null
                        ? newproposalline.getEscmOldProposalline().getLineTotal()
                        : BigDecimal.ZERO);
                if (diff.compareTo(BigDecimal.ZERO) != 0) {
                  ProposalManagementActionMethod.insertEncumbranceModification(encline,
                      diff.negate(), null, false);
                  encline.setAPPAmt(encline.getAPPAmt().add(diff.negate()));
                  OBDal.getInstance().save(encline);
                }
              } else if (newproposalline.getStatus() != null
                  && newproposalline.getEscmOldProposalline() != null
                  && newproposalline.getEscmOldProposalline().getStatus() == null) {
                EfinBudgetManencumlines encline = newproposalline.getEfinBudgmanencumline();
                diff = newproposalline.getEscmOldProposalline() != null
                    ? newproposalline.getEscmOldProposalline().getLineTotal()
                    : BigDecimal.ZERO;
                if (diff.compareTo(BigDecimal.ZERO) != 0) {
                  ProposalManagementActionMethod.insertEncumbranceModification(encline, diff, null,
                      false);
                  encline.setAPPAmt(encline.getAPPAmt().add(diff));
                  OBDal.getInstance().save(encline);
                }
              }
            }
          }
        }
      }
      OBDal.getInstance().flush();
    } catch (final Exception e) {
      log.error("Exception in updateManualEncumAppAmt after Reject : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * fetching bid detail from the proposal to do the validation as well as updation
   * 
   * @param proposal
   * @param appliedamtchk
   *          -- true ( then checking validation ) -- false ( doing updation)
   * @param isreject
   *          -- true( checking validation while reject) -- false ( checking validation while
   *          approve)
   * @return -- if any error then throw error falg as "true" else "false"
   */
  @SuppressWarnings("rawtypes")
  public static boolean getProposaltoBidDetailsRej(EscmProposalMgmt proposal, boolean appliedamtchk,
      boolean isreject, EscmProposalmgmtLine proposalmgmtline) {
    Query query = null, query1 = null;
    boolean errorflag = false;
    String message = null;
    BigDecimal bidAmt = BigDecimal.ZERO, proposalAmt = BigDecimal.ZERO, diff = BigDecimal.ZERO,
        unAppAmt = BigDecimal.ZERO;
    BigDecimal reqLnNetAmt = BigDecimal.ZERO;

    EfinBudgetManencumlines line = null;
    AccountingCombination com = null, encom = null;
    try {

      // before doing validation update all the proposal line failure reason is null
      if (appliedamtchk) {
        String UpdateQuery = "";
        if (proposal != null) {
          UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= null "
              + "  where escm_proposalmgmt_id= ? ";
        } else {
          UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= null "
              + "  where escm_proposalmgmt_line_id= ? ";
        }

        query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
        if (proposal != null)
          query1.setParameter(0, proposal.getId());
        else
          query1.setParameter(0, proposalmgmtline.getId());
        query1.executeUpdate();
      }

      // select query for fetching proposal to bid details
      // String sqlQuery = " select prlln.em_efin_c_validcombination_id as
      // procomid,coalesce((prlln.line_total),0) as proposalamt , "
      // + "bidln.escm_bidmgmt_id,bidln.em_efin_budgmanencumline_id, "
      // + "coalesce(manenc.revamount,0)- coalesce(manenc.app_amt,0)-
      // coalesce(manenc.used_amount,0), "
      // + "(select coalesce(sum(reqln.priceactual* srcref.quantity),0) as srcamt "
      // + "from escm_bidsourceref srcref "
      // + "join escm_bidmgmt_line ln on ln.escm_bidmgmt_line_id= srcref.escm_bidmgmt_line_id "
      // + "join m_requisitionline reqln on reqln.m_requisitionline_id= srcref.m_requisitionline_id
      // "
      // + "where ln.escm_bidmgmt_id=bidln.escm_bidmgmt_id and
      // ln.c_validcombination_id=prlln.em_efin_c_validcombination_id "
      // + "group by ln.c_validcombination_id ),bidln.c_validcombination_id ,manenc.isauto
      // ,coalesce(manenc.enc_decrease,0) "
      // + "as decrease , coalesce( manenc.enc_increase,0) as increase, sum(reqln.linenetamt) as
      // reqlinenetamt, bid.efin_budget_manencum_id "
      // + "from escm_proposalmgmt_line prlln "
      // + "join escm_bidmgmt_line bidln on bidln.escm_bidmgmt_line_id= prlln.escm_bidmgmt_line_id "
      // + "join escm_bidmgmt bid on bid.escm_bidmgmt_id=bidln.escm_bidmgmt_id "
      // + "left join efin_budget_manencumlines manenc on manenc.efin_budget_manencumlines_id=
      // bidln.em_efin_budgmanencumline_id "
      // + "left join escm_bidsourceref bidsrcref on
      // bidsrcref.escm_bidmgmt_line_id=bidln.escm_bidmgmt_line_id "
      // + "left join m_requisitionline reqln on
      // reqln.m_requisitionline_id=bidsrcref.m_requisitionline_id where ";
      // if (proposal != null)
      // sqlQuery += " prlln.escm_proposalmgmt_id = ? ";
      // else
      // sqlQuery += " prlln.escm_proposalmgmt_line_id = ? ";
      // sqlQuery += " and prlln.issummarylevel='N' and (prlln.status != 'CL' or prlln.status is
      // null) "
      // + "group by prlln.em_efin_c_validcombination_id,bidln.escm_bidmgmt_id, "
      // + "bidln.em_efin_budgmanencumline_id,manenc.revamount,manenc.app_amt,manenc.used_amount
      // ,bidln.c_validcombination_id,"
      // + "manenc.isauto, manenc.enc_decrease,manenc.enc_increase,prlln.line_total,
      // prlln.escm_proposalmgmt_line_id, bid.efin_budget_manencum_id ";
      String sqlQuery = " select  prlln.em_efin_c_validcombination_id as procomid,sum(coalesce((prlln.line_total),0)) as proposalamt,"
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
          + "  group by bidln.escm_bidmgmt_id,prlln.em_efin_c_validcombination_id , prlln.em_efin_c_validcombination_id "
          + " ,bid.efin_budget_manencum_id order by srcamt ";
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      if (proposal != null)
        query.setParameter(0, proposal.getId());
      // else
      // query.setParameter(0, proposalmgmtline.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          line = null;
          message = null;
          bidAmt = BigDecimal.ZERO;
          proposalAmt = BigDecimal.ZERO;
          reqLnNetAmt = BigDecimal.ZERO;
          diff = BigDecimal.ZERO;

          // if uniquecode present in lines
          if (row[0] != null)
            com = OBDal.getInstance().get(AccountingCombination.class, row[0].toString());
          // if (row[6] != null)
          // encom = OBDal.getInstance().get(AccountingCombination.class, row[6].toString());
          if (row[1] != null)
            proposalAmt = new BigDecimal(row[1].toString());
          // if (row[5] != null)
          // bidAmt = new BigDecimal(row[5].toString());
          if (row[3] != null) {
            line = ProposalManagementActionMethod.getEncumbranceLine(row[3].toString(),
                row[0].toString());
            if (line != null)
              unAppAmt = (line.getRevamount().subtract(line.getAPPAmt())
                  .subtract(line.getUsedAmount()));
            // unAppAmt = new BigDecimal(row[4].toString());
          }
          if (row[2] != null)
            reqLnNetAmt = new BigDecimal(row[2].toString());

          if (row[3] != null && line != null)
            line = OBDal.getInstance().get(EfinBudgetManencumlines.class, line.getId());

          // calculate the difference from proposal to bid ( if diff value is negative then decrease
          // or else increase)
          // diff = proposalAmt.subtract(bidAmt);
          diff = proposalAmt.subtract(reqLnNetAmt);
          // if proposal is manual encumbrance then do the further validaion
          if (proposal.getEfinEncumbrance() != null
              && proposal.getEfinEncumbrance().getEncumMethod().equals("M")) {

            // if diff is zero then dont do anything
            if (diff.compareTo(BigDecimal.ZERO) == 0) {

            }
            // increase (if diff is +ve )
            else if (diff.compareTo(BigDecimal.ZERO) > 0) {

              // in case of manual increase - then reduce
              // the applied amt
              if (!appliedamtchk) {
                String UpdateQuery = " update  efin_budget_manencumlines  set app_amt= app_amt-? "
                    + " where efin_budget_manencumlines_id = ? ";
                query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                query1.setParameter(0, diff);
                query1.setParameter(1, line.getId());
                query1.executeUpdate();
              }
            }
            // decrease ( diff is -ve)
            else {
              // in case of manual decrease - unapplied amount will be exist then increase the
              // applied amt
              if (appliedamtchk) {
                // check increase applied amount greater than un applied amount or not , if throw
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
                }
              }
              // update the applied amount(increase)
              else {
                String UpdateQuery = " update  efin_budget_manencumlines  set app_amt= app_amt+? "
                    + " where efin_budget_manencumlines_id = ? ";
                query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                query1.setParameter(0, diff.negate());
                query1.setParameter(1, line.getId());
                query1.executeUpdate();
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
                if (isreject) {
                  // delete the encumbrance lines
                  if (proposal.getEscmBidmgmt().getEncumbrance() != null)
                    deleteEncumLine(proposal);
                  else if (proposal.getEfinEncumbrance() != null)
                    deleteEncumLines(proposal.getEfinEncumbrance(), com, proposal, null);

                  proposal.setEfinEncumbrance(null);
                } else {
                  addedCancelModificationProBid(proposal.getEfinEncumbrance(), com, proposal);
                }
              } else if (appliedamtchk) {
                if (isreject) {
                  // check funds available in old unique code
                  errorflag = chkFundsAvailforReactOldUniqueCode(proposal);
                }
              }
            }
            // increase
            else if (diff.compareTo(BigDecimal.ZERO) > 0) {
              if (!appliedamtchk) {
                if (isreject) {
                  // delete encumbrance modification
                  deleteModification(line, diff);
                } else
                  BidManagementDAO.insertEncumbranceModification(line, diff.negate(), null, "PRO",
                      null, null);

                String UpdateQuery = " update  efin_budget_manencumlines  set  app_amt= app_amt- ? "
                    + " where efin_budget_manencumlines_id = ? ";
                query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                query1.setParameter(0, diff);
                query1.setParameter(1, line.getId());
                query1.executeUpdate();
              }
            }
            // decrease
            else if (diff.compareTo(BigDecimal.ZERO) < 0) {
              if (appliedamtchk) {
                errorflag = ProposalManagementActionMethod.chkAutoEncumbranceValidation(proposal,
                    true, com.getId(), false, true, diff.negate());
              } else {
                if (isreject) {
                  // delete encumbrance modification
                  deleteModification(line, diff);
                } else {
                  BidManagementDAO.insertEncumbranceModification(line, diff.negate(), null, "PRO",
                      null, null);
                }

                String UpdateQuery = " update  efin_budget_manencumlines  set  app_amt= app_amt+ ? "
                    + " where efin_budget_manencumlines_id = ? ";
                query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                query1.setParameter(0, diff.negate());
                /*
                 * Trigger changes query1.setParameter(1, diff.negate()); query1.setParameter(2,
                 * diff.negate());
                 */
                query1.setParameter(1, line.getId());
                query1.executeUpdate();
                // update budget inquiry Trigger changes
                /*
                 * EfinEncumbarnceRevision.updateBudgetInquiry(line, line.getManualEncumbrance(),
                 * diff.negate());
                 */
              }
            }
          }
        }
      }
      if ((proposal != null && proposal.getEfinEncumbrance() != null
          && proposal.getEfinEncumbrance().getEncumMethod().equals("A"))
          || (proposalmgmtline != null
              && proposalmgmtline.getEscmProposalmgmt().getEfinEncumbrance() != null
              && proposalmgmtline.getEscmProposalmgmt().getEfinEncumbrance().getEncumMethod()
                  .equals("A"))) {
        // update amount for unused combination in encumbrance line
        String strQry2 = "";
        strQry2 = "  select c_validcombination_id,efin_budget_manencumlines_id,efin_budget_manencum_id, system_decrease"
            + "  from efin_budget_manencumlines   where efin_budget_manencum_id= ?  and (isauto  is null or isauto='N')  "
            + "               and c_validcombination_id not in ( select em_efin_c_validcombination_id from escm_proposalmgmt_line where ";
        if (proposal != null)
          strQry2 += "   escm_proposalmgmt_id= ? ";
        else
          strQry2 += "   escm_proposalmgmt_line_id= ? ";
        strQry2 += "and issummarylevel='N' and (status != 'CL' or status is null)  and em_efin_c_validcombination_id is not null  )   ";
        query = OBDal.getInstance().getSession().createSQLQuery(strQry2);
        if (proposal != null) {
          query.setParameter(0, proposal.getEfinEncumbrance().getId());
          query.setParameter(1, proposal.getId());
        } else {
          query.setParameter(0,
              proposalmgmtline.getEscmProposalmgmt().getEfinEncumbrance().getId());
          query.setParameter(1, proposalmgmtline.getId());
        }

        List unUsedUniqCodeList = query.list();
        if (unUsedUniqCodeList != null && unUsedUniqCodeList.size() > 0) {
          for (Iterator iterator = unUsedUniqCodeList.iterator(); iterator.hasNext();) {
            Object[] row = (Object[]) iterator.next();
            line = OBDal.getInstance().get(EfinBudgetManencumlines.class, row[1].toString());

            if (appliedamtchk) {
              errorflag = ProposalManagementActionMethod.chkAutoEncumbranceValidation(proposal,
                  true, com.getId(), true, true, new BigDecimal(row[3].toString()));
            } else {
              if (isreject) {
                deleteModification(line, new BigDecimal(row[3].toString()).negate());
              } else {
                BidManagementDAO.insertEncumbranceModification(line,
                    new BigDecimal(row[3].toString()), null, "PRO", null, null);
              }
              // Trigger changes
              line.setAPPAmt(line.getAPPAmt().add(new BigDecimal(row[3].toString())));
              // update budget inquiry Trigger changes
              /*
               * EfinEncumbarnceRevision.updateBudgetInquiry(line, line.getManualEncumbrance(), new
               * BigDecimal(row[3].toString()));
               */
            }
          }
        }
        if (proposal != null && !appliedamtchk)
          proposal.setEfinEncumbrance(null);
      }
      if (appliedamtchk) {
        String selectQry = "";
        selectQry = " select   count(escm_proposalmgmt_line_id ) from escm_proposalmgmt_line where  em_efin_failure_reason is not null ";
        if (proposal != null)
          selectQry += "  and escm_proposalmgmt_id= ? ";
        else
          selectQry += "  and escm_proposalmgmt_line_id= ? ";
        query1 = OBDal.getInstance().getSession().createSQLQuery(selectQry);
        if (proposal != null)
          query1.setParameter(0, proposal.getId());
        else
          query1.setParameter(0, proposalmgmtline.getId());
        List countList = query1.list();
        if (query1 != null && countList.size() > 0) {
          BigInteger count = (BigInteger) countList.get(0);
          if (count.compareTo(BigInteger.ZERO) > 0) {
            errorflag = true;
          } else
            errorflag = false;
        }
      }
      log.debug("errorflag" + errorflag);
      return errorflag;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in getBidtoProposalDetails " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return true;
    }
  }

  /**
   * 
   * @param bidmgmt
   * @return
   * @throws ParseException
   */
  public static boolean chkFundsAvailforReactOldUniqueCode(EscmProposalMgmt proposal)
      throws ParseException {
    boolean errorFlag = false;
    String message = "";
    List<AccountingCombination> acctcomlist = new ArrayList<AccountingCombination>();
    OBQuery<EfinBudgetInquiry> budInq = null;
    Query query1 = null;
    JSONObject uniquecodeResult = new JSONObject(), json = null, UniqueCodejson = null,
        json2 = null;
    JSONArray uniqueCodeListArray = new JSONArray();
    Boolean sameUniqueCode = false;
    EfinBudgetManencum encumbrance = null;
    EscmBidMgmt bid = null;
    try {

      bid = proposal.getEscmBidmgmt();
      encumbrance = bid.getEncumbrance();

      message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");

      if (encumbrance != null) {
        OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
            .createQuery(EfinBudManencumRev.class, " as e where e.manualEncumbranceLines.id in "
                + " ( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id=:encumID)");
        revQuery.setNamedParameter("encumID", encumbrance.getId());
        if (revQuery.list().size() > 0) {
          for (EfinBudManencumRev rev : revQuery.list()) {
            EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();

            if (UniqueCodejson != null && UniqueCodejson.has("Uniquecode")) {
              for (int i = 0; i < uniqueCodeListArray.length(); i++) {
                json2 = uniqueCodeListArray.getJSONObject(i);
                if (json2.getString("Uniquecode")
                    .equals(lines.getAccountingCombination().getId())) {
                  json2.put("Amount",
                      new BigDecimal(json2.getString("Amount")).add(rev.getRevamount().negate()));
                  sameUniqueCode = true;
                  break;
                } else
                  continue;
              }
            }
            if (!sameUniqueCode) {
              UniqueCodejson = new JSONObject();
              UniqueCodejson.put("Uniquecode", lines.getAccountingCombination().getId());
              UniqueCodejson.put("Amount", rev.getRevamount().negate());
              uniqueCodeListArray.put(UniqueCodejson);
            }
          }
          uniquecodeResult.put("uniquecodeList", uniqueCodeListArray);

        }

        if (uniquecodeResult != null) {
          JSONArray array = uniquecodeResult.getJSONArray("uniquecodeList");
          for (int i = 0; i < array.length(); i++) {
            json = array.getJSONObject(i);
            AccountingCombination acctcom = OBDal.getInstance().get(AccountingCombination.class,
                json.getString("Uniquecode"));
            if (acctcom != null) {

              budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                  "efinBudgetint.id=:budInitId and accountingCombination.id=:acctId ");
              budInq.setNamedParameter("budInitId", encumbrance.getBudgetInitialization().getId());
              budInq.setNamedParameter("acctId", acctcom.getId());

              log.debug("budInq:" + budInq.list().size());
              // if isdepartment fund yes, then check dept level distribution acct.
              if (acctcom.isEFINDepartmentFund()) {
                if (budInq.list() != null && budInq.list().size() > 0) {
                  for (EfinBudgetInquiry Enquiry : budInq.list()) {
                    if (acctcom.getId().equals(Enquiry.getAccountingCombination().getId())) {
                      log.debug("getFundsAvailable:" + Enquiry.getFundsAvailable());
                      if (new BigDecimal(json.getString("Amount"))
                          .compareTo(Enquiry.getFundsAvailable()) > 0) {
                        // funds not available
                        errorFlag = true;
                        String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
                            + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? and (status != 'CL' or status is null) ";
                        query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                        query1.setParameter(0, message);
                        query1.setParameter(1, acctcom.getId());
                        query1.setParameter(2, proposal.getId());
                        query1.executeUpdate();
                      }
                    }
                  }
                } else {
                  errorFlag = true;
                  message = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost");
                  message = message.replace("@", "0");
                  String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
                      + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? and (status != 'CL' or status is null) ";
                  query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                  query1.setParameter(0, message);
                  query1.setParameter(1, acctcom.getId());
                  query1.setParameter(2, proposal.getId());
                  query1.executeUpdate();
                }
              }
              // if isdepartment fund No, then check Org level distribution acct.
              else {
                acctcomlist = CommonValidationsDAO.getParentAccountCom(acctcom,
                    acctcom.getClient().getId());

                if (acctcomlist != null && acctcomlist.size() > 0) {
                  AccountingCombination combination = acctcomlist.get(0);

                  budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                      "efinBudgetint.id=:budInitId and accountingCombination.id=:acctId");
                  budInq.setNamedParameter("budInitId",
                      encumbrance.getBudgetInitialization().getId());
                  budInq.setNamedParameter("acctId", combination.getId());
                  if (budInq.list() != null && budInq.list().size() > 0) {
                    for (EfinBudgetInquiry Enquiry : budInq.list()) {
                      if (combination.getId().equals(Enquiry.getAccountingCombination().getId())) {
                        log.debug("getFundsAvailable:" + Enquiry.getFundsAvailable());
                        if (new BigDecimal(json.getString("Amount"))
                            .compareTo(Enquiry.getFundsAvailable()) > 0) {
                          // funds not available
                          errorFlag = true;
                          String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
                              + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? and (status != 'CL' or status is null) ";
                          query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                          query1.setParameter(0, message);
                          query1.setParameter(1, acctcom.getId());
                          query1.setParameter(2, proposal.getId());
                          query1.executeUpdate();
                        }
                      }
                    }
                  }
                } else {
                  errorFlag = true;
                  String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= ? "
                      + " where em_efin_c_validcombination_id = ? and escm_proposalmgmt_id= ? and (status != 'CL' or status is null) ";
                  query1 = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
                  query1.setParameter(0, message);
                  query1.setParameter(1, acctcom.getId());
                  query1.setParameter(2, proposal.getId());
                  query1.executeUpdate();
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in chkFundsAvailforReactOldUniqueCode " + e.getMessage());
    }
    return errorFlag;
  }

  /**
   * Delete Encumbrance Lines
   * 
   * @param proposal
   * @return
   */
  public static boolean deleteEncumLine(EscmProposalMgmt proposal) {
    EfinBudgetManencum encumbrance = null;
    EscmBidMgmt bid = null;
    try {
      bid = proposal.getEscmBidmgmt();
      encumbrance = bid.getEncumbrance();
      // EfinBudgetManencumlines manenculine = encumbrance.getEfinBudgetManencumlinesList();
      // fetching revision record based on newly created encumbrance lines
      OBQuery<EscmProposalmgmtLine> propLnQry = OBDal.getInstance()
          .createQuery(EscmProposalmgmtLine.class, " as e where e.efinBudgmanencumline.id in "
              + "(select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id=:encumID ) ");
      propLnQry.setNamedParameter("encumID", encumbrance.getId());
      if (propLnQry.list().size() > 0) {
        for (EscmProposalmgmtLine propLn : propLnQry.list()) {
          if (propLn.getEfinBudgmanencumline() != null
              && propLn.getEscmBidmgmtLine().getEfinBudgmanencumline() != null
              && !propLn.getEfinBudgmanencumline()
                  .equals(propLn.getEscmBidmgmtLine().getEfinBudgmanencumline())) {
            EfinBudgetManencumlines propEncumLine = propLn.getEfinBudgmanencumline();
            EfinBudgetManencumlines bidEncumLine = propLn.getEscmBidmgmtLine()
                .getEfinBudgmanencumline();
            propLn.setEfinBudgmanencumline(propLn.getEscmBidmgmtLine().getEfinBudgmanencumline());
            propLn.setEFINUniqueCode(bidEncumLine.getAccountingCombination());
            propLn.setEFINUniqueCodeName(bidEncumLine.getUniqueCodeName());
            // propLn.setEFINFundsAvailable(bidEncumLine.getFundsAvailable());
            OBDal.getInstance().save(propLn);
            encumbrance.setDocumentStatus("DR");
            OBDal.getInstance().remove(propEncumLine);
            encumbrance.setDocumentStatus("CO");
          } else if (propLn.getEscmBidmgmtLine().getEfinBudgmanencumline() == null) {
            OBQuery<EfinBudgetManencumlines> encumLnQuery = OBDal.getInstance().createQuery(
                EfinBudgetManencumlines.class,
                " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:uniqueCodeId");
            encumLnQuery.setNamedParameter("encumID", encumbrance.getId());
            encumLnQuery.setNamedParameter("uniqueCodeId", propLn.getEFINUniqueCode());
            propLn.setEfinBudgmanencumline(null);
            if (encumLnQuery.list().size() > 0) {
              for (EfinBudgetManencumlines enucmLn : encumLnQuery.list()) {
                encumbrance.setDocumentStatus("DR");
                OBDal.getInstance().remove(enucmLn);
              }
              encumbrance.setDocumentStatus("CO");
            }
          }
        }
        OBDal.getInstance().flush();
      }
      OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance()
          .createQuery(EfinBudManencumRev.class, " as e where e.manualEncumbranceLines.id in"
              + " ( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id=:encumID)");
      revQuery.setNamedParameter("encumID", encumbrance.getId());

      if (revQuery.list().size() > 0) {
        for (EfinBudManencumRev rev : revQuery.list()) {
          rev.setSRCManencumline(null);
          OBDal.getInstance().save(rev);
          OBDal.getInstance().flush();
          EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
          lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
          lines.getEfinBudManencumRevList().remove(rev);
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in deleteEncumLine " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return false;
    }
    return true;
  }

  /**
   * delete the encumbrance lines if new unique code is added other than proposal
   * 
   * @param encum
   * @param com
   * @param proposal
   */
  public static void deleteEncumLines(EfinBudgetManencum encum, AccountingCombination com,
      EscmProposalMgmt proposal, EscmProposalmgmtLine proposalmgmtline) {
    EfinBudgetManencumlines line = null;
    List<EfinBudgetManencumlines> lineList = new ArrayList<EfinBudgetManencumlines>();
    List<EscmProposalmgmtLine> propsallnLs = null;
    try {

      OBQuery<EfinBudgetManencumlines> delLineQry = OBDal.getInstance()
          .createQuery(EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id=:encumID "
              + " and e.accountingCombination.id=:acctID and e.isauto='Y' ");
      delLineQry.setNamedParameter("encumID", encum.getId());
      delLineQry.setNamedParameter("acctID", com.getId());
      delLineQry.setMaxResult(1);
      lineList = delLineQry.list();
      if (lineList.size() > 0) {
        line = lineList.get(0);
        log.debug("line:" + line);
        if (proposal != null) {
          OBQuery<EscmProposalmgmtLine> propsalln = OBDal.getInstance().createQuery(
              EscmProposalmgmtLine.class,
              " as e where e.escmProposalmgmt.id=:proposalId and  e.efinBudgmanencumline.id=:encumLnId");
          propsalln.setNamedParameter("proposalId", proposal.getId());
          propsalln.setNamedParameter("encumLnId", line.getId());
          propsallnLs = propsalln.list();
        } else {
          OBQuery<EscmProposalmgmtLine> propsalln = OBDal.getInstance().createQuery(
              EscmProposalmgmtLine.class,
              " as e where e.id=:proposalLineId and e.efinBudgmanencumline.id=:encumLnId");
          propsalln.setNamedParameter("proposalLineId", proposalmgmtline.getId());
          propsalln.setNamedParameter("encumLnId", line.getId());
          propsallnLs = propsalln.list();
        }

        if (propsallnLs.size() > 0) {
          for (EscmProposalmgmtLine prosalline : propsallnLs) {
            prosalline.setEfinBudgmanencumline(null);
            OBDal.getInstance().save(prosalline);
          }
        }
        encum.getEfinBudgetManencumlinesList().remove(line);
        encum.setDocumentStatus("DR");
        OBDal.getInstance().remove(line);
      }
      encum.setDocumentStatus("CO");
      OBDal.getInstance().flush();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in deleteEncumLines " + e, e);
    }
  }

  public static void addedCancelModificationProBid(EfinBudgetManencum encum,
      AccountingCombination com, EscmProposalMgmt proposal) {
    EfinBudgetManencumlines line = null;
    List<EfinBudgetManencumlines> lineList = new ArrayList<EfinBudgetManencumlines>();
    try {

      OBQuery<EfinBudgetManencumlines> delLineQry = OBDal.getInstance()
          .createQuery(EfinBudgetManencumlines.class, " as e where e.manualEncumbrance.id=:encumID "
              + " and e.accountingCombination.id=:acctID and e.isauto='Y' ");
      delLineQry.setNamedParameter("encumID", encum.getId());
      delLineQry.setNamedParameter("acctID", com.getId());
      delLineQry.setMaxResult(1);
      lineList = delLineQry.list();
      if (lineList.size() > 0) {
        line = lineList.get(0);
        log.debug("line:" + line);
        BidManagementDAO.insertEncumbranceModification(line, line.getRevamount().negate(), null,
            "PRO", null, null);
        line.setAPPAmt(BigDecimal.ZERO);
        OBDal.getInstance().save(line);

        // OBQuery<EscmProposalmgmtLine> propsalln = OBDal.getInstance().createQuery(
        // EscmProposalmgmtLine.class, " as e where e.escmProposalmgmt.id='" + proposal.getId()
        // + "' " + " and e.efinBudgmanencumline.id='" + line.getId() + "'");
        //
        // if (propsalln.list().size() > 0) {
        // // for (EscmProposalmgmtLine prosalline : propsalln.list()) {
        // // prosalline.setEfinBudgmanencumline(null);
        // // OBDal.getInstance().save(prosalline);
        // // }
        // }
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in deleteEncumLines " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * 
   * @param line
   * @param amount
   */
  public static void deleteModification(EfinBudgetManencumlines line, BigDecimal amount) {
    EfinBudManencumRev revline = null;
    List<EfinBudManencumRev> revList = new ArrayList<EfinBudManencumRev>(); // and e.revamount='" +
                                                                            // amount + "'
    try {

      OBQuery<EfinBudManencumRev> revQry = OBDal.getInstance().createQuery(EfinBudManencumRev.class,
          " as e where e.manualEncumbranceLines.id=:encumLineID and e.revamount=:revAmt and e.isauto='Y'");
      revQry.setNamedParameter("encumLineID", line.getId());
      revQry.setNamedParameter("revAmt", amount);
      revQry.setMaxResult(1);

      revList = revQry.list();
      if (revList.size() > 0) {
        revline = revList.get(0);
        log.debug("revline:" + revline.getRevamount());
        line.getEfinBudManencumRevList().remove(revline);
        // OBDal.getInstance().remove(revline);
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in deleteEncumLines " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * reactivate the split PR from the proposal
   * 
   * @param resultEncum
   * @param proposal
   */
  public static void reactivateSplitPR(JSONObject resultEncum, EscmProposalMgmt proposal,
      Boolean isCancel, EscmProposalmgmtLine proposalmgmtline) {
    List<EfinBudgetManencumlines> manenculine = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencum encumbrance = null;
    EfinBudgetManencumlines srcEncumLines = null;
    List<EfinBudManencumRev> revList = null;
    try {
      if (proposal != null)
        encumbrance = proposal.getEfinEncumbrance();
      else
        encumbrance = proposalmgmtline.getEscmProposalmgmt().getEfinEncumbrance();

      // From PR
      if (proposal != null) {

        Map<EfinBudgetManencumlines, Double> proposalLineMap = proposal
            .getEscmProposalmgmtLineList().stream().filter(b -> b.getEfinBudgmanencumline() != null)
            .collect(Collectors.groupingBy(EscmProposalmgmtLine::getEfinBudgmanencumline,
                Collectors.summingDouble(a -> a.getLineTotal().doubleValue())));

        for (Map.Entry<EfinBudgetManencumlines, Double> entry : proposalLineMap.entrySet()) {

          BigDecimal requisitionLineAmt = BigDecimal.ZERO;

          for (EscmProposalmgmtLine proposalLine : proposal.getEscmProposalmgmtLineList()) {

            if (proposalLine.getEfinBudgmanencumline() != null
                && proposalLine.getEfinBudgmanencumline().getId().equals(entry.getKey().getId())) {
              for (EscmProposalsourceRef sourceRef : proposalLine.getEscmProposalsourceRefList()) {
                RequisitionLine reqline = sourceRef.getRequisitionLine();
                requisitionLineAmt = requisitionLineAmt
                    .add(sourceRef.getReservedQuantity().multiply(reqline.getUnitPrice()));
              }
            }
          }

          BigDecimal revAmtSum = BigDecimal.ZERO;

          EfinBudgetManencumlines newEncumlines = entry.getKey();

          OBQuery<EfinBudManencumRev> revAmtQuery = OBDal.getInstance().createQuery(
              EfinBudManencumRev.class, " as e where e.sRCManencumline.id = :proposalEncumLineID");
          revAmtQuery.setNamedParameter("proposalEncumLineID", newEncumlines.getId());

          if (revAmtQuery != null) {
            List<EfinBudManencumRev> revAmtList = revAmtQuery.list();
            if (revAmtList.size() > 0) {

              Map<EfinBudgetManencumlines, Double> encumLineRevAmtMap = revAmtList.stream()
                  .filter(b -> b.getManualEncumbranceLines() != null)
                  .collect(Collectors.groupingBy(EfinBudManencumRev::getManualEncumbranceLines,
                      Collectors.summingDouble(a -> (a.getRevamount().negate()).doubleValue())));

              for (Map.Entry<EfinBudgetManencumlines, Double> revision : encumLineRevAmtMap
                  .entrySet()) {

                EfinBudgetManencumlines oldEncumLines = revision.getKey();

                revAmtSum = BigDecimal.valueOf(revision.getValue());

                if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                  if (revAmtSum.compareTo(requisitionLineAmt) > 0) {
                    oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(requisitionLineAmt));
                    oldEncumLines.setRemainingAmount(oldEncumLines.getRemainingAmount()
                        .add(revAmtSum.subtract(requisitionLineAmt)));
                    OBDal.getInstance().save(oldEncumLines);

                  } else {
                    oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(revAmtSum));
                    OBDal.getInstance().save(oldEncumLines);
                  }
                }
                if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                  oldEncumLines.setAPPAmt(oldEncumLines.getAPPAmt().add(revAmtSum));
                  OBDal.getInstance().save(oldEncumLines);
                }
              }

            }
          }
        }
      }

      if (!isCancel) {
        // update encumbrance line is null
        if (proposal != null) {
          for (EscmProposalmgmtLine ln : proposal.getEscmProposalmgmtLineList()) {
            ln.setEfinBudgmanencumline(null);
            OBDal.getInstance().save(ln);
          }
          // update the bid management header status as "In Active"
          proposal.setEfinEncumbrance(null);
          proposal.setEfinIsbudgetcntlapp(false);
          encumbrance.setBusinessPartner(null);
          OBDal.getInstance().save(proposal);
        } else {
          proposalmgmtline.setEfinBudgmanencumline(null);
          OBDal.getInstance().save(proposalmgmtline);
          // update the bid management header status as "In Active"
          proposalmgmtline.getEscmProposalmgmt().setEfinEncumbrance(null);
          proposalmgmtline.getEscmProposalmgmt().setEfinIsbudgetcntlapp(false);
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
              else {
                line.setAPPAmt(line.getAPPAmt().add(line.getRevamount().negate()));
                BidManagementDAO.insertEncumbranceModification(line, line.getRevamount().negate(),
                    null, "PRO", null, null);
              }
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
            } else {
              BidManagementDAO.insertEncumbranceModification(lines, rev.getRevamount().negate(),
                  srcEncumLines, "PRO", null, null);
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in reactivateSplitPR " + e.getMessage());
    }
  }

  @SuppressWarnings("rawtypes")
  public static Boolean chkAndcancelforProposalPRFullQty(EscmProposalMgmt proposal,
      EfinBudgetManencum encumbrance, Boolean isChkFundsAppliedAmt, Boolean isreject) {
    Query query = null;
    @SuppressWarnings("unused")
    BigDecimal unappAmt = BigDecimal.ZERO;
    BigDecimal reqAmount = BigDecimal.ZERO, diff = BigDecimal.ZERO, appliedAmt = BigDecimal.ZERO,
        usedAmt = BigDecimal.ZERO, revAmt = BigDecimal.ZERO, proposalAmt = BigDecimal.ZERO;
    // List<EscmProposalmgmtLine> linelist = new ArrayList<EscmProposalmgmtLine>();
    Boolean errorFlag = false;
    List<EfinBudgetManencumlines> enclinelist = new ArrayList<EfinBudgetManencumlines>();
    List<EfinBudgetManencumlines> lineList = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencumlines line = null;
    String encumType = null;
    if (encumbrance != null)
      encumType = encumbrance.getEncumMethod();
    try {

      if (isChkFundsAppliedAmt) {
        String UpdateQuery = " update  escm_proposalmgmt_line  set em_efin_failure_reason= null "
            + "  where escm_proposalmgmt_id= ? ";
        query = OBDal.getInstance().getSession().createSQLQuery(UpdateQuery);
        query.setParameter(0, proposal.getId());
        query.executeUpdate();
      }

      String prosallineQry = "   select   sum(ln.line_total),ln.em_efin_c_validcombination_id  from escm_proposalmgmt_line ln  where ln.escm_proposalmgmt_id= ?   "
          + "            and ln.issummarylevel  ='N'  group by ln.em_efin_c_validcombination_id  ";
      query = OBDal.getInstance().getSession().createSQLQuery(prosallineQry);
      query.setParameter(0, proposal.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
              row[1].toString());

          if (row[0] != null)
            proposalAmt = new BigDecimal(row[0].toString());

          // OBQuery<EscmProposalmgmtLine> prosallnQry = OBDal.getInstance()
          // .createQuery(EscmProposalmgmtLine.class, " as e where e.escmProposalmgmt.id='"
          // + proposal.getId() + "' and e.eFINUniqueCode.id='" + row[1].toString() + "'");
          // linelist = prosallnQry.list();

          String reqlnQry = "   select case when coalesce(sum(reqln.qty),0) > 0   "
              + "          then  sum(round((coalesce(reqln.priceactual,0)*coalesce(ref.quantity,0)),2))   else 0 end  as amount ,reqln.em_efin_c_validcombination_id  from escm_proposalsource_ref ref "
              + "           join escm_proposalmgmt_line ln on ln.escm_proposalmgmt_line_id= ref.escm_proposalmgmt_line_id  and ln.issummarylevel  ='N' "
              + "           join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id "
              + "          and reqln.em_escm_issummary='N' " + " where ln .escm_proposalmgmt_id= ? "
              + "    and reqln.em_efin_c_validcombination_id=  ? group by reqln.em_efin_c_validcombination_id ";
          query = OBDal.getInstance().getSession().createSQLQuery(reqlnQry);
          query.setParameter(0, proposal.getId());
          query.setParameter(1, row[1].toString());
          log.debug("strQuery:" + query.toString());
          List reqqueryList = query.list();
          if (reqqueryList != null && reqqueryList.size() > 0) {
            for (Iterator reqiterator = reqqueryList.iterator(); reqiterator.hasNext();) {
              Object[] reqrow = (Object[]) reqiterator.next();
              if (reqrow[0] != null) {
                reqAmount = new BigDecimal(reqrow[0].toString());
              }
              diff = proposalAmt.subtract(reqAmount);

              OBQuery<EfinBudgetManencumlines> enclineQry = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e where e.manualEncumbrance.id='" + encumbrance.getId()
                      + "' and e.accountingCombination.id='" + row[1].toString() + "'");
              enclineQry.setMaxResult(1);
              enclinelist = enclineQry.list();
              if (enclinelist.size() > 0) {
                enclinelist = enclineQry.list();
                line = enclinelist.get(0);
                appliedAmt = line.getAPPAmt();
                usedAmt = line.getUsedAmount();
                revAmt = line.getRevamount();
                unappAmt = (revAmt.subtract(appliedAmt)).subtract(usedAmt);
              }
              if (diff.compareTo(BigDecimal.ZERO) == 0) {
                BidManagementDAO.insertEncumbranceModification(line, proposalAmt.negate(), null,
                    "PRO", null, null);
              }
              // increase
              if (diff.compareTo(BigDecimal.ZERO) > 0) {
                // update applied amt,rev amount in case of approve for both manual and auto (
                // reject case increase is decrease and decrease is increase)
                if (!isChkFundsAppliedAmt) {
                  line.setAPPAmt(line.getAPPAmt().subtract(proposalAmt.negate()));
                  BidManagementDAO.insertEncumbranceModification(line, proposalAmt.negate(), null,
                      "PRO", null, null);

                }
              }
              // decrease
              else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                // update applied amt and revamount in case of approve both manual and auto ( reject
                // case increase is decrease and decrease is increase)
                if (!isChkFundsAppliedAmt) {
                  line.setAPPAmt(line.getAPPAmt().subtract(proposalAmt.negate()));
                  BidManagementDAO.insertEncumbranceModification(line, proposalAmt.negate(), null,
                      "PRO", null, null);
                }
              }
            }
          } else {

            if (encumType.equals("A") && !isChkFundsAppliedAmt) {
              // delete the encumbrance lines
              OBQuery<EfinBudgetManencumlines> unusedLineQry = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  " as e where e.manualEncumbrance.id='" + encumbrance.getId()
                      + "' and e.accountingCombination.id='" + com.getId()
                      + "' and (e.isauto='N' or e.isauto is null) ");
              unusedLineQry.setMaxResult(1);
              lineList = unusedLineQry.list();
              if (lineList.size() > 0) {
                line = lineList.get(0);
                log.debug("line:" + line);
                line.setAPPAmt(line.getAPPAmt().subtract(proposalAmt.negate()));
                BidManagementDAO.insertEncumbranceModification(line, proposalAmt.negate(), null,
                    "PRO", null, null);
              }
            }
          }
        }
      }
      return errorFlag;
    } catch (Exception e) {
      log.error("Exception in getUniqueCodeListforFundschk " + e.getMessage());
    }
    return errorFlag;
  }

  /*
   * To check whether record is already revoked
   * 
   * @param proposalId,userId,roleId return isAlreadyRevoked
   * 
   */
  public static Boolean isAlreadyRevoked(String proposalId, String userId, String roleId) {
    Query query = null;
    @SuppressWarnings("rawtypes")
    List histList = null;
    Boolean isAlreadyRevoked = true;
    try {
      OBContext.setAdminMode();
      // Task No. 7304
      /*
       * String sqlString = " select pmgmt.escm_proposalmgmt_id from escm_proposalmgmt pmgmt " +
       * " left join (select ad_role_id, createdby, escm_proposalmgmt_id, requestreqaction from escm_proposalmgmt_hist where seqno="
       * + " (select max(seqno) from escm_proposalmgmt_hist " +
       * " where escm_proposalmgmt_id =:Escm_Proposalmgmt_ID and Requestreqaction not in ('RMIR','RMIREQ','RMI','RMIRES','F','FR'))"
       * +
       * " and escm_proposalmgmt_id =:Escm_Proposalmgmt_ID) hist on hist.escm_proposalmgmt_id=pmgmt.escm_proposalmgmt_id "
       * + " where  exists (select 1 from escm_proposalmgmt " +
       * " where escm_proposalmgmt_id =:Escm_Proposalmgmt_ID and createdby =:AD_User_ID and ad_role_id=:AD_Role_ID) "
       * +
       * " and pmgmt.escm_proposalmgmt_id =:Escm_Proposalmgmt_ID and requestreqaction='SUB' and requestreqaction!='REV' "
       * ;
       */
      String sqlString = " select requestreqaction, ad_role_id, createdby "
          + " from escm_proposalmgmt_hist where Escm_Proposalmgmt_ID = :Escm_Proposalmgmt_ID "
          + " order by created desc limit 1 ";
      query = OBDal.getInstance().getSession().createSQLQuery(sqlString);
      query.setParameter("Escm_Proposalmgmt_ID", proposalId);
      // query.setParameter("AD_User_ID", userId);
      // query.setParameter("AD_Role_ID", roleId);
      histList = query.list();
      if (histList.size() > 0) {
        Object[] row = (Object[]) histList.get(0);
        if ("SUB".equals(row[0].toString()) && roleId.equals(row[1].toString())
            && userId.equals(row[2].toString())) {
          isAlreadyRevoked = false;
        }
      }
    } catch (Exception e) {
      log.error("Exception in isAlreadyRevoked " + e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
    return isAlreadyRevoked;
  }

  /**
   * update the proposal management header while revert
   * 
   * @param proposalmgmt
   * @return
   */
  // update proposal header status based on revert
  public static boolean updateproposalmanagementheaderforRevert(EscmProposalMgmt proposalmgmt) {
    try {
      OBContext.setAdminMode();
      proposalmgmt.setUpdated(new java.util.Date());
      proposalmgmt.setUpdatedBy(OBContext.getOBContext().getUser());
      proposalmgmt.setProposalappstatus("INC");
      proposalmgmt.setEscmDocaction("SA");
      if (!proposalmgmt.getProposalstatus().equals("AWD")
          && !proposalmgmt.getProposalstatus().equals("PAWD")) {
        proposalmgmt.setProposalstatus("SHO");
      }

      if (!proposalmgmt.isNeedEvaluation()) {
        proposalmgmt.setProposalstatus("DR");
      }

      proposalmgmt.setEUTNextRole(null);
      return true;
    } catch (final Exception e) {
      log.error("Exception in updateproposalmanagementheaderforRevert: ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
