package sa.elm.ob.finance.ad_process.Sadad;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This servlet class was responsible for Sadad Integration Process
 * 
 * We will send request to Sadad to get bill details from sadad
 * 
 * @author Sathishkumar
 *
 */
public class GetBillStatusFromSadad extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(GetBillStatusFromSadad.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      final String invoiceId = bundle.getParams().get(ReceiveBillErrorConstant.INVOICE_ID)
          .toString();
      log.debug("InvoiceId :" + invoiceId);

      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      if (invoice != null) {
        log.debug("sadadbillnumber:" + invoice.getEfinSadadnewbillnumber());

        if (invoice.getEfinSadadnewbillnumber() == null
            || StringUtils.isEmpty(invoice.getEfinSadadnewbillnumber())) {
          OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
              ReceiveBillErrorConstant.BILLNOTEMPTY);
          bundle.setResult(result);
          return;
        } else if (invoice.getEfinSadadbillstatus() != null
            && "5".equals(invoice.getEfinSadadbillstatus())) {
          OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
              ReceiveBillErrorConstant.STATUS_SUCCESS_MSG);
          bundle.setResult(result);
          return;
        } else {
          log.debug("getting billstatus");
          JSONObject response = GetBillStatusFromSadadDAO.getBillStatus(invoice);
          log.debug("return response:" + response);
          if (response != null) {
            if (ReceiveBillErrorConstant.RESPONSEEMPTY
                .equals(response.get(ReceiveBillErrorConstant.ERROR_MSG))) {
              OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
                  ReceiveBillErrorConstant.RESPONSEEMPTY_MSG);
              bundle.setResult(result);
              return;
            } else if (ReceiveBillErrorConstant.STATUS_HASERROR
                .equals(response.get(ReceiveBillErrorConstant.ERROR_MSG))) {
              OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.WARNING,
                  ReceiveBillErrorConstant.STATUS_HASERRORMSG);
              bundle.setResult(result);
              return;
            } else if (ReceiveBillErrorConstant.SADDADCONNNECTIONFAILED
                .equals(response.get(ReceiveBillErrorConstant.ERROR_MSG))) {
              OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.WARNING,
                  ReceiveBillErrorConstant.SADDADCONNNECTIONFAILED_MSG);
              bundle.setResult(result);
              return;
            } else {
              if ("5".equals(invoice.getEfinSadadbillstatus())) {
                if (AddReceiptInOrdertoReceive.addReceiptARInvoice(invoice)) {
                  OBError result = OBErrorBuilder.buildMessage(null,
                      ReceiveBillErrorConstant.SUCCESS,
                      ReceiveBillErrorConstant.STATUS_SUCCESS_MSG);
                  bundle.setResult(result);
                  return;
                } else {
                  OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
                      ReceiveBillErrorConstant.STATUS_ADDRECEIPT_FAIL_MSG);
                  bundle.setResult(result);
                  return;
                }
              } else {
                OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.WARNING,
                    ReceiveBillErrorConstant.STATUS_SUCCESS_MSG);
                bundle.setResult(result);
                return;
              }
            }
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
                "@HB_INTERNAL_ERROR@");
            bundle.setResult(result);
            return;
          }
        }
      }
    } catch (

    Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      String errorMsg = e.getMessage();
      if (errorMsg.contains(ReceiveBillErrorConstant.INTERNAL_SERVER_ERRORCODE)
          || errorMsg.contains(ReceiveBillErrorConstant.NO_CONNECTION)) {
        OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
            ReceiveBillErrorConstant.SADDADCONNNECTIONFAILED);
        bundle.setResult(result);
        return;
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
            "@HB_INTERNAL_ERROR@");
        bundle.setResult(result);
        return;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
