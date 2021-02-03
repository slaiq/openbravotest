
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
 *         &lt;element name="MainAccountNo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "mainAccountNo" })
@XmlRootElement(name = "GetMainAccountByIdRequest")
public class GetMainAccountByIdRequest {

  @XmlElement(name = "MainAccountNo")
  protected Integer mainAccountNo;

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

}
