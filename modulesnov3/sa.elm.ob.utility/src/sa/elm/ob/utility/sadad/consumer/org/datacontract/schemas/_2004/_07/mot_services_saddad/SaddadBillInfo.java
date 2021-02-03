
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for SaddadBillInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SaddadBillInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="BillAmount" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="BillNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="BillStatus" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CreationDate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CustomerName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SaddadBillInfo", propOrder = { "billAmount", "billNumber", "billStatus",
    "creationDate", "customerName" })
public class SaddadBillInfo {

  @XmlElementRef(name = "BillAmount", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> billAmount;
  @XmlElementRef(name = "BillNumber", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> billNumber;
  @XmlElementRef(name = "BillStatus", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> billStatus;
  @XmlElementRef(name = "CreationDate", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> creationDate;
  @XmlElementRef(name = "CustomerName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> customerName;

  /**
   * Gets the value of the billAmount property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getBillAmount() {
    return billAmount;
  }

  /**
   * Sets the value of the billAmount property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setBillAmount(JAXBElement<String> value) {
    this.billAmount = value;
  }

  /**
   * Gets the value of the billNumber property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getBillNumber() {
    return billNumber;
  }

  /**
   * Sets the value of the billNumber property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setBillNumber(JAXBElement<String> value) {
    this.billNumber = value;
  }

  /**
   * Gets the value of the billStatus property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getBillStatus() {
    return billStatus;
  }

  /**
   * Sets the value of the billStatus property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setBillStatus(JAXBElement<String> value) {
    this.billStatus = value;
  }

  /**
   * Gets the value of the creationDate property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getCreationDate() {
    return creationDate;
  }

  /**
   * Sets the value of the creationDate property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setCreationDate(JAXBElement<String> value) {
    this.creationDate = value;
  }

  /**
   * Gets the value of the customerName property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getCustomerName() {
    return customerName;
  }

  /**
   * Sets the value of the customerName property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setCustomerName(JAXBElement<String> value) {
    this.customerName = value;
  }

}
