package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;

/**
 * 
 * @author qualian
 *
 */

public class PurOrdercontractSummaryline extends SimpleCallout {
  private static Logger log = Logger.getLogger(PurOrdercontractSummaryline.class);
  /**
   * Callout to update the line tot price
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpemEscmQuantity = vars.getNumericParameter("inpemEscmQuantity");
    String inpemEscmNegotUnitPrice = vars.getNumericParameter("inpemEscmNegotUnitPrice");
    BigDecimal inpquantity = BigDecimal.ZERO;
    BigDecimal inpnegprice = BigDecimal.ZERO, fundsAvailable = BigDecimal.ZERO;
    String budgetInit = vars.getStringParameter("inpemEfinBudgetintId");
    String uniqueCode = vars.getStringParameter("inpemEfinCValidcombinationId");
    if (!inpemEscmQuantity.equals("")) {
      inpquantity = new BigDecimal(inpemEscmQuantity);

    }
    if (!inpemEscmNegotUnitPrice.equals("")) {
      inpnegprice = new BigDecimal(inpemEscmNegotUnitPrice);
    }
    BigDecimal multiplyamt = BigDecimal.ZERO;
    try {

      if (inpLastFieldChanged.equals("inpemEscmQuantity")
          || inpLastFieldChanged.equals("inpemEscmNegotUnitPrice")) {
        if (!inpemEscmQuantity.equals("") || !inpemEscmNegotUnitPrice.equals("")) {
          multiplyamt = inpquantity.multiply(inpnegprice);
          info.addResult("inpemEscmLineTotalprice", multiplyamt);
        }
      }
      // update uniquecode name, funds available
      if (inpLastFieldChanged.equals("inpemEfinCValidcombinationId")) {
        if (uniqueCode.equals("")) {
          info.addResult("inpemEfinFundsAvailable", BigDecimal.ZERO);
          info.addResult("inpemEfinUniquecodename", "");
        } else {
          AccountingCombination dimention = OBDal.getInstance().get(AccountingCombination.class,
              uniqueCode);
          fundsAvailable = RequisitionDao.getAutoEncumFundsAvailable(uniqueCode, budgetInit);
          info.addResult("inpemEfinFundsAvailable", fundsAvailable);
          info.addResult("inpemEfinUniquecodename", dimention.getEfinUniquecodename());
        }
      }

    } catch (Exception e) {
      log.debug("Exception in PurOrdercontractSummaryline  callout:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
