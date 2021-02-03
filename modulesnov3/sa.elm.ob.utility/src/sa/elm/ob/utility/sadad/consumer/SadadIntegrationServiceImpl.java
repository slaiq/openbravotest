package sa.elm.ob.utility.sadad.consumer;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.openbravo.base.session.OBPropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ServiceAccount;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.AddSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.AddSaddadBillResponse;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.DeleteSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.DeleteSaddadBillResponse;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.GetSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.GetSaddadBillResponse;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.ISaddadLibrary;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.SaddadLibrary;;

/**
 * 
 * @author mrahim
 *
 */
public class SadadIntegrationServiceImpl implements SadadIntegrationService {
  private static final Logger log = LoggerFactory.getLogger(SadadIntegrationServiceImpl.class);

  private static String SERVICE_KEY = "sadad.service.key";
  private static String SERVICE_USER_NAME = "sadad.service.user";
  private static String SERVICE_USER_PASSWORD = "sadad.service.password";
  private final static QName _ServiceAccountPassword_QNAME = new QName(
      "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.ServiceCore", "Password");
  private final static QName _ServiceAccountServiceKey_QNAME = new QName(
      "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.ServiceCore", "ServiceKey");
  private final static QName _ServiceAccountUserName_QNAME = new QName(
      "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.ServiceCore", "UserName");

  @Override
  public AddSaddadBillResponse createNewBill(AddSaddadBillRequest saddadBillRequest) {

    ServiceAccount serviceAccount = getServiceCredentials();
    log.debug("Sadad Request [Add Bill:] " + printRequestLogs(saddadBillRequest));
    AddSaddadBillResponse saddadBillResponse = getSadadService().addNewSaddadBill(saddadBillRequest,
        serviceAccount);
    log.debug("Sadad Response [Add Bill:] " + printResponeLogs(saddadBillResponse));
    return saddadBillResponse;
  }

  @Override
  public GetSaddadBillResponse getSadadBillStatus(GetSaddadBillRequest saddadBillRequest) {
    ServiceAccount serviceAccount = getServiceCredentials();

    GetSaddadBillResponse saddadBillResponse = getSadadService().getSaddadBill(saddadBillRequest,
        serviceAccount);

    return saddadBillResponse;
  }

  @Override
  public DeleteSaddadBillResponse deleteBill(DeleteSaddadBillRequest deleteSaddadBillRequest) {

    ServiceAccount serviceAccount = getServiceCredentials();

    DeleteSaddadBillResponse saddadBillResponse = getSadadService()
        .deleteSaddadBill(deleteSaddadBillRequest, serviceAccount);

    return saddadBillResponse;
  }

  /**
   * Get Property value from property file
   * 
   * @param key
   * @return
   */
  private String getProperty(String key) {

    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(key);
  }

  // /**
  // * Get the service account credentials
  // *
  // * @return
  // */
  // private ServiceAccount getServiceCredentials() {
  //
  // ObjectFactory objectFactory = new ObjectFactory();
  // ServiceAccount serviceAccount = objectFactory.createServiceAccount();
  // serviceAccount
  // .setServiceKey(objectFactory.createServiceAccountServiceKey(getProperty(SERVICE_KEY)));
  // serviceAccount
  // .setUserName(objectFactory.createServiceAccountUserName(getProperty(SERVICE_USER_NAME)));
  // serviceAccount.setPassword(
  // objectFactory.createServiceAccountPassword(getProperty(SERVICE_USER_PASSWORD)));
  //
  // return serviceAccount;
  // }

  /**
   * Get the service account credentials
   * 
   * @return
   */
  private ServiceAccount getServiceCredentials() {

    ServiceAccount serviceAccount = new ServiceAccount();

    serviceAccount.setServiceKey(new JAXBElement<String>(_ServiceAccountServiceKey_QNAME,
        String.class, getProperty(SERVICE_KEY)));
    serviceAccount.setUserName(new JAXBElement<String>(_ServiceAccountUserName_QNAME, String.class,
        getProperty(SERVICE_USER_NAME)));
    serviceAccount.setPassword(new JAXBElement<String>(_ServiceAccountPassword_QNAME, String.class,
        getProperty(SERVICE_USER_PASSWORD)));

    return serviceAccount;
  }

  /**
   * Get the Library Interface
   * 
   * @return
   */
  private ISaddadLibrary getSadadService() {

    SaddadLibrary saddadLibrary = new SaddadLibrary();
    ISaddadLibrary iSaddadLibrary = saddadLibrary.getBasicHttpBindingISaddadLibrary();

    return iSaddadLibrary;

  }

  private String printRequestLogs(AddSaddadBillRequest addSaddadBillRequest) {
    String str = "";
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      JAXBContext ctx = JAXBContext.newInstance(AddSaddadBillRequest.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(addSaddadBillRequest, baos);
      str = baos.toString();
    } catch (Exception e) {
      e.printStackTrace();
      // catch exception
    }

    return str;

  }

  private String printResponeLogs(AddSaddadBillResponse addSaddadBillResponse) {
    String str = "";
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      JAXBContext ctx = JAXBContext.newInstance(AddSaddadBillResponse.class);
      Marshaller marshaller = ctx.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(addSaddadBillResponse, baos);
      str = baos.toString();
    } catch (Exception e) {
      e.printStackTrace();
      // catch exception
    }

    return str;

  }

}
