
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for GuarantorInfoStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GuarantorInfoStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GuarantorIdentity" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="GuarantorName" type="{http://yefi.gov.sa/PersonProfileCommonTypes/xml/schemas/version2.0}PersonNameDetailsStructure" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GuarantorInfoStructure", propOrder = {
    "guarantorIdentity",
    "guarantorName"
})
public class GuarantorInfoStructure {

    @XmlElement(name = "GuarantorIdentity")
    protected String guarantorIdentity;
    @XmlElement(name = "GuarantorName")
    protected PersonNameDetailsStructure guarantorName;

    /**
     * Gets the value of the guarantorIdentity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGuarantorIdentity() {
        return guarantorIdentity;
    }

    /**
     * Sets the value of the guarantorIdentity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGuarantorIdentity(String value) {
        this.guarantorIdentity = value;
    }

    /**
     * Gets the value of the guarantorName property.
     * 
     * @return
     *     possible object is
     *     {@link PersonNameDetailsStructure }
     *     
     */
    public PersonNameDetailsStructure getGuarantorName() {
        return guarantorName;
    }

    /**
     * Sets the value of the guarantorName property.
     * 
     * @param value
     *     allowed object is
     *     {@link PersonNameDetailsStructure }
     *     
     */
    public void setGuarantorName(PersonNameDetailsStructure value) {
        this.guarantorName = value;
    }

}
