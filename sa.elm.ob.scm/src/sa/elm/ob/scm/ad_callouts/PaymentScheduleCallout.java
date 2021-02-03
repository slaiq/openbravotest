package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
 * @author Mouli K
 * @implNote This callout is to fetch the default value for Amount,Value % & Updating the Amount
 *           based on Value % and vice versa.
 *
 */

public class PaymentScheduleCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(PaymentScheduleCallout.class);
  private static final long serialVersionUID = 1L;
  Integer roundoffConst = 2;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String orderId = vars.getStringParameter("inpcOrderId");
    String inpamount = vars.getStringParameter("inpamount");
    inpamount = inpamount.replaceAll(",", "");
    String inpvalueper = vars.getStringParameter("inpvaluePer");
    inpvalueper = inpvalueper.replaceAll(",", "");

    Order order = OBDal.getInstance().get(Order.class, orderId);
    BigDecimal amt = BigDecimal.ZERO, per = BigDecimal.ZERO,
        amount = (inpamount != null && inpamount != "") ? new BigDecimal(inpamount)
            : BigDecimal.ZERO,
        valueper = (inpvalueper != null && inpvalueper != "") ? new BigDecimal(inpvalueper)
            : BigDecimal.ZERO;

    try {
      OBContext.setAdminMode();
      if (order != null) {
        if (inpLastFieldChanged.equals("inpadOrgId")) {
          amt = order.getGrandTotalAmount().subtract(order.getEscmPaymentscheduleAmt());
          if (order.getGrandTotalAmount().compareTo(BigDecimal.ZERO) > 0)
            per = (amt.multiply(new BigDecimal(100))).divide(order.getGrandTotalAmount(), 2,
                RoundingMode.FLOOR);
          info.addResult("inpamount", amt.setScale(roundoffConst, RoundingMode.HALF_UP));
          info.addResult("inpvaluePer", per.setScale(roundoffConst, RoundingMode.HALF_UP));
        } else if (inpLastFieldChanged.equals("inpamount")) {
          if (order.getGrandTotalAmount().compareTo(BigDecimal.ZERO) > 0)
            per = (amount.multiply(new BigDecimal(100))).divide(order.getGrandTotalAmount(), 2,
                RoundingMode.FLOOR);
          info.addResult("inpvaluePer", per.setScale(roundoffConst, RoundingMode.HALF_UP));
        } else if (inpLastFieldChanged.equals("inpvaluePer")) {
          amt = order.getGrandTotalAmount().multiply(valueper.divide(new BigDecimal(100)));
          info.addResult("inpamount", amt.setScale(roundoffConst, RoundingMode.HALF_UP));
        }
      }

    } catch (Exception e) {
      log.error("Exception in PaymentScheduleCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
