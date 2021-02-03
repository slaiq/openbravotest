package sa.elm.ob.finance.ad_process.Sadad;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jfree.util.Log;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.sadad.consumer.SadadIntegrationService;
import sa.elm.ob.utility.sadad.consumer.SadadIntegrationServiceImpl;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.SaddadBillInfo;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.GetSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.GetSaddadBillResponse;

public class GetBillStatusFromSadadDAO {
  private static final Logger log = LoggerFactory.getLogger(GetBillStatusFromSadadDAO.class);

  /**
   * This method is to get the bill status of particular invoice from sadad
   * 
   * @param invoice
   * @return JSONObject
   */
  public static JSONObject getBillStatus(Invoice invoice) {
    JSONObject responseJson = new JSONObject();

    if (!"5".equals(invoice.getEfinSadadbillstatus())) {

      int billNumber = Integer.parseInt(invoice.getEfinSadadnewbillnumber());
      GetSaddadBillRequest request = new GetSaddadBillRequest();
      GetSaddadBillResponse response = null;
      request.setBillNo(billNumber);
      log.debug("get request:" + request);

      // Call to sadad get the billstatus
      SadadIntegrationService service = new SadadIntegrationServiceImpl();
      response = service.getSadadBillStatus(request);
      log.debug("get response:" + response);

      try {
        if (response == null) {
          responseJson.put(ReceiveBillErrorConstant.ERROR_MSG,
              ReceiveBillErrorConstant.RESPONSEEMPTY);
        } else {
          if (setValueForFields(response, invoice)) {
            log.debug(ReceiveBillErrorConstant.ERROR_MSG + ReceiveBillErrorConstant.STATUS_SUCCESS);
            responseJson.put(ReceiveBillErrorConstant.ERROR_MSG,
                ReceiveBillErrorConstant.STATUS_SUCCESS);
          } else {
            log.debug(
                ReceiveBillErrorConstant.ERROR_MSG + ReceiveBillErrorConstant.STATUS_HASERROR);
            responseJson.put(ReceiveBillErrorConstant.ERROR_MSG,
                ReceiveBillErrorConstant.STATUS_HASERROR);
          }

        }
        return responseJson;
      } catch (Exception e) {
        Log.debug("Error while getting status");
        OBDal.getInstance().rollbackAndClose();
        e.printStackTrace();
        return null;
      }
    } else {
      try {
        return responseJson.put(ReceiveBillErrorConstant.ERROR_MSG,
            ReceiveBillErrorConstant.STATUS_SUCCESS);
      } catch (JSONException e) {
        Log.debug("Error while getting status");
        OBDal.getInstance().rollbackAndClose();
        e.printStackTrace();
        return null;
      }
    }

  }

  /**
   * This method is used to set value for corresponding fields using response from saddad
   * 
   * @param response
   * @param invoice
   * 
   * @return true if there is no error , else false
   * 
   */
  private static boolean setValueForFields(GetSaddadBillResponse response, Invoice invoice) {
    if (response.isHasError()) {
      log.debug("haserror=true");
      log.debug("errormsg" + response.getErrorMessage().getValue());

      SaddadBillInfo billInfo = response.getSaddadBillInfo().getValue();
      invoice.setEfinSadadhaserror(true);
      invoice.setEfinSadaderrormessage(response.getErrorMessage().getValue());
      invoice.setEfinSadaddeletionstatus(null);
      if (billInfo != null) {
        log.debug("status" + billInfo.getBillStatus().getValue());
        if ("Else".equals(billInfo.getBillStatus().getValue())) {
          invoice.setEfinSadadbillstatus("ELSE");
        } else {
          invoice.setEfinSadadbillstatus(billInfo.getBillStatus().getValue());
        }
      }
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      return false;
    } else {
      SaddadBillInfo billInfo = response.getSaddadBillInfo().getValue();
      log.debug("haserror=false");
      log.debug("errors=null");
      invoice.setEfinSadadhaserror(false);
      invoice.setEfinSadaderrormessage(null);
      invoice.setEfinSadaddeletionstatus(null);
      if (billInfo != null) {
        log.debug("status" + billInfo.getBillStatus().getValue());
        if ("Else".equals(billInfo.getBillStatus().getValue())) {
          invoice.setEfinSadadbillstatus("ELSE");
        } else {
          invoice.setEfinSadadbillstatus(billInfo.getBillStatus().getValue());
        }
      }
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      return true;
    }
  }

}
