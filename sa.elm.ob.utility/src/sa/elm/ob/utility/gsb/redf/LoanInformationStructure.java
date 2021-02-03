
package sa.elm.ob.utility.gsb.redf;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LoanInformationStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LoanInformationStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CitizenName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ContractNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TotalLateInstallments" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="LoanerType" type="{http://yefi.gov.sa/REDF/LoanInquirySchema/xml/schemas/version2.0}LoanerTypeType"/>
 *         &lt;element name="IsScheduled" type="{http://yefi.gov.sa/CommonTypes/xml/schemas/version2.0}YesNoType"/>
 *         &lt;element name="IsDemanded" type="{http://yefi.gov.sa/CommonTypes/xml/schemas/version2.0}YesNoType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LoanInformationStructure", namespace = "http://yefi.gov.sa/REDF/LoanInquirySchema/xml/schemas/version2.0", propOrder = {
    "citizenName",
    "contractNumber",
    "totalLateInstallments",
    "loanerType",
    "isScheduled",
    "isDemanded"
})
public class LoanInformationStructure {

    @XmlElement(name = "CitizenName")
    protected String citizenName;
    @XmlElement(name = "ContractNumber")
    protected String contractNumber;
    @XmlElement(name = "TotalLateInstallments", required = true)
    protected BigDecimal totalLateInstallments;
    @XmlElement(name = "LoanerType", required = true)
    @XmlSchemaType(name = "string")
    protected LoanerTypeType loanerType;
    @XmlElement(name = "IsScheduled", required = true)
    @XmlSchemaType(name = "string")
    protected YesNoType isScheduled;
    @XmlElement(name = "IsDemanded", required = true)
    @XmlSchemaType(name = "string")
    protected YesNoType isDemanded;

    /**
     * Gets the value of the citizenName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCitizenName() {
        return citizenName;
    }

    /**
     * Sets the value of the citizenName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCitizenName(String value) {
        this.citizenName = value;
    }

    /**
     * Gets the value of the contractNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContractNumber() {
        return contractNumber;
    }

    /**
     * Sets the value of the contractNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContractNumber(String value) {
        this.contractNumber = value;
    }

    /**
     * Gets the value of the totalLateInstallments property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getTotalLateInstallments() {
        return totalLateInstallments;
    }

    /**
     * Sets the value of the totalLateInstallments property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setTotalLateInstallments(BigDecimal value) {
        this.totalLateInstallments = value;
    }

    /**
     * Gets the value of the loanerType property.
     * 
     * @return
     *     possible object is
     *     {@link LoanerTypeType }
     *     
     */
    public LoanerTypeType getLoanerType() {
        return loanerType;
    }

    /**
     * Sets the value of the loanerType property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoanerTypeType }
     *     
     */
    public void setLoanerType(LoanerTypeType value) {
        this.loanerType = value;
    }

    /**
     * Gets the value of the isScheduled property.
     * 
     * @return
     *     possible object is
     *     {@link YesNoType }
     *     
     */
    public YesNoType getIsScheduled() {
        return isScheduled;
    }

    /**
     * Sets the value of the isScheduled property.
     * 
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *     
     */
    public void setIsScheduled(YesNoType value) {
        this.isScheduled = value;
    }

    /**
     * Gets the value of the isDemanded property.
     * 
     * @return
     *     possible object is
     *     {@link YesNoType }
     *     
     */
    public YesNoType getIsDemanded() {
        return isDemanded;
    }

    /**
     * Sets the value of the isDemanded property.
     * 
     * @param value
     *     allowed object is
     *     {@link YesNoType }
     *     
     */
    public void setIsDemanded(YesNoType value) {
        this.isDemanded = value;
    }

}
