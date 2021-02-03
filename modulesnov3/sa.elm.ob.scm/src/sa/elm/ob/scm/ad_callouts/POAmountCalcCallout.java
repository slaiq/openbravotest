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

public class POAmountCalcCallout extends SimpleCallout {

  /**
   * Callout for PO Amount Calculation
   */

  private static Logger log = Logger.getLogger(POAmountCalcCallout.class);

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpemEscmAdvpaymntPercntge = vars.getNumericParameter("inpemEscmAdvpaymntPercntge");
    String inpemEscmAdvpaymntAmt = vars.getNumericParameter("inpemEscmAdvpaymntAmt");
    String inpemEscmRetainPercn = vars.getNumericParameter("inpemEscmRetainPercn");
    String inpemEscmTotretainAmt = vars.getNumericParameter("inpemEscmTotretainAmt");
    // String inpgrandtotal = vars.getNumericParameter("inpgrandtotal");
    String inpOrderId = vars.getStringParameter("inpcOrderId");

    // String inpemEscmPoamount = vars.getStringParameter("inpemEscmPoamount");
    log.info("inpLastFieldChanged>" + inpLastFieldChanged);
    BigDecimal amt = new BigDecimal(0);
    try {
      OBContext.setAdminMode();
      Order order = OBDal.getInstance().get(Order.class, inpOrderId);
      BigDecimal inpgrandtotal = order.getGrandTotalAmount();
      /*
       * calculated if advance payment % is entered = Total PO amount * Advance payment %
       */
      if (inpLastFieldChanged.equals("inpemEscmAdvpaymntPercntge")) {
        if (inpgrandtotal != null && inpemEscmAdvpaymntPercntge != null) {
          amt = inpgrandtotal
              .multiply(new BigDecimal(inpemEscmAdvpaymntPercntge).divide(new BigDecimal(100)));
          info.addResult("inpemEscmAdvpaymntAmt", amt);
          info.addResult("inpemEscmRecoupmentAmt", amt);
          info.addResult("inpemEscmRetainageAmt", amt);
          info.addResult("inpemEscmRecoupmentPer", inpemEscmAdvpaymntPercntge);
          info.addResult("inpemEscmRetainagePer", inpemEscmAdvpaymntPercntge);
        }
      }
      /*
       * calculated if advance payment amount is entered = advance payment amount / Total PO amount
       */
      else if (inpLastFieldChanged.equals("inpemEscmAdvpaymntAmt")) {
        if (inpgrandtotal != null && inpemEscmAdvpaymntAmt != null) {
          amt = (new BigDecimal(inpemEscmAdvpaymntAmt).multiply(new BigDecimal(100)))
              .divide(inpgrandtotal, 2, RoundingMode.FLOOR);
          info.addResult("inpemEscmAdvpaymntPercntge", amt);
          info.addResult("inpemEscmRecoupmentPer", amt);
          info.addResult("inpemEscmRetainagePer", amt);
          info.addResult("inpemEscmRecoupmentAmt", inpemEscmAdvpaymntAmt);
          info.addResult("inpemEscmRetainageAmt", inpemEscmAdvpaymntAmt);
        }
      }
      /*
       * calculated if Retainage %is entered = Total PO amount * Retainage %
       */
      else if (inpLastFieldChanged.equals("inpemEscmRetainPercn")) {
        if (inpgrandtotal != null && inpemEscmRetainPercn != null) {
          amt = inpgrandtotal
              .multiply(new BigDecimal(inpemEscmRetainPercn).divide(new BigDecimal(100)));
          info.addResult("inpemEscmTotretainAmt", amt);
        }
      }
      /*
       * calculated if Total Retainage Amount is entered = Total Retainage Amount / Total PO amount
       */
      else if (inpLastFieldChanged.equals("inpemEscmTotretainAmt")) {
        if (inpgrandtotal != null && inpemEscmTotretainAmt != null) {
          amt = new BigDecimal(inpemEscmTotretainAmt).divide(inpgrandtotal);
          info.addResult("inpemEscmRetainPercn", amt);
        }
      }
      info.addResult("inpgrandtotal", inpgrandtotal);

    } catch (NumberFormatException e) {
      log.error("Number Format Exception in POAmountCalcCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } catch (Exception e) {
      log.error("Exception in POAmountCalcCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
