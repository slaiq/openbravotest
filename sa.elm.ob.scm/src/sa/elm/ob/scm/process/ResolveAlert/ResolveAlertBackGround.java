package sa.elm.ob.scm.process.ResolveAlert;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.system.Client;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

/**
 * 
 * @author Gopalakrishnan on 13/04/2017
 * 
 */

public class ResolveAlertBackGround extends DalBaseProcess {
  /**
   * This Process Class is responsible to resolve new alerts for transactions alert
   * 
   */
  static int counter = 0;

  private ProcessLogger logger;

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    final Session session = OBDal.getInstance().getSession();
    Client objClient = OBContext.getOBContext().getCurrentClient();
    SQLQuery query = null;

    try {
      // select recent transaction date with physical qty is null and its transaction date
      String qry = "select ad.ad_alert_id from ad_alert ad "
          + " join m_requisition req on req.m_requisition_id=ad.referencekey_id and req.em_escm_doc_status='ESCM_AP' "
          + " where ad.status='NEW' and ad.created <= now() - interval'4 day' and ad.ad_client_id='"
          + objClient.getId()
          + "' "
          + " union  select ad.ad_alert_id from ad_alert ad  "
          + " join escm_material_request req on req.escm_material_request_id=ad.referencekey_id and ( req.status ='ESCM_TR' or req.status='DR') "
          + " where ad.status='NEW' and ad.created <= now() - interval'4 day' and ad.ad_client_id='"
          + objClient.getId() + "'";
      query = session.createSQLQuery(qry);
      // query.setParameter("product", product.getId());
      logger.log("Number of Alerts Solved " + query.list().size() + "\n");
      if (query.list().size() > 0) {
        for (Object resultObj : query.list()) {
          final String alertid = (String) resultObj;
          Alert objAlert = OBDal.getInstance().get(Alert.class, alertid.toString());
          objAlert.setAlertStatus("SOLVED");
          OBDal.getInstance().save(objAlert);
          OBDal.getInstance().flush();
        }
      }

    } catch (Exception e) {
      // catch any possible exception and throw it as a Quartz
      // JobExecutionException
      throw new JobExecutionException(e.getMessage(), e);
    }
  }
}
