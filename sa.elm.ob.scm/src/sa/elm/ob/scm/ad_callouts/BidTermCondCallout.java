package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;


/**
 * This Callout is to validate the Attribute Value field
 * 
 * @author Mouli.K
 * 
 */

@SuppressWarnings("serial")
public class BidTermCondCallout extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;

    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String attributeValue = vars.getStringParameter("inpattrvalue");
    attributeValue = attributeValue.trim();
    String numberRegExp = "^[1-9]\\d*(\\.\\d+)?$";
    BigDecimal attrValue = BigDecimal.ZERO;

    try {
      if (inpLastFieldChanged.equals("inpattrvalue")) {
        if (!attributeValue.isEmpty() && attributeValue.matches(numberRegExp)) {
          attrValue = new BigDecimal(attributeValue);
          info.addResult("inpattrvalue", attrValue.divide(BigDecimal.ONE, 2, RoundingMode.HALF_UP).toString());
        } else {
          info.addResult("inpattrvalue", "");
          info.addResult("ERROR", OBMessageUtils.messageBD("ESCM_AttrVal_Empty"));
          return;
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in BidTermCondCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
