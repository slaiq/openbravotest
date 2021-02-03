
package sa.elm.ob.utility.sadad.consumer.org.tempuri;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
 *         &lt;element name="BillNo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="DeleteReasons" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="UserNo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "billNo", "deleteReasons", "userNo" })
@XmlRootElement(name = "DeleteSaddadBillRequest")
public class DeleteSaddadBillRequest {

  @XmlElement(name = "BillNo")
  protected Integer billNo;
  @XmlElementRef(name = "DeleteReasons", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
  protected JAXBElement<String> deleteReasons;
  @XmlElement(name = "UserNo")
  protected Integer userNo;

  /**
   * Gets the value of the billNo property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getBillNo() {
    return billNo;
  }

  /**
   * Sets the value of the billNo property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setBillNo(Integer value) {
    this.billNo = value;
  }

  /**
   * Gets the value of the deleteReasons property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getDeleteReasons() {
    return deleteReasons;
  }

  /**
   * Sets the value of the deleteReasons property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setDeleteReasons(JAXBElement<String> value) {
    this.deleteReasons = value;
  }

  /**
   * Gets the value of the userNo property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getUserNo() {
    return userNo;
  }

  /**
   * Sets the value of the userNo property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setUserNo(Integer value) {
    this.userNo = value;
  }

}
