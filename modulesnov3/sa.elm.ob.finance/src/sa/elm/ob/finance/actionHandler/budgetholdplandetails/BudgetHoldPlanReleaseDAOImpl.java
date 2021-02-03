package sa.elm.ob.finance.actionHandler.budgetholdplandetails;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINRdvBudgHold;
import sa.elm.ob.finance.EFINRdvBudgHoldLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvBudgTransfer;
import sa.elm.ob.finance.EfinRdvHold;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.EfinRdvHoldHeader;
import sa.elm.ob.finance.EfinRdvHoldTypes;
import sa.elm.ob.finance.actionHandler.RdvHoldRelease.HoldReleaseLineHandlerDAO;
import sa.elm.ob.finance.actionHandler.RdvHoldRelease.HoldReleaseLineHandlerDAOImpl;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess.RdvHoldActionDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess.RdvHoldActionDAOimpl;

/**
 * 
 * @author divya J on 28-11-2019
 */

public class BudgetHoldPlanReleaseDAOImpl implements BudgetHoldPlanReleaseDAO {
  private static final Logger log = LoggerFactory.getLogger(BudgetHoldPlanReleaseDAOImpl.class);
  private static final String Release = "RM";
  Integer roundoffConst = 2;

  @Override
  public JSONObject addHoldReleaseInRDV(Connection conn, EFINRdvBudgHold rdvBudgHold,
      JSONArray selectedLines, boolean ismanual) {
    JSONObject result = new JSONObject();
    AccountingCombination acctComb = null;
    try {
      if (selectedLines.length() > 0) {
        for (int line = 0; line < selectedLines.length(); line++) {
          JSONObject selectedRow = selectedLines.getJSONObject(line);
          String acctCombination = selectedRow.getString("accountingCombination");
          BigDecimal releaseAmt = new BigDecimal(selectedRow.getString("enteredAmount"));
          String holdTypeId = selectedRow.getString("efinRdvHoldTypes");
          String budgetHoldPlanLineId = selectedRow.getString("id");
          EFINRdvBudgHoldLine budgetholdLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
              budgetHoldPlanLineId);
          String actionType = Release;
          if (acctCombination != null)
            acctComb = OBDal.getInstance().get(AccountingCombination.class, acctCombination);
          EfinRDVTransaction rdvTxn = getOrInsertRDVTransaction(rdvBudgHold);
          int count = applyHoldAmtBasedOnWeightage(rdvBudgHold, rdvTxn, acctComb, releaseAmt,
              holdTypeId, budgetHoldPlanLineId, conn, actionType, null, ismanual);
          if (count == 0) {
            result.put("result", "0");
          } else {
            result.put("result", "1");
            // budgetholdLine.setReleaseAmount(budgetholdLine.getReleaseAmount().add(releaseAmt));
            OBDal.getInstance().save(budgetholdLine);
          }
        }
      }

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in addHoldReleaseInRDV : ", e);
      return result;
    } finally {
    }
    return result;
  }

  public EfinRDVTransaction getOrInsertRDVTransaction(EFINRdvBudgHold rdvBudgHold) {
    EfinRDVTransaction rdvTxn = null;
    EfinRDVTransaction latestrdvTxnObj = null;
    List<EfinRDVTransaction> trdvTxnList = new ArrayList<EfinRDVTransaction>();
    Long versionNo = (long) 1;
    SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      if (rdvBudgHold.getEfinRdvtxn() != null) {
        rdvTxn = rdvBudgHold.getEfinRdvtxn().getEfinRdvtxn();
        latestrdvTxnObj = getLatestRDVTransaction(rdvTxn);
        if (latestrdvTxnObj != null && latestrdvTxnObj.getTXNVersion() != null) {
          versionNo = latestrdvTxnObj.getTXNVersion() + 1;
        }
        if (rdvTxn.getTxnverStatus().equals("DR") && rdvTxn.getAppstatus().equals("DR")) {
          return rdvTxn;
        } else {
          OBQuery<EfinRDVTransaction> rdvTrxnQry = OBDal.getInstance().createQuery(
              EfinRDVTransaction.class,
              " as e where e.efinRdv.id=:rdvTxnId and e.txnverStatus='DR' "
                  + " and e.appstatus='DR'  and e.tXNVersion >:versionNo order by e.creationDate desc ");
          rdvTrxnQry.setNamedParameter("rdvTxnId", rdvTxn.getEfinRdv().getId());
          rdvTrxnQry.setNamedParameter("versionNo", rdvTxn.getTXNVersion());
          rdvTrxnQry.setMaxResult(1);
          trdvTxnList = rdvTrxnQry.list();
          if (trdvTxnList.size() > 0) {
            rdvTxn = trdvTxnList.get(0);
            return rdvTxn;
          } else {
            EfinRDVTransaction rdvTxnObj = OBProvider.getInstance().get(EfinRDVTransaction.class);
            rdvTxnObj.setClient(rdvTxnObj.getClient());
            rdvTxnObj.setOrganization(rdvTxn.getEfinRdv().getOrganization());
            rdvTxnObj.setCreatedBy(OBContext.getOBContext().getUser());
            rdvTxnObj.setUpdatedBy(OBContext.getOBContext().getUser());
            rdvTxnObj.setEfinRdv(rdvTxn.getEfinRdv());
            rdvTxnObj.setTXNVersion(versionNo);
            rdvTxnObj.setTxnverStatus("DR");
            rdvTxnObj.setCertificateDate(
                yearDateFormat.parse(yearDateFormat.format(new java.util.Date())));
            rdvTxnObj.setAdvancetransaction(false);
            rdvTxnObj.setRole(OBContext.getOBContext().getRole());
            rdvTxnObj
                .setTxnverDate(yearDateFormat.parse(yearDateFormat.format(new java.util.Date())));
            rdvTxnObj.setTxnverDateGreg(dateYearFormat.format(new java.util.Date()));
            OBDal.getInstance().save(rdvTxnObj);
            OBDal.getInstance().flush();
            return rdvTxnObj;
          }

        }
      }

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getOrInsertRDVTransaction : ", e);
      return rdvTxn;
    } finally {
    }
    return rdvTxn;
  }

  public EfinRDVTransaction getLatestRDVTransaction(EfinRDVTransaction rdvTxn) {
    EfinRDVTransaction latestrdvTxnObj = null;
    List<EfinRDVTransaction> trdvTxnList = new ArrayList<EfinRDVTransaction>();
    try {
      if (rdvTxn != null) {

        OBQuery<EfinRDVTransaction> rdvTrxnQry = OBDal.getInstance().createQuery(
            EfinRDVTransaction.class,
            " as e where e.efinRdv.id=:rdvTxnId  order by e.creationDate desc ");
        rdvTrxnQry.setNamedParameter("rdvTxnId", rdvTxn.getEfinRdv().getId());
        rdvTrxnQry.setMaxResult(1);
        trdvTxnList = rdvTrxnQry.list();
        if (trdvTxnList.size() > 0) {
          latestrdvTxnObj = trdvTxnList.get(0);
        }
      }

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getLatestRDVTransaction : ", e);
      return null;
    } finally {
    }
    return latestrdvTxnObj;
  }

  public int applyHoldAmtBasedOnWeightage(EFINRdvBudgHold rdvBudgHold,
      EfinRDVTransaction efinRDVTransaction, AccountingCombination acctCombination,
      BigDecimal releaseAmt, String holdTypeId, String budgetHoldPlanLineId, Connection conn,
      String actionType, EfinBudgetTransfertrxline revLine, Boolean ismanual) {
    List<EfinRDVTxnline> lineList = new ArrayList<EfinRDVTxnline>();
    List<EfinRdvHoldAction> holdActionList = new ArrayList<EfinRdvHoldAction>();
    EfinRDVTransaction rdvTxnObj = null;
    EfinRDVTxnline lineObj = null;
    BigDecimal totalHoldAmt = BigDecimal.ZERO;
    String refgroupId = SequenceIdData.getUUID();
    EfinBudgetTransfertrxline transferLine = null;
    try {
      transferLine = revLine;
      if (efinRDVTransaction.getClient().getCurrency() != null)
        roundoffConst = efinRDVTransaction.getClient().getCurrency().getStandardPrecision()
            .intValue();
      rdvTxnObj = efinRDVTransaction;

      EFINRdvBudgHoldLine budgHoldLineObj = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
          budgetHoldPlanLineId);

      OBQuery<EfinRdvHoldAction> rdvBudgholdLineActQry = OBDal.getInstance().createQuery(
          EfinRdvHoldAction.class, " as e where e.efinRdvBudgholdline.id=:budgHoldLineId ");
      rdvBudgholdLineActQry.setNamedParameter("budgHoldLineId", budgetHoldPlanLineId);
      holdActionList = rdvBudgholdLineActQry.list();
      if (holdActionList.size() > 0) {
        for (EfinRdvHoldAction holdActObj : holdActionList) {

          OBQuery<EfinRDVTxnline> rdvCurrentLineQry = OBDal.getInstance().createQuery(
              EfinRDVTxnline.class,
              " as e where e.efinRdvtxn.id=:RDVTxnId  and e.trxlnNo=:trxlnNo");
          rdvCurrentLineQry.setNamedParameter("RDVTxnId", rdvTxnObj.getId());
          rdvCurrentLineQry.setNamedParameter("trxlnNo",
              holdActObj.getEfinRdvtxnline().getTrxlnNo());
          lineList = rdvCurrentLineQry.list();
          if (lineList.size() > 0) {
            lineObj = lineList.get(0);
            BigDecimal weigtage = (holdActObj.getRDVHoldAmount()
                .divide(budgHoldLineObj.getHoldAmount(), 15, RoundingMode.HALF_UP))
                    .multiply(releaseAmt).setScale(roundoffConst, RoundingMode.HALF_UP);

            if (totalHoldAmt.add(weigtage).compareTo(releaseAmt) > 0) {
              weigtage = releaseAmt.subtract(totalHoldAmt);
            }
            totalHoldAmt = totalHoldAmt.add(weigtage);

            if (ismanual) {
              OBQuery<EfinRdvBudgTransfer> transferLineQry = OBDal.getInstance().createQuery(
                  EfinRdvBudgTransfer.class,
                  " as e where e.efinRdvBudgholdline.id=:holdLineObjId and (e.amount- e.releaseamount) >0  and e.released='Y' order by e.amount desc ");
              transferLineQry.setNamedParameter("holdLineObjId", budgHoldLineObj.getId());
              if (transferLineQry.list().size() > 0) {
                for (EfinRdvBudgTransfer transfer : transferLineQry.list()) {
                  BigDecimal transferAmount = transfer.getAmount()
                      .subtract(transfer.getReleaseamount());
                  transferLine = transfer.getEfinBudgetTransfertrxline();
                  if (transferAmount.compareTo(weigtage) >= 0) {
                    transferLine = transfer.getEfinBudgetTransfertrxline();
                    insertHoldAction(lineObj, weigtage, holdTypeId, budgetHoldPlanLineId, conn,
                        actionType, refgroupId, holdActObj, transferLine, ismanual, transfer);

                    // update release amount in efin_rdv_budgetTransfer
                    transfer.setReleaseamount(transfer.getReleaseamount().add(weigtage));
                    OBDal.getInstance().save(transfer);
                    weigtage = BigDecimal.ZERO;
                    break;
                  } else {
                    weigtage = weigtage.subtract(transferAmount); // 16-10 = 6
                    insertHoldAction(lineObj, transferAmount, holdTypeId, budgetHoldPlanLineId,
                        conn, actionType, refgroupId, holdActObj, transferLine, ismanual, transfer);

                    // update release amount in efin_rdv_budgetTransfer
                    transfer.setReleaseamount(transfer.getReleaseamount().add(transferAmount));
                    OBDal.getInstance().save(transfer);

                  }

                }
                if (weigtage.compareTo(BigDecimal.ZERO) > 0) {
                  insertHoldAction(lineObj, weigtage, holdTypeId, budgetHoldPlanLineId, conn,
                      actionType, refgroupId, holdActObj, null, ismanual, null);
                }
              } else {

                insertHoldAction(lineObj, weigtage, holdTypeId, budgetHoldPlanLineId, conn,
                    actionType, refgroupId, holdActObj, transferLine, ismanual, null);
              }
            } else {
              insertHoldAction(lineObj, weigtage, holdTypeId, budgetHoldPlanLineId, conn,
                  actionType, refgroupId, holdActObj, transferLine, ismanual, null);
            }
          }
        }
      }

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in applyHoldAmtBasedOnWeightage : ", e);
      return 0;
    }
    return 1;
  }

  public int insertHoldAction(EfinRDVTxnline rdvTxnLineObj, BigDecimal holdAmount,
      String holdTypeId, String budgetHoldPlanLineId, Connection conn, String actionType,
      String refgroupId, EfinRdvHoldAction holdActObj, EfinBudgetTransfertrxline revLine,
      Boolean ismanual, EfinRdvBudgTransfer rdvBudgTransfer) {

    Long lineNo = (long) 10;
    try {

      RdvHoldActionDAO rdvHoldActionDao = new RdvHoldActionDAOimpl(conn);
      if (rdvTxnLineObj.getEFINRdvLineActHistList().size() > 0)
        lineNo = (long) (rdvTxnLineObj.getEFINRdvLineActHistList().size() + 10);

      EFINRdvBudgHoldLine rdvBudgHoldLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
          budgetHoldPlanLineId);

      EfinRdvHoldAction action = OBProvider.getInstance().get(EfinRdvHoldAction.class);
      action.setTxngroupref(refgroupId);
      action.setClient(rdvTxnLineObj.getClient());
      action.setOrganization(rdvTxnLineObj.getOrganization());
      action.setSequenceNumber(lineNo);
      action.setTxnApplicationNo(rdvTxnLineObj.getEfinRdvtxn().getTXNVersion().toString());
      action.setAction(actionType);
      action.setActionDate(new java.util.Date());
      action.setAmount(rdvTxnLineObj.getMatchAmt());
      action.setEfinRdvHoldTypes(OBDal.getInstance().get(EfinRdvHoldTypes.class, holdTypeId));
      action.setRDVHoldPercentage(BigDecimal.ZERO);
      action.setRDVHoldAmount(holdAmount.negate());
      action.setActionReason(null);
      action.setActionJustification(null);
      action.setBusinessPartner(null);
      action.setName(null);
      action.setEfinRdvtxnline(rdvTxnLineObj);
      action.setFreezeRdvHold(false);
      action.setInvoice(null);
      action.setAmrasarfAmount(BigDecimal.ZERO);
      action.setTxn(true);
      action.setEfinBudgetTransfertrxline(revLine);
      action.setEfinRdvBudgtransfer(rdvBudgTransfer);
      action.setRDVHoldRel(holdActObj);
      OBDal.getInstance().save(action);
      holdActObj.setReleasedAmount(holdActObj.getReleasedAmount().add(holdAmount));
      OBDal.getInstance().flush();

      rdvHoldActionDao.insertHoldHeader(action, action.getEfinRdvtxnline(),
          action.getRDVHoldAmount(), null, null);

      if (ismanual && rdvBudgHoldLine != null) {
        // if (revLine != null) {
        // rdvBudgHoldLine
        // .setBudgTransferamt(rdvBudgHoldLine.getBudgTransferamt().subtract(holdAmount));
        // }
        rdvBudgHoldLine.setReleaseAmount(rdvBudgHoldLine.getReleaseAmount().add(holdAmount));
      }

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in insertPenaltyAction : ", e);
      return 0;
    }
    return 1;
  }

  @Override
  public JSONObject addBudgRevHoldReleaseInRDV(Connection conn, EfinBudgetTransfertrx transferTrx) {
    JSONObject result = new JSONObject();
    AccountingCombination acctComb = null;
    try {
      if (transferTrx != null) {
        for (EfinBudgetTransfertrxline lineObj : transferTrx.getEfinBudgetTransfertrxlineList()) {
          if (lineObj.getEfinRdvBudgtransferList().size() > 0) {
            for (EfinRdvBudgTransfer rdvBudgTransfer : lineObj.getEfinRdvBudgtransferList()) {
              EFINRdvBudgHoldLine budgetholdLine = rdvBudgTransfer.getEfinRdvBudgholdline();

              BigDecimal releaseAmt = rdvBudgTransfer.getAmount();
              /*
               * String acctCombination = budgetholdLine.getAccountingCombination().getId(); String
               * holdTypeId = budgetholdLine.getEfinRdvHoldTypes().getId(); String
               * budgetHoldPlanLineId = budgetholdLine.getId(); String actionType = Release; if
               * (acctCombination != null) acctComb = budgetholdLine.getAccountingCombination();
               * EfinRDVTransaction rdvTxn = getOrInsertRDVTransaction(
               * budgetholdLine.getEfinRdvBudghold()); int count =
               * applyHoldAmtBasedOnWeightage(budgetholdLine.getEfinRdvBudghold(), rdvTxn, acctComb,
               * releaseAmt, holdTypeId, budgetHoldPlanLineId, conn, actionType, lineObj, false); if
               * (count == 0) { OBDal.getInstance().rollbackAndClose(); result.put("result", "0"); }
               * else {
               */
              result.put("result", "1");
              rdvBudgTransfer.setReleased(true);
              OBDal.getInstance().save(rdvBudgTransfer);
              budgetholdLine
                  .setBudgTransferamt(budgetholdLine.getBudgTransferamt().subtract(releaseAmt));
              // budgetholdLine.setReleaseAmount(budgetholdLine.getReleaseAmount().add(releaseAmt));
              OBDal.getInstance().save(budgetholdLine);
              // }
            }
          }
        }
      }
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in addBudgRevHoldReleaseInRDV : ", e);
      return result;
    } finally {

    }
    return result;

  }

  public static AccountingCombination get999AccountCombination(AccountingCombination com,
      String orgId, String clientId) {
    String department = null;
    List<AccountingCombination> acctlist = new ArrayList<AccountingCombination>();
    AccountingCombination acctComb999 = null;
    try {

      EfinBudgetControlParam budgContrparam = FundsReqMangementDAO.getControlParam(clientId);
      department = budgContrparam.getBudgetcontrolCostcenter().getId();
      OBQuery<AccountingCombination> accountCommQry = OBDal.getInstance().createQuery(
          AccountingCombination.class,
          "account.id= '" + com.getAccount().getId() + "'" + " and businessPartner.id='"
              + com.getBusinessPartner().getId() + "' " + "and salesRegion.id='" + department
              + "' and project.id = '" + com.getProject().getId() + "' " + "and salesCampaign.id='"
              + com.getSalesCampaign().getId() + "' " + "and activity.id='"
              + com.getActivity().getId() + "' and stDimension.id='" + com.getStDimension().getId()
              + "' " + " and ndDimension.id = '" + com.getNdDimension().getId() + "' "
              + " and organization.id = '" + orgId + "'");

      log.debug("accountCommQry:" + accountCommQry.getWhereAndOrderBy());
      acctlist = accountCommQry.list();
      if (acctlist.size() > 0) {
        acctComb999 = acctlist.get(0);
      }
    }

    catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in get999AccountCombination : ", e);
      return acctComb999;
    } finally {

    }
    return acctComb999;
  }

  public static void updateBudgetInquiry(EfinBudgetManencum manEncumbarance) {

    OBQuery<EfinBudgetInquiry> budInq = null;
    List<EfinBudgetInquiry> inqList = new ArrayList<EfinBudgetInquiry>();
    List<AccountingCombination> act990List = new ArrayList<AccountingCombination>();
    String department = null;
    String organization = null;
    AccountingCombination acctCom990 = null;
    try {
      if (manEncumbarance != null) {
        for (EfinBudgetManencumlines lines : manEncumbarance.getEfinBudgetManencumlinesList()) {
          if (lines.getRevamount().compareTo(BigDecimal.ZERO) > 0) {
            // budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
            // "efinBudgetint.id=:budgInitId and accountingCombination.id=:acctCombId");
            // budInq.setNamedParameter("budgInitId",
            // manEncumbarance.getBudgetInitialization().getId());
            // budInq.setNamedParameter("acctCombId", lines.getAccountingCombination().getId());
            // inqList = budInq.list();
            // if (inqList.size() > 0) {
            // EfinBudgetInquiry budgetInqObj = inqList.get(0);
            // budgetInqObj.setEncumbrance(budgetInqObj.getEncumbrance().subtract(lines.getAmount()));
            // OBDal.getInstance().save(budgetInqObj);
            // }
            AccountingCombination acctCom999 = lines.getAccountingCombination();
            // 990 acct
            EfinBudgetControlParam budgContrparam = FundsReqMangementDAO
                .getControlParam(manEncumbarance.getClient().getId());
            department = budgContrparam.getBudgetcontrolunit().getId();
            organization = budgContrparam.getAgencyHqOrg().getId();
            OBQuery<AccountingCombination> accountCommQry = OBDal.getInstance().createQuery(
                AccountingCombination.class,
                "account.id= '" + acctCom999.getAccount().getId() + "'"
                    + " and businessPartner.id='" + acctCom999.getBusinessPartner().getId() + "' "
                    + "and salesRegion.id='" + department + "' and project.id = '"
                    + acctCom999.getProject().getId() + "' " + "and salesCampaign.id='"
                    + acctCom999.getSalesCampaign().getId() + "' " + "and activity.id='"
                    + acctCom999.getActivity().getId() + "' and stDimension.id='"
                    + acctCom999.getStDimension().getId() + "' " + " and ndDimension.id = '"
                    + acctCom999.getNdDimension().getId() + "' " + " and organization.id = '"
                    + organization + "'");
            act990List = accountCommQry.list();
            if (act990List.size() > 0) {
              acctCom990 = act990List.get(0);
              if (acctCom990 != null) {
                budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                    "efinBudgetint.id=:budgInitId and accountingCombination.id=:acctCombId");
                budInq.setNamedParameter("budgInitId",
                    manEncumbarance.getBudgetInitialization().getId());
                budInq.setNamedParameter("acctCombId", acctCom990.getId());
                inqList = budInq.list();
                if (inqList.size() > 0) {
                  EfinBudgetInquiry budgetInqObj = inqList.get(0);
                  budgetInqObj
                      .setEncumbrance(budgetInqObj.getEncumbrance().subtract(lines.getAmount()));
                  OBDal.getInstance().save(budgetInqObj);
                }
              }
            }
          }
        }
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log.error("Exception in updateBudgetInquiry " + e.getMessage());
    }
  }

  @Override
  public JSONObject releaseRevert(JSONArray selectedLines) {
    JSONObject result = new JSONObject();
    HoldReleaseLineHandlerDAO dao = new HoldReleaseLineHandlerDAOImpl();
    RdvHoldActionDAO rdvHoldActionDao = new RdvHoldActionDAOimpl(
        OBDal.getInstance().getConnection());
    try {
      if (selectedLines.length() > 0) {
        for (int i = 0; i < selectedLines.length(); i++) {
          JSONObject selectedRow = selectedLines.getJSONObject(i);
          BigDecimal releaseAmt = new BigDecimal(selectedRow.getString("enteredamt"));
          if (releaseAmt.compareTo(BigDecimal.ZERO) > 0) {
            String holdActionId = selectedRow.getString("id");
            EfinRdvHoldAction holdActionRelObj = OBDal.getInstance().get(EfinRdvHoldAction.class,
                holdActionId);
            EfinRdvHoldAction holdActionHoldObj = null;
            EFINRdvBudgHoldLine budgHoldLineObj = null;
            if (holdActionRelObj.getRDVHoldRel() != null) {
              holdActionHoldObj = holdActionRelObj.getRDVHoldRel();
              if (holdActionHoldObj.getEfinRdvBudgholdline() != null) {
                budgHoldLineObj = holdActionHoldObj.getEfinRdvBudgholdline();
              }
            }
            // update relase amount in hold release
            if (holdActionRelObj != null) {
              if ((holdActionRelObj.getRDVHoldAmount().add(releaseAmt))
                  .compareTo(BigDecimal.ZERO) == 0) {
                OBDal.getInstance().remove(holdActionRelObj);
              } else {
                holdActionRelObj
                    .setRDVHoldAmount(holdActionRelObj.getRDVHoldAmount().add(releaseAmt));
                OBDal.getInstance().save(holdActionRelObj);

                // update hold action release amt
                if (holdActionHoldObj != null) {
                  holdActionHoldObj.setReleasedAmount(
                      holdActionHoldObj.getReleasedAmount().add(releaseAmt.negate()));
                  OBDal.getInstance().save(holdActionHoldObj);
                }
              }
              OBDal.getInstance().flush();
              // update hold header
              EfinRdvHoldHeader holdHeader = dao
                  .getrdvHoldheader(holdActionRelObj.getEfinRdvtxnline());
              if (holdHeader != null) {
                if ((holdHeader.getEfinRdvtxnline().getEfinRdvHoldActionList().size() == 1
                    || holdHeader.getEfinRdvtxnline().getEfinRdvHoldActionList().size() == 0)
                    && (holdHeader.getRDVHoldAmount().add(releaseAmt))
                        .compareTo(BigDecimal.ZERO) == 0) {
                  OBDal.getInstance().remove(holdHeader);
                } else {
                  holdHeader.setRDVHoldAmount(holdHeader.getRDVHoldAmount().add(releaseAmt));
                  holdHeader
                      .setUpdatedRdvHoldAmt(holdHeader.getUpdatedRdvHoldAmt().add(releaseAmt));
                  OBDal.getInstance().save(holdHeader);
                }
              }
              // update hold
              OBQuery<EfinRdvHold> hold = OBDal.getInstance().createQuery(EfinRdvHold.class,
                  " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id='"
                      + holdActionRelObj.getEfinRdvtxnline().getEfinRdvtxn().getId()
                      + "') and e.rDVHoldType.id='"
                      + holdActionRelObj.getEfinRdvHoldTypes().getDeductionType().getId() + "'");
              hold.setMaxResult(1);
              if (hold.list().size() > 0) {
                EfinRdvHold upHold = hold.list().get(0);
                upHold.setRDVHoldApplied(upHold.getRDVHoldApplied().add(releaseAmt));
                OBDal.getInstance().save(upHold);
              }
            }

            // update budget Transfer release amt
            if (holdActionRelObj.getEfinRdvBudgtransfer() != null) {
              EfinRdvBudgTransfer budgTransfer = holdActionRelObj.getEfinRdvBudgtransfer();
              budgTransfer
                  .setReleaseamount(budgTransfer.getReleaseamount().add(releaseAmt.negate()));
              OBDal.getInstance().save(budgTransfer);
            }

            // update budgholdline obj releaseamt
            if (budgHoldLineObj != null) {
              budgHoldLineObj
                  .setReleaseAmount(budgHoldLineObj.getReleaseAmount().add(releaseAmt.negate()));
              OBDal.getInstance().save(budgHoldLineObj);
            }
          }
        }
        result.put("result", "1");
      }

    } catch (Exception e) {
      try {
        result.put("result", "0");
        return result;
      } catch (JSONException e1) {
        log.error("Exception in updateBudgetInquiry " + e.getMessage());
      }
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      log.error("Exception in updateBudgetInquiry " + e.getMessage());
    }
    return result;
  }

  @Override
  public boolean releaseRevertValidatio(JSONArray selectedLines) {
    Boolean isAlreadyAmtRevert = false;
    try {

      if (selectedLines.length() > 0) {
        for (int i = 0; i < selectedLines.length(); i++) {
          JSONObject selectedRow = selectedLines.getJSONObject(i);
          BigDecimal releaseAmt = new BigDecimal(selectedRow.getString("enteredamt"));
          if (releaseAmt.compareTo(BigDecimal.ZERO) > 0) {
            String holdActionId = selectedRow.getString("id");
            EfinRdvHoldAction holdActionRelObj = OBDal.getInstance().get(EfinRdvHoldAction.class,
                holdActionId);
            if (holdActionRelObj != null) {
              if ((holdActionRelObj.getRDVHoldAmount().negate()).compareTo(releaseAmt) < 0) {
                isAlreadyAmtRevert = true;
                break;
              }
              if (holdActionRelObj.getRDVHoldRel() != null
                  && holdActionRelObj.getRDVHoldRel().getEfinRdvBudgholdline() != null
                  && holdActionRelObj.getEfinRdvtxnline() != null) {
                if (releaseAmt
                    .compareTo(holdActionRelObj.getEfinRdvtxnline().getNetmatchAmt()) > 0) {
                  isAlreadyAmtRevert = true;
                  break;
                }
              }

            } else {
              isAlreadyAmtRevert = true;
              break;
            }

          }
        }
      }

    } catch (Exception e) {
      isAlreadyAmtRevert = true;
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in updateBudgetInquiry " + e.getMessage());
      return isAlreadyAmtRevert;
    }
    return isAlreadyAmtRevert;
  }
}
