
package sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Record complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Record">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="UKName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="UKLabel" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="UKType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="UKValue" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Fields" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}ArrayOfRecordField" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Record", propOrder = {
    "ukName",
    "ukLabel",
    "ukType",
    "ukValue",
    "fields"
})
public class Record {

    @XmlElementRef(name = "UKName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> ukName;
    @XmlElementRef(name = "UKLabel", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> ukLabel;
    @XmlElementRef(name = "UKType", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> ukType;
    @XmlElementRef(name = "UKValue", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> ukValue;
    @XmlElementRef(name = "Fields", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<ArrayOfRecordField> fields;

    /**
     * Gets the value of the ukName property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getUKName() {
        return ukName;
    }

    /**
     * Sets the value of the ukName property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setUKName(JAXBElement<String> value) {
        this.ukName = value;
    }

    /**
     * Gets the value of the ukLabel property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getUKLabel() {
        return ukLabel;
    }

    /**
     * Sets the value of the ukLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setUKLabel(JAXBElement<String> value) {
        this.ukLabel = value;
    }

    /**
     * Gets the value of the ukType property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getUKType() {
        return ukType;
    }

    /**
     * Sets the value of the ukType property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setUKType(JAXBElement<String> value) {
        this.ukType = value;
    }

    /**
     * Gets the value of the ukValue property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getUKValue() {
        return ukValue;
    }

    /**
     * Sets the value of the ukValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setUKValue(JAXBElement<String> value) {
        this.ukValue = value;
    }

    /**
     * Gets the value of the fields property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfRecordField }{@code >}
     *     
     */
    public JAXBElement<ArrayOfRecordField> getFields() {
        return fields;
    }

    /**
     * Sets the value of the fields property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfRecordField }{@code >}
     *     
     */
    public void setFields(JAXBElement<ArrayOfRecordField> value) {
        this.fields = value;
    }

}
