package sa.elm.ob.finance.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.finance.EFINCostcenters;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.scm.actionHandler.irtabs.irtabprocess.BankGrnteWrkBnch;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

/**
 * 
 * @author sathish kumar.p
 *
 */

public class CostCenterTabDisablePorcess extends IRTabIconVariables {
  private static Logger log = Logger.getLogger(BankGrnteWrkBnch.class);

  /**
   * This class is used to disable and enable delete button in costcenter tab present in cost center
   * linking window
   * 
   */

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {

      OBContext.setAdminMode();
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");

      // cost center linking department tab
      if (tabId.equals("57AA8C32AA2E4A36819DAA3AFEF2DC1C")) {

        String bcuDep = null, costCenterDep = null;
        if (!org.apache.commons.lang.StringUtils.isEmpty(recordId)) {
          // get budget control parameter 990 and 999 dep
          final OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance().createQuery(
              EfinBudgetControlParam.class,
              "as e where e.client.id= :clientID ");
          controlParam.setNamedParameter("clientID", OBContext.getOBContext().getCurrentClient().getId());
          if (controlParam.list().size() > 0) {
            EfinBudgetControlParam config = controlParam.list().get(0);
            bcuDep = config.getBudgetcontrolunit().getId();
            costCenterDep = config.getBudgetcontrolCostcenter().getId();
          }

          EFINCostcenters cost = OBDal.getInstance().get(EFINCostcenters.class, recordId);
          if (cost.getDepartment().getId().equals(bcuDep)
              || cost.getDepartment().getId().equals(costCenterDep)) {
            enable = 1;
          } else {
            enable = 0;
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
