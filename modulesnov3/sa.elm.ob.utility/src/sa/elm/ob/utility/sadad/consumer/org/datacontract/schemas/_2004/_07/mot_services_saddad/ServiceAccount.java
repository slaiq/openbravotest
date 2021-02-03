
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ServiceAccount complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ServiceAccount"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ServiceKey" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="UserName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceAccount", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.ServiceCore", propOrder = {
    "password", "serviceKey", "userName" })
public class ServiceAccount {

  @XmlElementRef(name = "Password", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.ServiceCore", type = JAXBElement.class, required = false)
  protected JAXBElement<String> password;
  @XmlElementRef(name = "ServiceKey", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.ServiceCore", type = JAXBElement.class, required = false)
  protected JAXBElement<String> serviceKey;
  @XmlElementRef(name = "UserName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.ServiceCore", type = JAXBElement.class, required = false)
  protected JAXBElement<String> userName;

  /**
   * Gets the value of the password property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getPassword() {
    return password;
  }

  /**
   * Sets the value of the password property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setPassword(JAXBElement<String> value) {
    this.password = value;
  }

  /**
   * Gets the value of the serviceKey property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getServiceKey() {
    return serviceKey;
  }

  /**
   * Sets the value of the serviceKey property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setServiceKey(JAXBElement<String> value) {
    this.serviceKey = value;
  }

  /**
   * Gets the value of the userName property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getUserName() {
    return userName;
  }

  /**
   * Sets the value of the userName property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setUserName(JAXBElement<String> value) {
    this.userName = value;
  }

}
