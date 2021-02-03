package sa.elm.ob.finance.ad_process;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetIntialization;

/**
 * 
 * @author sathish kumar on 2809/2017
 *
 */

public class BudgetInitializationClose extends DalBaseProcess {

  /**
   * This process is used to change status of the record to closed
   * 
   */
  private static final Logger LOG = LoggerFactory.getLogger(BudgetInitializationClose.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    OBError result = null;

    try {
      OBContext.setAdminMode();
      String budIntId = (String) bundle.getParams().get("Efin_Budgetint_ID");
      EfinBudgetIntialization efinBudgetInt = OBDal.getInstance().get(EfinBudgetIntialization.class,
          budIntId);

      if (efinBudgetInt != null) {
        efinBudgetInt.setStatus("CL");
        OBDal.getInstance().save(efinBudgetInt);
      }
      OBDal.getInstance().flush();

      result = OBErrorBuilder.buildMessage(null, "Success", "@EFIN_Process_Success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      LOG.error(" Exception while closing budget Intialization " + e);
      result = OBErrorBuilder.buildMessage(null, "error", e.toString());
      bundle.setResult(result);
      return;
    }

  }
}
