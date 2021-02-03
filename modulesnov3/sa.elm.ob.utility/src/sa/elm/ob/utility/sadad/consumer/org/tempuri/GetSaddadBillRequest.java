
package sa.elm.ob.utility.sadad.consumer.org.tempuri;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "billNo" })
@XmlRootElement(name = "GetSaddadBillRequest")
public class GetSaddadBillRequest {

  @XmlElement(name = "BillNo")
  protected Integer billNo;

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

}
