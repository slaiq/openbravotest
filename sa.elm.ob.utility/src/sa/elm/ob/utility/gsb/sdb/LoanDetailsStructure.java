
package sa.elm.ob.utility.gsb.sdb;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LoanDetailsStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LoanDetailsStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="LoanNumber" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="LoanDate" type="{http://yefi.gov.sa/CommonTypes/xml/schemas/version2.0}GHDateStructure" minOccurs="0"/>
 *         &lt;element name="LoanStatus" type="{http://yefi.gov.sa/SCSB/LoanInformation/xml/schemas/version1.0}LoanStatusType"/>
 *         &lt;element name="LoanClass" type="{http://yefi.gov.sa/SCSB/LoanInformation/xml/schemas/version1.0}LoanClassType"/>
 *         &lt;element name="Affiliation" type="{http://yefi.gov.sa/SCSB/LoanInformation/xml/schemas/version1.0}AffiliationType"/>
 *         &lt;element name="LoanAmount" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="PaidAmount" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="RemainingAmount" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="LateAmount" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="NumberOfInstallments" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="InstallmentsAmount" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         &lt;element name="LastInstallmentsDate" type="{http://yefi.gov.sa/CommonTypes/xml/schemas/version2.0}GHDateStructure" minOccurs="0"/>
 *         &lt;element name="LastInstallmentsAmount" type="{http://www.w3.org/2001/XMLSchema}decimal" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LoanDetailsStructure", propOrder = {
    "loanNumber",
    "loanDate",
    "loanStatus",
    "loanClass",
    "affiliation",
    "loanAmount",
    "paidAmount",
    "remainingAmount",
    "lateAmount",
    "numberOfInstallments",
    "installmentsAmount",
    "lastInstallmentsDate",
    "lastInstallmentsAmount"
})
public class LoanDetailsStructure {

    @XmlElement(name = "LoanNumber")
    protected String loanNumber;
    @XmlElement(name = "LoanDate")
    protected GHDateStructure loanDate;
    @XmlElement(name = "LoanStatus", required = true)
    @XmlSchemaType(name = "string")
    protected LoanStatusType loanStatus;
    @XmlElement(name = "LoanClass", required = true)
    @XmlSchemaType(name = "string")
    protected LoanClassType loanClass;
    @XmlElement(name = "Affiliation", required = true)
    @XmlSchemaType(name = "string")
    protected AffiliationType affiliation;
    @XmlElement(name = "LoanAmount", required = true)
    protected BigDecimal loanAmount;
    @XmlElement(name = "PaidAmount", required = true)
    protected BigDecimal paidAmount;
    @XmlElement(name = "RemainingAmount", required = true)
    protected BigDecimal remainingAmount;
    @XmlElement(name = "LateAmount", required = true)
    protected BigDecimal lateAmount;
    @XmlElement(name = "NumberOfInstallments")
    protected int numberOfInstallments;
    @XmlElement(name = "InstallmentsAmount", required = true)
    protected BigDecimal installmentsAmount;
    @XmlElement(name = "LastInstallmentsDate")
    protected GHDateStructure lastInstallmentsDate;
    @XmlElement(name = "LastInstallmentsAmount")
    protected BigDecimal lastInstallmentsAmount;

    /**
     * Gets the value of the loanNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLoanNumber() {
        return loanNumber;
    }

    /**
     * Sets the value of the loanNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLoanNumber(String value) {
        this.loanNumber = value;
    }

    /**
     * Gets the value of the loanDate property.
     * 
     * @return
     *     possible object is
     *     {@link GHDateStructure }
     *     
     */
    public GHDateStructure getLoanDate() {
        return loanDate;
    }

    /**
     * Sets the value of the loanDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link GHDateStructure }
     *     
     */
    public void setLoanDate(GHDateStructure value) {
        this.loanDate = value;
    }

    /**
     * Gets the value of the loanStatus property.
     * 
     * @return
     *     possible object is
     *     {@link LoanStatusType }
     *     
     */
    public LoanStatusType getLoanStatus() {
        return loanStatus;
    }

    /**
     * Sets the value of the loanStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoanStatusType }
     *     
     */
    public void setLoanStatus(LoanStatusType value) {
        this.loanStatus = value;
    }

    /**
     * Gets the value of the loanClass property.
     * 
     * @return
     *     possible object is
     *     {@link LoanClassType }
     *     
     */
    public LoanClassType getLoanClass() {
        return loanClass;
    }

    /**
     * Sets the value of the loanClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoanClassType }
     *     
     */
    public void setLoanClass(LoanClassType value) {
        this.loanClass = value;
    }

    /**
     * Gets the value of the affiliation property.
     * 
     * @return
     *     possible object is
     *     {@link AffiliationType }
     *     
     */
    public AffiliationType getAffiliation() {
        return affiliation;
    }

    /**
     * Sets the value of the affiliation property.
     * 
     * @param value
     *     allowed object is
     *     {@link AffiliationType }
     *     
     */
    public void setAffiliation(AffiliationType value) {
        this.affiliation = value;
    }

    /**
     * Gets the value of the loanAmount property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    /**
     * Sets the value of the loanAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setLoanAmount(BigDecimal value) {
        this.loanAmount = value;
    }

    /**
     * Gets the value of the paidAmount property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    /**
     * Sets the value of the paidAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setPaidAmount(BigDecimal value) {
        this.paidAmount = value;
    }

    /**
     * Gets the value of the remainingAmount property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    /**
     * Sets the value of the remainingAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRemainingAmount(BigDecimal value) {
        this.remainingAmount = value;
    }

    /**
     * Gets the value of the lateAmount property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getLateAmount() {
        return lateAmount;
    }

    /**
     * Sets the value of the lateAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setLateAmount(BigDecimal value) {
        this.lateAmount = value;
    }

    /**
     * Gets the value of the numberOfInstallments property.
     * 
     */
    public int getNumberOfInstallments() {
        return numberOfInstallments;
    }

    /**
     * Sets the value of the numberOfInstallments property.
     * 
     */
    public void setNumberOfInstallments(int value) {
        this.numberOfInstallments = value;
    }

    /**
     * Gets the value of the installmentsAmount property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getInstallmentsAmount() {
        return installmentsAmount;
    }

    /**
     * Sets the value of the installmentsAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setInstallmentsAmount(BigDecimal value) {
        this.installmentsAmount = value;
    }

    /**
     * Gets the value of the lastInstallmentsDate property.
     * 
     * @return
     *     possible object is
     *     {@link GHDateStructure }
     *     
     */
    public GHDateStructure getLastInstallmentsDate() {
        return lastInstallmentsDate;
    }

    /**
     * Sets the value of the lastInstallmentsDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link GHDateStructure }
     *     
     */
    public void setLastInstallmentsDate(GHDateStructure value) {
        this.lastInstallmentsDate = value;
    }

    /**
     * Gets the value of the lastInstallmentsAmount property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getLastInstallmentsAmount() {
        return lastInstallmentsAmount;
    }

    /**
     * Sets the value of the lastInstallmentsAmount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setLastInstallmentsAmount(BigDecimal value) {
        this.lastInstallmentsAmount = value;
    }

}
