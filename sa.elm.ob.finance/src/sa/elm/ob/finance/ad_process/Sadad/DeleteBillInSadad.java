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
import sa.elm.ob.utility.sadad.consumer.org.tempuri.DeleteSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.DeleteSaddadBillResponse;

/**
 * This Class was responsible for deleting bill in Sadad(Sadad Integration Process)
 * 
 * @author Sathishkumar
 *
 */

public class DeleteBillInSadad extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(DeleteBillInSadad.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      String errorMsg = "", status = "";
      boolean isValid = true;

      DeleteSaddadBillRequest deleteRequest = new DeleteSaddadBillRequest();
      DeleteSaddadBillResponse deleteResponse = null;

      final String invoiceId = bundle.getParams().get(ReceiveBillErrorConstant.INVOICE_ID)
          .toString();
      log.debug(ReceiveBillErrorConstant.WORD_SEPEARATOR + " " + invoiceId);

      if (StringUtils.isNotEmpty(invoiceId)) {
        Invoice arINvoice = OBDal.getInstance().get(Invoice.class, invoiceId);

        if (arINvoice != null) {
          errorMsg = DeleteBillValidation.preValidation(arINvoice);
          log.debug("errorMsg:" + errorMsg);

          if (StringUtils.isNotEmpty(errorMsg)) {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
                errorMsg);
            bundle.setResult(result);
            return;
          }

          deleteRequest = DeleteBillValidation.deleteSadadBillRequest(arINvoice);
          log.debug("deleteRequest:" + deleteRequest);

          // Call to sadad delete bill
          SadadIntegrationService service = new SadadIntegrationServiceImpl();
          deleteResponse = service.deleteBill(deleteRequest);
          log.debug("deleteResponse:" + deleteResponse);

          // set response from Sadad delete webservice to corresponding fields
          if (deleteResponse != null) {
            isValid = DeleteBillValidation.setFieldValue(deleteResponse, arINvoice);
            status = deleteResponse.getOperationStatus().value();
            log.debug("isValid:" + isValid);
            log.debug("status:" + status);
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
                ReceiveBillErrorConstant.ADDBILLFAILED);
            bundle.setResult(result);
            return;
          }

          if (isValid && ReceiveBillErrorConstant.DELETION_SUCCESS.equals(status)) {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.SUCCESS,
                ReceiveBillErrorConstant.EFIN_DELETESUCCESS);
            bundle.setResult(result);
            return;
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.WARNING,
                "@Efin_Addedbill_witherror@");
            bundle.setResult(result);
            return;
          }

        }

      }

    } catch (Exception e) {
      String errorMsg = e.getMessage();
      if (errorMsg.contains(ReceiveBillErrorConstant.INTERNAL_SERVER_ERRORCODE)
          || errorMsg.contains(ReceiveBillErrorConstant.NO_CONNECTION)) {
        OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
            ReceiveBillErrorConstant.SADDADCONNNECTIONFAILED);
        bundle.setResult(result);
        return;
      }
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      OBError result = OBErrorBuilder.buildMessage(null, ReceiveBillErrorConstant.ERROR,
          "@HB_INTERNAL_ERROR@");
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}