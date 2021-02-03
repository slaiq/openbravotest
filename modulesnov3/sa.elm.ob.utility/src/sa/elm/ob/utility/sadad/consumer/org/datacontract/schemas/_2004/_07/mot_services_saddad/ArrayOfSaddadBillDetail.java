
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ArrayOfSaddadBillDetail complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfSaddadBillDetail"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SaddadBillDetail" type="{http://schemas.datacontract.org/2004/07/MOT.Services.Saddad.BI}SaddadBillDetail" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfSaddadBillDetail", propOrder = { "saddadBillDetail" })
public class ArrayOfSaddadBillDetail {

  @XmlElement(name = "SaddadBillDetail", nillable = true)
  protected List<SaddadBillDetail> saddadBillDetail;

  /**
   * Gets the value of the saddadBillDetail property.
   * 
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a <CODE>set</CODE> method for the saddadBillDetail property.
   * 
   * <p>
   * For example, to add a new item, do as follows:
   * 
   * <pre>
   * getSaddadBillDetail().add(newItem);
   * </pre>
   * 
   * 
   * <p>
   * Objects of the following type(s) are allowed in the list {@link SaddadBillDetail }
   * 
   * 
   */
  public List<SaddadBillDetail> getSaddadBillDetail() {
    if (saddadBillDetail == null) {
      saddadBillDetail = new ArrayList<SaddadBillDetail>();
    }
    return this.saddadBillDetail;
  }

}
