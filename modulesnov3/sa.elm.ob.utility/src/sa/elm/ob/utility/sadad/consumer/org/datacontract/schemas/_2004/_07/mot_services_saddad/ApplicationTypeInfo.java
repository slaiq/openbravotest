
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ApplicationTypeInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ApplicationTypeInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ApplicationTypeCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ApplicationTypeName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ApplicationTypeInfo", propOrder = { "applicationTypeCode", "applicationTypeName" })
public class ApplicationTypeInfo {

  @XmlElementRef(name = "ApplicationTypeCode", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> applicationTypeCode;
  @XmlElementRef(name = "ApplicationTypeName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> applicationTypeName;

  /**
   * Gets the value of the applicationTypeCode property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getApplicationTypeCode() {
    return applicationTypeCode;
  }

  /**
   * Sets the value of the applicationTypeCode property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setApplicationTypeCode(JAXBElement<String> value) {
    this.applicationTypeCode = value;
  }

  /**
   * Gets the value of the applicationTypeName property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getApplicationTypeName() {
    return applicationTypeName;
  }

  /**
   * Sets the value of the applicationTypeName property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setApplicationTypeName(JAXBElement<String> value) {
    this.applicationTypeName = value;
  }

}
