package sa.elm.ob.utility.sadad.producer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.openbravo.base.session.OBPropertiesProvider;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

import sa.elm.ob.finance.ad_process.Sadad.BillCreationInGRP;

/**
 * MOT Sadad Integration End Point This service will be consumed by Violation Systems in MOT to
 * create the bill
 * 
 * @author mrahim
 *
 */
@Endpoint
public class SadadEndPoint {

  private static final String NAMESPACE_URI = "http://elm.sa/grp/soap";
  private static final String SERVICE_KEY = "grp.bill.service.key";
  private static final String USER_NAME = "grp.bill.service.username";
  private static final String PASSWORD = "grp.bill.service.password";
  private static final String CLIENT_ID = "grp.bill.service.clientid";
  private static final String INVALID_SERVICE_CREDENTIALS = "Invalid Service Credentials";

  /**
   * Creates a Sadad Bill in the GRP.
   * 
   * @param request
   * @return
   */
  @SuppressWarnings("unchecked")
  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "sadadBillRequest")
  @ResponsePayload
  public SadadBillResponse createBill(@RequestPayload SadadBillRequest request,
      @SoapHeader(value = "{" + NAMESPACE_URI + "}"
          + "serviceAccount") SoapHeaderElement soapHeaderElement)
      throws Exception {
    SadadBillResponse sadadBillResponse = new SadadBillResponse();
    // create an unmarshaller
    JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    // unmarshal the header from the specified source
    JAXBElement<ServiceAccount> headers = (JAXBElement<ServiceAccount>) unmarshaller
        .unmarshal(soapHeaderElement.getSource());
    // get the header values
    ServiceAccount serviceAccount = headers.getValue();
    // Validate the Service Credentials
    if (!validateServiceCredentials(serviceAccount)) {
      ObjectFactory objectFactory = new ObjectFactory();
      sadadBillResponse.setHasError(true);
      sadadBillResponse.setErrorMessage(
          objectFactory.createSadadBillResponseErrorMessage(INVALID_SERVICE_CREDENTIALS));
      return sadadBillResponse;

    }
    sadadBillResponse = BillCreationInGRP.billCreation(request, getProperty(CLIENT_ID));

    return sadadBillResponse;
  }

  /**
   * Validate the Web Service Credentials
   * 
   * @param serviceAccount
   * @return
   */
  private Boolean validateServiceCredentials(ServiceAccount serviceAccount) {

    Boolean isCredentialValid = true;

    String clientId = (null == serviceAccount.getServiceKey()) ? ""
        : serviceAccount.getServiceKey().getValue();
    String userName = (null == serviceAccount.getUserName()) ? ""
        : serviceAccount.getUserName().getValue();
    String password = (null == serviceAccount.getPassword()) ? ""
        : serviceAccount.getPassword().getValue();

    if (!clientId.trim().equals(getProperty(SERVICE_KEY))
        || !userName.equals(getProperty(USER_NAME)) || !password.equals(getProperty(PASSWORD))) {
      isCredentialValid = false;
    }

    return isCredentialValid;

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

}
