package sa.elm.ob.utility.ad_process;

import java.sql.Connection;

import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

/**
 * 
 * @author poongodi on 28/11/2018
 *
 */
public class RemoveAccessProcess extends DalBaseProcess {
  /**
   * This class is responsible to removing access of all windows,forms,lists,process to the to the
   * user until we don't have processed flag as 'N'
   */
  static int counter = 0;

  private ProcessLogger logger;

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();

    try {
      String clientId = bundle.getContext().getClient();
      Connection conn = OBDal.getInstance().getConnection();
      RemoveAccessProcessDAO dao = new RemoveAccessProcessDAO(conn);
      int preferencecount = dao.getProcessedRecord(clientId);
      if (preferencecount == 1) {
        logger.log("Success");
      } else {
        logger.log("Failure");
      }

    } catch (Exception e) {
      throw new JobExecutionException(e.getMessage(), e);
    }
  }
}
