
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for GHDateStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GHDateStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GregorianDate" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="HijriDate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GHDateStructure", namespace = "http://yefi.gov.sa/CommonTypes/xml/schemas/version2.0", propOrder = {
    "gregorianDate",
    "hijriDate"
})
public class GHDateStructure {

    @XmlElement(name = "GregorianDate")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar gregorianDate;
    @XmlElement(name = "HijriDate")
    protected String hijriDate;

    /**
     * Gets the value of the gregorianDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getGregorianDate() {
        return gregorianDate;
    }

    /**
     * Sets the value of the gregorianDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setGregorianDate(XMLGregorianCalendar value) {
        this.gregorianDate = value;
    }

    /**
     * Gets the value of the hijriDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHijriDate() {
        return hijriDate;
    }

    /**
     * Sets the value of the hijriDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHijriDate(String value) {
        this.hijriDate = value;
    }

}
