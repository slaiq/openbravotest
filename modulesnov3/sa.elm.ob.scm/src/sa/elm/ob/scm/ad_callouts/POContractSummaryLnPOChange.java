package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;

/**
 * 
 * @author qualian
 *
 */
public class POContractSummaryLnPOChange extends SimpleCallout {
  private static final String infoMessage = "Escm_DiscountChanged_AfterTax";
  /**
   * Callout for c_orderLine table
   */

  private static Logger log = Logger.getLogger(POContractSummaryLnPOChange.class);
  Integer roundoffConst = 2;

  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    // final String paramStandardPOSPrecision = info.getStringParameter("inpemObposPosprecision");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpemEscmPoChangeType = vars.getStringParameter("inpemEscmPoChangeType");
    String inpemEscmPoChangeValue = vars.getNumericParameter("inpemEscmPoChangeValue");
    // String inpemEscmPoChangeFactor = vars.getStringParameter("inpemEscmPoChangeFactor");
    // String inpemEscmLineTotalUpdated = vars.getNumericParameter("inpemEscmLineTotalUpdated");
    String inplineNetAmount = vars.getNumericParameter("inplinenetamt");
    // String inpcOrderLineId = vars.getStringParameter("inpcOrderlineId");
    String strCOrderId = vars.getStringParameter("inpcOrderId");
    BigDecimal changeValue = StringUtils.isEmpty(inpemEscmPoChangeValue) ? BigDecimal.ZERO
        : new BigDecimal(inpemEscmPoChangeValue);
    String parsedMessage = null;
    try {
      OBContext.setAdminMode();
      log.debug("inpLastFieldChanged>" + inpLastFieldChanged);
      // Get total Amount Lookuo Id
      // String totalAmtChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
      // "01");
      // Get total Percent Lookuo Id
      // String totalPercentChangeType = POContractSummaryTotPOChangeDAO
      // .getPOChangeLookUpId("POCHGTYP", "02");
      BigDecimal lnPriceUpdatedAmt = BigDecimal.ZERO;
      // lnPriceUpdatedAmt = POContractSummaryTotPOChangeDAO.getLineNetAmt(inpcOrderLineId);
      // if (lnPriceUpdatedAmt == null || lnPriceUpdatedAmt.compareTo(BigDecimal.ZERO) == 0)
      lnPriceUpdatedAmt = new BigDecimal(inplineNetAmount);
      log.debug("lnPriceUpdatedAmt>" + lnPriceUpdatedAmt);

      if (inpLastFieldChanged.equals("inpemEscmPoChangeType")
          || inpLastFieldChanged.equals("inpemEscmPoChangeValue")
          || inpLastFieldChanged.equals("inpemEscmPoChangeFactor")) {
        Order order = OBDal.getInstance().get(Order.class, strCOrderId);
        if (order.getEscmTotalTaxamt().compareTo(BigDecimal.ZERO) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);
        }
        order.setEscmCalculateTaxlines(false);
        OBDal.getInstance().save(order);
        OBDal.getInstance().flush();
      }
      // if change type as amount then round off the value to 2 decimal
      if (inpLastFieldChanged.equals("inpemEscmPoChangeValue")) {
        String amountChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
            "01");
        if (amountChangeType.equals(inpemEscmPoChangeType)) {
          info.addResult("inpemEscmPoChangeValue",
              changeValue.setScale(roundoffConst, RoundingMode.HALF_UP));
        }
      }
      if (inpLastFieldChanged.equals("inpemEscmPoChangeType")) {
        String amountChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
            "01");
        if (amountChangeType.equals(inpemEscmPoChangeType)) {
          if (changeValue.compareTo(new BigDecimal(0)) > 0) {
            info.addResult("inpemEscmPoChangeValue",
                changeValue.setScale(roundoffConst, RoundingMode.HALF_UP));
          }
        }
      }
      // if (inpLastFieldChanged.equals("inpemEscmPoChangeType")
      // || inpLastFieldChanged.equals("inpemEscmPoChangeValue")
      // || inpLastFieldChanged.equals("inpemEscmPoChangeFactor")) {
      // if (inpemEscmPoChangeType != null) {
      // String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "01");
      // String incFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "02");
      // // PO Change Type - Total Amt Calc
      // if (inpemEscmPoChangeType.equals(totalAmtChangeType)
      // && new BigDecimal(inpemEscmPoChangeValue).compareTo(BigDecimal.ZERO) > 0) {
      // if (inpemEscmPoChangeFactor.equals(decFactId)) {
      // lnPriceUpdatedAmt = lnPriceUpdatedAmt
      // .subtract(new BigDecimal(inpemEscmPoChangeValue));
      // } else if (inpemEscmPoChangeFactor.equals(incFactId)) {
      // lnPriceUpdatedAmt = lnPriceUpdatedAmt.add(new BigDecimal(inpemEscmPoChangeValue));
      // }
      // } // PO Change Type - Percentage Calc
      // else if (inpemEscmPoChangeType.equals(totalPercentChangeType)
      // && new BigDecimal(inpemEscmPoChangeValue).compareTo(BigDecimal.ZERO) > 0) {
      // if (inpemEscmPoChangeFactor.equals(decFactId)) {
      // lnPriceUpdatedAmt = lnPriceUpdatedAmt.subtract(lnPriceUpdatedAmt
      // .multiply(new BigDecimal(inpemEscmPoChangeValue).divide(new BigDecimal("100"))));
      // } else if (inpemEscmPoChangeFactor.equals(incFactId)) {
      // lnPriceUpdatedAmt = lnPriceUpdatedAmt.add(lnPriceUpdatedAmt
      // .multiply(new BigDecimal(inpemEscmPoChangeValue).divide(new BigDecimal("100"))));
      // }
      // }
      // // set updated amt
      // info.addResult("inplineNetAmount", lnPriceUpdatedAmt);
      // } else {
      // // set updated amt
      // info.addResult("inplineNetAmount", lnPriceUpdatedAmt);
      // }
      // }
    } catch (Exception e) {
      log.debug("Exception in POContractSummaryLnPOChange:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}