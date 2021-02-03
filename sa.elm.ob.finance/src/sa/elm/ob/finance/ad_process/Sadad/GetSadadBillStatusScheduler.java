package sa.elm.ob.finance.ad_process.Sadad;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

/**
 * This class is scheduler which is used to get the bill status from saddad.
 * 
 * And update the status based on the response from saddad
 * 
 * @author sathishkumar.p
 *
 */

public class GetSadadBillStatusScheduler extends DalBaseProcess {
  private ProcessLogger logger = null;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      ProcessContext context = bundle.getContext();
      logger = bundle.getLogger();
      String whereClause = " as inv where inv.efinSadadbillstatus !='5' and efinSadadnewbillnumber is not null and client.id ='"
          + context.getClient() + "'";
      OBQuery<Invoice> invoiceQry = OBDal.getInstance().createQuery(Invoice.class, whereClause);
      List<Invoice> invoiceList = invoiceQry.list();
      logger.logln("GetBillstatus from sadad Background process started.");
      if (invoiceList != null && invoiceList.size() > 0) {
        logger.logln("Totalinvoice count:" + invoiceList.size());
        for (Invoice invoice : invoiceList) {
          logger.logln("Started invoice:" + invoice.getDocumentNo());
          JSONObject response = GetBillStatusFromSadadDAO.getBillStatus(invoice);
          if (response != null) {
            if (ReceiveBillErrorConstant.RESPONSEEMPTY
                .equals(response.get(ReceiveBillErrorConstant.ERROR_MSG))) {
              logger.logln("Status message:"
                  + OBMessageUtils.messageBD(ReceiveBillErrorConstant.RESPONSEEMPTY));
            } else if (ReceiveBillErrorConstant.STATUS_HASERROR
                .equals(ReceiveBillErrorConstant.ERROR_MSG)) {
              logger.logln("Status message:"
                  + OBMessageUtils.messageBD(ReceiveBillErrorConstant.STATUS_HASERROR));
            } else {
              if ("5".equals(invoice.getEfinSadadbillstatus())) {
                if (AddReceiptInOrdertoReceive.addReceiptARInvoice(invoice)) {
                  logger.logln("Status message:"
                      + OBMessageUtils.messageBD(ReceiveBillErrorConstant.STATUS_SUCCESS));
                } else {
                  logger.logln("Status message:"
                      + OBMessageUtils.messageBD(ReceiveBillErrorConstant.STATUS_ADDRECEIPT_FAIL));
                }
              } else {
                logger.logln("Status message:"
                    + OBMessageUtils.messageBD(ReceiveBillErrorConstant.STATUS_SUCCESS));
              }
            }
          } else {
            logger.logln("Status message:" + OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          }
          logger.logln("Ended invoice:" + invoice.getDocumentNo());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.logln("Status message:" + OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}