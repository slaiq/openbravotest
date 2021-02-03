/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.budgetcontrolparameters;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetControlParam;

/**
 * @author Divya.J
 */

public class BudgetControlParameterAction implements Process {
  /**
   * This process allow the user to Complete the Budget Control Parameters.After Complete cant do
   * any edition.
   */
  private static final Logger LOG = Logger.getLogger(BudgetControlParameterAction.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // declare the variable
    final String budgCntrlParamId = (String) bundle.getParams().get("Efin_Budget_Ctrl_Param_ID")
        .toString();

    final EfinBudgetControlParam budgCntrlParamObj = OBDal.getInstance()
        .get(EfinBudgetControlParam.class, budgCntrlParamId);
    try {
      // set the ready as true
      budgCntrlParamObj.setReady(true);
      OBDal.getInstance().save(budgCntrlParamObj);
      OBDal.getInstance().flush();
      final OBError result = OBErrorBuilder.buildMessage(null, "success", "@Success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      LOG.error("Exception in  BudgetControlParameterAction" + e, e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
