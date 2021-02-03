package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Gopalakrishnan
 *
 */
@SuppressWarnings("serial")
public class PropertyCompensationHeaderCallout extends SimpleCallout {
  /**
   * Call out for property compensation header
   */
  private static final Logger log = LoggerFactory
      .getLogger(PropertyCompensationHeaderCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strBpId = vars.getStringParameter("inpcBpartnerId");
    String inpanqathAmount = vars.getNumericParameter("inpanqathAmount");
    String inplandAmount = vars.getNumericParameter("inplandAmount");
    String inptheSpace = vars.getNumericParameter("inptheSpace");
    String inpamountCompensation = vars.getNumericParameter("inpamountCompensation");
    String inpunitPrice = vars.getNumericParameter("inpunitPrice");
    BigDecimal anqathAmt = BigDecimal.ZERO;
    BigDecimal landAmount = BigDecimal.ZERO;
    BigDecimal theSpace = BigDecimal.ZERO;
    BigDecimal amountComp = BigDecimal.ZERO;
    BigDecimal unitPrice = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      if (inpLastFieldChanged.equals("inpcBpartnerId")) {
        BusinessPartner objBp = OBDal.getInstance().get(BusinessPartner.class, strBpId);
        if (objBp != null) {
          info.addResult("inpbpname", objBp.getId());
          info.addResult("inpcBpGroupId", objBp.getBusinessPartnerCategory().getId());
          if (objBp.getADUserList().size() > 0) {
            info.addResult("inpbpmobile", objBp.getADUserList().get(0) == null ? ""
                : objBp.getADUserList().get(0).getPhone());
          }
          info.addResult("inpbpNin", objBp.getEfinNationalidnumber());

        }
      }
      if (inpLastFieldChanged.equals("inpanqathAmount")
          || inpLastFieldChanged.equals("inplandAmount")) {
        anqathAmt = new BigDecimal(inpanqathAmount);
        landAmount = new BigDecimal(inplandAmount);
        amountComp = anqathAmt.add(landAmount);
        info.addResult("inpamountCompensation", amountComp);
      }
      if (inpLastFieldChanged.equals("inpamountCompensation")
          || inpLastFieldChanged.equals("inpanqathAmount")
          || inpLastFieldChanged.equals("inplandAmount")
          || inpLastFieldChanged.equals("inptheSpace")) {
        theSpace = new BigDecimal(inptheSpace);
        // amountComp = new BigDecimal(inpamountCompensation);
        if (theSpace.compareTo(BigDecimal.ZERO) > 0) {
          anqathAmt = new BigDecimal(inpanqathAmount);
          landAmount = new BigDecimal(inplandAmount);
          amountComp = anqathAmt.add(landAmount);
          unitPrice = amountComp.divide(theSpace);
        } else {
          unitPrice = BigDecimal.ZERO;
        }
        info.addResult("inpunitPrice", unitPrice);

      }

    } catch (Exception e) {
      if (log.isDebugEnabled())
        log.error("Exception in PropertyCompensationHeaderCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
