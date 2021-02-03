/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.encumbrance;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetManencum;

/**
 * @author Gowtham.V
 */

public class EncumbranceClose implements Process {
  /**
   * This process to CLose the encumbrance.
   */
  private static final Logger LOG = LoggerFactory.getLogger(EncumbranceClose.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    String strEncumId = (String) bundle.getParams().get("Efin_Budget_Manencum_ID");
    String status = "", message = "";
    try {
      EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class, strEncumId);
      encum.setDocumentStatus("CL");
      OBDal.getInstance().save(encum);
      // Message
      status = "success";
      message = "@ProcessOK@";
      final OBError result = OBErrorBuilder.buildMessage(null, status, message);
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Close the encumbrance" + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    }
  }

}
