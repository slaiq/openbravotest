package sa.elm.ob.finance.ad_process;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import sa.elm.ob.finance.dao.UniqueCodeGenerationDAO;

/**
 * 
 * @author sathishkumar.P
 * 
 */

public class UniqueCodeGenerationProcess extends DalBaseProcess {

  /**
   * This process is used to create unique code combination for various accounts
   */

  private static final Logger LOG = Logger.getLogger(UniqueCodeGenerationProcess.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    boolean isSuccess = Boolean.FALSE;
    final String clientID = OBContext.getOBContext().getCurrentClient().getId();
    OBError result;
    try {

      final OBQuery<AcctSchema> dimensions = OBDal.getInstance().createQuery(AcctSchema.class,
          "client.id='" + clientID + "'");

      if (dimensions.list().size() > 0) {
        final AcctSchema glConfigHeader = dimensions.list().get(0);
        if (!glConfigHeader.isEfinIsready()) {
          result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_Glconfig_notready@");
          bundle.setResult(result);
          return;
        }
      } else {
        result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_Process_fail@");
        bundle.setResult(result);
        return;
      }

      isSuccess = UniqueCodeGenerationDAO
          .generateCode(OBContext.getOBContext().getCurrentClient().getId());

      if (isSuccess) {
        result = OBErrorBuilder.buildMessage(null, "Success", "@EFIN_Process_Success@");
        bundle.setResult(result);
        return;
      } else {
        result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_Process_fail@");
        bundle.setResult(result);
        return;
      }

    } catch (OBException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("error while generating unique code generation", e);
      }
    }

  }
}
