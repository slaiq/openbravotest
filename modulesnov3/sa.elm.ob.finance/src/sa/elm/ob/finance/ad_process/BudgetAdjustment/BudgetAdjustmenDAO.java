/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.BudgetAdjustment;

import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINBudgetrevrules;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;

public class BudgetAdjustmenDAO {

  private static final Logger log = Logger.getLogger(BudgetAdjustmenDAO.class);

  public static JSONObject checkFundsAval(BudgetAdjustmentLine budAdjLn) throws JSONException {
    JSONObject json = new JSONObject();
    BigDecimal bcuFnAvl = BigDecimal.ZERO;
    BigDecimal percent = BigDecimal.ZERO;
    boolean isWarn = false;
    try {
      OBContext.setAdminMode();
      json.put("is990Acct", "false");
      json.put("isWarn", "false");
      json.put("isFundGreater", "false");
      if (budAdjLn.getAccountingCombination() != null) {
        AccountingCombination acctComb = OBDal.getInstance().get(AccountingCombination.class,
            budAdjLn.getAccountingCombination().getId());
        if (acctComb != null) {
          OBQuery<EfinBudgetControlParam> budgCtrlParam = OBDal.getInstance().createQuery(
              EfinBudgetControlParam.class,
              "as e where e.budgetcontrolunit.id = :budgetcontrolunitID ");
          budgCtrlParam.setNamedParameter("budgetcontrolunitID", acctComb.getSalesRegion().getId());
          List<EfinBudgetControlParam> budgCtrlParamList = budgCtrlParam.list();
          if (budgCtrlParamList.size() > 0) {
            json.put("is990Acct", "true");

            BudgetAdjustment budAdj = OBDal.getInstance().get(BudgetAdjustment.class,
                budAdjLn.getEfinBudgetadj().getId());

            OBQuery<EfinBudgetInquiry> bcuFundsAvl = OBDal.getInstance().createQuery(
                EfinBudgetInquiry.class,
                "as e where e.efinBudgetint.id = :efinBudgetintID and e.accountingCombination.id= :accountingCombinationID ");
            bcuFundsAvl.setNamedParameter("efinBudgetintID", budAdj.getEfinBudgetint().getId());
            bcuFundsAvl.setNamedParameter("accountingCombinationID",
                budAdjLn.getAccountingCombination().getId());
            List<EfinBudgetInquiry> bcuFundsAvlList = bcuFundsAvl.list();
            if (bcuFundsAvlList.size() > 0) {
              EfinBudgetInquiry bcuFn = bcuFundsAvlList.get(0);
              bcuFnAvl = bcuFn.getCurrentBudget();
            }
            OBQuery<EFINBudgetrevrules> revRul = OBDal.getInstance().createQuery(
                EFINBudgetrevrules.class,
                "as e where e.transactionType='BA' and e.enableBudgetRule ='Y' ");
            revRul.setFilterOnActive(true);
            revRul.setFilterOnReadableClients(true);
            List<EFINBudgetrevrules> revRulList = revRul.list();
            if (revRulList.size() > 0) {
              EFINBudgetrevrules budAdjRul = revRulList.get(0);
              percent = budAdjRul.getPercentage();
              isWarn = budAdjRul.isWarn();
              if (budAdjLn.getDecrease().compareTo(bcuFnAvl) > 0) {
                json.put("isWarn", "false");
                json.put("isFundGreater", "true");
              } else if (budAdjLn.getDecrease()
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
          log.debug("bcuFnAvl:" + bcuFnAvl + ", percent:" + percent + ", inc amt:"
              + budAdjLn.getDecrease());

        }
      }

    } catch (Exception e) {
      log.error("Exception in checkFundsAval : " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

  /**
   * This method is used to check decrease Amount greater than budget amount
   * 
   * @param budAdjLn
   * @return
   * @throws JSONException
   */
  public static JSONObject checkDecreseAmountGreaterthanBudgetAmount(BudgetAdjustmentLine budAdjLn)
      throws JSONException {
    JSONObject json = new JSONObject();
    BigDecimal orgAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      json.put("isDecrease", "false");
      if (budAdjLn.getAccountingCombination() != null) {
        BudgetAdjustment budAdj = OBDal.getInstance().get(BudgetAdjustment.class,
            budAdjLn.getEfinBudgetadj().getId());
        OBQuery<EfinBudgetInquiry> bcuFundsAvl = OBDal.getInstance().createQuery(
            EfinBudgetInquiry.class,
            "as e where e.efinBudgetint.id = :efinBudgetintID and e.accountingCombination.id= :accountingCombinationID ");
        bcuFundsAvl.setNamedParameter("efinBudgetintID", budAdj.getEfinBudgetint().getId());
        bcuFundsAvl.setNamedParameter("accountingCombinationID",
            budAdjLn.getAccountingCombination().getId());
        List<EfinBudgetInquiry> bcuFundsAvlList = bcuFundsAvl.list();
        if (bcuFundsAvlList.size() > 0) {
          EfinBudgetInquiry bcuFn = bcuFundsAvlList.get(0);
          orgAmt = bcuFn.getORGAmt().add(bcuFn.getCarryForward())
              .add(bcuFn.getObincAmt().subtract(bcuFn.getObdecAmt()));
          if (orgAmt.compareTo(budAdjLn.getDecrease()) < 0) {
            json.put("isDecrease", "true");
          }
        }

      }

    } catch (Exception e) {
      log.error("Exception in checkDecreseAmountGreaterthanBudgetAmount : " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }
}