package sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine;

import java.util.Map;


import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.InvoiceLine;

import sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao.MultipleInvoiceLineAgainstPOLineDAO;
import sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao.MultipleInvoiceLineAgainstPOLineDAOImpl;

/**
 * 
 * @author Priyanka Ranjan on 04/05/2019
 * 
 */
// Handler file for Split Invoice Line for a PO

public class MultipleInvoiceLineAgainstPOLine extends BaseProcessActionHandler {
  private static Logger LOG = Logger.getLogger(MultipleInvoiceLineAgainstPOLine.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    try {
      OBContext.setAdminMode();
      InvoiceLine invoiceline = null;
      String message = "";
      MultipleInvoiceLineAgainstPOLineDAO dao = new MultipleInvoiceLineAgainstPOLineDAOImpl();

      JSONObject jsonRequest = new JSONObject(content);
      String invoicelineId = jsonRequest.getString("inpcInvoicelineId");

      invoiceline = OBDal.getInstance().get(InvoiceLine.class, invoicelineId);
      if (invoiceline != null) {
        // split the line
        dao.cloneInvoiceLine(invoiceline);
        // set success message
        JSONObject errormsg = new JSONObject();
        JSONObject refreshGrid = new JSONObject();
        message = OBMessageUtils.parseTranslation("@Efin_Line_Splited@");
        errormsg.put("severity", "success");
        errormsg.put("text", message);
        jsonResponse.put("message", errormsg);
        refreshGrid.put("message", errormsg);
        refreshGrid.put("refreshGrid", new JSONObject());
        jsonResponse.put("responseActions", refreshGrid);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
        return jsonResponse;
      }

    } catch (Exception e) {
      LOG.error("Exception in MultipleInvoiceLineAgainstPOLine :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;

  }

}
