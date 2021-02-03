package sa.elm.ob.finance.ad_process.Sadad;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.sadad.consumer.SadadIntegrationService;
import sa.elm.ob.utility.sadad.consumer.SadadIntegrationServiceImpl;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.AddSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.AddSaddadBillResponse;

/**
 * 
 * This servlet class was responsible for Sadad Integration Process
 * 
 * We will send request to Sadad to add bill in Sadad
 * 
 * @author Sathishkumar
 *
 */

public class AddBillInSadad extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(AddBillInSadad.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      final String invoiceId = bundle.getParams().get(ReceiveBillErrorConstant.INVOICE_ID)
          .toString();
      String errorMsg = "";
      boolean isValid = true;

      AddSaddadBillRequest billRequest = new AddSaddadBillRequest();
      AddSaddadBillResponse billResponse = new AddSaddadBillResponse();

      log.debug(ReceiveBillErrorConstant.INVOICE_ID + ":" + invoiceId);

      if (StringUtils.isNotEmpty(invoiceId)) {
        Invoice arINvoice = OBDal.getInstance().get(Invoice.class, invoiceId);

        if (arINvoice != null) {
          log.debug("--Validation Block--");

          errorMsg = AddSadadBillValidation.validateFields(arINvoice);
          if (StringUtils.isNotEmpty(errorMsg)) {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
                errorMsg);
            bundle.setResult(result);
            return;
          }

          billRequest = AddSadadBillValidation.createSadadBillRequest(arINvoice);

          log.debug("--Calltosadad Block start--");

          // Call to sadad create bill webservice
          SadadIntegrationService service = new SadadIntegrationServiceImpl();
          billResponse = service.createNewBill(billRequest);

          log.debug("billResponse:" + billResponse);

          // set response from Saddad to corresponding fields
          if (billResponse != null) {
            isValid = AddSadadBillValidation.setFieldValue(billResponse, arINvoice);
            log.debug("valid:" + isValid);
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
                ReceiveBillErrorConstant.ADDBILLFAILED);
            bundle.setResult(result);
            return;
          }

          if (isValid) {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.SUCCESS,
                ReceiveBillErrorConstant.ADDBILLSUCCESS);
            bundle.setResult(result);
            return;
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.WARNING,
                ReceiveBillErrorConstant.ADDBILLERROR);
            bundle.setResult(result);
            return;
          }

        }

      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      String errorMsg = e.getMessage();
      if (errorMsg.contains(ReceiveBillErrorConstant.INTERNAL_SERVER_ERRORCODE)
          || errorMsg.contains(ReceiveBillErrorConstant.NO_CONNECTION)) {
        OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
            ReceiveBillErrorConstant.SADDADCONNNECTIONFAILED);
        bundle.setResult(result);
        return;
      }
      OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
          ReceiveBillErrorConstant.ADDBILLFAILED);
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}