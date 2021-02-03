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

import sa.elm.ob.finance.EfinBudgetControlParam;

/**
 * 
 * @author sathishkumar.P
 * 
 */

public class GLConfigurationReadyProcess extends DalBaseProcess {

  /**
   * This process is used to validate the dimensions defined in General Ledger Configuration
   */

  private static final Logger LOG = Logger.getLogger(GLConfigurationReadyProcess.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    String isvalid;
    boolean isComplete = false;
    OBError result = null;

    try {
      final String acctSchemaID = (String) bundle.getParams().get("C_AcctSchema_ID");
      Boolean isReady = false;

      // check the budget control parameters isready flag =true
      final OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance().createQuery(
          EfinBudgetControlParam.class,
          "client.id='" + OBContext.getOBContext().getCurrentClient().getId() + "'");

      if (controlParam.list().size() > 0) {
        for (EfinBudgetControlParam config : controlParam.list()) {
          if (config.isReady()) {
            isReady = true;
          }
        }
      } else {
        result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_Define_BudControl@");
        bundle.setResult(result);
        return;
      }

      if (!isReady) {
        result = OBErrorBuilder.buildMessage(null, "error", "@EFIN_Set_BudControlRdy@");
        bundle.setResult(result);
        return;
      }

      if (acctSchemaID != null) {
        isvalid = GLConfigurationReadyProcessDAO.validateDimension(acctSchemaID);

        if (isvalid == null) {
          // if Dimension is valid then set isready flag as "True" in AcctSchema table
          final AcctSchema acc = OBDal.getInstance().get(AcctSchema.class, acctSchemaID);
          acc.setEfinIsready(true);
          OBDal.getInstance().save(acc);
          OBDal.getInstance().flush();
        } else {
          result = OBErrorBuilder.buildMessage(null, "error", isvalid);
          bundle.setResult(result);
          return;
        }

        // Insert preference for based on 'isvisible' flag in dimension tab
        isComplete = GLConfigurationReadyProcessDAO.insertPreference(acctSchemaID);

        if (isComplete) {
          result = OBErrorBuilder.buildMessage(null, "Success", "@EFIN_Process_Success@");
          bundle.setResult(result);
          return;
        } else {
          result = OBErrorBuilder.buildMessage(null, "Success", "@EFIN_Process_Success@");
          bundle.setResult(result);
          return;
        }
      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Exception in GLConfigurationReadyProcess" + e, e);
      }
      result = OBErrorBuilder.buildMessage(null, "error", e.toString());
      bundle.setResult(result);
      return;
    }

  }
}
