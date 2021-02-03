package sa.elm.ob.utility.ad_process;

import java.sql.Connection;

import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

/**
 * 
 * @author qualian
 *
 */
public class DelegationRoleAccess extends DalBaseProcess {
  /**
   * This class is responsible to give access of all windows,forms,lists,process to the delegated
   * user
   */
  static int counter = 0;

  private ProcessLogger logger;

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();

    try {
      String clientId = bundle.getContext().getClient();
      String userId = bundle.getContext().getUser();
      int count = 0;
      boolean recalculate = false;
      String recalc = "N";
      Connection conn = OBDal.getInstance().getConnection();
      DelegationRoleAccessDAO dao = new DelegationRoleAccessDAO(conn);
      recalculate = dao.delegationRecalculate(clientId);
      if (recalculate)
        recalc = "Y";
      count = dao.delegationRoleDelete(clientId);
      if (count == 1) {
        logger.log("Success : all delegations access still yesterday was removed");
      } else {
        logger.log("Failure : all delegations access still yesterday was not removed");
      }

      int preferencecount = dao.delegationAccessPreference(clientId, recalc);
      if (preferencecount == 1) {
        logger.log("Success: all delegations access given to preferences ");
      } else {
        logger.log("Failure: all delegations access not given to preferences");
      }

      int windowCount = dao.delegationAccessWindow(clientId, userId, recalc);
      if (windowCount == 1) {
        logger.log("Success: all delegations access given to window,form,process,from,list ");
      } else {
        logger.log("Failure: all delegations access not given to window,form,process,from,list");
      }
      int checkCount = dao.delegationCheckBoxAccess(clientId);
      if (checkCount == 1) {
        logger.log("Success : all checkbox based delegation access given");
      } else {
        logger.log("Failure : all checkbox based delegation access not given ");
      }
      int updateCount = dao.delegationUpdateProcessFlag(clientId);
      if (updateCount == 1) {
        logger.log("Success : delegationUpdateProcessFlag");
      } else {
        logger.log("Failure : delegationUpdateProcessFlag ");
      }

    } catch (Exception e) {
      throw new JobExecutionException(e.getMessage(), e);
    }
  }
}
