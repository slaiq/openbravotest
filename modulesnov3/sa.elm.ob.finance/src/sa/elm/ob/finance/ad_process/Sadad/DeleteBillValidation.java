
package sa.elm.ob.finance.ad_process.Sadad;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.sadad.consumer.org.tempuri.DeleteSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.DeleteSaddadBillResponse;

/**
 * 
 * @author Sathishkumar
 *
 */

public class DeleteBillValidation {

  /**
   * This method is used to do prevalidations
   * 
   * @param arINvoice
   * @return ErrorMsg if some values are not valid else null
   */

  private final static QName _SadadDeleteRequest_delete = new QName("http://elm.sa/grp/soap",
      "deletereason");

  private static final Logger log = LoggerFactory.getLogger(DeleteBillValidation.class);

  public static String preValidation(Invoice arINvoice) {

    log.debug("prevalidation start");
    if (StringUtils.isEmpty(arINvoice.getEfinSadadnewbillnumber())) {
      return ReceiveBillErrorConstant.EFIN_DELETENOBILL;
    }

    try {
      Integer.parseInt(arINvoice.getEfinIdno());
    } catch (NumberFormatException e) {
      return ReceiveBillErrorConstant.EFIN_IDSHOULDBENO;
    }
    log.debug("prevalidation end");
    return "";

  }

  /**
   * This method is use to create AddSaddadBillRequest from selected invoice
   * 
   * @param Invoice
   * @return AddSaddadBillRequest
   */

  public static DeleteSaddadBillRequest deleteSadadBillRequest(Invoice arINvoice) {

    log.debug("--DeleteSaddadBillRequest Block--");

    DeleteSaddadBillRequest request = new DeleteSaddadBillRequest();
    request.setBillNo(Integer.parseInt(arINvoice.getEfinSadadnewbillnumber()));
    request.setDeleteReasons(new JAXBElement<String>(_SadadDeleteRequest_delete, String.class,
        arINvoice.getEfinDeletereason()));
    request.setUserNo(Integer.parseInt(arINvoice.getEfinIdno()));

    log.debug("--DeleteSaddadBillRequest Block end--");
    log.debug("request:" + request);

    return request;
  }

  /**
   * This method is used to set fields from response
   * 
   * @param request(AddSaddadBillResponse)
   * 
   * @return true or false
   */

  public static boolean setFieldValue(DeleteSaddadBillResponse response, Invoice invoice) {

    if (response.isHasError()) {
      log.debug("haserror =true");
      log.debug("error msg:" + response.getErrorMessage().getValue());
      log.debug("deletestatus:" + response.getOperationStatus().value());

      invoice.setEfinSadadhaserror(true);
      invoice.setEfinSadaderrormessage(response.getErrorMessage().getValue());
      if (ReceiveBillErrorConstant.DELETION_SUCCESS.equals(response.getOperationStatus().value())) {
        invoice.setEfinSadaddeletionstatus(ReceiveBillErrorConstant.EFIN_DELETION_SUCCESSTATUS);
      } else {
        invoice.setEfinSadaddeletionstatus(ReceiveBillErrorConstant.EFIN_DELETION_FAILSTATUS);
      }
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      return false;
    } else {
      log.debug("haserror =false");
      log.debug("error msg:" + response.getErrorMessage().getValue());
      log.debug("deletestatus:" + response.getOperationStatus().value());
      invoice.setEfinSadadhaserror(false);
      invoice.setEfinSadaderrormessage(null);
      if (ReceiveBillErrorConstant.DELETION_SUCCESS.equals(response.getOperationStatus().value())) {
        invoice.setEfinMarkfordeletion(true);
        invoice.setEfinSadaddeletionstatus(ReceiveBillErrorConstant.EFIN_DELETION_SUCCESSTATUS);
      } else {
        invoice.setEfinSadaddeletionstatus(ReceiveBillErrorConstant.EFIN_DELETION_FAILSTATUS);
      }
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
    }

    return true;
  }

}
