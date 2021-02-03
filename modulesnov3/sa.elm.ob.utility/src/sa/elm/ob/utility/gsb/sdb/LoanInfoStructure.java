
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LoanInfoStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LoanInfoStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="LoanerInfo" type="{http://yefi.gov.sa/SCSB/LoanInformation/xml/schemas/version1.0}LoanerInfoStructure" minOccurs="0"/>
 *         &lt;element name="LoanDetails" type="{http://yefi.gov.sa/SCSB/LoanInformation/xml/schemas/version1.0}LoanDetailsStructure" minOccurs="0"/>
 *         &lt;element name="GuarantorInfo" type="{http://yefi.gov.sa/SCSB/LoanInformation/xml/schemas/version1.0}GuarantorInfoStructure" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LoanInfoStructure", propOrder = {
    "loanerInfo",
    "loanDetails",
    "guarantorInfo"
})
public class LoanInfoStructure {

    @XmlElement(name = "LoanerInfo")
    protected LoanerInfoStructure loanerInfo;
    @XmlElement(name = "LoanDetails")
    protected LoanDetailsStructure loanDetails;
    @XmlElement(name = "GuarantorInfo")
    protected GuarantorInfoStructure guarantorInfo;

    /**
     * Gets the value of the loanerInfo property.
     * 
     * @return
     *     possible object is
     *     {@link LoanerInfoStructure }
     *     
     */
    public LoanerInfoStructure getLoanerInfo() {
        return loanerInfo;
    }

    /**
     * Sets the value of the loanerInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoanerInfoStructure }
     *     
     */
    public void setLoanerInfo(LoanerInfoStructure value) {
        this.loanerInfo = value;
    }

    /**
     * Gets the value of the loanDetails property.
     * 
     * @return
     *     possible object is
     *     {@link LoanDetailsStructure }
     *     
     */
    public LoanDetailsStructure getLoanDetails() {
        return loanDetails;
    }

    /**
     * Sets the value of the loanDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoanDetailsStructure }
     *     
     */
    public void setLoanDetails(LoanDetailsStructure value) {
        this.loanDetails = value;
    }

    /**
     * Gets the value of the guarantorInfo property.
     * 
     * @return
     *     possible object is
     *     {@link GuarantorInfoStructure }
     *     
     */
    public GuarantorInfoStructure getGuarantorInfo() {
        return guarantorInfo;
    }

    /**
     * Sets the value of the guarantorInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link GuarantorInfoStructure }
     *     
     */
    public void setGuarantorInfo(GuarantorInfoStructure value) {
        this.guarantorInfo = value;
    }

}
