
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for MainAccountInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MainAccountInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DepartmentAuthorizationCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="DepartmentBenefitCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="MainAccountName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="MainAccountNo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *         &lt;element name="OldMainAccountNo" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MainAccountInfo", propOrder = { "departmentAuthorizationCode",
    "departmentBenefitCode", "mainAccountName", "mainAccountNo", "oldMainAccountNo" })
public class MainAccountInfo {

  @XmlElementRef(name = "DepartmentAuthorizationCode", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> departmentAuthorizationCode;
  @XmlElementRef(name = "DepartmentBenefitCode", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> departmentBenefitCode;
  @XmlElementRef(name = "MainAccountName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> mainAccountName;
  @XmlElement(name = "MainAccountNo")
  protected Integer mainAccountNo;
  @XmlElement(name = "OldMainAccountNo")
  protected Integer oldMainAccountNo;

  /**
   * Gets the value of the departmentAuthorizationCode property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getDepartmentAuthorizationCode() {
    return departmentAuthorizationCode;
  }

  /**
   * Sets the value of the departmentAuthorizationCode property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setDepartmentAuthorizationCode(JAXBElement<String> value) {
    this.departmentAuthorizationCode = value;
  }

  /**
   * Gets the value of the departmentBenefitCode property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getDepartmentBenefitCode() {
    return departmentBenefitCode;
  }

  /**
   * Sets the value of the departmentBenefitCode property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setDepartmentBenefitCode(JAXBElement<String> value) {
    this.departmentBenefitCode = value;
  }

  /**
   * Gets the value of the mainAccountName property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getMainAccountName() {
    return mainAccountName;
  }

  /**
   * Sets the value of the mainAccountName property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setMainAccountName(JAXBElement<String> value) {
    this.mainAccountName = value;
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
   * Gets the value of the oldMainAccountNo property.
   * 
   * @return possible object is {@link Integer }
   * 
   */
  public Integer getOldMainAccountNo() {
    return oldMainAccountNo;
  }

  /**
   * Sets the value of the oldMainAccountNo property.
   * 
   * @param value
   *          allowed object is {@link Integer }
   * 
   */
  public void setOldMainAccountNo(Integer value) {
    this.oldMainAccountNo = value;
  }

}
