
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ArrayOfApplicationTypeInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfApplicationTypeInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="ApplicationTypeInfo" type="{http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI}ApplicationTypeInfo" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfApplicationTypeInfo", propOrder = { "applicationTypeInfo" })
public class ArrayOfApplicationTypeInfo {

  @XmlElement(name = "ApplicationTypeInfo", nillable = true)
  protected List<ApplicationTypeInfo> applicationTypeInfo;

  /**
   * Gets the value of the applicationTypeInfo property.
   * 
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a <CODE>set</CODE> method for the applicationTypeInfo property.
   * 
   * <p>
   * For example, to add a new item, do as follows:
   * 
   * <pre>
   * getApplicationTypeInfo().add(newItem);
   * </pre>
   * 
   * 
   * <p>
   * Objects of the following type(s) are allowed in the list {@link ApplicationTypeInfo }
   * 
   * 
   */
  public List<ApplicationTypeInfo> getApplicationTypeInfo() {
    if (applicationTypeInfo == null) {
      applicationTypeInfo = new ArrayList<ApplicationTypeInfo>();
    }
    return this.applicationTypeInfo;
  }

}
