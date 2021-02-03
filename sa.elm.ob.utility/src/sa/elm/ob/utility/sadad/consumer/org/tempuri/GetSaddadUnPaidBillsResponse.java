
package sa.elm.ob.utility.sadad.consumer.org.tempuri;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfSaddadBillInfo;

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
 *         &lt;element name="ErrorMessage" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="HasError" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="ResultCount" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="SaddadUnPaidBills" type="{http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI}ArrayOfSaddadBillInfo" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "errorMessage", "hasError", "resultCount", "saddadUnPaidBills" })
@XmlRootElement(name = "GetSaddadUnPaidBillsResponse")
public class GetSaddadUnPaidBillsResponse {

  @XmlElementRef(name = "ErrorMessage", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
  protected JAXBElement<String> errorMessage;
  @XmlElement(name = "HasError")
  protected Boolean hasError;
  @XmlElement(name = "ResultCount")
  protected Integer resultCount;
  @XmlElementRef(name = "SaddadUnPaidBills", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
  protected JAXBElement<ArrayOfSaddadBillInfo> saddadUnPaidBills;

  /**
   * Gets the value of the errorMessage property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getErrorMessage() {
    return errorMessage;
  }

  /**
   * Sets the value of the errorMessage property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setErrorMessage(JAXBElement<String> value) {
    this.errorMessage = value;
  }

  /**
   * Gets the value of the hasError property.
   * 
   * @return possible object is {@link Boolean }
   * 
   */
  public Boolean isHasError() {
    return hasError;
  }

  /**
   * Sets the value of the hasError property.
   * 
   * @param value
   *          allowed object is {@link Boolean }
   * 
   */
  public void setHasError(Boolean value) {
    this.hasError = value;
  }

  /**
   * Gets the value of the resultCount property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getResultCount() {
    return resultCount;
  }

  /**
   * Sets the value of the resultCount property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setResultCount(Integer value) {
    this.resultCount = value;
  }

  /**
   * Gets the value of the saddadUnPaidBills property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link ArrayOfSaddadBillInfo }{@code >}
   * 
   */
  public JAXBElement<ArrayOfSaddadBillInfo> getSaddadUnPaidBills() {
    return saddadUnPaidBills;
  }

  /**
   * Sets the value of the saddadUnPaidBills property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link ArrayOfSaddadBillInfo }{@code >}
   * 
   */
  public void setSaddadUnPaidBills(JAXBElement<ArrayOfSaddadBillInfo> value) {
    this.saddadUnPaidBills = value;
  }

}
