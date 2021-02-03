package sa.elm.ob.utility.dms.util;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;

import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ServiceAccount;

public class GetServiceAccount {

  private static String SERVICE_KEY = "DMS.service.key";
  private static String SERVICE_USER_NAME = "DMS.service.user";
  private static String SERVICE_USER_PASSWORD = "DMS.service.password";

  private final static QName _ServiceAccountPassword_QNAME = new QName(
      "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.ServiceCore", "Password");
  private final static QName _ServiceAccountServiceKey_QNAME = new QName(
      "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.ServiceCore", "ServiceKey");
  private final static QName _ServiceAccountUserName_QNAME = new QName(
      "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.ServiceCore", "UserName");

  /**
   * Get the service account credentials for DMS
   * 
   * @return {@link ServiceAccount}
   */
  public static ServiceAccount getServiceCredentials() {
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
   * Get Property value from property file
   * 
   * @param key
   * @return
   */

  public static String getProperty(String key) {
    if (key.equals(DMSConstants.DMS_CLIENT_ID)) {
      return OBContext.getOBContext().getUser().getUsername();
    }
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty(key);
  }

}
