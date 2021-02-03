package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;

/**
 * 
 * @author divya J 22-01-2020
 *
 */

public class POHoldPlanCallOut extends SimpleCallout {

  private static Logger log = Logger.getLogger(POHoldPlanCallOut.class);

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String orderID = vars.getStringParameter("inpcOrderId");
    try {
      OBContext.setAdminMode();
      if (inpLastFieldChanged.equals("inpcOrderId")) {
        Order order = OBDal.getInstance().get(Order.class, orderID);
        if (order != null) {
          info.addResult("inpdescription", order.getEscmNotes());
        }
      }

    } catch (Exception e) {
      log.error("Exception in POHoldPlanCallOut:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
