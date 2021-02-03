
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import java.math.BigDecimal;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for MOT_SubAccountInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MOT_SubAccountInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Amount" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/&gt;
 *         &lt;element name="ApplicationType" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="MOT_SubAccountName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="MOT_SubAccountNo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="MainAccountNo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="MaximumAmount" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/&gt;
 *         &lt;element name="UpdateAccount" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MOT_SubAccountInfo", propOrder = { "amount", "applicationType",
    "motSubAccountName", "motSubAccountNo", "mainAccountNo", "maximumAmount", "updateAccount" })
public class MOTSubAccountInfo {

  @XmlElement(name = "Amount")
  protected BigDecimal amount;
  @XmlElement(name = "ApplicationType")
  protected Integer applicationType;
  @XmlElementRef(name = "MOT_SubAccountName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> motSubAccountName;
  @XmlElement(name = "MOT_SubAccountNo")
  protected Integer motSubAccountNo;
  @XmlElement(name = "MainAccountNo")
  protected Integer mainAccountNo;
  @XmlElement(name = "MaximumAmount")
  protected BigDecimal maximumAmount;
  @XmlElement(name = "UpdateAccount")
  protected Integer updateAccount;

  /**
   * Gets the value of the amount property.
   * 
   * @return possible object is {@link BigDecimal }
   * 
   */
  public BigDecimal getAmount() {
    return amount;
  }

  /**
   * Sets the value of the amount property.
   * 
   * @param value
   *          allowed object is {@link BigDecimal }
   * 
   */
  public void setAmount(BigDecimal value) {
    this.amount = value;
  }

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
   * Gets the value of the motSubAccountName property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getMOTSubAccountName() {
    return motSubAccountName;
  }

  /**
   * Sets the value of the motSubAccountName property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setMOTSubAccountName(JAXBElement<String> value) {
    this.motSubAccountName = value;
  }

  /**
   * Gets the value of the motSubAccountNo property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getMOTSubAccountNo() {
    return motSubAccountNo;
  }

  /**
   * Sets the value of the motSubAccountNo property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setMOTSubAccountNo(Integer value) {
    this.motSubAccountNo = value;
  }

  /**
   * Gets the value of the mainAccountNo property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getMainAccountNo() {
    return mainAccountNo;
  }

  /**
   * Sets the value of the mainAccountNo property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setMainAccountNo(Integer value) {
    this.mainAccountNo = value;
  }

  /**
   * Gets the value of the maximumAmount property.
   * 
   * @return possible object is {@link BigDecimal }
   * 
   */
  public BigDecimal getMaximumAmount() {
    return maximumAmount;
  }

  /**
   * Sets the value of the maximumAmount property.
   * 
   * @param value
   *          allowed object is {@link BigDecimal }
   * 
   */
  public void setMaximumAmount(BigDecimal value) {
    this.maximumAmount = value;
  }

  /**
   * Gets the value of the updateAccount property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getUpdateAccount() {
    return updateAccount;
  }

  /**
   * Sets the value of the updateAccount property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setUpdateAccount(Integer value) {
    this.updateAccount = value;
  }

}
