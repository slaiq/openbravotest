package sa.elm.ob.finance.hooks;

import org.openbravo.advpaymentmngt.ProcessInvoiceHook;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;

/**
 * 
 * @author sathish kumar.p
 * 
 *         This class is to implement the hook for Process Invoice. This is pre-execution hook
 * 
 *
 */

public class PreProcessInvoiceHook implements ProcessInvoiceHook {

  private final String VOID_STATUS = "RC";
  private final String ERROR = "Error";

  @Override
  public OBError preProcess(Invoice invoice, String strDocAction) {

    if (VOID_STATUS.equals(strDocAction)) {
      OBError error = new OBError();
      error.setType(ERROR);
      error.setTitle(OBMessageUtils.messageBD("OBUIAPP_Error"));
      error.setMessage(OBMessageUtils.messageBD("Efin_void_notallowed"));
      return error;
    }

    if ("RE".equals(strDocAction)) {
      if ("7".equals(invoice.getEfinSadadbillstatus())
          || invoice.getEfinSadadnewbillnumber() != null) {
        String errorMSG = OBMessageUtils.messageBD("EFIN_CantreactivateAR");
        OBError msg = new OBError();
        msg.setType("Error");
        msg.setTitle("Error");
        msg.setMessage(errorMSG);
        return msg;
      } else {
        invoice.setEfinSadadhaserror(false);
        invoice.setEfinSadaderrormessage(null);
        OBDal.getInstance().save(invoice);
        OBDal.getInstance().flush();
      }
    }
    return null;
  }

  @Override
  public OBError postProcess(Invoice invoice, String strDocAction) {
    // TODO Auto-generated method stub
    return null;
  }

}
