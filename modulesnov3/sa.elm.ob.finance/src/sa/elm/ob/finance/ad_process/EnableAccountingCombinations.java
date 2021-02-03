package sa.elm.ob.finance.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 * 
 * @author sathishkumar.P
 * 
 */

public class EnableAccountingCombinations extends DalBaseProcess {

  /**
   * This process is enable all valid combination
   */

  private static final Logger LOG = Logger.getLogger(GLConfigurationReadyProcess.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    OBError result = null;
    Connection connection = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;

    try {

      String query = "update c_validcombination set isactive ='Y',EM_Efin_Issaved='Y' where ad_client_id ='"
          + OBContext.getOBContext().getCurrentClient().getId()
          + "' and account_id in (select c_elementvalue_id from c_elementvalue where isactive='Y' and ad_client_id='"
          + OBContext.getOBContext().getCurrentClient().getId() + "')";
      ps = connection.prepareStatement(query);
      ps.executeUpdate();

      result = OBErrorBuilder.buildMessage(null, "Success", "@EFIN_Process_Success@");
      bundle.setResult(result);
      return;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Exception in EnableAccountingCombinations" + e, e);
      }
      result = OBErrorBuilder.buildMessage(null, "error", e.toString());
      bundle.setResult(result);
      return;
    } finally {
      try {
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Exception in enable accounting" + e, e);
        }
      }
    }

  }
}
