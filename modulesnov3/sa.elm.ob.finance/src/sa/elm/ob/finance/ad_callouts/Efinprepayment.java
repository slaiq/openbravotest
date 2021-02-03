package sa.elm.ob.finance.ad_callouts;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.invoice.InvoiceLine;

public class Efinprepayment extends SimpleCallout {

  /**
   * Callout to update the Dimensions in order to receive.
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    List<InvoiceLine> ls = new ArrayList<InvoiceLine>();
    try {
      if (inpLastFieldChanged.equals("inpemEfinPrepayment")) {
        String invoice_id = vars.getStringParameter("inpemEfinPrepayment");
        OBQuery<InvoiceLine> inv = OBDal.getInstance().createQuery(InvoiceLine.class,
            "invoice.id='" + invoice_id + "'");
        ls = inv.list();
        if (ls.size() > 0) {
          InvoiceLine objInvLine = ls.get(0);

          info.addResult("inpemEfinCValidcombinationId",
              objInvLine.getEfinCValidcombination().getId());
          if (objInvLine.getEfinExpenseAccount() != null)
            info.addResult("inpemEfinExpenseAccount", objInvLine.getEfinExpenseAccount().getId());
        } else {
          info.addResult("inpemEfinCValidcombinationId", null);
        }

      }
    } catch (Exception e) {
      
    }

  }
}
