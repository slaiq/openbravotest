package sa.elm.ob.finance.ad_process;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINRdvBudgHoldLine;
import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRdvBudgTransfer;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.actionHandler.budgetholdplandetails.BudgetHoldPlanReleaseDAOImpl;
import sa.elm.ob.finance.ad_process.FundsRequest.FundsRequestActionDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess.RdvHoldActionDAO;
import sa.elm.ob.finance.ad_process.RDVProcess.RdvHoldProcess.RdvHoldActionDAOimpl;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.finance.util.TableIdConstant;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class BudgetRevisionReactivate extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(BudgetRevisionReactivate.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    Connection conn = OBDal.getInstance().getConnection();

    log.debug("entering into BudgetrevisionProcess");
    try {
      OBContext.setAdminMode();
      boolean isdistpresent = false, isfundserrorFlag = true;
      String fundsreqId = "";
      Boolean isValid = true;
      int count = 0;
      EfinBudgetManencum manualId = null;
      boolean secondLevelChild = true;
      boolean errorFlag = false;
      String BudRevId = (String) bundle.getParams().get("Efin_Budget_Transfertrx_ID");
      EfinBudgetTransfertrx efinBudgetRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          BudRevId);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = efinBudgetRev.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      isdistpresent = FundsRequestActionDAO.chkdistisdoneornot(null, efinBudgetRev, null);
      EfinBudgetManencum manualEncumbrance = null;
      long distributionCount = 0;
      RdvHoldActionDAO dao = null;
      EFINRdvBudgHoldLine holdLine = null;
      EfinRDVTransaction rdvVersion = null;
      int resultCount = 0;
      boolean isError = false;
      BigDecimal resultCount1 = BigDecimal.ONE;

      // if rdv version is completed then don't allow to reactivate the revision
      if (efinBudgetRev.isRdvhold()) {
        // for (EfinBudgetTransfertrxline line : efinBudgetRev.getEfinBudgetTransfertrxlineList()) {
        // List<EfinRdvBudgTransfer> transferList = line.getEfinRdvBudgtransferList();
        // if (transferList.size() > 0) {
        // for (EfinRdvBudgTransfer budgetTransfer : transferList) {
        // holdLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
        // budgetTransfer.getEfinRdvBudgholdline().getId());
        // if (holdLine.getEfinRdvBudghold().getSalesOrder() != null) {
        // OBQuery<EfinRDVTransaction> tran = OBDal.getInstance().createQuery(
        // EfinRDVTransaction.class,
        // " as e where e.efinRdv.id in (select id from Efin_RDV where salesOrder.id = :salesOrder)
        // order by e.creationDate desc");
        // tran.setNamedParameter("salesOrder",
        // holdLine.getEfinRdvBudghold().getSalesOrder().getId());
        // tran.setMaxResult(1);
        // if (tran != null && tran.list().size() > 0) {
        // rdvVersion = tran.list().get(0);
        // if (rdvVersion.getAppstatus().equals("APP")) {
        // OBDal.getInstance().rollbackAndClose();
        // OBError result = OBErrorBuilder.buildMessage(null, "error",
        // "@Efin_Revision_Reactive_Error@");
        // bundle.setResult(result);
        // return;
        // }
        // }
        // }
        // }
        // }
        // }
        OBQuery<EfinRDVTransaction> rdvTranscationQry = OBDal.getInstance().createQuery(
            EfinRDVTransaction.class,
            " as e where e.id in ( select txnln.efinRdvtxn.id from Efin_RDVTxnline txnln where txnln.id in "
                + " ( select holdact.efinRdvtxnline.id from  efin_rdv_hold_action holdact "
                + "where holdact.efinBudgetTransfertrxline is not null"
                + " and holdact.efinBudgetTransfertrxline.id in "
                + "( select revln.id from Efin_Budget_Transfertrxline revln where revln.efinBudgetTransfertrx.id=:revId ))) "
                + " and (e.txnverStatus <> 'DR' or (e.appstatus<>'DR' and e.appstatus<>'REJ')) ");
        rdvTranscationQry.setNamedParameter("revId", efinBudgetRev.getId());
        if (rdvTranscationQry.list().size() > 0) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Efin_Revision_Reactive_Error@");
          bundle.setResult(result);
          return;
        }
      }

      // before common validation check
      // We should reactivate only the funds request management created for this budget revision
      // which is having type ='Distribute' in lines
      // revert the changes in budget enquiry

      if (efinBudgetRev.isRdvhold() && efinBudgetRev.getManualEncumbrance() != null) {
        manualEncumbrance = efinBudgetRev.getManualEncumbrance();
        OBQuery<EfinBudgetManencum> chkLinePresent = OBDal.getInstance()
            .createQuery(EfinBudgetManencum.class, " as e where e.sourceref = '" + BudRevId + "'");

        if (chkLinePresent != null && chkLinePresent.list().size() > 0) {
          manualId = chkLinePresent.list().get(0);
          EfinBudgetManencum manual = manualId;
          manual.setDocumentStatus("DR");
          OBDal.getInstance().save(manual);
          for (EfinBudgetManencumlines reqln : manualId.getEfinBudgetManencumlinesList()) {
            if (reqln.getRevamount().compareTo(BigDecimal.ZERO) > 0) {
              List<EfinBudManencumRev> revlist = reqln.getEfinBudManencumRevList();
              for (EfinBudManencumRev revision : revlist) {
                // reqln.getEfinBudManencumRevList().remove(revision);
                OBDal.getInstance().remove(revision);
              }
              OBDal.getInstance().remove(reqln);
            }
          }
          OBDal.getInstance().remove(manualId);
          efinBudgetRev.setManualEncumbrance(null);
        }
        BudgetHoldPlanReleaseDAOImpl.updateBudgetInquiry(manualEncumbrance);
      }

      // Check all the validation before doing the changes
      // 1. Get distributed lines and check the validation in FRM
      // 2. Check budget revision common validations
      // 3. Get Release to HQ lines and check the validation in FRM

      List<Object> parameters = new ArrayList<Object>();

      for (EFINFundsReq fundreqmgmt : efinBudgetRev.getEFINFundsReqList()) {
        distributionCount = fundreqmgmt.getEFINFundsReqLineList().stream()
            .filter(a -> a.getREQType().equals("DIST")).count();
        if (distributionCount > 0) {
          parameters = new ArrayList<Object>();
          parameters.add(efinBudgetRev.getClient().getId());
          parameters.add(efinBudgetRev.getSalesCampaign().getId());
          parameters.add(efinBudgetRev.getSalesCampaign().getEfinBudgettype());
          parameters.add(
              efinBudgetRev.getEfinBudgetint() != null ? efinBudgetRev.getEfinBudgetint().getId()
                  : null);
          parameters.add(fundreqmgmt.getId());
          if ("C".equals(efinBudgetRev.getSalesCampaign().getEfinBudgettype())) {
            parameters.add(OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React")
                .replace("@", ""));
          } else {
            parameters.add(OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount")
                .replace("@", ""));

          }
          parameters.add("DIST");
          resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
              .call("efin_fundsreq_common_rea", parameters, null);

          if (resultCount1.intValue() == 0) {
            isError = true;
          }
        }
      }

      parameters = new ArrayList<Object>();
      parameters.add(efinBudgetRev.getClient().getId());
      parameters.add(efinBudgetRev.getSalesCampaign().getId());
      parameters.add(efinBudgetRev.getSalesCampaign().getEfinBudgettype());
      parameters
          .add(efinBudgetRev.getEfinBudgetint() != null ? efinBudgetRev.getEfinBudgetint().getId()
              : null);
      parameters.add(efinBudgetRev.getId());
      if ("C".equals(efinBudgetRev.getSalesCampaign().getEfinBudgettype())) {
        parameters.add(OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React")
            .replace("@", ""));
        parameters
            .add(OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds").replace("@", ""));
      } else {
        parameters.add(OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React")
            .replace("@", ""));
        parameters.add(
            OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount").replace("@", ""));
      }

      resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
          .call("efin_budgetrev_commonvalidrea", parameters, null);

      if (resultCount1.intValue() == 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_budgetRev_Failed"));
        bundle.setResult(result);
        return;
      }

      for (EFINFundsReq fundreqmgmt : efinBudgetRev.getEFINFundsReqList()) {
        distributionCount = fundreqmgmt.getEFINFundsReqLineList().stream()
            .filter(a -> !a.getREQType().equals("DIST")).count();
        if (distributionCount > 0) {
          parameters = new ArrayList<Object>();
          parameters.add(efinBudgetRev.getClient().getId());
          parameters.add(efinBudgetRev.getSalesCampaign().getId());
          parameters.add(efinBudgetRev.getSalesCampaign().getEfinBudgettype());
          parameters.add(
              efinBudgetRev.getEfinBudgetint() != null ? efinBudgetRev.getEfinBudgetint().getId()
                  : null);
          parameters.add(fundreqmgmt.getId());
          if ("C".equals(efinBudgetRev.getSalesCampaign().getEfinBudgettype())) {
            parameters.add(OBMessageUtils.messageBD("Efin_IncAmount_Greaterthan_CostAmount_React")
                .replace("@", ""));
          } else {
            parameters.add(OBMessageUtils.messageBD("Efin_DecAmount_Greaterthan_FundsAmount")
                .replace("@", ""));

          }
          parameters.add("REQ");
          resultCount1 = (BigDecimal) CallStoredProcedure.getInstance()
              .call("efin_fundsreq_common_rea", parameters, null);

          if (resultCount1.intValue() == 0) {
            isError = true;
          }
        }
      }

      if (isError) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("EFIN_FundsReq_Rev_Error"));
        bundle.setResult(result);
        return;
      }

      for (EFINFundsReq fundreqmgmt : efinBudgetRev.getEFINFundsReqList()) {
        distributionCount = fundreqmgmt.getEFINFundsReqLineList().stream()
            .filter(a -> a.getREQType().equals("DIST")).count();
        if (distributionCount > 0) {
          isfundserrorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(
              OBDal.getInstance().getConnection(), fundreqmgmt.getId(), false, true);
        }
      }
      if (!isfundserrorFlag) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_FundsReq_Rev_Error@");
        bundle.setResult(result);
        return;
      }

      if (!isdistpresent || isfundserrorFlag) {

        dao = new RdvHoldActionDAOimpl(OBDal.getInstance().getConnection());
        if (efinBudgetRev.isRdvhold()) {
          for (EfinBudgetTransfertrxline line : efinBudgetRev.getEfinBudgetTransfertrxlineList()) {
            List<EfinRdvHoldAction> holdTransferList = line.getEfinRdvHoldActionList();
            if (holdTransferList.size() > 0) {
              for (EfinRdvHoldAction holdRelActionObj : holdTransferList) {
                // if (holdRelActionObj.getRDVHoldRel() != null) {
                // EfinRdvHoldAction holdObj = holdRelActionObj.getRDVHoldRel();
                // holdObj.setReleasedAmount(
                // holdObj.getReleasedAmount().add(holdRelActionObj.getRDVHoldAmount()));
                // OBDal.getInstance().save(holdObj);
                // }
                EfinRdvBudgTransfer budgetTransfer = holdRelActionObj.getEfinRdvBudgtransfer();
                budgetTransfer.setReleaseamount(
                    budgetTransfer.getReleaseamount().add(holdRelActionObj.getRDVHoldAmount()));
                OBDal.getInstance().save(budgetTransfer);
                holdLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
                    budgetTransfer.getEfinRdvBudgholdline().getId());
                holdLine.setReleaseAmount(
                    holdLine.getReleaseAmount().add(holdRelActionObj.getRDVHoldAmount()));
                OBDal.getInstance().save(holdLine);
                dao.deleteHoldHed(holdRelActionObj);
              }
            }
            // update the release flag as 'N' in transferTable
            List<EfinRdvBudgTransfer> transferList = line.getEfinRdvBudgtransferList();
            if (transferList.size() > 0) {
              for (EfinRdvBudgTransfer budgetTransfer : transferList) {
                budgetTransfer.setReleased(false);
                OBDal.getInstance().save(budgetTransfer);
                holdLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
                    budgetTransfer.getEfinRdvBudgholdline().getId());
                holdLine.setBudgTransferamt(
                    holdLine.getBudgTransferamt().add(budgetTransfer.getAmount()));
                OBDal.getInstance().save(holdLine);
              }
            }
          }
        }
        isValid = CommonValidations.checkValidations(BudRevId, "BudgetRevision",
            OBContext.getOBContext().getCurrentClient().getId(), "RE", false);

        if (isValid) {// || efinBudgetRev.isRdvhold()

          count = reactivateBudgetInquiry(BudRevId);

          // After common validation check
          // We should reactivate only the funds request management created for this budget revision
          // which is having type ='Release to HQ' in lines
          // revert the changes in budget enquiry

          for (EFINFundsReq fundreqmgmt : efinBudgetRev.getEFINFundsReqList()) {
            distributionCount = fundreqmgmt.getEFINFundsReqLineList().stream()
                .filter(a -> !a.getREQType().equals("DIST")).count();
            if (distributionCount > 0) {
              isfundserrorFlag = FundsRequestActionDAO.reactivateBudgetInqchanges(
                  OBDal.getInstance().getConnection(), fundreqmgmt.getId(), false, true);
            }
          }
          if (!isfundserrorFlag) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@EFIN_FundsReq_Rev_Error@");
            bundle.setResult(result);
            return;
          }

          List<EfinBudgetTransfertrxline> lines = efinBudgetRev.getEfinBudgetTransfertrxlineList()
              .stream().filter(a -> a.getCurrentBudget().compareTo(BigDecimal.ZERO) <= 0)
              .collect(Collectors.toList());

          for (EfinBudgetTransfertrxline line : lines) {
            OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
                EfinBudgetInquiry.class,
                "accountingCombination.id='" + line.getAccountingCombination().getId()
                    + "' and efinBudgetint.id = '" + efinBudgetRev.getEfinBudgetint().getId()
                    + "' and salesCampaign.id = '" + efinBudgetRev.getSalesCampaign().getId()
                    + "' ");
            List<EfinBudgetInquiry> inquiryList = budgetinquiry.list();
            if (inquiryList != null && inquiryList.size() > 0) {

              EfinBudgetInquiry inquiry = inquiryList.get(0);

              // check child record have another child
              List<BaseOBObject> parentObjList, childObjList;
              HashMap<String, String> parentQryMap = new HashMap<String, String>();
              HashMap<String, String> childQryMap = new HashMap<String, String>();
              String parentWhereClause = "parent.id =:parentId";
              parentQryMap.put("parentId", inquiry.getId());
              // get child object (999) of parent (990)
              parentObjList = UtilityDAO.getQueryList(parentQryMap, parentWhereClause,
                  TableIdConstant.BUDGETENQUIRY_ID);
              for (BaseOBObject parentObj : parentObjList) {
                EfinBudgetInquiry parentInquiry = (EfinBudgetInquiry) parentObj;
                childQryMap.put("parentId", parentInquiry.getId());
                // get subchild object of child object
                childObjList = UtilityDAO.getQueryList(childQryMap, parentWhereClause,
                    TableIdConstant.BUDGETENQUIRY_ID);
                if (childObjList.size() > 0) {
                  secondLevelChild = false;
                }
              }
              if (secondLevelChild) {
                // remove child Node first(999)
                OBQuery<EfinBudgetInquiry> budgetEnqChildNode = OBDal.getInstance()
                    .createQuery(EfinBudgetInquiry.class, "parent.id =:parentId ");
                budgetEnqChildNode.setNamedParameter("parentId", inquiry.getId());
                List<EfinBudgetInquiry> inquiryChildList = budgetEnqChildNode.list();
                if (inquiryChildList.size() > 0) {
                  for (EfinBudgetInquiry childNode : inquiryChildList) {
                    if ((childNode.getCurrentBudget().add(childNode.getFundsAvailable())
                        .add(childNode.getREVAmount()).add(childNode.getSpentAmt()))
                            .compareTo(BigDecimal.ZERO) == 0) {

                      // should not allow to reactivate cost budget once funds budget created.
                      if (efinBudgetRev.getSalesCampaign().getEfinBudgettype().equals("C")) {
                        errorFlag = BudgetRevisionDAO
                            .checkFundsBudgetCreated(efinBudgetRev.getId());
                        if (errorFlag) {
                          OBDal.getInstance().rollbackAndClose();
                          OBError result = OBErrorBuilder.buildMessage(null, "error",
                              "@Efin_FundsCreated_NoReactivateCost@");
                          bundle.setResult(result);
                          return;
                        }
                      }

                      OBDal.getInstance().remove(childNode);
                      OBDal.getInstance().flush();
                    }
                  }
                }
                // remove parent (990)
                if ((inquiry.getCurrentBudget().add(inquiry.getFundsAvailable())
                    .add(inquiry.getREVAmount()).add(inquiry.getSpentAmt()))
                        .compareTo(BigDecimal.ZERO) == 0) {

                  // should not allow to reactivate cost budget once funds budget created.
                  if (efinBudgetRev.getSalesCampaign().getEfinBudgettype().equals("C")) {
                    errorFlag = BudgetRevisionDAO.checkFundsBudgetCreated(efinBudgetRev.getId());
                    if (errorFlag) {
                      OBDal.getInstance().rollbackAndClose();
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@Efin_FundsCreated_NoReactivateCost@");
                      bundle.setResult(result);
                      return;
                    }
                  }

                  OBQuery<BudgetAdjustmentLine> adjLine = OBDal.getInstance().createQuery(
                      BudgetAdjustmentLine.class,
                      "budgetInquiryLine.id = '" + inquiry.getId() + "'");
                  List<BudgetAdjustmentLine> adjLineList = adjLine.list();
                  if (adjLineList != null && adjLineList.size() > 0) {
                    OBDal.getInstance().rollbackAndClose();
                    throw new OBException((OBMessageUtils.messageBD("Efin_LinkedWithInquiry"))
                        .replace("@", line.getAccountingCombination().getEfinUniqueCode()));
                  }
                  OBDal.getInstance().remove(inquiry);
                }
              }
            }
            OBDal.getInstance().flush();

          }
          if (!efinBudgetRev.isRdvhold()) {
            OBQuery<EfinBudgetManencum> chkLinePresent = OBDal.getInstance().createQuery(
                EfinBudgetManencum.class, " as e where e.sourceref = '" + BudRevId + "'");

            if (chkLinePresent != null && chkLinePresent.list().size() > 0) {
              manualId = chkLinePresent.list().get(0);
              EfinBudgetManencum manual = manualId;
              manual.setDocumentStatus("DR");
              OBDal.getInstance().save(manual);
              for (EfinBudgetManencumlines reqln : manualId.getEfinBudgetManencumlinesList()) {
                List<EfinBudManencumRev> revlist = reqln.getEfinBudManencumRevList();
                for (EfinBudManencumRev revision : revlist) {
                  // reqln.getEfinBudManencumRevList().remove(revision);
                  OBDal.getInstance().remove(revision);
                }
                OBDal.getInstance().remove(reqln);
              }
              OBDal.getInstance().remove(manualId);
              efinBudgetRev.setManualEncumbrance(null);
            }
          }

          efinBudgetRev.setDocStatus("DR");
          efinBudgetRev.setAction("CO");
          // insert history

          if (efinBudgetRev != null) {

            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", efinBudgetRev.getId());
            historyData.put("Comments", "");
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.BUDGET_REVISION_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.BUDGET_REVISION_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.BUDGET_REVISION_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);

          }
          OBDal.getInstance().save(efinBudgetRev);
          OBDal.getInstance().flush();
          if (count > 0) {
            OBError result = OBErrorBuilder.buildMessage(null, "success",
                "@Efin_Budget_Rev_Reactivate@");
            bundle.setResult(result);
            return;
          }
        } else {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_budgetRev_Failed@");
          bundle.setResult(result);
          return;
        }
      }
    } catch (OBException exception) {
      OBDal.getInstance().rollbackAndClose();
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), exception.getMessage());
      bundle.setResult(error);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
      OBDal.getInstance().commitAndClose();
    }
  }

  /**
   * This method is used to reactivate budget inquiry
   * 
   * @param BudRevId
   * @return
   */
  public static int reactivateBudgetInquiry(String BudRevId) {

    int count = 0;
    String strquery, query = "";
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null, ps2 = null;
    ResultSet rs = null;

    try {
      OBContext.setAdminMode(true);
      strquery = "select revline.increase as increase, revline.decrease as decrease, inq.efin_budgetinquiry_id as inqId from efin_budget_transfertrxline revline\n"
          + "join efin_budget_transfertrx revHeader on revline.efin_budget_transfertrx_id = revHeader.efin_budget_transfertrx_id "
          + "join efin_budgetinquiry inq on revline.c_validcombination_id = inq.c_validcombination_id and revHeader.efin_budgetint_id = inq.efin_budgetint_id\n"
          + "where revHeader.efin_budget_transfertrx_id = ?";
      ps = conn.prepareStatement(strquery);
      ps.setString(1, BudRevId);
      rs = ps.executeQuery();
      while (rs.next()) {
        query = "  update efin_budgetinquiry set revdec_amt =revdec_amt- ?, revinc_amt =revinc_amt -? where efin_budgetinquiry_id=? ";
        ps2 = conn.prepareStatement(query);
        ps2.setBigDecimal(1, rs.getBigDecimal("decrease"));
        ps2.setBigDecimal(2, rs.getBigDecimal("increase"));
        ps2.setString(3, rs.getString("inqId"));
        ps2.executeUpdate();
      }
      count = 1;
    } catch (Exception e) {
      log.error("Exception in updatebudgetinquiry in Budget Revision: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          log.error("Exception in updatebudgetinquiry in Budget Revision: ", e);
        }
      }
      OBContext.restorePreviousMode();
    }
    return count;
  }
}
