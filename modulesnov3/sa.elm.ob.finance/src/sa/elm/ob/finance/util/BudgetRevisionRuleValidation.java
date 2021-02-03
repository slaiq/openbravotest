package sa.elm.ob.finance.util;

import java.math.BigDecimal;
import java.util.List;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudgetrevrules;

public class BudgetRevisionRuleValidation {
  private static final Logger LOG = LoggerFactory.getLogger(BudgetRevisionRuleValidation.class);
  public static final String budget_Revision = "BR";
  public static final String budget_Adjust = "BA";
  public static final String budget_Dist = "BD";

  /**
   * it will validate increase or decrease amount as per budget revision rule
   * 
   * @param headerId
   * @param transactionType
   * @param clientId
   * @param budgetIntId
   * @return 0 --success,1--warn,2--error
   */
  public static int checkRuleValidation(String headerId, String transactionType, String clientId,
      String budgetIntId) {
    String str_operator = null;
    Boolean isWarn = false;
    BigDecimal percentage = BigDecimal.ZERO;
    int failed_record_count;
    try {
      OBContext.setAdminMode();
      BudgetRevisionRuleValidationDAO dao = new BudgetRevisionRuleValidationDAO();
      OBQuery<EFINBudgetrevrules> objRevQuery = OBDal.getInstance().createQuery(
          EFINBudgetrevrules.class,
          "as e where e.transactionType=:type and e.enableBudgetRule='Y'");
      if (transactionType.equals(budget_Revision)) {

        objRevQuery.setNamedParameter("type", budget_Revision);
        objRevQuery.setMaxResult(1);
        List<EFINBudgetrevrules> objRevList = objRevQuery.list();
        if (objRevQuery != null && objRevList.size() > 0) {
          EFINBudgetrevrules objRev = objRevList.get(0);
          str_operator = objRev.getOperators();
          percentage = objRev.getPercentage();
          isWarn = objRev.isWarn();
        }
        failed_record_count = dao.checkRevisionRules(budget_Revision, headerId, clientId,
            percentage, str_operator, budgetIntId, isWarn);
        if (failed_record_count > 0) {
          if (isWarn) {
            return 1;
          } else {
            return 2;
          }
        }

      } else if (transactionType.equals(budget_Adjust)) {

        objRevQuery.setNamedParameter("type", budget_Adjust);
        objRevQuery.setMaxResult(1);
        List<EFINBudgetrevrules> objRevList = objRevQuery.list();
        if (objRevQuery != null && objRevList.size() > 0) {
          EFINBudgetrevrules objRev = objRevList.get(0);
          str_operator = objRev.getOperators();
          percentage = objRev.getPercentage();
          isWarn = objRev.isWarn();
        }
        failed_record_count = dao.checkRevisionRules(budget_Adjust, headerId, clientId, percentage,
            str_operator, budgetIntId, isWarn);
        if (failed_record_count > 0) {
          if (isWarn) {
            return 1;
          } else {
            return 2;
          }
        }
      } else if (transactionType.equals(budget_Dist)) {
        objRevQuery.setNamedParameter("type", budget_Dist);
        objRevQuery.setMaxResult(1);
        List<EFINBudgetrevrules> objRevList = objRevQuery.list();
        if (objRevQuery != null && objRevList.size() > 0) {
          EFINBudgetrevrules objRev = objRevList.get(0);
          str_operator = objRev.getOperators();
          percentage = objRev.getPercentage();
          isWarn = objRev.isWarn();
        }
        failed_record_count = dao.checkRevisionRules(budget_Dist, headerId, clientId, percentage,
            str_operator, budgetIntId, isWarn);
        if (failed_record_count > 0) {
          if (isWarn) {
            return 1;
          } else {
            return 2;
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  checkValidations " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return 0;
  }
}
