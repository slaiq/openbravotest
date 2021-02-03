package sa.elm.ob.finance.process.account_tree;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class UpdateAccountTree extends DalBaseProcess implements KillableProcess {

  private static final Logger log4j = Logger.getLogger(UpdateAccountTree.class);
  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    OBError result = new OBError();
    SimpleDateFormat datefFormatter = new SimpleDateFormat("dd-MM-yyyy");
    OBQuery<ElementValue> query = null;

    try {
      int counter = 0;
      String ids = "";
      OBContext.setAdminMode();
      result.setType("Success");
      result.setTitle(OBMessageUtils.messageBD("Efin_UpdateAcc_Tree"));

      query = OBDal.getInstance().createQuery(ElementValue.class,
          " where client.id='" + bundle.getContext().getClient() + "'");
      query.setFilterOnActive(Boolean.FALSE);

      addLog("Starting Background Process");

      if (query != null && query.list().size() > 0) {
        for (ElementValue element : query.list()) {
          if (element.getValidFromDate() != null && datefFormatter
              .format(element.getValidFromDate()).equals(datefFormatter.format(new Date()))) {
            element.setActive(Boolean.TRUE);

            if (element.getValidToDate() == null)
              element.setValidToDate(datefFormatter.parse("31-12-9999"));

            ids = ids + "," + element.getId();
            counter++;
          }

          if (element.isActive() && element.getValidToDate() != null
              && element.getValidToDate().before(new Date())) {
            element.setActive(Boolean.FALSE);
            ids = ids + "," + element.getId();
            counter++;
          }

          OBDal.getInstance().save(element);
        }
      }

      addLog(OBMessageUtils.messageBD("Efin_UpdateAcc_Tree"));
      addLog("Updated Records Count: " + counter);
      addLog("Updated Ids: " + ids.replaceFirst(",", ""));

      OBDal.getInstance().flush();
      bundle.setResult(result);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      result = OBMessageUtils.translateError(bundle.getConnection(), bundle.getContext().toVars(),
          OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
      log4j.error(result.getMessage(), e);
      addLog(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
      addLog("Ending background process.");
    }
  }

  /**
   * Adds a message to the log.
   * 
   * @param msg
   *          to add to the log
   */
  private void addLog(String msg) {
    addLog(msg, false);
  }

  /**
   * Add a message to the log.
   * 
   * @param msg
   * @param generalLog
   */
  private void addLog(String msg, boolean generalLog) {
    logger.log(msg + "\n");
  }

  @Override
  public void kill(ProcessBundle processBundle) throws Exception {
    addLog("Process Killed");
  }
}
