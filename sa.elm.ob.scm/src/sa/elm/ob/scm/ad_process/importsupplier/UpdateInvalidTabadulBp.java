package sa.elm.ob.scm.ad_process.importsupplier;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

import sa.elm.ob.scm.actionHandler.SupplierController;

public class UpdateInvalidTabadulBp extends DalBaseProcess {
  /**
   * This Process Class is responsible to update tabadul info invalid Business Partner records
   */
  static int counter = 0;

  private ProcessLogger logger;

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    final Session session = OBDal.getInstance().getSession();
    Client objClient = OBContext.getOBContext().getCurrentClient();
    SQLQuery query = null;
    SupplierController supplierController = new SupplierController();

    try {

      String qry = "select em_efin_documentno "
          + " from c_bpartner where em_escm_tabadulid is not null "
          + " and em_escm_tabadulid='-1' and ad_client_id='" + objClient.getId() + "'";
      query = session.createSQLQuery(qry);
      // query.setParameter("product", product.getId());
      logger.log("Number of BusinessPartnerUpdated " + query.list().size() + "\n");
      if (query.list().size() > 0) {
        for (Object resultObj : query.list()) {
          final String objBusinessPartner = (String) resultObj;
          BusinessPartner partner = supplierController.getBusinessPartnerByCRN(objBusinessPartner);
          supplierController.updateSupplier(partner);

        }
      }

    } catch (Exception e) {
      // catch any possible exception and throw it as a Quartz
      // JobExecutionException
      throw new JobExecutionException(e.getMessage(), e);
    }
  }
}
