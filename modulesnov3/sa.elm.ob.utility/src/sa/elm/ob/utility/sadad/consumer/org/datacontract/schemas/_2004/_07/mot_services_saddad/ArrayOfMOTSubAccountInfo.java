
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ArrayOfMOT_SubAccountInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfMOT_SubAccountInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="MOT_SubAccountInfo" type="{http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI}MOT_SubAccountInfo" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfMOT_SubAccountInfo", propOrder = { "motSubAccountInfo" })
public class ArrayOfMOTSubAccountInfo {

  @XmlElement(name = "MOT_SubAccountInfo", nillable = true)
  protected List<MOTSubAccountInfo> motSubAccountInfo;

  /**
   * Gets the value of the motSubAccountInfo property.
   * 
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a <CODE>set</CODE> method for the motSubAccountInfo property.
   * 
   * <p>
   * For example, to add a new item, do as follows:
   * 
   * <pre>
   * getMOTSubAccountInfo().add(newItem);
   * </pre>
   * 
   * 
   * <p>
   * Objects of the following type(s) are allowed in the list {@link MOTSubAccountInfo }
   * 
   * 
   */
  public List<MOTSubAccountInfo> getMOTSubAccountInfo() {
    if (motSubAccountInfo == null) {
      motSubAccountInfo = new ArrayList<MOTSubAccountInfo>();
    }
    return this.motSubAccountInfo;
  }

}
