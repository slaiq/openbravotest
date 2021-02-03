
package sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfXmlAttribute complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfXmlAttribute">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="XmlAttribute" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}XmlAttribute" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfXmlAttribute", propOrder = {
    "xmlAttribute"
})
public class ArrayOfXmlAttribute {

    @XmlElement(name = "XmlAttribute", nillable = true)
    protected List<XmlAttribute> xmlAttribute;

    /**
     * Gets the value of the xmlAttribute property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the xmlAttribute property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getXmlAttribute().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XmlAttribute }
     * 
     * 
     */
    public List<XmlAttribute> getXmlAttribute() {
        if (xmlAttribute == null) {
            xmlAttribute = new ArrayList<XmlAttribute>();
        }
        return this.xmlAttribute;
    }

}
