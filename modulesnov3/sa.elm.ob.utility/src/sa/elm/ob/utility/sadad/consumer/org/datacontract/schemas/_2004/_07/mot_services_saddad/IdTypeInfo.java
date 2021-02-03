
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for IdTypeInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IdTypeInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="IdTypeCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="IdTypeName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdTypeInfo", propOrder = { "idTypeCode", "idTypeName" })
public class IdTypeInfo {

  @XmlElementRef(name = "IdTypeCode", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> idTypeCode;
  @XmlElementRef(name = "IdTypeName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI", type = JAXBElement.class, required = false)
  protected JAXBElement<String> idTypeName;

  /**
   * Gets the value of the idTypeCode property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getIdTypeCode() {
    return idTypeCode;
  }

  /**
   * Sets the value of the idTypeCode property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setIdTypeCode(JAXBElement<String> value) {
    this.idTypeCode = value;
  }

  /**
   * Gets the value of the idTypeName property.
   * 
   * @return possible object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public JAXBElement<String> getIdTypeName() {
    return idTypeName;
  }

  /**
   * Sets the value of the idTypeName property.
   * 
   * @param value
   *          allowed object is {@link JAXBElement }{@code <}{@link String }{@code >}
   * 
   */
  public void setIdTypeName(JAXBElement<String> value) {
    this.idTypeName = value;
  }

}
