
package sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RecordField complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RecordField">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="FieldName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="FieldLabel" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="FieldType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="FieldValue" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="SQLFieldValue" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="SQLFieldObject" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="FieldGroup" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="FieldHtml" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RecordField", propOrder = {
    "fieldName",
    "fieldLabel",
    "fieldType",
    "fieldValue",
    "sqlFieldValue",
    "sqlFieldObject",
    "fieldGroup",
    "fieldHtml"
})
public class RecordField {

    @XmlElementRef(name = "FieldName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> fieldName;
    @XmlElementRef(name = "FieldLabel", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> fieldLabel;
    @XmlElementRef(name = "FieldType", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> fieldType;
    @XmlElementRef(name = "FieldValue", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> fieldValue;
    @XmlElementRef(name = "SQLFieldValue", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> sqlFieldValue;
    @XmlElementRef(name = "SQLFieldObject", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> sqlFieldObject;
    @XmlElementRef(name = "FieldGroup", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> fieldGroup;
    @XmlElementRef(name = "FieldHtml", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> fieldHtml;

    /**
     * Gets the value of the fieldName property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFieldName() {
        return fieldName;
    }

    /**
     * Sets the value of the fieldName property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFieldName(JAXBElement<String> value) {
        this.fieldName = value;
    }

    /**
     * Gets the value of the fieldLabel property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFieldLabel() {
        return fieldLabel;
    }

    /**
     * Sets the value of the fieldLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFieldLabel(JAXBElement<String> value) {
        this.fieldLabel = value;
    }

    /**
     * Gets the value of the fieldType property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFieldType() {
        return fieldType;
    }

    /**
     * Sets the value of the fieldType property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFieldType(JAXBElement<String> value) {
        this.fieldType = value;
    }

    /**
     * Gets the value of the fieldValue property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFieldValue() {
        return fieldValue;
    }

    /**
     * Sets the value of the fieldValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFieldValue(JAXBElement<String> value) {
        this.fieldValue = value;
    }

    /**
     * Gets the value of the sqlFieldValue property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSQLFieldValue() {
        return sqlFieldValue;
    }

    /**
     * Sets the value of the sqlFieldValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSQLFieldValue(JAXBElement<String> value) {
        this.sqlFieldValue = value;
    }

    /**
     * Gets the value of the sqlFieldObject property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getSQLFieldObject() {
        return sqlFieldObject;
    }

    /**
     * Sets the value of the sqlFieldObject property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setSQLFieldObject(JAXBElement<String> value) {
        this.sqlFieldObject = value;
    }

    /**
     * Gets the value of the fieldGroup property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFieldGroup() {
        return fieldGroup;
    }

    /**
     * Sets the value of the fieldGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFieldGroup(JAXBElement<String> value) {
        this.fieldGroup = value;
    }

    /**
     * Gets the value of the fieldHtml property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFieldHtml() {
        return fieldHtml;
    }

    /**
     * Sets the value of the fieldHtml property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFieldHtml(JAXBElement<String> value) {
        this.fieldHtml = value;
    }

}
