package sa.elm.ob.finance.util.budget;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

import sa.elm.ob.utility.util.Utility;

public class BudgetingUtilsServiceImpl implements BudgetingUtilsService {
  private static final Logger log = Logger.getLogger(BudgetingUtilsServiceImpl.class);
  private static final String ELEMENT_LEVEL = "C";

  @Override
  public Boolean isProjectAccount(String strElementValueId, String strClientId) {
    Boolean isProjectAccount = Boolean.FALSE;

    try {
      ElementValue element = Utility.getObject(ElementValue.class, strElementValueId);
      BudgetUtilsDAO dao = new BudgetUtilsDAOImpl();
      List<String> projectSummaryAccounts = new ArrayList<String>();

      if (element != null) {
        if (element.isSummaryLevel() && element.getElementLevel().equals(ELEMENT_LEVEL)
            && element.isEfinAllowBudgeting()) {

          projectSummaryAccounts = dao.getProjectSummaryAccounts(strClientId);

          if (projectSummaryAccounts.contains(strElementValueId))
            isProjectAccount = Boolean.TRUE;

          return isProjectAccount;
        } else {
          String strParentAccountID = dao.getParentAccount(strElementValueId, strClientId);
          if (StringUtils.isNotEmpty(strParentAccountID)) {
            isProjectAccount = isProjectAccount(strParentAccountID, strClientId);
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception while isProjectAccount() :" + e);
      e.printStackTrace();
    }
    return isProjectAccount;
  }

  @Override
  public Boolean isFundsOnlyAccount(String strElementValueID, String strClientId) {
    Boolean isFundsOnly = Boolean.FALSE;
    try {
      Boolean isProjectAccount = isProjectAccount(strElementValueID, strClientId);
      ElementValue account = Utility.getObject(ElementValue.class, strElementValueID);

      if (isProjectAccount && account.isEfinFundsonly()) {
        isFundsOnly = Boolean.TRUE;
      }
    } catch (Exception e) {
      log.error("Exception while isFundsOnlyAccount() :" + e);
      e.printStackTrace();
    }
    return isFundsOnly;
  }

}
