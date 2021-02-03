
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for CustomerTypeInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CustomerTypeInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="CustomerTypeCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CustomerTypeName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CustomerTypeInfo", propOrder = { "customerTypeCode", "customerTypeName" })
public class CustomerTypeInfo {

  @XmlElementRef(name = "CustomerTypeCode", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> customerTypeCode;
  @XmlElementRef(name = "CustomerTypeName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> customerTypeName;

  /**
   * Gets the value of the customerTypeCode property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getCustomerTypeCode() {
    return customerTypeCode;
  }

  /**
   * Sets the value of the customerTypeCode property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setCustomerTypeCode(JAXBElement<String> value) {
    this.customerTypeCode = value;
  }

  /**
   * Gets the value of the customerTypeName property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getCustomerTypeName() {
    return customerTypeName;
  }

  /**
   * Sets the value of the customerTypeName property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setCustomerTypeName(JAXBElement<String> value) {
    this.customerTypeName = value;
  }

}
