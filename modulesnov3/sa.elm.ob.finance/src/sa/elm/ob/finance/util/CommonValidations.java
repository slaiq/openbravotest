package sa.elm.ob.finance.util;

import java.math.BigDecimal;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.util.DAO.BudgetValidationsDAO;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;

/**
 * @author Priyanka Ranjan on 30/09/2017
 */
// Common Validations
public class CommonValidations {

  private static final Logger LOG = LoggerFactory.getLogger(CommonValidations.class);
  public static final String budget_revision = "BudgetRevision";
  public static final String budget_Adjust = "BudgetAdjustment";
  public static final String budget_Dist = "BudgetDistribution";
  public static final String Encum = "Encumbrance";

  /**
   * 
   * @param headerId
   * @param transactionType
   * @param clientId
   * @param actionType
   * @return true or false
   */
  public static boolean checkValidations(String headerId, String transactionType, String clientId,
      String actionType, boolean isWarn) {
    try {
      OBContext.setAdminMode();

      if (transactionType.equals(budget_revision)) {

        if (CommonValidationsDAO.checkCommonVal("BR", headerId, clientId, actionType, isWarn) > 0) {
          return false;
        }

      } else if (transactionType.equals(budget_Adjust)) {
        if (CommonValidationsDAO.checkCommonVal("BADJ", headerId, clientId, actionType,
            isWarn) > 0) {
          return false;
        }
      } else if (transactionType.equals(budget_Dist)) {

        if (CommonValidationsDAO.checkCommonVal("BD", headerId, clientId, actionType, isWarn) > 0) {
          return false;
        }
      } else if (transactionType.equals(Encum)) {

        if (CommonValidationsDAO.checkCommonValEncum("ENC", headerId, clientId, actionType) > 0) {
          return false;
        }
      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  checkValidations " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  /**
   * 
   * @param headerId
   * @param clientId
   * @return count
   */
  public static int checkBudgetValidations(String headerId, String clientId) {
    int count = 0;
    try {
      OBContext.setAdminMode();
      count = BudgetValidationsDAO.BudgetValidations(headerId, clientId);

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in  checkBudgetValidations " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  public static JSONObject getFundsAvailable(EfinBudgetIntialization budgetIntialization,
      AccountingCombination accountingCombination) {
    return CommonValidationsDAO.CommonFundsChecking(budgetIntialization, accountingCombination,
        BigDecimal.ZERO);
  }

  public static String getEncumbranceLineId(String strManualEncumbranceId,
      String strCombinationId) {
    return CommonValidationsDAO.getEncumbranceLineId(strManualEncumbranceId, strCombinationId);
  }

  public static int checkUserRoleForSubmit(String tableName, String userId, String roleId,
      String recordId, String keyColumn) {
    return CommonValidationsDAO.checkUserRoleForSubmit(tableName, userId, roleId, recordId,
        keyColumn);
  }
}
