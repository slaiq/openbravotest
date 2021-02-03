
package sa.elm.ob.utility.sadad.consumer.org.tempuri;

import java.math.BigDecimal;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfSaddadBillDetail;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ApplicationType" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="CustomerName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="CustomerType" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="IDNo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="IDType" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="MainAccount" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="MobileNo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="Notes" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="SaddadBillDetails" type="{http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI}ArrayOfSaddadBillDetail" minOccurs="0"/&gt;
 *         &lt;element name="TotalBillAmount" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "applicationType", "customerName", "customerType", "idNo",
    "idType", "mainAccount", "mobileNo", "notes", "saddadBillDetails", "totalBillAmount" })
@XmlRootElement(name = "AddSaddadBillRequest")
public class AddSaddadBillRequest {

  @XmlElement(name = "ApplicationType")
  protected Integer applicationType;
  @XmlElementRef(name = "CustomerName", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
  protected JAXBElement<String> customerName;
  @XmlElement(name = "CustomerType")
  protected Integer customerType;
  @XmlElementRef(name = "IDNo", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
  protected JAXBElement<String> idNo;
  @XmlElement(name = "IDType")
  protected Integer idType;
  @XmlElement(name = "MainAccount")
  protected Integer mainAccount;
  @XmlElementRef(name = "MobileNo", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
  protected JAXBElement<String> mobileNo;
  @XmlElementRef(name = "Notes", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
  protected JAXBElement<String> notes;
  @XmlElementRef(name = "SaddadBillDetails", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
  protected JAXBElement<ArrayOfSaddadBillDetail> saddadBillDetails;
  @XmlElement(name = "TotalBillAmount")
  protected BigDecimal totalBillAmount;

  /**
   * Gets the value of the applicationType property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getApplicationType() {
    return applicationType;
  }

  /**
   * Sets the value of the applicationType property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setApplicationType(Integer value) {
    this.applicationType = value;
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

  /**
   * Gets the value of the customerType property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getCustomerType() {
    return customerType;
  }

  /**
   * Sets the value of the customerType property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setCustomerType(Integer value) {
    this.customerType = value;
  }

  /**
   * Gets the value of the idNo property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getIDNo() {
    return idNo;
  }

  /**
   * Sets the value of the idNo property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setIDNo(JAXBElement<String> value) {
    this.idNo = value;
  }

  /**
   * Gets the value of the idType property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getIDType() {
    return idType;
  }

  /**
   * Sets the value of the idType property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setIDType(Integer value) {
    this.idType = value;
  }

  /**
   * Gets the value of the mainAccount property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getMainAccount() {
    return mainAccount;
  }

  /**
   * Sets the value of the mainAccount property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setMainAccount(Integer value) {
    this.mainAccount = value;
  }

  /**
   * Gets the value of the mobileNo property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getMobileNo() {
    return mobileNo;
  }

  /**
   * Sets the value of the mobileNo property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setMobileNo(JAXBElement<String> value) {
    this.mobileNo = value;
  }

  /**
   * Gets the value of the notes property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getNotes() {
    return notes;
  }

  /**
   * Sets the value of the notes property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setNotes(JAXBElement<String> value) {
    this.notes = value;
  }

  /**
   * Gets the value of the saddadBillDetails property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link ArrayOfSaddadBillDetail
   *         }{@code >}
   * 
   */
  public JAXBElement<ArrayOfSaddadBillDetail> getSaddadBillDetails() {
    return saddadBillDetails;
  }

  /**
   * Sets the value of the saddadBillDetails property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link ArrayOfSaddadBillDetail
   *          }{@code >}
   * 
   */
  public void setSaddadBillDetails(JAXBElement<ArrayOfSaddadBillDetail> value) {
    this.saddadBillDetails = value;
  }

  /**
   * Gets the value of the totalBillAmount property.
   * 
   * @return possible object is {@link BigDecimal }
   * 
   */
  public BigDecimal getTotalBillAmount() {
    return totalBillAmount;
  }

  /**
   * Sets the value of the totalBillAmount property.
   * 
   * @param value
   *          allowed object is {@link BigDecimal }
   * 
   */
  public void setTotalBillAmount(BigDecimal value) {
    this.totalBillAmount = value;
  }

}
