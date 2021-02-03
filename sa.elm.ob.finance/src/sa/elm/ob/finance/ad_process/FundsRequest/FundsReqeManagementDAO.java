/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.FundsRequest;

import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EFINBudgetrevrules;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;

public class FundsReqeManagementDAO {

  private static final Logger log = Logger.getLogger(FundsReqeManagementDAO.class);

  public static JSONObject checkFundsAval(EFINFundsReqLine fundsLine) throws JSONException {
    JSONObject json = new JSONObject();
    BigDecimal bcuFnAvl = BigDecimal.ZERO;
    BigDecimal percent = BigDecimal.ZERO;
    boolean isWarn = false;
    boolean hasBudgetRevRules = true;
    try {
      OBContext.setAdminMode();
      json.put("is990Acct", "false");
      json.put("isWarn", "false");
      json.put("isFundGreater", "false");
      EFINFundsReq fundsReq = OBDal.getInstance().get(EFINFundsReq.class,
          fundsLine.getEfinFundsreq().getId());
      if (fundsLine.getFromaccount() != null) {
        AccountingCombination acctComb = OBDal.getInstance().get(AccountingCombination.class,
            fundsLine.getFromaccount().getId());
        if (acctComb != null) {
          OBQuery<EfinBudgetControlParam> budgCtrlParam = OBDal.getInstance().createQuery(
              EfinBudgetControlParam.class,
              "as e where e.budgetcontrolunit.id = :budgetcontrolunitID ");
          budgCtrlParam.setNamedParameter("budgetcontrolunitID", acctComb.getSalesRegion().getId());
          List<EfinBudgetControlParam> budgCtrlParamList = budgCtrlParam.list();
          if (budgCtrlParamList.size() > 0) {
            log.debug("is990Acct-yes");
            json.put("is990Acct", "true");
          }

          EFINFundsReq fundReq = OBDal.getInstance().get(EFINFundsReq.class,
              fundsLine.getEfinFundsreq().getId());

          OBQuery<EfinBudgetInquiry> bcuFundsAvl = OBDal.getInstance().createQuery(
              EfinBudgetInquiry.class, " as e where e.efinBudgetint.id = :efinBudgetintID and "
                  + " e.accountingCombination.id= :accountingCombinationID ");
          bcuFundsAvl.setNamedParameter("efinBudgetintID", fundReq.getEfinBudgetint().getId());
          bcuFundsAvl.setNamedParameter("accountingCombinationID",
              fundsLine.getFromaccount().getId());
          List<EfinBudgetInquiry> bcuFundsAvlList = bcuFundsAvl.list();
          if (bcuFundsAvlList.size() > 0) {
            EfinBudgetInquiry bcuFn = bcuFundsAvlList.get(0);
            bcuFnAvl = bcuFn.getBCUFundsAvailable();
          }
          OBQuery<EFINBudgetrevrules> revRul = OBDal.getInstance().createQuery(
              EFINBudgetrevrules.class,
              "as e where e.transactionType='BD' and e.enableBudgetRule ='Y' ");
          List<EFINBudgetrevrules> revRulList = revRul.list();
          if (revRulList.size() > 0) {
            EFINBudgetrevrules budDist = revRulList.get(0);
            percent = budDist.getPercentage();
            isWarn = budDist.isWarn();
          } else {
            hasBudgetRevRules = false;
          }
          log.debug("bcuFnAvl:" + bcuFnAvl + ", percent:" + percent + ", inc amt:"
              + fundsLine.getDecrease());
          if (!fundsReq.isReserve() && fundsLine.getDecrease().compareTo(bcuFnAvl) > 0) {
            json.put("isWarn", "false");
            json.put("isFundGreater", "true");
            log.debug("isWarn-false");
          } else if (fundsLine.getDecrease()
              .compareTo(bcuFnAvl.multiply((percent).divide(new BigDecimal("100")))) > 0) {
            json.put("isFundGreater", "true");
            if (isWarn) {
              json.put("isWarn", "true");
            } else {
              json.put("isWarn", "false");
            }
          }

          if (!hasBudgetRevRules) {
            json.put("isWarn", "false");
            json.put("isFundGreater", "false");
          }
          // }
        }
      }
    } catch (Exception e) {
      log.error("Exception in checkFundsAval : " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }
}