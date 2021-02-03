
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PersonNameDetailsStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PersonNameDetailsStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="PersonNameBody" type="{http://yefi.gov.sa/PersonProfileCommonTypes/xml/schemas/version2.0}PersonNameBodyStructure" minOccurs="0"/>
 *           &lt;element name="PersonFullName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attribute name="language" use="required" type="{http://yefi.gov.sa/CommonTypes/xml/schemas/version2.0}LanguageType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PersonNameDetailsStructure", namespace = "http://yefi.gov.sa/PersonProfileCommonTypes/xml/schemas/version2.0", propOrder = {
    "personNameBody",
    "personFullName"
})
public class PersonNameDetailsStructure {

    @XmlElement(name = "PersonNameBody")
    protected PersonNameBodyStructure personNameBody;
    @XmlElement(name = "PersonFullName")
    protected String personFullName;
    @XmlAttribute(name = "language", required = true)
    protected LanguageType language;

    /**
     * Gets the value of the personNameBody property.
     * 
     * @return
     *     possible object is
     *     {@link PersonNameBodyStructure }
     *     
     */
    public PersonNameBodyStructure getPersonNameBody() {
        return personNameBody;
    }

    /**
     * Sets the value of the personNameBody property.
     * 
     * @param value
     *     allowed object is
     *     {@link PersonNameBodyStructure }
     *     
     */
    public void setPersonNameBody(PersonNameBodyStructure value) {
        this.personNameBody = value;
    }

    /**
     * Gets the value of the personFullName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPersonFullName() {
        return personFullName;
    }

    /**
     * Sets the value of the personFullName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPersonFullName(String value) {
        this.personFullName = value;
    }

    /**
     * Gets the value of the language property.
     * 
     * @return
     *     possible object is
     *     {@link LanguageType }
     *     
     */
    public LanguageType getLanguage() {
        return language;
    }

    /**
     * Sets the value of the language property.
     * 
     * @param value
     *     allowed object is
     *     {@link LanguageType }
     *     
     */
    public void setLanguage(LanguageType value) {
        this.language = value;
    }

}
