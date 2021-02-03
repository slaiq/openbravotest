
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for SaddadBillDetail complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SaddadBillDetail"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="BillAmount" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/&gt;
 *         &lt;element name="MOT_SubAccount" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SaddadBillDetail", propOrder = { "billAmount", "motSubAccount" })
public class SaddadBillDetail {

  @XmlElement(name = "BillAmount")
  protected BigDecimal billAmount;
  @XmlElement(name = "MOT_SubAccount")
  protected Integer motSubAccount;

  /**
   * Gets the value of the billAmount property.
   * 
   * @return possible object is {@link BigDecimal }
   * 
   */
  public BigDecimal getBillAmount() {
    return billAmount;
  }

  /**
   * Sets the value of the billAmount property.
   * 
   * @param value
   *          allowed object is {@link BigDecimal }
   * 
   */
  public void setBillAmount(BigDecimal value) {
    this.billAmount = value;
  }

  /**
   * Gets the value of the motSubAccount property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getMOTSubAccount() {
    return motSubAccount;
  }

  /**
   * Sets the value of the motSubAccount property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setMOTSubAccount(Integer value) {
    this.motSubAccount = value;
  }

}
