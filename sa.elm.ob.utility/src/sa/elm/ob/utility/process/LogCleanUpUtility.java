package sa.elm.ob.utility.process;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

/**
 * 
 * @author Gopalakrishnan on 26/11/2017
 * 
 */

public class LogCleanUpUtility extends DalBaseProcess {
  /**
   * This Process Class is responsible to clean up openbravo log
   * 
   */
  static int counter = 0;

  private ProcessLogger logger;

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    final Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null, ps2 = null, ps3 = null, ps4 = null, ps5 = null;
    logger.log("Starting background to clean up process " + counter + "\n");
    try {
      ps = conn.prepareStatement("truncate table ad_process_run");
      ps.executeUpdate();
      ps2 = conn.prepareStatement("delete from ad_pinstance ;");
      ps2.executeUpdate();
      ps3 = conn.prepareStatement(
          "delete from AD_Process_Request where status<> 'SCH' and status <> 'MIS';");
      ps3.executeUpdate();
      ps4 = conn.prepareStatement("truncate table ad_session_usage_audit cascade");
      ps4.executeUpdate();
      ps5 = conn.prepareStatement("truncate table ad_session_usage_audit cascade");
      ps5.executeUpdate();

    } catch (Exception e) {
      // catch any possible exception and throw it as a Quartz
      // JobExecutionException
      throw new JobExecutionException(e.getMessage(), e);
    }
  }
}
