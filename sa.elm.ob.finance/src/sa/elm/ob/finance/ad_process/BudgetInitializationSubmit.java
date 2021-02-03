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
 * @author sathish kumar on 26/09/2017
 *
 */

public class BudgetInitializationSubmit extends DalBaseProcess {

  /**
   * BudgetInitializationSubmit to change status of the record
   * 
   */
  private static final Logger LOG = LoggerFactory.getLogger(BudgetInitializationSubmit.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    OBError result = null;

    try {
      OBContext.setAdminMode();
      String budIntId = (String) bundle.getParams().get("Efin_Budgetint_ID");
      EfinBudgetIntialization efinBudgetInt = OBDal.getInstance().get(EfinBudgetIntialization.class,
          budIntId);

      if (efinBudgetInt != null) {
        if (efinBudgetInt.getStatus().equals("DR")) {
          efinBudgetInt.setStatus("OP");
          OBDal.getInstance().save(efinBudgetInt);
        } else if (efinBudgetInt.getStatus().equals("CL")) {
          efinBudgetInt.setStatus("OP");
          OBDal.getInstance().save(efinBudgetInt);
        } else if (efinBudgetInt.getStatus().equals("OP")) {
          if (efinBudgetInt.getEFINBudgetList().size() == 0) {
            efinBudgetInt.setStatus("DR");
            OBDal.getInstance().save(efinBudgetInt);
            OBDal.getInstance().flush();
          } else {
            result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_linked_budget@");
            bundle.setResult(result);
            return;
          }
        }
      }
      OBDal.getInstance().flush();

      result = OBErrorBuilder.buildMessage(null, "Success", "@EFIN_Process_Success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      LOG.error(" Exception while submit budget Intialization " + e);
      result = OBErrorBuilder.buildMessage(null, "error", e.toString());
      bundle.setResult(result);
      return;
    }

  }
}
