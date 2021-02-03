package sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke;

import java.sql.Connection;
import java.util.Iterator;

import javax.enterprise.inject.Instance;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;

/**
 * 
 * @author sathish kumar
 *
 */

public class GetFactoryMassRevoke {

  public Logger log4j = Logger.getLogger(GetFactoryMassRevoke.class);

  private Instance<ProcessMassRevoke> hooks = null;

  public GetFactoryMassRevoke(Instance<ProcessMassRevoke> hooks2) {
    this.hooks = hooks2;
  }

  /**
   * This method is used to return factory object of the class based on the windowId
   * 
   * @param windowId
   * @param con
   * @return corresponding class object
   */
  public MassRevoke getRevoke(String windowId, Connection con) {
    MassRevoke massRevoke = null;

    try {
      massRevoke = executeHooks(hooks, windowId, con);
    } catch (Exception e) {
      OBException obException = new OBException(e.getMessage(), e.getCause());
      throw obException;
    }

    return massRevoke;

  }

  /**
   * This method is used to perform mass revoke
   * 
   * @param hooks
   * @param param1
   * @param param2
   * @return
   * @throws Exception
   */
  private MassRevoke executeHooks(Instance<? extends Object> hooks, String param1,
      Connection param2) throws Exception {
    MassRevoke massRevoke = null;
    for (Iterator<? extends Object> procIter = hooks.iterator(); procIter.hasNext();) {
      Object proc = procIter.next();
      massRevoke = ((ProcessMassRevoke) proc).preProcess(param1, param2);
      if (massRevoke != null) {
        break;
      }

    }
    return massRevoke;
  }

}
