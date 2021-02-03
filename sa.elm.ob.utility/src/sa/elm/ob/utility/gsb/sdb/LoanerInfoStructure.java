
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LoanerInfoStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LoanerInfoStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="LoanerIdentity" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="LoanerName" type="{http://yefi.gov.sa/PersonProfileCommonTypes/xml/schemas/version2.0}PersonNameDetailsStructure" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LoanerInfoStructure", propOrder = {
    "loanerIdentity",
    "loanerName"
})
public class LoanerInfoStructure {

    @XmlElement(name = "LoanerIdentity")
    protected String loanerIdentity;
    @XmlElement(name = "LoanerName")
    protected PersonNameDetailsStructure loanerName;

    /**
     * Gets the value of the loanerIdentity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLoanerIdentity() {
        return loanerIdentity;
    }

    /**
     * Sets the value of the loanerIdentity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLoanerIdentity(String value) {
        this.loanerIdentity = value;
    }

    /**
     * Gets the value of the loanerName property.
     * 
     * @return
     *     possible object is
     *     {@link PersonNameDetailsStructure }
     *     
     */
    public PersonNameDetailsStructure getLoanerName() {
        return loanerName;
    }

    /**
     * Sets the value of the loanerName property.
     * 
     * @param value
     *     allowed object is
     *     {@link PersonNameDetailsStructure }
     *     
     */
    public void setLoanerName(PersonNameDetailsStructure value) {
        this.loanerName = value;
    }

}
