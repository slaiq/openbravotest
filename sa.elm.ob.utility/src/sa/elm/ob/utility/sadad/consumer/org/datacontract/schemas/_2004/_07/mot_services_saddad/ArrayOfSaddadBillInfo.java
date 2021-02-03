
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ArrayOfSaddadBillInfo complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfSaddadBillInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SaddadBillInfo" type="{http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI}SaddadBillInfo" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfSaddadBillInfo", propOrder = { "saddadBillInfo" })
public class ArrayOfSaddadBillInfo {

  @XmlElement(name = "SaddadBillInfo", nillable = true)
  protected List<SaddadBillInfo> saddadBillInfo;

  /**
   * Gets the value of the saddadBillInfo property.
   * 
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a <CODE>set</CODE> method for the saddadBillInfo property.
   * 
   * <p>
   * For example, to add a new item, do as follows:
   * 
   * <pre>
   * getSaddadBillInfo().add(newItem);
   * </pre>
   * 
   * 
   * <p>
   * Objects of the following type(s) are allowed in the list {@link SaddadBillInfo }
   * 
   * 
   */
  public List<SaddadBillInfo> getSaddadBillInfo() {
    if (saddadBillInfo == null) {
      saddadBillInfo = new ArrayList<SaddadBillInfo>();
    }
    return this.saddadBillInfo;
  }

}
