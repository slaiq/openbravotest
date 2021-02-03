/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EFINBudgetrevrules;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.properties.Resource;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.finance.util.TableIdConstant;
import sa.elm.ob.finance.util.budget.BudgetingUtilsService;
import sa.elm.ob.finance.util.budget.BudgetingUtilsServiceImpl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.UtilityDAO;

public class BudgetRevisionDAO {

  private static final Logger log = Logger.getLogger(BudgetRevisionDAO.class);

  /**
   * This method is used to check funds available
   * 
   * @param budRevLn
   * @return
   * @throws JSONException
   */
  public static JSONObject checkFundsAval(EfinBudgetTransfertrxline budRevLn) throws JSONException {
    JSONObject json = new JSONObject();
    BigDecimal bcuFnAvl = BigDecimal.ZERO;
    BigDecimal percent = BigDecimal.ZERO;
    boolean isWarn = false;
    try {
      OBContext.setAdminMode();
      json.put("is990Acct", "false");
      json.put("isWarn", "false");
      json.put("isFundGreater", "false");
      json.put("BCUFund", "0");
      if (budRevLn.getAccountingCombination() != null) {
        AccountingCombination acctComb = OBDal.getInstance().get(AccountingCombination.class,
            budRevLn.getAccountingCombination().getId());
        if (acctComb != null) {
          OBQuery<EfinBudgetControlParam> budgCtrlParam = OBDal.getInstance().createQuery(
              EfinBudgetControlParam.class,
              "as e where e.budgetcontrolunit.id ='" + acctComb.getSalesRegion().getId() + "'");

          if (budgCtrlParam.list().size() > 0) {
            json.put("is990Acct", "true");

            EfinBudgetTransfertrx budRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
                budRevLn.getEfinBudgetTransfertrx().getId());

            OBQuery<EfinBudgetInquiry> bcuFundsAvl = OBDal.getInstance().createQuery(
                EfinBudgetInquiry.class,
                "as e where e.efinBudgetint.id ='" + budRev.getEfinBudgetint().getId()
                    + "' and e.accountingCombination.id='"
                    + budRevLn.getAccountingCombination().getId() + "'");
            if (bcuFundsAvl.list().size() > 0) {
              EfinBudgetInquiry bcuFn = bcuFundsAvl.list().get(0);
              bcuFnAvl = bcuFn.getCurrentBudget();
              json.put("BCUFund", bcuFnAvl.toString());
            }
            OBQuery<EFINBudgetrevrules> revRul = OBDal.getInstance().createQuery(
                EFINBudgetrevrules.class,
                "as e where e.transactionType='BR' and e.enableBudgetRule=true");
            revRul.setFilterOnActive(true);
            if (revRul.list().size() > 0) {
              EFINBudgetrevrules budRevRul = revRul.list().get(0);
              if (budRevRul.isEnableBudgetRule()) {
                percent = budRevRul.getPercentage();
                isWarn = budRevRul.isWarn();
              }

              log.debug("bcuFnAvl:" + bcuFnAvl + ", percent:" + percent + ", inc amt:"
                  + budRevLn.getDecrease());
              if (budRevLn.getDecrease().compareTo(bcuFnAvl) > 0) {
                json.put("isWarn", "false");
                json.put("isFundGreater", "true");
              } else if (budRevLn.getDecrease()
                  .compareTo(bcuFnAvl.multiply((percent).divide(new BigDecimal("100")))) > 0) {
                json.put("isFundGreater", "true");
                if (isWarn) {
                  json.put("isWarn", "true");
                } else {
                  json.put("isWarn", "false");
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

  /**
   * This method is to check the following things
   * 
   * 1. Check cost budget is created when we increase any funds unique code 2. check cost current
   * budget is greater than or equal to funds increase amount
   * 
   * @param Budget
   *          revision header id
   * 
   * @return success or failure
   */

  public static Boolean checkManualUniqueCodeEntry(EfinBudgetTransfertrx transfertrx) {
    List<EfinBudgetTransfertrxline> lines = null;
    Boolean isSuccess = true;
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    if ("F".equals(transfertrx.getSalesCampaign().getEfinBudgettype())) {

      lines = transfertrx.getEfinBudgetTransfertrxlineList().stream()
          .filter(a -> a.getCurrentBudget().compareTo(BigDecimal.ZERO) <= 0)
          .collect(Collectors.toList());

      for (EfinBudgetTransfertrxline line : lines) {
        AccountingCombination costUniquecode = line.getAccountingCombination()
            .getEfinCostcombination();

        if (costUniquecode != null) {
          if (!budUtil.isFundsOnlyAccount(line.getAccountingCombination().getAccount().getId(),
              line.getClient().getId())) {
            OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance()
                .createQuery(EfinBudgetInquiry.class, "accountingCombination.id='"
                    + (costUniquecode != null ? costUniquecode.getId() : null)
                    + "' and efinBudgetint.id = '" + transfertrx.getEfinBudgetint().getId() + "'");
            List<EfinBudgetInquiry> budgetinquiryList = budgetinquiry.list();
            if (budgetinquiryList == null || budgetinquiryList.size() == 0) {
              isSuccess = false;
              line.setStatus(OBMessageUtils.messageBD("Efin_NocostBudgetdefined"));
              OBDal.getInstance().save(line);

            } else {
              EfinBudgetInquiry inquiry = budgetinquiryList.get(0);

              if (inquiry.getCurrentBudget().compareTo(line.getIncrease()) < 0) {
                isSuccess = false;
                line.setStatus(OBMessageUtils.messageBD("Efin_NocostBudgetdefined"));
                OBDal.getInstance().save(line);
              }
            }
          }
        }
        OBDal.getInstance().flush();
      }

    }

    return isSuccess;
  }

  /**
   * This method is used to check whether enough funds is available in 990 or 999
   * 
   * @param accountCombinationId
   * @param budgetIntId
   * @param clientId
   * @param amount
   * @return true if it has enough FA else false
   */

  public static Boolean checkFundsAvailableinBCU(String accountCombinationId, String budgetIntId,
      String clientId, BigDecimal amount, String budgetCostCenterId) {
    OBContext.setAdminMode();
    boolean isFundsAvail = false;

    List<BaseOBObject> parentObjList, childObjList;
    HashMap<String, String> parentQryMap = new HashMap<String, String>();
    HashMap<String, String> childQryMap = new HashMap<String, String>();

    String parentWhereClause = "accountingCombination.id=:accountCombinationId and efinBudgetint.id = :intId";
    String childWhereClause = "parent.id=:parentId and department.id =:deptId";

    parentQryMap.put("accountCombinationId", accountCombinationId);
    parentQryMap.put("intId", budgetIntId);

    parentObjList = UtilityDAO.getQueryList(parentQryMap, parentWhereClause,
        TableIdConstant.BUDGETENQUIRY_ID);

    for (BaseOBObject parentObj : parentObjList) {
      EfinBudgetInquiry parentInquiry = (EfinBudgetInquiry) parentObj;
      if (parentInquiry != null && (amount.compareTo(parentInquiry.getBCUFundsAvailable()) > 0)) {

        childQryMap.put("parentId", parentInquiry.getId());
        childQryMap.put("deptId", budgetCostCenterId);

        childObjList = UtilityDAO.getQueryList(childQryMap, childWhereClause,
            TableIdConstant.BUDGETENQUIRY_ID);

        for (BaseOBObject childObj : childObjList) {
          EfinBudgetInquiry childEnquiry = (EfinBudgetInquiry) childObj;
          if (childEnquiry != null) {
            if (amount.compareTo(
                childEnquiry.getFundsAvailable().add(parentInquiry.getBCUFundsAvailable())) < 0) {
              return true;
            } else {
              return false;
            }
          }
        }
      }
    }
    return isFundsAvail;
  }

  /**
   * This method is used to update the budget enquiry while deleting funds request management
   * 
   * @param fundsReqheader
   * @param reqline
   */

  public static void revertBudgetInquiry(EFINFundsReq fundsReqheader, EFINFundsReqLine reqline) {

    try {
      OBContext.setAdminMode();
      String whereClause = " as e where e.efinBudgetint.id= :intId "
          + " and   ( e.accountingCombination.id= :combinationId "
          + "        or e.accountingCombination.id=:toaccountId) ";

      HashMap<String, String> budgetEnquiryMap = new HashMap<String, String>();

      budgetEnquiryMap.put("intId", fundsReqheader.getEfinBudgetint().getId());
      budgetEnquiryMap.put("combinationId", reqline.getFromaccount().getId());
      budgetEnquiryMap.put("toaccountId", reqline.getToaccount().getId());

      List<BaseOBObject> budgetEnquiryList = UtilityDAO.getQueryList(budgetEnquiryMap, whereClause,
          TableIdConstant.BUDGETENQUIRY_ID);

      for (BaseOBObject budgetEnquiryObj : budgetEnquiryList) {
        EfinBudgetInquiry inq = (EfinBudgetInquiry) budgetEnquiryObj;
        if (reqline.getToaccount().getId().equals(inq.getAccountingCombination().getId())) {
          inq.setDisincAmt(inq.getDisincAmt().subtract(reqline.getDecrease()));
        } else {
          inq.setDisdecAmt(inq.getDisdecAmt().subtract(reqline.getDecrease()));
        }
        OBDal.getInstance().save(inq);
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("error while updating budget enquiry" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check funds budget revision is created for cost account
   * 
   * @param recordId
   * @return boolean true or false
   */
  public static boolean checkFundsBudgetCreated(String recordId) {
    BudgetingUtilsService budUtil = new BudgetingUtilsServiceImpl();
    List<EfinBudgetInquiry> budInqLs = new ArrayList<EfinBudgetInquiry>();
    try {
      EfinBudgetTransfertrx budRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class, recordId);
      for (EfinBudgetTransfertrxline budRevLn : budRev.getEfinBudgetTransfertrxlineList()) {
        String acctCombId = budRevLn.getAccountingCombination().getId();
        if (acctCombId != null) {
          AccountingCombination acctComb = OBDal.getInstance().get(AccountingCombination.class,
              acctCombId);
          /*
           * OBQuery<EFINBudgetTypeAcct> budgetTypeAcct = OBDal.getInstance().createQuery(
           * EFINBudgetTypeAcct.class,
           * "salesCampaign.efinBudgettype ='F' and accountElement.id =:accountId");
           * budgetTypeAcct.setNamedParameter("accountId", acctComb.getAccount().getId()); if
           * (budgetTypeAcct.list() != null && budgetTypeAcct.list().size() > 0) {
           */
          if (!budUtil.isFundsOnlyAccount(acctComb.getAccount().getId(),
              budRevLn.getClient().getId())) {
            if (budRevLn.getAccountingCombination().getEfinFundscombination() != null) {
              AccountingCombination fundsUniqcode = budRevLn.getAccountingCombination()
                  .getEfinFundscombination();
              if (fundsUniqcode != null) {
                OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
                    EfinBudgetInquiry.class,
                    "accountingCombination.id =:acctCombId and efinBudgetint.id =:budgetIntId ");
                budgetinquiry.setNamedParameter("acctCombId", fundsUniqcode.getId());
                budgetinquiry.setNamedParameter("budgetIntId", budRev.getEfinBudgetint().getId());
                budInqLs = budgetinquiry.list();
                if (budInqLs != null && budInqLs.size() > 0) {
                  return true;
                }
              }
            }
          }
          // }
        }
      }
      return false;
    } catch (Exception e) {
      log.error("Exception in checking funds budget created", e);
      return false;
    }
  }

  /**
   * insert alert for budget user when budget revision (created from Budget hold Plan) approved
   * 
   * @param vars
   * @param header
   */
  @SuppressWarnings("unchecked")
  public static void insertAlertforBudgetUser(VariablesSecureApp vars,
      EfinBudgetTransfertrx header) {
    String alertWindow = AlertWindow.BudRevHoldPlanApp;
    List<Object[]> holdAlertList = new ArrayList<Object[]>();
    String BUDGET_USERT_PREF = "EFIN_BUDGET_USER";
    String Alert_Status_New = "NEW";
    String description = null, message = null;
    String alertKey = "finance.budgrev.app.hold.alert";
    try {
      OBContext.setAdminMode();

      String hqlQry = " select  com.account. searchKey  ,sum(budgTran.amount), trxln.increase-sum(budgTran.amount)  from  Efin_Budget_Transfertrxline trxln\n"
          + " join trxln. efinRdvBudgtransferList as budgTran "
          + " join trxln. accountingCombination com"
          + " where trxln.increase >0    and trxln. efinBudgetTransfertrx.id= ? "
          + " group by trxln. accountingCombination,trxln.increase ,com.account.searchKey ";
      Query alertQry = OBDal.getInstance().getSession().createQuery(hqlQry.toString());
      alertQry.setParameter(0, header.getId());
      if (alertQry.list().size() > 0) {

        // to solve the alert
        OBQuery<Alert> alertObj = OBDal.getInstance().createQuery(Alert.class,
            " as e where e.referenceSearchKey =:referenceSearchKey and e.alertStatus = 'NEW' and e.eutAlertKey =:alertKey");
        alertObj.setNamedParameter("referenceSearchKey", header.getId());
        alertObj.setNamedParameter("alertKey", alertKey);
        List<Alert> alertList = alertObj.list();
        if (alertList.size() > 0) {
          alertList.forEach(a -> {
            Alert alert = OBDal.getInstance().get(Alert.class, a.getId());
            alert.setAlertStatus("SOLVED");
          });
        }

        holdAlertList = alertQry.list();
        for (Object[] o : holdAlertList) {
          if (o[0] != null && o[1] != null) {
            if (message == null) {
              message = o[0].toString() + " - " + Resource
                  .getProperty("finance.budgrevision.approved.holdalert.hold", vars.getLanguage())
                  + ":" + o[1].toString();
            } else {
              message = message + o[0].toString() + " - " + Resource
                  .getProperty("finance.budgrevision.approved.holdalert.hold", vars.getLanguage())
                  + ":" + o[1].toString();
            }
            if (o[2] != null) {
              BigDecimal budgRevAmt = new BigDecimal(o[2].toString());
              if (budgRevAmt.compareTo(BigDecimal.ZERO) > 0) {
                message = message + ","
                    + Resource.getProperty("finance.budgrevision.approved.holdalert.direct",
                        vars.getLanguage())
                    + ":" + budgRevAmt;
              }
            }
            if (message != null) {
              message = message + " ; ";
            }
          }
        }
        description = OBMessageUtils.messageBD("Efin_BudgRevApp_HoldAlert").replace("%", message);
        AlertUtility.alertInsertionPreferenceBudUser(header.getId(), header.getDocumentNo(),
            BUDGET_USERT_PREF, header.getClient().getId(), description, Alert_Status_New,
            alertWindow, alertKey, Constants.GENERIC_TEMPLATE, Constants.Budget_Revision_W, null);

      }

    }

    catch (Exception e) {
      log.error(" Exception while insertAlertforRDVHoldUser for rdv: " + e);
      throw new OBException(e.getMessage());
    } finally {
    }
  }

}